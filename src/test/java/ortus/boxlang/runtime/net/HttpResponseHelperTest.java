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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;

class HttpResponseHelperTest {

	@Test
	@DisplayName( "It can extract first header value from single-value header" )
	void testExtractFirstHeaderByName_singleValue() {
		IStruct headers = new Struct();
		headers.put( Key.contentType, "text/html" );

		String result = HttpResponseHelper.extractFirstHeaderByName( headers, Key.contentType );

		assertThat( result ).isEqualTo( "text/html" );
	}

	@Test
	@DisplayName( "It can extract first header value from multi-value header array" )
	void testExtractFirstHeaderByName_multiValue() {
		IStruct	headers	= new Struct();
		Array	values	= new Array();
		values.add( "value1" );
		values.add( "value2" );
		headers.put( Key.of( "Custom-Header" ), values );

		String result = HttpResponseHelper.extractFirstHeaderByName( headers, Key.of( "Custom-Header" ) );

		assertThat( result ).isEqualTo( "value1" );
	}

	@Test
	@DisplayName( "It returns null for missing header" )
	void testExtractFirstHeaderByName_missing() {
		IStruct	headers	= new Struct();

		String	result	= HttpResponseHelper.extractFirstHeaderByName( headers, Key.contentType );

		assertThat( result ).isNull();
	}

	@Test
	@DisplayName( "It returns null for empty array header" )
	void testExtractFirstHeaderByName_emptyArray() {
		IStruct	headers	= new Struct();
		Array	values	= new Array();
		headers.put( Key.of( "Empty-Header" ), values );

		String result = HttpResponseHelper.extractFirstHeaderByName( headers, Key.of( "Empty-Header" ) );

		assertThat( result ).isNull();
	}

