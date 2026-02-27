package ortus.boxlang.compiler.prettyprint.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Separator {

	@JsonProperty( ":" )
	COLON(":" ),

	@JsonProperty( "=" )
	EQUALS("=" ),

	@JsonProperty( ": " )
	COLON_SPACE(": " ),

	@JsonProperty( "= " )
	EQUALS_SPACE("= " );

	private final String symbol;

	Separator( String symbol ) {
		this.symbol = symbol;
	}

	@JsonValue
	public String getSymbol() {
		return symbol;
	}
}
