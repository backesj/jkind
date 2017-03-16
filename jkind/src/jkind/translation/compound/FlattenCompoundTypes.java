package jkind.translation.compound;

import java.util.List;

import jkind.lustre.Function;
import jkind.lustre.Node;

/**
 * Flatten arrays and records to scalars
 * 
 * Assumption: All node calls have been inlined.
 */
public class FlattenCompoundTypes {
	public static Node node(Node node, List<Function> funcs) {
		node = RemoveNonConstantArrayIndices.node(node, funcs);
		node = RemoveArrayUpdates.node(node, funcs);
		node = RemoveRecordUpdates.node(node, funcs);
		node = FlattenCompoundComparisons.node(node, funcs);
		node = FlattenCompoundVariables.node(node);
		node = FlattenCompoundExpressions.node(node);
		return node;
	}
}
