package jkind.solvers.yices;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jkind.JKindException;
import jkind.lustre.Type;
import jkind.lustre.values.FunctionValue;
import jkind.lustre.values.Value;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.sexp.Symbol;
import jkind.solvers.Result;
import jkind.solvers.SatResult;
import jkind.solvers.UnsatResult;
import jkind.solvers.mathsat.MathSatParser.BodyContext;
import jkind.solvers.mathsat.MathSatParser.ConsBodyContext;
import jkind.solvers.mathsat.MathSatParser.FnAppContext;
import jkind.solvers.mathsat.MathSatParser.IdContext;
import jkind.solvers.mathsat.MathSatParser.SymbolBodyContext;
import jkind.solvers.smtlib2.Quoting;
import jkind.solvers.yices.YicesParser.AliasContext;
import jkind.solvers.yices.YicesParser.FunctionContext;
import jkind.solvers.yices.YicesParser.NumericContext;
import jkind.solvers.yices.YicesParser.SatResultContext;
import jkind.solvers.yices.YicesParser.UnsatCoreContext;
import jkind.solvers.yices.YicesParser.UnsatResultContext;
import jkind.solvers.yices.YicesParser.ValueContext;
import jkind.solvers.yices.YicesParser.VariableContext;
import jkind.util.Util;

import org.antlr.v4.runtime.tree.TerminalNode;

public class ResultExtractorListener extends YicesBaseListener {
	private Result result;
	private YicesModel model;
	private List<Symbol> unsatCore;
	private final Map<String, Type> varTypes;
	private static final String fnArgPrefix = "x";

	public ResultExtractorListener(Map<String, Type> varTypes) {
		this.varTypes = varTypes;
	}

	public Result getResult() {
		return result;
	}

	@Override
	public void enterSatResult(SatResultContext ctx) {
		model = new YicesModel(varTypes);
		result = new SatResult(model);
	}

	@Override
	public void enterUnsatResult(UnsatResultContext ctx) {
		unsatCore = new ArrayList<>();
	}

	@Override
	public void enterUnsatCore(UnsatCoreContext ctx) {
		for (TerminalNode node : ctx.INT()) {
			unsatCore.add(new Symbol(node.getText()));
		}
	}

	@Override
	public void exitUnsatResult(UnsatResultContext ctx) {
		result = new UnsatResult(unsatCore);
	}

	@Override
	public void enterAlias(AliasContext ctx) {
		model.addAlias(ctx.ID(0).getText(), ctx.ID(1).getText());
	}

	@Override
	public void enterVariable(VariableContext ctx) {
		String var = ctx.ID().getText();
		Type type = varTypes.get(var);
		if (type != null) {
			model.addValue(var, Util.parseValue(type, ctx.value().getText()));
		}
	}

	@Override
	public void enterFunction(FunctionContext ctx) {
		String id = ctx.ID().getText();

		Value fnDef = model.getValue(id);

		List<Sexp> argDefs = new ArrayList<>();
		List<Sexp> argVals = new ArrayList<>();

		for (int i = 0; i < ctx.value().size() - 1; i++) {
			ValueContext val = ctx.value(i);
			Sexp bodySexp = sexp(val);
			argVals.add(bodySexp);
			argDefs.add(new Cons(fnArgPrefix + i, sexpType(bodySexp)));
		}
		Sexp bodyVal = sexp(ctx.value(ctx.value().size()-1));
		Sexp args = new Cons(argDefs);
		Sexp fnBody = null;
		if (fnDef == null) {
			fnBody = bodyVal;
		} else {
			Cons fnDefSexp = ((Cons) ((FunctionValue) fnDef).getBody());
			fnBody = fnDefSexp.args.get(0);
		}

		fnBody = fnValsToITE(argVals, bodyVal, fnBody);
		bodyVal = new Cons(args, fnBody);

		model.addValue(id, new FunctionValue(id, bodyVal));
	}

	private static Sexp fnValsToITE(List<Sexp> inputVals, Sexp outputVal, Sexp elseExpr) {
		Sexp ands = null;
		int i = 0;
		for (Sexp inputVal : inputVals) {
			Sexp eq = eq(new Symbol(fnArgPrefix + i++), inputVal);
			if (ands == null) {
				ands = eq;
			} else {
				ands = and(ands, eq);
			}
		}
		return new Cons("ite", ands, outputVal, elseExpr);
	}

	private static Sexp and(Sexp a, Sexp b) {
		return new Cons("and", a, b);
	}

	private static Sexp eq(Sexp a, Sexp b) {
		return new Cons("=", a, b);
	}

	private static Symbol sexpType(Sexp sexp) {
			String str = sexp.toString();

			if (str.equals("true") || str.equals("false")) {
				return new Symbol("Bool");
			}
			try {
				Integer.parseInt(str);
				return new Symbol("Int");
			} catch (NumberFormatException e) {
			}
			if(!str.contains("/")){
				throw new JKindException("Parsed unexpected value: '" + str + "' from MathSAT result");
			}
			return new Symbol("Real");
	}

	private static String getId(IdContext id) {
		return Quoting.unquote(id.getText());
	}

	private static Sexp sexp(ValueContext ctx) {
		NumericContext numeric = ctx.numeric();
		TerminalNode bool = ctx.BOOL();
		Sexp value = null;
		if (numeric != null) {
			List<TerminalNode> ints = numeric.INT();
			if (ints.size() == 1) {
				value = new Symbol(ints.get(0).getText());
			} else if (ints.size() == 2) {
				value = new Cons("/", new Symbol(ints.get(0).getText()), new Symbol(ints.get(1).getText()));
			} else {
				throw new IllegalArgumentException("Unexpected number of terminals in NumericContext");
			}
			if (numeric.getText().contains("-")) {
				return new Cons("-", value);
			}
			return value;
		}
		if(bool != null){
			return new Symbol(bool.getText());
		}
		throw new IllegalArgumentException();
	}

}
