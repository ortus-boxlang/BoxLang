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
package ortus.boxlang.runtime.net.soap;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

public class WsdlDefinitionTest {

	@DisplayName( "Test definition creation with URL" )
	@Test
	void testDefinitionCreationWithUrl() {
		WsdlDefinition definition = new WsdlDefinition( "http://example.com/service.wsdl" );

		assertThat( definition.getWsdlUrl() ).isEqualTo( "http://example.com/service.wsdl" );
		assertThat( definition.getOperations() ).isEmpty();
	}

	@DisplayName( "Test definition fluent setters" )
	@Test
	void testDefinitionFluentSetters() {
		WsdlDefinition definition = new WsdlDefinition( "http://example.com/service.wsdl" )
		    .setServiceEndpoint( "http://example.com/endpoint" )
		    .setTargetNamespace( "http://example.com/ns" )
		    .setServiceName( "TestService" )
		    .setBindingStyle( "document" );

		assertThat( definition.getServiceEndpoint() ).isEqualTo( "http://example.com/endpoint" );
		assertThat( definition.getTargetNamespace() ).isEqualTo( "http://example.com/ns" );
		assertThat( definition.getServiceName() ).isEqualTo( "TestService" );
		assertThat( definition.getBindingStyle() ).isEqualTo( "document" );
	}

	@DisplayName( "Test adding operations" )
	@Test
	void testAddingOperations() {
		WsdlDefinition	definition	= new WsdlDefinition( "http://example.com/service.wsdl" );
		WsdlOperation	op1			= new WsdlOperation( "operation1" );
		WsdlOperation	op2			= new WsdlOperation( "operation2" );

		definition.addOperation( op1 );
		definition.addOperation( op2 );

		assertThat( definition.getOperations() ).hasSize( 2 );
		assertThat( definition.hasOperation( Key.of( "operation1" ) ) ).isTrue();
		assertThat( definition.hasOperation( Key.of( "operation2" ) ) ).isTrue();
	}

	@DisplayName( "Test getting operation by name" )
	@Test
	void testGettingOperationByName() {
		WsdlDefinition	definition	= new WsdlDefinition( "http://example.com/service.wsdl" );
		WsdlOperation	operation	= new WsdlOperation( "testOp" );

		definition.addOperation( operation );

		WsdlOperation retrieved = definition.getOperation( Key.of( "testOp" ) );
		assertThat( retrieved ).isNotNull();
		assertThat( retrieved.getName() ).isEqualTo( "testOp" );
	}

	@DisplayName( "Test getting non-existent operation returns null" )
	@Test
	void testGettingNonExistentOperation() {
		WsdlDefinition	definition	= new WsdlDefinition( "http://example.com/service.wsdl" );
		WsdlOperation	operation	= definition.getOperation( Key.of( "nonExistent" ) );

		assertThat( operation ).isNull();
	}

	@DisplayName( "Test definition toStruct conversion" )
	@Test
	void testDefinitionToStruct() {
		WsdlDefinition definition = new WsdlDefinition( "http://example.com/service.wsdl" )
		    .setServiceEndpoint( "http://example.com/endpoint" )
		    .setServiceName( "TestService" );

		definition.addOperation( new WsdlOperation( "op1" ) );

		IStruct struct = definition.toStruct();

		assertThat( struct.getAsString( Key.of( "wsdlUrl" ) ) ).isEqualTo( "http://example.com/service.wsdl" );
		assertThat( struct.getAsString( Key.of( "serviceEndpoint" ) ) ).isEqualTo( "http://example.com/endpoint" );
		assertThat( struct.getAsString( Key.of( "serviceName" ) ) ).isEqualTo( "TestService" );
		assertThat( struct.containsKey( Key.of( "operations" ) ) ).isTrue();
	}

	@DisplayName( "Test operation name case insensitivity" )
	@Test
	void testOperationNameCaseInsensitivity() {
		WsdlDefinition	definition	= new WsdlDefinition( "http://example.com/service.wsdl" );
		WsdlOperation	operation	= new WsdlOperation( "TestOperation" );

		definition.addOperation( operation );

		assertThat( definition.hasOperation( Key.of( "testoperation" ) ) ).isTrue();
		assertThat( definition.hasOperation( Key.of( "TESTOPERATION" ) ) ).isTrue();
		assertThat( definition.getOperation( Key.of( "testoperation" ) ) ).isNotNull();
	}

}
