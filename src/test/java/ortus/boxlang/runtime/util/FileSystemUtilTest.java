package ortus.boxlang.runtime.util;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.ConfigOverrideBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class FileSystemUtilTest {

	private static BoxRuntime			runtime;
	private static RequestBoxContext	context;
	private static IScope				variables;
	private static Path					absoluteAppPath;

	@BeforeAll
	public static void setUp() {
		runtime = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() {
		absoluteAppPath	= Path.of( "src/test/resources" ).toAbsolutePath();
		context			= new ScriptingRequestBoxContext( new ConfigOverrideBoxContext( runtime.getRuntimeContext(), config -> {
							config.getAsStruct( Key.mappings ).put( "/root", absoluteAppPath );
							return config;
						} ) );
		variables		= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "Test Expand Path with the passed in resolved file path as a directory" )
	@Test
	void testExpandPathResolvedDirectoryParent() {
		ResolvedFilePath resolvedPath = FileSystemUtil.expandPath( context, "foo/bar" );
		assertThat( resolvedPath.absolutePath() ).isNotNull();
		System.out.println( resolvedPath.absolutePath().toString() );
		assertThat( resolvedPath.absolutePath().toString() ).isNotEqualTo( "unknown" );
		ResolvedFilePath resolvedPath2 = FileSystemUtil.expandPath( context, "models", resolvedPath );
		assertThat( resolvedPath2.absolutePath().toString() ).isEqualTo(
		    resolvedPath.absolutePath().toString() + "/models"
		);
	}
}
