package ortus.boxlang.runtime.config.segments;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RuntimeConfig {

	/**
	 * The directory where the modules are located by default:
	 * {@code /{user-home}/modules}
	 */
	@JsonProperty( "modulesDirectory" )
	private String				modulesDirectory;

	/**
	 * The cache configurations for the runtime
	 */
	@JsonProperty( "caches" )
	private List<CacheConfig>	caches	= new ArrayList<>();

	public RuntimeConfig() {
	}

	public String getModulesDirectory() {
		return modulesDirectory;
	}

	public void setModulesDirectory( String modulesDirectory ) {
		this.modulesDirectory = modulesDirectory;
	}

	public List<CacheConfig> getCaches() {
		return caches;
	}

	public void setCaches( List<CacheConfig> caches ) {
		this.caches = caches;
	}
}
