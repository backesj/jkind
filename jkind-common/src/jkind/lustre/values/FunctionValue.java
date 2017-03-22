package jkind.lustre.values;

import jkind.lustre.BinaryOp;
import jkind.lustre.UnaryOp;
import jkind.sexp.Sexp;

public class FunctionValue extends Value{
	
	private final Sexp body;
	private final String name;
	public FunctionValue(String name, Sexp body){
		this.body = body;
		this.name = name;
	}
	
	public String getName(){
		return name;
	}
	
	public Sexp getBody(){
		return body;
	}

	@Override
	public Value applyBinaryOp(BinaryOp op, Value right) {
		throw new IllegalArgumentException("It is not expected to use a function this way");
	}

	@Override
	public Value applyUnaryOp(UnaryOp op) {
		throw new IllegalArgumentException("It is not expected to use a function this way");
	}

}
