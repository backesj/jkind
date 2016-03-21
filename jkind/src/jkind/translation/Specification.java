package jkind.translation;

import java.util.Map;

import jkind.lustre.InductType;
import jkind.lustre.Node;
import jkind.lustre.Type;
import jkind.slicing.DependencyMap;
import jkind.slicing.LustreSlicer;
import jkind.util.Util;

public class Specification {
	public final Node node;
	public final DependencyMap dependencyMap;
	public final Map<String, Type> typeMap;
	private Relation transitionRelation;
	private Relation supportTransitionRelation;

	public Specification(Node raw) {
		this.dependencyMap = new DependencyMap(raw, raw.properties);
		if (this instanceof InductiveDataTypeSpecification) {
			// For InductiveDataTypeSpecifications we set all of the properties
			// to "prop=prop". This is because CVC4 does not like quantifiers
			// that are not specified at the top level. So we do not do slicing
			// for specifications containing inductive datatypes.
			this.node = raw;
		} else {
			this.node = LustreSlicer.slice(raw, dependencyMap);
		}
		this.typeMap = Util.getTypeMap(node);
	}

	public Relation getTransitionRelation() {
		if (transitionRelation == null) {
			transitionRelation = Lustre2Sexp.constructTransitionRelation(node);
		}
		return transitionRelation;
	}

	public Relation getSupportTransitionRelation() {
		if (supportTransitionRelation == null) {
			supportTransitionRelation = Lustre2Sexp.constructSupportTransitionRelation(node);
		}
		return supportTransitionRelation;
	}
	
}
