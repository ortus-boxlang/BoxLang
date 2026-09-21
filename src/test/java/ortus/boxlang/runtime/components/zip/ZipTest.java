package ortus.boxlang.runtime.components.zip;

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

public class ZipTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );
	static Key			bxhttp	= new Key( "bxhttp" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
		if ( !FileSystemUtil.exists( "src/test/resources/tmp/zip_tests" ) ) {
			FileSystemUtil.createDirectoryIfMissing( Path.of( "src/test/resources/tmp/zip_tests" ) );
		}
	}

	@AfterAll
	public static void teardown() {
		if ( FileSystemUtil.exists( "src/test/resources/tmp/zip_tests" ) ) {
			FileSystemUtil.deleteDirectory( "src/test/resources/tmp/zip_tests", true );
		}
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It can zip a simple file" )
	@Test
	public void testZipSimple() {
		String	source		= "src/test/resources/chuck_norris.jpg";
		String	destination	= "src/test/resources/tmp/zip_tests/chuck_norris_test.zip";

		variables.put( Key.source, source );
		variables.put( Key.destination, destination );
		// @formatter:off
		instance.executeSource(
			"""
				bx:zip source="#source#" file="#destination#" {}
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

	@DisplayName( "It can zip a directory" )
	@Test
	public void testZipDir() {
		String	source		= "src/test/resources";
		String	destination	= "src/test/resources/tmp/zip_tests/test_directory_zip.zip";

		variables.put( Key.source, source );
		variables.put( Key.destination, destination );
		// @formatter:off
		instance.executeSource(
			"""
				bx:zip source="#source#" file="#destination#" recurse="false" {}
			""",
		    context
		);
		// @formatter:on

		Array list = ZipUtil.listEntriesFlat( destination, "", true, null );
		System.out.println( list.toList() );
		assertThat( list.toList() ).doesNotContain( "resources/" );
		assertThat( list.toList().size() ).isGreaterThan( 3 );
	}

	@DisplayName( "It can zip with params" )
	@Test
	public void testZipParams() {
		String	source		= "src/test/resources";
		String	destination	= "src/test/resources/tmp/zip_tests/test_param_zip.zip";

		variables.put( Key.source, source );
		variables.put( Key.destination, destination );
		// @formatter:off
		instance.executeSource(
			"""
				images = directoryList( source, false, "path", "chuck_norris.jpg" );
				bx:zip file="#destination#" {
					for ( image in images ) {
						bx:zipparam source="#image#";
					}
				}
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

	@DisplayName( "It can zip with binary params" )
	@Test
	public void testZipBinaryParam() {
		String	source		= "src/test/resources";
		String	destination	= "src/test/resources/tmp/zip_tests/test_binary_param_zip.zip";

		variables.put( Key.source, source );
		variables.put( Key.destination, destination );
		// @formatter:off
		instance.executeSource(
			"""
				images = directoryList( source, false, "path", "chuck_norris.jpg" );
				bx:zip file="#destination#" {
					for ( image in images ) {
						binaryFile = fileReadBinary( image );
						bx:zipparam content="#binaryFile#" entryPath="binary_chuck.jpg";
					}
				}
			""",
		    context
		);
		// @formatter:on

		Array list = ZipUtil.listEntriesFlat( destination, "", true, null );
		System.out.println( list );
		assertThat( list.toList() ).doesNotContain( "resources/" );
		assertThat( list.size() ).isEqualTo( 1 );
		assertThat( list.get( 0 ) ).isEqualTo( "binary_chuck.jpg" );
	}

	@DisplayName( "It prevents zip file from including itself when zipping its parent directory" )
	@Test
	public void testZipInPlaceDoesNotIncludeItself() {
		String	fixtureDir		= "src/test/resources/tmp/zip_tests/zip-in-place-fixture";
		String	sampleTextPath	= fixtureDir + "/sample-one.txt";
		String	sampleCsvPath	= fixtureDir + "/sample-two.csv";
		String	archivePath		= fixtureDir + "/in_place_test.zip";

		variables.put( Key.of( "fixtureDir" ), fixtureDir );
		variables.put( Key.of( "sampleTextPath" ), sampleTextPath );
		variables.put( Key.of( "sampleCsvPath" ), sampleCsvPath );
		variables.put( Key.of( "archivePath" ), archivePath );

		// @formatter:off
		instance.executeSource(
			"""
				// Clean up fixture directory if it exists
				if ( directoryExists( fixtureDir ) ) {
					directoryDelete( fixtureDir, true );
				}
				directoryCreate( fixtureDir, true );

				// Create sample files
				fileWrite( sampleTextPath, "Sample text file for zip test" );
				fileWrite( sampleCsvPath, "id,name\\n1,alpha\\n2,beta" );

				// Zip the fixture directory, placing the zip inside that same directory
				bx:zip source="#fixtureDir#" file="#archivePath#" recurse="false" overwrite="true" {}
			""",
		    context
		);
		// @formatter:on

		// List the contents of the zip file using ZipUtil
		Array zipContents = ZipUtil.listEntriesFlat( archivePath, "", true, null );
		System.out.println( "Contents of zip file: " + zipContents.toList() );

		// Verify that the zip file itself is NOT included in the archive
		// This is the red test - it should fail until we fix the zip component
		assertThat( zipContents.toList() ).doesNotContain( "in_place_test.zip" );

		// Verify that the sample files ARE included
		assertThat( zipContents.toList() ).contains( "sample-one.txt" );
		assertThat( zipContents.toList() ).contains( "sample-two.csv" );
	}
}