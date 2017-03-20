package jkind.analysis.evaluation;

import java.util.List;

import jkind.lustre.Equation;
import jkind.lustre.Function;
import jkind.lustre.FunctionCallExpr;
import jkind.lustre.IdExpr;
import jkind.lustre.Node;
import jkind.lustre.values.Value;
import jkind.solvers.smtlib2.SmtLib2Solver;
import jkind.util.StreamIndex;

public class SmtLibFunctionEvaluator extends FunctionEvaluator {

	private final SmtLib2Solver solver;
	public SmtLibFunctionEvaluator(SmtLib2Solver solver, Node node, List<Function> functions, int length) {
		super(node, functions, length);
		this.solver = solver;
	}

	@Override
	public Value visit(IdExpr e) {
		Equation eq = idToEqMap.get(e);
		if (eq != null) {
			return eq.expr.accept(this);
		}
		String name = "$" + e.id + StreamIndex.getSuffix(currentDepth);
		
		return null;
	}

	@Override
	public Value visit(FunctionCallExpr e) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private String eval(String strExpr){
		solver.send("(eval ("+strExpr+"))");
	}

}
