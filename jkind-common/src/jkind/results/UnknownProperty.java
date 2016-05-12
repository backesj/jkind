package jkind.results;

import java.util.ArrayList;
import java.util.List;

import jkind.JKindException;

/**
 * An unknown property
 */
public final class UnknownProperty extends Property {
	private final int trueFor;
	private final List<Counterexample> cexs = new ArrayList<>();

	public UnknownProperty(String name, int trueFor, Counterexample cex, double runtime) {
		super(name, runtime);
        this.trueFor = trueFor;
        if (cex != null) {
            this.cexs.add(cex);
        }
	}
	
	public UnknownProperty(String name, int trueFor, List<Counterexample> cexs, double runtime) {
        super(name, runtime);
        this.trueFor = trueFor;
        this.cexs.addAll(cexs);
    }
	
	/**
	 * How many steps the property was true for in the base step
	 */
	public int getTrueFor() {
		return trueFor;
	}
	
	/**
	 * Inductive counterexamples for the property, only available if
	 * JKindApi.setInductiveCounterexamples()
	 */
	public List<Counterexample> getInductiveCounterexamples() {
		return cexs;
	}
	
	/**
	 * adds an inductive counterexample
	 */
	public void addInductiveCounterexample(Counterexample cex){
	    if(cex == null){
	        throw new JKindException("Cannot add null counterexample");
	    }
	    cexs.add(cex);
	}
}
