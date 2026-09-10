package ortus.boxlang.runtime.bifs.global.zip;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.itadaki.bzip2.BZip2InputStream;
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
import ortus.boxlang.runtime.util.jtar.TarEntry;
import ortus.boxlang.runtime.util.jtar.TarInputStream;

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

	@Test
	public void testCompressTar() throws Exception {
		String	source		= "src/test/resources/chuck_norris.jpg";
		String	destination	= "src/test/resources/tmp/compress_tests/test.tar";
		variables.put( Key.source, source );
		variables.put( Key.destination, destination );

		instance.executeSource( "compress( format='tar', source=source, destination=destination );", context );

		try ( TarInputStream tarInputStream = new TarInputStream( Files.newInputStream( Path.of( destination ) ) ) ) {
			TarEntry entry = tarInputStream.getNextEntry();
			assertThat( entry ).isNotNull();
			assertThat( entry.getName() ).isEqualTo( "chuck_norris.jpg" );
		}
	}

	@Test
	public void testCompressTgz() {
		String	source		= "src/test/resources/chuck_norris.jpg";
		String	destination	= "src/test/resources/tmp/compress_tests/test.tgz";
		variables.put( Key.source, source );
		variables.put( Key.destination, destination );

		instance.executeSource( "compress( format='tgz', source=source, destination=destination );", context );

		assertThat( Files.exists( Path.of( destination ) ) ).isTrue();
	}

	@Test
	public void testCompressDetectsFormatFromDestination() {
		String	source		= "src/test/resources/chuck_norris.jpg";
		String	destination	= "src/test/resources/tmp/compress_tests/detected.tar";
		variables.put( Key.source, source );
		variables.put( Key.destination, destination );

		instance.executeSource( "compress( source=source, destination=destination );", context );

		assertThat( Files.exists( Path.of( destination ) ) ).isTrue();
	}

	@Test
	public void testCompressBzip() throws Exception {
		String	source		= "src/test/resources/chuck_norris.jpg";
		String	destination	= "src/test/resources/tmp/compress_tests/test.bz2";
		variables.put( Key.source, source );
		variables.put( Key.destination, destination );

		instance.executeSource( "compress( format='bzip2', source=source, destination=destination );", context );

		try ( BZip2InputStream input = new BZip2InputStream( Files.newInputStream( Path.of( destination ) ), false );
		    ByteArrayOutputStream output = new ByteArrayOutputStream() ) {
			input.transferTo( output );
			assertThat( output.toByteArray() ).isEqualTo( Files.readAllBytes( Path.of( source ) ) );
		}
	}

	@Test
	public void testCompressTbzAndTarBzAliases() throws Exception {
		String source = "src/test/resources/chuck_norris.jpg";
		for ( String format : new String[] { "tbz", "tbz2", "tar.bz" } ) {
			String destination = "src/test/resources/tmp/compress_tests/test-" + format
			    + ( format.equals( "tbz2" ) ? ".tbz2" : ".tbz" );
			variables.put( Key.source, source );
			variables.put( Key.destination, destination );

			instance.executeSource( "compress( format='" + format + "', source=source, destination=destination );", context );

			try ( BZip2InputStream bzipInput = new BZip2InputStream( Files.newInputStream( Path.of( destination ) ), false );
			    TarInputStream tarInput = new TarInputStream( bzipInput ) ) {
				TarEntry entry = tarInput.getNextEntry();
				assertThat( entry ).isNotNull();
				assertThat( entry.getName() ).isEqualTo( "chuck_norris.jpg" );
			}
		}
	}

}