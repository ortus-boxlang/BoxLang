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
import ortus.boxlang.runtime.scopes.VariablesScope;

public class YesNoFormatTest {

	static BoxRuntime		instance;
	static IBoxContext		context;
	static VariablesScope	variables;

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= ( VariablesScope ) context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		variables.clear();
	}

	@DisplayName( "It works" )
	@Test
	public void testItWorks() {
		assertThat( instance.executeStatement( "yesNoFormat( 'true' )", context ) ).isEqualTo( "Yes" );
		assertThat( instance.executeStatement( "yesNoFormat( true )", context ) ).isEqualTo( "Yes" );
		assertThat( instance.executeStatement( "yesNoFormat( 1 )", context ) ).isEqualTo( "Yes" );
		assertThat( instance.executeStatement( "yesNoFormat( 999 )", context ) ).isEqualTo( "Yes" );
		assertThat( instance.executeStatement( "yesNoFormat( 'yes' )", context ) ).isEqualTo( "Yes" );

		assertThat( instance.executeStatement( "'true'.yesNoFormat()", context ) ).isEqualTo( "Yes" );
		assertThat( instance.executeStatement( "true.yesNoFormat()", context ) ).isEqualTo( "Yes" );
		assertThat( instance.executeStatement( "(1).yesNoFormat()", context ) ).isEqualTo( "Yes" );
		assertThat( instance.executeStatement( "(999).yesNoFormat()", context ) ).isEqualTo( "Yes" );
		assertThat( instance.executeStatement( "'yes'.yesNoFormat()", context ) ).isEqualTo( "Yes" );

		assertThat( instance.executeStatement( "yesNoFormat( 'false' )", context ) ).isEqualTo( "No" );
		assertThat( instance.executeStatement( "yesNoFormat( false )", context ) ).isEqualTo( "No" );
		assertThat( instance.executeStatement( "yesNoFormat( 0 )", context ) ).isEqualTo( "No" );
		assertThat( instance.executeStatement( "yesNoFormat( 'no' )", context ) ).isEqualTo( "No" );

		assertThat( instance.executeStatement( "'false'.yesNoFormat()", context ) ).isEqualTo( "No" );
		assertThat( instance.executeStatement( "false.yesNoFormat()", context ) ).isEqualTo( "No" );
		assertThat( instance.executeStatement( "(0).yesNoFormat()", context ) ).isEqualTo( "No" );
		assertThat( instance.executeStatement( "'no'.yesNoFormat()", context ) ).isEqualTo( "No" );
	}

}
