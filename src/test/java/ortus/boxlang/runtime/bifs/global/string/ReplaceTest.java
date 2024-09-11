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

package ortus.boxlang.runtime.bifs.global.string;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class ReplaceTest {

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

	@Test
	public void testReplaceOnce() {
		instance.executeSource(
		    """
		    result = replace( "redgreenbluered", "red", 'brad', "one" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "bradgreenbluered" );
	}

	@Test
	public void testReplaceAll() {
		instance.executeSource(
		    """
		    result = replace( "redgreenbluered", "red", 'brad', "all" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "bradgreenbluebrad" );
	}

	@Test
	public void testReplaceOnceMember() {
		instance.executeSource(
		    """
		    result = "redgreenbluered".replace( "red", 'brad', "one" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "bradgreenbluered" );
	}

	@Test
	public void testReplaceAllMember() {
		instance.executeSource(
		    """
		    result = "redgreenbluered".replace( "red", 'brad', "all" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "bradgreenbluebrad" );
	}

	@Test
	public void testReplaceOnceClosure() {
		instance.executeSource(
		    """
		    result = "redgreenbluered".ReplaceNoCase(
				"red",
				( found, position, original ) => {
					retString = uCase( found ) & position;
					return retString
				},
				"one" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "RED1greenbluered" );
	}

	@Test
	public void testReplaceAllClosure() {
		instance.executeSource(
		    """
		    result = "redgreenbluered".ReplaceNoCase(
				"red",
				( found, position, original ) => {
					retString = uCase( found ) & position;
					return retString
				},
				"all" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "RED1greenblueRED13" );
	}

}
