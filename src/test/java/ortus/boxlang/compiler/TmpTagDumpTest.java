package ortus.boxlang.compiler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.services.CodeProfilerService;

class TmpTagDumpTest {

	private BoxRuntime	runtime;
	private boolean		prev;

	@BeforeEach
	void setup() {
		this.runtime										= BoxRuntime.getInstance();
		this.prev											= runtime.getConfiguration().codeProfilerEnabled;
		runtime.getConfiguration().codeProfilerEnabled		= true;
		RunnableLoader.getInstance().getBoxpiler().clearPagePool();
		RunnableLoader.getInstance().getBoxpiler().clearClassFiles();
		CodeProfilerService.setActive( true );
		CodeProfilerService.reset();
	}

	@AfterEach
	void teardown() {
		runtime.getConfiguration().codeProfilerEnabled = this.prev;
		CodeProfilerService.setActive( false );
		CodeProfilerService.reset();
	}

	@Test
	void dumpBxm() throws Exception {
		String	rel		= "src/test/resources/profiler/ProfilerSample.bxm";
		runtime.executeTemplate( rel );
		Path	abs		= Paths.get( rel ).toAbsolutePath().normalize();
		String	key		= abs.toString().toLowerCase( Locale.ROOT );
		Files.writeString( Paths.get( "build/tagdump.txt" ), CodeProfilerService.dumpSpans( key ) );
	}
}