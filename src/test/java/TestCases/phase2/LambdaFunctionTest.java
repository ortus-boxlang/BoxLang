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
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Lambda;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.FunctionMeta;

public class LambdaFunctionTest {

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

	@DisplayName( "basic lambda" )
	@Test
	public void testBasicLambda() {

		instance.executeSource(
		    """
		       foo = () -> {
		    	return "my func";
		    }
		    result = foo();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "my func" );
		assertThat( variables.get( foo ) instanceof Lambda ).isEqualTo( true );

		IStruct meta = ( ( Lambda ) variables.get( foo ) ).getMetaData();

		assertThat( meta.get( Key.of( "name" ) ) ).isEqualTo( "Lambda" );
		// Defaults
		assertThat( meta.get( Key.of( "hint" ) ) ).isEqualTo( "" );
		assertThat( meta.get( Key.of( "output" ) ) ).isEqualTo( false );
		assertThat( meta.get( Key.of( "returnType" ) ) ).isEqualTo( "any" );
		assertThat( meta.get( Key.of( "access" ) ) ).isEqualTo( "public" );

	}

	@DisplayName( "basic Lambda no body" )
	@Test
	public void testBasicLambdaNoBody() {

		instance.executeSource(
		    """
		        foo = () -> "my func";
		    	result = foo();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "my func" );
		assertThat( variables.get( foo ) instanceof Lambda ).isEqualTo( true );

		IStruct meta = ( ( Lambda ) variables.get( foo ) ).getMetaData();

		assertThat( meta.get( Key.of( "name" ) ) ).isEqualTo( "Lambda" );
		// Defaults
		assertThat( meta.get( Key.of( "hint" ) ) ).isEqualTo( "" );
		assertThat( meta.get( Key.of( "output" ) ) ).isEqualTo( false );
		assertThat( meta.get( Key.of( "returnType" ) ) ).isEqualTo( "any" );
		assertThat( meta.get( Key.of( "access" ) ) ).isEqualTo( "public" );

	}

