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

package ortus.boxlang.runtime.components.system;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.EnumSource;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

public class ParamTest {

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

	@DisplayName( "It can param tag" )
	@Test
	public void testCanParamTag() {

		instance.executeSource(
		    """
		    <cfparam name="result" default="my default">
		       """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.getAsString( result ) ).isEqualTo( "my default" );
	}

	@DisplayName( "It can param BL tag" )
	@Test
	public void testCanParamBLTag() {

		instance.executeSource(
		    """
		    <bx:param name="result" default="my default">
		       """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsString( result ) ).isEqualTo( "my default" );
	}

	@DisplayName( "It can param script" )
	@Test
	public void testCanParamScript() {

		instance.executeSource(
		    """
		    param name="result" default="my default";
		       """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "my default" );
	}

	@DisplayName( "It can param ACF script" )
	@Test
	public void testCanParamACFScript() {

		instance.executeSource(
		    """
		    cfparam(  name="result", default="my default");
		       """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.getAsString( result ) ).isEqualTo( "my default" );
	}

	@DisplayName( "It can param script shortcut no type" )
	@Test
	public void testCanParamScriptShortcutNoType() {

		instance.executeSource(
		    """
		    param result="my default";
		       """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "my default" );
	}

	@DisplayName( "It can param script shortcut scope no type" )
	@Test
	public void testCanParamScriptShortcutScopeNoType() {

		instance.executeSource(
		    """
		    param variables.result="my default";
		       """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "my default" );
	}

	@DisplayName( "It can param script shortcut scope type" )
	@Test
	public void testCanParamScriptShortcutScopeType() {

		instance.executeSource(
		    """
		    param String variables.result="my default";
		       """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "my default" );
	}

	@DisplayName( "It can param script shortcut scope type Only" )
	@Test
	public void testCanParamScriptShortcutScopeTypeOnly() {

		instance.executeSource(
		    """
		    variables.result="value";
		      param String variables.result;
		         """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "value" );
	}

	@DisplayName( "It can param script shortcut with type" )
	@Test
	public void testCanParamScriptShortcutWithType() {

		instance.executeSource(
		    """
		    param String result="my default";
		       """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "my default" );
	}

	@DisplayName( "It can param script shortcut with type only" )
	@Test
	public void testCanParamScriptShortcutWithTypeOnly() {

		instance.executeSource(
		    """
		    result ="foo"
		       param String result;
		          """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "foo" );
	}

	private static Stream<Arguments> paramStatements() {
		return Stream.of( BoxSourceType.BOXSCRIPT, BoxSourceType.CFSCRIPT ).flatMap( sourceType -> Stream.of(
		    "param result = getDefault();",
		    "param String result = getDefault();",
		    "param variables.result = getDefault();",
		    "param String variables.result = getDefault();"
		).map( statement -> Arguments.of( statement, sourceType ) ) );
	}

	@DisplayName( "It does not evaluate a param default when the variable exists" )
	@ParameterizedTest
	@MethodSource( "paramStatements" )
	public void testParamDoesNotEvaluateDefaultWhenVariableExists( String statement, BoxSourceType sourceType ) {
		instance.executeSource(
		    """
		    variables.defaultCalls = 0;
		    variables.result = "existing value";
		    function getDefault() {
		        variables.defaultCalls++;
		        return "default value";
		    }
		    """ + statement,
		    context, sourceType );
		assertThat( variables.getAsString( result ) ).isEqualTo( "existing value" );
		assertThat( variables.getAsInteger( Key.of( "defaultCalls" ) ) ).isEqualTo( 0 );
	}

	@DisplayName( "It evaluates a param default exactly once when the variable is missing" )
	@ParameterizedTest
	@MethodSource( "paramStatements" )
	public void testParamEvaluatesDefaultOnceWhenVariableIsMissing( String statement, BoxSourceType sourceType ) {
		instance.executeSource(
		    """
		    variables.defaultCalls = 0;
		    function getDefault() {
		        variables.defaultCalls++;
		        return "default value";
		    }
		    """ + statement,
		    context, sourceType );
		assertThat( variables.getAsString( result ) ).isEqualTo( "default value" );
		assertThat( variables.getAsInteger( Key.of( "defaultCalls" ) ) ).isEqualTo( 1 );
	}

	private static Stream<Arguments> paramForms() {
		return Stream.concat( paramStatements(), Stream.of(
		    Arguments.of( "bx:param name=\"result\" default=getDefault();", BoxSourceType.BOXSCRIPT ),
		    Arguments.of( "param name=\"result\" default=getDefault();", BoxSourceType.CFSCRIPT ),
		    Arguments.of( "cfparam(name=\"result\", default=getDefault());", BoxSourceType.CFSCRIPT ),
		    Arguments.of( "<bx:param name=\"result\" default=\"#getDefault()#\">", BoxSourceType.BOXTEMPLATE ),
		    Arguments.of( "<cfparam name=\"result\" default=\"#getDefault()#\">", BoxSourceType.CFTEMPLATE )
		) );
	}

