package jkind.translation;

import java.util.List;
import java.util.Map;

import jkind.lustre.Function;
import jkind.lustre.Node;
import jkind.lustre.Type;
import jkind.slicing.DependencyMap;
import jkind.slicing.LustreSlicer;
import jkind.util.Util;

public class Specification {
	public final Node node;
	public final DependencyMap dependencyMap;
	public final Map<String, Type> typeMap;
	public final List<Function> functions;
	private Relation transitionRelation;
	private Relation ivcTransitionRelation;

	public Specification(Node raw, List<Function> functions, boolean slicing, boolean assertionSlicing) {
		if (slicing) {
			this.dependencyMap = new DependencyMap(raw, raw.properties, assertionSlicing);
		} else {
			this.dependencyMap = DependencyMap.full(raw);
		}
		this.node = LustreSlicer.slice(raw, dependencyMap);
		this.typeMap = Util.getTypeMap(node);
		this.functions = Util.safeList(functions);
	}
	
	public Specification(Node raw) {
		this(raw, null, false, false);
	}

	public List<Function> getFunctions(){
		return functions;
	}
	
	public Relation getTransitionRelation() {
		if (transitionRelation == null) {
			transitionRelation = Lustre2Sexp.constructTransitionRelation(node);
		}
		return transitionRelation;
	}

	public Relation getIvcTransitionRelation() {
		if (ivcTransitionRelation == null) {
			ivcTransitionRelation = Lustre2Sexp.constructIvcTransitionRelation(node);
		}
		return ivcTransitionRelation;
	}
}
