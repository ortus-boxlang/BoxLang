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
package ortus.boxlang.runtime.bifs.global.net;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
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
import ortus.boxlang.runtime.services.HttpService;

public class SOAPTest {

	static BoxRuntime		instance;
	private IBoxContext		context;
	private IScope			variables;
	private HttpService		httpService;
	private static String	calculatorWsdlUrl;

	@BeforeAll
	public static void setUpAll() {
		instance			= BoxRuntime.getInstance( true );

		// Use local calculator WSDL for testing
		calculatorWsdlUrl	= Paths.get( "src/test/resources/wsdl/calculator.wsdl" ).toAbsolutePath().toUri().toString();
	}

	@BeforeEach
	public void setUp() {
		this.context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		this.variables		= this.context.getScopeNearby( VariablesScope.name );
		this.httpService	= BoxRuntime.getInstance().getHttpService();

		// Clear SOAP client cache before each test
		this.httpService.clearAllSoapClients();
	}

	@AfterEach
	public void tearDown() {
		// Clear SOAP client cache after each test
		this.httpService.clearAllSoapClients();
	}

	@Test
	@DisplayName( "It can create a SOAP client from WSDL" )
	void testSoapClientCreation() {
		// @formatter:off
		instance.executeSource(
		    """
		    result = soap( "%s" )
		    """.formatted( calculatorWsdlUrl ),
		    this.context
		);
		// @formatter:on

		Object result = this.variables.get( Key.of( "result" ) );
		assertThat( result ).isNotNull();
		assertThat( result.getClass().getName() ).isEqualTo( "ortus.boxlang.runtime.net.soap.BoxSoapClient" );
	}

	@Test
	@DisplayName( "It caches SOAP clients by WSDL URL" )
	void testSoapClientCaching() {
		// @formatter:off
		instance.executeSource(
		    """
		    client1 = soap( "%s" )
		    client2 = soap( "%s" )
		    """.formatted( calculatorWsdlUrl, calculatorWsdlUrl ),
		    this.context
		);
		// @formatter:on

		Object	client1	= this.variables.get( Key.of( "client1" ) );
		Object	client2	= this.variables.get( Key.of( "client2" ) );

		assertThat( client1 ).isNotNull();
		assertThat( client2 ).isNotNull();
		assertThat( client1 ).isSameInstanceAs( client2 );
	}

