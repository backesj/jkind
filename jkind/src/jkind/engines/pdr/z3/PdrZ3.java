package jkind.engines.pdr.z3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.uni_freiburg.informatik.ultimate.logic.Term;
import jkind.engines.pdr.Cube;
import jkind.engines.pdr.Frame;
import jkind.engines.pdr.NameGenerator;
import jkind.engines.pdr.PdrSmt;
import jkind.engines.pdr.SmtInterpolFrame;
import jkind.engines.pdr.PdrSmtInterpol.Option;
import jkind.engines.pdr.TCube;
import jkind.lustre.Expr;
import jkind.lustre.Function;
import jkind.lustre.Node;
import jkind.lustre.VarDecl;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.sexp.Symbol;
import jkind.solvers.z3.Z3Solver;
import jkind.translation.Lustre2Sexp;
import jkind.translation.Relation;
import jkind.util.StreamIndex;
import jkind.util.Util;

public class PdrZ3 extends Z3Solver implements PdrSmt{

	private final List<VarDecl> varDecls;
	private final Map<String, Collection<Sexp>> variableLists = new HashMap<>();

	private final Sexp I;
	private final Sexp P;
	private final List<Frame> F;

	private final Set<Sexp> predicates = new HashSet<>();

	private final NameGenerator abstractAssertions = new NameGenerator("abstract");
	private final Node node;
	private final List<Function> functions;
	
	
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
		List<Sexp> varSyms = varDecls.stream().map(v -> new Symbol(v.id)).collect(Collectors.toList());
		List<Sexp> baseAbs = varSyms.stream().map(s -> abs(pre(s))).collect(Collectors.toList());
		List<Sexp> primeAbs = varSyms.stream().map(s -> abs(curr(s))).collect(Collectors.toList());
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
			return new Cons(cons.head, cons.args.stream().map(a -> pre(a)).collect(Collectors.toList()));
		}
	}
	
	private Sexp curr(Sexp sexp){
		if (sexp instanceof Symbol) {
			Symbol sym = (Symbol) sexp;
			return curr(sym.str);
		}else{
			Cons cons = (Cons)sexp;
			return new Cons(cons.head, cons.args.stream().map(a -> curr(a)).collect(Collectors.toList()));
		}
	}
	
	private Sexp abs(Sexp sexp){
		if (sexp instanceof Symbol) {
			Symbol sym = (Symbol) sexp;
			return abs(sym.str);
		}else{
			Cons cons = (Cons)sexp;
			return new Cons(cons.head, cons.args.stream().map(a -> abs(a)).collect(Collectors.toList()));
		}
	}
	
	private Sexp name(Sexp sexp, String name){
		return new Cons("!", sexp, new Symbol(":named"), new Symbol(name));
	}
	
	private Sexp abs(String str){
		return new Symbol(str+"-");
	}
	
	private Sexp pre(String str){
		return new StreamIndex(str, 0).getEncoded();
	}
	
	private Sexp curr(String str){
		return new StreamIndex(str, 1).getEncoded();
	}
	
	private List<Sexp> concat(List<Sexp> a, List<Sexp> b){
		List<Sexp> result = new ArrayList<Sexp>(a.size() + b.size());
		result.addAll(a);
		result.addAll(b);
		return result;
	}

	@Override
	public Cube getBadCube() {
		query(and(R(depth()), not(P)))
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
	
	@Override
	public boolean isInitial(Cube cube) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public TCube solveRelative(TCube s, Option option) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public TCube solveRelative(TCube s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isBlocked(TCube s) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Frame createInitialFrame() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void refine(List<Cube> cubes) {
		// TODO Auto-generated method stub
		
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
