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
package ortus.boxlang.runtime.dynamic;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.ContainerBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Range;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class MatchExpressionTest {

	private IBoxContext newContext() {
		return new ScriptingRequestBoxContext();
	}

	private IScope variablesScope( IBoxContext context ) {
		return context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "range patterns align with finite Range membership semantics" )
	@Test
	void testRangePatternsReuseFiniteRangeMembershipSemantics() {
		IBoxContext context = newContext();
		MatchExpression.Pattern pattern = MatchExpression.range( 5, 1 );
		Range range = new Range( 5, 1 );

		assertThat( pattern.matches( context, 5 ) ).isEqualTo( range.contains( 5 ) );
		assertThat( pattern.matches( context, "3" ) ).isEqualTo( range.contains( ( Object ) "3" ) );
		assertThat( pattern.matches( context, 0 ) ).isEqualTo( range.contains( 0 ) );
		assertThat( pattern.matches( context, 2.5 ) ).isEqualTo( range.contains( ( Object ) 2.5 ) );
	}

	@DisplayName( "range patterns reject oversized ranges the same way as the range operator" )
	@Test
	void testRangePatternsRejectOversizedRanges() {
		assertThrows( BoxRuntimeException.class, () -> MatchExpression.range( 1, Integer.MIN_VALUE ) );
	}

	@DisplayName( "structural object defaults distinguish missing values from explicit nulls" )
	@Test
	void testObjectPatternDefaultsDistinguishMissingFromNull() {
		IBoxContext context = newContext();
		IScope variables = variablesScope( context );
		MatchExpression.Pattern pattern = MatchExpression.object(
		    new MatchExpression.ObjectBinding[] {
		        MatchExpression.objectBinding(
		            "name",
		            MatchExpression.target( true, VariablesScope.name.getName(), "name" ),
		            null,
		            ctx -> "guest"
		        )
		    }
		);

		assertThat( pattern.matches( context, new Struct() ) ).isTrue();
		assertThat( variables.get( Key.of( "name" ) ) ).isEqualTo( "guest" );

		context = newContext();
		variables = variablesScope( context );
		Struct explicitNull = new Struct();
		explicitNull.put( Key.of( "name" ), null );

		assertThat( pattern.matches( context, explicitNull ) ).isTrue();
		assertThat( variables.get( Key.of( "name" ) ) ).isNull();
	}

	@DisplayName( "structural array defaults do not replace explicit nulls" )
	@Test
	void testArrayPatternDefaultsDoNotReplaceExplicitNulls() {
		IBoxContext context = newContext();
		IScope variables = variablesScope( context );
		MatchExpression.Pattern pattern = MatchExpression.array(
		    new MatchExpression.ArrayBinding[] {
		        MatchExpression.arrayBinding(
		            MatchExpression.target( true, VariablesScope.name.getName(), "head" ),
		            null,
		            ctx -> "fallback"
		        )
		    }
		);
		Array explicitNull = new Array();
		explicitNull.add( null );

		assertThat( pattern.matches( context, explicitNull ) ).isTrue();
		assertThat( variables.get( Key.of( "head" ) ) ).isNull();
	}

	@DisplayName( "failed structural alternatives do not leak bindings into later or branches" )
	@Test
	void testStructuralOrPatternsRemainAtomic() {
		ContainerBoxContext context = new ContainerBoxContext( newContext() );
		IScope variables = variablesScope( context );
		variables.put( Key.of( "name" ), "outer" );

		MatchExpression.Pattern pattern = MatchExpression.or(
		    new MatchExpression.Pattern[] {
		        MatchExpression.object(
		            new MatchExpression.ObjectBinding[] {
		                MatchExpression.objectBinding(
		                    "user",
		                    null,
		                    new MatchExpression.ObjectBinding[] {
		                        MatchExpression.objectBinding(
		                            "name",
		                            MatchExpression.target( true, VariablesScope.name.getName(), "name" ),
		                            null,
		                            null
		                        )
		                    },
		                    null
		                ),
		                MatchExpression.objectBinding(
		                    "role",
		                    MatchExpression.target( true, VariablesScope.name.getName(), "role" ),
		                    null,
		                    null
		                )
		            }
		        ),
		        MatchExpression.wildcard()
		    }
		);

		Struct user = new Struct();
		user.put( Key.of( "name" ), "Ada" );
		Struct subject = new Struct();
		subject.put( Key.of( "user" ), user );

		assertThat( pattern.matches( context, subject ) ).isTrue();
		assertThat( variables.get( Key.of( "name" ) ) ).isEqualTo( "outer" );
	}
}