package jkind.engines.pdr.z3;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import jkind.engines.pdr.Cube;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.util.Util;

public class SexpCube extends Cube{

	private final List<Sexp> lits;
	
	public SexpCube(List<Sexp> lits){
		this.lits = Util.safeList(lits);
	}
	
	public List<Sexp> getLits(){
		return lits;
	}
	
	public Sexp toSexp(){
		return new Cons("and", lits);
	}
	
	@Override
	public boolean subsumes(Cube other) {
		return ((SexpCube)other).lits.containsAll(lits);
	}

	@Override
	public Cube removeLiteral(int i) {
		List<Sexp> newLits = new LinkedList<>();
		newLits.addAll(lits);
		newLits.remove(i);
		return new SexpCube(newLits);
	}

	@Override
	public int size() {
		return lits.size();
	}

}
