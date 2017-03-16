package jkind.lustre;

import java.util.List;

import jkind.Assert;
import jkind.lustre.visitors.AstVisitor;
import jkind.util.Util;

public class Function extends Ast {
	public final String id;
	public final List<VarDecl> inputs;
	public final List<VarDecl> outputs;
	
	public Function(Location location, String id, List<VarDecl> inputs, List<VarDecl> outputs) {
		super(location);
		Assert.isNotNull(id);
		this.id = id;
		this.inputs = Util.safeList(inputs);
		this.outputs = Util.safeList(outputs);
		if(outputs.size() != 1){
			throw new IllegalArgumentException("we currently only support functions with a single output");
		}
	}
	
	public Function(String id, List<VarDecl> inputs, List<VarDecl> outputs) {
		this(Location.NULL, id, inputs, outputs);
	}

	@Override
	public <T, S extends T> T accept(AstVisitor<T, S> visitor) {
		return visitor.visit(this);
	}
	
	@Override
	public int hashCode(){
		return this.id.hashCode();
	}
	
	@Override
	public boolean equals(Object o){
		if(!(o instanceof Function)){
			return false;
		}
		return this.id.equals(((Function)o).id);
	}

}
