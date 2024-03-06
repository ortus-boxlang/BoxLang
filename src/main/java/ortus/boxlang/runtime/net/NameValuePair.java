package ortus.boxlang.runtime.net;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NameValuePair {

	private final @Nonnull String	name;
	private final @Nullable String	value;

	public NameValuePair( @Nonnull String name, @Nullable String value ) {
		this.name	= name;
		this.value	= value;
	}

	public static NameValuePair fromNativeArray( String[] nameAndValue ) {
		if ( nameAndValue.length > 1 ) {
			return new NameValuePair( nameAndValue[ 0 ], nameAndValue[ 1 ] );
		}
		return new NameValuePair( nameAndValue[ 0 ], null );
	}

	@Nonnull
	public String getName() {
		return name;
	}

	@Nullable
	public String getValue() {
		return value;
	}

	public String toString() {
		if ( this.value == null ) {
			return this.name;
		}
		return this.name + "=" + this.value;
	}

}
