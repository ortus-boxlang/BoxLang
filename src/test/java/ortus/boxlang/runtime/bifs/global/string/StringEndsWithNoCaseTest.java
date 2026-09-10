/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.string;

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

public class StringEndsWithNoCaseTest {

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

	@DisplayName( "StringEndsWithNoCase BIF - case insensitive match" )
	@Test
	public void testEndsWithNoCaseMatch() {
		instance.executeSource(
		    """
		    result = StringEndsWithNoCase( "BoxLang is great", "GREAT" );
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( result ) ).isTrue();
	}

	@DisplayName( "StringEndsWithNoCase BIF - no match" )
	@Test
	public void testEndsWithNoCaseNoMatch() {
		instance.executeSource(
		    """
		    result = StringEndsWithNoCase( "BoxLang is great", "terrible" );
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( result ) ).isFalse();
	}

	@DisplayName( "endsWithNoCase member method" )
	@Test
	public void testEndsWithNoCaseMember() {
		instance.executeSource(
		    """
		    result = "BoxLang is great".endsWithNoCase( "GREAT" );
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( result ) ).isTrue();
	}

	@DisplayName( "endsWithNoCase member method on non-string types" )
	@Test
	public void testEndsWithNoCaseOnNonStringTypes() {
		instance.executeSource(
		    """
		    result = true.endsWithNoCase( "UE" );
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( result ) ).isTrue();
	}
}
