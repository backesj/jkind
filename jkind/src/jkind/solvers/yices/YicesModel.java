package jkind.solvers.yices;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jkind.lustre.Type;
import jkind.lustre.values.FunctionValue;
import jkind.lustre.values.Value;
import jkind.sexp.Symbol;
import jkind.solvers.Model;
import jkind.util.StreamIndex;
import jkind.util.Util;

public class YicesModel extends Model {
	private final Map<String, String> aliases = new HashMap<>();
	private final Map<String, Value> values = new HashMap<>();

	public YicesModel(Map<String, Type> varTypes) {
		super(varTypes);
	}

	public void addAlias(String from, String to) {
		aliases.put(from, to);
	}

	public void addValue(String name, Value value) {
		values.put(name, value);
	}

	private String getAlias(String name) {
		String result = name;
		while (aliases.containsKey(result)) {
			result = aliases.get(result);
		}
		return result;
	}

	@Override
	public Value getValue(String name) {
		String alias = getAlias(name);
		Value value = values.get(alias);
		if (value == null) {
			Type type = varTypes.get(name);
			if(type == null){
				return null;
			}
			return Util.getDefaultValue(type);
		} else {
			if (StreamIndex.isFunctionEncoded(alias) && ! (value instanceof FunctionValue) ) {
				return new FunctionValue(name, new Symbol(value.toString()));
			}
			return value;
		}
	}

	@Override
	public Set<String> getVariableNames() {
		Set<String> result = new HashSet<>();
		result.addAll(aliases.keySet());
		result.addAll(values.keySet());
		return result;
	}
}
