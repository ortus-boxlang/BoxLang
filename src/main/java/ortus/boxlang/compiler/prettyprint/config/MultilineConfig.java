package ortus.boxlang.compiler.prettyprint.config;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

public class MultilineConfig {

	@JsonProperty( "element_count" )
	private int				elementCount	= 4;

	@JsonProperty( "comma_dangle" )
	private boolean			commaDangle		= false;

	@JsonProperty( "leading_comma" )
	private LeadingComma	leadingComma	= new LeadingComma();

	@JsonProperty( "min_length" )
	private int				minLength		= 40;

	public MultilineConfig() {
	}

	public LeadingComma getLeadingComma() {
		return leadingComma;
	}

	@JsonSetter( "leading_comma" )
	public MultilineConfig setLeadingComma( Object leadingComma ) {
		if ( leadingComma instanceof Boolean b ) {
			this.leadingComma = new LeadingComma( b );
		} else if ( leadingComma instanceof Map lcMap ) {
			this.leadingComma = LeadingComma.fromMap( lcMap );
		}

		return this;
	}

	public int getMinLength() {
		return minLength;
	}

	public MultilineConfig setMinLength( int minLength ) {
		this.minLength = minLength;
		return this;
	}

	public boolean getCommaDangle() {
		return commaDangle;
	}

	public MultilineConfig setCommaDangle( boolean commaDangle ) {
		this.commaDangle = commaDangle;
		return this;
	}

	public int getElementCount() {
		return elementCount;
	}

	@JsonSetter( "element_count" )
	public MultilineConfig setElementCount( int elementCount ) {
		this.elementCount = elementCount;
		return this;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "element_count", elementCount );
		map.put( "comma_dangle", commaDangle );
		map.put( "leading_comma", leadingComma.toMap() );
		map.put( "min_length", minLength );
		return map;
	}
}