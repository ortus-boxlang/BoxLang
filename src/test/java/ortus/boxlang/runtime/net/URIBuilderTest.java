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

import static com.google.common.truth.Truth.assertThat;
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

	@DisplayName( "Test empty constructor creates URIBuilder with empty base path" )
	@Test
	void testEmptyConstructor() throws URISyntaxException {
		URIBuilder	builder	= new URIBuilder();
		URI			result	= builder.build();
		assertThat( result.toString() ).isEmpty();
	}

	@DisplayName( "Test string constructor with simple URL" )
	@Test
	void testStringConstructorSimpleURL() throws URISyntaxException {
		URIBuilder	builder	= new URIBuilder( "http://example.com/api" );
		URI			result	= builder.build();
		assertEquals( "http://example.com/api", result.toString() );
	}

	@DisplayName( "Test string constructor with URL and existing query parameters" )
	@Test
	void testStringConstructorWithQueryParams() throws URISyntaxException {
		URIBuilder	builder	= new URIBuilder( "http://example.com/api?existing=param&another=value" );
		URI			result	= builder.build();
		assertEquals( "http://example.com/api?existing=param&another=value", result.toString() );
	}

	@DisplayName( "Test URI constructor" )
	@Test
	void testURIConstructor() throws URISyntaxException {
		URI			original	= new URI( "http://example.com/test?key=value" );
		URIBuilder	builder		= new URIBuilder( original );
		URI			result		= builder.build();
		assertEquals( "http://example.com/test?key=value", result.toString() );
	}

	@DisplayName( "Test addParameter with single parameter" )
	@Test
	void testAddParameterSingle() throws URISyntaxException {
		URIBuilder builder = new URIBuilder( "http://example.com/api" );
		builder.addParameter( "key", "value" );
		URI result = builder.build();
		assertEquals( "http://example.com/api?key=value", result.toString() );
	}

	@DisplayName( "Test addParameter with multiple parameters" )
	@Test
	void testAddParameterMultiple() throws URISyntaxException {
		URIBuilder builder = new URIBuilder( "http://example.com/api" );
		builder.addParameter( "key1", "value1" );
		builder.addParameter( "key2", "value2" );
		builder.addParameter( "key3", "value3" );
		URI result = builder.build();
		assertEquals( "http://example.com/api?key1=value1&key2=value2&key3=value3", result.toString() );
	}

	@DisplayName( "Test addParameter with null value" )
	@Test
	void testAddParameterNullValue() throws URISyntaxException {
		URIBuilder builder = new URIBuilder( "http://example.com/api" );
		builder.addParameter( "flag", null );
		URI result = builder.build();
		assertThat( result.toString() ).contains( "flag" );
	}

	@DisplayName( "Test addParameter with duplicate parameter names" )
	@Test
	void testAddParameterDuplicateNames() throws URISyntaxException {
		URIBuilder builder = new URIBuilder( "http://example.com/api" );
		builder.addParameter( "tags", "java" );
		builder.addParameter( "tags", "programming" );
		builder.addParameter( "tags", "boxlang" );
		URI result = builder.build();
		assertEquals( "http://example.com/api?tags=java&tags=programming&tags=boxlang", result.toString() );
	}

	@DisplayName( "Test addParameter with special characters in value" )
	@Test
	void testAddParameterSpecialCharacters() throws URISyntaxException {
		URIBuilder builder = new URIBuilder( "http://example.com/api" );
		builder.addParameter( "name", "JohnDoe" );
		builder.addParameter( "filter", "status=active" );
		URI result = builder.build();
		assertThat( result.toString() ).contains( "name=JohnDoe" );
		assertThat( result.toString() ).contains( "filter=status=active" );
	}

	@DisplayName( "Test setPort changes port number" )
	@Test
	void testSetPort() throws URISyntaxException {
		URIBuilder builder = new URIBuilder( "http://example.com/api" );
		builder.setPort( 8080 );
		URI result = builder.build();
		assertEquals( "http://example.com:8080/api", result.toString() );
	}

	@DisplayName( "Test setPort with existing port" )
	@Test
	void testSetPortReplaceExisting() throws URISyntaxException {
		URIBuilder builder = new URIBuilder( "http://example.com:80/api" );
		builder.setPort( 9000 );
		URI result = builder.build();
		assertEquals( "http://example.com:9000/api", result.toString() );
	}

	@DisplayName( "Test setPort with HTTPS" )
	@Test
	void testSetPortHTTPS() throws URISyntaxException {
		URIBuilder builder = new URIBuilder( "https://secure.example.com/api" );
		builder.setPort( 8443 );
		URI result = builder.build();
		assertEquals( "https://secure.example.com:8443/api", result.toString() );
	}

	@DisplayName( "Test setPort with complete URI" )
	@Test
	void testSetPortWithCompleteURI() throws URISyntaxException {
		URIBuilder builder = new URIBuilder( "http://example.com/api" );
		builder.setPort( 9000 );
		URI result = builder.build();
		assertThat( result.getPort() ).isEqualTo( 9000 );
		assertThat( result.toString() ).startsWith( "http://example.com:9000" );
	}

	@DisplayName( "Test build with no parameters" )
	@Test
	void testBuildNoParameters() throws URISyntaxException {
		URIBuilder	builder	= new URIBuilder( "http://example.com/api/v1/users" );
		URI			result	= builder.build();
		assertEquals( "http://example.com/api/v1/users", result.toString() );
	}

	@DisplayName( "Test build preserves existing and new parameters" )
	@Test
	void testBuildPreservesExistingAndNewParameters() throws URISyntaxException {
		URIBuilder builder = new URIBuilder( "http://example.com/search?existing=value" );
		builder.addParameter( "new", "param" );
		URI result = builder.build();
		assertEquals( "http://example.com/search?existing=value&new=param", result.toString() );
	}

	@DisplayName( "Test build with relative path" )
	@Test
	void testBuildRelativePath() throws URISyntaxException {
		URIBuilder builder = new URIBuilder( "/api/endpoint" );
		builder.addParameter( "id", "123" );
		URI result = builder.build();
		assertEquals( "/api/endpoint?id=123", result.toString() );
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

	@DisplayName( "Tests that the URI Builder will adhere to RFC 3986 and not throw an error or mutate allowed path characters which are pre-encoded" )
	@Test
	void testAllowedPathCharacters() throws URISyntaxException {
		URIBuilder	builder	= new URIBuilder(
		    "http://localhost:8080/chaos-monkey/exam%2520p%20%20%20le%20%28fo%252Fo%29%2B%2C%21%40%23%24%25%5E%26%2A%28%29_%2B~%20%3B%3A.txt" );
		URI			result	= builder.build();

		assertEquals( "http://localhost:8080/chaos-monkey/exam%2520p%20%20%20le%20%28fo%252Fo%29%2B%2C%21%40%23%24%25%5E%26%2A%28%29_%2B~%20%3B%3A.txt",
		    result.toString() );
	}

	@DisplayName( "Test parseQueryString with empty parameters" )
	@Test
	void testParseQueryStringEmptyParameters() throws URISyntaxException {
		URIBuilder	builder	= new URIBuilder( "http://example.com/test?" );
		URI			result	= builder.build();
		assertEquals( "http://example.com/test", result.toString() );
	}

	@DisplayName( "Test parseQueryString with parameter without value" )
	@Test
	void testParseQueryStringParameterWithoutValue() throws URISyntaxException {
		URIBuilder	builder	= new URIBuilder( "http://example.com/test?flag&other=value" );
		URI			result	= builder.build();
		assertThat( result.toString() ).contains( "flag" );
		assertThat( result.toString() ).contains( "other=value" );
	}

	@DisplayName( "Test complex URL with user info, port, path, and parameters" )
	@Test
	void testComplexURL() throws URISyntaxException {
		URIBuilder builder = new URIBuilder( "http://user:pass@example.com:8080/path/to/resource?existing=param" );
		builder.addParameter( "new", "value" );
		URI result = builder.build();
		assertThat( result.toString() ).contains( "user:pass@example.com:8080" );
		assertThat( result.toString() ).contains( "existing=param" );
		assertThat( result.toString() ).contains( "new=value" );
	}

	@DisplayName( "Test with localhost and custom port" )
	@Test
	void testLocalhostWithCustomPort() throws URISyntaxException {
		URIBuilder builder = new URIBuilder( "http://localhost/api" );
		builder.setPort( 3000 );
		builder.addParameter( "debug", "true" );
		URI result = builder.build();
		assertEquals( "http://localhost:3000/api?debug=true", result.toString() );
	}

	@DisplayName( "Test with IPv4 address" )
	@Test
	void testIPv4Address() throws URISyntaxException {
		URIBuilder builder = new URIBuilder( "http://192.168.1.1/api" );
		builder.setPort( 8080 );
		builder.addParameter( "key", "value" );
		URI result = builder.build();
		assertEquals( "http://192.168.1.1:8080/api?key=value", result.toString() );
	}

	@DisplayName( "Test with fragment (hash) in URL" )
	@Test
	void testURLWithFragment() throws URISyntaxException {
		URIBuilder	builder	= new URIBuilder( "http://example.com/page#section" );
		URI			result	= builder.build();
		assertThat( result.toString() ).isEqualTo( "http://example.com/page#section" );
	}

	@DisplayName( "Test parameter ordering is preserved" )
	@Test
	void testParameterOrderingPreserved() throws URISyntaxException {
		URIBuilder builder = new URIBuilder( "http://example.com/api" );
		builder.addParameter( "first", "1" );
		builder.addParameter( "second", "2" );
		builder.addParameter( "third", "3" );
		URI result = builder.build();
		assertEquals( "http://example.com/api?first=1&second=2&third=3", result.toString() );
	}

	@DisplayName( "Test empty string parameter value" )
	@Test
	void testEmptyStringParameterValue() throws URISyntaxException {
		URIBuilder builder = new URIBuilder( "http://example.com/api" );
		builder.addParameter( "empty", "" );
		builder.addParameter( "other", "value" );
		URI result = builder.build();
		assertThat( result.toString() ).contains( "empty=" );
		assertThat( result.toString() ).contains( "other=value" );
	}

}