	@DisplayName( "basic Lambda no body no parens" )
	@Test
	public void testBasicLambdaNoBodyNoParens() {

		instance.executeSource(
		    """
		        foo = param1 -> param1;
		    	result = foo("my func");
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "my func" );
		assertThat( variables.get( foo ) instanceof Lambda ).isEqualTo( true );

		IStruct meta = ( ( Lambda ) variables.get( foo ) ).getMetaData();

		assertThat( meta.get( Key.of( "name" ) ) ).isEqualTo( "Lambda" );
		// Defaults
		assertThat( meta.get( Key.of( "hint" ) ) ).isEqualTo( "" );
		assertThat( meta.get( Key.of( "output" ) ) ).isEqualTo( false );
		assertThat( meta.get( Key.of( "returnType" ) ) ).isEqualTo( "any" );
		assertThat( meta.get( Key.of( "access" ) ) ).isEqualTo( "public" );

	}

	@DisplayName( "argument defaults" )
	@Test
	public void testArgumentDefaults() {

		instance.executeSource(
		    """
		    foo = ( param1, param2="param2 default", param3, param4="param4 default" ) -> arguments;
		    result = foo( 'Brad' );
		      """,
		    context );
		assertThat( variables.get( result ) instanceof ArgumentsScope ).isEqualTo( true );
		ArgumentsScope args = ( ArgumentsScope ) variables.get( result );

		assertThat( args.get( Key.of( "param1" ) ) ).isEqualTo( "Brad" );
		assertThat( args.get( Key.of( "param2" ) ) ).isEqualTo( "param2 default" );
		assertThat( args.get( Key.of( "param3" ) ) ).isEqualTo( null );
		assertThat( args.get( Key.of( "param4" ) ) ).isEqualTo( "param4 default" );

		assertThat( args.get( Key.of( "1" ) ) ).isEqualTo( "Brad" );
		assertThat( args.get( Key.of( "2" ) ) ).isEqualTo( "param2 default" );
		assertThat( args.get( Key.of( "3" ) ) ).isEqualTo( null );
		assertThat( args.get( Key.of( "4" ) ) ).isEqualTo( "param4 default" );

	}

	@DisplayName( "lambda metadata" )
	@Test
	public void testLambdaMetadata() {

		instance.executeSource(
		    """
		    foo = (
		      	required string param1 hint="My param",
		      	numeric param2=42 luis="majano"
		      ) hint="my Closure" output=false brad='wood' -> {
		        return "value";
		      }
		      result = foo(4);
		          """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "value" );
		Lambda	UDFfoo	= ( ( Lambda ) variables.get( foo ) );
		IStruct	meta	= UDFfoo.getMetaData();

		assertThat( meta.get( Key.of( "name" ) ) ).isEqualTo( "Lambda" );
		assertThat( meta.get( Key.of( "access" ) ) ).isEqualTo( "public" );

		Array args = ( ( Array ) meta.get( Key.of( "parameters" ) ) );
		assertThat( args.size() ).isEqualTo( 2 );

		IStruct param1 = ( IStruct ) args.get( 0 );
		assertThat( param1.get( Key.of( "name" ) ) ).isEqualTo( "param1" );
		assertThat( param1.get( Key.of( "hint" ) ) ).isEqualTo( "My param" );
		assertThat( param1.get( Key.of( "required" ) ) ).isEqualTo( true );
		assertThat( param1.get( Key.of( "type" ) ) ).isEqualTo( "string" );

		IStruct param2 = ( IStruct ) args.get( 1 );
		assertThat( param2.get( Key.of( "name" ) ) ).isEqualTo( "param2" );
		assertThat( param2.get( Key.of( "luis" ) ) ).isEqualTo( "majano" );
		assertThat( param2.get( Key.of( "hint" ) ) ).isEqualTo( "" );
		assertThat( param2.get( Key.of( "required" ) ) ).isEqualTo( false );
		assertThat( param2.get( Key.of( "type" ) ) ).isEqualTo( "numeric" );

		FunctionMeta	$bx			= ( ( FunctionMeta ) Referencer.get( context, UDFfoo, BoxMeta.key, false ) );
		IStruct			annotations	= ( IStruct ) $bx.meta.get( Key.of( "annotations" ) );
		assertThat( annotations.get( Key.of( "hint" ) ) ).isEqualTo( "my Closure" );
		assertThat( annotations.get( Key.of( "output" ) ) ).isEqualTo( false );
		assertThat( annotations.get( Key.of( "brad" ) ) ).isEqualTo( "wood" );

		Array	params				= ( Array ) $bx.meta.get( Key.of( "parameters" ) );

		IStruct	param1Annotations	= ( IStruct ) Referencer.get( context, params.get( 0 ), Key.of( "annotations" ), false );
		assertThat( param1Annotations.get( Key.of( "hint" ) ) ).isEqualTo( "My param" );

		IStruct param2Annotations = ( IStruct ) Referencer.get( context, params.get( 1 ), Key.of( "annotations" ), false );
		assertThat( param2Annotations.get( Key.of( "luis" ) ) ).isEqualTo( "majano" );

	}

	@DisplayName( "positional arguments" )
	@Test
	public void testPositionalArguments() {

		instance.executeSource(
		    """
		    foo = ( param1, param2 ) -> arguments;
		    result=foo( 'value1', 'value2', "value3", "value4" );
		    """,
		    context );

		ArgumentsScope argsScope = ( ArgumentsScope ) variables.get( Key.of( "result" ) );
		assertThat( argsScope.size() ).isEqualTo( 4 );
		Object[] args = argsScope.asNativeArray();
		assertThat( args[ 0 ] ).isEqualTo( "value1" );
		assertThat( args[ 1 ] ).isEqualTo( "value2" );
		assertThat( args[ 2 ] ).isEqualTo( "value3" );
		assertThat( args[ 3 ] ).isEqualTo( "value4" );

		List<String> keys = argsScope.getKeysAsStrings();
		assertThat( keys.get( 0 ) ).isEqualTo( "param1" );
		assertThat( keys.get( 1 ) ).isEqualTo( "param2" );
		assertThat( keys.get( 2 ) ).isEqualTo( "3" );
		assertThat( keys.get( 3 ) ).isEqualTo( "4" );

	}

	@DisplayName( "named arguments" )
	@Test
	public void testNamedArguments() {

		instance.executeSource(
		    """
		     foo = ( param1, param2 ) -> {
		     	return arguments;
		     }
		    result =  foo( param1='value1', param2='value2', param3="value3", param4="value4" );
		     """,
		    context );

		ArgumentsScope argsScope = ( ArgumentsScope ) variables.get( Key.of( "result" ) );
		assertThat( argsScope.size() ).isEqualTo( 4 );
		Object[] args = argsScope.asNativeArray();
		assertThat( args[ 0 ] ).isEqualTo( "value1" );
		assertThat( args[ 1 ] ).isEqualTo( "value2" );
		assertThat( args[ 2 ] ).isEqualTo( "value3" );
		assertThat( args[ 3 ] ).isEqualTo( "value4" );

		List<String> keys = argsScope.getKeysAsStrings();
		assertThat( keys.get( 0 ) ).isEqualTo( "param1" );
		assertThat( keys.get( 1 ) ).isEqualTo( "param2" );
		assertThat( keys.get( 2 ) ).isEqualTo( "param3" );
		assertThat( keys.get( 3 ) ).isEqualTo( "param4" );

	}

	@DisplayName( "argument collection" )
	@Test
	public void testArgumentCollection() {

		instance.executeSource(
		    """
		        	foo = ( param1, param2 ) -> {
		           	return arguments;
		           }
		        result =    foo(
		    	argumentCollection=[
		    		param1:'value1',
		    		param2 : 'value2',
		    		param3 :"value3",
		    		param4: "value4"
		    	]
		    );
		      """,
		    context );

		ArgumentsScope argsScope = ( ArgumentsScope ) variables.get( Key.of( "result" ) );
		assertThat( argsScope.size() ).isEqualTo( 4 );
		Object[] args = argsScope.asNativeArray();
		assertThat( args[ 0 ] ).isEqualTo( "value1" );
		assertThat( args[ 1 ] ).isEqualTo( "value2" );
		assertThat( args[ 2 ] ).isEqualTo( "value3" );
		assertThat( args[ 3 ] ).isEqualTo( "value4" );

		List<String> keys = argsScope.getKeysAsStrings();
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
		      result = (()->"done")()
		    """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "done" );

	}

