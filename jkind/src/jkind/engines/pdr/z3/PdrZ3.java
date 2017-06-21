package jkind.engines.pdr.z3;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.uni_freiburg.informatik.ultimate.logic.Term;
import jkind.JKindException;
import jkind.engines.StopException;
import jkind.engines.pdr.CounterexampleException;
import jkind.engines.pdr.Cube;
import jkind.engines.pdr.Frame;
import jkind.engines.pdr.NameGenerator;
import jkind.engines.pdr.PdrSmt;
import jkind.engines.pdr.PredicateCollector;
import jkind.engines.pdr.SmtInterpolCube;
import jkind.engines.pdr.SmtInterpolFrame;
import jkind.engines.pdr.PdrSmtInterpol.Option;
import jkind.engines.pdr.TCube;
import jkind.lustre.Expr;
import jkind.lustre.Function;
import jkind.lustre.Node;
import jkind.lustre.VarDecl;
import jkind.lustre.values.BooleanValue;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.sexp.Symbol;
import jkind.solvers.Model;
import jkind.solvers.Result;
import jkind.solvers.SatResult;
import jkind.solvers.SimpleModel;
import jkind.solvers.UnsatResult;
import jkind.solvers.z3.Z3Solver;
import jkind.translation.Lustre2Sexp;
import jkind.translation.Relation;
import jkind.util.StreamIndex;
import jkind.util.Util;

public class PdrZ3 extends Z3Solver implements PdrSmt{

	private final List<VarDecl> varDecls;
	private final Map<String, List<Sexp>> variableLists = new HashMap<>();

	private final Sexp I;
	private final Sexp P;
	private final List<Frame> F;

	private final Set<Sexp> predicates = new HashSet<>();

	private final NameGenerator abstractAssertions = new NameGenerator("abstract");
	private final Node node;
	private final List<Function> functions;
	private final List<Sexp> varSyms;
	
	
	public PdrZ3(Node node, List<Function> functions, List<Frame> F, String property, String scratchBase){
		super(scratchBase, false);
		Relation relation = Lustre2Sexp.constructTransitionRelation(node);
		define(relation);
		varDecls = Util.getVarDecls(node);
		
		I = Lustre2Sexp.INIT;
		P = new StreamIndex(property, 1).getEncoded();
		this.F = F;
		
		this.node = node;
		this.functions = functions;
		
		//assert the relation
		varSyms = varDecls.stream().map(v -> new Symbol(v.id)).collect(toList());
		List<Sexp> baseAbs = varSyms.stream().map(s -> abs(pre(s))).collect(toList());
		List<Sexp> primeAbs = varSyms.stream().map(s -> abs(curr(s))).collect(toList());
		assertSexp(T(baseAbs, primeAbs));
		addPredicates(SexpPredicateCollector.collect(I));
		addPredicates(SexpPredicateCollector.collect(P));
	}
	
	
	private void addPredicates(Set<Sexp> otherPredicates) {
		otherPredicates.removeAll(predicates);
		predicates.addAll(otherPredicates);
		for (Sexp sexp : otherPredicates) {
			comment("New predicate: " + sexp);
			assertAbstract(new Cons("=", pre(sexp), abs(pre(sexp))));
			assertAbstract(new Cons("=", curr(sexp), abs(curr(sexp))));
		}
		return;
	}
	
	private void assertAbstract(Sexp sexp){
		assertSexp(name(sexp, abstractAssertions.getNextName()));
	}
	
	private Sexp T(List<Sexp> baseVars, List<Sexp> primeVars){
		return new Cons(Relation.T, concat(baseVars, primeVars));
	}
	
	private Sexp pre(Sexp sexp){
		if (sexp instanceof Symbol) {
			Symbol sym = (Symbol) sexp;
			return pre(sym.str);
		}else{
			Cons cons = (Cons)sexp;
			return new Cons(cons.head, cons.args.stream().map(a -> pre(a)).collect(toList()));
		}
	}
	
	private Sexp curr(Sexp sexp){
		if (sexp instanceof Symbol) {
			Symbol sym = (Symbol) sexp;
			return curr(sym.str);
		}else{
			Cons cons = (Cons)sexp;
			return new Cons(cons.head, cons.args.stream().map(a -> curr(a)).collect(toList()));
		}
	}
	
	private Sexp abs(Sexp sexp){
		if (sexp instanceof Symbol) {
			Symbol sym = (Symbol) sexp;
			return abs(sym.str);
		}else{
			Cons cons = (Cons)sexp;
			return new Cons(cons.head, cons.args.stream().map(a -> abs(a)).collect(toList()));
		}
	}
	
