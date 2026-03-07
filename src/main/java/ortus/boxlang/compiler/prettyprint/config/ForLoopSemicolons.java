package ortus.boxlang.compiler.prettyprint.config;

import java.util.LinkedHashMap;
import java.util.Map;

public class ForLoopSemicolons {

	private boolean padding = true;

	public ForLoopSemicolons() {
	}

	public boolean getPadding() {
		return padding;
	}

	public ForLoopSemicolons setPadding( boolean padding ) {
		this.padding = padding;
		return this;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "padding", padding );
		return map;
	}
}
