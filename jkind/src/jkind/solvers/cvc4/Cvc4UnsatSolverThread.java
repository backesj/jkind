package jkind.solvers.cvc4;

import java.util.concurrent.BlockingQueue;

public class Cvc4UnsatSolverThread extends Cvc4SolverThread {
	
	public Cvc4UnsatSolverThread(String scratchBase, BlockingQueue<MultiSolverResult> outgoing){
		super(scratchBase, outgoing);
	}
	
	@Override
	protected String[] getSolverOptions(){
		return new String[]{"--lang", "smt", "--quant-ind"};
	}

}
