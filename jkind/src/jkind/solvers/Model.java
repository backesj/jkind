package jkind.solvers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jkind.lustre.Type;
import jkind.lustre.values.Value;
import jkind.util.FunctionTable;
import jkind.util.StreamIndex;

public abstract class Model {
	protected Map<String, Type> varTypes;
	private List<FunctionTable> funcImplementations = new ArrayList<>();
	
	public Model(Map<String, Type> varTypes) {
		this.varTypes = Collections.unmodifiableMap(new HashMap<>(varTypes));
	}
	
	public abstract Value getValue(String name);
	public abstract Set<String> getVariableNames();

	public Value getValue(StreamIndex si) {
		return getValue(si.getEncoded().str);
	}
	
	public void addImplementation(List<FunctionTable> funcImplementations) {
		this.funcImplementations.addAll(funcImplementations);
	}

	public void addImplementation(FunctionTable funcImplementation) {
		this.funcImplementations.add(funcImplementation);
	}

	public List<FunctionTable> getFuncImplementations() {
		return funcImplementations;
	}
}
