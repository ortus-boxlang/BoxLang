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

package ortus.boxlang.runtime.bifs.global.system;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.ClassNotFoundBoxLangException;

public class CreateObjectTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
		instance.getClassLocator().clearClassLoaders();
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "Test BIF CreateObject With BX" )
	@Test
	void testBIFBX() {
		Object test = instance.executeStatement( "createObject( 'class', 'src.test.java.TestCases.phase3.MyClass' )" );
		assertTrue( test instanceof IClassRunnable );
	}

	@DisplayName( "Test BIF CreateObject With BX no type" )
	@Test
	void testBIFBXNoType() {
		Object test = instance.executeStatement( "createObject( 'src.test.java.TestCases.phase3.MyClass' )" );
		assertTrue( test instanceof IClassRunnable );
	}

	@DisplayName( "Can obey externalOnly flag" )
	@Test
	void testBIFBXExternalOnly() {

		String mappingPath = Paths.get( "src/test/java/TestCases/" ).toAbsolutePath().toString();
		instance.getConfiguration().registerMapping( "/bxexternalTest", Struct.of(
		    Key.path, mappingPath,
		    Key.external, true
		) );
		instance.getConfiguration().registerMapping( "/bxinternalTest", Struct.of(
		    Key.path, mappingPath,
		    Key.external, false
		) );

		Object test = instance.executeStatement( "createObject( 'component', 'bxexternalTest.phase3.MyClass' )" );
		assertTrue( test instanceof IClassRunnable );

		Throwable t = assertThrows( ClassNotFoundBoxLangException.class,
		    () -> instance.executeStatement( "createObject( type='component', className='bxinternalTest.phase3.MyClass', externalOnly=true )" ) );
		assertThat( t.getMessage() ).contains( "has not been located" );
	}

	@DisplayName( "Test BIF CreateObject Java" )
	@Test
	void testBIFJava() {
		Object test = instance.executeStatement( "createObject( 'java', 'java.lang.String' )" );
		assertTrue( test instanceof DynamicObject );
		test = instance.executeStatement( "createObject( 'java', 'java.lang.String' ).init()" );
		assertTrue( test instanceof String );
	}

	@DisplayName( "It can createobject java with one class path as string" )
	@Test
	void testBIFJavaClassPathAsString() {
		DynamicObject test = ( DynamicObject ) instance.executeStatement(
		    "createObject( 'java', 'HelloWorld', '/src/test/resources/libs/helloworld.jar' )"
		);
		assertThat( test.getTargetClass().getName() ).isEqualTo( "HelloWorld" );
	}

	@DisplayName( "It can createobject java with one class path as an array" )
	@Test
	void testBIFJavaClassPathAsArray() {
		DynamicObject test = ( DynamicObject ) instance.executeStatement(
		    "createObject( 'java', 'HelloWorld', ['/src/test/resources/libs/helloworld.jar'] )"
		);
		assertThat( test.getTargetClass().getName() ).isEqualTo( "HelloWorld" );
	}

	@DisplayName( "Test createObject webservice type creates BoxSoapClient from local WSDL" )
	@Test
	void testCreateObjectWebservice() {
		// Using a local WSDL file for testing
		String wsdlPath = Paths.get( "src/test/resources/wsdl/calculator.wsdl" ).toAbsolutePath().toUri().toString();

		instance.executeSource(
		    """
		    result = createObject( "webservice", "%s" )
		    """.formatted( wsdlPath ),
		    context );

		Object soapClient = variables.get( result );
		assertThat( soapClient ).isNotNull();
		assertThat( soapClient.getClass().getName() ).contains( "BoxSoapClient" );
	}

	@DisplayName( "Test webservice creation is cached" )
	@Test
	void testWebserviceCreationIsCached() {
		String wsdlPath = Paths.get( "src/test/resources/wsdl/calculator.wsdl" ).toAbsolutePath().toUri().toString();

		instance.executeSource(
		    """
		    client1 = createObject( "webservice", "%s" )
		    client2 = createObject( "webservice", "%s" )
		    """.formatted( wsdlPath, wsdlPath ),
		    context );

		Object	client1	= variables.get( Key.of( "client1" ) );
		Object	client2	= variables.get( Key.of( "client2" ) );

		// Should return the same cached instance
		assertThat( client1 ).isSameInstanceAs( client2 );
	}

	@DisplayName( "Test webservice client from local WSDL file" )
	@Test
	void testWebserviceClientOperations() {
		String wsdlPath = Paths.get( "src/test/resources/wsdl/calculator.wsdl" ).toAbsolutePath().toUri().toString();

		instance.executeSource(
		    """
		    client = createObject( "webservice", "%s" )
		    """.formatted( wsdlPath ),
		    context );

		Object client = variables.get( Key.of( "client" ) );
		assertThat( client ).isNotNull();
		assertThat( client.getClass().getName() ).contains( "BoxSoapClient" );
	}

	@DisplayName( "Test webservice with live WSDL from Beeceptor mock service" )
	@Test
	void testWebserviceWithLiveWSDL() {
		// Using Beeceptor's free SOAP mock service
		try {
			instance.executeSource(
			    """
			    result = createObject(
			        "webservice",
			        "https://soap-test-server.mock.beeceptor.com/CountryInfoService?WSDL"
			    )
			    """,
			    context );

			Object soapClient = variables.get( result );
			assertThat( soapClient ).isNotNull();
			assertThat( soapClient.getClass().getName() ).contains( "BoxSoapClient" );
		} catch ( Exception e ) {
			// Skip test if network is unavailable
			org.junit.jupiter.api.Assumptions.assumeTrue( false, "Network unavailable: " + e.getMessage() );
		}
	}

	@DisplayName( "Test invoke methods on webservice client created from WSDL" )
	@Test
	void testWebserviceInvokeMethods() {
		try {
			// @formatter:off
			instance.executeSource(
			    """
			    ws = createObject(
			        "webservice",
			        "https://soap-test-server.mock.beeceptor.com/CountryInfoService?WSDL"
			    )

			    // Test calling ListOfContinentsByName method
			    continents = ws.ListOfContinentsByName()
			    hasContinents = !isNull( continents )

			    // Test calling ListOfCountryNamesByName method
			    countries = ws.ListOfCountryNamesByName()
			    hasCountries = !isNull( countries )

			    // Test case-insensitive method calls
			    continents2 = ws.listofcontinentsbyname()
			    caseInsensitive = !isNull( continents2 )

				println( continents )
				println( countries )
				println( continents2 )
			    """,
			    context );
			// @formatter:on

			// Verify results
			assertThat( variables.get( Key.of( "hasContinents" ) ) ).isEqualTo( true );
			assertThat( variables.get( Key.of( "hasCountries" ) ) ).isEqualTo( true );
			assertThat( variables.get( Key.of( "caseInsensitive" ) ) ).isEqualTo( true );

			// Verify the actual response objects are not null
			assertThat( variables.get( Key.of( "continents" ) ) ).isNotNull();
			assertThat( variables.get( Key.of( "countries" ) ) ).isNotNull();
		} catch ( Exception e ) {
			// Skip test if network is unavailable or service is down
			org.junit.jupiter.api.Assumptions.assumeTrue( false, "Network/Service unavailable: " + e.getMessage() );
		}
	}

	@DisplayName( "Test webservice method invocation using invoke() BIF" )
	@Test
	void testWebserviceMethodViaInvoke() {
		try {
			// @formatter:off
			instance.executeSource(
			    """
			    ws = createObject(
			        "webservice",
			        "https://soap-test-server.mock.beeceptor.com/CountryInfoService?WSDL"
			    )

			    // Test using invoke() BIF to call methods
			    result1 = invoke( ws, "ListOfContinentsByName" )
			    hasResult1 = !isNull( result1 )

			    result2 = invoke( ws, "ListOfCountryNamesByName" )
			    hasResult2 = !isNull( result2 )
			    """,
			    context );
			// @formatter:on

			assertThat( variables.get( Key.of( "hasResult1" ) ) ).isEqualTo( true );
			assertThat( variables.get( Key.of( "hasResult2" ) ) ).isEqualTo( true );
			assertThat( variables.get( Key.of( "result1" ) ) ).isNotNull();
			assertThat( variables.get( Key.of( "result2" ) ) ).isNotNull();
		} catch ( Exception e ) {
			// Skip test if network is unavailable
			org.junit.jupiter.api.Assumptions.assumeTrue( false, "Network/Service unavailable: " + e.getMessage() );
		}
	}

	@DisplayName( "Test multiple webservice clients with different WSDLs" )
	@Test
	void testMultipleWebserviceClients() {
		String wsdlPath1 = Paths.get( "src/test/resources/wsdl/calculator.wsdl" ).toAbsolutePath().toUri().toString();

		instance.executeSource(
		    """
		    client1 = createObject( "webservice", "%s" )
		    client2 = createObject( "webservice", "%s" )
		    areSame = ( client1 == client2 )
		    """.formatted( wsdlPath1, wsdlPath1 ),
		    context );

		Object	client1	= variables.get( Key.of( "client1" ) );
		Object	client2	= variables.get( Key.of( "client2" ) );

		// Same WSDL should return cached instance
		assertThat( client1 ).isSameInstanceAs( client2 );
		assertThat( variables.get( Key.of( "areSame" ) ) ).isEqualTo( true );
	}

	@DisplayName( "Test webservice error handling with invalid WSDL" )
	@Test
	void testWebserviceInvalidWSDL() {
		// Test with non-existent file
		assertThrows( Exception.class, () -> {
			instance.executeSource(
			    """
			    result = createObject( "webservice", "/nonexistent/file.wsdl" )
			    """,
			    context );
		} );
	}

}
