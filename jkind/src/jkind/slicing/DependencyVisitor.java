package jkind.slicing;

import java.util.HashSet;
import java.util.Set;

import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.QuantExpr;
import jkind.lustre.VarDecl;
import jkind.lustre.visitors.ExprIterVisitor;

public class DependencyVisitor extends ExprIterVisitor {
	
	private Set<String> quantVarNames = new HashSet<>();
	
	public static DependencySet get(Expr expr) {
		DependencyVisitor visitor = new DependencyVisitor();
		expr.accept(visitor);
		return visitor.set;
	}
	
	private DependencySet set = new DependencySet();
	
	@Override
	public Void visit(IdExpr e) {
		if (!quantVarNames.contains(e.id)) {
			set.add(e.id);
		}
		return null;
	}
	
	@Override
	public Void visit(QuantExpr e) {
		Set<String> names = new HashSet<>();
		for(VarDecl var : e.boundVars){
			names.add(var.id);
		}
		quantVarNames.addAll(names);
		e.expr.accept(this);
		quantVarNames.removeAll(names);
		return null;
	}
}