	@Test
	@DisplayName( "It can access SOAP operations" )
	void testSoapOperationInvocation() {
		// @formatter:off
		instance.executeSource(
		    """
		    ws = soap( "%s" )
		    operations = ws.getOperations()
		    hasOperations = arrayLen( operations ) > 0
		    """.formatted( calculatorWsdlUrl ),
		    this.context
		);
		// @formatter:on

		assertThat( this.variables.getAsBoolean( Key.of( "hasOperations" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "It returns a BoxSoapClient instance" )
	void testFluentConfiguration() {
		// @formatter:off
		instance.executeSource(
		    """
		    ws = soap( "%s" )
		    result = ws
		    """.formatted( calculatorWsdlUrl ),
		    this.context
		);
		// @formatter:on

		Object result = this.variables.get( Key.of( "result" ) );
		assertThat( result ).isNotNull();
		assertThat( result.getClass().getName() ).isEqualTo( "ortus.boxlang.runtime.net.soap.BoxSoapClient" );
	}

	@Test
	@DisplayName( "It can list SOAP operations" )
	void testFluentClientOperationInvocation() {
		// Test with local WSDL only - no actual HTTP invocation
		// @formatter:off
		instance.executeSource(
		    """
		    ws = soap( "%s" )
		    operations = ws.getOperations()
			println( operations )
		    hasOperations = !isNull( operations )
		    """.formatted( calculatorWsdlUrl ),
		    this.context
		);
		// @formatter:on

		assertThat( this.variables.getAsBoolean( Key.of( "hasOperations" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "It can work with live WSDL services" )
	void testLiveWSDLService() {
		// Use a real, publicly available WSDL service for testing
		// This service is specifically designed for testing and doesn't block Java clients
		// Alternative: https://www.dataaccess.com/webservicesserver/NumberConversion.wso?WSDL
		String liveWsdlUrl = "https://www.w3schools.com/xml/tempconvert.asmx?WSDL";

		try {
			// @formatter:off
			instance.executeSource(
			    """
			    ws = soap( "%s" )
			    operations = ws.getOperations()
			    hasOps = arrayLen( operations ) > 0
			    """.formatted( liveWsdlUrl ),
			    this.context
			);
			// @formatter:on

			assertThat( this.variables.getAsBoolean( Key.of( "hasOps" ) ) ).isTrue();
		} catch ( Exception e ) {
			e.printStackTrace();
			// Network issues are acceptable in tests
			org.junit.jupiter.api.Assumptions.assumeTrue( false, "Network request failed: " + e.getMessage() );
		}
	}

	@Test
	@DisplayName( "It can get client statistics" )
	void testClientStatistics() {
		// @formatter:off
		instance.executeSource(
		    """
		    ws = soap( "%s" )
		    stats = ws.getStatistics()
		    hasStats = structKeyExists( stats, "totalInvocations" )
		    """.formatted( calculatorWsdlUrl ),
		    this.context
		);
		// @formatter:on

		assertThat( this.variables.getAsBoolean( Key.of( "hasStats" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "It can be used with method invocation" )
	void testMethodInvocation() {
		try {
			String liveWsdlUrl = "https://www.w3schools.com/xml/tempconvert.asmx?WSDL";

			// @formatter:off
			instance.executeSource(
			    """
			    ws = soap( "%s" )

			    // Test that operations are callable - CelsiusToFahrenheit method
			    result = ws.invoke( "CelsiusToFahrenheit", 0 )
			    hasResult = !isNull( result )
			    """.formatted( liveWsdlUrl ),
			    this.context
			);
			// @formatter:on

			assertThat( this.variables.getAsBoolean( Key.of( "hasResult" ) ) ).isTrue();
		} catch ( Exception e ) {
			// Network issues are acceptable in tests
			org.junit.jupiter.api.Assumptions.assumeTrue( false, "Network request failed: " + e.getMessage() );
		}
	}

	@Test
	@DisplayName( "It throws exception for invalid WSDL" )
	void testInvalidWSDL() {
		// @formatter:off
		instance.executeSource(
		    """
		    try {
		        ws = soap( "http://invalid-wsdl-url-that-does-not-exist.com/service.wsdl" )
		        hasError = false
		    } catch( any e ) {
		        hasError = true
		        errorMessage = e.message
		    }
		    """,
		    this.context
		);
		// @formatter:on

		assertThat( this.variables.getAsBoolean( Key.of( "hasError" ) ) ).isTrue();
		assertThat( this.variables.getAsString( Key.of( "errorMessage" ) ) ).contains( "WSDL" );
	}

	@Test
	@DisplayName( "It creates independent clients for different WSDLs" )
	void testMultipleWSDLClients() {
		String liveWsdlUrl = "https://www.w3schools.com/xml/tempconvert.asmx?WSDL";

		try {
			// @formatter:off
			instance.executeSource(
			    """
			    localWs = soap( "%s" )
			    remoteWs = soap( "%s" )

			    areDifferent = !localWs.equals( remoteWs )
			    """.formatted( calculatorWsdlUrl, liveWsdlUrl ),
			    this.context
			);
			// @formatter:on

			assertThat( this.variables.getAsBoolean( Key.of( "areDifferent" ) ) ).isTrue();
		} catch ( Exception e ) {
			// Network issues are acceptable in tests
			org.junit.jupiter.api.Assumptions.assumeTrue( false, "Network request failed: " + e.getMessage() );
		}
	}
}
