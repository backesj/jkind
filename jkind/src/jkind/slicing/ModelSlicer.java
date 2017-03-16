package jkind.slicing;

import jkind.solvers.FunctionModel;
import jkind.solvers.Model;
import jkind.solvers.SimpleModel;
import jkind.util.StreamIndex;

public class ModelSlicer {
	public static SimpleModel slice(Model original, DependencySet keep) {
		FunctionModel sliced = new FunctionModel();
		for (String var : original.getVariableNames()) {
			StreamIndex si = StreamIndex.decode(var);
			if (si != null && keep.contains(si.getStream())) {
				sliced.putValue(si, original.getValue(var));
			}
		}
		if(original instanceof FunctionModel){
			sliced.addImplementation(((FunctionModel) original).getFuncImplementations().get(0));
		}
		return sliced;
	}
}
