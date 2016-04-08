package jkind.api.kind2;

import jkind.lustre.ArrayType;
import jkind.lustre.QuantExpr;
import jkind.lustre.Type;
import jkind.lustre.VarDecl;
import jkind.lustre.visitors.PrettyPrintVisitor;

public class Kind2PrettyPrintVisitor extends PrettyPrintVisitor {
	@Override
	public Void visit(VarDecl varDecl) {
		write(varDecl.id);
		write(" : ");
		write(convert(varDecl.type));
		return null;
	}

	private String convert(Type type) {
		if (type instanceof ArrayType) {
			ArrayType at = (ArrayType) type;
			return convert(at.base) + "^" + at.size;
		} else {
			return type.toString();
		}
	}
	
	@Override
    public Void visit(QuantExpr e) {
        write(e.op);
        write("(");
        String delim = "";
        for(VarDecl var : e.boundVars){
            write(delim+var.id+" : "+var.type);
            delim = ",";
        }
        write(")");
        expr(e.expr);
        return null;
    }
}
