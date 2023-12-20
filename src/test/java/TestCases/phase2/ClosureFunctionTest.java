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
package TestCases.phase2;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Closure;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.FunctionMeta;

public class ClosureFunctionTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );
	static Key			foo		= new Key( "foo" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "basic Closure" )
	@Test
	public void testBasicClosure() {

		instance.executeSource(
		    """
		       foo = function() {
		    	return "my func";
		    }
		    result = foo();
		    """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( "my func" );
		assertThat( variables.dereference( foo, false ) instanceof Closure ).isEqualTo( true );

		Struct meta = ( ( Closure ) variables.dereference( foo, false ) ).getMetaData();

		assertThat( meta.dereference( Key.of( "name" ), false ) ).isEqualTo( "Closure" );
		// Defaults
		assertThat( meta.dereference( Key.of( "hint" ), false ) ).isEqualTo( "" );
		assertThat( meta.dereference( Key.of( "output" ), false ) ).isEqualTo( false );
		assertThat( meta.dereference( Key.of( "returnType" ), false ) ).isEqualTo( "any" );
		assertThat( meta.dereference( Key.of( "access" ), false ) ).isEqualTo( "public" );

	}

	@DisplayName( "basic arrow Closure" )
	@Test
	public void testBasicArrowClosure() {

		instance.executeSource(
		    """
		       foo = () => {
		    	return "my func";
		    }
		    result = foo();
		    """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( "my func" );
		assertThat( variables.dereference( foo, false ) instanceof Closure ).isEqualTo( true );

		Struct meta = ( ( Closure ) variables.dereference( foo, false ) ).getMetaData();

		assertThat( meta.dereference( Key.of( "name" ), false ) ).isEqualTo( "Closure" );
		// Defaults
		assertThat( meta.dereference( Key.of( "hint" ), false ) ).isEqualTo( "" );
		assertThat( meta.dereference( Key.of( "output" ), false ) ).isEqualTo( false );
		assertThat( meta.dereference( Key.of( "returnType" ), false ) ).isEqualTo( "any" );
		assertThat( meta.dereference( Key.of( "access" ), false ) ).isEqualTo( "public" );

	}

	@DisplayName( "basic arrow Closure no body" )
	@Test
	public void testBasicArrowClosureNoBody() {

		instance.executeSource(
		    """
		        foo = () => "my func";
		    	result = foo();
		    """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( "my func" );
		assertThat( variables.dereference( foo, false ) instanceof Closure ).isEqualTo( true );

		Struct meta = ( ( Closure ) variables.dereference( foo, false ) ).getMetaData();

		assertThat( meta.dereference( Key.of( "name" ), false ) ).isEqualTo( "Closure" );
		// Defaults
		assertThat( meta.dereference( Key.of( "hint" ), false ) ).isEqualTo( "" );
		assertThat( meta.dereference( Key.of( "output" ), false ) ).isEqualTo( false );
		assertThat( meta.dereference( Key.of( "returnType" ), false ) ).isEqualTo( "any" );
		assertThat( meta.dereference( Key.of( "access" ), false ) ).isEqualTo( "public" );

	}

	@DisplayName( "basic arrow Closure no body no parens" )
	@Test
	public void testBasicArrowClosureNoBodyNoParens() {

		instance.executeSource(
		    """
		        foo = param1 => param1;
		    	result = foo("my func");
		    """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( "my func" );
		assertThat( variables.dereference( foo, false ) instanceof Closure ).isEqualTo( true );

		Struct meta = ( ( Closure ) variables.dereference( foo, false ) ).getMetaData();

		assertThat( meta.dereference( Key.of( "name" ), false ) ).isEqualTo( "Closure" );
		// Defaults
		assertThat( meta.dereference( Key.of( "hint" ), false ) ).isEqualTo( "" );
		assertThat( meta.dereference( Key.of( "output" ), false ) ).isEqualTo( false );
		assertThat( meta.dereference( Key.of( "returnType" ), false ) ).isEqualTo( "any" );
		assertThat( meta.dereference( Key.of( "access" ), false ) ).isEqualTo( "public" );

	}

	@DisplayName( "argument defaults" )
	@Test
	public void testArgumentDefaults() {

		instance.executeSource(
		    """
		    foo = function( param1, param2="param2 default", param3, param4="param4 default" ) {
		    // Return entire arguments scope
		      	return arguments;
		      }
		      result = foo( 'Brad' );
		      """,
		    context );
		assertThat( variables.dereference( result, false ) instanceof ArgumentsScope ).isEqualTo( true );
		ArgumentsScope args = ( ArgumentsScope ) variables.dereference( result, false );

		assertThat( args.dereference( Key.of( "param1" ), false ) ).isEqualTo( "Brad" );
		assertThat( args.dereference( Key.of( "param2" ), false ) ).isEqualTo( "param2 default" );
		assertThat( args.dereference( Key.of( "param3" ), false ) ).isEqualTo( null );
		assertThat( args.dereference( Key.of( "param4" ), false ) ).isEqualTo( "param4 default" );

		assertThat( args.dereference( Key.of( "1" ), false ) ).isEqualTo( "Brad" );
		assertThat( args.dereference( Key.of( "2" ), false ) ).isEqualTo( "param2 default" );
		assertThat( args.dereference( Key.of( "3" ), false ) ).isEqualTo( null );
		assertThat( args.dereference( Key.of( "4" ), false ) ).isEqualTo( "param4 default" );

	}

	@DisplayName( "argument defaults arrow" )
	@Test
	public void testArgumentDefaultsArrow() {

		instance.executeSource(
		    """
		    foo = ( param1, param2="param2 default", param3, param4="param4 default" ) => arguments;
		    result = foo( 'Brad' );
		      """,
		    context );
		assertThat( variables.dereference( result, false ) instanceof ArgumentsScope ).isEqualTo( true );
		ArgumentsScope args = ( ArgumentsScope ) variables.dereference( result, false );

		assertThat( args.dereference( Key.of( "param1" ), false ) ).isEqualTo( "Brad" );
		assertThat( args.dereference( Key.of( "param2" ), false ) ).isEqualTo( "param2 default" );
		assertThat( args.dereference( Key.of( "param3" ), false ) ).isEqualTo( null );
		assertThat( args.dereference( Key.of( "param4" ), false ) ).isEqualTo( "param4 default" );

		assertThat( args.dereference( Key.of( "1" ), false ) ).isEqualTo( "Brad" );
		assertThat( args.dereference( Key.of( "2" ), false ) ).isEqualTo( "param2 default" );
		assertThat( args.dereference( Key.of( "3" ), false ) ).isEqualTo( null );
		assertThat( args.dereference( Key.of( "4" ), false ) ).isEqualTo( "param4 default" );

	}

	@DisplayName( "closure metadata" )
	@Test
	public void testClosureMetadata() {

		instance.executeSource(
		    """
		    foo = function(
		      	required string param1 hint="My param",
		      	numeric param2=42 luis="majano"
		      ) hint="my Closure" output=true brad="wood" {
		        return "value";
		      }
		      result = foo(5);
		          """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( "value" );
		Closure	UDFfoo	= ( ( Closure ) variables.dereference( foo, false ) );
		Struct	meta	= UDFfoo.getMetaData();

		assertThat( meta.dereference( Key.of( "name" ), false ) ).isEqualTo( "Closure" );
		assertThat( meta.dereference( Key.of( "hint" ), false ) ).isEqualTo( "my Closure" );
		assertThat( meta.dereference( Key.of( "output" ), false ) ).isEqualTo( true );
		assertThat( meta.dereference( Key.of( "brad" ), false ) ).isEqualTo( "wood" );
		assertThat( meta.dereference( Key.of( "returnType" ), false ) ).isEqualTo( "any" );
		assertThat( meta.dereference( Key.of( "access" ), false ) ).isEqualTo( "public" );

		Array args = ( ( Array ) meta.dereference( Key.of( "parameters" ), false ) );
		assertThat( args.size() ).isEqualTo( 2 );

		Struct param1 = ( Struct ) args.get( 0 );
		assertThat( param1.dereference( Key.of( "name" ), false ) ).isEqualTo( "param1" );
		assertThat( param1.dereference( Key.of( "hint" ), false ) ).isEqualTo( "My param" );
		assertThat( param1.dereference( Key.of( "required" ), false ) ).isEqualTo( true );
		assertThat( param1.dereference( Key.of( "type" ), false ) ).isEqualTo( "string" );

		Struct param2 = ( Struct ) args.get( 1 );
		assertThat( param2.dereference( Key.of( "name" ), false ) ).isEqualTo( "param2" );
		assertThat( param2.dereference( Key.of( "luis" ), false ) ).isEqualTo( "majano" );
		assertThat( param2.dereference( Key.of( "hint" ), false ) ).isEqualTo( "" );
		assertThat( param2.dereference( Key.of( "required" ), false ) ).isEqualTo( false );
		assertThat( param2.dereference( Key.of( "type" ), false ) ).isEqualTo( "numeric" );

		FunctionMeta	$bx			= ( ( FunctionMeta ) Referencer.get( UDFfoo, BoxMeta.key, false ) );
		Struct			annotations	= ( Struct ) $bx.meta.dereference( Key.of( "annotations" ), false );
		assertThat( annotations.dereference( Key.of( "hint" ), false ) ).isEqualTo( "my Closure" );
		assertThat( annotations.dereference( Key.of( "output" ), false ) ).isEqualTo( true );
		assertThat( annotations.dereference( Key.of( "brad" ), false ) ).isEqualTo( "wood" );

		Array	params				= ( Array ) $bx.meta.dereference( Key.of( "parameters" ), false );

		Struct	param1Annotations	= ( Struct ) Referencer.get( params.get( 0 ), Key.of( "annotations" ), false );
		assertThat( param1Annotations.dereference( Key.of( "hint" ), false ) ).isEqualTo( "My param" );

		Struct param2Annotations = ( Struct ) Referencer.get( params.get( 1 ), Key.of( "annotations" ), false );
		assertThat( param2Annotations.dereference( Key.of( "luis" ), false ) ).isEqualTo( "majano" );

	}

	@DisplayName( "closure metadata arrow" )
	@Test
	public void testClosureMetadataArrow() {

		instance.executeSource(
		    """
		    foo = (
		      	required string param1 hint="My param",
		      	numeric param2=42 luis="majano"
		      ) => {
		        return "value";
		      }
		      result = foo(5);
		          """,
		    context );
		assertThat( variables.dereference( result, false ) ).isEqualTo( "value" );
		Closure	UDFfoo	= ( ( Closure ) variables.dereference( foo, false ) );
		Struct	meta	= UDFfoo.getMetaData();

		assertThat( meta.dereference( Key.of( "name" ), false ) ).isEqualTo( "Closure" );
		assertThat( meta.dereference( Key.of( "access" ), false ) ).isEqualTo( "public" );

		Array args = ( ( Array ) meta.dereference( Key.of( "parameters" ), false ) );
		assertThat( args.size() ).isEqualTo( 2 );

		Struct param1 = ( Struct ) args.get( 0 );
		assertThat( param1.dereference( Key.of( "name" ), false ) ).isEqualTo( "param1" );
		assertThat( param1.dereference( Key.of( "hint" ), false ) ).isEqualTo( "My param" );
		assertThat( param1.dereference( Key.of( "required" ), false ) ).isEqualTo( true );
		assertThat( param1.dereference( Key.of( "type" ), false ) ).isEqualTo( "string" );

		Struct param2 = ( Struct ) args.get( 1 );
		assertThat( param2.dereference( Key.of( "name" ), false ) ).isEqualTo( "param2" );
		assertThat( param2.dereference( Key.of( "luis" ), false ) ).isEqualTo( "majano" );
		assertThat( param2.dereference( Key.of( "hint" ), false ) ).isEqualTo( "" );
		assertThat( param2.dereference( Key.of( "required" ), false ) ).isEqualTo( false );
		assertThat( param2.dereference( Key.of( "type" ), false ) ).isEqualTo( "numeric" );

		FunctionMeta	$bx					= ( ( FunctionMeta ) Referencer.get( UDFfoo, BoxMeta.key, false ) );
		Array			params				= ( Array ) $bx.meta.dereference( Key.of( "parameters" ), false );

		Struct			param1Annotations	= ( Struct ) Referencer.get( params.get( 0 ), Key.of( "annotations" ), false );
		assertThat( param1Annotations.dereference( Key.of( "hint" ), false ) ).isEqualTo( "My param" );

		Struct param2Annotations = ( Struct ) Referencer.get( params.get( 1 ), Key.of( "annotations" ), false );
		assertThat( param2Annotations.dereference( Key.of( "luis" ), false ) ).isEqualTo( "majano" );

	}

	@DisplayName( "positional arguments" )
	@Test
	public void testPositionalArguments() {

		instance.executeSource(
		    """
		    result = {};
		       foo = function( param1, param2 ) {
		       	result = arguments;
		       }
		       foo( 'value1', 'value2', "value3", "value4" );
		       """,
		    context );

		ArgumentsScope argsScope = ( ArgumentsScope ) variables.dereference( Key.of( "result" ), false );
		assertThat( argsScope.size() ).isEqualTo( 4 );
		Object[] args = argsScope.asNativeArray();
		assertThat( args[ 0 ] ).isEqualTo( "value1" );
		assertThat( args[ 1 ] ).isEqualTo( "value2" );
		assertThat( args[ 2 ] ).isEqualTo( "value3" );
		assertThat( args[ 3 ] ).isEqualTo( "value4" );

		List<String> keys = argsScope.getKeys();
		assertThat( keys.get( 0 ) ).isEqualTo( "param1" );
		assertThat( keys.get( 1 ) ).isEqualTo( "param2" );
		assertThat( keys.get( 2 ) ).isEqualTo( "3" );
		assertThat( keys.get( 3 ) ).isEqualTo( "4" );

	}

	@DisplayName( "positional arguments arrow" )
	@Test
	public void testPositionalArgumentsArrow() {

		instance.executeSource(
		    """
		    result = {};
		       foo = ( param1, param2 ) => result = arguments;
		       foo( 'value1', 'value2', "value3", "value4" );
		       """,
		    context );

		ArgumentsScope argsScope = ( ArgumentsScope ) variables.dereference( Key.of( "result" ), false );
		assertThat( argsScope.size() ).isEqualTo( 4 );
		Object[] args = argsScope.asNativeArray();
		assertThat( args[ 0 ] ).isEqualTo( "value1" );
		assertThat( args[ 1 ] ).isEqualTo( "value2" );
		assertThat( args[ 2 ] ).isEqualTo( "value3" );
		assertThat( args[ 3 ] ).isEqualTo( "value4" );

		List<String> keys = argsScope.getKeys();
		assertThat( keys.get( 0 ) ).isEqualTo( "param1" );
		assertThat( keys.get( 1 ) ).isEqualTo( "param2" );
		assertThat( keys.get( 2 ) ).isEqualTo( "3" );
		assertThat( keys.get( 3 ) ).isEqualTo( "4" );

	}

	@DisplayName( "named arguments" )
	@Test
	@Disabled
	public void testNamedArguments() {

		instance.executeSource(
		    """
		    result = {};
		       foo = function( param1, param2 ) {
		       	result = arguments;
		       }
		       foo( param1='value1', param2='value2', param3="value3", param4="value4" );
		       """,
		    context );

		ArgumentsScope argsScope = ( ArgumentsScope ) variables.dereference( Key.of( "result" ), false );
		assertThat( argsScope.size() ).isEqualTo( 4 );
		Object[] args = argsScope.asNativeArray();
		assertThat( args[ 0 ] ).isEqualTo( "value1" );
		assertThat( args[ 1 ] ).isEqualTo( "value2" );
		assertThat( args[ 2 ] ).isEqualTo( "value3" );
		assertThat( args[ 3 ] ).isEqualTo( "value4" );

		List<String> keys = argsScope.getKeys();
		assertThat( keys.get( 0 ) ).isEqualTo( "param1" );
		assertThat( keys.get( 1 ) ).isEqualTo( "param2" );
		assertThat( keys.get( 2 ) ).isEqualTo( "param3" );
		assertThat( keys.get( 3 ) ).isEqualTo( "param4" );

	}

	@DisplayName( "argument collection" )
	@Test
	@Disabled
	public void testArgumentCollection() {

		instance.executeSource(
		    """
		        result = {};
		        	foo = ( param1, param2 ) => {
		           	result = arguments;
		           }
		           foo(
		    	argumentCollection={
		    		param1:'value1',
		    		param2 : 'value2',
		    		param3 :"value3",
		    		param4: "value4"
		    	}
		    );
		      """,
		    context );

		ArgumentsScope argsScope = ( ArgumentsScope ) variables.dereference( Key.of( "result" ), false );
		assertThat( argsScope.size() ).isEqualTo( 4 );
		Object[] args = argsScope.asNativeArray();
		assertThat( args[ 0 ] ).isEqualTo( "value1" );
		assertThat( args[ 1 ] ).isEqualTo( "value2" );
		assertThat( args[ 2 ] ).isEqualTo( "value3" );
		assertThat( args[ 3 ] ).isEqualTo( "value4" );

		List<String> keys = argsScope.getKeys();
		assertThat( keys.get( 0 ) ).isEqualTo( "param1" );
		assertThat( keys.get( 1 ) ).isEqualTo( "param2" );
		assertThat( keys.get( 2 ) ).isEqualTo( "param3" );
		assertThat( keys.get( 3 ) ).isEqualTo( "param4" );

	}

	@DisplayName( "IIFE" )
	@Test
	public void testIIFE() {

		instance.executeSource(
		    """
		      result = (function() { return "done" })()
		    """,
		    context );

		assertThat( variables.dereference( result, false ) ).isEqualTo( "done" );

	}

	@DisplayName( "IIFE arrow" )
	@Test
	public void testIIFEArrow() {

		instance.executeSource(
		    """
		      result = (()=>"done")()
		    """,
		    context );

		assertThat( variables.dereference( result, false ) ).isEqualTo( "done" );

	}

	@DisplayName( "lexical scoping" )
	@Test
	public void testLexicalScoping() {

		instance.executeSource(
		    """
		    declaringScope = "foobar";
		    result = ()=>declaringScope;
		    """,
		    context );

		Closure		closure			= ( Closure ) variables.dereference( result, false );

		IBoxContext	newContext		= new ScriptingBoxContext( instance.getRuntimeContext() );
		IScope		newVariables	= newContext.getScopeNearby( VariablesScope.name );

		// Stick our original closure into the new variables scope in the new context
		newVariables.assign( Key.of( "originalClosure" ), closure );
		instance.executeSource(
		    """
		      result = originalClosure();
		    """,
		    newContext );

		assertThat( newVariables.dereference( result, false ) ).isEqualTo( "foobar" );

	}

	@DisplayName( "function returns function" )
	@Test
	public void testFunctionReturnsFunction() {

		instance.executeSource(
		    """
		      foo = () => {
		         	return () => {
		         		return "done";
		         	}
		         }
		    result = foo()();
		       """,
		    context );

		assertThat( variables.dereference( result, false ) ).isEqualTo( "done" );

	}

	@DisplayName( "higher order function" )
	@Test
	public void testHigherOrderFunction() {

		instance.executeSource(
		    """
		        foo = ( provider ) => {
		           	return provider();
		           }
		    myProvider = ()=>"Brad";

		      result = foo( myProvider );
		         """,
		    context );

		assertThat( variables.dereference( result, false ) ).isEqualTo( "Brad" );

		instance.executeSource(
		    """
		     foo = ( provider ) => {
		        	return provider();
		        }

		    result = foo( ()=>"Luis" );
		      """,
		    context );

		assertThat( variables.dereference( result, false ) ).isEqualTo( "Luis" );

		instance.executeSource(
		    """
		          foo = ( provider ) => {
		             	return provider();
		             }

		    function myProvider() {
		    	return "Gavin";
		    }

		        result = foo( myProvider );
		           """,
		    context );

		assertThat( variables.dereference( result, false ) ).isEqualTo( "Gavin" );

	}

}