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
package ortus.boxlang.runtime.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;

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

public class URIBuilderTest {

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

	@DisplayName( "Tests that the URI Builder will not double encode query params" )
	@Test
	void testEncodeOnce() throws URISyntaxException {
		URIBuilder builder = new URIBuilder( "http://localhost:8080/test" );
		builder.addParameter( "prefix", "A6953938-B4ED-4506-B878ADFB7E67A6E2%2Fone%2F" );
		builder.addParameter( "delimiter", "%2F" );

		URI result = builder.build();
		assertEquals( "http://localhost:8080/test?prefix=A6953938-B4ED-4506-B878ADFB7E67A6E2%2Fone%2F&delimiter=%2F", result.toString() );
	}

	@DisplayName( "Tests that the URI Builder will adhere to RFC 3986 and not throw an error with allowed path characters" )
	@Test
	void testAllowedPathCharacters() throws URISyntaxException {
		URIBuilder	builder	= new URIBuilder(
		    "http://localhost:8080/chaos-monkey/exam%2520p%20%20%20le%20(fo%252Fo)+,!@%23$%25%5E&*()_+~%20;:.txt" );
		URI			result	= builder.build();

		assertEquals( "http://localhost:8080/chaos-monkey/exam%2520p%20%20%20le%20(fo%252Fo)+,!@%23$%25%5E&*()_+~%20;:.txt",
		    result.toString() );
	}

	@DisplayName( "Tests that the URI Builder will decode allowed RFC 3986 characters " )
	@Test
	void testDecodeAllowedPathCharacters() throws URISyntaxException {
		URIBuilder	builder	= new URIBuilder(
		    "http://localhost:8080/chaos-monkey/exam%2520p%20%20%20le%20%28fo%252Fo%29%2B%2C%21%40%23%24%25%5E%26%2A%28%29_%2B~%20%3B%3A.txt" );
		URI			result	= builder.build();

		assertEquals( "http://localhost:8080/chaos-monkey/exam%2520p%20%20%20le%20(fo%252Fo)+,!@%23$%25%5E&*()_+~%20;:.txt",
		    result.toString() );
	}

}
