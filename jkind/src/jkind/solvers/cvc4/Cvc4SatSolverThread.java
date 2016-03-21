package jkind.solvers.cvc4;

import java.util.concurrent.BlockingQueue;

public class Cvc4SatSolverThread extends Cvc4SolverThread {
	
	public Cvc4SatSolverThread(String scratchBase, BlockingQueue<MultiSolverResult> outgoing){
		super(scratchBase, outgoing);
	}
	
	@Override
	protected String[] getSolverOptions(){
		return new String[]{"--lang", "smt", "--fmf-fun"};
	}
}
