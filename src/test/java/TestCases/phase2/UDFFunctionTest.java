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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.javaboxpiler.JavaBoxpiler;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Lambda;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.types.exceptions.ParseException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.FunctionMeta;
import ortus.boxlang.runtime.util.ResolvedFilePath;

public class UDFFunctionTest {

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

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "basic UDF" )
	@Test
	public void testBasicUDF() {

		instance.executeSource(
		    """
		       function foo() {
		    	return "my func";
		    }
		    result = foo();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "my func" );
		assertThat( variables.get( foo ) instanceof UDF ).isEqualTo( true );

		IStruct meta = ( ( UDF ) variables.get( foo ) ).getMetaData();

		assertThat( meta.get( Key.of( "name" ) ) ).isEqualTo( "foo" );
		// Defaults
		assertThat( meta.get( Key.of( "hint" ) ) ).isEqualTo( "" );
		assertThat( meta.get( Key.of( "output" ) ) ).isEqualTo( false );
		assertThat( meta.get( Key.of( "returnType" ) ) ).isEqualTo( "Any" );
		assertThat( meta.get( Key.of( "access" ) ) ).isEqualTo( "public" );

	}

	@DisplayName( "argument defaults" )
	@Test
	public void testArgumentDefaults() {

		instance.executeSource(
		    """
		         function foo( param1, param2="param2 default", param3, param4="param4 default" ) {
		    // Return entire arguments scope
		      	return arguments;
		      }
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

	@DisplayName( "return argument" )
	@Test
	public void testReturnArgument() {

		instance.executeSource(
		    """
		       function foo( param ) {
		    	return param;
		    }
		    result = foo( 'Brad' );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Brad" );
		assertThat( variables.get( foo ) instanceof UDF ).isEqualTo( true );

	}

	@DisplayName( "It should allow you to declare multiple functions" )
	@Test
	public void testMultipleFunctionDeclarations() {

		instance.executeSource(
		    """
		          function foo( param ) {
		       	return param;
		       }
		    function bar( param ) {
		       	return param;
		       }
		       result = foo( 'Brad' );
		       """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Brad" );
		assertThat( variables.get( foo ) instanceof UDF ).isEqualTo( true );

	}

	@DisplayName( "It should not allow you to declare multiple functions with the same name" )
	@Test
	public void testMultipleFunctionDeclarationsSameName() {
		if ( instance.getCompiler() instanceof JavaBoxpiler ) {
			return;
		}

		assertThrows( IllegalStateException.class, () -> {
			instance.executeSource(
			    """
			          function foo() {
			       	return "first";
			       }
			    function foo() {
			       	return "second";
			       }
			       result = foo();
			       """,
			    context );
		} );
	}

	@DisplayName( "It should allow you to include functions with the same name" )
	@Test
	public void testMultipleFunctionDeclarationsSameNameInclude() {
		instance.executeSource(
		    """
		    include template="src/test/java/TestCases/phase2/includeFuncs.cfm";
		             function foo() {
		          	return "first";
		          }
		          result = foo();
		          """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "second" );
		assertThat( variables.get( foo ) instanceof UDF ).isEqualTo( true );

	}

	@DisplayName( "UDF metadata optional values" )
	@Test
	public void testUDFMetadataOptionalValues() {

		instance.executeSource(
		    """
		    function foo() k1 k2="v2" k3 k4:"v4" k5  {
		      }
		          """,
		    context );
		UDF				UDFfoo		= ( ( UDF ) variables.get( foo ) );

		// Now we test our "new" meta view which breaks out actual UDF meta, javaadoc, and annotations (inline and @)
		FunctionMeta	$bx			= ( ( FunctionMeta ) Referencer.get( context, UDFfoo, BoxMeta.key, false ) );
		IStruct			annotations	= ( IStruct ) $bx.meta.get( Key.of( "annotations" ) );
		assertThat( annotations.get( Key.of( "k1" ) ) ).isEqualTo( "" );
		assertThat( annotations.get( Key.of( "k2" ) ) ).isEqualTo( "v2" );
		assertThat( annotations.get( Key.of( "k3" ) ) ).isEqualTo( "" );
		assertThat( annotations.get( Key.of( "k4" ) ) ).isEqualTo( "v4" );
		assertThat( annotations.get( Key.of( "k5" ) ) ).isEqualTo( "" );

	}

	@DisplayName( "UDF metadata" )
	@Test
	public void testUDFMetadata() {

		instance.executeSource(
		    """
		      @myRealAnnotation
		      @anotherAnnotation "brad"
		      @anotherAnnotation2 42
		      @anotherAnnotation3 true
		      @anotherAnnotation4 [1,2,3]
		      @anotherAnnotation5 { foo : 'bar' }
		    @letsGetFunky "brad" 42 true [1,2,3] { foo : 'bar' }
		         public String function foo(
		         	required string param1 hint="My param",
		         	numeric param2=42 luis="majano"
		         ) hint="my UDF" output=true brad="wood" {
		           return "value";
		         }
		         result = foo(5);
		             """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "value" );
		UDF		UDFfoo	= ( ( UDF ) variables.get( foo ) );
		IStruct	meta	= UDFfoo.getMetaData();

		// "Legacy" meta view just crams all the annotations into a struct with the "real" UDF metadata
		assertThat( meta.get( Key.of( "name" ) ) ).isEqualTo( "foo" );
		assertThat( meta.get( Key.of( "hint" ) ) ).isEqualTo( "my UDF" );
		assertThat( meta.get( Key.of( "output" ) ) ).isEqualTo( true );
		assertThat( meta.get( Key.of( "brad" ) ) ).isEqualTo( "wood" );
		assertThat( meta.get( Key.of( "returnType" ) ) ).isEqualTo( "String" );
		assertThat( meta.get( Key.of( "access" ) ) ).isEqualTo( "public" );
		assertThat( meta.get( Key.of( "myRealAnnotation" ) ) ).isEqualTo( "" );
		assertThat( meta.get( Key.of( "anotherAnnotation" ) ) ).isEqualTo( "brad" );
		assertThat( meta.get( Key.of( "anotherAnnotation2" ) ) ).isEqualTo( 42 );
		assertThat( meta.get( Key.of( "anotherAnnotation3" ) ) ).isEqualTo( true );
		assertThat( meta.get( Key.of( "anotherAnnotation4" ) ) instanceof Array ).isTrue();
		assertThat( meta.get( Key.of( "anotherAnnotation5" ) ) instanceof IStruct ).isTrue();
		assertThat( meta.get( Key.of( "letsGetFunky" ) ) instanceof Array ).isTrue();
		Array funkyAnnoationMeta = meta.getAsArray( Key.of( "letsGetFunky" ) );
		assertThat( funkyAnnoationMeta.size() ).isEqualTo( 5 );
		assertThat( funkyAnnoationMeta.get( 0 ) ).isEqualTo( "brad" );
		assertThat( funkyAnnoationMeta.get( 1 ) ).isEqualTo( 42 );
		assertThat( funkyAnnoationMeta.get( 2 ) ).isEqualTo( true );
		assertThat( funkyAnnoationMeta.get( 3 ) instanceof Array ).isTrue();
		assertThat( funkyAnnoationMeta.get( 4 ) instanceof IStruct ).isTrue();

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

		// Now we test our "new" meta view which breaks out actual UDF meta, javaadoc, and annotations (inline and @)
		FunctionMeta	$bx			= ( ( FunctionMeta ) Referencer.get( context, UDFfoo, BoxMeta.key, false ) );
		IStruct			annotations	= ( IStruct ) $bx.meta.get( Key.of( "annotations" ) );
		assertThat( annotations.get( Key.of( "hint" ) ) ).isEqualTo( "my UDF" );
		assertThat( annotations.get( Key.of( "output" ) ) ).isEqualTo( true );
		assertThat( annotations.get( Key.of( "brad" ) ) ).isEqualTo( "wood" );
		assertThat( annotations.get( Key.of( "myRealAnnotation" ) ) ).isEqualTo( "" );
		assertThat( annotations.get( Key.of( "anotherAnnotation" ) ) ).isEqualTo( "brad" );
		assertThat( annotations.get( Key.of( "anotherAnnotation2" ) ) ).isEqualTo( 42 );
		assertThat( annotations.get( Key.of( "anotherAnnotation3" ) ) ).isEqualTo( true );
		assertThat( annotations.get( Key.of( "anotherAnnotation4" ) ) instanceof Array ).isTrue();
		assertThat( annotations.get( Key.of( "anotherAnnotation5" ) ) instanceof IStruct ).isTrue();
		assertThat( annotations.get( Key.of( "letsGetFunky" ) ) instanceof Array ).isTrue();
		Array funkyAnnoation = annotations.getAsArray( Key.of( "letsGetFunky" ) );
		assertThat( funkyAnnoation.size() ).isEqualTo( 5 );
		assertThat( funkyAnnoation.get( 0 ) ).isEqualTo( "brad" );
		assertThat( funkyAnnoation.get( 1 ) ).isEqualTo( 42 );
		assertThat( funkyAnnoation.get( 2 ) ).isEqualTo( true );
		assertThat( funkyAnnoation.get( 3 ) instanceof Array ).isTrue();
		assertThat( funkyAnnoation.get( 4 ) instanceof IStruct ).isTrue();

		Array	params				= ( Array ) $bx.meta.get( Key.of( "parameters" ) );
		IStruct	param1Annotations	= ( IStruct ) Referencer.get( context, params.get( 0 ), Key.of( "annotations" ), false );
		assertThat( param1Annotations.get( Key.of( "hint" ) ) ).isEqualTo( "My param" );
		IStruct param2Annotations = ( IStruct ) Referencer.get( context, params.get( 1 ), Key.of( "annotations" ), false );
		assertThat( param2Annotations.get( Key.of( "luis" ) ) ).isEqualTo( "majano" );
	}

	@DisplayName( "UDF metadata javadoc" )
	@Test
	public void testUDFMetadataJavadoc() {

		instance.executeSource(
		    """
		    /**
		    * my UDF
		    * also more hint here
		    *
		    * @param1.hint My param
		    *
		    * @param2 param2 hint
		    * @param2.luis majano
		    * is spread across two lines
		    * @mxunit:expectedException
		    *
		    * @returns Pure Gold
		    */
		          public String function foo( param1, param2 ) output=true brad="wood" inject {
		           	return "value";
		           }
		        result = foo();
		             """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "value" );
		UDF		UDFfoo	= ( ( UDF ) variables.get( foo ) );
		IStruct	meta	= UDFfoo.getMetaData();

		assertThat( meta.get( Key.of( "name" ) ) ).isEqualTo( "foo" );
		assertThat( meta.get( Key.of( "output" ) ) ).isEqualTo( true );
		assertThat( meta.get( Key.of( "brad" ) ) ).isEqualTo( "wood" );
		assertThat( meta.get( Key.of( "inject" ) ) ).isEqualTo( "" );
		assertThat( meta.get( Key.of( "returnType" ) ) ).isEqualTo( "String" );
		assertThat( meta.get( Key.of( "access" ) ) ).isEqualTo( "public" );

		Array args = ( ( Array ) meta.get( Key.of( "parameters" ) ) );
		assertThat( args.size() ).isEqualTo( 2 );

		IStruct param1 = ( IStruct ) args.get( 0 );
		assertThat( param1.get( Key.of( "name" ) ) ).isEqualTo( "param1" );
		assertThat( param1.get( Key.of( "required" ) ) ).isEqualTo( false );
		assertThat( param1.get( Key.of( "type" ) ) ).isEqualTo( "Any" );

		IStruct param2 = ( IStruct ) args.get( 1 );
		assertThat( param2.get( Key.of( "name" ) ) ).isEqualTo( "param2" );
		assertThat( param2.getAsString( Key.of( "hint" ) ).trim() ).isEqualTo( "param2 hint" );
		assertThat( param2.get( Key.of( "required" ) ) ).isEqualTo( false );

		FunctionMeta	$bx				= ( ( FunctionMeta ) Referencer.get( context, UDFfoo, BoxMeta.key, false ) );
		IStruct			documentation	= ( IStruct ) $bx.meta.get( Key.of( "documentation" ) );
		assertThat( documentation.get( Key.of( "hint" ) ) ).isEqualTo( "my UDF also more hint here" );
		assertThat( documentation.get( Key.of( "returns" ) ) ).isEqualTo( "Pure Gold" );
		assertThat( documentation ).containsKey( Key.of( "mxunit:expectedException" ) );
		assertThat( documentation.getAsString( Key.of( "mxunit:expectedException" ) ).trim() ).isEqualTo( "" );

		Array	params				= ( Array ) $bx.meta.get( Key.of( "parameters" ) );

		IStruct	param1Documentation	= ( IStruct ) Referencer.get( context, params.get( 0 ), Key.of( "documentation" ), false );
		assertThat( param1Documentation.getAsString( Key.of( "hint" ) ).trim() ).isEqualTo( "My param" );

		IStruct param2Documentation = ( IStruct ) Referencer.get( context, params.get( 1 ), Key.of( "documentation" ), false );
		assertThat( param2Documentation.getAsString( Key.of( "hint" ) ).trim() ).isEqualTo( "param2 hint" );
		assertThat( param2Documentation.getAsString( Key.of( "luis" ) ).trim() ).isEqualTo( "majano is spread across two lines" );

		IStruct annotations = $bx.meta.getAsStruct( Key.of( "annotations" ) );
		assertThat( annotations.getAsBoolean( Key.of( "output" ) ) ).isEqualTo( true );
		assertThat( annotations.getAsString( Key.of( "brad" ) ) ).isEqualTo( "wood" );
		assertThat( annotations.getAsString( Key.of( "inject" ) ) ).isEqualTo( "" );

	}

	@DisplayName( "positional arguments" )
	@Test
	public void testPositionalArguments() {

		instance.executeSource(
		    """
		    result = {};
		       function foo( param1, param2 ) {
		       	result = arguments;
		       }
		       foo( 'value1', 'value2', "value3", "value4" );
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

	@DisplayName( "named arguments equals" )
	@Test
	public void testNamedArgumentsEquals() {

		instance.executeSource(
		    """
		    result = {};
		       function foo( param1, param2 ) {
		       	result = arguments;
		       }
		       foo( param1='value1', param2='value2', param3="value3", param4="value4" );
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

	@DisplayName( "named arguments colon" )
	@Test
	public void testNamedArgumentsColon() {

		instance.executeSource(
		    """
		    result = {};
		       function foo( param1, param2 ) {
		       	result = arguments;
		       }
		       foo( param1:'value1', param2 : 'value2', param3 :"value3", param4: "value4" );
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

	@DisplayName( "named positional arguments mix" )
	@Test
	public void testNamedPositionalArgumentsMix() {

		Throwable e = assertThrows( ParseException.class, () -> instance.executeSource(
		    """
		    function foo() {
		    }
		    foo( param1='value1', 'value2' );
		    """ ) );

		assertThat( e.getMessage() ).contains( "named" );

		e = assertThrows( ParseException.class, () -> instance.executeSource(
		    """
		    function foo() {
		    }
		    foo( 'value2', param1='value1' );
		    """ ) );

		assertThat( e.getMessage() ).contains( "named" );

	}

	@DisplayName( "argument collection" )
	@Test
	public void testArgumentCollection() {

		instance.executeSource(
		    """
		        result = {};
		           function foo( param1, param2 ) {
		           	result = arguments;
		           }
		           foo(
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

	@Test
	public void testUDFInJava() {

		Function func = new Lambda() {

			public Object _invoke( FunctionBoxContext context ) {
				return context.getScopeNearby( ArgumentsScope.name ).dereference( context, Key.of( "param1" ), false );
			}

			@Override
			public List<ImportDefinition> getImports() {
				return null;
			}

			@Override
			public Key getName() {
				return Key.of( "myFunc" );
			}

			@Override
			public Argument[] getArguments() {
				return new Argument[] { new Argument( false, "string", Key.of( "param1" ) ) };
			}

			@Override
			public String getReturnType() {
				return "any";
			}

			@Override
			public IStruct getAnnotations() {
				return Struct.EMPTY;
			}

			@Override
			public IStruct getDocumentation() {
				return Struct.EMPTY;
			}

			@Override
			public Access getAccess() {
				return Access.PUBLIC;
			}

			@Override
			public long getRunnableCompileVersion() {
				return 0;
			}

			@Override
			public LocalDateTime getRunnableCompiledOn() {
				return null;
			}

			@Override
			public Object getRunnableAST() {
				return null;
			}

			public ResolvedFilePath getRunnablePath() {
				return ResolvedFilePath.of( Path.of( "unknown" ) );
			}

			public BoxSourceType getSourceType() {
				return BoxSourceType.BOXSCRIPT;
			}
		};

		variables.put( Key.of( "myFunc" ), func );

		instance.executeSource(
		    """
		    result = myFunc( "brad" );
		    println( result )
		       """,
		    context, BoxSourceType.BOXSCRIPT );

	}

	@DisplayName( "Runtime Default" )
	@Test
	public void testRuntimeDefault() {

		instance.executeSource(
		    """
		    function foo( param1=request.foo ) {
		    	return arguments.param1;
		    }
		    request.foo="first"
		    result = foo();

		    request.foo="second"
		    result2 = foo();
		        """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "first" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "second" );
	}

	@DisplayName( "Self Referencing Argument Default" )
	@Test
	public void testSelfReferencingArgumentDefault() {

		instance.executeSource(
		    """
		       function foo(
		    	required param1,
		    	param2=arguments.param1,
		    	param3=param2
		    ) {
		       	return arguments.param3;
		       }

		       result = foo( param1="value yo!" );
		           """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( "value yo!" );
	}

	@DisplayName( "meta test" )
	@Test
	public void metaTest() {
		// @formatter:off
		instance.executeSource(
		    """
				/**
				* @brad wood
				* @param1.luis majano
				*/
		        function foo( param1 ) {}
		        println( foo.$bx.meta )
		      """,
		    context, BoxSourceType.CFSCRIPT );
		// @formatter:on
	}

	@DisplayName( "default null argument collection" )
	@Test
	public void testDefaultNullArgumentCollection() {

		instance.executeSource(
		    """
		    	function foo( param1, param2 ) {
		    	  bar( argumentCollection=arguments)
		    	}
		    	function bar( param1='default1', param2='default2' ) {
		    		variables.result = arguments
		    	}
		    	foo( 'actual value' )
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.getAsStruct( result ).getAsString( Key.of( "param1" ) ) ).isEqualTo( "actual value" );
		assertThat( variables.getAsStruct( result ).getAsString( Key.of( "param2" ) ) ).isEqualTo( "default2" );
	}

	@DisplayName( "default null argument collection" )
	@Test
	public void testDefaultNullArgumentExplicit() {

		instance.executeSource(
		    """
		    	function bar( param1='default1', param2='default2' ) {
		    		variables.result = arguments
		    	}
		    	bar( 'actual value', null )
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.getAsStruct( result ).getAsString( Key.of( "param1" ) ) ).isEqualTo( "actual value" );
		assertThat( variables.getAsStruct( result ).getAsString( Key.of( "param2" ) ) ).isEqualTo( "default2" );
	}

	@DisplayName( "default null argument explicit named" )
	@Test
	public void testDefaultNullArgumentExplicitName() {

		instance.executeSource(
		    """
		    	function bar( param1='default1', param2='default2' ) {
		    		variables.result = arguments
		    	}
		    	bar( param1='actual value', param2=null )
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.getAsStruct( result ).getAsString( Key.of( "param1" ) ) ).isEqualTo( "actual value" );
		assertThat( variables.getAsStruct( result ).getAsString( Key.of( "param2" ) ) ).isEqualTo( "default2" );
	}

	@Test
	public void testPackageAccess() {

		instance.executeSource(
		    """
		      	package function foo() {
		    }
		      """,
		    context, BoxSourceType.CFSCRIPT );
	}

	@Test
	public void testLeadingOES() {
		instance.executeSource(
		    """
		    function huh() {
		    	;	foo=bar;
		    	;;;; return "quirky!"
		    	}
		       """,
		    context );
	}

	@Test
	public void testExtraArgComma() {
		instance.executeSource(
		    """
		    function test3( param1, param2, ){}
		         """,
		    context, BoxSourceType.CFSCRIPT );
	}

	@Test
	public void testExtraArrayType() {
		instance.executeSource(
		    """
		    result = "";
		       function foo( struct[] param ){
		    	variables.result &= param[1]["id"];
		    	variables.result &= param[2]["id"];
		    }
		    foo( [ { "id": 1 }, { "id": 2 } ] )
		            """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "12" );
	}

	@Test
	public void testArgumentCollection2() {
		instance.executeSource(
		    """
		    key = ""
		    value = ""
		       function proxy() {
		       	method(argumentCollection = arguments);
		       }

		       function method(a,b) {
		       	key = arguments.keyList();
		       	value = arguments.valueArray().toList();
		       }

		       proxy('brad','luis','gavin');
		                 """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( Key.of( "key" ) ) ).isEqualTo( "a,b,3" );
		assertThat( variables.get( Key.of( "value" ) ) ).isEqualTo( "brad,luis,gavin" );
	}

	@Test
	public void testArgumentCollection3() {
		instance.executeSource(
		    """
		    key = ""
		    value = ""
		       function proxy(v) {
		       	method(argumentCollection = arguments);
		       }

		       function method(a,b) {
		       	key = arguments.keyList();
		       	value = arguments.valueArray().toList();
		       }

		       proxy('brad','luis','gavin');
		                 """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( Key.of( "key" ) ) ).isEqualTo( "a,b,v,3" );
		assertThat( variables.get( Key.of( "value" ) ) ).isEqualTo( ",luis,brad,gavin" );
	}

	@Test
	public void testArgumentCollection4() {
		instance.executeSource(
		    """
		    key = ""
		    value = ""
		    function outer(a, b) {
		    	inner(argumentCollection = arguments);
		    }

		    function inner(b) {
		       	key = arguments.keyList();
		       	value = arguments.valueArray().toList();
		    }

		    outer('hello', 'world');
		      """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( Key.of( "key" ) ) ).isEqualTo( "b,a" );
		assertThat( variables.get( Key.of( "value" ) ) ).isEqualTo( "world,hello" );
	}

	@Test
	public void testArgumentCollection5() {
		instance.executeSource(
		    """
		       key = ""
		       value = ""
		          function proxy(v) {
		          	method(argumentCollection = arguments);
		          }

		    function method(a,b,g,k) {
		          	key = arguments.keyList();
		          	value = arguments.valueArray().toList();
		          }

		          proxy('brad','luis','gavin');
		                    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( Key.of( "key" ) ) ).isEqualTo( "a,b,g,k,v" );
		assertThat( variables.get( Key.of( "value" ) ) ).isEqualTo( ",luis,gavin,,brad" );
	}

	@Test
	public void testArgumentCollection6() {
		instance.executeSource(
		    """
		    function foo(a,b,c) {
		    	variables.result = arguments;
		    }

		    foo(
		    	argumentCollection={
		    		a:'brad',
		    		b:'luis'
		    	},
		    	c='gavin'
		    );
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.getAsStruct( result ).get( "a" ) ).isEqualTo( "brad" );
		assertThat( variables.getAsStruct( result ).get( "b" ) ).isEqualTo( "luis" );
		assertThat( variables.getAsStruct( result ).get( "c" ) ).isEqualTo( "gavin" );
	}

	@Test
	public void testArgumentCollection7() {
		instance.executeSource(
		    """
		    function foo() {
		    	variables.result = arguments;
		    }

		    foo(
		    	argumentCollection={
		    		a:'brad',
		    		b:'luis'
		    	},
		    	c='gavin'
		    );
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.getAsStruct( result ).get( "a" ) ).isEqualTo( "brad" );
		assertThat( variables.getAsStruct( result ).get( "b" ) ).isEqualTo( "luis" );
		assertThat( variables.getAsStruct( result ).get( "c" ) ).isEqualTo( "gavin" );
	}

	@Test
	public void testArgumentCollection8() {
		instance.executeSource(
		    """
		    function test_a( string a) {
		    	variables.result = arguments;
		    }
		    function test_b(required b) {
		    	return test_a( argumentCollection = arguments, a = arguments.b );
		    }
		    test_b('hello world');
		       """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.getAsStruct( result ).get( "a" ) ).isEqualTo( "hello world" );
		assertThat( variables.getAsStruct( result ).get( "b" ) ).isEqualTo( "hello world" );
	}

	@Test
	public void testDefaultAssignmentScope() {
		instance.executeSource(
		    """
		     function foo() {
		     	test = "bar"
		     return local.test
		     }
		    result = foo()
		          """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
	}

	@Test
	public void testDefaultValueTypeCheck() {
		instance.executeSource(
		    """
		    import ortus.boxlang.runtime.scopes.Key;

		        function foo( string param1=Key.of("brad") ) {
		        return param1
		        }
		       result = foo(null)
		             """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "brad" );
		assertThat( variables.get( result ) ).isInstanceOf( String.class );
	}

	@Test
	public void testDefaultValueNew() {
		instance.executeSource(
		    """
		    	function foo( arg=new src.test.java.TestCases.phase3.MyClass() ) {
		    		return arg;
		    	}
		    	result = foo() instanceof "myclass"
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@Test
	public void testDefaultValueReference() {
		instance.executeSource(
		    """
		      	function foo( headers={}, flag=false ) {
		      		if( flag ) {
		      			headers["brad"] = "wood";
		      		}
		    		return headers;
		      	}
		      	foo(flag=true);
		    result = foo();
		      """,
		    context );
		assertThat( variables.getAsStruct( result ) ).isEmpty();

	}

	@Test
	public void testOverwriteArgumentsScope() {
		instance.executeSource(
		    """
		      function foo() {
		    // Keys in argument scope are replaced with contents of this struct
		      	arguments = { brad : 'wood' };
		      	return arguments;
		      }
		      result = foo( luis = 'majano' );
		      """,
		    context );
		assertThat( variables.getAsStruct( result ) ).containsKey( Key.of( "brad" ) );
		assertThat( variables.getAsStruct( result ) ).doesNotContainKey( Key.of( "luis" ) );
		assertThat( variables.getAsStruct( result ).get( Key.of( "brad" ) ) ).isEqualTo( "wood" );
	}

	@Test
	public void testOverwriteArgumentsScopeNonStruct() {
		instance.executeSource(
		    """
		      function foo() {
		    // Not assigning a struct, so we just set local.arguments as a normal variable
		      	arguments = "hello";
		          variables.localRef = local;
		      	return arguments;
		      }
		      result = foo( luis = 'majano' );
		      """,
		    context );
		assertThat( variables.getAsStruct( result ) ).containsKey( Key.of( "luis" ) );
		assertThat( variables.getAsStruct( result ) ).doesNotContainKey( Key.of( "brad" ) );
		assertThat( variables.getAsStruct( result ).get( Key.of( "luis" ) ) ).isEqualTo( "majano" );
		assertThat( variables.getAsStruct( Key.of( "localRef" ) ) ).containsKey( Key.of( "arguments" ) );
		assertThat( variables.getAsStruct( Key.of( "localRef" ) ).get( Key.of( "arguments" ) ) ).isEqualTo( "hello" );
	}

}
