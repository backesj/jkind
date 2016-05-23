package jkind.api.results;

import jkind.JKindException;
import jkind.results.Counterexample;
import jkind.results.InvalidProperty;
import jkind.results.Property;
import jkind.results.UnknownProperty;
import jkind.results.ValidProperty;

public class PropertyResult extends AnalysisResult {
	protected Property property;
	protected final Renaming renaming;
	protected boolean invertStatus = false;
	protected boolean invalidInPast = false;

	protected int elapsed;
	protected int baseProgress;
	protected Status status;

	public PropertyResult(String name, Renaming renaming, boolean invertStatus) {
		this(name, renaming);
		this.invertStatus = invertStatus;
	}

	public PropertyResult(String name, Renaming renaming) {
		super(name);
		this.elapsed = 0;
		this.status = Status.WAITING;
		this.property = null;
		this.renaming = renaming;
	}

	public Property getProperty() {
		return property;
	}

	public int getElapsed() {
		return elapsed;
	}

	public int getBaseProgress() {
		return baseProgress;
	}

	public Status getStatus() {
		return status;
	}

	public void setProperty(Property original) {
	    Property renamed = null;
		if (renaming == null) {
			renamed = original;
		} else {
			renamed = renaming.rename(original);
		}

		//if the property already exists update the counterexample
		if(property != null && property instanceof UnknownProperty && renamed instanceof UnknownProperty){
		    UnknownProperty renamedUnknown = (UnknownProperty)renamed;
		    for(Counterexample cex : renamedUnknown.getInductiveCounterexamples()){
		        ((UnknownProperty)property).addInductiveCounterexample(cex);
		    }
		}else{
		    property = renamed;
		}
		
		if (property instanceof ValidProperty) {
			if (invalidInPast) {
				if (invertStatus) {
					throw new JKindException("Refinement not supported for inverted property");
				}
				setStatus(Status.VALID_REFINED);
			} else {
				setStatus(invertStatus ? Status.INVALID : Status.VALID);
			}
		} else if (property instanceof InvalidProperty) {
			setStatus(invertStatus ? Status.VALID : Status.INVALID);
		} else if (property instanceof UnknownProperty) {
			setStatus(Status.UNKNOWN);
		}
	}

	public void start() {
		setStatus(Status.WORKING);
	}

	public void tick() {
		if (status == Status.WORKING || status == Status.INVALID) {
			pcs.firePropertyChange("elapased", elapsed, ++elapsed);
		}
	}

	public void cancel() {
		if (status == Status.WORKING || status == Status.WAITING) {
			setStatus(Status.CANCELED);
		}
	}

	public void done() {
		if (status == Status.WORKING || status == Status.WAITING) {
			setStatus(Status.ERROR);
		}
	}

	public void setBaseProgress(int k) {
		if (status == Status.WORKING) {
			pcs.firePropertyChange("progress", baseProgress, baseProgress = k);
		}
	}

	public void setStatus(Status status) {
		if (this.status == Status.INVALID) {
			invalidInPast = true;
		}
		pcs.firePropertyChange("status", this.status, this.status = status);
	}

	@Override
	public String toString() {
		return name + " - " + status;
	}
}
