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

import static com.github.tomakehurst.wiremock.client.WireMock.aMultipart;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.havingExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;

@WireMockTest
public class HTTPTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );
	static Key			bxhttp	= new Key( "bxhttp" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
		if ( FileSystemUtil.exists( "src/test/resources/tmp/http_tests" ) ) {
			FileSystemUtil.deleteDirectory( "src/test/resources/tmp/http_tests", true );
		}
	}

	@AfterAll
	public static void teardown() {
		if ( FileSystemUtil.exists( "src/test/resources/tmp/http_tests" ) ) {
			FileSystemUtil.deleteDirectory( "src/test/resources/tmp/http_tests", true );
		}
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It can make HTTP call script" )
	@Test
	public void testCanMakeHTTPCallScript( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor( get( "/posts/1" )
		    .willReturn(
		        aResponse()
		            .withBody( "Done" )
		            .withStatus( 200 ) ) );

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();

		// @formatter:off
		instance.executeSource(
		    String.format( """
					bx:http url="%s" {
						bx:httpparam type="header" name="User-Agent" value="Mozilla";
					}
					result = bxhttp
				""",
		        baseURL + "/posts/1"
		    ),
		    context
		);
		// @formatter:on

		IStruct bxhttp = variables.getAsStruct( result );
		assertThat( bxhttp.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( bxhttp.get( Key.statusText ) ).isEqualTo( "OK" );
		assertThat( bxhttp.getAsString( Key.fileContent ).replaceAll( "\\s+", "" ) ).isEqualTo( "Done" );
	}

	@DisplayName( "It can make HTTP with port attribute" )
	@Test
	public void testCanMakeHTTPRequestPort( WireMockRuntimeInfo wmRuntimeInfo ) {
		// this is the good request - it would return a 200
		stubFor( get( "/posts/1" )
		    .willReturn(
		        aResponse()
		            .withBody( "Done" )
		            .withStatus( 200 ) )
		);
		String baseURL = wmRuntimeInfo.getHttpBaseUrl();

		// @formatter:off
		instance.executeSource( String.format( """
			bx:http url="%s" port="12345" {
				bx:httpparam type="header" name="User-Agent" value="Mozilla";
			}
			result = bxhttp;
			""", baseURL + "/posts/1" ),
		    context );
		// @formatter:on

		// Our port change should produce a bad gateway error
		IStruct bxhttp = variables.getAsStruct( result );
		assertThat( bxhttp.get( Key.statusCode ) ).isEqualTo( 502 );
		assertThat( bxhttp.get( Key.statusText ) ).isEqualTo( "Bad Gateway" );
		assertThat( bxhttp.getAsString( Key.fileContent ).replaceAll( "\\s+", "" ) ).isEqualTo( "ConnectionFailure" );
	}

	@DisplayName( "It parses returned cookies into a Cookies query object" )
	@Test
	public void testCookiesInQuery( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor( get( "/cookies" )
		    .willReturn( ok().withHeader( "Set-Cookie", "foo=bar;path=/;secure;samesite=none;httponly" ) ) );

		instance.executeSource(
		    String.format( "bx:http url=\"%s\" {}", wmRuntimeInfo.getHttpBaseUrl() + "/cookies" ),
		    context
		);

		IStruct httpResult = variables.getAsStruct( bxhttp );
		assertThat( httpResult.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( httpResult.get( Key.statusText ) ).isEqualTo( "OK" );
		Query cookies = httpResult.getAsQuery( Key.cookies );
		Assertions.assertEquals( 1, cookies.size() );
		Object[] row = cookies.getRow( 0 );
		assertThat( row ).isEqualTo( new Object[] { "foo", "bar", "/", "", "", true, true, "none" } );
	}

	@DisplayName( "It parses returned cookies into a Cookies query object" )
	@Test
	public void testMultipleCookies( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor( get( "/cookies" ).willReturn( ok()
		    .withHeader( "Set-Cookie", "foo=bar;path=/;secure;samesite=none;httponly" )
		    .withHeader( "Set-Cookie", "baz=qux;path=/;expires=Mon, 31 Dec 2038 23:59:59 GMT" )
		    .withHeader( "Set-Cookie", "one=two;max-age=2592000;domain=example.com" ) ) );

		instance.executeSource(
		    String.format( "bx:http url=\"%s\" {}", wmRuntimeInfo.getHttpBaseUrl() + "/cookies" ),
		    context
		);

		IStruct httpResult = variables.getAsStruct( bxhttp );
		assertThat( httpResult.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( httpResult.get( Key.statusText ) ).isEqualTo( "OK" );
		Query cookies = httpResult.getAsQuery( Key.cookies );
		Assertions.assertEquals( 3, cookies.size() );
		assertThat( cookies.getRow( 0 ) ).isEqualTo( new Object[] { "foo", "bar", "/", "", "", true, true, "none" } );
		assertThat( cookies.getRow( 1 ) )
		    .isEqualTo( new Object[] { "baz", "qux", "/", "", "Mon, 31 Dec 2038 23:59:59 GMT", "", "", "" } );
		assertThat( cookies.getRow( 2 ) ).isEqualTo( new Object[] { "one", "two", "", "example.com", "30", "", "", "" } );
	}

	@DisplayName( "It can make a post request with form params" )
	@Test
	public void testPostFormParams( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    post( "/posts" )
		        .withFormParam( "name", equalTo( "foobar" ) )
		        .withFormParam( "body", equalTo( "lorem ipsum dolor" ) )
		        .willReturn(
		            created()
		                .withBody( "{\"id\": 1, \"name\": \"foobar\", \"body\": \"lorem ipsum dolor\"}" )
		        )
		);

		// @formatter:off
		instance.executeSource( String.format( """
			bx:http method="POST" url="%s" {
				bx:httpparam type="formfield" name="name" value="foobar";
				bx:httpparam type="formfield" name="body" value="lorem ipsum dolor";
			}
			result = bxhttp;
			println( result )
		""", wmRuntimeInfo.getHttpBaseUrl() + "/posts" ), context );
		// @formatter:on

		IStruct httpResult = variables.getAsStruct( bxhttp );
		assertThat( httpResult.get( Key.statusCode ) ).isEqualTo( 201 );
		assertThat( httpResult.get( Key.statusText ) ).isEqualTo( "Created" );
		String body = httpResult.getAsString( Key.fileContent );
		assertThat( body ).isEqualTo( "{\"id\": 1, \"name\": \"foobar\", \"body\": \"lorem ipsum dolor\"}" );
	}

	@DisplayName( "It can make a post request with form params where one name has multiple values" )
	@Test
	public void testPostFormParamsMultipleValuesForOneName( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    post( "/posts" )
		        .withFormParam( "tags", havingExactly( "tag-a", "tag-b" ) )
		        .willReturn( created().withBody( "{\"id\": 1, \"tags\": [ \"tag-a\", \"tag-b\" ] }" ) ) );

		// @formatter:off
		instance.executeSource( String.format( """
			bx:http method="POST" url="%s" {
				bx:httpparam type="formfield" name="tags" value="tag-a";
				bx:httpparam type="formfield" name="tags" value="tag-b";
			}
		""", wmRuntimeInfo.getHttpBaseUrl() + "/posts" ), context );
		// @formatter:on

		IStruct httpResult = variables.getAsStruct( bxhttp );
		assertThat( httpResult.get( Key.statusCode ) ).isEqualTo( 201 );
		assertThat( httpResult.get( Key.statusText ) ).isEqualTo( "Created" );
		String body = httpResult.getAsString( Key.fileContent );
		assertThat( body ).isEqualTo( "{\"id\": 1, \"tags\": [ \"tag-a\", \"tag-b\" ] }" );
	}

	@DisplayName( "It can make a post request with a json body" )
	@Test
	public void testPostJsonBody( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    post( "/posts" )
		        .withRequestBody( equalToJson( "{\"name\": \"foobar\", \"body\": \"lorem ipsum dolor\"}" ) )
		        .willReturn( created()
		            .withBody( "{\"id\": 1, \"name\": \"foobar\", \"body\": \"lorem ipsum dolor\"}" ) ) );

		instance.executeSource( String.format(
		    """
		    bx:http method="POST" url="%s" {
		    	bx:httpparam type="body" value="#JSONSerialize( { 'name': 'foobar', 'body': 'lorem ipsum dolor' } )#";
		    }
		    """,
		    wmRuntimeInfo.getHttpBaseUrl() + "/posts" ), context );

		IStruct httpResult = variables.getAsStruct( bxhttp );
		assertThat( httpResult.get( Key.statusCode ) ).isEqualTo( 201 );
		assertThat( httpResult.get( Key.statusText ) ).isEqualTo( "Created" );
		String body = httpResult.getAsString( Key.fileContent );
		assertThat( body ).isEqualTo( "{\"id\": 1, \"name\": \"foobar\", \"body\": \"lorem ipsum dolor\"}" );
	}

	@DisplayName( "It can return and retain binary content" )
	@Test
	public void testBinaryReturn( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( "/image" )
		        .willReturn(
		            // simulate duplicate headers
		            ok().withHeader( "Content-Type", "image/jpeg; charset=utf-8" )
		                .withBody(
		                    ( byte[] ) FileSystemUtil.read( "src/test/resources/chuck_norris.jpg" ) ) ) );

		// @formatter:off
		instance.executeSource( String.format(
			"""
			bx:http method="GET" getAsBinary=true url="%s" {
				bx:httpparam type="header" name="Host" value="boxlang.io";
			}
			""",
			wmRuntimeInfo.getHttpBaseUrl() + "/image" ),
			context
		);
		// @formatter:on

		IStruct httpResult = variables.getAsStruct( bxhttp );
		assertThat( httpResult.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( httpResult.get( Key.statusText ) ).isEqualTo( "OK" );
		Object body = httpResult.get( Key.fileContent );
		assertThat( body ).isInstanceOf( byte[].class );

		// Now test with getAsBinary set to "no" which should still return binary
		// content
		// @formatter:off
		instance.executeSource( String.format(
			"""
			bx:http method="GET" getAsBinary="no" url="%s" {
				bx:httpparam type="header" name="Host" value="boxlang.io";
			}
			""",
			wmRuntimeInfo.getHttpBaseUrl() + "/image" ),
			context
		);
		// @formatter:on

		httpResult = variables.getAsStruct( bxhttp );
		assertThat( httpResult.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( httpResult.get( Key.statusText ) ).isEqualTo( "OK" );
		body = httpResult.get( Key.fileContent );
		assertThat( body ).isInstanceOf( byte[].class );
	}

	@DisplayName( "Will treat random content types as string contents" )
	@Test
	public void testRandomContentTypes( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( "/blah" )
		        .willReturn(
		            ok().withHeader( "Content-Type", "blah; charset=utf-8" )
		                .withBody( "Hello world!" ) ) );

		// @formatter:off
		instance.executeSource( String.format(
			"""
			bx:http method="GET" url="%s" {}
			""",
			wmRuntimeInfo.getHttpBaseUrl() + "/blah" ),
			context
		);
		// @formatter:on

		IStruct httpResult = variables.getAsStruct( bxhttp );
		assertThat( httpResult.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( httpResult.get( Key.statusText ) ).isEqualTo( "OK" );
		Object body = httpResult.get( Key.fileContent );
		assertThat( body ).isInstanceOf( String.class );
		assertThat( ( String ) body ).isEqualTo( "Hello world!" );
	}

	@DisplayName( "It can write out a binary file using the file name in the disposition header" )
	@Test
	public void testBinaryFileWrite( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( "/image" )
		        .willReturn(
		            ok().withHeader( "Content-Type", "image/jpeg; charset=utf-8" )
		                .withHeader( "Content-Disposition",
		                    "attachment; filename=\"chuck_norris_dl.jpg\"" )
		                .withBody(
		                    ( byte[] ) FileSystemUtil.read( "src/test/resources/chuck_norris.jpg" ) ) ) );

		// @formatter:off
		instance.executeSource( String.format(
			"""
			bx:http method="GET" getAsBinary=true url="%s" path="src/test/resources/tmp/http_tests" {}
			""",
			wmRuntimeInfo.getHttpBaseUrl() + "/image" ),
			context
		);
		// @formatter:on
		assertThat( FileSystemUtil.exists( "src/test/resources/tmp/http_tests/chuck_norris_dl.jpg" ) ).isTrue();
	}

	@DisplayName( "It will always return an http result even if a file and path are specified" )
	@Test
	public void testResultWithPath( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( "/bad-image.jpg" )
		        .willReturn( notFound() ) );

		// @formatter:off
		instance.executeSource( String.format(
			"""
			bx:http method="GET" getAsBinary=true url="%s" path="src/test/resources/tmp/http_tests" file="bad-image.jpg" result="httpResult" {}
			""",
			wmRuntimeInfo.getHttpBaseUrl() + "/bad-image.jpg" ),
			context
		);
		// @formatter:on
		IStruct httpResult = variables.getAsStruct( Key.of( "httpResult" ) );
		assertThat( httpResult.get( Key.statusCode ) ).isEqualTo( 404 );
		assertThat( FileSystemUtil.exists( "src/test/resources/tmp/http_tests/bad-image.jpg" ) ).isTrue();
	}

	@DisplayName( "It can write out a binary file using a specified filename" )
	@Test
	public void testBinaryFileWriteWithName( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( "/image" )
		        .willReturn(
		            ok().withHeader( "Content-Type", "image/jpeg; charset=utf-8" )
		                .withHeader( "Content-Disposition",
		                    "attachment; filename=\"chuck_norris_dl.jpg\"" )
		                .withBody(
		                    ( byte[] ) FileSystemUtil.read( "src/test/resources/chuck_norris.jpg" ) ) ) );

		// @formatter:off
		instance.executeSource( String.format(
			"""
			bx:http method="GET" getAsBinary=true url="%s" path="src/test/resources/tmp/http_tests" file="chuck.jpg" {}
			""",
			wmRuntimeInfo.getHttpBaseUrl() + "/image" ),
			context
		);
		// @formatter:on
		assertThat( FileSystemUtil.exists( "src/test/resources/tmp/http_tests/chuck.jpg" ) ).isTrue();
	}

	@DisplayName( "It can write out a binary file using the file attribute as the full path" )
	@Test
	public void testBinaryFileWriteFileOnly( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( "/image" )
		        .willReturn(
		            ok().withHeader( "Content-Type", "image/jpeg; charset=utf-8" )
		                .withHeader( "Content-Disposition",
		                    "attachment; filename=\"chuck_norris_dl.jpg\"" )
		                .withBody(
		                    ( byte[] ) FileSystemUtil.read( "src/test/resources/chuck_norris.jpg" ) ) ) );

		// @formatter:off
		instance.executeSource( String.format(
			"""
			bx:http method="GET" getAsBinary=true url="%s" file="src/test/resources/tmp/http_tests/chucky.jpg" {}
			""",
			wmRuntimeInfo.getHttpBaseUrl() + "/image" ),
			context
		);
		// @formatter:on
		assertThat( FileSystemUtil.exists( "src/test/resources/tmp/http_tests/chucky.jpg" ) ).isTrue();
	}

	@DisplayName( "It can send binary content" )
	@Test
	public void testBinarySend( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    post( "/image" )
		        .willReturn(
		            ok() ) );

		// @formatter:off
		variables.put(  Key.of( "fileContent" ), FileSystemUtil.read( "src/test/resources/chuck_norris.jpg" ) );
		instance.executeSource( String.format(
			"""
			bx:http method="POST" url="%s" {
				bx:httpparam type="body" value="#fileContent#";
			}
			""",
			wmRuntimeInfo.getHttpBaseUrl() + "/image" ),
			context
		);
		// @formatter:on

		IStruct httpResult = variables.getAsStruct( bxhttp );
		assertThat( httpResult.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( httpResult.get( Key.statusText ) ).isEqualTo( "OK" );
	}

	@DisplayName( "Will throw an error if binary is returned and getAsBinary is never" )
	@Test
	public void testBinaryThrow( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( "/image" )
		        .willReturn(
		            ok()
		                .withHeader( "Content-Type", "image/jpeg; charset=utf-8" )
		                .withBody(
		                    ( byte[] ) FileSystemUtil.read( "src/test/resources/chuck_norris.jpg" )
		                )
		        )
		);

		// @formatter:off
		assertThrows( BoxRuntimeException.class, () -> instance.executeSource( String.format(
			"""
			bx:http method="GET" getAsBinary="never" url="%s" {}
			""",
			wmRuntimeInfo.getHttpBaseUrl() + "/image" ),
			context
		) );
		// @formatter:on
	}

	@DisplayName( "It can make HTTP call ACF script" )
	@Test
	public void testCanMakeHTTPCallACFScript( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( "/posts/1" )
		        .willReturn( ok().withHeader( "Content-Type", "application/json; charset=utf-8" ).withBody(
		            """
		            {
		              "userId": 1,
		              "id": 1,
		              "title": "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
		              "body": "quia et suscipit\\nsuscipit recusandae consequuntur expedita et cum\\nreprehenderit molestiae ut ut quas totam\\nnostrum rerum est autem sunt rem eveniet architecto"
		            }
		            """ ) ) );

		instance.executeSource(
		    String.format( "cfhttp( url=\"%s\", result=\"result\" ) {}",
		        wmRuntimeInfo.getHttpBaseUrl() + "/posts/1" ),
		    context,
		    BoxSourceType.CFSCRIPT );

		IStruct httpResult = variables.getAsStruct( result );
		assertThat( httpResult.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( httpResult.get( Key.statusText ) ).isEqualTo( "OK" );
		assertThat( httpResult.getAsString( Key.fileContent ).replaceAll( "\\s+", "" ) ).isEqualTo(
		    """
		    {
		      "userId": 1,
		      "id": 1,
		      "title": "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
		      "body": "quia et suscipit\\nsuscipit recusandae consequuntur expedita et cum\\nreprehenderit molestiae ut ut quas totam\\nnostrum rerum est autem sunt rem eveniet architecto"
		    }
		    """
		        .replaceAll(
		            "\\s+", "" ) );
	}

	@DisplayName( "It can make a default GET request" )
	@Test
	public void testCanMakeHTTPCallTag( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( "/posts/1" )
		        .willReturn( ok().withHeader( "Content-Type", "application/json; charset=utf-8" ).withBody(
		            """
		            {
		              "userId": 1,
		              "id": 1,
		              "title": "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
		              "body": "quia et suscipit\\nsuscipit recusandae consequuntur expedita et cum\\nreprehenderit molestiae ut ut quas totam\\nnostrum rerum est autem sunt rem eveniet architecto"
		            }
		            """ ) ) );

		instance.executeSource(
		    String.format( """
		                     <cfhttp result="result" url="%s"></bxhttp>
		                   """, wmRuntimeInfo.getHttpBaseUrl() + "/posts/1" ),
		    context, BoxSourceType.CFTEMPLATE );

		IStruct httpResult = variables.getAsStruct( result );
		assertThat( httpResult.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( httpResult.get( Key.statusText ) ).isEqualTo( "OK" );
		assertThat( httpResult.getAsString( Key.fileContent ).replaceAll( "\\s+", "" ) ).isEqualTo(
		    """
		    {
		      "userId": 1,
		      "id": 1,
		      "title": "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
		      "body": "quia et suscipit\\nsuscipit recusandae consequuntur expedita et cum\\nreprehenderit molestiae ut ut quas totam\\nnostrum rerum est autem sunt rem eveniet architecto"
		    }
		    """
		        .replaceAll(
		            "\\s+", "" ) );
	}

	@DisplayName( "It can make a default GET request in BL Tags" )
	@Test
	public void testCanMakeHTTPCallBLTag( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( "/posts/1" )
		        .willReturn( ok().withHeader( "Content-Type", "application/json; charset=utf-8" ).withBody(
		            """
		            {
		              "userId": 1,
		              "id": 1,
		              "title": "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
		              "body": "quia et suscipit\\nsuscipit recusandae consequuntur expedita et cum\\nreprehenderit molestiae ut ut quas totam\\nnostrum rerum est autem sunt rem eveniet architecto"
		            }
		            """ ) ) );

		instance.executeSource(
		    String.format( """
		                     <bx:http result="result" url="%s"></bx:http>
		                   """, wmRuntimeInfo.getHttpBaseUrl() + "/posts/1" ),
		    context, BoxSourceType.BOXTEMPLATE );

		IStruct httpResult = variables.getAsStruct( result );
		assertThat( httpResult.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( httpResult.get( Key.statusText ) ).isEqualTo( "OK" );
		assertThat( httpResult.getAsString( Key.fileContent ).replaceAll( "\\s+", "" ) ).isEqualTo(
		    """
		    {
		      "userId": 1,
		      "id": 1,
		      "title": "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
		      "body": "quia et suscipit\\nsuscipit recusandae consequuntur expedita et cum\\nreprehenderit molestiae ut ut quas totam\\nnostrum rerum est autem sunt rem eveniet architecto"
		    }
		    """
		        .replaceAll(
		            "\\s+", "" ) );
	}

	@DisplayName( "It can make HTTP call tag attributeCollection" )
	@Test
	public void testCanMakeHTTPCallTagAttributeCollection( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( "/posts/1" )
		        .withHeader( "Accept-Encoding", equalTo( "sdf" ) )
		        .willReturn( ok().withHeader( "Content-Type", "application/json; charset=utf-8" ).withBody(
		            """
		            {
		              "userId": 1,
		              "id": 1,
		              "title": "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
		              "body": "quia et suscipit\\nsuscipit recusandae consequuntur expedita et cum\\nreprehenderit molestiae ut ut quas totam\\nnostrum rerum est autem sunt rem eveniet architecto"
		            }
		            """ ) ) );

		instance.executeSource(
		    String.format( """
		                   <cfset attrs = { type="header", name="Accept-Encoding", value="gzip,deflate" }>
		                      <cfhttp url="%s">
		                      	<cfhttpparam attributeCollection="#attrs#" value="sdf" />
		                      </cfhttp>
		                      <cfset result = cfhttp>
		                        """, wmRuntimeInfo.getHttpBaseUrl() + "/posts/1" ),
		    context, BoxSourceType.CFTEMPLATE );

		IStruct httpResult = variables.getAsStruct( result );
		assertThat( httpResult.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( httpResult.get( Key.statusText ) ).isEqualTo( "OK" );
		assertThat( httpResult.getAsString( Key.fileContent ).replaceAll( "\\s+", "" ) ).isEqualTo(
		    """
		    {
		      "userId": 1,
		      "id": 1,
		      "title": "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
		      "body": "quia et suscipit\\nsuscipit recusandae consequuntur expedita et cum\\nreprehenderit molestiae ut ut quas totam\\nnostrum rerum est autem sunt rem eveniet architecto"
		    }
		    """
		        .replaceAll(
		            "\\s+", "" ) );

	}

	@DisplayName( "It can make HTTP call with custom transfer encoding" )
	@Test
	public void testCanMakeHTTPCallTransferEncoding( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( "/posts/3" )
		        .withHeader( "Accept-Encoding", equalTo( "deflate;q=0" ) )
		        .willReturn( ok().withHeader( "Content-Type", "application/json; charset=utf-8" ).withBody(
		            """
		            {
		              "userId": 3,
		              "id": 3,
		              "title": "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
		              "body": "quia et suscipit\\nsuscipit recusandae consequuntur expedita et cum\\nreprehenderit molestiae ut ut quas totam\\nnostrum rerum est autem sunt rem eveniet architecto"
		            }
		            """ ) ) );

		instance.executeSource(
		    String.format( """
		                                   <cfhttp url="%s">
		                                   	<cfhttpparam type="Header" name="Accept-Encoding" value="deflate;q=0">
		                   <cfhttpparam type="Header" name="TE" value="deflate;q=0">
		                                   </cfhttp>
		                                   <cfset result = cfhttp>
		                                     """, wmRuntimeInfo.getHttpBaseUrl() + "/posts/3" ),
		    context, BoxSourceType.CFTEMPLATE );

		IStruct httpResult = variables.getAsStruct( result );
		assertThat( httpResult.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( httpResult.get( Key.statusText ) ).isEqualTo( "OK" );
		assertThat( httpResult.getAsString( Key.fileContent ).replaceAll( "\\s+", "" ) ).isEqualTo(
		    """
		    {
		      "userId": 3,
		      "id": 3,
		      "title": "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
		      "body": "quia et suscipit\\nsuscipit recusandae consequuntur expedita et cum\\nreprehenderit molestiae ut ut quas totam\\nnostrum rerum est autem sunt rem eveniet architecto"
		    }
		    """
		        .replaceAll(
		            "\\s+", "" ) );

	}

	@DisplayName( "It can make a GET request with URL params" )
	@Test
	public void testGetWithParams( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( "/posts?userId=1" )
		        .willReturn( ok().withHeader( "Content-Type", "application/json; charset=utf-8" ).withBody(
		            """
		            [{"userId":1,"id":1,"title":"suntautfacererepellatprovidentoccaecatiexcepturioptioreprehenderit","body":"quiaetsuscipit\\nsuscipitrecusandaeconsequunturexpeditaetcum\\nreprehenderitmolestiaeututquastotam\\nnostrumrerumestautemsuntremevenietarchitecto"},{"userId":1,"id":2,"title":"quiestesse","body":"estrerumtemporevitae\\nsequisintnihilreprehenderitdolorbeataeeadoloresneque\\nfugiatblanditiisvoluptateporrovelnihilmolestiaeutreiciendis\\nquiaperiamnondebitispossimusquinequenisinulla"},{"userId":1,"id":3,"title":"eamolestiasquasiexercitationemrepellatquiipsasitaut","body":"etiustosedquoiure\\nvoluptatemoccaecatiomniseligendiautad\\nvoluptatemdoloribusvelaccusantiumquispariatur\\nmolestiaeporroeiusodioetlaboreetvelitaut"},{"userId":1,"id":4,"title":"eumetestoccaecati","body":"ullametsaepereiciendisvoluptatemadipisci\\nsitametautemassumendaprovidentrerumculpa\\nquishiccommodinesciuntremteneturdoloremqueipsamiure\\nquissuntvoluptatemrerumillovelit"},{"userId":1,"id":5,"title":"nesciuntquasodio","body":"repudiandaeveniamquaeratsuntsed\\naliasautfugiatsitautemsedest\\nvoluptatemomnispossimusessevoluptatibusquis\\nestautteneturdolorneque"},{"userId":1,"id":6,"title":"doloremeummagnieosaperiamquia","body":"utaspernaturcorporisharumnihilquisprovidentsequi\\nmollitianobisaliquidmolestiae\\nperspiciatiseteanemoabreprehenderitaccusantiumquas\\nvoluptatedoloresvelitetdoloremquemolestiae"},{"userId":1,"id":7,"title":"magnamfacilisautem","body":"doloreplaceatquibusdameaquovitae\\nmagniquisenimquiquisquonemoautsaepe\\nquidemrepellatexcepturiutquia\\nsuntutsequieoseasedquas"},{"userId":1,"id":8,"title":"doloremdoloreestipsam","body":"dignissimosaperiamdoloremquieum\\nfacilisquibusdamanimisintsuscipitquisintpossimuscum\\nquaeratmagnimaioresexcepturi\\nipsamutcommodidolorvoluptatummodiautvitae"},{"userId":1,"id":9,"title":"nesciuntiureomnisdoloremtemporaetaccusantium","body":"consecteturaniminesciuntiuredolore\\nenimquiaad\\nveniamautemutquamautnobis\\netestautquodautprovidentvoluptasautemvoluptas"},{"userId":1,"id":10,"title":"optiomolestiasidquiaeum","body":"quoetexpeditamodicumofficiavelmagni\\ndoloribusquirepudiandae\\nveronisisit\\nquosveniamquodsedaccusamusveritatiserror"}]""" ) ) );

		instance.executeSource(
		    String.format( """
		                    bx:http url="%s" {
		                   	 bx:httpparam type="url" name="userId" value=1;
		                   }
		                   result = bxhttp;
		                    """, wmRuntimeInfo.getHttpBaseUrl() + "/posts" ),
		    context );

		IStruct httpResult = variables.getAsStruct( result );

		Assertions.assertTrue( httpResult.containsKey( Key.statusCode ) );
		assertThat( httpResult.get( Key.statusCode ) ).isEqualTo( 200 );
		Assertions.assertTrue( httpResult.containsKey( Key.status_code ) );
		assertThat( httpResult.get( Key.status_code ) ).isEqualTo( 200 );

		Assertions.assertTrue( httpResult.containsKey( Key.statusText ) );
		assertThat( httpResult.get( Key.statusText ) ).isEqualTo( "OK" );
		Assertions.assertTrue( httpResult.containsKey( Key.status_text ) );
		assertThat( httpResult.get( Key.status_text ) ).isEqualTo( "OK" );

		Assertions.assertTrue( httpResult.containsKey( Key.HTTP_Version ) );
		assertThat( httpResult.getAsString( Key.HTTP_Version ) ).isEqualTo( "HTTP/2" );

		Assertions.assertTrue( httpResult.containsKey( Key.errorDetail ) );
		assertThat( httpResult.getAsString( Key.errorDetail ) ).isEqualTo( "" );

		Assertions.assertTrue( httpResult.containsKey( Key.mimetype ) );
		assertThat( httpResult.getAsString( Key.mimetype ) ).isEqualTo( "application/json" );

		Assertions.assertTrue( httpResult.containsKey( Key.charset ) );
		assertThat( httpResult.getAsString( Key.charset ) ).isEqualTo( "utf-8" );

		Assertions.assertTrue( httpResult.containsKey( Key.cookies ) );
		Query cookies = httpResult.getAsQuery( Key.cookies );
		Assertions.assertNotNull( cookies );
		assertThat( cookies ).isInstanceOf( Query.class );
		Assertions.assertEquals( 0, cookies.size() );

		Assertions.assertTrue( httpResult.containsKey( Key.fileContent ) );
		assertThat( httpResult.getAsString( Key.fileContent ).replaceAll( "\\s+", "" ) ).isEqualTo(
		    """
		    	[{"userId":1,"id":1,"title":"suntautfacererepellatprovidentoccaecatiexcepturioptioreprehenderit","body":"quiaetsuscipit\\nsuscipitrecusandaeconsequunturexpeditaetcum\\nreprehenderitmolestiaeututquastotam\\nnostrumrerumestautemsuntremevenietarchitecto"},{"userId":1,"id":2,"title":"quiestesse","body":"estrerumtemporevitae\\nsequisintnihilreprehenderitdolorbeataeeadoloresneque\\nfugiatblanditiisvoluptateporrovelnihilmolestiaeutreiciendis\\nquiaperiamnondebitispossimusquinequenisinulla"},{"userId":1,"id":3,"title":"eamolestiasquasiexercitationemrepellatquiipsasitaut","body":"etiustosedquoiure\\nvoluptatemoccaecatiomniseligendiautad\\nvoluptatemdoloribusvelaccusantiumquispariatur\\nmolestiaeporroeiusodioetlaboreetvelitaut"},{"userId":1,"id":4,"title":"eumetestoccaecati","body":"ullametsaepereiciendisvoluptatemadipisci\\nsitametautemassumendaprovidentrerumculpa\\nquishiccommodinesciuntremteneturdoloremqueipsamiure\\nquissuntvoluptatemrerumillovelit"},{"userId":1,"id":5,"title":"nesciuntquasodio","body":"repudiandaeveniamquaeratsuntsed\\naliasautfugiatsitautemsedest\\nvoluptatemomnispossimusessevoluptatibusquis\\nestautteneturdolorneque"},{"userId":1,"id":6,"title":"doloremeummagnieosaperiamquia","body":"utaspernaturcorporisharumnihilquisprovidentsequi\\nmollitianobisaliquidmolestiae\\nperspiciatiseteanemoabreprehenderitaccusantiumquas\\nvoluptatedoloresvelitetdoloremquemolestiae"},{"userId":1,"id":7,"title":"magnamfacilisautem","body":"doloreplaceatquibusdameaquovitae\\nmagniquisenimquiquisquonemoautsaepe\\nquidemrepellatexcepturiutquia\\nsuntutsequieoseasedquas"},{"userId":1,"id":8,"title":"doloremdoloreestipsam","body":"dignissimosaperiamdoloremquieum\\nfacilisquibusdamanimisintsuscipitquisintpossimuscum\\nquaeratmagnimaioresexcepturi\\nipsamutcommodidolorvoluptatummodiautvitae"},{"userId":1,"id":9,"title":"nesciuntiureomnisdoloremtemporaetaccusantium","body":"consecteturaniminesciuntiuredolore\\nenimquiaad\\nveniamautemutquamautnobis\\netestautquodautprovidentvoluptasautemvoluptas"},{"userId":1,"id":10,"title":"optiomolestiasidquiaeum","body":"quoetexpeditamodicumofficiavelmagni\\ndoloribusquirepudiandae\\nveronisisit\\nquosveniamquodsedaccusamusveritatiserror"}]
		    """
		        .replaceAll(
		            "\\s+", "" ) );
	}

	@DisplayName( "It can handle bad gateways" )
	@Test
	public void testBadGateway() {
		// @formatter:off
		instance.executeSource( """
			bx:http method="GET" url="https://does-not-exist.also-does-not-exist" {
				bx:httpparam type="header" name="User-Agent" value="HyperCFML/7.5.2";
			}
			result = bxhttp;
		""", context );
		// @formatter:on

		IStruct httpResult = variables.getAsStruct( result );

		Assertions.assertTrue( httpResult.containsKey( Key.statusCode ) );
		assertThat( httpResult.get( Key.statusCode ) ).isEqualTo( 502 );
		Assertions.assertTrue( httpResult.containsKey( Key.status_code ) );
		assertThat( httpResult.get( Key.status_code ) ).isEqualTo( 502 );

		Assertions.assertTrue( httpResult.containsKey( Key.statusText ) );
		assertThat( httpResult.get( Key.statusText ) ).isEqualTo( "Bad Gateway" );
		Assertions.assertTrue( httpResult.containsKey( Key.status_text ) );
		assertThat( httpResult.get( Key.status_text ) ).isEqualTo( "Bad Gateway" );

		Assertions.assertTrue( httpResult.containsKey( Key.fileContent ) );
		assertThat( httpResult.get( Key.fileContent ) ).isEqualTo( "Connection Failure" );

		Assertions.assertTrue( httpResult.containsKey( Key.errorDetail ) );
		assertThat( httpResult.get( Key.errorDetail ) )
		    .isEqualTo( "Unknown host: does-not-exist.also-does-not-exist: Name or service not known." );
	}

	@DisplayName( "It can handle timeouts" )
	@Test
	public void testTimeout( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor( get( "/timeout" )
		    .willReturn(
		        aResponse()
		            .withStatus( 200 )
		            .withFixedDelay( 5000 ) )
		);

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();
		// @formatter:off
		instance.executeSource( String.format( """
			bx:http timeout="1" method="GET" url="%s";
			result = bxhttp;
			println( result )
		""", baseURL + "/timeout" ), context );
		// @formatter:on

		IStruct httpResult = variables.getAsStruct( result );

		Assertions.assertTrue( httpResult.containsKey( Key.statusCode ) );
		assertThat( httpResult.get( Key.statusCode ) ).isEqualTo( 408 );
		Assertions.assertTrue( httpResult.containsKey( Key.status_code ) );
		assertThat( httpResult.get( Key.status_code ) ).isEqualTo( 408 );

		Assertions.assertTrue( httpResult.containsKey( Key.statusText ) );
		assertThat( httpResult.get( Key.statusText ) ).isEqualTo( "Request Timeout" );
		Assertions.assertTrue( httpResult.containsKey( Key.status_text ) );
		assertThat( httpResult.get( Key.status_text ) ).isEqualTo( "Request Timeout" );

		Assertions.assertTrue( httpResult.containsKey( Key.fileContent ) );
		assertThat( httpResult.get( Key.fileContent ) ).isEqualTo( "Request Timeout" );

		Assertions.assertTrue( httpResult.containsKey( Key.errorDetail ) );
		assertThat( httpResult.get( Key.errorDetail ) ).isEqualTo( "The request timed out after 1 second(s)" );
	}

	@DisplayName( "It can handle files" )
	@Test
	public void testFiles( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    post( "/files" )
		        .withMultipartRequestBody( aMultipart().withName( "photo" ) )
		        .willReturn( created().withBody( "{\"success\": true }" ) ) );

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();
		// @formatter:off
		instance.executeSource( String.format( """
			bx:http method="POST" url="%s" {
				bx:httpparam type="file" name="photo" file="/src/test/resources/chuck_norris.jpg";
			}
			result = bxhttp;
		""", baseURL + "/files" ), context );
		// @formatter:on

		IStruct httpResult = variables.getAsStruct( result );

		Assertions.assertTrue( httpResult.containsKey( Key.statusCode ) );
		assertThat( httpResult.get( Key.statusCode ) ).isEqualTo( 201 );
		Assertions.assertTrue( httpResult.containsKey( Key.status_code ) );
		assertThat( httpResult.get( Key.status_code ) ).isEqualTo( 201 );

		Assertions.assertTrue( httpResult.containsKey( Key.statusText ) );
		assertThat( httpResult.get( Key.statusText ) ).isEqualTo( "Created" );
		Assertions.assertTrue( httpResult.containsKey( Key.status_text ) );
		assertThat( httpResult.get( Key.status_text ) ).isEqualTo( "Created" );

		Assertions.assertTrue( httpResult.containsKey( Key.fileContent ) );
		assertThat( httpResult.get( Key.fileContent ) ).isEqualTo( "{\"success\": true }" );
	}

	@DisplayName( "It can handle multipart uploads" )
	@Test
	public void testMultipart( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    post( "/multipart" )
		        .withMultipartRequestBody(
		            aMultipart()
		                .withName( "photo" )
		                .withName( "joke" )
		                .withBody( containing( "Chuck Norris can divide by zero." ) ) )
		        .willReturn( created().withBody( "{\"success\": true }" ) ) );

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();
		// @formatter:off
		instance.executeSource( String.format( """
			bx:http method="POST" url="%s" {
				bx:httpparam type="file" name="photo" file="/src/test/resources/chuck_norris.jpg";
				bx:httpparam type="formfield" name="joke" value="Chuck Norris can divide by zero.";
			}
			result = bxhttp;
		""", baseURL + "/multipart" ), context );
		// @formatter:on

		IStruct httpResult = variables.getAsStruct( result );

		Assertions.assertTrue( httpResult.containsKey( Key.statusCode ) );
		assertThat( httpResult.get( Key.statusCode ) ).isEqualTo( 201 );
		Assertions.assertTrue( httpResult.containsKey( Key.status_code ) );
		assertThat( httpResult.get( Key.status_code ) ).isEqualTo( 201 );

		Assertions.assertTrue( httpResult.containsKey( Key.statusText ) );
		assertThat( httpResult.get( Key.statusText ) ).isEqualTo( "Created" );
		Assertions.assertTrue( httpResult.containsKey( Key.status_text ) );
		assertThat( httpResult.get( Key.status_text ) ).isEqualTo( "Created" );

		Assertions.assertTrue( httpResult.containsKey( Key.fileContent ) );
		assertThat( httpResult.get( Key.fileContent ) ).isEqualTo( "{\"success\": true }" );
	}

	@DisplayName( "Will not double encode params when encoded is set to false on a URL param" )
	@Test
	public void testDoubleEncoding( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    post( "/test-params?myParam=This%26is%20my%2Fvery%20long%3Fquery%2Bparam" )
		        .willReturn( ok().withBody( "{\"success\": true }" ) ) );

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();
		// @formatter:off
		instance.executeSource( String.format( """
			bx:http method="POST" url="%s" {
				bx:httpparam type="url" encoded="false" name="myParam" value="#urlEncodedFormat( "This&is my/very long?query+param" )#";
			}
			result = bxhttp;
		""", baseURL + "/test-params" ), context );
		// @formatter:on

		IStruct httpResult = variables.getAsStruct( result );

		Assertions.assertTrue( httpResult.containsKey( Key.statusCode ) );
		assertThat( httpResult.get( Key.statusCode ) ).isEqualTo( 200 );

		Assertions.assertTrue( httpResult.containsKey( Key.fileContent ) );
		assertThat( httpResult.get( Key.fileContent ) ).isEqualTo( "{\"success\": true }" );
	}

	@DisplayName( "It can process a basic authentication request" )
	@Test
	public void testBasicAuth( WireMockRuntimeInfo wmRuntimeInfo ) {
		String	username			= "admin";
		String	password			= "password";
		String	base64Credentials	= Base64.getEncoder().encodeToString( ( username + ":" + password ).getBytes() );
		stubFor(
		    get( "/posts/1" )
		        .withHeader( "Authorization", equalTo( "Basic " + base64Credentials ) )
		        .willReturn( ok()
		            .withHeader( "Content-Type", "application/json; charset=utf-8" )
		            .withHeader( "Authorization", "Basic " + base64Credentials )
		            .withBody(
		                """
		                {
		                  "userId": 1,
		                  "id": 1,
		                  "title": "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
		                  "body": "quia et suscipit\\nsuscipit recusandae consequuntur expedita et cum\\nreprehenderit molestiae ut ut quas totam\\nnostrum rerum est autem sunt rem eveniet architecto"
		                }
		                """ ) ) );

		instance.executeSource(
		    String.format( """
		                     <cfhttp result="result" url="%s" username="%s" password="%s" throwonerror="true"></bxhttp>
		                   """,
		        wmRuntimeInfo.getHttpBaseUrl() + "/posts/1",
		        username,
		        password ),
		    context, BoxSourceType.CFTEMPLATE );

		IStruct httpResult = variables.getAsStruct( result );
		assertThat( httpResult.getAsString( Key.header ) ).contains( "authorization: Basic " + base64Credentials );
	}

	@DisplayName( "It can handle client certificates" )
	@Test
	public void testClientCert( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor( get( urlEqualTo( "/posts/2" ) )
		    .withHeader( "X-Client-Cert", matching( ".*" ) ) // The header must be present and contain any value
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withBody( "Access granted" ) ) );
		String	clientCertPath		= "src/test/resources/tmp/http_tests/cert.p12";

		String	clientCertPassword	= "password";
		try {
			createClientCertificate( clientCertPath, clientCertPassword );
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "An error occurred when attempting to create the test certificate", e );
		}

		// @formatter:off
		instance.executeSource( String.format( """
			bx:http method="GET" url="%s" clientCert="%s" clientCertPassword="%s" debug=true {}
			result = bxhttp
			println( result )
		""", wmRuntimeInfo.getHttpBaseUrl() + "/posts/2", clientCertPath, clientCertPassword ), context );
		// @formatter:on

		IStruct httpResult = variables.getAsStruct( result );
		Assertions.assertTrue( httpResult.containsKey( Key.statusCode ) );
		assertThat( httpResult.get( Key.statusCode ) ).isEqualTo( 200 ); // Assuming successful response
		Assertions.assertTrue( httpResult.containsKey( Key.statusText ) );
		assertThat( httpResult.get( Key.statusText ) ).isEqualTo( "OK" ); // Assuming successful response
	}

	@DisplayName( "It can use onRequestStart callback" )
	@Test
	public void testOnRequestStartCallback( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor( get( "/test-callback" )
		    .willReturn(
		        aResponse()
		            .withBody( "Success" )
		            .withStatus( 200 ) ) );

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();

		// @formatter:off
		instance.executeSource(
		    String.format( """
				callbackData = {};
				onRequestStartFn = ( result ) => {
					callbackData.called = true;
					callbackData.url = result.request.url;
					callbackData.method = result.request.method;
					callbackData.hasHeaders = structKeyExists( result.request, 'headers');
				};
				bx:http url="%s" onRequestStart=onRequestStartFn;
				result = bxhttp;
			""",
		        baseURL + "/test-callback"
		    ),
		    context
		);
		// @formatter:on

		IStruct callbackData = variables.getAsStruct( Key.of( "callbackData" ) );
		assertThat( callbackData.getAsBoolean( Key.of( "called" ) ) ).isTrue();
		assertThat( callbackData.getAsString( Key.of( "url" ) ) ).contains( "/test-callback" );
		assertThat( callbackData.getAsString( Key.of( "method" ) ) ).isEqualTo( "GET" );
		assertThat( callbackData.getAsBoolean( Key.of( "hasHeaders" ) ) ).isTrue();

		IStruct httpResult = variables.getAsStruct( result );
		assertThat( httpResult.get( Key.statusCode ) ).isEqualTo( 200 );
	}

	@DisplayName( "It can use onComplete callback" )
	@Test
	public void testOnCompleteCallback( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor( get( "/test-complete" )
		    .willReturn(
		        aResponse()
		            .withBody( "Completed" )
		            .withStatus( 200 ) ) );

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();

		// @formatter:off
		instance.executeSource(
		    String.format( """
				completeData = {};
				onCompleteFn = (httpResult, response) => {
					completeData.called = true;
					completeData.statusCode = httpResult.statusCode;
					completeData.fileContent = httpResult.fileContent;
					completeData.hasResponse = !isNull(response);
				};
				bx:http url="%s" onComplete=onCompleteFn;
				result = bxhttp;
			""",
		        baseURL + "/test-complete"
		    ),
		    context
		);
		// @formatter:on

		IStruct completeData = variables.getAsStruct( Key.of( "completeData" ) );
		assertThat( completeData.getAsBoolean( Key.of( "called" ) ) ).isTrue();
		assertThat( completeData.getAsInteger( Key.of( "statusCode" ) ) ).isEqualTo( 200 );
		assertThat( completeData.getAsString( Key.of( "fileContent" ) ) ).isEqualTo( "Completed" );
		assertThat( completeData.getAsBoolean( Key.of( "hasResponse" ) ) ).isTrue();

		IStruct httpResult = variables.getAsStruct( result );
		assertThat( httpResult.get( Key.statusCode ) ).isEqualTo( 200 );
	}

	@DisplayName( "It can use onError callback" )
	@Test
	public void testOnErrorCallback( WireMockRuntimeInfo wmRuntimeInfo ) {
		// Don't stub anything - this will cause a connection error
		String baseURL = "http://localhost:99999"; // Invalid port to force error

		// @formatter:off
		instance.executeSource(
		    String.format( """
				errorData = {};
				onErrorFn = (exception, httpResult) => {
					errorData.called = true;
					errorData.hasException = !isNull(exception);
					errorData.hasHttpResult = !isNull(httpResult);
					errorData.statusCode = httpResult.statusCode ?: 0;
				};
				bx:http url="%s" timeout="1" onError=onErrorFn;
				result = bxhttp;
			""",
		        baseURL + "/test-error"
		    ),
		    context
		);
		// @formatter:on

		IStruct errorData = variables.getAsStruct( Key.of( "errorData" ) );
		assertThat( errorData.getAsBoolean( Key.of( "called" ) ) ).isTrue();
		assertThat( errorData.getAsBoolean( Key.of( "hasException" ) ) ).isTrue();
		assertThat( errorData.getAsBoolean( Key.of( "hasHttpResult" ) ) ).isTrue();

		IStruct httpResult = variables.getAsStruct( result );
		// Should have an error status code (5xx for connection/server errors)
		assertThat( httpResult.getAsInteger( Key.statusCode ) ).isGreaterThan( 499 );
	}

	@DisplayName( "It can use multiple callbacks together" )
	@Test
	public void testMultipleCallbacks( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor( get( "/test-multi" )
		    .willReturn(
		        aResponse()
		            .withBody( "Multi-callback test" )
		            .withStatus( 200 ) ) );

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();

		// @formatter:off
		instance.executeSource(
		    String.format( """
				callbacks = {
					onRequestStart: false,
					onComplete: false
				};

				onRequestStartFn = (data) => { callbacks.onRequestStart = true; };
				onCompleteFn = (httpResult, response) => { callbacks.onComplete = true; };

				bx:http url="%s"
					onRequestStart=onRequestStartFn
					onComplete=onCompleteFn;

				result = bxhttp;
			""",
		        baseURL + "/test-multi"
		    ),
		    context
		);
		// @formatter:on

		IStruct callbacks = variables.getAsStruct( Key.of( "callbacks" ) );
		assertThat( callbacks.getAsBoolean( Key.of( "onRequestStart" ) ) ).isTrue();
		assertThat( callbacks.getAsBoolean( Key.of( "onComplete" ) ) ).isTrue();

		IStruct httpResult = variables.getAsStruct( result );
		assertThat( httpResult.get( Key.statusCode ) ).isEqualTo( 200 );
	}

	@DisplayName( "It can modify request in onRequestStart callback" )
	@Test
	public void testOnRequestStartModification( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor( get( "/test-modification" )
		    .willReturn(
		        aResponse()
		            .withBody( "Modified" )
		            .withStatus( 200 ) ) );

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();

		// @formatter:off
		instance.executeSource(
		    String.format( """
				requestInfo = {};
				onRequestStartFn = ( result ) => {
					// Capture request details
					requestInfo.url = result.request.url;
					requestInfo.method = result.request.method;
					requestInfo.headerCount = structCount(result.request.headers);
					requestInfo.executionTime = result.executionTime ?: 0;
				};
				bx:http url="%s" onRequestStart=onRequestStartFn;
				result = bxhttp;
			""",
		        baseURL + "/test-modification"
		    ),
		    context
		);
		// @formatter:on

		IStruct requestInfo = variables.getAsStruct( Key.of( "requestInfo" ) );
		assertThat( requestInfo.getAsString( Key.of( "url" ) ) ).contains( "/test-modification" );
		assertThat( requestInfo.getAsString( Key.of( "method" ) ) ).isEqualTo( "GET" );
		assertThat( requestInfo.getAsInteger( Key.of( "headerCount" ) ) ).isGreaterThan( 0 );
	}

	@DisplayName( "It can access execution time in onComplete callback" )
	@Test
	public void testExecutionTimeInCallback( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor( get( "/test-timing" )
		    .willReturn(
		        aResponse()
		            .withBody( "Timing test" )
		            .withStatus( 200 ) ) );

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();

		// @formatter:off
		instance.executeSource(
		    String.format( """
				timingData = {};
				onCompleteFn = (httpResult, response) => {
					timingData.hasExecutionTime = structKeyExists(httpResult, 'executionTime');
					timingData.executionTime = httpResult.executionTime ?: -1;
					timingData.isNumeric = isNumeric(httpResult.executionTime);
				};
				bx:http url="%s" onComplete=onCompleteFn;
				result = bxhttp;
			""",
		        baseURL + "/test-timing"
		    ),
		    context
		);
		// @formatter:on

		IStruct timingData = variables.getAsStruct( Key.of( "timingData" ) );
		assertThat( timingData.getAsBoolean( Key.of( "hasExecutionTime" ) ) ).isTrue();
		assertThat( timingData.getAsBoolean( Key.of( "isNumeric" ) ) ).isTrue();
		assertThat( timingData.getAsLong( Key.of( "executionTime" ) ) ).isGreaterThan( -1L );

		IStruct httpResult = variables.getAsStruct( result );
		assertThat( httpResult.containsKey( Key.executionTime ) ).isTrue();
	}

	@DisplayName( "It can stream response with onChunk callback" )
	@Test
	public void testOnChunkCallbackStreaming( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor( get( "/test-streaming" )
		    .willReturn(
		        aResponse()
		            .withBody( "Line 1\nLine 2\nLine 3\nLine 4\nLine 5" )
		            .withStatus( 200 )
		    ) );

		String baseURL = "http://localhost:" + wmRuntimeInfo.getHttpPort();

		instance.executeSource(
		    """
		       chunkData = {
		           chunks: [],
		           chunkCount: 0,
		           totalContent: ""
		       };
		    """,
		    context
		);

		// @formatter:off
		instance.executeSource(
		    String.format(
		        """
					onChunkFn = ( chunkNumber, content, totalBytes , result, httpClient, response ) => {
						chunkData.chunkCount++;
						arrayAppend(chunkData.chunks, {
							chunkNumber: chunkNumber,
							content: content,
							totalBytes: totalBytes
						});
						chunkData.totalContent &= content;
					};
					bx:http url="%s" onChunk=onChunkFn;
					result = bxhttp;
				""",
			        baseURL + "/test-streaming"
			    ),
			    context
			);
		// @formatter:on

		IStruct chunkData = variables.getAsStruct( Key.of( "chunkData" ) );
		assertThat( chunkData.getAsInteger( Key.of( "chunkCount" ) ) ).isGreaterThan( 0 );
		assertThat( chunkData.get( Key.of( "chunks" ) ) ).isInstanceOf( Array.class );

		Array chunks = ( Array ) chunkData.get( Key.of( "chunks" ) );
		assertThat( chunks.size() ).isGreaterThan( 0 );

		// Verify first chunk has expected structure
		IStruct firstChunk = ( IStruct ) chunks.get( 0 );
		assertThat( firstChunk.containsKey( Key.of( "chunkNumber" ) ) ).isTrue();
		assertThat( firstChunk.containsKey( Key.of( "content" ) ) ).isTrue();
		assertThat( firstChunk.containsKey( Key.of( "totalBytes" ) ) ).isTrue();

		// Verify httpResult indicates streaming mode
		IStruct httpResult = variables.getAsStruct( result );
		assertThat( httpResult.getAsBoolean( Key.of( "stream" ) ) ).isTrue();
		assertThat( httpResult.getAsInteger( Key.statusCode ) ).isEqualTo( 200 );
	}

	@DisplayName( "It accumulates streamed content in fileContent" )
	@Test
	public void testStreamingAccumulatesContent( WireMockRuntimeInfo wmRuntimeInfo ) {
		String testContent = "First line\nSecond line\nThird line";
		stubFor( get( "/test-accumulation" )
		    .willReturn(
		        aResponse()
		            .withBody( testContent )
		            .withStatus( 200 )
		    ) );

		String baseURL = "http://localhost:" + wmRuntimeInfo.getHttpPort();

		// @formatter:off
		instance.executeSource(
		    String.format(
		        """
					chunkTracker = { count: 0 };
					onChunkFn = (data) => {
						chunkTracker.count++;
					};
					bx:http url="%s" onChunk=onChunkFn;
					result = bxhttp;
				""",
			        baseURL + "/test-accumulation"
			    ),
			    context
			);
		// @formatter:on

		IStruct	httpResult	= variables.getAsStruct( result );
		String	fileContent	= httpResult.getAsString( Key.fileContent );

		// Verify accumulated content contains all lines
		assertThat( fileContent ).contains( "First line" );
		assertThat( fileContent ).contains( "Second line" );
		assertThat( fileContent ).contains( "Third line" );

		// Verify chunks were processed
		IStruct chunkTracker = variables.getAsStruct( Key.of( "chunkTracker" ) );
		assertThat( chunkTracker.getAsInteger( Key.of( "count" ) ) ).isGreaterThan( 0 );
	}

	@DisplayName( "It handles streaming with onComplete callback" )
	@Test
	public void testStreamingWithOnComplete( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor( get( "/test-streaming-complete" )
		    .willReturn(
		        aResponse()
		            .withBody( "Stream line 1\nStream line 2" )
		            .withStatus( 200 )
		    ) );

		String baseURL = "http://localhost:" + wmRuntimeInfo.getHttpPort();

		instance.executeSource(
		    """
		       streamData = {
		           chunksCalled: false,
		           completeCalled: false,
		           finalContent: ""
		       };
		    """,
		    context
		);

		// @formatter:off
		instance.executeSource(
		    String.format(
		        """
					onChunkFn = (data) => {
						streamData.chunksCalled = true;
					};
					onCompleteFn = (httpResult, response) => {
						streamData.completeCalled = true;
						streamData.finalContent = httpResult.fileContent;
					};
					bx:http url="%s" onChunk=onChunkFn onComplete=onCompleteFn;
					result = bxhttp;
				""",
			        baseURL + "/test-streaming-complete"
			    ),
			    context
			);
		// @formatter:on

		IStruct streamData = variables.getAsStruct( Key.of( "streamData" ) );
		assertThat( streamData.getAsBoolean( Key.of( "chunksCalled" ) ) ).isTrue();
		assertThat( streamData.getAsBoolean( Key.of( "completeCalled" ) ) ).isTrue();
		assertThat( streamData.getAsString( Key.of( "finalContent" ) ) ).isNotEmpty();
	}

	@DisplayName( "It provides chunk metadata in streaming mode" )
	@Test
	public void testChunkMetadata( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor( get( "/test-chunk-metadata" )
		    .willReturn(
		        aResponse()
		            .withBody( "Metadata test\nLine 2\nLine 3" )
		            .withHeader( "Content-Type", "text/plain" )
		            .withStatus( 200 )
		    ) );

		String baseURL = "http://localhost:" + wmRuntimeInfo.getHttpPort();

		instance.executeSource(
		    """
		       metadataTracker = {
		           hasStatusCode: false,
		           hasHeaders: false,
		           hasHttpResult: false,
		           statusCode: 0
		       };
		    """,
		    context
		);

		// @formatter:off
		instance.executeSource(
		    String.format(
		        """
					onChunkFn = (currentChunk, line, totalBytes, result, httpClient, response ) => {
						metadataTracker.hasStatusCode = structKeyExists(result, 'statusCode');
						metadataTracker.hasHeaders = structKeyExists(result, 'responseHeader');
						metadataTracker.statusCode = result.statusCode ?: 0;
					};
					bx:http url="%s" onChunk=onChunkFn;
					result = bxhttp;
				""",
			        baseURL + "/test-chunk-metadata"
			    ),
			    context
			);
		// @formatter:on

		IStruct metadataTracker = variables.getAsStruct( Key.of( "metadataTracker" ) );
		assertThat( metadataTracker.getAsBoolean( Key.of( "hasStatusCode" ) ) ).isTrue();
		assertThat( metadataTracker.getAsBoolean( Key.of( "hasHeaders" ) ) ).isTrue();
		assertThat( metadataTracker.getAsInteger( Key.of( "statusCode" ) ) ).isEqualTo( 200 );
	}

	@DisplayName( "It handles streaming errors gracefully" )
	@Test
	public void testStreamingErrorHandling( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor( get( "/test-streaming-error" )
		    .willReturn(
		        aResponse()
		            .withBody( "Error content" )
		            .withStatus( 500 )
		    ) );

		String baseURL = "http://localhost:" + wmRuntimeInfo.getHttpPort();

		instance.executeSource(
		    """
		       errorTracker = {
		           chunksCalled: false,
		           statusCode: 0
		       };
		    """,
		    context
		);

		// @formatter:off
		instance.executeSource(
		    String.format(
		        """
					onChunkFn = (chunkNumber, content, totalBytes, result, httpClient, response) => {
						errorTracker.chunksCalled = true;
						errorTracker.statusCode = result.statusCode;
					};
					bx:http url="%s" onChunk=onChunkFn throwOnError=false;
					result = bxhttp;
				""",
			        baseURL + "/test-streaming-error"
			    ),
			    context
			);
		// @formatter:on

		// Verify chunks were processed even with error status
		IStruct errorTracker = variables.getAsStruct( Key.of( "errorTracker" ) );
		assertThat( errorTracker.getAsBoolean( Key.of( "chunksCalled" ) ) ).isTrue();
		assertThat( errorTracker.getAsInteger( Key.of( "statusCode" ) ) ).isEqualTo( 500 );

		IStruct httpResult = variables.getAsStruct( result );
		assertThat( httpResult.getAsInteger( Key.statusCode ) ).isEqualTo( 500 );
	}

	@DisplayName( "Can track basic HTTP client statistics" )
	@Test
	public void testHttpClientStatistics( WireMockRuntimeInfo wmRuntimeInfo ) {
		String baseURL = wmRuntimeInfo.getHttpBaseUrl();

		// Setup mock endpoint
		stubFor( get( urlEqualTo( "/stats-test" ) )
		    .willReturn( ok( "Test response for statistics" )
		        .withHeader( "Content-Type", "text/plain" ) ) );

		// @formatter:off
		instance.executeSource(
		    String.format(
		        """
					// Get client from HttpService
					client = getBoxRuntime().getHttpService().getOrBuildClient(
						"HTTP/2",
						false,
						null,
						null,
						null,
						null,
						null,
						null,
						null
					);

					// Record creation time
					createdAt = client.getCreatedAt();

					// Verify initial statistics
					initialStats = client.getStatistics();

					// Make a successful request
					client.newRequest( "%s", getBoxContext() ).send();

					// Get updated statistics
					stats = client.getStatistics();
				""",
		        baseURL + "/stats-test"
		    ),
		    context
		);
		// @formatter:on

		// Verify initial statistics
		IStruct initialStats = variables.getAsStruct( Key.of( "initialStats" ) );
		assertThat( initialStats.getAsLong( Key.of( "totalRequests" ) ) ).isEqualTo( 0L );
		assertThat( initialStats.getAsLong( Key.of( "successfulRequests" ) ) ).isEqualTo( 0L );
		assertThat( initialStats.getAsLong( Key.of( "failedRequests" ) ) ).isEqualTo( 0L );
		assertThat( initialStats.getAsLong( Key.of( "bytesReceived" ) ) ).isEqualTo( 0L );
		assertThat( initialStats.getAsLong( Key.of( "bytesSent" ) ) ).isEqualTo( 0L );

		// Verify updated statistics after request
		IStruct stats = variables.getAsStruct( Key.of( "stats" ) );
		assertThat( stats.getAsLong( Key.of( "totalRequests" ) ) ).isEqualTo( 1L );
		assertThat( stats.getAsLong( Key.of( "successfulRequests" ) ) ).isEqualTo( 1L );
		assertThat( stats.getAsLong( Key.of( "failedRequests" ) ) ).isEqualTo( 0L );
		assertThat( stats.getAsLong( Key.of( "bytesReceived" ) ) ).isGreaterThan( 0L );
		assertThat( stats.getAsLong( Key.of( "totalExecutionTimeMs" ) ) ).isGreaterThan( 0L );
		assertThat( stats.getAsLong( Key.of( "minExecutionTimeMs" ) ) ).isGreaterThan( 0L );
		assertThat( stats.getAsLong( Key.of( "maxExecutionTimeMs" ) ) ).isGreaterThan( 0L );
		assertThat( stats.get( Key.of( "lastUsedTimestamp" ) ) ).isNotNull();
		assertThat( stats.get( Key.of( "createdAt" ) ) ).isNotNull();
	}

	@DisplayName( "Can track HTTP client failure statistics" )
	@Test
	public void testHttpClientFailureStatistics( WireMockRuntimeInfo wmRuntimeInfo ) {
		String baseURL = wmRuntimeInfo.getHttpBaseUrl();

		// Setup mock endpoints for different failure types
		stubFor( get( urlEqualTo( "/timeout-test" ) )
		    .willReturn( ok()
		        .withFixedDelay( 5000 ) ) ); // Delay longer than timeout

		stubFor( get( urlEqualTo( "/error-test" ) )
		    .willReturn( aResponse()
		        .withStatus( 500 )
		        .withBody( "Server error" ) ) );

		// @formatter:off
		instance.executeSource(
		    String.format(
		        """
					// Get client from HttpService
					client = getBoxRuntime().getHttpService().getOrBuildClient(
						"HTTP/2",
						false,
						null,
						null,
						null,
						null,
						null,
						null,
						null
					);

					// Test timeout failure
					client.newRequest( "%s", getBoxContext() )
						.timeout( 1 )
						.throwOnError( false )
						.send();

					// Test HTTP error (should be successful request but error status)
					client.newRequest( "%s", getBoxContext() )
						.throwOnError( false )
						.send();

					// Test connection failure (invalid host)
					client.newRequest( "http://invalid-host-that-does-not-exist-12345.com", getBoxContext() )
						.timeout( 1 )
						.throwOnError( false )
						.send();

					// Get statistics
					stats = client.getStatistics();
				""",
		        baseURL + "/timeout-test",
		        baseURL + "/error-test"
		    ),
		    context
		);
		// @formatter:on

		IStruct stats = variables.getAsStruct( Key.of( "stats" ) );

		// Verify total requests (3 attempts)
		assertThat( stats.getAsLong( Key.of( "totalRequests" ) ) ).isEqualTo( 3L );

		// Verify timeout failure was tracked
		assertThat( stats.getAsLong( Key.of( "timeoutFailures" ) ) ).isGreaterThan( 0L );

		// Verify connection failure was tracked
		assertThat( stats.getAsLong( Key.of( "connectionFailures" ) ) ).isGreaterThan( 0L );

		// HTTP error (500) is a successful request, just error status
		assertThat( stats.getAsLong( Key.of( "successfulRequests" ) ) ).isGreaterThan( 0L );
	}

	@DisplayName( "Can track min and max execution times across multiple requests" )
	@Test
	public void testHttpClientExecutionTimeTracking( WireMockRuntimeInfo wmRuntimeInfo ) {
		String baseURL = wmRuntimeInfo.getHttpBaseUrl();

		// Setup mock endpoints with different delays
		stubFor( get( urlEqualTo( "/fast" ) )
		    .willReturn( ok( "Fast response" ) ) );

		stubFor( get( urlEqualTo( "/slow" ) )
		    .willReturn( ok( "Slow response" )
		        .withFixedDelay( 100 ) ) );

		stubFor( get( urlEqualTo( "/medium" ) )
		    .willReturn( ok( "Medium response" )
		        .withFixedDelay( 50 ) ) );

		// @formatter:off
		instance.executeSource(
		    String.format(
		        """
					// Get client from HttpService
					client = getBoxRuntime().getHttpService().getOrBuildClient(
						"HTTP/2",
						false,
						null,
						null,
						null,
						null,
						null,
						null,
						null
					);

					// Make requests with different response times
					client.newRequest( "%s", getBoxContext() ).send();
					client.newRequest( "%s", getBoxContext() ).send();
					client.newRequest( "%s", getBoxContext() ).send();

					// Get statistics
					stats = client.getStatistics();
				""",
		        baseURL + "/fast",
		        baseURL + "/slow",
		        baseURL + "/medium"
		    ),
		    context
		);
		// @formatter:on

		IStruct	stats		= variables.getAsStruct( Key.of( "stats" ) );

		Long	minTime		= stats.getAsLong( Key.of( "minExecutionTimeMs" ) );
		Long	maxTime		= stats.getAsLong( Key.of( "maxExecutionTimeMs" ) );
		Long	totalTime	= stats.getAsLong( Key.of( "totalExecutionTimeMs" ) );
		Long	avgTime		= stats.getAsLong( Key.of( "averageExecutionTimeMs" ) );

		// Verify min is less than max
		assertThat( minTime ).isLessThan( maxTime );

		// Verify total is sum of all executions
		assertThat( totalTime ).isGreaterThan( minTime );
		assertThat( totalTime ).isGreaterThan( maxTime );

		// Verify average makes sense
		assertThat( avgTime ).isGreaterThan( 0L );
		assertThat( avgTime ).isAtLeast( minTime );
		assertThat( avgTime ).isAtMost( maxTime );
	}

	@DisplayName( "Can reset HTTP client statistics" )
	@Test
	public void testHttpClientStatisticsReset( WireMockRuntimeInfo wmRuntimeInfo ) {
		String baseURL = wmRuntimeInfo.getHttpBaseUrl();

		stubFor( get( urlEqualTo( "/reset-test" ) )
		    .willReturn( ok( "Test response" ) ) );

		// @formatter:off
		instance.executeSource(
		    String.format(
		        """
					// Get client from HttpService
					client = getBoxRuntime().getHttpService().getOrBuildClient(
						"HTTP/2",
						false,
						null,
						null,
						null,
						null,
						null,
						null,
						null
					);

					// Reset statistics first (client may be cached and have stats from previous tests)
					client.resetStatistics();
					createdAt = client.getCreatedAt();

					// Make some requests
					client.newRequest( "%s", getBoxContext() ).send();
					client.newRequest( "%s", getBoxContext() ).send();

					// Get statistics before reset
					beforeReset = client.getStatistics();

					// Reset statistics
					client.resetStatistics();

					// Get statistics after reset
					afterReset = client.getStatistics();
					afterCreatedAt = client.getCreatedAt();
				""",
		        baseURL + "/reset-test",
		        baseURL + "/reset-test"
		    ),
		    context
		);
		// @formatter:on

		IStruct	beforeReset	= variables.getAsStruct( Key.of( "beforeReset" ) );
		IStruct	afterReset	= variables.getAsStruct( Key.of( "afterReset" ) );

		// Verify statistics were non-zero before reset
		assertThat( beforeReset.getAsLong( Key.of( "totalRequests" ) ) ).isEqualTo( 2L );
		assertThat( beforeReset.getAsLong( Key.of( "successfulRequests" ) ) ).isEqualTo( 2L );

		// Verify statistics were reset to zero
		assertThat( afterReset.getAsLong( Key.of( "totalRequests" ) ) ).isEqualTo( 0L );
		assertThat( afterReset.getAsLong( Key.of( "successfulRequests" ) ) ).isEqualTo( 0L );
		assertThat( afterReset.getAsLong( Key.of( "failedRequests" ) ) ).isEqualTo( 0L );
		assertThat( afterReset.getAsLong( Key.of( "bytesReceived" ) ) ).isEqualTo( 0L );
		assertThat( afterReset.getAsLong( Key.of( "bytesSent" ) ) ).isEqualTo( 0L );
		assertThat( afterReset.getAsLong( Key.of( "totalExecutionTimeMs" ) ) ).isEqualTo( 0L );

		// Verify minExecutionTimeMs was reset to 0 (sentinel value handled)
		assertThat( afterReset.getAsLong( Key.of( "minExecutionTimeMs" ) ) ).isEqualTo( 0L );
		assertThat( afterReset.getAsLong( Key.of( "maxExecutionTimeMs" ) ) ).isEqualTo( 0L );

		// Verify createdAt timestamp was NOT reset (immutable)
		assertThat( afterReset.get( Key.of( "createdAt" ) ) ).isEqualTo( beforeReset.get( Key.of( "createdAt" ) ) );
	}

	private void createClientCertificate( String certPath, String certPassword ) throws Exception {
		// Generate a key pair
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance( "RSA" );
		keyGen.initialize( 2048, new SecureRandom() );
		KeyPair			keyPair		= keyGen.generateKeyPair();
		PrivateKey		privateKey	= keyPair.getPrivate();

		// Create a self-signed certificate
		X509Certificate	cert		= null;
		try {
			cert = generateSelfSignedCertificate( keyPair );
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "An error occurred when attempting to create the test certificate", e );
		}

		// Create a KeyStore and store the certificate and private key
		KeyStore keyStore = KeyStore.getInstance( "PKCS12" );
		keyStore.load( null, null );
		keyStore.setKeyEntry( "client-cert", privateKey, certPassword.toCharArray(), new Certificate[] { cert } );

		// Save the KeyStore to a file
		certPath = Path.of( certPath ).toAbsolutePath().toString();
		File certFile = new File( certPath );
		FileUtils.touch( certFile );
		try ( FileOutputStream fos = new FileOutputStream( certPath ) ) {
			keyStore.store( fos, certPassword.toCharArray() );
		}
	}

	@DisplayName( "Test asJSON transformer" )
	@Test
	public void testAsJSONTransformer( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( urlEqualTo( "/json-data" ) )
		        .willReturn(
		            aResponse()
		                .withStatus( 200 )
		                .withHeader( "Content-Type", "application/json" )
		                .withBody( "{\"name\":\"John\",\"age\":30,\"active\":true}" )
		        )
		);

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();
		// @formatter:off
		instance.executeSource(
		    String.format( """
		                   result = http( "%s/json-data" )
		                       .get()
		                       .asJSON()
		                       .send();
		                   """, baseURL ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		Object jsonResult = variables.get( result );
		assertThat( jsonResult ).isInstanceOf( IStruct.class );
		IStruct data = ( IStruct ) jsonResult;
		assertThat( data.getAsString( Key.of( "name" ) ) ).isEqualTo( "John" );
		assertThat( data.getAsInteger( Key.of( "age" ) ) ).isEqualTo( 30 );
		assertThat( data.getAsBoolean( Key.of( "active" ) ) ).isTrue();
	}

	@DisplayName( "Test asJSON transformer with array response" )
	@Test
	public void testAsJSONTransformerArray( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( urlEqualTo( "/json-array" ) )
		        .willReturn(
		            aResponse()
		                .withStatus( 200 )
		                .withHeader( "Content-Type", "application/json" )
		                .withBody( "[\"apple\",\"banana\",\"orange\"]" )
		        )
		);

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();
		// @formatter:off
		instance.executeSource(
		    String.format( """
		                   result = http( "%s/json-array" )
		                       .get()
		                       .asJSON()
		                       .send();
		                   """, baseURL ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		Object jsonResult = variables.get( result );
		assertThat( jsonResult ).isInstanceOf( Array.class );
		Array data = ( Array ) jsonResult;
		assertThat( data.size() ).isEqualTo( 3 );
		assertThat( data.get( 0 ) ).isEqualTo( "apple" );
		assertThat( data.get( 1 ) ).isEqualTo( "banana" );
		assertThat( data.get( 2 ) ).isEqualTo( "orange" );
	}

	@DisplayName( "Test asText transformer" )
	@Test
	public void testAsTextTransformer( WireMockRuntimeInfo wmRuntimeInfo ) {
		String testContent = "This is plain text content from the server.";
		stubFor(
		    get( urlEqualTo( "/text-data" ) )
		        .willReturn(
		            aResponse()
		                .withStatus( 200 )
		                .withHeader( "Content-Type", "text/plain" )
		                .withBody( testContent )
		        )
		);

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();
		// @formatter:off
		instance.executeSource(
		    String.format( """
		                   result = http( "%s/text-data" )
		                       .get()
		                       .asText()
		                       .send();
		                   """, baseURL ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		Object textResult = variables.get( result );
		assertThat( textResult ).isInstanceOf( String.class );
		assertThat( textResult ).isEqualTo( testContent );
	}

	@DisplayName( "Test asXML transformer" )
	@Test
	public void testAsXMLTransformer( WireMockRuntimeInfo wmRuntimeInfo ) {
		String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><item>Test</item><value>123</value></root>";
		stubFor(
		    get( urlEqualTo( "/xml-data" ) )
		        .willReturn(
		            aResponse()
		                .withStatus( 200 )
		                .withHeader( "Content-Type", "application/xml" )
		                .withBody( xmlContent )
		        )
		);

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();
		// @formatter:off
		instance.executeSource(
		    String.format( """
		                   result = http( "%s/xml-data" )
		                       .get()
		                       .asXML()
		                       .send();
		                   """, baseURL ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		Object xmlResult = variables.get( result );
		assertThat( xmlResult ).isInstanceOf( ortus.boxlang.runtime.types.XML.class );
		// Verify it's a valid XML object by accessing it
		ortus.boxlang.runtime.types.XML xmlDoc = ( ortus.boxlang.runtime.types.XML ) xmlResult;
		assertThat( xmlDoc ).isNotNull();
	}

	@DisplayName( "Test custom transformer with BoxLang function" )
	@Test
	public void testCustomTransformer( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( urlEqualTo( "/custom-data" ) )
		        .willReturn(
		            aResponse()
		                .withStatus( 200 )
		                .withBody( "UPPERCASE TEXT" )
		        )
		);

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();
		// @formatter:off
		instance.executeSource(
		    String.format( """
		                   result = http( "%s/custom-data" )
		                       .get()
		                       .transform( (httpResult) => {
		                           return lCase( httpResult.fileContent );
		                       } )
		                       .send();
		                   """, baseURL ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		Object transformedResult = variables.get( result );
		assertThat( transformedResult ).isInstanceOf( String.class );
		assertThat( transformedResult ).isEqualTo( "uppercase text" );
	}

	@DisplayName( "Test transformer with async request" )
	@Test
	public void testTransformerWithAsync( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( urlEqualTo( "/async-json" ) )
		        .willReturn(
		            aResponse()
		                .withStatus( 200 )
		                .withHeader( "Content-Type", "application/json" )
		                .withBody( "{\"status\":\"success\"}" )
		        )
		);

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();
		// @formatter:off
		instance.executeSource(
		    String.format( """
		                   future = http( "%s/async-json" )
		                       .get()
		                       .asJSON()
		                       .sendAsync();
		                   result = future.get();
		                   """, baseURL ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		Object jsonResult = variables.get( result );
		assertThat( jsonResult ).isInstanceOf( IStruct.class );
		IStruct data = ( IStruct ) jsonResult;
		assertThat( data.getAsString( Key.of( "status" ) ) ).isEqualTo( "success" );
	}

	@DisplayName( "Test no transformer returns httpResult struct" )
	@Test
	public void testNoTransformer( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( urlEqualTo( "/no-transform" ) )
		        .willReturn(
		            aResponse()
		                .withStatus( 200 )
		                .withBody( "test content" )
		        )
		);

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();
		// @formatter:off
		instance.executeSource(
		    String.format( """
		                   result = http( "%s/no-transform" )
		                       .get()
		                       .send();
		                   """, baseURL ),
		    context, BoxSourceType.BOXSCRIPT
		);
		// @formatter:on

		Object httpResult = variables.get( result );
		assertThat( httpResult ).isInstanceOf( IStruct.class );
		IStruct resultStruct = ( IStruct ) httpResult;
		assertThat( resultStruct.containsKey( Key.statusCode ) ).isTrue();
		assertThat( resultStruct.containsKey( Key.fileContent ) ).isTrue();
		assertThat( resultStruct.getAsInteger( Key.statusCode ) ).isEqualTo( 200 );
	}

	private X509Certificate generateSelfSignedCertificate( KeyPair keyPair ) throws Exception {

		if ( Security.getProvider( BouncyCastleProvider.PROVIDER_NAME ) == null ) {
			Security.addProvider( new BouncyCastleProvider() );
		}

		// Create the certificate
		X500Principal	subject		= new X500Principal( "CN=Test Certificate" );
		Calendar		calendar	= Calendar.getInstance();
		calendar.setTime( new Date() );
		Date startDate = calendar.getTime();
		calendar.add( Calendar.YEAR, 1 );
		Date						endDate		= calendar.getTime();

		// Generate the certificate
		ContentSigner				signer		= new JcaContentSignerBuilder( "SHA256withRSA" ).build( keyPair.getPrivate() );
		X509v3CertificateBuilder	certBuilder	= new JcaX509v3CertificateBuilder(
		    subject,
		    BigInteger.valueOf( System.currentTimeMillis() ),
		    startDate,
		    endDate,
		    subject,
		    keyPair.getPublic() );

		return new JcaX509CertificateConverter().setProvider( "BC" ).getCertificate( certBuilder.build( signer ) );
	}

}