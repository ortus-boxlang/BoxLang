package ortus.boxlang.runtime.config.segments;

public class RuntimeConfig {

	private String		modulesDirectory;
	private CacheConfig	caches	= new CacheConfig();

	public RuntimeConfig() {
	}

	public String getModulesDirectory() {
		return modulesDirectory;
	}

	public void setModulesDirectory( String modulesDirectory ) {
		this.modulesDirectory = modulesDirectory;
	}

	public CacheConfig getCaches() {
		return caches;
	}

	public void setCaches( CacheConfig caches ) {
		this.caches = caches;
	}
}
