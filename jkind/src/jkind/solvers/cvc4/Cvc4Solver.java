package jkind.solvers.cvc4;

import java.util.List;

import jkind.lustre.InductType;
import jkind.sexp.Sexp;
import jkind.sexp.Symbol;
import jkind.solvers.smtlib2.SmtLib2Solver;
import jkind.translation.InductiveDataTypeSpecification;
import jkind.translation.Specification;

public class Cvc4Solver extends SmtLib2Solver {
        
	public Cvc4Solver(String scratchBase) {
		super(scratchBase);
	}

	@Override
	protected String getSolverName() {
		return "CVC4";
	}

	@Override
	protected String[] getSolverOptions() {
		return new String[] { "--lang", "smt", "--fmf-fun" };
	}
    
	@Override
	public void initialize(Specification spec) {
		send("(set-option :produce-models true)");
		send("(set-option :incremental true)");
		send("(set-option :rewrite-divk true)");
//		send("(set-logic AUFLIRA)");
		send("(set-logic ALL_SUPPORTED)");
		
		if(spec instanceof InductiveDataTypeSpecification){
		    InductiveDataTypeSpecification indSpec = (InductiveDataTypeSpecification)spec;
	        for(InductType type : indSpec.inductTypes){
	                define(type);
	        }
	        for(Sexp sexp : indSpec.functions){
                send(sexp);
            }
		}
	}

	@Override
	protected List<Symbol> getUnsatCore(List<Symbol> activationLiterals) {
		// CVC4 does not yet support unsat-cores
		return activationLiterals;
	}
}
