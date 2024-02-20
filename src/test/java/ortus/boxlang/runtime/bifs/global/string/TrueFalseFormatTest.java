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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class TrueFalseFormatTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;

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

	@DisplayName( "It can do true false formats" )
	@Test
	void testItWorks() {
		assertThat( instance.executeStatement( "trueFalseFormat( 'true' )", context ) ).isEqualTo( "true" );
		assertThat( instance.executeStatement( "trueFalseFormat( true )", context ) ).isEqualTo( "true" );
		assertThat( instance.executeStatement( "trueFalseFormat( 1 )", context ) ).isEqualTo( "true" );
		assertThat( instance.executeStatement( "trueFalseFormat( 999 )", context ) ).isEqualTo( "true" );
		assertThat( instance.executeStatement( "trueFalseFormat( 'yes' )", context ) ).isEqualTo( "true" );

		assertThat( instance.executeStatement( "'true'.trueFalseFormat()", context ) ).isEqualTo( "true" );
		assertThat( instance.executeStatement( "true.trueFalseFormat()", context ) ).isEqualTo( "true" );
		assertThat( instance.executeStatement( "(1).trueFalseFormat()", context ) ).isEqualTo( "true" );
		assertThat( instance.executeStatement( "(999).trueFalseFormat()", context ) ).isEqualTo( "true" );
		assertThat( instance.executeStatement( "'yes'.trueFalseFormat()", context ) ).isEqualTo( "true" );

		assertThat( instance.executeStatement( "trueFalseFormat( 'false' )", context ) ).isEqualTo( "false" );
		assertThat( instance.executeStatement( "trueFalseFormat( false )", context ) ).isEqualTo( "false" );
		assertThat( instance.executeStatement( "trueFalseFormat( 0 )", context ) ).isEqualTo( "false" );
		assertThat( instance.executeStatement( "trueFalseFormat( 'no' )", context ) ).isEqualTo( "false" );

		assertThat( instance.executeStatement( "'false'.trueFalseFormat()", context ) ).isEqualTo( "false" );
		assertThat( instance.executeStatement( "false.trueFalseFormat()", context ) ).isEqualTo( "false" );
		assertThat( instance.executeStatement( "(0).trueFalseFormat()", context ) ).isEqualTo( "false" );
		assertThat( instance.executeStatement( "'no'.trueFalseFormat()", context ) ).isEqualTo( "false" );
		assertThat( instance.executeStatement( "trueFalseFormat( '' )", context ) ).isEqualTo( "false" );
		assertThat( instance.executeStatement( "trueFalseFormat( null )", context ) ).isEqualTo( "false" );

	}

}