	@ParameterizedTest
	@MethodSource( "paramForms" )
	public void testAllParamFormsDeferDefaults( String statement, BoxSourceType sourceType ) {
		instance.executeSource( """
		                        variables.defaultCalls = 0;
		                        function getDefault() {
		                            variables.defaultCalls++;
		                            return { value: "default" };
		                        }
		                        """, this.context );
		this.variables.put( result, "existing" );
		instance.executeSource( statement, this.context, sourceType );
		assertThat( this.variables.get( result ) ).isEqualTo( "existing" );
		assertThat( this.variables.getAsInteger( Key.of( "defaultCalls" ) ) ).isEqualTo( 0 );
		this.variables.remove( result );
		instance.executeSource( statement, this.context, sourceType );
		assertThat( this.variables.getAsStruct( result ) ).isEqualTo( Struct.of( "value", "default" ) );
		assertThat( this.variables.getAsInteger( Key.of( "defaultCalls" ) ) ).isEqualTo( 1 );
	}

	@ParameterizedTest
	@MethodSource( "paramForms" )
	public void testParamPreservesFunctionDefaults( String statement, BoxSourceType sourceType ) {
		instance.executeSource( "function callback() { throw 'Must not invoke the default value'; }", this.context );
		instance.executeSource( statement.replace( "getDefault()", "callback" ), this.context, sourceType );
		assertThat( this.variables.get( result ) ).isSameInstanceAs( this.variables.get( Key.of( "callback" ) ) );
	}

	@ParameterizedTest
	@MethodSource( "paramForms" )
	public void testParamChecksExistenceOnce( String statement, BoxSourceType sourceType ) {
		int[]	reads	= { 0 };
		Struct	holder	= new Struct() {

							@Override
							public Object dereference( IBoxContext context, Key key, Boolean safe ) {
								reads[ 0 ]++;
								return super.dereference( context, key, safe );
							}
						};
		holder.put( result, "existing" );
		this.variables.put( Key.of( "holder" ), holder );
		instance.executeSource( "function getDefault() { return 'default'; }", this.context );
		String nestedStatement = statement.replace( "variables.result", "holder.result" );
		if ( !nestedStatement.contains( "holder.result" ) ) {
			nestedStatement = nestedStatement.replace( "result", "holder.result" );
		}
		instance.executeSource( nestedStatement, this.context, sourceType );
		assertThat( reads[ 0 ] ).isEqualTo( 1 );
		holder.remove( result );
		reads[ 0 ] = 0;
		instance.executeSource( nestedStatement, this.context, sourceType );
		assertThat( reads[ 0 ] ).isEqualTo( 1 );
		assertThat( holder.get( result ) ).isEqualTo( "default" );
	}

	@ParameterizedTest
	@MethodSource( "paramForms" )
	public void testParamDefaultCanReturnNull( String statement, BoxSourceType sourceType ) {
		instance.executeSource( "function getDefault() { return javacast('null', ''); }", this.context );
		instance.executeSource( statement, this.context, sourceType );
		assertThat( this.variables.get( result ) ).isNull();
	}

	@ParameterizedTest
	@MethodSource( "paramForms" )
	public void testParamDefersExpressionsInsideLiterals( String statement, BoxSourceType sourceType ) {
		instance.executeSource( """
		                        variables.defaultCalls = 0;
		                        function getDefault() { variables.defaultCalls++; return "default"; }
		                        """, this.context );
		this.variables.put( result, "existing" );
		String arrayDefault = statement.replace( "getDefault()", "[getDefault()]" );
		instance.executeSource( arrayDefault, this.context, sourceType );
		assertThat( this.variables.getAsInteger( Key.of( "defaultCalls" ) ) ).isEqualTo( 0 );
		this.variables.remove( result );
		instance.executeSource( arrayDefault, this.context, sourceType );
		assertThat( this.variables.getAsInteger( Key.of( "defaultCalls" ) ) ).isEqualTo( 1 );
		assertThat( this.variables.getAsArray( result ).get( 0 ) ).isEqualTo( "default" );
	}

	@Test
	public void testParamLiteralDefaultsAreFreshForEachCall() {
		instance.executeSource( """
		                        function withDefault() {
		                            param local.items = [];
		                            return local.items;
		                        }
		                        variables.first = withDefault();
		                        variables.first.append( "changed" );
		                        variables.result = withDefault();
		                        """, this.context );
		assertThat( this.variables.getAsArray( result ).size() ).isEqualTo( 0 );
	}

