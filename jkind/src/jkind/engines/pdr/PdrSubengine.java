package jkind.engines.pdr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import jkind.JKindSettings;
import jkind.engines.Director;
import jkind.engines.StopException;
import jkind.engines.messages.InductiveCounterexampleMessage;
import jkind.advice.VariableUsageChecker;
import jkind.engines.invariant.InvariantSet;
import jkind.engines.messages.InvalidMessage;
import jkind.engines.messages.InvariantMessage;
import jkind.engines.messages.Itinerary;
import jkind.engines.messages.ValidMessage;
import jkind.engines.pdr.PdrSmt.Option;
import jkind.lustre.Expr;
import jkind.lustre.Node;
import jkind.lustre.builders.NodeBuilder;
import jkind.slicing.LustreSlicer;
import jkind.solvers.Model;
import jkind.translation.Specification;
import jkind.util.Util;
import de.uni_freiburg.informatik.ultimate.logic.Term;

/**
 * PDR algorithm based on
 * "Efficient implementation of property directed reachability" by Niklas Een,
 * Alan Mishchenko, and Robert Brayton
 * 
 * SMT extension based on
 * "IC3 Modulo Theories via Implicit Predicate Abstraction" by Alessandro
 * Cimatti, Alberto Griggio, Sergio Mover, and Stefano Tonetta
 */
public class PdrSubengine extends Thread {
	private final Node node;
	private final String prop;
	private final PdrEngine parent;
	private final Director director;

	private final List<Frame> F = new ArrayList<>();
	private final PdrSmt Z;

	private volatile boolean cancel = false;
	private final BlockingQueue<Expr> incomingInvariants = new LinkedBlockingQueue<>();
	private final InvariantSet invariants = new InvariantSet();
	private VariableUsageChecker variableUsageChecker;
	private final List<Expr> initialInvariants;

	public PdrSubengine(String prop, List<Expr> initialInvariants, Specification spec,
			String scratchBase, PdrEngine parent, Director director) {
		super("pdr-" + prop);
		this.prop = prop;
		this.initialInvariants = initialInvariants;
		Node single = new NodeBuilder(spec.node).clearProperties().addProperty(prop).build();
		this.node = LustreSlicer.slice(single, spec.dependencyMap);
		this.parent = parent;
		this.director = director;

		this.Z = new PdrSmt(node, F, prop, scratchBase);

		this.variableUsageChecker = new VariableUsageChecker(Util.getVarDecls(node));
	}

	private void tryAddInvariants(List<Expr> invs) {
		for (Expr inv : invs) {
			if (variableUsageChecker.check(inv)) {
				if (invariants.add(inv)) {
					Z.addInvariant(inv);
				}
			}
		}
	}

	public void recieveInvariants(List<Expr> invs) {
		for (Expr inv : invs) {
			incomingInvariants.add(inv);
		}
	}

	public void cancel() {
		cancel = true;
	}

	@Override
	public void run() {
        JKindSettings settings = parent.getSettings();
		Z.comment("Checking property: " + prop);

		// Create F_INF and F[0]
		F.add(new Frame());
		addFrame(Z.createInitialFrame());
		tryAddInvariants(initialInvariants);

		try {
			while (true) {
				Cube c = Z.getBadCube();
				if (c != null) {
					blockCube(new TCube(c, depth()));
				} else {
					addFrame(new Frame());
					Z.comment("Number of frames: " + F.size());
					List<Expr> invariants = propogateBlockedCubes();
					if (invariants != null) {
						sendValidAndInvariants(invariants);
						return;
					}
					if(settings.inductiveCounterexamples){
                        reportInductCex();
                    }
				}
			}
		} catch (CounterexampleException cex) {
			Z.comment("Found counterexample of length " + cex.getLength());
			sendInvalid(cex.getLength(), cex.getModel());
			return;
		} catch (StopException | OutOfMemoryError e) {
			parent.reportUnknown(prop);
			return;
		} catch (Throwable t) {
			parent.reportThrowable(t);
			return;
		}
	}

