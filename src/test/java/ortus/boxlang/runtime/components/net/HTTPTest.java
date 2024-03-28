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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.created;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
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
	static Key			cfhttp	= new Key( "cfhttp" );

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
		                                       result = cfhttp;
		                                        """, baseURL + "/posts/1" ),
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct cfhttp = variables.getAsStruct( result );
		assertThat( cfhttp.get( Key.statusCode ) ).isEqualTo( 200 );
		assertThat( cfhttp.get( Key.statusText ) ).isEqualTo( "OK" );
		assertThat( cfhttp.getAsString( Key.fileContent ).replaceAll( "\\s+", "" ) ).isEqualTo( "Done" );
	}

	@DisplayName( "It parses returned cookies into a Cookies query object" )
	@Test
	public void testCookiesInQuery( WireMockRuntimeInfo wmRuntimeInfo ) {
		stubFor( get( "/cookies" ).willReturn( ok().withHeader( "Set-Cookie", "foo=bar;path=/;secure;samesite=none;httponly" ) ) );

		instance.executeSource( String.format( "http url=\"%s\" {}", wmRuntimeInfo.getHttpBaseUrl() + "/cookies" ),
		    context );

		assertThat( variables.get( cfhttp ) ).isInstanceOf( IStruct.class );

		IStruct res = variables.getAsStruct( cfhttp );
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

		assertThat( variables.get( cfhttp ) ).isInstanceOf( IStruct.class );

		IStruct res = variables.getAsStruct( cfhttp );
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

		assertThat( variables.get( cfhttp ) ).isInstanceOf( IStruct.class );

		IStruct res = variables.getAsStruct( cfhttp );
		assertThat( res.get( Key.statusCode ) ).isEqualTo( 201 );
		assertThat( res.get( Key.statusText ) ).isEqualTo( "Created" );
		String body = res.getAsString( Key.fileContent );
		assertThat( body ).isEqualTo( "{\"id\": 1, \"name\": \"foobar\", \"body\": \"lorem ipsum dolor\"}" );
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

		assertThat( variables.get( cfhttp ) ).isInstanceOf( IStruct.class );

		IStruct res = variables.getAsStruct( cfhttp );
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
		                     <cfhttp result="result" url="%s"></cfhttp>
		                   """, wmRuntimeInfo.getHttpBaseUrl() + "/posts/1" ),
		    context, BoxSourceType.CFTEMPLATE );

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
		                   result = cfhttp;
		                    """, wmRuntimeInfo.getHttpBaseUrl() + "/posts" ),
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct cfhttp = variables.getAsStruct( result );

		Assertions.assertTrue( cfhttp.containsKey( Key.statusCode ) );
		assertThat( cfhttp.get( Key.statusCode ) ).isEqualTo( 200 );
		Assertions.assertTrue( cfhttp.containsKey( Key.status_code ) );
		assertThat( cfhttp.get( Key.status_code ) ).isEqualTo( 200 );

		Assertions.assertTrue( cfhttp.containsKey( Key.statusText ) );
		assertThat( cfhttp.get( Key.statusText ) ).isEqualTo( "OK" );
		Assertions.assertTrue( cfhttp.containsKey( Key.status_text ) );
		assertThat( cfhttp.get( Key.status_text ) ).isEqualTo( "OK" );

		Assertions.assertTrue( cfhttp.containsKey( Key.HTTP_Version ) );
		assertThat( cfhttp.getAsString( Key.HTTP_Version ) ).isEqualTo( "HTTP/2" );

		Assertions.assertTrue( cfhttp.containsKey( Key.errorDetail ) );
		assertThat( cfhttp.getAsString( Key.errorDetail ) ).isEqualTo( "" );

		Assertions.assertTrue( cfhttp.containsKey( Key.mimetype ) );
		assertThat( cfhttp.getAsString( Key.mimetype ) ).isEqualTo( "application/json" );

		Assertions.assertTrue( cfhttp.containsKey( Key.charset ) );
		assertThat( cfhttp.getAsString( Key.charset ) ).isEqualTo( "utf-8" );

		Assertions.assertTrue( cfhttp.containsKey( Key.cookies ) );
		Query cookies = cfhttp.getAsQuery( Key.cookies );
		Assertions.assertNotNull( cookies );
		assertThat( cookies ).isInstanceOf( Query.class );
		Assertions.assertEquals( 0, cookies.size() );

		Assertions.assertTrue( cfhttp.containsKey( Key.fileContent ) );
		assertThat( cfhttp.getAsString( Key.fileContent ).replaceAll( "\\s+", "" ) ).isEqualTo(
		    """
		    	[{"userId":1,"id":1,"title":"suntautfacererepellatprovidentoccaecatiexcepturioptioreprehenderit","body":"quiaetsuscipit\\nsuscipitrecusandaeconsequunturexpeditaetcum\\nreprehenderitmolestiaeututquastotam\\nnostrumrerumestautemsuntremevenietarchitecto"},{"userId":1,"id":2,"title":"quiestesse","body":"estrerumtemporevitae\\nsequisintnihilreprehenderitdolorbeataeeadoloresneque\\nfugiatblanditiisvoluptateporrovelnihilmolestiaeutreiciendis\\nquiaperiamnondebitispossimusquinequenisinulla"},{"userId":1,"id":3,"title":"eamolestiasquasiexercitationemrepellatquiipsasitaut","body":"etiustosedquoiure\\nvoluptatemoccaecatiomniseligendiautad\\nvoluptatemdoloribusvelaccusantiumquispariatur\\nmolestiaeporroeiusodioetlaboreetvelitaut"},{"userId":1,"id":4,"title":"eumetestoccaecati","body":"ullametsaepereiciendisvoluptatemadipisci\\nsitametautemassumendaprovidentrerumculpa\\nquishiccommodinesciuntremteneturdoloremqueipsamiure\\nquissuntvoluptatemrerumillovelit"},{"userId":1,"id":5,"title":"nesciuntquasodio","body":"repudiandaeveniamquaeratsuntsed\\naliasautfugiatsitautemsedest\\nvoluptatemomnispossimusessevoluptatibusquis\\nestautteneturdolorneque"},{"userId":1,"id":6,"title":"doloremeummagnieosaperiamquia","body":"utaspernaturcorporisharumnihilquisprovidentsequi\\nmollitianobisaliquidmolestiae\\nperspiciatiseteanemoabreprehenderitaccusantiumquas\\nvoluptatedoloresvelitetdoloremquemolestiae"},{"userId":1,"id":7,"title":"magnamfacilisautem","body":"doloreplaceatquibusdameaquovitae\\nmagniquisenimquiquisquonemoautsaepe\\nquidemrepellatexcepturiutquia\\nsuntutsequieoseasedquas"},{"userId":1,"id":8,"title":"doloremdoloreestipsam","body":"dignissimosaperiamdoloremquieum\\nfacilisquibusdamanimisintsuscipitquisintpossimuscum\\nquaeratmagnimaioresexcepturi\\nipsamutcommodidolorvoluptatummodiautvitae"},{"userId":1,"id":9,"title":"nesciuntiureomnisdoloremtemporaetaccusantium","body":"consecteturaniminesciuntiuredolore\\nenimquiaad\\nveniamautemutquamautnobis\\netestautquodautprovidentvoluptasautemvoluptas"},{"userId":1,"id":10,"title":"optiomolestiasidquiaeum","body":"quoetexpeditamodicumofficiavelmagni\\ndoloribusquirepudiandae\\nveronisisit\\nquosveniamquodsedaccusamusveritatiserror"}]
		    """.replaceAll(
		        "\\s+", "" ) );
	}

}
