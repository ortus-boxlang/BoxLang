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

public class WsdlParameterTest {

	@DisplayName( "Test parameter creation with name" )
	@Test
	void testParameterCreationWithName() {
		WsdlParameter param = new WsdlParameter( "testParam" );

		assertThat( param.getName() ).isEqualTo( "testParam" );
		assertThat( param.getType() ).isNull();
		assertThat( param.isRequired() ).isFalse();
		assertThat( param.isArray() ).isFalse();
	}

	@DisplayName( "Test parameter creation with name and type" )
	@Test
	void testParameterCreationWithNameAndType() {
		WsdlParameter param = new WsdlParameter( "testParam", "string" );

		assertThat( param.getName() ).isEqualTo( "testParam" );
		assertThat( param.getType() ).isEqualTo( "string" );
	}

	@DisplayName( "Test parameter fluent setters" )
	@Test
	void testParameterFluentSetters() {
		WsdlParameter param = new WsdlParameter( "testParam" )
		    .setType( "string" )
		    .setNamespace( "http://example.com" )
		    .setRequired( true )
		    .setArray( false )
		    .setDocumentation( "Test parameter" );

		assertThat( param.getName() ).isEqualTo( "testParam" );
		assertThat( param.getType() ).isEqualTo( "string" );
		assertThat( param.getNamespace() ).isEqualTo( "http://example.com" );
		assertThat( param.isRequired() ).isTrue();
		assertThat( param.isArray() ).isFalse();
		assertThat( param.getDocumentation() ).isEqualTo( "Test parameter" );
	}

	@DisplayName( "Test parameter fluent interface returns same instance" )
	@Test
	void testFluentInterfaceReturnsSameInstance() {
		WsdlParameter	param	= new WsdlParameter( "test" );
		WsdlParameter	result	= param.setType( "string" );

		assertThat( result ).isSameInstanceAs( param );
	}

	@DisplayName( "Test parameter toStruct conversion" )
	@Test
	void testParameterToStruct() {
		WsdlParameter	param	= new WsdlParameter( "username", "string" )
		    .setNamespace( "http://schemas.xmlsoap.org/soap/encoding/" )
		    .setRequired( true );

		IStruct			struct	= param.toStruct();

		assertThat( struct.getAsString( Key.of( "name" ) ) ).isEqualTo( "username" );
		assertThat( struct.getAsString( Key.of( "type" ) ) ).isEqualTo( "string" );
		assertThat( struct.getAsString( Key.of( "namespace" ) ) ).isEqualTo( "http://schemas.xmlsoap.org/soap/encoding/" );
		assertThat( struct.getAsBoolean( Key.of( "required" ) ) ).isTrue();
		assertThat( struct.getAsBoolean( Key.of( "isArray" ) ) ).isFalse();
	}

	@DisplayName( "Test parameter with array type" )
	@Test
	void testParameterWithArrayType() {
		WsdlParameter param = new WsdlParameter( "items", "ArrayOfString" )
		    .setArray( true );

		assertThat( param.isArray() ).isTrue();

		IStruct struct = param.toStruct();
		assertThat( struct.getAsBoolean( Key.of( "isArray" ) ) ).isTrue();
	}

	@DisplayName( "Test parameter with optional field" )
	@Test
	void testParameterWithOptionalField() {
		WsdlParameter param = new WsdlParameter( "optionalParam", "int" )
		    .setRequired( false );

		assertThat( param.isRequired() ).isFalse();

		IStruct struct = param.toStruct();
		assertThat( struct.getAsBoolean( Key.of( "required" ) ) ).isFalse();
	}

	@DisplayName( "Test parameter with complex type" )
	@Test
	void testParameterWithComplexType() {
		WsdlParameter param = new WsdlParameter( "address" )
		    .setType( "tns:AddressType" )
		    .setNamespace( "http://example.com/types" )
		    .setRequired( true );

		assertThat( param.getType() ).isEqualTo( "tns:AddressType" );
		assertThat( param.getNamespace() ).isEqualTo( "http://example.com/types" );
	}

	@DisplayName( "Test parameter toString representation" )
	@Test
	void testParameterToString() {
		WsdlParameter	param		= new WsdlParameter( "testParam", "string" );

		String			strValue	= param.toString();

		assertThat( strValue ).isNotNull();
		assertThat( strValue ).contains( "testParam" );
	}

	@DisplayName( "Test parameter with documentation" )
	@Test
	void testParameterWithDocumentation() {
		WsdlParameter param = new WsdlParameter( "param1" )
		    .setDocumentation( "This is a test parameter" );

		assertThat( param.getDocumentation() ).isEqualTo( "This is a test parameter" );

		IStruct struct = param.toStruct();
		assertThat( struct.containsKey( Key.of( "documentation" ) ) ).isTrue();
	}

}
