package jkind.translation;

import java.util.ArrayList;
import java.util.List;

import jkind.JKindException;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.sexp.Symbol;

public class InlineTransitionRelation {
	private static int index = -1;
	private static final String PREV = "\\$.*\\$0";
	private static final String NEXT = "\\$.*\\$1";
	private static final String INIT = "%init";
	
	public static Sexp inline(Relation relation, int k){
		index = k-1;
		//we assume ids are in the form $id$0 and $id$1
		return rewriteIds(relation.getBody());		
	}
	
	private static Sexp rewriteIds(Sexp sexp){
		if(sexp instanceof Cons){
			Cons cons = (Cons)sexp;
			return rewriteIds(cons);
		}
		if(!(sexp instanceof Symbol)){
			throw new JKindException("Unexpected sexp class: "+sexp.getClass());
		}
		Symbol symbol = (Symbol)sexp;
		return rewriteIds(symbol);
	}
	
	private static Sexp rewriteIds(Cons cons){
		List<Sexp> args = new ArrayList<>();
		for(Sexp arg : cons.args){
			args.add(rewriteIds(arg));
		}
		return new Cons(rewriteIds(cons.head), args);
	}
	
	private static Sexp rewriteIds(Symbol symbol){
		String str = symbol.str;
		int end = str.lastIndexOf('$')+1;
		if(str.matches(INIT)){
			return index == -1 ? new Symbol("true") : new Symbol("false");
		}
		if(str.matches(PREV)){
			str = str.substring(0, end);
			String ending = (index == -1) ? "~1" : Integer.toString(index);
			return new Symbol(str+ending);
		}
		if(str.matches(NEXT)){
			str = str.substring(0, end);
			return new Symbol(str+(index+1));
		}
		return symbol;
	}
	

}
