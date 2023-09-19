package ortus.boxlang.runtime.config;

import ortus.boxlang.runtime.config.segments.CompilerConfig;
import ortus.boxlang.runtime.config.segments.RuntimeConfig;

public class Configuration {

	private CompilerConfig	compiler	= new CompilerConfig();
	private RuntimeConfig	runtime		= new RuntimeConfig();

	public Configuration() {
	}

	public CompilerConfig getCompiler() {
		return compiler;
	}

	public void setCompiler( CompilerConfig compiler ) {
		this.compiler = compiler;
	}

	public RuntimeConfig getRuntime() {
		return runtime;
	}

	public void setRuntime( RuntimeConfig runtime ) {
		this.runtime = runtime;
	}
}
