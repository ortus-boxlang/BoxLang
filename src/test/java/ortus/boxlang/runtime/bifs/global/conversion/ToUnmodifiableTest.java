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

package ortus.boxlang.runtime.bifs.global.conversion;

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
import ortus.boxlang.runtime.types.unmodifiable.UnmodifiableArray;
import ortus.boxlang.runtime.types.unmodifiable.UnmodifiableStruct;

public class ToUnmodifiableTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );
	static Key			result2	= new Key( "result2" );

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

	@DisplayName( "It can turn arrays to Unmodifiable" )
	@Test
	public void testToArrayUnmodifiable() {
		// @formatter:off
		instance.executeSource(
		    """
		      	result = toUnmodifiable( [ 1, 2, 3 ] )
		    	result2 = [1,2,3].toUnmodifiable();
		    """,
		    context );
		// @formatter:on
		assertThat( variables.get( result ) ).isInstanceOf( UnmodifiableArray.class );
		assertThat( variables.get( result2 ) ).isInstanceOf( UnmodifiableArray.class );
	}

	@DisplayName( "It can turn structs to Unmodifiable" )
	@Test
	public void testToStructUnmodifiable() {
		// @formatter:off
		instance.executeSource(
		    """
		      	result = toUnmodifiable( { a: 1, b: 2, c: 3 } )
		    	result2 = { a: 1, b: 2, c: 3 }.toUnmodifiable();
		    """,
		    context );
		// @formatter:on
		assertThat( variables.get( result ) ).isInstanceOf( UnmodifiableStruct.class );
		assertThat( variables.get( result2 ) ).isInstanceOf( UnmodifiableStruct.class );
	}

}
