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
package TestCases.phase1;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxLangException;

public class RefutableDestructuringDeclarationTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "refutable object destructuring declaration commits bindings on success" )
	@Test
	public void testRefutableObjectDestructuringDeclarationCommitsBindingsOnSuccess() {
		instance.executeSource(
		    """
		    function unpack( struct payload ) {
		    	var { name, role = "guest" } = payload else {
		    		return { failed: true };
		    	}

		    	return { failed: false, name: name, role: role };
		    }

		    result = unpack( { name: "Mina" } );
		    """,
		    context );

		IStruct result = ( IStruct ) variables.get( Key.of( "result" ) );
		assertThat( result.get( Key.of( "failed" ) ) ).isEqualTo( false );
		assertThat( result.get( Key.of( "name" ) ) ).isEqualTo( "Mina" );
		assertThat( result.get( Key.of( "role" ) ) ).isEqualTo( "guest" );
	}

	@DisplayName( "refutable object destructuring declaration discards tentative bindings on failure" )
	@Test
	public void testRefutableObjectDestructuringDeclarationDiscardsTentativeBindingsOnFailure() {
		instance.executeSource(
		    """
		    function unpack( struct payload ) {
		    	var { name, role = "guest" } = payload else {
		    		return {
		    			failed: true,
		    			nameDefined: isDefined( "name" ),
		    			roleDefined: isDefined( "role" )
		    		};
		    	}

		    	return { failed: false, name: name, role: role };
		    }

		    result = unpack( {} );
		    """,
		    context );

		IStruct result = ( IStruct ) variables.get( Key.of( "result" ) );
		assertThat( result.get( Key.of( "failed" ) ) ).isEqualTo( true );
		assertThat( result.get( Key.of( "nameDefined" ) ) ).isEqualTo( false );
		assertThat( result.get( Key.of( "roleDefined" ) ) ).isEqualTo( false );
	}

	@DisplayName( "refutable destructuring declarations require at least one binding target" )
	@Test
	public void testRefutableDestructuringDeclarationRequiresBindingTarget() {
		BoxLangException t = assertThrows( BoxLangException.class, () -> instance.executeSource(
		    """
		    var _ = 42 else {
		    	failed = true;
		    }
		    """,
		    context ) );

		assertThat( t.getMessage() ).contains( "at least one binding target" );
	}

	@DisplayName( "refutable literal-only declarations are rejected" )
	@Test
	public void testRefutableLiteralOnlyDeclarationIsRejected() {
		BoxLangException t = assertThrows( BoxLangException.class, () -> instance.executeSource(
		    """
		    var 42 = 42 else {
		    	failed = true;
		    }
		    """,
		    context ) );

		assertThat( t.getMessage() ).contains( "at least one binding target" );
	}

	@DisplayName( "nested empty array declarations are rejected" )
	@Test
	public void testNestedEmptyArrayDeclarationIsRejected() {
		BoxLangException t = assertThrows( BoxLangException.class, () -> instance.executeSource(
		    """
		    var [ [] ] = [ [] ] else {
		    	failed = true;
		    }
		    """,
		    context ) );

		assertThat( t.getMessage() ).contains( "at least one binding target" );
	}

	@DisplayName( "refutable declarations support script-level else statements" )
	@Test
	public void testRefutableDeclarationSupportsScriptLevelElseStatements() {
		instance.executeSource(
		    """
		    data = {};
		    var { name } = data else {
		    	failed = true;
		    }
		    """,
		    context );

		assertThat( variables.get( Key.of( "failed" ) ) ).isEqualTo( true );
	}

	@DisplayName( "final refutable declarations keep final binding behavior" )
	@Test
	public void testFinalRefutableDeclarationKeepsFinalBindingBehavior() {
		instance.executeSource(
		    """
		    data = { name: "Mina" };
		    final { name } = data else {
		    	failed = true;
		    }
		    """,
		    context );

		assertThat( variables.get( Key.of( "name" ) ) ).isEqualTo( "Mina" );

		BoxLangException t = assertThrows( BoxLangException.class, () -> instance.executeSource(
		    """
		    name = "Noa";
		    """,
		    context ) );

		assertThat( t.getMessage() ).contains( "Cannot reassign final key" );
	}

	@DisplayName( "scoped targets are disallowed for refutable declaration destructuring" )
	@Test
	public void testScopedTargetsDisallowedForRefutableDeclarationDestructuring() {
		BoxLangException t = assertThrows( BoxLangException.class, () -> instance.executeSource(
		    """
		    data = { a: 1 };
		    var { a: variables.a } = data else {
		    	failed = true;
		    }
		    """,
		    context ) );

		assertThat( t.getMessage() ).contains( "Scoped targets are not allowed" );
	}

	@DisplayName( "scoped targets are disallowed for final refutable declaration destructuring" )
	@Test
	public void testScopedTargetsDisallowedForFinalRefutableDeclarationDestructuring() {
		BoxLangException t = assertThrows( BoxLangException.class, () -> instance.executeSource(
		    """
		    data = { a: 1 };
		    final { a: variables.a } = data else {
		    	failed = true;
		    }
		    """,
		    context ) );

		assertThat( t.getMessage() ).contains( "Scoped targets are not allowed" );
	}
}