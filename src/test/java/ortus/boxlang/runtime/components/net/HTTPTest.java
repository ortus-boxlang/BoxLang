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
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.google.common.truth.Truth.assertThat;

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
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;

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
	public void testCanMakeHTTPCallScript( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor( get( "/posts/1" ).willReturn( aResponse().withBody( "Done" ).withStatus( 200 ) ) );

		String baseURL = wmRuntimeInfo.getHttpBaseUrl();

		instance.executeSource( String.format( """
		                                        http url="%s" {
		                                            httpparam type="header" name="User-Agent" value="Mozilla";
		                                       }
		                                       result = bxhttp;
		                                        """, baseURL + "/posts/1" ),
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct bxhttp = variables.getAsStruct( result );
		assertThat( bxhttp.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( bxhttp.get( Key.statusText ) ).isEqualTo( "OK" );
		assertThat( bxhttp.getAsString( Key.fileContent ).replaceAll( "\\s+", "" ) ).isEqualTo( "Done" );
	}

	@DisplayName( "It parses returned cookies into a Cookies query object" )
	@Test
	public void testCookiesInQuery( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor( get( "/cookies" ).willReturn( ok().withHeader( "Set-Cookie", "foo=bar;path=/;secure;samesite=none;httponly" ) ) );

		instance.executeSource( String.format( "http url=\"%s\" {}", wmRuntimeInfo.getHttpBaseUrl() + "/cookies" ),
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

		instance.executeSource( String.format( "http url=\"%s\" {}", wmRuntimeInfo.getHttpBaseUrl() + "/cookies" ),
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
		                                       http method="POST" url="%s" {
		                                       	httpparam type="formfield" name="name" value="foobar";
		                                       	httpparam type="formfield" name="body" value="lorem ipsum dolor";
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
			http method="POST" url="%s" {
				httpparam type="formfield" name="tags" value="tag-a";
				httpparam type="formfield" name="tags" value="tag-b";
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
		                                       http method="POST" url="%s" {
		                                       	httpparam type="body" value="#JSONSerialize( { 'name': 'foobar', 'body': 'lorem ipsum dolor' } )#";
		                                       }
		                                       """, wmRuntimeInfo.getHttpBaseUrl() + "/posts" ), context );

		assertThat( variables.get( bxhttp ) ).isInstanceOf( IStruct.class );

		IStruct res = variables.getAsStruct( bxhttp );
		assertThat( res.get( Key.statusCode ) ).isEqualTo( 201 );
		assertThat( res.get( Key.statusText ) ).isEqualTo( "Created" );
		String body = res.getAsString( Key.fileContent );
		assertThat( body ).isEqualTo( "{\"id\": 1, \"name\": \"foobar\", \"body\": \"lorem ipsum dolor\"}" );
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

	@DisplayName( "It can make a GET request with URL params" )
	@Test
	public void testGetWithParams( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor(
		    get( "/posts?userId=1" ).willReturn( ok().withHeader( "Content-Type", "application/json; charset=utf-8" ).withBody(
		        """
		        [{"userId":1,"id":1,"title":"suntautfacererepellatprovidentoccaecatiexcepturioptioreprehenderit","body":"quiaetsuscipit\\nsuscipitrecusandaeconsequunturexpeditaetcum\\nreprehenderitmolestiaeututquastotam\\nnostrumrerumestautemsuntremevenietarchitecto"},{"userId":1,"id":2,"title":"quiestesse","body":"estrerumtemporevitae\\nsequisintnihilreprehenderitdolorbeataeeadoloresneque\\nfugiatblanditiisvoluptateporrovelnihilmolestiaeutreiciendis\\nquiaperiamnondebitispossimusquinequenisinulla"},{"userId":1,"id":3,"title":"eamolestiasquasiexercitationemrepellatquiipsasitaut","body":"etiustosedquoiure\\nvoluptatemoccaecatiomniseligendiautad\\nvoluptatemdoloribusvelaccusantiumquispariatur\\nmolestiaeporroeiusodioetlaboreetvelitaut"},{"userId":1,"id":4,"title":"eumetestoccaecati","body":"ullametsaepereiciendisvoluptatemadipisci\\nsitametautemassumendaprovidentrerumculpa\\nquishiccommodinesciuntremteneturdoloremqueipsamiure\\nquissuntvoluptatemrerumillovelit"},{"userId":1,"id":5,"title":"nesciuntquasodio","body":"repudiandaeveniamquaeratsuntsed\\naliasautfugiatsitautemsedest\\nvoluptatemomnispossimusessevoluptatibusquis\\nestautteneturdolorneque"},{"userId":1,"id":6,"title":"doloremeummagnieosaperiamquia","body":"utaspernaturcorporisharumnihilquisprovidentsequi\\nmollitianobisaliquidmolestiae\\nperspiciatiseteanemoabreprehenderitaccusantiumquas\\nvoluptatedoloresvelitetdoloremquemolestiae"},{"userId":1,"id":7,"title":"magnamfacilisautem","body":"doloreplaceatquibusdameaquovitae\\nmagniquisenimquiquisquonemoautsaepe\\nquidemrepellatexcepturiutquia\\nsuntutsequieoseasedquas"},{"userId":1,"id":8,"title":"doloremdoloreestipsam","body":"dignissimosaperiamdoloremquieum\\nfacilisquibusdamanimisintsuscipitquisintpossimuscum\\nquaeratmagnimaioresexcepturi\\nipsamutcommodidolorvoluptatummodiautvitae"},{"userId":1,"id":9,"title":"nesciuntiureomnisdoloremtemporaetaccusantium","body":"consecteturaniminesciuntiuredolore\\nenimquiaad\\nveniamautemutquamautnobis\\netestautquodautprovidentvoluptasautemvoluptas"},{"userId":1,"id":10,"title":"optiomolestiasidquiaeum","body":"quoetexpeditamodicumofficiavelmagni\\ndoloribusquirepudiandae\\nveronisisit\\nquosveniamquodsedaccusamusveritatiserror"}]""" ) ) );

		instance.executeSource(
		    String.format( """
		                    http url="%s" {
		                   	 httpparam type="url" name="userId" value=1;
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
			http method="GET" url="https://does-not-exist.also-does-not-exist" {
				httpparam type="header" name="User-Agent" value="HyperCFML/7.5.2";
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
			http timeout="1" method="GET" url="%s" {
				httpparam type="header" name="User-Agent" value="HyperCFML/7.5.2";
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
		assertThat( bxhttp.get( Key.errorDetail ) ).isEqualTo( "Request timed out after 1 second." );
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
			http method="POST" url="%s" {
				httpparam type="file" name="photo" file="/src/test/resources/chuck_norris.jpg";
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
			http method="POST" url="%s" {
				httpparam type="file" name="photo" file="/src/test/resources/chuck_norris.jpg";
				httpparam type="formfield" name="joke" value="Chuck Norris can divide by zero.";
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

}
