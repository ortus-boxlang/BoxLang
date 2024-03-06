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
import static org.junit.Assert.*;

import org.junit.jupiter.api.*;

import ortus.boxlang.parser.BoxScriptType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;

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

	@DisplayName( "It can make a GET request with URL params" )
	@Test
	public void testGetWithParams() {
		instance.executeSource(
		    """
		     http url="https://jsonplaceholder.typicode.com/posts" {
		    	 httpparam type="header" name="User-Agent" value="Mozilla";
		    	 httpparam type="url" name="userId" value=1;
		    }
		    result = cfhttp;
		     """,
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

		Assertions.assertTrue( cfhttp.containsKey( Key.responseHeader ) );
		IStruct headers = cfhttp.getAsStruct( Key.responseHeader );
		Assertions.assertNotNull( headers );
		assertThat( headers ).isInstanceOf( IStruct.class );
		Assertions.assertTrue( headers.size() >= 24 );

		Assertions.assertTrue( headers.containsKey( Key.of( "x-ratelimit-remaining" ) ) );

		Assertions.assertTrue( headers.containsKey( Key.of( "reporting-endpoints" ) ) );
		assertThat( headers.get( Key.of( "reporting-endpoints" ) ) ).isEqualTo(
		    "heroku-nel=https://nel.heroku.com/reports?ts=1709586771&sid=e11707d5-02a7-43ef-b45e-2cf4d2036f7d&s=4SezAwb%2BZetUNTPpjh2Z3bLupZCROMh3BmptfECslZ0%3D" );

		Assertions.assertTrue( headers.containsKey( Key.of( "cf-cache-status" ) ) );

		Assertions.assertTrue( headers.containsKey( Key.of( "http_version" ) ) );
		assertThat( headers.get( Key.of( "http_version" ) ) ).isEqualTo( "HTTP/2" );

		Assertions.assertTrue( headers.containsKey( Key.of( "explanation" ) ) );
		assertThat( headers.get( Key.of( "explanation" ) ) ).isEqualTo( "OK" );

		Assertions.assertTrue( headers.containsKey( Key.of( "expires" ) ) );
		assertThat( headers.get( Key.of( "expires" ) ) ).isEqualTo( "-1" );

		Assertions.assertTrue( headers.containsKey( Key.of( "server" ) ) );
		assertThat( headers.get( Key.of( "server" ) ) ).isEqualTo( "cloudflare" );

		Assertions.assertTrue( headers.containsKey( Key.of( "x-powered-by" ) ) );
		assertThat( headers.get( Key.of( "x-powered-by" ) ) ).isEqualTo( "Express" );

		Assertions.assertTrue( headers.containsKey( Key.of( "content-type" ) ) );
		assertThat( headers.get( Key.of( "content-type" ) ) ).isEqualTo( "application/json; charset=utf-8" );

		Assertions.assertTrue( headers.containsKey( Key.of( "pragma" ) ) );
		assertThat( headers.get( Key.of( "pragma" ) ) ).isEqualTo( "no-cache" );

		Assertions.assertTrue( headers.containsKey( Key.of( "vary" ) ) );
		assertThat( headers.get( Key.of( "vary" ) ) ).isEqualTo( "Origin, Accept-Encoding" );

		Assertions.assertTrue( headers.containsKey( Key.of( "x-ratelimit-limit" ) ) );

		Assertions.assertTrue( headers.containsKey( Key.of( "age" ) ) );

		Assertions.assertTrue( headers.containsKey( Key.of( "cache-control" ) ) );
		assertThat( headers.get( Key.of( "cache-control" ) ) ).isEqualTo( "max-age=43200" );

		Assertions.assertTrue( headers.containsKey( Key.of( "cf-ray" ) ) );

		Assertions.assertTrue( headers.containsKey( Key.of( "alt-svc" ) ) );

		Assertions.assertTrue( headers.containsKey( Key.of( "status_code" ) ) );
		assertThat( headers.get( Key.of( "status_code" ) ) ).isEqualTo( "200" );

		Assertions.assertTrue( headers.containsKey( Key.of( "via" ) ) );
		assertThat( headers.get( Key.of( "via" ) ) ).isEqualTo( "1.1 vegur" );

		Assertions.assertTrue( headers.containsKey( Key.of( "date" ) ) );

		Assertions.assertTrue( headers.containsKey( Key.of( "x-ratelimit-reset" ) ) );

		Assertions.assertTrue( headers.containsKey( Key.of( "nel" ) ) );

		Assertions.assertTrue( headers.containsKey( Key.of( "etag" ) ) );

		Assertions.assertTrue( headers.containsKey( Key.of( "x-content-type-options" ) ) );
		assertThat( headers.get( Key.of( "x-content-type-options" ) ) ).isEqualTo( "nosniff" );

		Assertions.assertTrue( headers.containsKey( Key.of( "access-control-allow-credentials" ) ) );
		assertThat( headers.get( Key.of( "access-control-allow-credentials" ) ) ).isEqualTo( "true" );

		Assertions.assertTrue( headers.containsKey( Key.of( "report-to" ) ) );

		Assertions.assertTrue( cfhttp.containsKey( Key.fileContent ) );
		assertThat( cfhttp.getAsString( Key.fileContent ).replaceAll( "\\s+", "" ) ).isEqualTo(
		    """
		    	[{"userId":1,"id":1,"title":"suntautfacererepellatprovidentoccaecatiexcepturioptioreprehenderit","body":"quiaetsuscipit\\nsuscipitrecusandaeconsequunturexpeditaetcum\\nreprehenderitmolestiaeututquastotam\\nnostrumrerumestautemsuntremevenietarchitecto"},{"userId":1,"id":2,"title":"quiestesse","body":"estrerumtemporevitae\\nsequisintnihilreprehenderitdolorbeataeeadoloresneque\\nfugiatblanditiisvoluptateporrovelnihilmolestiaeutreiciendis\\nquiaperiamnondebitispossimusquinequenisinulla"},{"userId":1,"id":3,"title":"eamolestiasquasiexercitationemrepellatquiipsasitaut","body":"etiustosedquoiure\\nvoluptatemoccaecatiomniseligendiautad\\nvoluptatemdoloribusvelaccusantiumquispariatur\\nmolestiaeporroeiusodioetlaboreetvelitaut"},{"userId":1,"id":4,"title":"eumetestoccaecati","body":"ullametsaepereiciendisvoluptatemadipisci\\nsitametautemassumendaprovidentrerumculpa\\nquishiccommodinesciuntremteneturdoloremqueipsamiure\\nquissuntvoluptatemrerumillovelit"},{"userId":1,"id":5,"title":"nesciuntquasodio","body":"repudiandaeveniamquaeratsuntsed\\naliasautfugiatsitautemsedest\\nvoluptatemomnispossimusessevoluptatibusquis\\nestautteneturdolorneque"},{"userId":1,"id":6,"title":"doloremeummagnieosaperiamquia","body":"utaspernaturcorporisharumnihilquisprovidentsequi\\nmollitianobisaliquidmolestiae\\nperspiciatiseteanemoabreprehenderitaccusantiumquas\\nvoluptatedoloresvelitetdoloremquemolestiae"},{"userId":1,"id":7,"title":"magnamfacilisautem","body":"doloreplaceatquibusdameaquovitae\\nmagniquisenimquiquisquonemoautsaepe\\nquidemrepellatexcepturiutquia\\nsuntutsequieoseasedquas"},{"userId":1,"id":8,"title":"doloremdoloreestipsam","body":"dignissimosaperiamdoloremquieum\\nfacilisquibusdamanimisintsuscipitquisintpossimuscum\\nquaeratmagnimaioresexcepturi\\nipsamutcommodidolorvoluptatummodiautvitae"},{"userId":1,"id":9,"title":"nesciuntiureomnisdoloremtemporaetaccusantium","body":"consecteturaniminesciuntiuredolore\\nenimquiaad\\nveniamautemutquamautnobis\\netestautquodautprovidentvoluptasautemvoluptas"},{"userId":1,"id":10,"title":"optiomolestiasidquiaeum","body":"quoetexpeditamodicumofficiavelmagni\\ndoloribusquirepudiandae\\nveronisisit\\nquosveniamquodsedaccusamusveritatiserror"}]
		    """.replaceAll(
		        "\\s+", "" ) );
	}

}
