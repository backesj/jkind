package jkind.engines;

import jkind.SolverOption;
import jkind.analysis.LinearChecker;
import jkind.analysis.YicesArithOnlyCheck;
import jkind.lustre.Node;
import jkind.solvers.Solver;
import jkind.solvers.cvc4.Cvc4MultiSolver;
import jkind.solvers.cvc4.Cvc4Solver;
import jkind.solvers.mathsat.MathSatSolver;
import jkind.solvers.smtinterpol.SmtInterpolSolver;
import jkind.solvers.yices.YicesSolver;
import jkind.solvers.yices2.Yices2Solver;
import jkind.solvers.z3.Z3Solver;
import jkind.translation.InductiveDataTypeSpecification;
import jkind.translation.Specification;

public class SolverUtil {
	public static Solver getSolver(SolverOption solverOption, String scratchBase, Specification spec) {
		switch (solverOption) {
		case YICES:
			return new YicesSolver(scratchBase, YicesArithOnlyCheck.check(spec.node));
		case CVC4:
		    if(spec instanceof InductiveDataTypeSpecification){
		        return new Cvc4MultiSolver(scratchBase);
		    }
			return new Cvc4Solver(scratchBase);
		case Z3:
			return new Z3Solver(scratchBase, LinearChecker.isLinear(spec.node));
		case YICES2:
			return new Yices2Solver(scratchBase);
		case MATHSAT:
			return new MathSatSolver(scratchBase);
		case SMTINTERPOL:
			return new SmtInterpolSolver(scratchBase);
		}
		throw new IllegalArgumentException("Unknown solver: " + solverOption);
	}
}
