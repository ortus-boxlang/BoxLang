
/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ortus.boxlang.runtime.components.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class CacheTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result			= new Key( "result" );
	static String		tmpDirectory	= "src/test/resources/tmp/CacheComponentTest";
	static Key			testCacheKey	= Key.of( "CacheTestCache" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
		if ( !FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.createDirectory( tmpDirectory );
		}

		instance.getCacheService().createCache(
		    testCacheKey,
		    Key.boxCacheProvider,
		    Struct.of(
		        Key.maxObjects, 10,
		        Key.objectStore, "ConcurrentStore"
		    )
		);

	}

	@AfterAll
	public static void teardown() {
		if ( FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.deleteDirectory( tmpDirectory, true );
		}
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
		variables.clear();
	}

	@DisplayName( "It tests the Component CFCache with a simple variable" )
	@Test
	public void testComponentCF() {
		instance.executeSource(
		    """
		    <cfcache action="cache" name="result" key="foo" value="bar" />
		    """,
		    context, BoxSourceType.CFTEMPLATE );

		assertTrue( variables.get( result ) instanceof String );
		assertTrue( variables.getAsString( result ).length() > 0 );
		assertEquals( variables.getAsString( result ), "bar" );
	}

	@DisplayName( "It tests the Component BXCache with a simple variable" )
	@Test
	public void testComponentBX() {
		instance.executeSource(
		    """
		    <bx:cache action="cache" name="result" key="foo" value="bar" />
		    """,
		    context, BoxSourceType.BOXTEMPLATE );

		assertTrue( variables.get( result ) instanceof String );
		assertTrue( variables.getAsString( result ).length() > 0 );
		assertEquals( variables.getAsString( result ), "bar" );
	}

	@DisplayName( "It tests the Component Cache with a script parsing" )
	@Test
	public void testComponentScript() {
		instance.executeSource(
		    """
		    bx:cache action="cache" name="result" key="foo" value="bar";
		    """,
		    context, BoxSourceType.BOXSCRIPT );

		assertTrue( variables.get( result ) instanceof String );
		assertTrue( variables.getAsString( result ).length() > 0 );
		assertEquals( variables.getAsString( result ), "bar" );
	}

	@DisplayName( "It tests the ability to put and retrieve a cache object" )
	@Test
	public void testComponentPut() {
		instance.executeSource(
		    """
		    bx:cache action="put" key="foo" value="bar";
		    bx:cache action="get" key="foo" name="result";
		    bx:cache action="put" key="foo" value="baz";
		    bx:cache action="get" key="foo" name="result2";
		         """,
		    context, BoxSourceType.BOXSCRIPT );

		assertTrue( variables.get( result ) instanceof String );
		assertTrue( variables.getAsString( result ).length() > 0 );
		assertEquals( variables.getAsString( result ), "bar" );
		assertEquals( variables.getAsString( Key.of( "result2" ) ), "baz" );
	}

	@DisplayName( "It tests the Component Cache with complex object" )
	@Test
	public void testComponentScriptComplex() {
		instance.executeSource(
		    """
		       myStruct = { "foo" : "bar" };
		    bx:cache action="put" name="result" key="foo" value="#myStruct#";
		    bx: cache action="get" key="foo" name="result";
		          """,
		    context, BoxSourceType.BOXSCRIPT );

		assertTrue( variables.get( result ) instanceof IStruct );
		assertEquals( variables.getAsStruct( result ).getAsString( Key.of( "foo" ) ), "bar" );
	}

	@DisplayName( "It tests the Component BXCache with content between tags" )
	@Test
	public void testComponentBXContent() {
		instance.executeSource(
		    """
		         <bx:cache action="cache" name="result">
		      	<bx:script>
		      		actual = "bar";
		      	</bx:script>
		    <bx:output>
		      	<h1>Hello #actual#!</h1>
		    </bx:output>
		      </bx:cache>
		         """,
		    context, BoxSourceType.BOXTEMPLATE );

		assertTrue( variables.get( result ) instanceof String );
		assertTrue( variables.getAsString( result ).length() > 0 );
		assertEquals( variables.getAsString( result ).trim(), "<h1>Hello bar!</h1>" );
	}

	@DisplayName( "It tests the Component BXCache with content between tags and the optimal action" )
	@Test
	public void testComponentBXContentOptimal() {
		instance.executeSource(
		    """
		         <bx:cache action="optimal" name="result">
		      	<bx:script>
		      		actual = "baz";
		      	</bx:script>
		    <bx:output>
		      	<h1>Hello #actual#!</h1>
		    </bx:output>
		      </bx:cache>
		         """,
		    context, BoxSourceType.BOXTEMPLATE );

		assertTrue( variables.get( result ) instanceof String );
		assertTrue( variables.getAsString( result ).length() > 0 );

		assertEquals( variables.getAsString( result ).trim(), "<h1>Hello baz!</h1>" );
	}

	@DisplayName( "It tests the Component BXCache with content between tags and the content action" )
	@Test
	public void testComponentBXContentAction() {
		instance.executeSource(
		    """
		         <bx:cache action="content" name="result">
		      	<bx:script>
		      		actual = "blah";
		      	</bx:script>
		    <bx:output>
		      	<h1>Hello #actual#!</h1>
		    </bx:output>
		      </bx:cache>
		         """,
		    context, BoxSourceType.BOXTEMPLATE );

		assertTrue( variables.get( result ) instanceof String );
		assertTrue( variables.getAsString( result ).length() > 0 );

		assertEquals( variables.getAsString( result ).trim(), "<h1>Hello blah!</h1>" );
	}

	@DisplayName( "It tests the Component BXCache a directory caching" )
	@Test
	public void testComponentBXContentDirectory() {
		variables.put( Key.of( "tmpDirectory" ), tmpDirectory );
		instance.executeSource(
		    """
		         <bx:cache action="cache" name="result" directory="#tmpDirectory#">
		      	<bx:script>
		      		actual = "bar";
		      	</bx:script>
		    <bx:output>
		      	<h1>Hello #actual#!</h1>
		    </bx:output>
		      </bx:cache>
		         """,
		    context, BoxSourceType.BOXTEMPLATE );

		assertTrue( variables.get( result ) instanceof String );
		assertTrue( variables.getAsString( result ).length() > 0 );

		assertEquals( variables.getAsString( result ).trim(), "<h1>Hello bar!</h1>" );
	}

	@DisplayName( "It can flush a single item from a named cache" )
	@Test
	public void testComponentBXDirectorySingleItemFlush() {

		variables.put( Key.cacheName, testCacheKey.getName() );
		instance.executeSource(
		    """
		       bx:cache action="put" key="foo" cachename="#cachename#" value="bar";
		    bx:cache action="get" key="foo" cachename="#cachename#" name="result";
		       bx:cache action="flush" key="foo";
		       """,
		    context, BoxSourceType.BOXSCRIPT );

		assertTrue( variables.get( result ) instanceof String );
		assertTrue( variables.getAsString( result ).length() > 0 );
		assertEquals( variables.getAsString( result ), "bar" );
		assertFalse( variables.containsKey( Key.of( "result2" ) ) );

	}

	@DisplayName( "It can flush a directory cache" )
	@Test
	public void testComponentBXDirectoryFlush() {
		variables.put( Key.of( "tmpDirectory" ), tmpDirectory );
		instance.executeSource(
		    """
		         <bx:cache action="cache" name="result" directory="#tmpDirectory#">
		      	<bx:script>
		      		actual = "deletion";
		      	</bx:script>
		    <bx:output>
		      	<h1>Hello #actual#!</h1>
		    </bx:output>
		      </bx:cache>
		         """,
		    context, BoxSourceType.BOXTEMPLATE );

		instance.executeSource(
		    """
		       <bx:cache action="flush" directory="#tmpDirectory#"/>
		    <bx:script>
		    sleep( 100 );
		    </bx:script>
		       """,
		    context, BoxSourceType.BOXTEMPLATE );

		try {
			assertEquals( 0, Files.walk( Path.of( tmpDirectory ) ).filter( path -> Files.isRegularFile( path ) ).count() );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}

	}

}