    private void blockCube(TCube s0) {
		PriorityQueue<TCube> Q = new PriorityQueue<>();
		Q.add(s0);

		while (!Q.isEmpty()) {
			checkCancel();
			checkNewInvariants();
			TCube s = Q.poll();

			if (s.getFrame() == 0) {
				Z.refine(getCubes(s.getCube()));
				Z.comment("Refined abstraction");
				return;
			}

			if (!isBlocked(s)) {
				assert (!Z.isInitial(s.getCube()));
				TCube z = Z.solveRelative(s, Option.EXTRACT_MODEL);
				if (z.getFrame() != TCube.FRAME_NULL) {
					// Cube 's' was blocked by image of predecessor
					generalize(z);

					// Push z as far forward as possible
					while (z.getFrame() < depth() - 1) {
						TCube nz = Z.solveRelative(z.next());
						if (nz.getFrame() != TCube.FRAME_NULL) {
							z = nz;
						} else {
							break;
						}
					}

					addBlockedCube(z);
					if (s.getFrame() < depth() && z.getFrame() != TCube.FRAME_INF) {
						Q.add(s.next());
					}
				} else {
					// Cube 's' was not blocked by image of predecessor
					z.setFrame(s.getFrame() - 1);
					z.getCube().setNext(s.getCube());
					Q.add(z);
					Q.add(s);
				}
			}
		}
	}

	private void checkNewInvariants() {
		if (!incomingInvariants.isEmpty()) {
			List<Expr> drain = new ArrayList<>();
			incomingInvariants.drainTo(drain);
			tryAddInvariants(drain);
		}
	}

	private boolean isBlocked(TCube s) {
		// Check syntactic subsumption (faster than SAT):
		for (int d = s.getFrame(); d < F.size(); d++) {
			for (Cube c : F.get(d).getCubes()) {
				if (c.subsumes(s.getCube())) {
					return true;
				}
			}
		}

		// Semantic subsumption thru SAT:
		return Z.isBlocked(s);
	}

	private void generalize(TCube s) {
		List<Term> pLiterals = new ArrayList<>(s.getCube().getPLiterals());

		for (Term p : pLiterals) {
			s.getCube().removePLiteral(p);
			if (Z.isInitial(s.getCube()) || Z.solveRelative(s).getFrame() == TCube.FRAME_NULL) {
				s.getCube().addPLiteral(p);
			}
		}
	}

	private int depth() {
		return F.size() - 2;
	}

	private void addFrame(Frame frame) {
		F.add(F.size() - 1, frame);
		System.out.println("Num PDR Frames: "+F.size());
	}

	private List<Expr> propogateBlockedCubes() {
		for (int k = 1; k < depth(); k++) {
			for (Cube c : new ArrayList<>(F.get(k).getCubes())) {
				checkCancel();
				checkNewInvariants();
				TCube s = Z.solveRelative(new TCube(c, k + 1), Option.NO_IND);
				if (s.getFrame() != TCube.FRAME_NULL) {
					addBlockedCube(s);
				}
			}

			if (F.get(k).isEmpty()) {
				return getInvariants(k + 1);
			}
		}

		return null;
	}

	private List<Expr> getInvariants(int k) {
		List<Expr> invariants = new ArrayList<>();
		for (int i = k; i < F.size(); i++) {
			for (Cube c : F.get(i).getCubes()) {
				invariants.add(Z.getInvariant(c));
			}
		}
		return invariants;
	}

	private void checkCancel() {
		if (cancel) {
			throw new StopException();
		}
	}

	private void addBlockedCube(TCube s) {
		int k = Math.min(s.getFrame(), depth() + 1);

		// Remove subsumed clauses:
		for (int d = 1; d <= k; d++) {
			Set<Cube> cubes = F.get(d).getCubes();
			for (Cube c : new ArrayList<>(cubes)) {
				if (s.getCube().subsumes(c)) {
					cubes.remove(c);
				}
			}
		}

		// Store clause
		F.get(k).add(s.getCube());
		Z.comment("Blocked [" + k + "] : " + s.getCube());

		// Report if invariant
		if (s.getFrame() == TCube.FRAME_INF) {
			Expr invariant = Z.getInvariant(s.getCube());
			sendInvariant(invariant);
		}
	}

	private List<Cube> getCubes(Cube init) {
		List<Cube> result = new ArrayList<>();
		Cube curr = init;
		while (curr != null) {
			result.add(curr);
			curr = curr.getNext();
		}
		return result;
    }

    private void reportInductCex() {
        Model model = Z.getIndCex();
        if (model != null) {
            director.broadcast(new InductiveCounterexampleMessage(Collections.singletonList(prop), F.size(),
                    model, parent.getName()));
        }
    }

    private void sendValidAndInvariants(List<Expr> invariants) {
        Itinerary itinerary = director.getValidMessageItinerary();
        director.broadcast(new ValidMessage(parent.getName(), prop, 1, invariants, null, itinerary));
		director.broadcast(new InvariantMessage(invariants));
	}

	private void sendInvalid(int length, Model model) {
		Itinerary itinerary = director.getInvalidMessageItinerary();
		director.broadcast(new InvalidMessage(parent.getName(), prop, length, model, itinerary));
	}

	private void sendInvariant(Expr invariant) {
		director.broadcast(new InvariantMessage(invariant));
	}
}