	@Test
	public void testParamDefaultCanConstructJavaObjectsInFunctionContext() {
		instance.executeSource( """
		                        import java.lang.StringBuilder;
		                        function withDefault( supplied ) {
		                            var fallback = "local default";
		                            param arguments.supplied = new StringBuilder( fallback );
		                            return arguments.supplied;
		                        }
		                        variables.result = withDefault().toString();
		                        """, this.context );
		assertThat( this.variables.getAsString( result ) ).isEqualTo( "local default" );
	}

	@Test
	public void testParamDefaultPreservesExplicitFunctionScopes() {
		instance.executeSource( """
		                        function withDefault( fallback ) {
		                            var localFallback = "local default";
		                            param local.first = arguments.fallback;
		                            param local.second = local.localFallback;
		                            param local.third = (local.localFallback = "changed");
		                            return [local.first, local.second, local.localFallback];
		                        }
		                        variables.result = withDefault("argument default");
		                        """, this.context );
		assertThat( this.variables.getAsArray( result ).get( 0 ) ).isEqualTo( "argument default" );
		assertThat( this.variables.getAsArray( result ).get( 1 ) ).isEqualTo( "local default" );
		assertThat( this.variables.getAsArray( result ).get( 2 ) ).isEqualTo( "changed" );
	}

	@DisplayName( "It preserves falsey param values without evaluating the default" )
	@ParameterizedTest
	@EnumSource( value = BoxSourceType.class, names = { "BOXSCRIPT", "CFSCRIPT" } )
	public void testParamPreservesFalseyValues( BoxSourceType sourceType ) {
		instance.executeSource(
		    """
		    variables.values = { flag: false, count: 0, text: "" };
		    param variables.values.flag = failDefault();
		    param variables.values.count = failDefault();
		    param variables.values.text = failDefault();
		    function failDefault() {
		        throw "Default must not be evaluated";
		    }
		    """, context, sourceType );
		assertThat( variables.getAsStruct( Key.of( "values" ) ) ).isEqualTo( Struct.of( "flag", false, "count", 0, "text", "" ) );
	}

	@DisplayName( "It evaluates defaults for null and missing nested values" )
	@ParameterizedTest
	@EnumSource( value = BoxSourceType.class, names = { "BOXSCRIPT", "CFSCRIPT" } )
	public void testParamNullAndMissingNestedValues( BoxSourceType sourceType ) {
		instance.executeSource(
		    """
		    variables.defaultCalls = 0;
		    variables.values = { present: javacast( "null", "" ) };
		    function getDefault() {
		        variables.defaultCalls++;
		        return "default value";
		    }
		    param variables.values.present = getDefault();
		    param variables.values.missing = getDefault();
		    """, context, sourceType );
		assertThat( variables.getAsStruct( Key.of( "values" ) ) ).isEqualTo( Struct.of( "present", "default value", "missing", "default value" ) );
		assertThat( variables.getAsInteger( Key.of( "defaultCalls" ) ) ).isEqualTo( 2 );
	}

	@DisplayName( "It resolves param defaults in the current function context" )
	@ParameterizedTest
	@EnumSource( value = BoxSourceType.class, names = { "BOXSCRIPT", "CFSCRIPT" } )
	public void testParamFunctionContext( BoxSourceType sourceType ) {
		instance.executeSource(
		    """
		    function withDefault( supplied ) {
		        var fallback = "local default";
		        param arguments.supplied = fallback;
		        return arguments.supplied;
		    }
		    variables.existing = withDefault( "existing value" );
		    variables.result = withDefault();
		    """, context, sourceType );
		assertThat( variables.getAsString( Key.of( "existing" ) ) ).isEqualTo( "existing value" );
		assertThat( variables.getAsString( result ) ).isEqualTo( "local default" );
	}

	@DisplayName( "It still requires a variable when param has no default" )
	@Test
	public void testParamWithoutDefaultRequiresVariable() {
		assertThrows( KeyNotFoundException.class, () -> instance.executeSource( "param String variables.missing;", context ) );
	}

	@DisplayName( "It can param a struct to a default value" )
	@Test
	public void testParamStructOnMissing() {
		instance.executeSource(
		    """
		    	param variables.result = {};
		    """,
		    context );
		assertThat( variables.getAsStruct( result ) ).isEqualTo( Struct.of() );
	}

	@DisplayName( "It skips paraming a struct with values" )
	@Test
	public void testParamStructWithValues() {
		instance.executeSource(
		    """
		    variables.result = { "foo": "bar" };
		      	param variables.result = {};
		      """,
		    context );
		assertThat( variables.getAsStruct( result ) ).isEqualTo( Struct.of( "foo", "bar" ) );
	}

}
