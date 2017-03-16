package jkind.solvers;

import java.util.ArrayList;
import java.util.List;

public class FunctionModel extends SimpleModel {
	private List<String> funcImplementations = new ArrayList<>();

	public FunctionModel() {
	}

	public FunctionModel(Model model) {
		super(model);
		if (model instanceof FunctionModel) {
			if (((FunctionModel) model).getFuncImplementations().size() > 0) {
				addImplementation(((FunctionModel) model).getFuncImplementations().get(0));
			}
		}
	}

	public void addImplementation(String funcImplementation) {
		funcImplementations.add(funcImplementation);
	}

	public List<String> getFuncImplementations() {
		return funcImplementations;
	}

}
