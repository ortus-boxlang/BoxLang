package ortus.boxlang.runtime.bifs.global.zip;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.util.ZipUtil;

public class CompressTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );
	static Key			bxhttp	= new Key( "bxhttp" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
		if ( !FileSystemUtil.exists( "src/test/resources/tmp/compress_tests" ) ) {
			FileSystemUtil.createDirectoryIfMissing( Path.of( "src/test/resources/tmp/compress_tests" ) );
		}
	}

	@AfterAll
	public static void teardown() {
		if ( FileSystemUtil.exists( "src/test/resources/tmp/compress_tests" ) ) {
			FileSystemUtil.deleteDirectory( "src/test/resources/tmp/compress_tests", true );
		}
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It can compress a simple file" )
	@Test
	public void testCompressSimple() {
		String	source		= "src/test/resources/chuck_norris.jpg";
		String	destination	= "src/test/resources/tmp/compress_tests/chuck_norris_test.zip";

		variables.put( Key.source, source );
		variables.put( Key.destination, destination );
		// @formatter:off
		instance.executeSource(
			"""
				compress( source=source, destination=destination, recurse=false )
			""",
		    context
		);
		// @formatter:on

		Array list = ZipUtil.listEntriesFlat( destination, "", true, null );
		System.out.println( list );
		assertThat( list.toList() ).doesNotContain( "resources/" );
		assertThat( list.size() ).isEqualTo( 1 );
		assertThat( list.get( 0 ) ).isEqualTo( "chuck_norris.jpg" );
	}

	@DisplayName( "It can compress a directory" )
	@Test
	public void testCompressDir() {
		String	source		= "src/test/resources";
		String	destination	= "src/test/resources/tmp/compress_tests/test_directory_zip.zip";

		variables.put( Key.source, source );
		variables.put( Key.destination, destination );
		// @formatter:off
		instance.executeSource(
			"""
				compress( source=source, destination=destination, includeBaseFolder=false, recurse=false )
			""",
		    context
		);
		// @formatter:on

		Array list = ZipUtil.listEntriesFlat( destination, "", true, null );
		System.out.println( list.toList() );
		assertThat( list.toList() ).doesNotContain( "resources/" );
		assertThat( list.toList().size() ).isGreaterThan( 3 );
	}

}