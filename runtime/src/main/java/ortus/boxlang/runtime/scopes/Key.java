package ortus.boxlang.runtime.scopes;

/**
 * Represents a case-insenstive key, while retaining the original case too.
 */
public class Key {
    private String name;
    private String nameNoCase;

    public Key( String name ) {
        this.name = name;
        this.nameNoCase = name.toUpperCase();
    }

    public String getNameNoCase() {
        return this.nameNoCase;
    }

    public String getName() {
        return this.name;
    }

    @Override
	public boolean equals( Object obj ) {
        if( this == obj ) return true;
        return false;
    }

    @Override
	public int hashCode() {
		return nameNoCase.hashCode();
	}

    public static Key of( String name ) {
        return new Key( name );
    }

}
