package jkind.engines.pdr;

import java.util.ArrayList;
import java.util.List;

import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.Util;

public class SmtInterpolCube extends Cube{
	private final List<Term> pLiterals = new ArrayList<>();

	
	public SmtInterpolCube(){
	}
	
	public SmtInterpolCube(List<Term> pLiterals){
		this.pLiterals.addAll(pLiterals);
	}
	
	public void addPLiteral(Term term) {
		pLiterals.add(term);
	}

	public void removePLiteral(Term term) {
		pLiterals.remove(term);
	}

	public List<Term> getPLiterals() {
		return pLiterals;
	}

	public boolean subsumes(Cube other) {
		return ((SmtInterpolCube)other).pLiterals.containsAll(pLiterals);
	}

	public Term toTerm(Script script) {
		return Util.and(script, pLiterals.toArray(new Term[pLiterals.size()]));
	}

	@Override
	public String toString() {
		return pLiterals.toString();
	}

	@Override
	public Cube removeLiteral(int i) {
		List<Term> lits = new ArrayList<>();
		lits.addAll(pLiterals);
		lits.remove(i);
		SmtInterpolCube newCube = new SmtInterpolCube(lits);
		newCube.setNext(getNext());
		return newCube;
	}

	@Override
	public int size() {
		return pLiterals.size();
	}
}
