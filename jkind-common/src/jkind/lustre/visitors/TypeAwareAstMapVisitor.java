package jkind.lustre.visitors;

import java.util.List;

import jkind.lustre.Expr;
import jkind.lustre.Function;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.Type;

public class TypeAwareAstMapVisitor extends AstMapVisitor {
	protected TypeReconstructor typeReconstructor;
	
	public TypeAwareAstMapVisitor(List<Function> funcs){
		typeReconstructor = new TypeReconstructor(funcs);
	}

	protected Type getType(Expr e) {
		return e.accept(typeReconstructor);
	}

	@Override
	public Program visit(Program e) {
		typeReconstructor = new TypeReconstructor(e);
		return super.visit(e);
	}

	@Override
	public Node visit(Node e) {
		typeReconstructor.setNodeContext(e);
		return super.visit(e);
	}
}
