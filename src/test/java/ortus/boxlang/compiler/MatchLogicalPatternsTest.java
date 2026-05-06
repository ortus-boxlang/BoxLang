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
package ortus.boxlang.compiler;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class MatchLogicalPatternsTest {

	private static BoxRuntime	instance;

	private IBoxContext			context;
	private IScope				variables;

	@BeforeAll
	public static void setupRuntime() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		this.context	= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		this.variables	= this.context.getScopeNearby( VariablesScope.name );
	}

	@Test
	public void testMatchOrPatternMatchesEitherLiteralAlternative() {
		this.variables.put( ortus.boxlang.runtime.scopes.Key.of( "value" ), 2 );

		Object result = instance.executeStatement(
		    "match value { 1 or 2 -> \"small\" _ -> \"many\" }",
		    this.context
		);

		assertThat( result ).isEqualTo( "small" );
	}

	@Test
	public void testMatchAndPatternRequiresBothSubpatterns() {
		this.variables.put( ortus.boxlang.runtime.scopes.Key.of( "value" ), 2 );

		Object result = instance.executeStatement(
		    "match value { x and 2 -> x _ -> \"fallback\" }",
		    this.context
		);

		assertThat( result ).isEqualTo( 2 );
	}

	@Test
	public void testMatchNotPatternMatchesWhenNestedPatternFails() {
		this.variables.put( ortus.boxlang.runtime.scopes.Key.of( "value" ), 2 );

		Object result = instance.executeStatement(
		    "match value { not 1 -> \"other\" _ -> \"one\" }",
		    this.context
		);

		assertThat( result ).isEqualTo( "other" );
	}

	@Test
	public void testMatchNotPatternRejectsBindingPatterns() {
		this.variables.put( ortus.boxlang.runtime.scopes.Key.of( "value" ), 2 );

		BoxRuntimeException exception = assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeStatement( "match value { not x -> x _ -> \"fallback\" }", this.context )
		);

		assertThat( exception ).hasMessageThat().contains( "not patterns cannot contain binding patterns" );
	}

	@Test
	public void testMatchPredicatePatternUsesLambdaToMatchSubject() {
		this.variables.put( ortus.boxlang.runtime.scopes.Key.of( "value" ), 2 );

		Object result = instance.executeStatement(
		    "match value { ?( x -> x > 1 ) -> \"many\" _ -> \"small\" }",
		    this.context
		);

		assertThat( result ).isEqualTo( "many" );
	}

	@Test
	public void testMatchPredicatePatternFallsThroughWhenPredicateReturnsFalse() {
		this.variables.put( ortus.boxlang.runtime.scopes.Key.of( "value" ), 2 );

		Object result = instance.executeStatement(
		    "match value { ?( x -> x > 5 ) -> \"many\" _ -> \"small\" }",
		    this.context
		);

		assertThat( result ).isEqualTo( "small" );
	}

	@Test
	public void testMatchOrPatternShortCircuitsBeforeEvaluatingLaterBinding() {
		this.variables.put( ortus.boxlang.runtime.scopes.Key.of( "value" ), 2 );
		this.variables.put( ortus.boxlang.runtime.scopes.Key.of( "marker" ), "initial" );

		Object result = instance.executeStatement(
		    """
		    match value {
		    	2 or marker -> "two"
		    	_ -> "fallback"
		    }
		    """,
		    this.context
		);

		assertThat( result ).isEqualTo( "two" );
		assertThat( this.variables.get( ortus.boxlang.runtime.scopes.Key.of( "marker" ) ) ).isEqualTo( "initial" );
	}

	@Test
	public void testMatchAndPatternShortCircuitsBeforeEvaluatingLaterBinding() {
		this.variables.put( ortus.boxlang.runtime.scopes.Key.of( "value" ), 2 );
		this.variables.put( ortus.boxlang.runtime.scopes.Key.of( "marker" ), "initial" );

		Object result = instance.executeStatement(
		    """
		    match value {
		    	1 and marker -> "one"
		    	_ -> "fallback"
		    }
		    """,
		    this.context
		);

		assertThat( result ).isEqualTo( "fallback" );
		assertThat( this.variables.get( ortus.boxlang.runtime.scopes.Key.of( "marker" ) ) ).isEqualTo( "initial" );
	}

	@Test
	public void testMatchLogicalPatternsRespectAndBeforeOrPrecedence() {
		this.variables.put( ortus.boxlang.runtime.scopes.Key.of( "value" ), 2 );

		Object result = instance.executeStatement(
		    """
		    match value {
		    	2 or 2 and 3 -> "two"
		    	_ -> "fallback"
		    }
		    """,
		    this.context
		);

		assertThat( result ).isEqualTo( "two" );
	}

	@Test
	public void testMatchPredicatePatternRejectsNonFunctionExpressionsWhenEvaluated() {
		this.variables.put( ortus.boxlang.runtime.scopes.Key.of( "value" ), 2 );

		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeStatement( "match value { ?( 1 ) -> \"many\" _ -> \"small\" }", this.context )
		);
	}
}