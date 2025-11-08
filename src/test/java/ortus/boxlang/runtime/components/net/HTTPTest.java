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
		            .withStatus( 200 ) )
		);

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();

		// @formatter:off
		instance.executeSource(
		    String.format( """
					bx:http url="%s" {
						bx:httpparam type="header" name="User-Agent" value="Mozilla";
					}
					result = bxhttp
					println( result )
				""",
		        baseURL + "/posts/1"
		    ),
		    context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		IStruct bxhttp = variables.getAsStruct( result );
		assertThat( bxhttp.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( bxhttp.get( Key.statusText ) ).isEqualTo( "OK" );
		assertThat( bxhttp.getAsString( Key.fileContent ).replaceAll( "\\s+", "" ) ).isEqualTo( "Done" );
	}

	@DisplayName( "It can make HTTP with port attribute" )
	@Test
	public void testCanMakeHTTPRequestPort( WireMockRuntimeInfo wmRuntimeInfo ) {
		// this is the good request - it would return a 200
		stubFor( get( "/posts/1" ).willReturn( aResponse().withBody( "Done" ).withStatus( 200 ) ) );

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();

		instance.executeSource( String.format( """
		                                        bx:http url="%s" port="12345" {
		                                            bx:httpparam type="header" name="User-Agent" value="Mozilla";
		                                       }
		                                       result = bxhttp;
		                                        """, baseURL + "/posts/1" ),
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		// Our port change should produce a bad gateway error
		IStruct bxhttp = variables.getAsStruct( result );
		assertThat( bxhttp.get( Key.statusCode ) ).isEqualTo( 502 );
		assertThat( bxhttp.get( Key.statusText ) ).isEqualTo( "Bad Gateway" );
		assertThat( bxhttp.getAsString( Key.fileContent ).replaceAll( "\\s+", "" ) ).isEqualTo( "ConnectionFailure" );
	}

	@DisplayName( "It parses returned cookies into a Cookies query object" )
	@Test
	public void testCookiesInQuery( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor( get( "/cookies" ).willReturn( ok().withHeader( "Set-Cookie", "foo=bar;path=/;secure;samesite=none;httponly" ) ) );

		instance.executeSource( String.format( "bx:http url=\"%s\" {}", wmRuntimeInfo.getHttpBaseUrl() + "/cookies" ),
		    context );

		assertThat( variables.get( bxhttp ) ).isInstanceOf( IStruct.class );

		IStruct res = variables.getAsStruct( bxhttp );
		assertThat( res.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( res.get( Key.statusText ) ).isEqualTo( "OK" );
		Query cookies = res.getAsQuery( Key.cookies );
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
		    .withHeader( "Set-Cookie", "one=two;max-age=2592000;domain=example.com" )
		) );

		instance.executeSource( String.format( "bx:http url=\"%s\" {}", wmRuntimeInfo.getHttpBaseUrl() + "/cookies" ),
		    context );

		assertThat( variables.get( bxhttp ) ).isInstanceOf( IStruct.class );

		IStruct res = variables.getAsStruct( bxhttp );
		assertThat( res.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( res.get( Key.statusText ) ).isEqualTo( "OK" );
		Query cookies = res.getAsQuery( Key.cookies );
		Assertions.assertEquals( 3, cookies.size() );
		assertThat( cookies.getRow( 0 ) ).isEqualTo( new Object[] { "foo", "bar", "/", "", "", true, true, "none" } );
		assertThat( cookies.getRow( 1 ) ).isEqualTo( new Object[] { "baz", "qux", "/", "", "Mon, 31 Dec 2038 23:59:59 GMT", "", "", "" } );
		assertThat( cookies.getRow( 2 ) ).isEqualTo( new Object[] { "one", "two", "", "example.com", "30", "", "", "" } );
	}

	@DisplayName( "It can make a post request with form params" )
	@Test
	public void testPostFormParams( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    post( "/posts" )
		        .withFormParam( "name", equalTo( "foobar" ) )
		        .withFormParam( "body", equalTo( "lorem ipsum dolor" ) )
		        .willReturn( created().withBody( "{\"id\": 1, \"name\": \"foobar\", \"body\": \"lorem ipsum dolor\"}" ) ) );

		instance.executeSource( String.format( """
		                                       bx:http method="POST" url="%s" {
		                                       	bx:httpparam type="formfield" name="name" value="foobar";
		                                       	bx:httpparam type="formfield" name="body" value="lorem ipsum dolor";
		                                       }
		                                       """, wmRuntimeInfo.getHttpBaseUrl() + "/posts" ), context );

		assertThat( variables.get( bxhttp ) ).isInstanceOf( IStruct.class );

		IStruct res = variables.getAsStruct( bxhttp );
		assertThat( res.get( Key.statusCode ) ).isEqualTo( 201 );
		assertThat( res.get( Key.statusText ) ).isEqualTo( "Created" );
		String body = res.getAsString( Key.fileContent );
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

		assertThat( variables.get( bxhttp ) ).isInstanceOf( IStruct.class );

		IStruct res = variables.getAsStruct( bxhttp );
		assertThat( res.get( Key.statusCode ) ).isEqualTo( 201 );
		assertThat( res.get( Key.statusText ) ).isEqualTo( "Created" );
		String body = res.getAsString( Key.fileContent );
		assertThat( body ).isEqualTo( "{\"id\": 1, \"tags\": [ \"tag-a\", \"tag-b\" ] }" );
	}

	@DisplayName( "It can make a post request with a json body" )
	@Test
	public void testPostJsonBody( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    post( "/posts" )
		        .withRequestBody( equalToJson( "{\"name\": \"foobar\", \"body\": \"lorem ipsum dolor\"}" ) )
		        .willReturn( created().withBody( "{\"id\": 1, \"name\": \"foobar\", \"body\": \"lorem ipsum dolor\"}" ) ) );

		instance.executeSource( String.format( """
		                                       bx:http method="POST" url="%s" {
		                                       	bx:httpparam type="body" value="#JSONSerialize( { 'name': 'foobar', 'body': 'lorem ipsum dolor' } )#";
		                                       }
		                                       """, wmRuntimeInfo.getHttpBaseUrl() + "/posts" ), context );

		assertThat( variables.get( bxhttp ) ).isInstanceOf( IStruct.class );

		IStruct res = variables.getAsStruct( bxhttp );
		assertThat( res.get( Key.statusCode ) ).isEqualTo( 201 );
		assertThat( res.get( Key.statusText ) ).isEqualTo( "Created" );
		String body = res.getAsString( Key.fileContent );
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
		                .withBody( ( byte[] ) FileSystemUtil.read( "src/test/resources/chuck_norris.jpg" ) ) ) );

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

		assertThat( variables.get( bxhttp ) ).isInstanceOf( IStruct.class );

		IStruct res = variables.getAsStruct( bxhttp );
		assertThat( res.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( res.get( Key.statusText ) ).isEqualTo( "OK" );
		Object body = res.get( Key.fileContent );
		assertThat( body ).isInstanceOf( byte[].class );

		// Now test with getAsBinary set to "no" which should still return binary content
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

		assertThat( variables.get( bxhttp ) ).isInstanceOf( IStruct.class );

		res = variables.getAsStruct( bxhttp );
		assertThat( res.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( res.get( Key.statusText ) ).isEqualTo( "OK" );
		body = res.get( Key.fileContent );
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

		assertThat( variables.get( bxhttp ) ).isInstanceOf( IStruct.class );

		IStruct res = variables.getAsStruct( bxhttp );
		assertThat( res.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( res.get( Key.statusText ) ).isEqualTo( "OK" );
		Object body = res.get( Key.fileContent );
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
		                .withHeader( "Content-Disposition", "attachment; filename=\"chuck_norris_dl.jpg\"" )
		                .withBody( ( byte[] ) FileSystemUtil.read( "src/test/resources/chuck_norris.jpg" ) ) ) );

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
		        .willReturn( notFound() )
		);

		// @formatter:off
		instance.executeSource( String.format(
			"""
			bx:http method="GET" getAsBinary=true url="%s" path="src/test/resources/tmp/http_tests" file="bad-image.jpg" result="httpResult" {}
			""",
			wmRuntimeInfo.getHttpBaseUrl() + "/bad-image.jpg" ),
			context
		);
		// @formatter:on
		assertThat( variables.get( Key.of( "httpResult" ) ) ).isInstanceOf( IStruct.class );
		IStruct res = variables.getAsStruct( Key.of( "httpResult" ) );
		assertThat( res.get( Key.statusCode ) ).isEqualTo( 404 );
		assertThat( FileSystemUtil.exists( "src/test/resources/tmp/http_tests/bad-image.jpg" ) ).isTrue();
	}

	@DisplayName( "It can write out a binary file using a specified filename" )
	@Test
	public void testBinaryFileWriteWithName( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( "/image" )
		        .willReturn(
		            ok().withHeader( "Content-Type", "image/jpeg; charset=utf-8" )
		                .withHeader( "Content-Disposition", "attachment; filename=\"chuck_norris_dl.jpg\"" )
		                .withBody( ( byte[] ) FileSystemUtil.read( "src/test/resources/chuck_norris.jpg" ) ) ) );

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
		                .withHeader( "Content-Disposition", "attachment; filename=\"chuck_norris_dl.jpg\"" )
		                .withBody( ( byte[] ) FileSystemUtil.read( "src/test/resources/chuck_norris.jpg" ) ) ) );

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
		            ok()
		        )
		);

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

		assertThat( variables.get( bxhttp ) ).isInstanceOf( IStruct.class );

		IStruct res = variables.getAsStruct( bxhttp );
		assertThat( res.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( res.get( Key.statusText ) ).isEqualTo( "OK" );
	}

	@DisplayName( "Will throw an error if binary is returned and getAsBinary is never" )
	@Test
	public void testBinaryThrow( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( "/image" )
		        .willReturn(
		            ok().withHeader( "Content-Type", "image/jpeg; charset=utf-8" )
		                .withBody( ( byte[] ) FileSystemUtil.read( "src/test/resources/chuck_norris.jpg" ) ) ) );

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

		instance.executeSource( String.format( "cfhttp( url=\"%s\", result=\"result\" ) {}", wmRuntimeInfo.getHttpBaseUrl() + "/posts/1" ), context,
		    BoxSourceType.CFSCRIPT );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct bxhttp = variables.getAsStruct( result );
		assertThat( bxhttp.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( bxhttp.get( Key.statusText ) ).isEqualTo( "OK" );
		assertThat( bxhttp.getAsString( Key.fileContent ).replaceAll( "\\s+", "" ) ).isEqualTo(
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

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct bxhttp = variables.getAsStruct( result );
		assertThat( bxhttp.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( bxhttp.get( Key.statusText ) ).isEqualTo( "OK" );
		assertThat( bxhttp.getAsString( Key.fileContent ).replaceAll( "\\s+", "" ) ).isEqualTo(
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

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct bxhttp = variables.getAsStruct( result );
		assertThat( bxhttp.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( bxhttp.get( Key.statusText ) ).isEqualTo( "OK" );
		assertThat( bxhttp.getAsString( Key.fileContent ).replaceAll( "\\s+", "" ) ).isEqualTo(
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

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct bxhttp = variables.getAsStruct( result );
		assertThat( bxhttp.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( bxhttp.get( Key.statusText ) ).isEqualTo( "OK" );
		assertThat( bxhttp.getAsString( Key.fileContent ).replaceAll( "\\s+", "" ) ).isEqualTo(
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

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct bxhttp = variables.getAsStruct( result );
		assertThat( bxhttp.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( bxhttp.get( Key.statusText ) ).isEqualTo( "OK" );
		assertThat( bxhttp.getAsString( Key.fileContent ).replaceAll( "\\s+", "" ) ).isEqualTo(
		    """
		    {
		      "userId": 3,
		      "id": 3,
		      "title": "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
		      "body": "quia et suscipit\\nsuscipit recusandae consequuntur expedita et cum\\nreprehenderit molestiae ut ut quas totam\\nnostrum rerum est autem sunt rem eveniet architecto"
		    }
		    """.replaceAll(
		        "\\s+", "" ) );

	}

	@DisplayName( "It can make a GET request with URL params" )
	@Test
	public void testGetWithParams( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( "/posts?userId=1" ).willReturn( ok().withHeader( "Content-Type", "application/json; charset=utf-8" ).withBody(
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

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct bxhttp = variables.getAsStruct( result );

		Assertions.assertTrue( bxhttp.containsKey( Key.statusCode ) );
		assertThat( bxhttp.get( Key.statusCode ) ).isEqualTo( 200 );
		Assertions.assertTrue( bxhttp.containsKey( Key.status_code ) );
		assertThat( bxhttp.get( Key.status_code ) ).isEqualTo( 200 );

		Assertions.assertTrue( bxhttp.containsKey( Key.statusText ) );
		assertThat( bxhttp.get( Key.statusText ) ).isEqualTo( "OK" );
		Assertions.assertTrue( bxhttp.containsKey( Key.status_text ) );
		assertThat( bxhttp.get( Key.status_text ) ).isEqualTo( "OK" );

		Assertions.assertTrue( bxhttp.containsKey( Key.HTTP_Version ) );
		assertThat( bxhttp.getAsString( Key.HTTP_Version ) ).isEqualTo( "HTTP/2" );

		Assertions.assertTrue( bxhttp.containsKey( Key.errorDetail ) );
		assertThat( bxhttp.getAsString( Key.errorDetail ) ).isEqualTo( "" );

		Assertions.assertTrue( bxhttp.containsKey( Key.mimetype ) );
		assertThat( bxhttp.getAsString( Key.mimetype ) ).isEqualTo( "application/json" );

		Assertions.assertTrue( bxhttp.containsKey( Key.charset ) );
		assertThat( bxhttp.getAsString( Key.charset ) ).isEqualTo( "utf-8" );

		Assertions.assertTrue( bxhttp.containsKey( Key.cookies ) );
		Query cookies = bxhttp.getAsQuery( Key.cookies );
		Assertions.assertNotNull( cookies );
		assertThat( cookies ).isInstanceOf( Query.class );
		Assertions.assertEquals( 0, cookies.size() );

		Assertions.assertTrue( bxhttp.containsKey( Key.fileContent ) );
		assertThat( bxhttp.getAsString( Key.fileContent ).replaceAll( "\\s+", "" ) ).isEqualTo(
		    """
		    	[{"userId":1,"id":1,"title":"suntautfacererepellatprovidentoccaecatiexcepturioptioreprehenderit","body":"quiaetsuscipit\\nsuscipitrecusandaeconsequunturexpeditaetcum\\nreprehenderitmolestiaeututquastotam\\nnostrumrerumestautemsuntremevenietarchitecto"},{"userId":1,"id":2,"title":"quiestesse","body":"estrerumtemporevitae\\nsequisintnihilreprehenderitdolorbeataeeadoloresneque\\nfugiatblanditiisvoluptateporrovelnihilmolestiaeutreiciendis\\nquiaperiamnondebitispossimusquinequenisinulla"},{"userId":1,"id":3,"title":"eamolestiasquasiexercitationemrepellatquiipsasitaut","body":"etiustosedquoiure\\nvoluptatemoccaecatiomniseligendiautad\\nvoluptatemdoloribusvelaccusantiumquispariatur\\nmolestiaeporroeiusodioetlaboreetvelitaut"},{"userId":1,"id":4,"title":"eumetestoccaecati","body":"ullametsaepereiciendisvoluptatemadipisci\\nsitametautemassumendaprovidentrerumculpa\\nquishiccommodinesciuntremteneturdoloremqueipsamiure\\nquissuntvoluptatemrerumillovelit"},{"userId":1,"id":5,"title":"nesciuntquasodio","body":"repudiandaeveniamquaeratsuntsed\\naliasautfugiatsitautemsedest\\nvoluptatemomnispossimusessevoluptatibusquis\\nestautteneturdolorneque"},{"userId":1,"id":6,"title":"doloremeummagnieosaperiamquia","body":"utaspernaturcorporisharumnihilquisprovidentsequi\\nmollitianobisaliquidmolestiae\\nperspiciatiseteanemoabreprehenderitaccusantiumquas\\nvoluptatedoloresvelitetdoloremquemolestiae"},{"userId":1,"id":7,"title":"magnamfacilisautem","body":"doloreplaceatquibusdameaquovitae\\nmagniquisenimquiquisquonemoautsaepe\\nquidemrepellatexcepturiutquia\\nsuntutsequieoseasedquas"},{"userId":1,"id":8,"title":"doloremdoloreestipsam","body":"dignissimosaperiamdoloremquieum\\nfacilisquibusdamanimisintsuscipitquisintpossimuscum\\nquaeratmagnimaioresexcepturi\\nipsamutcommodidolorvoluptatummodiautvitae"},{"userId":1,"id":9,"title":"nesciuntiureomnisdoloremtemporaetaccusantium","body":"consecteturaniminesciuntiuredolore\\nenimquiaad\\nveniamautemutquamautnobis\\netestautquodautprovidentvoluptasautemvoluptas"},{"userId":1,"id":10,"title":"optiomolestiasidquiaeum","body":"quoetexpeditamodicumofficiavelmagni\\ndoloribusquirepudiandae\\nveronisisit\\nquosveniamquodsedaccusamusveritatiserror"}]
		    """.replaceAll(
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

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct bxhttp = variables.getAsStruct( result );

		Assertions.assertTrue( bxhttp.containsKey( Key.statusCode ) );
		assertThat( bxhttp.get( Key.statusCode ) ).isEqualTo( 502 );
		Assertions.assertTrue( bxhttp.containsKey( Key.status_code ) );
		assertThat( bxhttp.get( Key.status_code ) ).isEqualTo( 502 );

		Assertions.assertTrue( bxhttp.containsKey( Key.statusText ) );
		assertThat( bxhttp.get( Key.statusText ) ).isEqualTo( "Bad Gateway" );
		Assertions.assertTrue( bxhttp.containsKey( Key.status_text ) );
		assertThat( bxhttp.get( Key.status_text ) ).isEqualTo( "Bad Gateway" );

		Assertions.assertTrue( bxhttp.containsKey( Key.fileContent ) );
		assertThat( bxhttp.get( Key.fileContent ) ).isEqualTo( "Connection Failure" );

		Assertions.assertTrue( bxhttp.containsKey( Key.errorDetail ) );
		assertThat( bxhttp.get( Key.errorDetail ) ).isEqualTo( "Unknown host: does-not-exist.also-does-not-exist: Name or service not known." );
	}

	@DisplayName( "It can handle timeouts" )
	@Test
	public void testTimeout( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor( get( "/timeout" ).willReturn( aResponse().withStatus( 200 ).withFixedDelay( 5000 ) ) );

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();
		// @formatter:off
		instance.executeSource( String.format( """
			bx:http timeout="1" method="GET" url="%s" {
				bx:httpparam type="header" name="User-Agent" value="HyperCFML/7.5.2";
			}
			result = bxhttp;
		""", baseURL + "/timeout" ), context );
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct bxhttp = variables.getAsStruct( result );

		Assertions.assertTrue( bxhttp.containsKey( Key.statusCode ) );
		assertThat( bxhttp.get( Key.statusCode ) ).isEqualTo( 408 );
		Assertions.assertTrue( bxhttp.containsKey( Key.status_code ) );
		assertThat( bxhttp.get( Key.status_code ) ).isEqualTo( 408 );

		Assertions.assertTrue( bxhttp.containsKey( Key.statusText ) );
		assertThat( bxhttp.get( Key.statusText ) ).isEqualTo( "Request Timeout" );
		Assertions.assertTrue( bxhttp.containsKey( Key.status_text ) );
		assertThat( bxhttp.get( Key.status_text ) ).isEqualTo( "Request Timeout" );

		Assertions.assertTrue( bxhttp.containsKey( Key.fileContent ) );
		assertThat( bxhttp.get( Key.fileContent ) ).isEqualTo( "Request Timeout" );

		Assertions.assertTrue( bxhttp.containsKey( Key.errorDetail ) );
		assertThat( bxhttp.get( Key.errorDetail ) ).isEqualTo( "The request timed out after 1 second(s)" );
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

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct bxhttp = variables.getAsStruct( result );

		Assertions.assertTrue( bxhttp.containsKey( Key.statusCode ) );
		assertThat( bxhttp.get( Key.statusCode ) ).isEqualTo( 201 );
		Assertions.assertTrue( bxhttp.containsKey( Key.status_code ) );
		assertThat( bxhttp.get( Key.status_code ) ).isEqualTo( 201 );

		Assertions.assertTrue( bxhttp.containsKey( Key.statusText ) );
		assertThat( bxhttp.get( Key.statusText ) ).isEqualTo( "Created" );
		Assertions.assertTrue( bxhttp.containsKey( Key.status_text ) );
		assertThat( bxhttp.get( Key.status_text ) ).isEqualTo( "Created" );

		Assertions.assertTrue( bxhttp.containsKey( Key.fileContent ) );
		assertThat( bxhttp.get( Key.fileContent ) ).isEqualTo( "{\"success\": true }" );
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
		                .withBody( containing( "Chuck Norris can divide by zero." ) )
		        )
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

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct bxhttp = variables.getAsStruct( result );

		Assertions.assertTrue( bxhttp.containsKey( Key.statusCode ) );
		assertThat( bxhttp.get( Key.statusCode ) ).isEqualTo( 201 );
		Assertions.assertTrue( bxhttp.containsKey( Key.status_code ) );
		assertThat( bxhttp.get( Key.status_code ) ).isEqualTo( 201 );

		Assertions.assertTrue( bxhttp.containsKey( Key.statusText ) );
		assertThat( bxhttp.get( Key.statusText ) ).isEqualTo( "Created" );
		Assertions.assertTrue( bxhttp.containsKey( Key.status_text ) );
		assertThat( bxhttp.get( Key.status_text ) ).isEqualTo( "Created" );

		Assertions.assertTrue( bxhttp.containsKey( Key.fileContent ) );
		assertThat( bxhttp.get( Key.fileContent ) ).isEqualTo( "{\"success\": true }" );
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

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct bxhttp = variables.getAsStruct( result );

		Assertions.assertTrue( bxhttp.containsKey( Key.statusCode ) );
		assertThat( bxhttp.get( Key.statusCode ) ).isEqualTo( 200 );

		Assertions.assertTrue( bxhttp.containsKey( Key.fileContent ) );
		assertThat( bxhttp.get( Key.fileContent ) ).isEqualTo( "{\"success\": true }" );
	}

	@DisplayName( "It can process a basic authentication request" )
	@Test
	public void testBasicAuth( WireMockRuntimeInfo wmRuntimeInfo ) {
		String	username			= "admin";
		String	password			= "password";
		String	base64Credentials	= Base64.getEncoder().encodeToString( ( username + ":" + password ).getBytes() );
		stubFor(
		    get( "/posts/1" )
		        .willReturn( ok()
		            .withHeader( "Authorization", "Basic " + base64Credentials )
		            .withHeader( "Content-Type", "application/json; charset=utf-8" )
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
		                     <cfhttp result="result" url="%s" username="%s" password="%s"></bxhttp>
		                   """,
		        wmRuntimeInfo.getHttpBaseUrl() + "/posts/1",
		        username,
		        password
		    ),
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		IStruct bxhttp = variables.getAsStruct( result );
		assertThat( bxhttp.getAsString( Key.header ) ).contains( "authorization: Basic " + base64Credentials );
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
			result = bxhttp;
		""", wmRuntimeInfo.getHttpBaseUrl() + "/posts/2", clientCertPath, clientCertPassword ), context );
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		IStruct bxhttp = variables.getAsStruct( result );
		Assertions.assertTrue( bxhttp.containsKey( Key.statusCode ) );
		assertThat( bxhttp.get( Key.statusCode ) ).isEqualTo( 200 ); // Assuming successful response
		Assertions.assertTrue( bxhttp.containsKey( Key.statusText ) );
		assertThat( bxhttp.get( Key.statusText ) ).isEqualTo( "OK" ); // Assuming successful response
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
		    keyPair.getPublic()
		);

		return new JcaX509CertificateConverter().setProvider( "BC" ).getCertificate( certBuilder.build( signer ) );
	}

	@DisplayName( "It can handle chunked/streaming responses with onChunk callback" )
	@Test
	public void testCanHandleChunkedResponseWithCallback( WireMockRuntimeInfo wmRuntimeInfo ) {
		// Create a large response to ensure multiple chunks
		StringBuilder largeBody = new StringBuilder();
		for ( int i = 0; i < 1000; i++ ) {
			largeBody.append( "Line " ).append( i ).append( " - This is some test data for chunk processing.\n" );
		}

		stubFor( get( "/chunked-data" )
		    .willReturn(
		        aResponse()
		            .withBody( largeBody.toString() )
		            .withStatus( 200 )
		            .withHeader( "Content-Type", "text/plain" ) )
		);

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();

		// @formatter:off
		instance.executeSource(
		    String.format( """
					chunks = []
					chunkCount = 0
					totalBytes = 0
					hasResultInCallback = false
					
					function myCallback(data) {
						chunkCount++
						totalBytes = data.totalReceived
						chunks.append({
							number: data.chunkNumber,
							size: len(data.chunk),
							hasHeaders: structKeyExists(data, 'headers')
						})
						// Verify result is available in callback
						if (structKeyExists(data, 'result')) {
							hasResultInCallback = true
						}
					}
					
					bx:http url="%s" onChunk=myCallback {
					}
					
					result = bxhttp
				""",
		        baseURL + "/chunked-data"
		    ),
		    context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		IStruct bxhttp = variables.getAsStruct( result );
		assertThat( bxhttp.get( Key.statusCode ) ).isEqualTo( 200 );

		// Verify HTTPResult has same keys as non-streaming responses
		assertThat( bxhttp.containsKey( Key.responseHeader ) ).isTrue();
		assertThat( bxhttp.containsKey( Key.errorDetail ) ).isTrue();
		assertThat( bxhttp.containsKey( Key.executionTime ) ).isTrue();

		// Verify chunks were processed
		assertThat( variables.get( Key.of( "chunkCount" ) ) ).isInstanceOf( Integer.class );
		Integer chunkCountValue = ( Integer ) variables.get( Key.of( "chunkCount" ) );
		assertThat( chunkCountValue ).isGreaterThan( 0 );

		// Verify total bytes
		assertThat( variables.get( Key.of( "totalBytes" ) ) ).isInstanceOf( Long.class );
		Long totalBytesValue = ( Long ) variables.get( Key.of( "totalBytes" ) );
		assertThat( totalBytesValue ).isGreaterThan( 0L );

		// Verify chunks array
		assertThat( variables.get( Key.of( "chunks" ) ) ).isInstanceOf( Array.class );
		Array chunksArray = ( Array ) variables.get( Key.of( "chunks" ) );
		assertThat( chunksArray.size() ).isGreaterThan( 0 );

		// Verify first chunk has headers
		IStruct firstChunk = ( IStruct ) chunksArray.get( 0 );
		assertThat( firstChunk.getAsBoolean( Key.of( "hasHeaders" ) ) ).isTrue();

		// Verify subsequent chunks don't have headers (unless it's a single chunk)
		if ( chunksArray.size() > 1 ) {
			IStruct secondChunk = ( IStruct ) chunksArray.get( 1 );
			assertThat( secondChunk.getAsBoolean( Key.of( "hasHeaders" ) ) ).isFalse();
		}

		// Verify chunk numbers are sequential
		for ( int i = 0; i < chunksArray.size(); i++ ) {
			IStruct chunk = ( IStruct ) chunksArray.get( i );
			assertThat( chunk.getAsInteger( Key.of( "number" ) ) ).isEqualTo( i + 1 );
		}

		// Verify result struct is available in callback
		assertThat( variables.get( Key.of( "hasResultInCallback" ) ) ).isInstanceOf( Boolean.class );
		assertThat( ( Boolean ) variables.get( Key.of( "hasResultInCallback" ) ) ).isTrue();
	}

	@DisplayName( "It can handle binary chunked responses with onChunk callback" )
	@Test
	public void testCanHandleBinaryChunkedResponse( WireMockRuntimeInfo wmRuntimeInfo ) {
		byte[] binaryData = new byte[ 10000 ];
		for ( int i = 0; i < binaryData.length; i++ ) {
			binaryData[ i ] = ( byte ) ( i % 256 );
		}

		stubFor( get( "/binary-chunked" )
		    .willReturn(
		        aResponse()
		            .withBody( binaryData )
		            .withStatus( 200 )
		            .withHeader( "Content-Type", "application/octet-stream" ) )
		);

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();

		// @formatter:off
		instance.executeSource(
		    String.format( """
					chunks = []
					totalSize = 0
					
					function binaryCallback(data) {
						chunks.append({
							number: data.chunkNumber,
							isBinary: isBinary(data.chunk)
						})
						totalSize += arrayLen(data.chunk)
					}
					
					bx:http url="%s" getAsBinary="true" onChunk=binaryCallback {
					}
					
					result = bxhttp
				""",
		        baseURL + "/binary-chunked"
		    ),
		    context
		);
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		IStruct bxhttp = variables.getAsStruct( result );
		assertThat( bxhttp.get( Key.statusCode ) ).isEqualTo( 200 );

		// Verify chunks were binary
		Array chunksArray = ( Array ) variables.get( Key.of( "chunks" ) );
		assertThat( chunksArray.size() ).isGreaterThan( 0 );

		for ( Object chunkObj : chunksArray ) {
			IStruct chunk = ( IStruct ) chunkObj;
			assertThat( chunk.getAsBoolean( Key.of( "isBinary" ) ) ).isTrue();
		}

		// Verify total size matches
		Integer totalSize = ( Integer ) variables.get( Key.of( "totalSize" ) );
		assertThat( totalSize ).isEqualTo( binaryData.length );
	}

}
