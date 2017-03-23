package jkind.solvers.mathsat;

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
import jkind.solvers.mathsat.MathSatParser.AssignmentContext;
import jkind.solvers.mathsat.MathSatParser.BodyContext;
import jkind.solvers.mathsat.MathSatParser.ConsBodyContext;
import jkind.solvers.mathsat.MathSatParser.FnAppContext;
import jkind.solvers.mathsat.MathSatParser.IdContext;
import jkind.solvers.mathsat.MathSatParser.ModelContext;
import jkind.solvers.mathsat.MathSatParser.SymbolBodyContext;
import jkind.solvers.smtlib2.Quoting;
import jkind.solvers.smtlib2.SmtLib2Model;

public class ModelExtractor {
	private static final String fnArgPrefix = "x";

	public static SmtLib2Model getModel(ModelContext ctx, Map<String, Type> varTypes) {
		SmtLib2Model model = new SmtLib2Model(varTypes);
		for (AssignmentContext assignCtx : ctx.assignment()) {
			walkAssign(assignCtx, model);
		}
		return model;
	}

	public static void walkAssign(AssignmentContext assignCtx, SmtLib2Model model) {
		IdContext id = assignCtx.id();
		Sexp bodyVal = sexp(assignCtx.body());
		String var = null;

		if (id != null) {
			var = getId(id);
		} else {
			// this is a value for a specific input to a function
			// we need to construct a sexp representation for this function
			// based on this particular evaluation and other evaluations
			// reported in the model. This sexp representation is executed later
			// to come up with a table reprsentation for the function. This is
			// really a hack to try to consistently represent models for
			// functions returned by the solver.

			FnAppContext fnApp = assignCtx.fnApp();
			List<BodyContext> symbols = fnApp.body();
			IdContext fnId = fnApp.id();

			var = fnId.getText();
			Value fnDef = null;
			fnDef = model.getValue(var);

			List<Sexp> argDefs = new ArrayList<>();
			List<Sexp> argVals = new ArrayList<>();
			
			int i = 0;
			for (BodyContext sym : symbols) {				
				Sexp bodySexp = sexp(sym);
				argVals.add(bodySexp);
				argDefs.add(new Cons(fnArgPrefix + i++, sexpType(bodySexp)));
			}
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

		}
		model.addValue(var, bodyVal);
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
		if (sexp instanceof Symbol) {
			String str = sexp.toString();

			if (str.equals("true") || str.equals("false")) {
				return new Symbol("Bool");
			}
			try {
				Integer.parseInt(str);
				return new Symbol("Int");
			} catch (NumberFormatException e) {
				throw new JKindException("Parsed unexpected value: '" + str + "' from MathSAT result");
			}
		} else {
			Cons cons = (Cons) sexp;
			if (cons.head.toString().equals("/") || cons.head.toString().equals("to_real")) {
				return new Symbol("Real");
			}
			return sexpType(cons.args.get(0));
		}
	}

	private static String getId(IdContext id) {
		return Quoting.unquote(id.getText());
	}

	private static Sexp sexp(BodyContext ctx) {
		if (ctx instanceof SymbolBodyContext) {
			SymbolBodyContext sbc = (SymbolBodyContext) ctx;
			return new Symbol(sbc.symbol().getText());
		} else if (ctx instanceof ConsBodyContext) {
			ConsBodyContext cbc = (ConsBodyContext) ctx;
			List<Sexp> args = new ArrayList<>();
			for (BodyContext sub : cbc.body()) {
				args.add(sexp(sub));
			}
			String fnText = cbc.fn().getText();
			if (fnText.equals("to_real")) {
				if (args.size() != 1) {
					throw new IllegalArgumentException(
							"we assumed that the 'to_real' function should only have one argument");
				}
				return new Cons("/", args.get(0), new Symbol("1"));
			} else {
				return new Cons(fnText, args);
			}
		} else {
			throw new IllegalArgumentException();
		}
	}
}
