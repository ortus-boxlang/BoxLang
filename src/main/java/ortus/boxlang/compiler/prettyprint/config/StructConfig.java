package ortus.boxlang.compiler.prettyprint.config;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSON.Feature;

public class StructConfig {

	private boolean			padding			= false;
	@JsonProperty( "empty_padding" )
	private boolean			emptyPadding	= false;
	@JsonProperty( "quote_keys" )
	private boolean			quoteKeys		= false;
	private Separator		separator		= Separator.COLON_SPACE;
	private MultilineConfig	multiline		= new MultilineConfig();

	public static class LeadingComma {

		private boolean	enabled	= false;
		private boolean	padding	= true;

		public LeadingComma() {
		}

		public LeadingComma( boolean enabled ) {
			this.enabled = enabled;
		}

		public static LeadingComma fromMap( Map<String, Object> map ) {
			LeadingComma lc = new LeadingComma();
			if ( map.containsKey( "enabled" ) && map.get( "enabled" ) instanceof Boolean b ) {
				lc.setEnabled( b );
			}
			if ( map.containsKey( "padding" ) && map.get( "padding" ) instanceof Boolean b ) {
				lc.setPadding( b );
			}
			return lc;
		}

		public boolean getEnabled() {
			return enabled;
		}

		public LeadingComma setEnabled( boolean enabled ) {
			this.enabled = enabled;
			return this;
		}

		public boolean getPadding() {
			return padding;
		}

		public LeadingComma setPadding( boolean padding ) {
			this.padding = padding;
			return this;
		}
	}

	public static class MultilineConfig {

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

		public MultilineConfig setElementCount( int elementCount ) {
			this.elementCount = elementCount;
			return this;
		}
	}

	public StructConfig() {
	}

	public MultilineConfig getMultiline() {
		return multiline;
	}

	public StructConfig setMultiline( MultilineConfig multiline ) {
		this.multiline = multiline;
		return this;
	}

	public Separator getSeparator() {
		return separator;
	}

	public StructConfig setSeparator( Separator separator ) {
		this.separator = separator;
		return this;
	}

	public boolean getQuoteKeys() {
		return quoteKeys;
	}

	public StructConfig setQuoteKeys( boolean quoteKeys ) {
		this.quoteKeys = quoteKeys;
		return this;
	}

	public boolean getPadding() {
		return padding;
	}

	public StructConfig setPadding( boolean padding ) {
		this.padding = padding;
		return this;
	}

	public boolean getEmptyPadding() {
		return emptyPadding;
	}

	public StructConfig setEmptyPadding( boolean emptyPadding ) {
		this.emptyPadding = emptyPadding;
		return this;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "padding", padding );
		map.put( "empty_padding", emptyPadding );
		return map;
	}

	public String toJSON() {
		try {
			return JSON.std.with( Feature.PRETTY_PRINT_OUTPUT, Feature.WRITE_NULL_PROPERTIES )
			    .asString( toMap() );
		} catch ( IOException e ) {
			e.printStackTrace();
			throw new RuntimeException( "Failed to convert to JSON", e );
		}
	}
}