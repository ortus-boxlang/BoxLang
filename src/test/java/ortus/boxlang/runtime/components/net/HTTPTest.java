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
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		variables.clear();
	}

	@DisplayName( "It can make HTTP call script" )
	@Test
	public void testCanMakeHTTPCallScript() {

		instance.executeSource(
		    """
		     http url="http://www.google.com" {
		         httpparam type="header" name="Accept-Encoding" value="gzip,deflate";
		         httpparam type="header" name="Accept" value="text/html";
		         httpparam type="header" name="Accept-Language" value="en-US,en;q=0.5";
		         httpparam type="header" name="User-Agent" value="Mozilla";
		    }
		    result = cfhttp;
		     """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct cfhttp = variables.getAsStruct( result );
		assertThat( cfhttp.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( cfhttp.get( Key.statusText ) ).isEqualTo( "OK" );
		assertThat( cfhttp.get( Key.fileContent ) ).isEqualTo( "This is the response text" );
	}

	@DisplayName( "It can make HTTP call ACF script" )
	@Test
	public void testCanMakeHTTPCallACFScript() {

		instance.executeSource(
		    """
		     cfhttp( url="http://www.google.com" ) {
		         cfhttpparam( type="header", name="Accept-Encoding", value="gzip,deflate");
		         cfhttpparam( type="header", name="Accept", value="text/html");
		         cfhttpparam( type="header", name="Accept-Language", value="en-US,en;q=0.5");
		         cfhttpparam( type="header", name="User-Agent", value="Mozilla");
		    }
		    result = cfhttp;
		     """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct cfhttp = variables.getAsStruct( result );
		assertThat( cfhttp.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( cfhttp.get( Key.statusText ) ).isEqualTo( "OK" );
		assertThat( cfhttp.get( Key.fileContent ) ).isEqualTo( "This is the response text" );
	}

	@DisplayName( "It can make HTTP call tag" )
	@Test
	public void testCanMakeHTTPCallTag() {

		instance.executeSource(
		    """
		       <cfhttp url="http://www.google.com">
		           <cfhttpparam type="header" name="Accept-Encoding" value="gzip,deflate" />
		           <cfhttpparam type="header" name="Accept" value="text/html" />
		           <cfhttpparam type="header" name="Accept-Language" value="en-US,en;q=0.5" />
		           <cfhttpparam type="header" name="User-Agent" value="Mozilla" />
		       </cfhttp>
		    <cfset result = cfhttp>
		       """,
		    context, BoxScriptType.CFMARKUP );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct cfhttp = variables.getAsStruct( result );
		assertThat( cfhttp.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( cfhttp.get( Key.statusText ) ).isEqualTo( "OK" );
		assertThat( cfhttp.get( Key.fileContent ) ).isEqualTo( "This is the response text" );

	}

}
