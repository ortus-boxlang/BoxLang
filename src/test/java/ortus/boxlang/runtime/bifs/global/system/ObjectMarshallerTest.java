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

import org.junit.jupiter.api.AfterAll;
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
import ortus.boxlang.runtime.types.Query;

public class ObjectMarshallerTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
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

	@DisplayName( "It can serialize / deserialize an object" )
	@Test
	public void testSerializationDeserialization() {
		// @formatter:off
		instance.executeSource(
		    """
			// arrays
		    a = [1,2,3,4,5];
			serialized = objectSerialize( a );
			result = objectDeserialize( serialized );
			assert result == a;

			// structs
			a = { "a": 1, "b": 2, "c": 3 };
			serialized = objectSerialize( a );
			result = objectDeserialize( serialized );
			assert result == a;

			// strings
			a = "hello world";
			serialized = objectSerialize( a );
			result = objectDeserialize( serialized );
			assert result == a;

			// Numbers
			a = 12345;
			serialized = objectSerialize( a );
			result = objectDeserialize( serialized );
			assert result == a;

			// Dates
			a = now();
			serialized = objectSerialize( a );
			result = objectDeserialize( serialized );
			assert result == a;

			// Query
			a = queryNew("directory,name,type");
			queryAddRow( a, { "directory": "/tmp", "name": "test.txt", "type": "file" } );
			serialized = objectSerialize( a );
			result = objectDeserialize( serialized );
		    """,
		context );
		// @formatter:on
		Query test = ( Query ) variables.get( result );
		assertThat( test.size() ).isEqualTo( 1 );
	}

	@DisplayName( "Can serialize/deserialize bx classes" )
	@Test
	public void testSerializationDeserializationBxClasses() {
		// @formatter:off
		instance.executeSource(
		    """
			// Class
			person = new src.test.bx.Person()
			person.setName( "Luis" )
			person.setSurname( "Majano" )
			person.setAge( 1 )
			println( person.toJson() )

			serialized = objectSerialize( person )
			result = objectDeserialize( serialized )

			println( result.toJson() )
			assert result.getName() == "Luis"
			assert result.getSurname() == "Majano"
			// Should be null, since it is not serializable
			assert result.getAge() == null
		    """,
		context );
		// @formatter:on
	}

}
