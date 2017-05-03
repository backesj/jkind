package jkind.engines.pdr;

import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.Util;

public class SmtInterpolFrame extends Frame{

	private final Term term;

	
	public SmtInterpolFrame(Term term) {
		this.term = term;
	}

	public SmtInterpolFrame() {
		this.term = null;
	}

	
	public Term toTerm(Script script) {
		if (term != null) {
			return term;
		}

		Term[] terms = new Term[cubes.size()];
		int i = 0;
		for (Cube c : cubes) {
			terms[i] = Util.not(script, ((SmtInterpolCube)c).toTerm(script));
			i++;
		}
		return Util.and(script, terms);
	}
	
	public boolean isEmpty() {
		return term == null && cubes.isEmpty();
	}

	@Override
	public String toString() {
		if (term != null) {
			return term.toString();
		} else {
			return cubes.toString();
		}
	}
	
}