	@Test
	@DisplayName( "It can generate cookies query from Set-Cookie header" )
	void testGenerateCookiesQuery_singleCookie() {
		IStruct headers = new Struct();
		headers.put( Key.of( "Set-Cookie" ), "sessionid=abc123; Path=/; Secure; HttpOnly" );

		Query cookies = HttpResponseHelper.generateCookiesQuery( headers );

		assertThat( cookies.size() ).isEqualTo( 1 );
		assertThat( cookies.getRowAsStruct( 0 ).getAsString( Key._NAME ) ).isEqualTo( "sessionid" );
		assertThat( cookies.getRowAsStruct( 0 ).getAsString( Key.value ) ).isEqualTo( "abc123" );
		assertThat( cookies.getRowAsStruct( 0 ).get( Key.path ) ).isEqualTo( "/" );
		assertThat( cookies.getRowAsStruct( 0 ).get( Key.secure ) ).isEqualTo( true );
		assertThat( cookies.getRowAsStruct( 0 ).get( Key.httpOnly ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "It can generate cookies query from multiple Set-Cookie headers" )
	void testGenerateCookiesQuery_multipleCookies() {
		IStruct	headers	= new Struct();
		Array	cookies	= new Array();
		cookies.add( "cookie1=value1; Path=/" );
		cookies.add( "cookie2=value2; Domain=example.com" );
		headers.put( Key.of( "Set-Cookie" ), cookies );

		Query result = HttpResponseHelper.generateCookiesQuery( headers );

		assertThat( result.size() ).isEqualTo( 2 );
		assertThat( result.getRowAsStruct( 0 ).getAsString( Key._NAME ) ).isEqualTo( "cookie1" );
		assertThat( result.getRowAsStruct( 1 ).getAsString( Key._NAME ) ).isEqualTo( "cookie2" );
	}

	@Test
	@DisplayName( "It handles max-age conversion to expires" )
	void testGenerateCookiesQuery_maxAge() {
		IStruct headers = new Struct();
		headers.put( Key.of( "Set-Cookie" ), "sessionid=xyz; max-age=86400" );

		Query cookies = HttpResponseHelper.generateCookiesQuery( headers );

		assertThat( cookies.size() ).isEqualTo( 1 );
		// max-age 86400 seconds = 1 day
		assertThat( cookies.getRowAsStruct( 0 ).getAsString( Key.expires ) ).isEqualTo( "1" );
	}

	@Test
	@DisplayName( "It handles empty Set-Cookie header" )
	void testGenerateCookiesQuery_empty() {
		IStruct headers = new Struct();
		headers.put( Key.of( "Set-Cookie" ), new Array() );

		Query cookies = HttpResponseHelper.generateCookiesQuery( headers );

		assertThat( cookies.size() ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "It can parse cookie string with SameSite attribute" )
	void testParseCookieStringIntoQuery_sameSite() {
		Query	cookies			= new Query();
		String	cookieString	= "token=abc123; Path=/; SameSite=Strict";

		cookies.addColumn( Key._NAME, ortus.boxlang.runtime.types.QueryColumnType.VARCHAR );
		cookies.addColumn( Key.value, ortus.boxlang.runtime.types.QueryColumnType.VARCHAR );
		cookies.addColumn( Key.path, ortus.boxlang.runtime.types.QueryColumnType.VARCHAR );
		cookies.addColumn( Key.domain, ortus.boxlang.runtime.types.QueryColumnType.VARCHAR );
		cookies.addColumn( Key.expires, ortus.boxlang.runtime.types.QueryColumnType.VARCHAR );
		cookies.addColumn( Key.secure, ortus.boxlang.runtime.types.QueryColumnType.VARCHAR );
		cookies.addColumn( Key.httpOnly, ortus.boxlang.runtime.types.QueryColumnType.VARCHAR );
		cookies.addColumn( Key.samesite, ortus.boxlang.runtime.types.QueryColumnType.VARCHAR );

		HttpResponseHelper.parseCookieStringIntoQuery( cookieString, cookies );

		assertThat( cookies.size() ).isEqualTo( 1 );
		assertThat( cookies.getRowAsStruct( 0 ).get( Key.samesite ) ).isEqualTo( "Strict" );
	}

	@Test
	@DisplayName( "It ignores malformed cookie strings" )
	void testParseCookieStringIntoQuery_malformed() {
		Query	cookies			= new Query();
		String	cookieString	= "invalid-cookie-no-equals";

		cookies.addColumn( Key._NAME, ortus.boxlang.runtime.types.QueryColumnType.VARCHAR );
		cookies.addColumn( Key.value, ortus.boxlang.runtime.types.QueryColumnType.VARCHAR );
		cookies.addColumn( Key.path, ortus.boxlang.runtime.types.QueryColumnType.VARCHAR );
		cookies.addColumn( Key.domain, ortus.boxlang.runtime.types.QueryColumnType.VARCHAR );
		cookies.addColumn( Key.expires, ortus.boxlang.runtime.types.QueryColumnType.VARCHAR );
		cookies.addColumn( Key.secure, ortus.boxlang.runtime.types.QueryColumnType.VARCHAR );
		cookies.addColumn( Key.httpOnly, ortus.boxlang.runtime.types.QueryColumnType.VARCHAR );
		cookies.addColumn( Key.samesite, ortus.boxlang.runtime.types.QueryColumnType.VARCHAR );

		HttpResponseHelper.parseCookieStringIntoQuery( cookieString, cookies );

		assertThat( cookies.size() ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "It can generate status line" )
	void testGenerateStatusLine() {
		String result = HttpResponseHelper.generateStatusLine( "HTTP/1.1", "200", "OK" );

		assertThat( result ).isEqualTo( "HTTP/1.1 200 OK" );
	}

	@Test
	@DisplayName( "It can generate status line for HTTP/2" )
	void testGenerateStatusLine_http2() {
		String result = HttpResponseHelper.generateStatusLine( "HTTP/2", "404", "Not Found" );

		assertThat( result ).isEqualTo( "HTTP/2 404 Not Found" );
	}

	@Test
	@DisplayName( "It can generate header string from status line and headers" )
	void testGenerateHeaderString_singleValueHeaders() {
		IStruct headers = new Struct();
		headers.put( Key.contentType, "text/html" );
		headers.put( Key.of( "Content-Length" ), "1234" );

		String result = HttpResponseHelper.generateHeaderString( "HTTP/1.1 200 OK", headers );

		assertThat( result ).contains( "HTTP/1.1 200 OK" );
		assertThat( result ).contains( "Content-Length: 1234" );
		assertThat( result ).contains( "content-type: text/html" );
	}

	@Test
	@DisplayName( "It can generate header string with multi-value headers" )
	void testGenerateHeaderString_multiValueHeaders() {
		IStruct	headers	= new Struct();
		Array	values	= new Array();
		values.add( "value1" );
		values.add( "value2" );
		headers.put( Key.of( "Set-Cookie" ), values );

		String result = HttpResponseHelper.generateHeaderString( "HTTP/1.1 200 OK", headers );

		assertThat( result ).contains( "Set-Cookie: value1" );
		assertThat( result ).contains( "Set-Cookie: value2" );
	}

	@Test
	@DisplayName( "It can transform headers map to response header struct" )
	void testTransformToResponseHeaderStruct_singleValue() {
		Map<String, List<String>> headersMap = new HashMap<>();
		headersMap.put( "Content-Type", Arrays.asList( "text/html" ) );
		headersMap.put( "Content-Length", Arrays.asList( "1234" ) );

		IStruct result = HttpResponseHelper.transformToResponseHeaderStruct( headersMap );

		assertThat( result.size() ).isEqualTo( 2 );
		assertThat( result.getAsString( Key.of( "Content-Type" ) ) ).isEqualTo( "text/html" );
		assertThat( result.getAsString( Key.of( "Content-Length" ) ) ).isEqualTo( "1234" );
	}

	@Test
	@DisplayName( "It converts single-value arrays to strings in response headers" )
	void testTransformToResponseHeaderStruct_singleValueArrayToString() {
		Map<String, List<String>> headersMap = new HashMap<>();
		headersMap.put( "Content-Type", Arrays.asList( "application/json" ) );

		IStruct result = HttpResponseHelper.transformToResponseHeaderStruct( headersMap );

		// Single-value array should be converted to string
		assertThat( result.get( Key.of( "Content-Type" ) ) ).isInstanceOf( String.class );
		assertThat( result.getAsString( Key.of( "Content-Type" ) ) ).isEqualTo( "application/json" );
	}

	@Test
	@DisplayName( "It keeps multi-value arrays as arrays in response headers" )
	void testTransformToResponseHeaderStruct_multiValue() {
		Map<String, List<String>> headersMap = new HashMap<>();
		headersMap.put( "Set-Cookie", Arrays.asList( "cookie1=value1", "cookie2=value2" ) );

		IStruct result = HttpResponseHelper.transformToResponseHeaderStruct( headersMap );

		assertThat( result.get( Key.of( "Set-Cookie" ) ) ).isInstanceOf( Array.class );
		Array cookies = ( Array ) result.get( Key.of( "Set-Cookie" ) );
		assertThat( cookies.size() ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "It filters out HTTP/2 pseudo-headers" )
	void testTransformToResponseHeaderStruct_filtersPseudoHeaders() {
		Map<String, List<String>> headersMap = new HashMap<>();
		headersMap.put( ":status", Arrays.asList( "200" ) );
		headersMap.put( "Content-Type", Arrays.asList( "text/html" ) );

		IStruct result = HttpResponseHelper.transformToResponseHeaderStruct( headersMap );

		assertThat( result.containsKey( Key.of( ":status" ) ) ).isFalse();
		assertThat( result.containsKey( Key.of( "Content-Type" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "It returns empty struct for null headers map" )
	void testTransformToResponseHeaderStruct_null() {
		IStruct result = HttpResponseHelper.transformToResponseHeaderStruct( null );

		assertThat( result ).isNotNull();
		assertThat( result.size() ).isEqualTo( 0 );
	}

	@Test
	@DisplayName( "It can extract charset from Content-Type header" )
	void testExtractCharset_utf8() {
		String result = HttpResponseHelper.extractCharset( "text/html; charset=UTF-8" );

		assertThat( result ).isEqualTo( "UTF-8" );
	}

	@Test
	@DisplayName( "It can extract charset from Content-Type with spaces" )
	void testExtractCharset_withSpaces() {
		String result = HttpResponseHelper.extractCharset( "application/json;charset=ISO-8859-1" );

		assertThat( result ).isEqualTo( "ISO-8859-1" );
	}

	@Test
	@DisplayName( "It returns null for Content-Type without charset" )
	void testExtractCharset_noCharset() {
		String result = HttpResponseHelper.extractCharset( "text/html" );

		assertThat( result ).isNull();
	}

	@Test
	@DisplayName( "It returns null for null Content-Type" )
	void testExtractCharset_null() {
		String result = HttpResponseHelper.extractCharset( null );

		assertThat( result ).isNull();
	}

	@Test
	@DisplayName( "It returns null for empty Content-Type" )
	void testExtractCharset_empty() {
		String result = HttpResponseHelper.extractCharset( "" );

		assertThat( result ).isNull();
	}

	@Test
	@DisplayName( "It can extract various charset encodings" )
	void testExtractCharset_variousEncodings() {
		assertThat( HttpResponseHelper.extractCharset( "text/html; charset=windows-1252" ) ).isEqualTo( "windows-1252" );
		assertThat( HttpResponseHelper.extractCharset( "application/xml; charset=UTF-16" ) ).isEqualTo( "UTF-16" );
		assertThat( HttpResponseHelper.extractCharset( "text/plain; charset=ASCII" ) ).isEqualTo( "ASCII" );
	}
}