	@DisplayName( "function returns function" )
	@Test
	public void testFunctionReturnsFunction() {

		instance.executeSource(
		    """
		      foo = () -> {
		         	return () -> {
		         		return "done";
		         	}
		         }
		    result = foo()();
		       """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "done" );

	}

	@DisplayName( "higher order function" )
	@Test
	public void testHigherOrderFunction() {

		instance.executeSource(
		    """
		        foo = ( provider ) -> {
		           	return provider();
		           }
		    myProvider = ()->"Brad";

		      result = foo( myProvider );
		         """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "Brad" );

		instance.executeSource(
		    """
		     foo = ( provider ) -> {
		        	return provider();
		        }

		    result = foo( ()->"Luis" );
		      """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "Luis" );

		instance.executeSource(
		    """
		          foo = ( provider ) -> {
		             	return provider();
		             }

		    function myProvider() {
		    	return "Gavin";
		    }

		        result = foo( myProvider );
		           """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "Gavin" );

	}

	@DisplayName( "empty body" )
	@Test
	public void testEmptyBody() {

		instance.executeSource(
		    """
		      myLambda = ()->{};
		    result = myLambda();
		       """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( null );

		instance.executeSource(
		    """
		      myLambda = i->{};
		    result = myLambda(6);
		       """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( null );

	}

	@DisplayName( "no body" )
	@Test
	public void testNoBody() {

		instance.executeSource(
		    """
		      myLambda = i->i;
		    result = myLambda(6);
		       """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( 6 );

	}

	@DisplayName( "body with without returns" )
	@Test
	public void testBodyWithWithoutReturns() {

		instance.executeSource(
		    """
		        myLambda = i=>{
		    return i
		     };
		      result = myLambda(6);
		         """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( 6 );

		instance.executeSource(
		    """
		        myLambda = i=>{
		    return
		     };
		      result = myLambda(6);
		         """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( null );

		instance.executeSource(
		    """
		        myLambda = i=>{
		    return null;
		     };
		      result = myLambda(6);
		         """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( null );

	}

}