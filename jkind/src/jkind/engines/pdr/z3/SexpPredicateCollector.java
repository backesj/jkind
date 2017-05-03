package jkind.engines.pdr.z3;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.sexp.Symbol;

public class SexpPredicateCollector {
	
	private final static Set<String> nonPredSyms;
	
	static {
		Set<String> syms = new HashSet<>();
		syms.add("and");
		syms.add("or");
		syms.add("not");
		nonPredSyms = Collections.unmodifiableSet(syms);
	}
	
	public static Set<Sexp> collect(Sexp sexp){
		if(sexp instanceof Symbol){
			return Collections.singleton(sexp);
		}else if (sexp instanceof Cons) {
			Cons cons = (Cons) sexp;
			if(!nonPredSyms.contains(cons.head)){
				return Collections.singleton(sexp);
			}
			return cons.args.stream().flatMap(a -> collect(a).stream()).collect(Collectors.toSet());
		}
		throw new IllegalArgumentException("Unknown Sexp type: '"+sexp.getClass()+"'");
	
	}
	

}
