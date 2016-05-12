package jkind;

import jkind.analysis.LinearChecker;
import jkind.analysis.StaticAnalyzer;
import jkind.engines.Director;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.slicing.DependencyMap;
import jkind.slicing.LustreSlicer;
import jkind.translation.InductiveDataTypeSpecification;
import jkind.translation.InlineSimpleEquations;
import jkind.translation.Specification;
import jkind.translation.Translate;
import jkind.util.Util;

public class JKind {
	public static void main(String[] args) {
		try {
			JKindSettings settings = JKindArgumentParser.parse(args);
			Program program = Main.parseLustre(settings.filename);

			StaticAnalyzer.check(program, settings.solver);
			if (!LinearChecker.isLinear(program)) {
				if (settings.pdrMax > 0) {
					Output.warning("disabling PDR due to non-linearities");
					settings.pdrMax = 0;
				}
			}

			program = Translate.translateProgram(program);
			Node main = program.getMainNode();
			DependencyMap dependencyMap = new DependencyMap(main, main.properties);
			main = LustreSlicer.slice(main, dependencyMap);
			//kind of hacky, but we need a way for the specification to contain
			//recursive functions if they are defined
			Specification userSpec;
            if (Util.containsInductDataTypes(program)) {
                userSpec = new InductiveDataTypeSpecification(main, program);
            } else {
                userSpec = new Specification(main);
            }
            Specification analysisSpec;
            if (userSpec instanceof InductiveDataTypeSpecification) {
                analysisSpec = userSpec;
            } else {
                analysisSpec = getAnalysisSpec(userSpec, settings);
            }
			int exitCode = new Director(settings, userSpec, analysisSpec).run();
			System.exit(exitCode); // Kills all threads
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(ExitCodes.UNCAUGHT_EXCEPTION);
		}
	}

	private static Specification getAnalysisSpec(Specification userSpec, JKindSettings settings) {
		if (settings.inline) {
			Node inlined = InlineSimpleEquations.node(userSpec.node);
			return new Specification(inlined);
		} else {
			return userSpec;
		}
	}
}
