package jkind.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import jkind.lustre.VarDecl;

public class FunctionTable {

	public final String funcName;
	public final Set<FunctionTableRow> rows = new HashSet<>();
	public final List<VarDecl> inputs;
	public final List<VarDecl> outputs;

	public FunctionTable(String funcName, List<VarDecl> inputs, List<VarDecl> outputs) {
		this.funcName = funcName;
		this.inputs = inputs;
		this.outputs = outputs;
	}

	public void addRow(FunctionTableRow row) {
		rows.add(row);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(StringUtils.repeat("-", (inputs.size()+1)*10));
		sb.append("\n");
		for(VarDecl input : inputs){
			sb.append(String.format("%-10s", input.id +" "));
		}
		sb.append("| ");
		sb.append(funcName);
		sb.append("\n");
		sb.append(StringUtils.repeat("-", (inputs.size()+1)*10));
		sb.append("\n");
		for (FunctionTableRow row : rows) {
			sb.append(row);
			sb.append("\n");
		}
		return sb.toString();
	}

}
