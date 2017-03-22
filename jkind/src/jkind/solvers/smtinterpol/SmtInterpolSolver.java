package jkind.solvers.smtinterpol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;

import jkind.JKindException;
import jkind.analysis.evaluation.SmtInterpolFunctionEvaluator;
import jkind.lustre.Function;
import jkind.lustre.Node;
import jkind.lustre.Type;
import jkind.lustre.VarDecl;
import jkind.lustre.values.Value;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.sexp.Symbol;
import jkind.solvers.Model;
import jkind.solvers.Result;
import jkind.solvers.SatResult;
import jkind.solvers.SimpleModel;
import jkind.solvers.Solver;
import jkind.solvers.SolverParserErrorListener;
import jkind.solvers.UnknownResult;
import jkind.solvers.UnsatResult;
import jkind.solvers.smtlib2.ModelExtractor;
import jkind.solvers.smtlib2.SmtLib2Lexer;
import jkind.solvers.smtlib2.SmtLib2Parser;
import jkind.solvers.smtlib2.SmtLib2Parser.ModelContext;
import jkind.solvers.smtlib2.SmtLib2Solver;
import jkind.translation.Relation;
import jkind.util.FunctionTable;
import de.uni_freiburg.informatik.ultimate.logic.Annotation;
import de.uni_freiburg.informatik.ultimate.logic.Logics;
import de.uni_freiburg.informatik.ultimate.logic.QuotedObject;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;

public class SmtInterpolSolver extends Solver {
	private final Script script;

	public SmtInterpolSolver(String scratchBase) {
		this.script = SmtInterpolUtil.getScript(scratchBase);
	}

	@Override
	public void initialize() {
		script.setOption(":produce-unsat-cores", true);
		script.setLogic(Logics.QF_UFLIRA);
		script.setOption(":verbosity", 2);
	}

	@Override
	public void assertSexp(Sexp sexp) {
		script.assertTerm(convert(sexp));
	}

	@Override
	public void define(VarDecl decl) {
		varTypes.put(decl.id, decl.type);
		script.declareFun(decl.id, new Sort[0], getSort(decl.type));
	}

	@Override
	public void define(Relation relation) {
		TermVariable[] params = createTermVariables(relation.getInputs());
		Term definition = convert(params, relation.getBody());
		script.defineFun(relation.getName(), params, script.sort("Bool"), definition);
	}

	private TermVariable[] createTermVariables(List<VarDecl> inputs) {
		return inputs.stream().map(this::createTermVariable).toArray(i -> new TermVariable[i]);
	}

	private TermVariable createTermVariable(VarDecl decl) {
		return script.variable(decl.id, getSort(decl.type));
	}

	@Override
	public Result query(Sexp sexp) {
		Model model;

		push();
		assertSexp(new Cons("not", sexp));

		switch (script.checkSat()) {
		case SAT:
			model = extractModel(script.getModel());
			pop();
			return new SatResult(model);

		case UNSAT:
			pop();
			return new UnsatResult();

		case UNKNOWN:
			model = extractModel(script.getModel());
			pop();
			return new UnknownResult(model);
		}

		throw new JKindException("Unhandled result from solver");
	}

	@Override
	protected Result quickCheckSat(List<Symbol> activationLiterals) {
		push();

		for (Symbol actLit : activationLiterals) {
			String name = "_" + actLit.str;
			script.assertTerm(script.annotate(convert(actLit), new Annotation(":named", name)));
		}

		switch (script.checkSat()) {
		case SAT:
			pop();
			return new SatResult();

		case UNSAT:
			List<Symbol> unsatCore = new ArrayList<>();
			for (Term t : script.getUnsatCore()) {
				unsatCore.add(new Symbol(t.toString().substring(1)));
			}
			pop();
			return new UnsatResult(unsatCore);

		case UNKNOWN:
			pop();
			return new UnknownResult();
		}

		throw new JKindException("Unhandled result from solver");
	}

	private Model extractModel(de.uni_freiburg.informatik.ultimate.logic.Model model) {
		return SmtLib2Solver.parseSMTLib2Model(model.toString(), varTypes, "SMTInterpol");
	}
	
	protected Model parseModel(String string) {
		CharStream stream = new ANTLRInputStream(string);
		SmtLib2Lexer lexer = new SmtLib2Lexer(stream);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		SmtLib2Parser parser = new SmtLib2Parser(tokens);
		parser.removeErrorListeners();
		parser.addErrorListener(new SolverParserErrorListener());
		ModelContext ctx = parser.model();

		if (parser.getNumberOfSyntaxErrors() > 0) {
			throw new JKindException("Error parsing SMTInterpol output: " + string);
		}
				
		return ModelExtractor.getModel(ctx, varTypes);
	}

	@Override
	public void push() {
		script.push(1);
	}

	@Override
	public void pop() {
		script.pop(1);
	}

	@Override
	public void comment(String str) {
		script.echo(new QuotedObject(str));
	}

	@Override
	public void stop() {
	}

	private Sort getSort(Type type) {
		return SmtInterpolUtil.getSort(script, type);
	}

	private Term convert(TermVariable[] params, Sexp sexp) {
		return SmtInterpolUtil.convert(script, params, sexp);
	}

	private Term convert(Sexp sexp) {
		return SmtInterpolUtil.convert(script, sexp);
	}

	@Override
	protected void declareImplemented(Function function) {
		SmtInterpolUtil.decalreFunction(function, script);
	}

}
