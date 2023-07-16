package ortus.boxlang.runtime.scopes;
/**
 * All scope implementations must implement this interface
 */
public interface IScope {

    public int getLookupOrder();
    //public Object getValue( Key name );

    //public IScope setValue( Key name, Object value );
}
