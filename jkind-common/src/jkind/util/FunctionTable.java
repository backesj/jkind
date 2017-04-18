package jkind.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

public class FunctionTable {

	private final String funcName;
	private final Set<FunctionTableRow> rows = new HashSet<>();
	private final Collection<String> inputs;

	public FunctionTable(String funcName, Collection<String> inputs) {
		this.funcName = funcName;
		this.inputs = inputs;
	}

	public void addRow(FunctionTableRow row) {
		rows.add(row);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(StringUtils.repeat("-", (inputs.size()+1)*10));
		sb.append("\n");
		for(String input : inputs){
			sb.append(String.format("%-10s", input +" "));
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
