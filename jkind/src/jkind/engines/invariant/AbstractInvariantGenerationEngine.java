package jkind.engines.invariant;

import java.util.ArrayList;
import java.util.List;

import jkind.JKindSettings;
import jkind.engines.Director;
import jkind.engines.SolverBasedEngine;
import jkind.engines.StopException;
import jkind.engines.messages.BaseStepMessage;
import jkind.engines.messages.InductiveCounterexampleMessage;
import jkind.engines.messages.InvalidMessage;
import jkind.engines.messages.InvariantMessage;
import jkind.engines.messages.Itinerary;
import jkind.engines.messages.UnknownMessage;
import jkind.engines.messages.ValidMessage;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.solvers.Model;
import jkind.solvers.Result;
import jkind.solvers.SatResult;
import jkind.solvers.UnknownResult;
import jkind.translation.Specification;
import jkind.util.SexpUtil;

public abstract class AbstractInvariantGenerationEngine extends SolverBasedEngine {
	public AbstractInvariantGenerationEngine(String name, Specification spec,
			JKindSettings settings, Director director) {
		super(name, spec, settings, director);
	}

	@Override
	public void main() {
		try {
			Invariant invariant = createInitialInvariant();
			if (invariant.isTrivial()) {
				comment("No invariants proposed");
				return;
			}

			createVariables(-1);
			createVariables(0);
			for (int k = 1; k <= settings.n; k++) {
				comment("K = " + k);

				refineBaseStep(k - 1, invariant);
				if (invariant.isTrivial()) {
					comment("No invariants remaining after base step");
					return;
				}

				createVariables(k);
				refineInductiveStep(k, invariant);
			}
		} catch (StopException se) {
		}
	}

	protected abstract Invariant createInitialInvariant();

	private void refineBaseStep(int k, Invariant invariant) {
		solver.push();
		Result result;

		for (int i = 0; i <= k; i++) {
			assertBaseTransition(i);
		}

		do {
			checkForStop();

			Sexp query = SexpUtil.conjoinInvariants(invariant.toExprs(), k);
			result = solver.query(query);

			if (result instanceof SatResult) {
				Model model = ((SatResult) result).getModel();
				invariant.refine(model, k);
				comment("Finished single base step refinement");
			} else if (result instanceof UnknownResult) {
				throw new StopException();
			}
		} while (!invariant.isTrivial() && result instanceof SatResult);

		solver.pop();
	}

	private void refineInductiveStep(int k, Invariant original) {
		solver.push();
		Invariant invariant = original.copy();
		Result result;

		for (int i = 0; i <= k; i++) {
			assertInductiveTransition(i);
		}

		do {
			checkForStop();

			result = solver.query(getInductiveQuery(k, invariant));

			if (result instanceof SatResult) {
				Model model = ((SatResult) result).getModel();
				invariant.refine(model, k);
				comment("Finished single inductive step refinement");
			}
		} while (!invariant.isTrivial() && result instanceof SatResult);

		solver.pop();

		sendValidAndInvariant(invariant, k);
		original.reduceProven(invariant);
		return;
	}

	private void checkForStop() {
		processMessages();
		if (properties.isEmpty()) {
			throw new StopException();
		}
	}

	private Sexp getInductiveQuery(int k, Invariant invariant) {
		List<Expr> exprs = invariant.toExprs();
		List<Sexp> hyps = new ArrayList<>();
		for (int i = 0; i < k; i++) {
			hyps.add(SexpUtil.conjoinInvariants(exprs, i));
		}
		Sexp conc = SexpUtil.conjoinInvariants(exprs, k);

		return new Cons("=>", SexpUtil.and(hyps), conc);
	}

	private void sendValidAndInvariant(Invariant invariant, int k) {
		List<Expr> invs = invariant.toFinalInvariants();
		comment("Sending invariants:");
		for (Expr inv : invs) {
			comment("  " + inv);
		}

		sendValidProperties(invs, k);
		director.broadcast(new InvariantMessage(invs));
	}

	private void sendValidProperties(List<Expr> invs, int k) {
		List<String> valid = new ArrayList<>();
		for (Expr inv : invs) {
			if (inv instanceof IdExpr) {
				IdExpr idExpr = (IdExpr) inv;
				if (properties.contains(idExpr.id)) {
					valid.add(idExpr.id);
				}
			}
		}

		if (!valid.isEmpty()) {
			Itinerary itinerary = director.getValidMessageItinerary();
			director.broadcast(new ValidMessage(getName(), valid, k, invs, itinerary));
		}
	}

	@Override
	protected void handleMessage(BaseStepMessage bsm) {
	}

	@Override
	protected void handleMessage(InductiveCounterexampleMessage icm) {
	}

	@Override
	protected void handleMessage(InvalidMessage im) {
		properties.removeAll(im.invalid);
	}

	@Override
	protected void handleMessage(InvariantMessage im) {
	}

	@Override
	protected void handleMessage(UnknownMessage um) {
		properties.removeAll(um.unknown);
	}

	@Override
	protected void handleMessage(ValidMessage vm) {
		properties.removeAll(vm.valid);
	}
}