	private Sexp name(Sexp sexp, String name){
		return new Cons("!", sexp, new Symbol(":named"), new Symbol(name));
	}
	
	private Sexp abs(String str){
		if(stringIsConst(str)){
			return new Symbol(str);
		}
		return new Symbol(str+"-");
	}
	
	private Sexp pre(String str){
		if(stringIsConst(str)){
			return new Symbol(str);
		}
		return new StreamIndex(str, 0).getEncoded();
	}
	
	private Sexp curr(String str){
		if(stringIsConst(str)){
			return new Symbol(str);
		}
		return new StreamIndex(str, 1).getEncoded();
	}
	
	private Sexp index(Sexp sexp, int i){
		if (sexp instanceof Symbol) {
			Symbol sym = (Symbol) sexp;
			return index(sym.str, i);
		}else{
			Cons cons = (Cons)sexp;
			return new Cons(cons.head, cons.args.stream().map(a -> index(a, i)).collect(toList()));
		}
	}
	
	private Sexp index(String str, int i){
		if(stringIsConst(str)){
			return new Symbol(str);
		}
		return new StreamIndex(str, i).getEncoded();
	}
	
	private boolean stringIsConst(String str){
		boolean result = false;
		result |= str.equals("true");
		result |= str.equals("false");
		result |= str.matches("^[0-9]*\\.?[0-9]*$");
		return result;
	}
	
	private List<Sexp> concat(List<Sexp> a, List<Sexp> b){
		List<Sexp> result = new ArrayList<Sexp>(a.size() + b.size());
		result.addAll(a);
		result.addAll(b);
		return result;
	}

	@Override
	public Cube getBadCube() {
		Result result = query(and(R(depth()), not(P)));
		if(result instanceof UnsatResult){
			return null;
		}
		if (result instanceof SatResult) {
			SatResult satRes = (SatResult) result;
			return extractCube(satRes.getModel());
		}
		throw new JKindException("We expect the solver to return either SAT or UNSAT");
	}


	private Cube extractCube(Model model) {
		List<Sexp> lits = new ArrayList<>();
		for(Sexp predicate : predicates){
			BooleanValue val = (BooleanValue) PredicateEvaluator.evaluate(model, predicate);
			lits.add(val.value ? predicate : not(predicate));
		}
		return new SexpCube(lits);
	}
	
	
	private int depth() {
		return F.size() - 2;
	}
	
	private Sexp R(int k) {
		List<Sexp> conjuncts = new ArrayList<>();
		for (int i = k; i < F.size(); i++) {
			conjuncts.add(((SexpFrame)F.get(i)).toSexp());
		}
		return and(conjuncts);
	}
	
	
	private Sexp and(Sexp head, Sexp ... others){
		List<Sexp> elements = new ArrayList<>();
		elements.add(head);
		elements.addAll(Arrays.asList(others));
		return and(elements);
	}
	
	private Sexp and(List<Sexp> sexps){
		if(sexps.size() == 0){
			throw new IllegalArgumentException();
		}
		if(sexps.size() == 1){
			return sexps.get(0);
		}
		return new Cons("and", sexps);
	}

	private Sexp not(Sexp sexp){
		return new Cons("not", sexp);
	}
	
	private Sexp not(SexpCube cube){
		return new Cons("not", cube.toSexp());
	}
	
	@Override
	public boolean isInitial(Cube cube) {
		return !((SexpCube)cube).getLits().contains(not(I));
	}

	@Override
	public TCube solveRelative(TCube s, Option option) {
		int frame = s.getFrame();
		SexpCube cube = (SexpCube)s.getCube();

		push();

		if (option != Option.NO_IND) {
			assertSexp(not(cube));
		}

		for (int i = frame - 1; i < F.size() - 1; i++) {
			assertSexp(name(((SexpFrame)F.get(i)).toSexp(), "F" + i));
		}
		assertSexp(((SexpFrame)F.get(F.size() - 1)).toSexp());

		List<Sexp> pLiterals = ((SexpCube)s.getCube()).getLits();
		for (int i = 0; i < pLiterals.size(); i++) {
			assertSexp(name(curr(pLiterals.get(i)), "P" + i));
		}

		Result result = checkSat();
		if(result instanceof UnsatResult){
			List<Symbol> unsatCore = getUnsatCore();
			pop();
			int minFrame = getMinimumF(unsatCore);
			SexpCube minCube = getMinimalNonInitialCube(pLiterals, unsatCore);

			if (minFrame == F.size() - 2 || minFrame == TCube.FRAME_INF) {
				return new TCube(minCube, minFrame);
			} else {
				return new TCube(minCube, minFrame + 1);
			}
		}else if(result instanceof SatResult){
		
			Cube c = null;
			if (option == Option.EXTRACT_MODEL) {
				c = extractCube(((SatResult)result).getModel());
			}
			pop();
			return new TCube(c, TCube.FRAME_NULL);

		}else{
			commentUnknownReason();
			throw new StopException();
		}
	}
	
