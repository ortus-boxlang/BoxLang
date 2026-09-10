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
package ortus.boxlang.runtime.bifs.global.stringbuilder;

import static com.google.common.truth.Truth.assertThat;

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
import ortus.boxlang.runtime.types.BoxStringBuilder;

public class StringBuilderNewTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "stringBuilderNew() creates an empty BoxStringBuilder" )
	@Test
	public void testStringBuilderNewEmpty() {
		instance.executeSource( """
		                        result = stringBuilderNew();
		                        """, context );
		assertThat( variables.get( result ) ).isInstanceOf( BoxStringBuilder.class );
		assertThat( ( ( BoxStringBuilder ) variables.get( result ) ).toString() ).isEqualTo( "" );
	}

	@DisplayName( "stringBuilderNew( 'hello' ) creates a seeded BoxStringBuilder" )
	@Test
	public void testStringBuilderNewWithValue() {
		instance.executeSource( """
		                        result = stringBuilderNew( 'hello' );
		                        """, context );
		assertThat( variables.get( result ) ).isInstanceOf( BoxStringBuilder.class );
		assertThat( ( ( BoxStringBuilder ) variables.get( result ) ).toString() ).isEqualTo( "hello" );
	}

	@DisplayName( "stringBuilderNew( capacity = N ) creates an empty BoxStringBuilder with capacity" )
	@Test
	public void testStringBuilderNewWithCapacityOnly() {
		instance.executeSource( """
		                        result = stringBuilderNew( capacity = 64 );
		                        """, context );
		assertThat( variables.get( result ) ).isInstanceOf( BoxStringBuilder.class );
		BoxStringBuilder sb = ( BoxStringBuilder ) variables.get( result );
		assertThat( sb.toString() ).isEqualTo( "" );
		assertThat( sb.getBuffer().capacity() ).isAtLeast( 64 );
	}

	@DisplayName( "stringBuilderNew( value, capacity ) creates a seeded BoxStringBuilder with capacity" )
	@Test
	public void testStringBuilderNewWithValueAndCapacity() {
		instance.executeSource( """
		                        result = stringBuilderNew( 'hello', 64 );
		                        """, context );
		assertThat( variables.get( result ) ).isInstanceOf( BoxStringBuilder.class );
		BoxStringBuilder sb = ( BoxStringBuilder ) variables.get( result );
		assertThat( sb.toString() ).isEqualTo( "hello" );
		assertThat( sb.getBuffer().capacity() ).isAtLeast( 64 );
	}

}
