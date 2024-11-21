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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.semver4j.Semver;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class GetSemverTest {

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

	@DisplayName( "It can parse a semantic version string" )
	@Test
	public void testGetSemver() {
		instance.executeSource(
		    """
		    result = getSemver( "1.2.3" );
		    """,
		    context );

		Semver semver = ( Semver ) variables.get( result );
		assertThat( semver.getMajor() ).isEqualTo( 1 );
		assertThat( semver.getMinor() ).isEqualTo( 2 );
		assertThat( semver.getPatch() ).isEqualTo( 3 );
	}

	@DisplayName( "It can build a semver using the semver builder" )
	@Test
	public void testBuildSemver() {
		instance.executeSource(
		    """
		    result = getSemver().withMajor( 1 ).withMinor( 2 ).withPatch( 3 ).toSemver();
		    """,
		    context );

		Semver semver = ( Semver ) variables.get( result );
		assertThat( semver.getMajor() ).isEqualTo( 1 );
		assertThat( semver.getMinor() ).isEqualTo( 2 );
		assertThat( semver.getPatch() ).isEqualTo( 3 );
	}

}
