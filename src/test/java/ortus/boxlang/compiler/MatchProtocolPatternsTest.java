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
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class MatchProtocolPatternsTest {

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
	public void testMatchProtocolPatternBindsValueWhenPredicateMatches() {
		this.variables.put(
		    Key.of( "value" ),
		    instance.executeStatement( "new src.test.java.TestCases.phase3.MatchEmail( address = \"ada@example.com\" )", this.context )
		);

		Object result = instance.executeStatement(
		    "match( value ) { Email( addr ) => addr; _ => \"fallback\" }",
		    this.context
		);

		assertThat( result ).isEqualTo( "ada@example.com" );
	}

	@Test
	public void testMatchProtocolPatternFallsThroughWhenPredicateReturnsFalse() {
		this.variables.put(
		    Key.of( "value" ),
		    instance.executeStatement( "new src.test.java.TestCases.phase3.MatchProtocolNeverBind( address = \"ada@example.com\" )", this.context )
		);

		Object result = instance.executeStatement(
		    "match( value ) { Phone( number ) => number; _ => \"fallback\" }",
		    this.context
		);

		assertThat( result ).isEqualTo( "fallback" );
	}

	@Test
	public void testMatchProtocolPatternEvaluatesGuardAfterBindingsAreProduced() {
		this.variables.put(
		    Key.of( "value" ),
		    instance.executeStatement( "new src.test.java.TestCases.phase3.MatchEmail( address = \"ada@example.com\" )", this.context )
		);

		Object result = instance.executeStatement(
		    "match( value ) { Email( addr ) if addr == \"ada@example.com\" => addr; _ => \"fallback\" }",
		    this.context
		);

		assertThat( result ).isEqualTo( "ada@example.com" );
	}

	@Test
	public void testMatchProtocolPatternDefersToTagMetadataWhenBothArePresent() {
		this.variables.put(
		    Key.of( "value" ),
		    instance.executeStatement(
		        "new src.test.java.TestCases.phase3.MatchTaggedProtocolEmail( canonical = \"tag@example.com\", address = \"protocol@example.com\" )",
		        this.context )
		);

		Object result = instance.executeStatement(
		    "match( value ) { Email( addr ) => addr; _ => \"fallback\" }",
		    this.context
		);

		assertThat( result ).isEqualTo( "tag@example.com" );
	}

	@Test
	public void testMatchProtocolPatternRejectsMissingBindingsMethodAfterSuccessfulPredicate() {
		this.variables.put(
		    Key.of( "value" ),
		    instance.executeStatement( "new src.test.java.TestCases.phase3.MatchProtocolMissingBindings()", this.context )
		);

		BoxRuntimeException exception = assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeStatement( "match( value ) { Email( addr ) => addr; _ => \"fallback\" }", this.context )
		);

		assertThat( exception ).hasMessageThat().contains( "$matchBindings must be implemented when $matchPredicate returns true." );
	}

	@Test
	public void testMatchProtocolPatternRejectsNonStructBindings() {
		this.variables.put(
		    Key.of( "value" ),
		    instance.executeStatement( "new src.test.java.TestCases.phase3.MatchProtocolBadBindings()", this.context )
		);

		BoxRuntimeException exception = assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeStatement( "match( value ) { Email( addr ) => addr; _ => \"fallback\" }", this.context )
		);

		assertThat( exception ).hasMessageThat().contains( "$matchBindings must return a struct of bound values." );
	}
}