	private void commentUnknownReason() {
		comment("Z3 solver returned 'unknown'");
	}

	
	private int getMinimumF(List<Symbol> unsatCore) {
		int min = TCube.FRAME_INF;
		for (Symbol t : unsatCore) {
			String name = t.toString();
			if (name.startsWith("F")) {
				int frame = Integer.parseInt(name.substring(1));
				if (frame < min) {
					min = frame;
				}
			}
		}
		return min;
	}
	
	private SexpCube getMinimalNonInitialCube(List<Sexp> plits, List<Symbol> unsatCore) {
		List<Sexp> lits = new ArrayList<>();
		for (Symbol t : unsatCore) {
			String name = t.toString();
			if (name.startsWith("P")) {
				int pIndex = Integer.parseInt(name.substring(1));
				lits.add(plits.get(pIndex));
			}
		}

		SexpCube sexpCube = new SexpCube(lits);
		if (isInitial(sexpCube)) {
			lits.add(not(I));
			sexpCube = new SexpCube(lits);
		}

		return sexpCube;
	}
	
	private Result checkSat(){
		return query(new Symbol("true"));
	}
	
	protected List<Symbol> getUnsatCore() {
		List<Symbol> unsatCore = new ArrayList<>();
		send("(get-unsat-core)");
		for (String s : readCore().split(" ")) {
			if (!s.isEmpty()) {
				unsatCore.add(new Symbol(s));
			}
		}
		return unsatCore;
	}
	
	@Override
	public TCube solveRelative(TCube s) {
		return solveRelative(s, Option.DEFAULT);
	}

	@Override
	public boolean isBlocked(TCube s) {
		int frame = s.getFrame();
		SexpCube cube = (SexpCube)s.getCube();
		Sexp query = and(cube.toSexp(), R(frame));
		Result result = query(query);
		return result instanceof UnsatResult;
	}

	@Override
	public Frame createInitialFrame() {
		SexpFrame frame = new SexpFrame();
		frame.add(new SexpCube(Collections.singletonList(I)));
		return frame;
	}

	@Override
	public void refine(List<Cube> cubes) {
		List<Sexp> pieces = new ArrayList<>();

		List<Sexp> preVars = varSyms.stream().map(v -> index(v,-1)).collect(toList());
		for (int i = 0; i < cubes.size() - 1; i++) {
			List<Sexp> postVars = varSyms.stream().map(v -> index(v,i)).collect(toList());
			pieces.add(and(index(((SexpCube)cubes.get(i)).toSexp(), i), T(preVars, postVars)));
			preVars = postVars;
		}
		int index = cubes.size() - 1;
		pieces.add(and(index(((SexpCube)cubes.get(index)).toSexp(), index), not(index(P,index))));

		List<Sexp> interpolants = getInterpolants(pieces);

		for (int i = 0; i < cubes.size() - 1; i++) {
			vars = getVariables(StreamIndex.getSuffix(i));
			addPredicates(PredicateCollector.collect(subst(interpolants[i], vars, base)));
		}
	}
	
	private List<Sexp> getInterpolants(List<Sexp> terms) {
		push();
		
		List<Sexp> names = new ArrayList<>();
		for (int i = 0; i < terms.size(); i++) {
			String name = "I" + i;
			names.add(new Symbol(name));
			assertSexp(name(terms.get(i), name));
		}
		
		Result result = checkSat();
		if(result instanceof UnsatResult) {
			send(new Cons("get-interpolant", names));
			Term[] interps = script.getInterpolants(names);
			pop();
			return interps;

		} else if(result instanceof SatResult){
			int length = terms.size() - 1;
			SimpleModel extractedModel = extractModel(script.getModel(), length);
			throw new CounterexampleException(length, extractedModel);

		} else {
			commentUnknownReason();
			throw new StopException();
		}
	}
	
	List<Sexp> readInterpolants(int n){
		List<Sexp> sexps = new ArrayList<>();
		for(int i = 0; i < n; i++){
			String line = "";
			try {
				line = fromSolver.readLine();
				comment(getSolverName() + ": " + line);
			} catch (IOException e) {
				e.printStackTrace();
			}
			sexps.add(stringToSexp(line));
		}
		return sexps;
	}

	private Sexp stringToSexp(String line) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void comment(String comment) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Expr getInvariant(Cube cube) {
		// TODO Auto-generated method stub
		return null;
	}

}
