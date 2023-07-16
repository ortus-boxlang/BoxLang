package ortus.boxlang.runtime.scopes;

import java.util.HashMap;

/**
 * Base scope implementation.  Extends HashMap for now. May want to switch to composition over inheritance, but this
 * is simpler for now and using the Key class provides our case insensitivity automatically.
 */
public class BaseScope extends HashMap<Key,Object> implements IScope {
    private int lookupOrder;

    public BaseScope( int lookupOrder ) {
        super();
        this.lookupOrder = lookupOrder;
    }

    public int getLookupOrder() {
        return this.lookupOrder;
    }

    //private Map<Key,Object> data = new HashMap<Key,Object>();
    //public Object getValue( Key name ) {    }

    //public IScope setValue( Key name, Object value ) {    }

}
