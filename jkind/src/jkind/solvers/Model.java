package jkind.solvers;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jkind.lustre.Type;
import jkind.lustre.values.Value;
import jkind.util.StreamIndex;

public abstract class Model {
    protected Map<String, Type> varTypes;
    private final Set<String> typeConstructors = new HashSet<>();

    public Model(Map<String, Type> varTypes) {
        this.varTypes = Collections.unmodifiableMap(new HashMap<>(varTypes));
    }

    public void addTypeConstructor(String name) {
        typeConstructors.add(name);
    }

    public boolean isTypeConstructor(String name) {
        return typeConstructors.contains(name);
    }

    public abstract Value getValue(String name);

    public abstract Set<String> getVariableNames();

    public Value getValue(StreamIndex si) {
		return getValue(si.getEncoded().str);
	}
}
