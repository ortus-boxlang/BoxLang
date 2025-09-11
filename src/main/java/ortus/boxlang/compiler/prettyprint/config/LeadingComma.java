package ortus.boxlang.compiler.prettyprint.config;

import java.util.Map;

public class LeadingComma {

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