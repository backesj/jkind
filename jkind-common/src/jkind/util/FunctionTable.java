package jkind.util;

import java.util.HashSet;
import java.util.Set;

public class FunctionTable {

	private final String funcName;
	private final Set<FunctionTableRow> rows = new HashSet<>();

	public FunctionTable(String funcName) {
		this.funcName = funcName;
	}

	public void addRow(FunctionTableRow row) {
		rows.add(row);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Function: ");
		sb.append(funcName);
		sb.append("\n");
		for (FunctionTableRow row : rows) {
			sb.append(row);
			sb.append("\n");
		}
		return sb.toString();
	}

}
