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

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

public class WsdlOperationTest {

	@DisplayName( "Test operation creation with name" )
	@Test
	void testOperationCreationWithName() {
		WsdlOperation operation = new WsdlOperation( "testOperation" );

		assertThat( operation.getName() ).isEqualTo( "testOperation" );
		assertThat( operation.getInputParameters() ).isEmpty();
		assertThat( operation.getOutputParameters() ).isEmpty();
	}

	@DisplayName( "Test operation fluent setters" )
	@Test
	void testOperationFluentSetters() {
		WsdlOperation operation = new WsdlOperation( "testOp" )
		    .setSoapAction( "http://example.com/testOp" )
		    .setNamespace( "http://example.com/ns" )
		    .setDocumentation( "Test operation" );

		assertThat( operation.getSoapAction() ).isEqualTo( "http://example.com/testOp" );
		assertThat( operation.getNamespace() ).isEqualTo( "http://example.com/ns" );
		assertThat( operation.getDocumentation() ).isEqualTo( "Test operation" );
	}

	@DisplayName( "Test operation fluent interface returns same instance" )
	@Test
	void testFluentInterfaceReturnsSameInstance() {
		WsdlOperation	operation	= new WsdlOperation( "test" );
		WsdlOperation	result		= operation.setSoapAction( "action" );

		assertThat( result ).isSameInstanceAs( operation );
	}

	@DisplayName( "Test adding input parameters" )
	@Test
	void testAddingInputParameters() {
		WsdlOperation	operation	= new WsdlOperation( "testOp" );
		WsdlParameter	param1		= new WsdlParameter( "param1", "string" );
		WsdlParameter	param2		= new WsdlParameter( "param2", "int" );

		operation.addInputParameter( param1 );
		operation.addInputParameter( param2 );

		List<WsdlParameter> params = operation.getInputParameters();
		assertThat( params ).hasSize( 2 );
		assertThat( params.get( 0 ).getName() ).isEqualTo( "param1" );
		assertThat( params.get( 1 ).getName() ).isEqualTo( "param2" );
	}

	@DisplayName( "Test adding output parameters" )
	@Test
	void testAddingOutputParameters() {
		WsdlOperation	operation	= new WsdlOperation( "testOp" );
		WsdlParameter	result		= new WsdlParameter( "result", "string" );

		operation.addOutputParameter( result );

		List<WsdlParameter> params = operation.getOutputParameters();
		assertThat( params ).hasSize( 1 );
		assertThat( params.get( 0 ).getName() ).isEqualTo( "result" );
	}

	@DisplayName( "Test operation toStruct conversion" )
	@Test
	void testOperationToStruct() {
		WsdlOperation operation = new WsdlOperation( "getUser" );
		operation.setSoapAction( "http://example.com/getUser" )
		    .setNamespace( "http://example.com/ns" );

		operation.addInputParameter( new WsdlParameter( "userId", "int" ) );
		operation.addOutputParameter( new WsdlParameter( "user", "User" ) );

		IStruct struct = operation.toStruct();

		assertThat( struct.getAsString( Key.of( "name" ) ) ).isEqualTo( "getUser" );
		assertThat( struct.getAsString( Key.of( "soapAction" ) ) ).isEqualTo( "http://example.com/getUser" );
		assertThat( struct.getAsString( Key.of( "namespace" ) ) ).isEqualTo( "http://example.com/ns" );
		assertThat( struct.getAsInteger( Key.of( "inputParameterCount" ) ) ).isEqualTo( 1 );
		assertThat( struct.getAsInteger( Key.of( "outputParameterCount" ) ) ).isEqualTo( 1 );
	}

	@DisplayName( "Test operation with multiple input parameters" )
	@Test
	void testOperationWithMultipleInputParameters() {
		WsdlOperation operation = new WsdlOperation( "calculate" );

		operation.addInputParameter( new WsdlParameter( "a", "double" ) );
		operation.addInputParameter( new WsdlParameter( "b", "double" ) );
		operation.addInputParameter( new WsdlParameter( "operator", "string" ) );

		assertThat( operation.getInputParameters() ).hasSize( 3 );
	}

	@DisplayName( "Test operation toString representation" )
	@Test
	void testOperationToString() {
		WsdlOperation	operation	= new WsdlOperation( "testOperation" );
		String			strValue	= operation.toString();

		assertThat( strValue ).isNotNull();
		assertThat( strValue ).contains( "testOperation" );
	}

	@DisplayName( "Test operation with documentation" )
	@Test
	void testOperationWithDocumentation() {
		WsdlOperation operation = new WsdlOperation( "testOp" )
		    .setDocumentation( "This operation performs a test" );

		assertThat( operation.getDocumentation() ).isEqualTo( "This operation performs a test" );

		IStruct struct = operation.toStruct();
		assertThat( struct.containsKey( Key.of( "documentation" ) ) ).isTrue();
	}

	@DisplayName( "Test operation without SOAP action" )
	@Test
	void testOperationWithoutSoapAction() {
		WsdlOperation operation = new WsdlOperation( "testOp" );

		// SOAP action should be null by default
		assertThat( operation.getSoapAction() ).isNull();
	}

}
