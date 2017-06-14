package jkind.util;

import java.util.List;

import jkind.lustre.values.Value;
import jkind.util.Util;

public class FunctionTableRow {

	private final List<Value> inputs;
	private final List<Value> outputs;

	public FunctionTableRow(List<Value> inputs, List<Value> outputs) {
		this.inputs = Util.safeList(inputs);
		this.outputs = outputs;
	}

	public List<Value> getInputs() {
		return inputs;
	}

	public List<Value> getOutputs() {
		return outputs;
	}

	@Override
	public int hashCode() {
		return inputs.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof FunctionTableRow) {
			FunctionTableRow row = (FunctionTableRow) o;
			return row.inputs.equals(inputs);
		}
		return false;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for(Value val : inputs){
			sb.append(String.format("%-10s", val.toString() + " "));
		}
		sb.append("|");
		for(Value val: outputs) {
			sb.append(" " + String.format("%-10s", val.toString()));
		}
		return sb.toString();
	}

}
