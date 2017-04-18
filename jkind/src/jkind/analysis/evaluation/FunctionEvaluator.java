package jkind.analysis.evaluation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import de.uni_freiburg.informatik.ultimate.logic.Model;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.Function;
import jkind.lustre.FunctionCallExpr;
import jkind.lustre.IdExpr;
import jkind.lustre.Node;
import jkind.lustre.Type;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;
import jkind.lustre.values.BooleanValue;
import jkind.lustre.values.IntegerValue;
import jkind.lustre.values.RealValue;
import jkind.lustre.values.Value;
import jkind.solvers.smtinterpol.SmtInterpolUtil;
import jkind.translation.FlattenFunctionsWithRecords;
import jkind.util.FunctionTable;
import jkind.util.FunctionTableRow;
import jkind.util.StreamIndex;

public abstract class FunctionEvaluator extends Evaluator {

	protected final Node node;
	protected final int length;
	private final Map<String, FunctionTable> funcTables = new HashMap<>();
	private final Map<String, Function> lustreFuncDefs = new HashMap<>();
	protected int currentDepth;
	protected final Map<IdExpr, Equation> idToEqMap = new HashMap<>();
	protected final Map<String, Type> idToTypeMap = new HashMap<>();
	private final List<Function> funcs;

	public FunctionEvaluator(Node node, List<Function> funcs, int length) {
		this.node = node;
		this.length = length;
		this.funcs = funcs;

		for (Equation eq : node.equations) {
			if (eq.lhs.size() != 1) {
				throw new IllegalArgumentException("We assume single variable equations");
			}
			idToEqMap.put(eq.lhs.get(0), eq);
		}

		for (VarDecl var : node.inputs) {
			idToTypeMap.put(var.id, var.type);
		}
		for (VarDecl var : node.outputs) {
			idToTypeMap.put(var.id, var.type);
		}
		for (VarDecl var : node.locals) {
			idToTypeMap.put(var.id, var.type);
		}

		for (Function func : funcs) {
			lustreFuncDefs.put(func.id, func);
		}

	}

	public List<FunctionTable> evaluateFuncs() {
		currentDepth = length;
		// TODO: we could memoize function calls at earlier depths to speed
		// things up

		while (currentDepth >= 0) {
			for (Equation eq : node.equations) {
				eq.expr.accept(this);
			}
			for (Expr expr : node.assertions) {
				expr.accept(this);
			}
			currentDepth--;
		}

		List<FunctionTable> tables = new ArrayList<>();
		for (Entry<String, FunctionTable> entry : funcTables.entrySet()) {
			tables.add(entry.getValue());
		}

		return tables;
	}

	@Override
	public Value visit(UnaryExpr e) {
		if (e.op.equals(UnaryOp.PRE)) {
			currentDepth--;
			Value val = e.expr.accept(this);
			currentDepth++;
			return val;
		}
		return super.visit(e);
	}

	protected void addFuncRow(String funcName, List<Value> inputVals, Value outputVal) {
		FunctionTable table = funcTables.get(funcName);

		if (table == null) {
			Function func = lustreFuncDefs.get(funcName);
			List<String> inputs = func.inputs.stream().map(v -> v.id).collect(Collectors.toList());
			table = new FunctionTable(funcName.replace(FlattenFunctionsWithRecords.functionRecordDelimeter, "."),
					inputs);
			funcTables.put(funcName, table);
		}

		FunctionTableRow row = new FunctionTableRow(inputVals, outputVal);
		table.addRow(row);
	}

}
