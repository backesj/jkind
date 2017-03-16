package jkind.translation;

import java.util.HashSet;
import java.util.Set;

import jkind.lustre.Expr;
import jkind.lustre.Function;
import jkind.lustre.FunctionCallExpr;
import jkind.lustre.NodeCallExpr;
import jkind.lustre.Program;
import jkind.lustre.visitors.AstMapVisitor;

public class ConvertNodeCallsToFunctionCalls extends AstMapVisitor {
	
	private final Set<String> functionNames;

	public static Program program(Program program) {
		Set<String> names = new HashSet<>();
		for(Function function : program.functions){
			names.add(function.id);
		}
		return new ConvertNodeCallsToFunctionCalls(names).visit(program);
	}

	private ConvertNodeCallsToFunctionCalls(Set<String> functionNames) {
		this.functionNames = functionNames;
	};
	
	@Override
	public Expr visit(NodeCallExpr e) {
		if(functionNames.contains(e.node)){
			return new FunctionCallExpr(e.location, e.node, visitExprs(e.args));
		}
		return new NodeCallExpr(e.location, e.node, visitExprs(e.args));
	}
	

}
