package ortus.boxlang.runtime.config.segments;

import java.util.HashMap;
import java.util.Map;

public class CacheConfig {

	private String				type		= "Caffeine";
	private Map<String, Object>	properties	= new HashMap<>();

	public String getType() {
		return type;
	}

	public void setType( String type ) {
		this.type = type;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties( Map<String, Object> properties ) {
		this.properties = properties;
	}
}
