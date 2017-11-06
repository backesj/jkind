package jkind.engines;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jkind.JKindException;
import jkind.SolverOption;
import jkind.analysis.LinearChecker;
import jkind.analysis.YicesArithOnlyCheck;
import jkind.lustre.Function;
import jkind.lustre.Node;
import jkind.lustre.builders.NodeBuilder;
import jkind.solvers.Solver;
import jkind.solvers.cvc4.Cvc4Solver;
import jkind.solvers.mathsat.MathSatSolver;
import jkind.solvers.smtinterpol.SmtInterpolSolver;
import jkind.solvers.yices.YicesSolver;
import jkind.solvers.yices2.Yices2Solver;
import jkind.solvers.z3.Z3Solver;

public class SolverUtil {
	public static Solver getSolver(SolverOption solverOption, String scratchBase, Node node, boolean UFsupport) {
		switch (solverOption) {
		case YICES:
			boolean arithOnly = YicesArithOnlyCheck.check(node) && !UFsupport;
			return new YicesSolver(scratchBase, arithOnly);
		case CVC4:
			return new Cvc4Solver(scratchBase);
		case Z3:
			return new Z3Solver(scratchBase, LinearChecker.isLinear(node));
		case YICES2:
			return new Yices2Solver(scratchBase);
		case MATHSAT:
			return new MathSatSolver(scratchBase);
		case SMTINTERPOL:
			return new SmtInterpolSolver(scratchBase);
		}
		throw new IllegalArgumentException("Unknown solver: " + solverOption);
	}

	public static boolean solverIsAvailable(SolverOption solverOption, boolean UFsupport) {
		try {
			Node emptyNode = new NodeBuilder("empty").build();
			getSolver(solverOption, null, emptyNode, UFsupport);
		} catch (JKindException e) {
			return false;
		} catch (NullPointerException e) {
			e.printStackTrace();
			throw e;
		}
		return true;
	}

	public static List<SolverOption> availableSolvers(boolean UFsupport) {
		return Arrays.stream(SolverOption.values()).filter(x -> solverIsAvailable(x,UFsupport))
				.collect(Collectors.toList());
	}

}
