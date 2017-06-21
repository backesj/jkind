package jkind.engines.pdr.z3;

import java.util.List;

import jkind.JKindException;
import jkind.lustre.BinaryOp;
import jkind.lustre.values.Value;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.sexp.Symbol;
import jkind.solvers.Model;

public class PredicateEvaluator {
	
	public static Value evaluate(Model model, Sexp predicate){
		if (predicate instanceof Symbol) {
			Symbol sym = (Symbol) predicate;
			return model.getValue(sym.str);
		}
		Cons cons = (Cons)predicate;
		String strOp = ((Symbol)cons.head).str;
		List<? extends Sexp> args = cons.args;
		
		if(args.size() != 2){
			throw new JKindException("This was unexpected");
		}
		
		Value leftVal = evaluate(model, args.get(0));
		Value rightVal = evaluate(model, args.get(1));
		BinaryOp binOp = null;
		switch(strOp){
		case "+":
			binOp = BinaryOp.PLUS;
			break;
		case "-":
			binOp = BinaryOp.MINUS;
			break;
		case "/":
			binOp = BinaryOp.DIVIDE;
			break;
		case "*":
			binOp = BinaryOp.MULTIPLY;
			break;
		case "<":
			binOp = BinaryOp.LESS;
			break;
		case "<=":
			binOp = BinaryOp.LESSEQUAL;
			break;
		case ">":
			binOp = BinaryOp.GREATER;
			break;
		case ">=":
			binOp = BinaryOp.GREATEREQUAL;
			break;
		case "=":
			binOp = BinaryOp.EQUAL;
			break;
		default:
			throw new JKindException("Unhandled operation: '"+strOp+"'");	
		}
		return leftVal.applyBinaryOp(binOp, rightVal);
	}

}
