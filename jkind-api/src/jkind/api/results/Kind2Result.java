package jkind.api.results;

import java.util.List;

public class Kind2Result extends JKindResult {


    public Kind2Result(String name, List<String> properties, Renaming renaming) {
        super(name, properties, renaming);
        // TODO Auto-generated constructor stub
    }
    
    @Override
    public PropertyResult addProperty(String property, boolean invertStatus) {
        if (renaming != null) {
            property = renaming.rename(property);
            if (property == null) {
                return null;
            }
        }

        PropertyResult propertyResult = new Kind2PropertyResult(property, renaming, invertStatus);
        propertyResults.add(propertyResult);
        propertyResult.setParent(this);
        pcs.fireIndexedPropertyChange("propertyResults", propertyResults.size() - 1, null,
                propertyResult);
        addStatus(propertyResult.getStatus());
        propertyResult.addPropertyChangeListener(this);
        return propertyResult;
    }

}
