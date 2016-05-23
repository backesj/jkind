package jkind.api.results;

import jkind.JKindException;
import jkind.results.Counterexample;
import jkind.results.InvalidProperty;
import jkind.results.Property;
import jkind.results.UnknownProperty;
import jkind.results.ValidProperty;

public class Kind2PropertyResult extends PropertyResult {

    public Kind2PropertyResult(String name, Renaming renaming, boolean invertStatus) {
        super(name, renaming, invertStatus);
    }

    @Override
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
            //we do not set this until the end for kind2
        }
    }
    
}
