package jkind.engines.pdr.z3;

import java.util.List;
import java.util.stream.Collectors;

import jkind.engines.pdr.Frame;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;

public class SexpFrame extends Frame{
	
	public Sexp toSexp(){
		List<Sexp> cubeSexps = cubes.stream().map(c -> ((SexpCube)c).toSexp()).collect(Collectors.toList());
		return new Cons("and", cubeSexps);
	}

}
