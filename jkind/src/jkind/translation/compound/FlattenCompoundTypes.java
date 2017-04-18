package jkind.translation.compound;

import java.util.List;

import jkind.lustre.Function;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.builders.ProgramBuilder;

/**
 * Flatten arrays and records to scalars
 * 
 * Assumption: All node calls have been inlined.
 */
public class FlattenCompoundTypes {
	public static Program program(Program program) {
		Node node = program.getMainNode();
		List<Function> funcs = program.functions;
		node = RemoveNonConstantArrayIndices.node(node, funcs);
		node = RemoveArrayUpdates.node(node, funcs);
		node = RemoveRecordUpdates.node(node, funcs);
		node = FlattenCompoundComparisons.node(node, funcs);
		node = FlattenCompoundVariables.node(node);
		program = new ProgramBuilder(program).clearNodes().addNode(node).build();
		program = FlattenCompoundExpressions.program(program);
		return program;
	}
}
