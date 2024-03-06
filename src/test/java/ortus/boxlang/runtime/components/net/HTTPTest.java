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

package ortus.boxlang.runtime.components.net;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.parser.BoxScriptType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;

public class HTTPTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It can make HTTP call script" )
	@Test
	public void testCanMakeHTTPCallScript() {
		instance.executeSource(
		    """
		     http url="https://jsonplaceholder.typicode.com/posts/1" {
		         httpparam type="header" name="User-Agent" value="Mozilla";
		    }
		    result = cfhttp;
		     """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct cfhttp = variables.getAsStruct( result );
		assertThat( cfhttp.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( cfhttp.get( Key.statusText ) ).isEqualTo( "OK" );
		assertThat( cfhttp.getAsString( Key.fileContent ).replaceAll( "\\s+", "" ) ).isEqualTo(
		    """
		    {
		      "userId": 1,
		      "id": 1,
		      "title": "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
		      "body": "quia et suscipit\\nsuscipit recusandae consequuntur expedita et cum\\nreprehenderit molestiae ut ut quas totam\\nnostrum rerum est autem sunt rem eveniet architecto"
		    }
		    """.replaceAll(
		        "\\s+", "" ) );
	}

	@DisplayName( "It can make HTTP call ACF script" )
	@Test
	public void testCanMakeHTTPCallACFScript() {

		instance.executeSource(
		    """
		     cfhttp( url="https://jsonplaceholder.typicode.com/posts/1" ) {
		         cfhttpparam( type="header", name="User-Agent", value="Mozilla");
		    }
		    result = cfhttp;
		     """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct cfhttp = variables.getAsStruct( result );
		assertThat( cfhttp.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( cfhttp.get( Key.statusText ) ).isEqualTo( "OK" );
		assertThat( cfhttp.getAsString( Key.fileContent ).replaceAll( "\\s+", "" ) ).isEqualTo(
		    """
		    {
		      "userId": 1,
		      "id": 1,
		      "title": "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
		      "body": "quia et suscipit\\nsuscipit recusandae consequuntur expedita et cum\\nreprehenderit molestiae ut ut quas totam\\nnostrum rerum est autem sunt rem eveniet architecto"
		    }
		    """.replaceAll(
		        "\\s+", "" ) );
	}

	@DisplayName( "It can make a default GET request" )
	@Test
	public void testCanMakeHTTPCallTag() {
		instance.executeSource(
		    """
		      <cfhttp url="https://jsonplaceholder.typicode.com/posts/1">
		          <cfhttpparam type="header" name="User-Agent" value="Mozilla" />
		      </cfhttp>
		    <cfset result = cfhttp>
		      """,
		    context, BoxScriptType.CFMARKUP );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct cfhttp = variables.getAsStruct( result );
		assertThat( cfhttp.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( cfhttp.get( Key.statusText ) ).isEqualTo( "OK" );
		assertThat( cfhttp.getAsString( Key.fileContent ).replaceAll( "\\s+", "" ) ).isEqualTo(
		    """
		    {
		      "userId": 1,
		      "id": 1,
		      "title": "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
		      "body": "quia et suscipit\\nsuscipit recusandae consequuntur expedita et cum\\nreprehenderit molestiae ut ut quas totam\\nnostrum rerum est autem sunt rem eveniet architecto"
		    }
		    """.replaceAll(
		        "\\s+", "" ) );
	}

	@DisplayName( "It can make HTTP call tag attributeCollection" )
	@Test
	public void testCanMakeHTTPCallTagAttributeCollection() {

		instance.executeSource(
		    """
		    <cfset attrs = { type="header", name="Accept-Encoding", value="gzip,deflate" }>
		       <cfhttp url="https://jsonplaceholder.typicode.com/posts/1">
		       	<cfhttpparam attributeCollection="#attrs#" value="sdf" />
		       </cfhttp>
		       <cfset result = cfhttp>
		         """,
		    context, BoxScriptType.CFMARKUP );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct cfhttp = variables.getAsStruct( result );
		assertThat( cfhttp.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( cfhttp.get( Key.statusText ) ).isEqualTo( "OK" );
		assertThat( cfhttp.getAsString( Key.fileContent ).replaceAll( "\\s+", "" ) ).isEqualTo(
		    """
		    {
		      "userId": 1,
		      "id": 1,
		      "title": "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
		      "body": "quia et suscipit\\nsuscipit recusandae consequuntur expedita et cum\\nreprehenderit molestiae ut ut quas totam\\nnostrum rerum est autem sunt rem eveniet architecto"
		    }
		    """.replaceAll(
		        "\\s+", "" ) );

	}

}
