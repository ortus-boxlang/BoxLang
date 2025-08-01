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
package TestCases.phase3;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.PrintStream;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.RequestScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.AbstractClassException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;
import ortus.boxlang.runtime.types.meta.ClassMeta;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class ClassTest {

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

	@DisplayName( "Test can create vanilla module config" )
	@Test
	void testVanillaModuleConfig() {

		IClassRunnable cfc = ( IClassRunnable ) DynamicObject.of( RunnableLoader.getInstance().loadClass(
		    """
		    /**
		     * This is the module descriptor and entry point for your module in the Runtime.
		     * The unique name of the moduel is the name of the directory on the modules folder.
		     * A BoxLang Mapping will be created for you with the name of the module.
		     *
		     * A Module can have the following folders that will be automatically registered:
		     * + bifs - Custom BIFs that will be registered into the runtime
		     * + interceptors - Custom Interceptors that will be registered into the runtime via the configure() method
		     * + libs - Custom Java libraries that your module leverages
		     * + tags - Custom tags that will be registered into the runtime
		     *
		     * Every Module will have it's own ClassLoader that will be used to load the module libs and dependencies.
		     */
		    component{

		    	/**
		    	 * --------------------------------------------------------------------------
		    	 * Module Properties
		    	 * --------------------------------------------------------------------------
		    	 * Here is where you define the properties of your module that the module service
		    	 * will use to register and activate your module
		    	 */

		    	/**
		    	 * Your module version. Try to use semantic versioning
		    	 * @mandatory
		    	 */
		    	this.version = "1.0.0";

		    	/**
		    	 * The BoxLang mapping for your module.  All BoxLang modules are registered with an internal
		    	 * mapping prefix of : bxModules.{this.mapping}, /bxmodules/{this.mapping}. Ex: bxModules.test, /bxmodules/test
		    	 */
		    	this.mapping = "test";

		    	/**
		    	 * Who built the module
		    	 */
		    	this.author = "Luis Majano";

		    	/**
		    	 * The module description
		    	 */
		    	this.description = "This module does amazing things";

		    	/**
		    	 * The module web URL
		    	 */
		    	this.webURL = "https://www.ortussolutions.com";

		    	/**
		    	 * This boolean flag tells the module service to skip the module registration/activation process.
		    	 */
		    	this.enabled = true;

		    	/**
		    	 * --------------------------------------------------------------------------
		    	 * Module Methods
		    	 * --------------------------------------------------------------------------
		    	 */

		    	/**
		    	 * Called by the ModuleService on module registration
		    	 *
		    	 * @moduleRecord - The module record registered in the ModuleService
		    	 * @runtime - The Runtime instance
		    	 */
		    	function configure( moduleRecord, runtime ){
		    		/**
		    		 * Every module has a settings configuration object
		    		 */
		    		settings = {
		    			loadedOn : now(),
		    			loadedBy : "Luis Majano"
		    		};

		    		/**
		    		 * The module interceptors to register into the runtime
		    		 */
		    		interceptors = [
		    			// { class="path.to.Interceptor", properties={} }
		    			{ class="bxModules.test.interceptors.Listener", properties={} }
		    		];

		    		/**
		    		 * A list of custom interception points to register into the runtime
		    		 */
		    		customInterceptionPoints = [ "onBxTestModule" ];
		    	}

		    	/**
		    	 * Called by the ModuleService on module activation
		    	 *
		    	 * @moduleRecord - The module record registered in the ModuleService
		    	 * @runtime - The Runtime instance
		    	 */
		    	function onLoad( moduleRecord, runtime ){

		    	}

		    	/**
		    	 * Called by the ModuleService on module deactivation
		    	 *
		    	 * @moduleRecord - The module record registered in the ModuleService
		    	 * @runtime - The Runtime instance
		    	 */
		    	function onUnload( moduleRecord, runtime ){

		    	}

		    	/**
		    	 * --------------------------------------------------------------------------
		    	 * Module Events
		    	 * --------------------------------------------------------------------------
		    	 * You can listen to any Runtime events by creating the methods
		    	 * that match the approved Runtime Interception Points
		    	 */
		    }

		      """, context, BoxSourceType.CFSCRIPT ) )
		    .invokeConstructor( context )
		    .getTargetInstance();
	}

	@DisplayName( "Test a basic boxlang class" )
	@Test
	public void testBasicBLClass() {

		// @formatter:off
		IClassRunnable	bxClass			= ( IClassRunnable ) DynamicObject.of( RunnableLoader.getInstance().loadClass(
		    """
				import foo;
				import java.lang.System;

				/**
				 * This is my class description
				 *
				 * @brad wood
				 * @luis
				 */
				@foo( bar )
				class accessors=true singleton gavin="pickin" inject {

					property numeric age default=1;
					property numeric test;
					property testAlone;

					variables.setup=true;
					System.out.println( "word" );
					request.foo="bar";
					println( request.asString())
					isInitted = false;
					println( "current template is " & getCurrentTemplatePath() );
					printLn( foo() )

					function init() {
						isInitted = true;
					}

					function foo() {
						return "I work! #bar()# #variables.setup# #setup# #request.foo# #isInitted#";
					}

					private function bar() {
						return "whee";
					}

					function getThis() {
						return this;
					}

					function runThisFoo() {
						return this.foo();
					}
				}
			""", context, BoxSourceType.BOXSCRIPT ) )
			.invokeConstructor( context )
			.getTargetInstance();
		// @formatter:on

		// Test shorthand properties work
		assertThat(
		    bxClass.dereferenceAndInvoke( context, Key.of( "getAge" ), new Object[] {}, false )
		).isEqualTo( 1 );
		assertThat(
		    bxClass.dereferenceAndInvoke( context, Key.of( "getTest" ), new Object[] {}, false )
		).isEqualTo( null );
		assertThat(
		    bxClass.dereferenceAndInvoke( context, Key.of( "getTestAlone" ), new Object[] {}, false )
		).isEqualTo( null );
		var			mdProperties	= bxClass.getMetaData().get( Key.of( "properties" ) );

		// execute public method
		Object		funcResult		= bxClass.dereferenceAndInvoke( context, Key.of( "foo" ), new Object[] {}, false );

		// private methods error
		Throwable	t				= assertThrows( BoxRuntimeException.class,
		    () -> bxClass.dereferenceAndInvoke( context, Key.of( "bar" ), new Object[] {}, false ) );
		assertThat( t.getMessage().contains( "bar" ) ).isTrue();

		// Can call public method that accesses private method, and variables, and request scope
		assertThat( funcResult ).isEqualTo( "I work! whee true true bar true" );
		assertThat( context.getScope( RequestScope.name ).get( Key.of( "foo" ) ) ).isEqualTo( "bar" );

		// This scope is reference to actual CFC instance
		funcResult = bxClass.dereferenceAndInvoke( context, Key.of( "getThis" ), new Object[] {}, false );
		assertThat( funcResult ).isEqualTo( bxClass );

		// Can call public methods on this
		funcResult = bxClass.dereferenceAndInvoke( context, Key.of( "runThisFoo" ), new Object[] {}, false );
		assertThat( funcResult ).isEqualTo( "I work! whee true true bar true" );
	}

	@DisplayName( "basic class" )
	@Test
	public void testBasicCFClass() {
		// @formatter:off
		IClassRunnable	cfc			= ( IClassRunnable ) DynamicObject.of( RunnableLoader.getInstance().loadClass(
		    """
				/**
				 * This is my class description
				 *
				 * @brad wood
				 * @luis
				 */
				component singleton gavin="pickin" inject foo="bar" {

					variables.setup=true;
					createObject('java','java.lang.System').out.println( "word" );
					request.foo="bar";
					println( request.asString())
					isInitted = false;
					println( "current template is " & getCurrentTemplatePath() );
					printLn( foo() )

					function init() {
						isInitted = true;
					}

					function foo() {
						return "I work! #bar()# #variables.setup# #setup# #request.foo# #isInitted#";
					}

					private function bar() {
						return "whee" ;
					}

					function getThis() {
						return this;
					}

					function runThisFoo() {
						return this.foo() ;
					}
				}


		    """, context, BoxSourceType.CFSCRIPT ) )
			.invokeConstructor( context )
			.getTargetInstance();
		// @formatter:on

		// execute public method
		Object			funcResult	= cfc.dereferenceAndInvoke( context, Key.of( "foo" ), new Object[] {}, false );

		// private methods error
		Throwable		t			= assertThrows( BoxRuntimeException.class,
		    () -> cfc.dereferenceAndInvoke( context, Key.of( "bar" ), new Object[] {}, false ) );
		assertThat( t.getMessage().contains( "bar" ) ).isTrue();

		// Can call public method that accesses private method, and variables, and request scope
		assertThat( funcResult ).isEqualTo( "I work! whee true true bar true" );
		assertThat( context.getScope( RequestScope.name ).get( Key.of( "foo" ) ) ).isEqualTo( "bar" );

		// This scope is reference to actual CFC instance
		funcResult = cfc.dereferenceAndInvoke( context, Key.of( "getThis" ), new Object[] {}, false );
		assertThat( funcResult ).isEqualTo( cfc );

		// Can call public methods on this
		funcResult = cfc.dereferenceAndInvoke( context, Key.of( "runThisFoo" ), new Object[] {}, false );
		assertThat( funcResult ).isEqualTo( "I work! whee true true bar true" );
	}

	@DisplayName( "basic class file" )
	@Test
	public void testBasicClassFile() {
		// @formatter:off
		instance.executeSource(
		    """
				cfc = new src.test.java.TestCases.phase3.MyClass();
				// execute public method
				result = cfc.foo();

				// private methods error
				try {
					cfc.bar()
					assert false;
				} catch( BoxRuntimeException e ) {
					assert e.message contains "bar";
				}

				// Can call public method that accesses private method, and variables, and request scope
				assert result == "I work! whee true true bar true";
				assert request.foo == "bar";

				// This scope is reference to actual CFC instance
				assert cfc.$bx.$class.getName() == cfc.getThis().$bx.$class.getName();

				// Can call public methods on this
				assert cfc.runThisFoo() == "I work! whee true true bar true";

				assert cfc.thisVar == "thisValue";
			""", context );
		// @formatter:on
	}

	@DisplayName( "parent super.init will preserve child variables scope" )
	@Test
	public void superInitTest() {
		// @formatter:off
		instance.executeSource(
		    """
				request.calls = [];
				settings = {
					"foo" : "bar"
				};
				cfc = new src.test.java.TestCases.phase3.Child( properties=settings );
				assert cfc.configure() == settings;

			""", context );
		// @formatter:on
	}

	@DisplayName( "basic class file via component path" )
	@Test
	public void testBasicClassFileViaComponentPath() {
		// @formatter:off
		instance.executeSource(
		    """
				newClassPaths = getApplicationMetadata().classPaths.append( expandPath( "/src/test/java/TestCases/phase3" ) );
				bx:application classPaths=newClassPaths;
				cfc = new MyClass();
				// execute public method
				result = cfc.foo();

			""", context );
		// @formatter:on
	}

	@DisplayName( "basic class file via component path subdir" )
	@Test
	public void testBasicClassFileViaComponentPathSubDir() {
		// @formatter:off
		instance.executeSource(
		    """
				newClassPaths = getApplicationMetadata().classPaths.append( expandPath( "/src/test/java/TestCases" ) );
				bx:application classPaths=newClassPaths;
				import phase3.MyClass as bradClass;
				cfc = new bradClass();
				// execute public method
				result = cfc.foo();

				assert result == "I work! whee true true bar true";
			""", context );
		// @formatter:on
	}

	@DisplayName( "legacy meta" )
	@Test
	public void testlegacyMeta() {

		instance.executeSource(
		    """
		    	cfc = new src.test.java.TestCases.phase3.MyClass();
		    """, context );

		var	cfc		= variables.getAsClassRunnable( Key.of( "cfc" ) );
		var	meta	= cfc.getMetaData();
		assertThat( meta.get( Key.of( "name" ) ) ).isEqualTo( "src.test.java.TestCases.phase3.MyClass" );
		assertThat( meta.get( Key.of( "type" ) ) ).isEqualTo( "Component" );
		assertThat( meta.get( Key.of( "fullname" ) ) ).isEqualTo( "src.test.java.TestCases.phase3.MyClass" );
		assertThat( meta.getAsString( Key.of( "path" ) ).contains( "MyClass.bx" ) ).isTrue();
		// assertThat( meta.get( Key.of( "hashcode" ) ) ).isEqualTo( cfc.hashCode() );
		assertThat( meta.get( Key.of( "properties" ) ) ).isInstanceOf( Array.class );
		assertThat( meta.getAsArray( Key.of( "properties" ) ) ).hasSize( 1 );
		Struct prop = ( Struct ) meta.getAsArray( Key.of( "properties" ) ).get( 0 );
		assertThat( prop ).doesNotContainKey( Key.of( "defaultValue" ) );

		assertThat( meta.get( Key.of( "functions" ) ) instanceof Array ).isTrue();
		assertThat( meta.getAsArray( Key.of( "functions" ) ).size() ).isEqualTo( 5 );
		assertThat( meta.get( Key.of( "extends" ) ) ).isNull();
		assertThat( meta.get( Key.of( "output" ) ) ).isEqualTo( false );
		assertThat( meta.get( Key.of( "persisent" ) ) ).isEqualTo( false );
		assertThat( meta.get( Key.of( "accessors" ) ) ).isEqualTo( true );
	}

	@DisplayName( "legacy meta CF" )
	@Test
	public void testlegacyMetaCF() {

		instance.executeSource(
		    """
		    	cfc = new src.test.java.TestCases.phase3.MyClassCF();
		    """, context );

		var	cfc		= variables.getAsClassRunnable( Key.of( "cfc" ) );
		var	meta	= cfc.getMetaData();
		assertThat( meta.get( Key.of( "name" ) ) ).isEqualTo( "src.test.java.TestCases.phase3.MyClassCF" );
		assertThat( meta.get( Key.of( "type" ) ) ).isEqualTo( "Component" );
		assertThat( meta.get( Key.of( "fullname" ) ) ).isEqualTo( "src.test.java.TestCases.phase3.MyClassCF" );
		assertThat( meta.getAsString( Key.of( "path" ) ).contains( "MyClassCF.cfc" ) ).isTrue();
		// assertThat( meta.get( Key.of( "hashcode" ) ) ).isEqualTo( cfc.hashCode() );
		assertThat( meta.get( Key.of( "properties" ) ) ).isInstanceOf( Array.class );
		assertThat( meta.get( Key.of( "functions" ) ) instanceof Array ).isTrue();
		assertThat( meta.getAsArray( Key.of( "functions" ) ).size() ).isEqualTo( 5 );
		assertThat( meta.get( Key.of( "extends" ) ) ).isNull();
		assertThat( meta.get( Key.of( "output" ) ) ).isEqualTo( true );
		assertThat( meta.get( Key.of( "persisent" ) ) ).isEqualTo( false );
		assertThat( meta.get( Key.of( "accessors" ) ) ).isEqualTo( false );
	}

	@DisplayName( "It should call onMissingMethod with pos args" )
	@Test
	public void testOnMissingMethodPos() {

		instance.executeSource(
		    """
		         	cfc = new src.test.java.TestCases.phase3.OnMissingMethod();
		       result = cfc.someFunc( "first", "second" );
		       result2 = cfc.variablesMissingCaller( "first", "second" );
		    result3 = cfc.headlessMissingCaller( "first", "second" );
		         """, context );

		String	res		= variables.getAsString( result );
		String	res2	= variables.getAsString( Key.of( "result2" ) );
		String	res3	= variables.getAsString( Key.of( "result3" ) );
		assertThat( res ).isEqualTo( "someFuncsecond" );
		assertThat( res2 ).isEqualTo( "doesNotExistsecond" );
		assertThat( res3 ).isEqualTo( "doesNotExistsecond" );
	}

	@DisplayName( "It should call onMissingMethod with named args" )
	@Test
	public void testOnMissingMethodNamed() {

		instance.executeSource(
		    """
		            	cfc = new src.test.java.TestCases.phase3.OnMissingMethod();
		          result = cfc.someFunc( foo="first", bar="second" );
		       result2 = cfc.variablesMissingCaller( foo="first", bar="second" );
		    result3 = cfc.headlessMissingCaller( foo="first", bar="second" );
		            """, context );

		String	res		= variables.getAsString( result );
		String	res2	= variables.getAsString( Key.of( "result2" ) );
		String	res3	= variables.getAsString( Key.of( "result3" ) );
		assertThat( res ).isEqualTo( "someFuncsecond" );
		assertThat( res2 ).isEqualTo( "doesNotExistsecond" );
		assertThat( res3 ).isEqualTo( "doesNotExistsecond" );
	}

	@DisplayName( "It should call onMissingMethod through invoke" )
	@Test
	public void testOnMissingMethodInvoke() {
		instance.executeSource(
		    """
		          cfc = new src.test.java.TestCases.phase3.OnMissingMethod();
		    result = invoke( cfc, "someFunc", [ "first", "second" ] );
		           """, context );

		String res = variables.getAsString( result );
		assertThat( res ).isEqualTo( "someFuncsecond" );
	}

	@DisplayName( "It should let you override java functions" )
	@Test
	public void testJavaOverride() {
		instance.executeSource(
		    """
		          clazz = new src.test.java.TestCases.phase3.JavaOverride();
		    result = clazz._invoke();
		           """, context );

		String res = variables.getAsString( result );
		assertThat( res ).isEqualTo( "From BoxLang" );
	}

	@DisplayName( "It should return null" )
	@Test
	public void testIncludeEvaluateReturnValue() {
		var test = instance.executeStatement(
		    """
		       include "/src/test/bx/includeTest.bxs";
		    """, context );

		assertThat( test ).isNull();
	}

	@DisplayName( "box meta" )
	@Test
	public void testBoxMeta() {

		instance.executeSource(
		    """
		    	cfc = new src.test.java.TestCases.phase3.MyClass();
		    """, context );

		var	cfc		= variables.getAsClassRunnable( Key.of( "cfc" ) );
		var	boxMeta	= ( ClassMeta ) cfc.getBoxMeta();
		var	meta	= boxMeta.meta;
		assertThat( meta.get( Key.of( "type" ) ) ).isEqualTo( "Class" );
		assertThat( meta.get( Key.of( "fullname" ) ) ).isEqualTo( "src.test.java.TestCases.phase3.MyClass" );
		assertThat( meta.getAsString( Key.of( "path" ) ).contains( "MyClass.bx" ) ).isTrue();
		assertThat( meta.get( Key.of( "hashcode" ) ) ).isEqualTo( cfc.hashCode() );
		assertThat( meta.get( Key.of( "properties" ) ) instanceof Array ).isTrue();
		Array properties = meta.getAsArray( Key.of( "properties" ) );
		assertThat( properties.size() ).isEqualTo( 1 );
		assertThat( properties.get( 0 ) instanceof IStruct ).isTrue();
		IStruct prop = ( IStruct ) properties.get( 0 );
		assertThat( prop.getAsStruct( Key.documentation ) ).containsKey( Key.of( "test" ) );
		assertThat( prop.getAsStruct( Key.documentation ).get( Key.of( "test" ) ) ).isEqualTo( "" );

		assertThat( meta.get( Key.of( "functions" ) ) instanceof Array ).isTrue();

		assertThat( meta.get( Key.of( "extends" ) ) instanceof IStruct ).isTrue();

		assertThat( meta.getAsArray( Key.of( "functions" ) ).size() ).isEqualTo( 5 );
		var fun1 = meta.getAsArray( Key.of( "functions" ) ).get( 0 );
		assertThat( fun1 ).isInstanceOf( Struct.class );
		assertThat( ( ( IStruct ) fun1 ).containsKey( Key.of( "name" ) ) ).isTrue();

		assertThat( meta.get( Key.of( "documentation" ) ) instanceof IStruct ).isTrue();
		var docs = meta.getAsStruct( Key.of( "documentation" ) );
		assertThat( docs.getAsString( Key.of( "brad" ) ).trim() ).isEqualTo( "wood" );
		assertThat( docs.get( Key.of( "luis" ) ) ).isEqualTo( "" );
		assertThat( docs.getAsString( Key.of( "_itwontlikeme~`!@#$%^&*()-=+[]{}\\|'\";:,.<>/?*áéíóúüñ" ) ).trim() ).isEqualTo( "formbuilderV2Form" );
		assertThat( docs.getAsString( Key.of( "hint" ) ).trim() ).isEqualTo( "This is my class description continued on this line \nand this one as well." );

		assertThat( meta.get( Key.of( "annotations" ) ) instanceof IStruct ).isTrue();
		var annos = meta.getAsStruct( Key.of( "annotations" ) );
		assertThat( annos.getAsString( Key.of( "foo" ) ).trim() ).isEqualTo( "bar" );
		assertThat( annos.getAsString( Key.of( "baz" ) ).trim() ).isEqualTo( "bum" );
		// assertThat( annos.getAsString( Key.of( "implements" ) ).trim() ).isEqualTo( "Luis,Jorge" );
		assertThat( annos.getAsString( Key.of( "singleton" ) ).trim() ).isEqualTo( "" );
		assertThat( annos.getAsString( Key.of( "gavin" ) ).trim() ).isEqualTo( "pickin" );
		assertThat( annos.getAsString( Key.of( "inject" ) ).trim() ).isEqualTo( "" );

	}

	@DisplayName( "properties" )
	@Test
	public void testProperties() {
		// @formatter:off
		instance.executeSource(
		    """
				cfc = new src.test.java.TestCases.phase3.PropertyTest();
				nameGet = cfc.getMyProperty();
				invalidSetErrored=false;
				try {
					// property is typed as string, an array should blow up
					setResult = cfc.setMyProperty( [] );
				} catch( any e ) {
					invalidSetErrored=true;
				}
				setResult = cfc.setMyProperty( "anotherValue" );
				nameGet2 = cfc.getMyProperty();
				test1 = cfc.getShortcutWithDefault()
				test2 = cfc.getTypedShortcutWithDefault()
				test3 = cfc.getChain()
				test4 = cfc.getName()
		    """, context );
		// @formatter:on

		var cfc = variables.getAsClassRunnable( Key.of( "cfc" ) );
		assertThat( variables.get( Key.of( "nameGet" ) ) ).isEqualTo( "myDefaultValue" );
		assertThat( variables.get( Key.of( "nameGet2" ) ) ).isEqualTo( "anotherValue" );
		assertThat( variables.get( Key.of( "setResult" ) ) ).isEqualTo( cfc );
		assertThat( variables.get( Key.of( "invalidSetErrored" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "test1" ) ) ).isEqualTo( "myDefaultValue" );
		assertThat( variables.get( Key.of( "test2" ) ) ).isEqualTo( "myDefaultValue2" );
		assertThat( variables.get( Key.of( "test3" ) ) ).isEqualTo( new Array() );
		assertThat( variables.get( Key.of( "test4" ) ) ).isNull();

		var	boxMeta	= ( ClassMeta ) cfc.getBoxMeta();
		var	meta	= boxMeta.meta;

		assertThat( meta.getAsArray( Key.of( "properties" ) ).size() ).isEqualTo( 7 );

		var prop1 = ( IStruct ) meta.getAsArray( Key.of( "properties" ) ).get( 0 );
		assertThat( prop1.get( "name" ) ).isEqualTo( "myProperty" );
		assertThat( prop1.get( "defaultValue" ) ).isEqualTo( "myDefaultValue" );
		assertThat( prop1.get( "type" ) ).isEqualTo( "string" );

		var prop1Annotations = prop1.getAsStruct( Key.of( "annotations" ) );
		assertThat( prop1Annotations.size() ).isEqualTo( 5 );

		assertThat( prop1Annotations.containsKey( Key.of( "preAnno" ) ) ).isTrue();
		assertThat( prop1Annotations.get( Key.of( "preAnno" ) ) ).isEqualTo( "" );

		assertThat( prop1Annotations.containsKey( Key.of( "inject" ) ) ).isTrue();
		assertThat( prop1Annotations.get( Key.of( "inject" ) ) ).isEqualTo( "" );

		var prop2 = ( IStruct ) meta.getAsArray( Key.of( "properties" ) ).get( 1 );
		assertThat( prop2.get( "name" ) ).isEqualTo( "anotherprop" );
		assertThat( prop2.get( "defaultValue" ) ).isEqualTo( null );
		assertThat( prop2.get( "type" ) ).isEqualTo( "string" );

		var prop2Annotations = prop2.getAsStruct( Key.of( "annotations" ) );
		assertThat( prop2Annotations.size() ).isEqualTo( 4 );

		assertThat( prop2Annotations.containsKey( Key.of( "preAnno" ) ) ).isTrue();
		assertThat( prop2Annotations.get( Key.of( "preAnno" ) ) instanceof Array ).isTrue();
		Array preAnno = prop2Annotations.getAsArray( Key.of( "preAnno" ) );
		assertThat( preAnno.size() ).isEqualTo( 2 );
		assertThat( preAnno.get( 0 ) ).isEqualTo( "myValue" );
		assertThat( preAnno.get( 1 ) ).isEqualTo( "anothervalue" );

		var prop3 = ( IStruct ) meta.getAsArray( Key.of( "properties" ) ).get( 2 );
		assertThat( prop3.get( "name" ) ).isEqualTo( "theName" );
		assertThat( prop3.get( "defaultValue" ) ).isEqualTo( null );
		assertThat( prop3.get( "type" ) ).isEqualTo( "any" );

		var prop3Annotations = prop3.getAsStruct( Key.of( "annotations" ) );
		assertThat( prop3Annotations.size() ).isEqualTo( 4 );
		assertThat( prop3Annotations.containsKey( Key.of( "ID" ) ) ).isTrue();
		assertThat( prop3Annotations.get( Key.of( "ID" ) ) ).isEqualTo( "" );

		var prop4 = ( IStruct ) meta.getAsArray( Key.of( "properties" ) ).get( 3 );
		assertThat( prop4.get( "name" ) ).isEqualTo( "name" );
		assertThat( prop4.get( "defaultValue" ) ).isEqualTo( null );
		assertThat( prop4.get( "type" ) ).isEqualTo( "string" );

		var prop2Docs = prop2.getAsStruct( Key.of( "documentation" ) );
		assertThat( prop2Docs.size() ).isEqualTo( 3 );
		assertThat( prop2Docs.getAsString( Key.of( "brad" ) ).trim() ).isEqualTo( "wood" );
		assertThat( prop2Docs.getAsString( Key.of( "luis" ) ).trim() ).isEqualTo( "" );
		assertThat( prop2Docs.getAsString( Key.of( "hint" ) ).trim() ).isEqualTo( "This is my property" );
	}

	@DisplayName( "properties" )
	@Test
	public void testPropertiesCF() {

		instance.executeSource(
		    """
		    cfc = new src.test.java.TestCases.phase3.PropertyTestCF();
		    nameGet = cfc.getMyProperty();
		    setResult = cfc.SetMyProperty( "anotherValue" );
		    nameGet2 = cfc.getMyProperty();
		    test1 = cfc.getShortcutWithDefault()
		    test2 = cfc.getTypedShortcutWithDefault()
		    test3 = cfc.getChain()
		           """, context );

		var cfc = variables.getAsClassRunnable( Key.of( "cfc" ) );

		assertThat( variables.get( Key.of( "nameGet" ) ) ).isEqualTo( "myDefaultValue" );
		assertThat( variables.get( Key.of( "nameGet2" ) ) ).isEqualTo( "anotherValue" );
		assertThat( variables.get( Key.of( "setResult" ) ) ).isEqualTo( cfc );
		assertThat( variables.get( Key.of( "test1" ) ) ).isEqualTo( "myDefaultValue" );
		assertThat( variables.get( Key.of( "test2" ) ) ).isEqualTo( "myDefaultValue2" );
		assertThat( variables.get( Key.of( "test3" ) ) ).isEqualTo( new Array() );

		var	boxMeta	= ( ClassMeta ) cfc.getBoxMeta();
		var	meta	= boxMeta.meta;

		assertThat( meta.getAsArray( Key.of( "properties" ) ).size() ).isEqualTo( 5 );

		var prop1 = ( IStruct ) meta.getAsArray( Key.of( "properties" ) ).get( 0 );
		assertThat( prop1.get( "name" ) ).isEqualTo( "myProperty" );
		assertThat( prop1.get( "defaultValue" ) ).isEqualTo( "myDefaultValue" );
		assertThat( prop1.get( "type" ) ).isEqualTo( "string" );

		var prop1Annotations = prop1.getAsStruct( Key.of( "annotations" ) );
		assertThat( prop1Annotations.size() ).isEqualTo( 5 );

		assertThat( prop1Annotations.containsKey( Key.of( "preAnno" ) ) ).isTrue();
		assertThat( prop1Annotations.get( Key.of( "preAnno" ) ) ).isEqualTo( "" );

		assertThat( prop1Annotations.containsKey( Key.of( "inject" ) ) ).isTrue();
		assertThat( prop1Annotations.get( Key.of( "inject" ) ) ).isEqualTo( "" );

		var prop2 = ( IStruct ) meta.getAsArray( Key.of( "properties" ) ).get( 1 );
		assertThat( prop2.get( "name" ) ).isEqualTo( "anotherprop" );
		assertThat( prop2.get( "defaultValue" ) ).isEqualTo( null );
		assertThat( prop2.get( "type" ) ).isEqualTo( "string" );

		var prop2Annotations = prop2.getAsStruct( Key.of( "annotations" ) );
		assertThat( prop2Annotations.size() ).isEqualTo( 6 );

		assertThat( prop2Annotations.containsKey( Key.of( "preAnno" ) ) ).isTrue();
		assertThat( prop2Annotations.get( Key.of( "preAnno" ) ) instanceof Array ).isTrue();
		Array preAnno = prop2Annotations.getAsArray( Key.of( "preAnno" ) );
		assertThat( preAnno.size() ).isEqualTo( 2 );
		assertThat( preAnno.get( 0 ) ).isEqualTo( "myValue" );
		assertThat( preAnno.get( 1 ) ).isEqualTo( "anothervalue" );

		var prop2Docs = prop2.getAsStruct( Key.of( "documentation" ) );
		assertThat( prop2Docs.size() ).isEqualTo( 3 );
		assertThat( prop2Docs.getAsString( Key.of( "brad" ) ).trim() ).isEqualTo( "wood" );
		assertThat( prop2Docs.getAsString( Key.of( "luis" ) ).trim() ).isEqualTo( "" );
		assertThat( prop2Docs.getAsString( Key.of( "hint" ) ).trim() ).isEqualTo( "This is my property" );

	}

	@DisplayName( "Implicit Constructor named" )
	@Test
	public void testImplicitConstructorNamed() {

		instance.executeSource(
		    """
		        	 cfc =  new src.test.java.TestCases.phase3.ImplicitConstructorTest( name="brad", age=43, favoriteColor="blue" );
		    name = cfc.getName();
		    age = cfc.getAge();
		    favoriteColor = cfc.getFavoriteColor();
		        """, context );

		assertThat( variables.get( Key.of( "name" ) ) ).isEqualTo( "brad" );
		assertThat( variables.get( Key.of( "age" ) ) ).isEqualTo( 43 );
		assertThat( variables.get( Key.of( "favoriteColor" ) ) ).isEqualTo( "blue" );

	}

	@DisplayName( "Implicit Constructor named argumentCollection" )
	@Test
	public void testImplicitConstructorNamedArgumentCollection() {

		instance.executeSource(
		    """
		        	 cfc =  new src.test.java.TestCases.phase3.ImplicitConstructorTest( argumentCollection={ name="brad", age=43, favoriteColor="blue" } );
		    name = cfc.getName();
		    age = cfc.getAge();
		    favoriteColor = cfc.getFavoriteColor();
		        """, context );

		assertThat( variables.get( Key.of( "name" ) ) ).isEqualTo( "brad" );
		assertThat( variables.get( Key.of( "age" ) ) ).isEqualTo( 43 );
		assertThat( variables.get( Key.of( "favoriteColor" ) ) ).isEqualTo( "blue" );

	}

	@DisplayName( "Implicit Constructor positional" )
	@Test
	public void testImplicitConstructorPositional() {

		instance.executeSource(
		    """
		        	cfc = new src.test.java.TestCases.phase3.ImplicitConstructorTest( {name="brad", age=43, favoriteColor="blue" });
		    name = cfc.getName();
		    age = cfc.getAge();
		    favoriteColor = cfc.getFavoriteColor();
		        """, context );

		assertThat( variables.get( Key.of( "name" ) ) ).isEqualTo( "brad" );
		assertThat( variables.get( Key.of( "age" ) ) ).isEqualTo( 43 );
		assertThat( variables.get( Key.of( "favoriteColor" ) ) ).isEqualTo( "blue" );

	}

	@DisplayName( "InitMethod Test" )
	@Test
	public void testInitMethod() {

		instance.executeSource(
		    """
		         	cfc = new src.test.java.TestCases.phase3.InitMethodTest( );

		    result = cfc.getInittedProperly();
		         """, context );

		assertThat( variables.get( Key.of( "result" ) ) ).isEqualTo( true );

	}

	@DisplayName( "PseudoConstructor can output" )
	@Test
	public void testPseudoConstructorOutput() {

		instance.executeSource(
		    """
		      	cfc = new src.test.java.TestCases.phase3.PseudoConstructorOutput();
		    result = getBoxContext().getBuffer().toString()

		      """, context );

		assertThat( variables.get( Key.of( "result" ) ) ).isEqualTo( "PseudoConstructorOutput" );

	}

	@DisplayName( "PseudoConstructor will not output" )
	@Test
	public void testPseudoConstructorNoOutput() {

		instance.executeSource(
		    """
		      	cfc = new src.test.java.TestCases.phase3.PseudoConstructorNoOutput();
		    result = getBoxContext().getBuffer().toString()

		      """, context );

		assertThat( variables.get( Key.of( "result" ) ) ).isEqualTo( "" );

	}

	@DisplayName( "can extend" )
	@Test
	public void testCanExtend() {

		instance.executeSource(
		    """
		    cfc = new src.test.java.TestCases.phase3.Chihuahua();
		    result = cfc.speak()
		    warm = cfc.isWarmBlooded()
		    name = cfc.getScientificName()
		    results = cfc.getResults()
		           """, context );

		// Polymorphism invokes overridden method
		assertThat( variables.get( Key.of( "result" ) ) ).isEqualTo( "Yip Yip!" );
		// inherited method from base class
		assertThat( variables.get( Key.of( "warm" ) ) ).isEqualTo( true );
		// Delegate to super.method() in parent class
		assertThat( variables.get( Key.of( "name" ) ) ).isEqualTo( "barkus annoyus Canis lupus Animal Kingdom" );

		// This array represents a specific order of operations that occur during the instantiation of our object hierachy
		// as well as specific values that need to be present to ensure correct behaviors
		assertThat( variables.getAsArray( Key.of( "results" ) ).toArray() ).isEqualTo( new Object[] {
		    // top most super class is instantiated first. getCurrentTemplate() shows that file
		    "animal pseudo Animal.cfc",
		    // Then the next super class is instantiated. getCurrentTemplate() shows that file
		    "Dog pseudo Dog.cfc",
		    // The variables scope in the Doc pseudo constructor is the "same" variables scope as the Animal pseudo constructor that ran before it
		    "dog sees variables.inAnimal as: true",
		    // And lastly, the concrete class is instantiated. getCurrentTemplate() shows that file
		    "Chihuahua pseudo Chihuahua.cfc",
		    // I'm calling super.init() first, so animal inits first. getCurrentTemplate() shows the current class. INCOMPAT WITH CF which returns concrete
		    // class!
		    "Animal init Animal.cfc",
		    // Then dog inits as we work backwards. getCurrentTemplate() shows the current class. INCOMPAT WITH CF which returns concrete class!
		    "Dog init Dog.cfc",
		    // Then the concrete class inits. getCurrentTemplate() shows the concrete class.
		    "Chihuahua init Chihuahua.cfc",
		    // A method inherited from a base class, sees "this" as the concrete class.
		    "animal this is: src.test.java.TestCases.phase3.Chihuahua",
		    // A method inherited from a base class, sees the top level "variables" scope.
		    "animal sees inDog as: true",
		    // A method delegated to as super.foo() sees "this" as the concrete class.
		    "super animal sees: src.test.java.TestCases.phase3.Chihuahua",
		    // A method delegated to as super.foo() sees the top level "variables" scope.
		    "super sees inDog as: true",
		} );

		var	cfc		= variables.getAsClassRunnable( Key.of( "cfc" ) );
		var	boxMeta	= ( ClassMeta ) cfc.getBoxMeta();
		var	meta	= boxMeta.meta;

		assertThat( meta.get( Key.of( "name" ) ) ).isEqualTo( "src.test.java.TestCases.phase3.Chihuahua" );

		IStruct extendsMeta = meta.getAsStruct( Key.of( "extends" ) );
		assertThat( extendsMeta.getAsString( Key.of( "name" ) ) ).endsWith( ".Dog" );

		extendsMeta = extendsMeta.getAsStruct( Key.of( "extends" ) );
		assertThat( extendsMeta.getAsString( Key.of( "name" ) ) ).endsWith( ".Animal" );

		extendsMeta = extendsMeta.getAsStruct( Key.of( "extends" ) );
		assertThat( extendsMeta ).hasSize( 0 );

	}

	@DisplayName( "class as struct" )
	@Test
	public void testClassAsStruct() {

		instance.executeSource(
		    """
		         	cfc = new src.test.java.TestCases.phase3.MyClass();
		       result = isStruct( cfc )
		    cfc.foo = "bar"
		    result2 = structGet( "cfc.foo")
		    keyArray = structKeyArray( cfc )

		         """, context );

		assertThat( variables.get( result ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "bar" );
		assertThat( variables.get( Key.of( "keyArray" ) ) ).isInstanceOf( Array.class );

	}

	@Test
	void testJavaMeta() {
		// @formatter:off
		instance.executeSource(
		    """
		        jClass = createObject( "java", "java.lang.System" )
		    	result = jClass.$bx.meta
				clazz = jClass.$bx.$class
		      	println( clazz )
		    """, context );
		// @formatter:on
		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		assertThat( variables.get( Key.of( "clazz" ) ) ).isEqualTo( System.class );
	}

	@Test
	public void testFunctionMeta() {
		// @formatter:off
		instance.executeSource(
		    """
		        cfc = new src.test.java.TestCases.phase3.FunctionMeta();
		    	result = cfc.$bx.meta
		      	println( result )
		    """, context );
		// @formatter:on
		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
	}

	@Test
	@Disabled
	public void testSuperHeadlessFunctionInvocationToChild() {

		instance.executeSource(
		    """
		    	request.calls = [];
		    	cfc = new src.test.java.TestCases.phase3.Child();
		    	result = request.calls;
		    """, context );

		assertThat( variables.getAsArray( result ) ).hasSize( 2 );
		assertThat( variables.getAsArray( result ).get( 0 ) ).isEqualTo( "running child setupFrameworkDefaults()" );
		assertThat( variables.getAsArray( result ).get( 1 ) ).isEqualTo( "running parent setupFrameworkDefaults()" );
	}

	@Test
	public void testClassWrappedInScriptIsland() {

		instance.executeSource(
		    """
		    	cfc = new src.test.java.TestCases.phase3.ClassWrappedInScript();
		    """, context );

	}

	@Test
	public void testClassIgnoreLeadingComment() {

		instance.executeSource(
		    """
		    	cfc = new src.test.java.TestCases.phase3.ClassLeadingComment();
		    """, context );

	}

	@Test
	public void testClassIgnoreTrailingComment() {

		instance.executeSource(
		    """
		    	cfc = new src.test.java.TestCases.phase3.ClassTrailingComment();
		    """, context );

	}

	@Test
	public void testCFImport() {

		instance.executeSource(
		    """
		    foo = new src.test.java.TestCases.phase3.CFImportTest();
		    foo.doSomething();
		       """, context );

	}

	@Test
	public void testCFImport2() {
		// This version quotes the class being imported
		instance.executeSource(
		    """
		    foo = new src.test.java.TestCases.phase3.CFImportTest2();
		    foo.doSomething();
		       """, context );

	}

	@Test
	public void testInlineJavaImplements() {
		instance.executeSource(
		    """
		    	import java:java.lang.Thread;
		    	jRunnable = new src.test.java.TestCases.phase3.JavaImplements();
		       assert jRunnable instanceof "java.lang.Runnable"
		    jThread = new java:Thread( jRunnable );
		    jThread.start();
		       """, context );

	}

	@Test
	public void testInlineJavaExtends() {
		instance.executeSource(
		    """
		    import java.util.Timer;
		      	myTask = new src.test.java.TestCases.phase3.JavaExtends();
		         assert myTask instanceof "java.util.TimerTask"

		      jtimer = new Timer();
		      jtimer.schedule(myTask, 1000);
		    myTask.cancel()
		         """, context );

	}

	@Test
	public void testInlineJavaExtendsField() {
		instance.executeSource(
		    """
		       	myContext = new src.test.java.TestCases.phase3.JavaExtends2();
		          assert myContext instanceof "ortus.boxlang.runtime.context.IBoxContext"

		    println( myContext.getTemplatesYo() )
		          """, context );

	}

	@Test
	public void testInlineJavaExtendsFieldPublic() {
		instance.executeSource(
		    """
		        myBIF = new src.test.java.TestCases.phase3.JavaExtends3();
		          assert myBIF instanceof "ortus.boxlang.runtime.bifs.BIF"
		       println( myBIF.__isMemberExecution )
		       println( myBIF.runtime )
		    myBIF.printStuff()
		          """, context );

	}

	@Test
	public void testGeneratedAccessors() {
		// @formatter:off
		instance.executeSource(
			"""
			cfc = new src.test.java.TestCases.phase3.GeneratedAccessor();
			""",
		context );
		IStruct cfc = (IStruct) variables.get( Key.of( "cfc" ) );
		assertThat( cfc.containsKey( Key.of( "getName" ) ) ).isEqualTo( true );
		assertThat( cfc.containsKey( Key.of( "setName" ) ) ).isEqualTo( true );
		assertThat( cfc.containsKey( Key.of( "getAge" ) ) ).isEqualTo( false );
		assertThat( cfc.containsKey( Key.of( "setAge" ) ) ).isEqualTo( true );
		assertThat( cfc.containsKey( Key.of( "getEmail" ) ) ).isEqualTo( true );
		assertThat( cfc.containsKey( Key.of( "setEmail" ) ) ).isEqualTo( false );
	}

	@Test
	public void testImplicitAccessor() {
		instance.executeSource(
		    """
		             clazz = new src.test.java.TestCases.phase3.ImplicitAccessor();
		             clazz.name="brad";
		       clazz.age=44;
		       name = clazz.name;
		       age = clazz.age;
		    methodsCalled = clazz.getMethodsCalled();
		               """, context );
		assertThat( variables.get( Key.of( "name" ) ) ).isEqualTo( "brad" );
		assertThat( variables.get( Key.of( "age" ) ) ).isEqualTo( 44 );
		assertThat( variables.get( Key.of( "methodsCalled" ) ) ).isEqualTo( "setNamesetAgegetNamegetAge" );
	}

	@Test
	public void testImplicitGeneratedAccessor() {
		instance.executeSource(
		    """
		             clazz = new src.test.java.TestCases.phase3.ImplicitGeneratedAccessor();
		             clazz.name="brad";
		       clazz.age=44;
		       name = clazz.name;
		       age = clazz.age;
		    // prove they're going in the variable scope, not this scope
		    keyExistsName = structKeyExists( clazz, "name")
		    keyExistsAge = structKeyExists( clazz, "age")
		               """, context );
		assertThat( variables.get( Key.of( "name" ) ) ).isEqualTo( "brad" );
		assertThat( variables.get( Key.of( "age" ) ) ).isEqualTo( 44 );
		assertThat( variables.get( Key.of( "keyExistsName" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "keyExistsAge" ) ) ).isEqualTo( false );
	}

	@Test
	public void testStaticInstance() {
		instance.executeSource(
		    """
		             clazz = new src.test.java.TestCases.phase3.StaticTestCF();
		    result1 = clazz.foo;
		    result2 = clazz.myStaticFunc();
		    result3 = clazz.myInstanceFunc();
		    result4 = clazz.scoped;
		    result5 = clazz.unscoped;
		    result6 = clazz.again;
		               """, context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( 42 );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "static42" );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( "instancestatic42" );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( "brad" );
		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( "wood" );
		assertThat( variables.get( Key.of( "result6" ) ) ).isEqualTo( "luis" );
	}

	@Test
	public void testStaticStatic() {
		instance.executeSource( """
		                        result1 = src.test.java.TestCases.phase3.StaticTest::foo;
		                        result2 = src.test.java.TestCases.phase3.StaticTest::myStaticFunc();
		                        result4 =  src.test.java.TestCases.phase3.StaticTest::scoped;
		                        result5 = src.test.java.TestCases.phase3.StaticTest::unscoped;
		                        result6 = src.test.java.TestCases.phase3.StaticTest::again;
		                        myStaticUDF   = src.test.java.TestCases.phase3.StaticTest::sayHello;
		                        result7   =  myStaticUDF();
		                        result8 = src.test.java.TestCases.phase3.StaticTest::123;
		                        result9 = src.test.java.TestCases.phase3.StaticTest2::getInstance().getStaticBrad();
		                        result10 = src.test.java.TestCases.phase3.StaticTest2::getInstance().thisStaticBrad;
								result11 = src.test.java.TestCases.phase3.StaticTest::finalStatic;
								result12 = src.test.java.TestCases.phase3.StaticTest::finalStatic2;
								result13 = src.test.java.TestCases.phase3.StaticTest::IAmStatic()
		                                                                                                                      """, context,
		    BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( 9000 );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "static9000" );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( "brad" );
		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( "wood" );
		assertThat( variables.get( Key.of( "result6" ) ) ).isEqualTo( "luis" );
		assertThat( variables.get( Key.of( "result7" ) ) ).isEqualTo( "Hello" );
		assertThat( variables.get( Key.of( "result8" ) ) ).isEqualTo( 456 );
		assertThat( variables.get( Key.of( "result9" ) ) ).isEqualTo( "wood" );
		assertThat( variables.get( Key.of( "result10" ) ) ).isEqualTo( "wood" );
		assertThat( variables.get( Key.of( "result11" ) ) ).isEqualTo( "finalStatic" );
		assertThat( variables.get( Key.of( "result12" ) ) ).isEqualTo( "finalStatic2" );
		assertThat( variables.get( Key.of( "result13" ) ) ).isEqualTo( "bradfinalStatic" );
	}

	@Test
	public void testStaticStaticFromScript() {
		instance.executeSource( """
		                        include "/src/test/java/TestCases/phase3/TestStaticFromScript.bxs";
		                        """, context,
		    BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( 9000 );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "static9000" );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( "brad" );
		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( "wood" );
		assertThat( variables.get( Key.of( "result6" ) ) ).isEqualTo( "luis" );
		assertThat( variables.get( Key.of( "result7" ) ) ).isEqualTo( "Hello" );
		assertThat( variables.get( Key.of( "result8" ) ) ).isEqualTo( 456 );
		assertThat( variables.get( Key.of( "result9" ) ) ).isEqualTo( "wood" );
		assertThat( variables.get( Key.of( "result10" ) ) ).isEqualTo( "wood" );
		assertThat( variables.get( Key.of( "result11" ) ) ).isEqualTo( "finalStatic" );
		assertThat( variables.get( Key.of( "result12" ) ) ).isEqualTo( "finalStatic2" );
	}

	@Test
	public void testStaticReferenceOnVariable() {
		instance.executeSource( """
			import src.test.java.TestCases.phase3.StaticTest;
			import src.test.java.TestCases.phase3.StaticTest2;
			// Unlike Java where imports are just symbols for the compiler, they are actual value objects we can pass around.
			myStaticVar = StaticTest;
			myStaticVar2 = StaticTest2;

			result1 = myStaticVar::foo;
			result2 = myStaticVar::myStaticFunc();
			result4 =  myStaticVar::scoped;
			result5 = myStaticVar::unscoped;
			result6 = myStaticVar::again;
			myStaticUDF   = myStaticVar::sayHello;
			result7   =  myStaticUDF();
			result8 = myStaticVar::123;
			result9 = myStaticVar2::getInstance().getStaticBrad();
			result10 = myStaticVar2::getInstance().thisStaticBrad;
			result11 = myStaticVar::finalStatic;
			result12 = myStaticVar::finalStatic2;

			javaStaticClassVar = createObject( 'java', 'java.lang.System' );
			result13 = javaStaticClassVar::out;

			result14 = java.lang.System::getProperty( 'java.version' );

			// create structs that mimic the path
			java.lang.System = createObject( 'java', 'java.lang.String' );
			// java.lang.System is a reference to our String class.
			result15 = java.lang.System::valueOf( true );
		            """, context,
		    BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( 9000 );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "static9000" );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( "brad" );
		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( "wood" );
		assertThat( variables.get( Key.of( "result6" ) ) ).isEqualTo( "luis" );
		assertThat( variables.get( Key.of( "result7" ) ) ).isEqualTo( "Hello" );
		assertThat( variables.get( Key.of( "result8" ) ) ).isEqualTo( 456 );
		assertThat( variables.get( Key.of( "result9" ) ) ).isEqualTo( "wood" );
		assertThat( variables.get( Key.of( "result10" ) ) ).isEqualTo( "wood" );
		assertThat( variables.get( Key.of( "result11" ) ) ).isEqualTo( "finalStatic" );
		assertThat( variables.get( Key.of( "result12" ) ) ).isEqualTo( "finalStatic2" );
		assertThat( variables.get( Key.of( "result13" ) ) ).isInstanceOf( PrintStream.class );
		assertThat( variables.get( Key.of( "result14" ) ) ).isEqualTo( System.getProperty( "java.version" ) );
		assertThat( variables.get( Key.of( "result15" ) ) ).isEqualTo( "true" );
	}

	@Test
	public void testStaticNestedMethod() {
		instance.executeSource(
		    """
		      	import java.lang.Thread;
		      	result = Thread::currentThread().getContextClassLoader();
		    // This is the same as above
		      	result2 = Thread.currentThread().getContextClassLoader();
		      """, context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( result ) ).isNotNull();
		assertThat( variables.get( result ) ).isInstanceOf( ClassLoader.class );
		assertThat( variables.get( Key.of( "result2" ) ) ).isNotNull();
		assertThat( variables.get( Key.of( "result2" ) ) ).isInstanceOf( ClassLoader.class );
	}

	@Test
	public void testStaticNestedProperty() {
		instance.executeSource(
		    """
		      	import java.lang.System;
		      	System::out.println( "testStaticNestedProperty test" );
		    // This is the same as above.
		      	System.out.println( "testStaticNestedProperty test2" );
		      """, context, BoxSourceType.BOXSCRIPT );
	}

	@Test
	public void testStaticInstanceCF() {
		instance.executeSource(
		    """
		    clazz = new src.test.java.TestCases.phase3.StaticTestCF();
		    result1 = clazz.foo;
		    result2 = clazz.myStaticFunc();
		    result3 = clazz.myInstanceFunc();
		    result4 = clazz.scoped;
		    result5 = clazz.unscoped;
		    result6 = clazz.again;
		    result7 = clazz.getStaticBrad();
		    clazz2 = new src.test.java.TestCases.phase3.StaticTestCF();
		    result8 = clazz2.getStaticBrad();
		                       """, context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( 42 );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "static42" );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( "instancestatic42" );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( "brad" );
		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( "wood" );
		assertThat( variables.get( Key.of( "result6" ) ) ).isEqualTo( "luis" );
		assertThat( variables.get( Key.of( "result7" ) ) ).isEqualTo( "wood" );
		assertThat( variables.get( Key.of( "result8" ) ) ).isEqualTo( "wood" );
	}

	@Test
	public void testStaticStaticCF() {
		instance.executeSource(
		    """
		    	result1 = src.test.java.TestCases.phase3.StaticTest::foo;
		    	result2 = src.test.java.TestCases.phase3.StaticTest::myStaticFunc();
		    	result4 = src.test.java.TestCases.phase3.StaticTest::scoped;
		    	result5 = src.test.java.TestCases.phase3.StaticTest::unscoped;
		    	result6 = src.test.java.TestCases.phase3.StaticTest::again;
		    	result7 = src.test.java.TestCases.phase3.StaticTest::123;
		    	result9 = src.test.java.TestCases.phase3.StaticTestCF2::getInstance().getStaticBrad();
		    	result10 = src.test.java.TestCases.phase3.StaticTestCF2::getInstance().thisStaticBrad;
				result11 = src.test.java.TestCases.phase3.StaticTest::myArray[ 3 ];
				result12 = src.test.java.TestCases.phase3.StaticTest::getMyArray()[ 3 ];
		    """, context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( 9000 );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "static9000" );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( "brad" );
		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( "wood" );
		assertThat( variables.get( Key.of( "result6" ) ) ).isEqualTo( "luis" );
		assertThat( variables.get( Key.of( "result7" ) ) ).isEqualTo( 456 );
		assertThat( variables.get( Key.of( "result9" ) ) ).isEqualTo( "wood" );
		assertThat( variables.get( Key.of( "result10" ) ) ).isEqualTo( "wood" );
		assertThat( variables.get( Key.of( "result11" ) ) ).isEqualTo( 3 );
		assertThat( variables.get( Key.of( "result12" ) ) ).isEqualTo( 3 );
	}

	@Test
	public void testStaticImport() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.StaticTest;
		    import src.test.java.TestCases.phase3.StaticTestCF2;

			result1 = StaticTest::foo;
			result2 = StaticTest::myStaticFunc();
			result4 = StaticTest::scoped;
			result5 = StaticTest::unscoped;
			result6 = StaticTest::again;
			result7 = StaticTest::123;
			result9 = StaticTestCF2::getInstance().getStaticBrad();
			result10 = StaticTestCF2::getInstance().thisStaticBrad;
			result11 = StaticTest::myArray[ 3 ];
			result12 = StaticTest::getMyArray()[ 3 ];
		                  """, context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( 9000 );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "static9000" );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( "brad" );
		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( "wood" );
		assertThat( variables.get( Key.of( "result6" ) ) ).isEqualTo( "luis" );
		assertThat( variables.get( Key.of( "result7" ) ) ).isEqualTo( 456 );
		assertThat( variables.get( Key.of( "result9" ) ) ).isEqualTo( "wood" );
		assertThat( variables.get( Key.of( "result10" ) ) ).isEqualTo( "wood" );
		assertThat( variables.get( Key.of( "result11" ) ) ).isEqualTo( 3 );
		assertThat( variables.get( Key.of( "result12" ) ) ).isEqualTo( 3 );
	}

	@Test
	public void testStaticImportDot() {
		instance.executeSource(
		    """
		          import src.test.java.TestCases.phase3.StaticTest;

		             result1 = StaticTest.foo;
		             result2 = StaticTest.myStaticFunc();
		             result4 = StaticTest.scoped;
		             result5 = StaticTest.unscoped;
		             result6 = StaticTest.again;
		          // instance
		          myInstance = new StaticTest();
		          result7 = myInstance.foo;
		          result8 = StaticTest.foo;
		          result9 = myInstance.myInstanceFunc2()
		       result10 = myInstance.getStaticBrad()
		    myInstance2 = new StaticTest();
		    result11 = myInstance2.getStaticBrad()

		                        """, context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( 9000 );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "static9000" );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( "brad" );
		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( "wood" );
		assertThat( variables.get( Key.of( "result6" ) ) ).isEqualTo( "luis" );
		assertThat( variables.get( Key.of( "result7" ) ) ).isEqualTo( 42 );
		assertThat( variables.get( Key.of( "result8" ) ) ).isEqualTo( 42 );
		assertThat( variables.get( Key.of( "result9" ) ) ).isInstanceOf( Array.class );
		Array result9 = variables.getAsArray( Key.of( "result9" ) );
		assertThat( result9.size() ).isEqualTo( 3 );
		assertThat( result9.get( 0 ) ).isEqualTo( "brad" );
		assertThat( result9.get( 1 ) ).isEqualTo( "wood" );
		assertThat( result9.get( 2 ) ).isEqualTo( 42 );
		assertThat( variables.get( Key.of( "result10" ) ) ).isEqualTo( "wood" );
		assertThat( variables.get( Key.of( "result11" ) ) ).isEqualTo( "wood" );
	}

	@Test
	public void testStaticArgDefaultTest() {
		instance.executeSource(
		    """
		    foo = src.test.java.TestCases.phase3.StaticArgDefaultTest::create("hello");
		      """, context, BoxSourceType.CFSCRIPT );
	}

	@Test
	public void testDotExtends() {
		instance.executeSource(
		    """
		       clazz = new src.test.java.TestCases.phase3.DotExtends();
		    result = clazz.childUDF()
		         """, context );
		assertThat( variables.get( result ) ).isEqualTo( "childUDFparent" );
	}

	@Test
	public void testRelativeInstantiation() {
		// @formatter:off
		instance.executeSource(
		    """
		    	clazz = new src.test.java.TestCases.phase3.RelativeInstantiation();
				result = clazz.findSibling()
				result2 = getMetadata( clazz.returnSibling() )
		    """, context );
		// @formatter:on
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
		assertThat( variables.getAsStruct( Key.of( "result2" ) ).getAsString( Key._NAME ) ).isEqualTo( "src.test.java.TestCases.phase3.FindMe" );
	}

	// @Disabled( "Brad Fix this pretty please!!" )
	@Test
	public void testRelativeInstantiationWithLoop() {
		// @formatter:off
		instance.executeSource(
		    """
		    	manager = new src.test.java.TestCases.phase3.RelativeManager()
				data = [1,2,3,4,5]
				result = manager.allApply( data )
		    """, context );
		// @formatter:on
		Array target = variables.getAsArray( result );
		System.out.println( target );
	}

	@Test
	public void testRelativeSelfInstantiation() {
		// @formatter:off
		instance.executeSource(
		    """
		        clazz = new src.test.java.TestCases.phase3.RelativeSelfInstantiation();
		   		result2 = getMetadata( clazz.clone() )
		    """, context );
		// @formatter:on
		assertThat( variables.getAsStruct( Key.of( "result2" ) ).getAsString( Key._NAME ) )
		    .isEqualTo( "src.test.java.TestCases.phase3.RelativeSelfInstantiation" );
	}

	@Test
	public void testRelativeSelfInstantiationx() {
		// @formatter:off
		instance.executeSource(
		    """
		        include template="src/test/java/TestCases/phase3/scriptIncludeASMIssue.cfm";
				                                      
		    """, context );
		// @formatter:on
	}

	@Test
	public void testAbstractClass() {
		Throwable t = assertThrows( AbstractClassException.class, () -> instance.executeSource(
		    """
		    clazz = new src.test.java.TestCases.phase3.AbstractClass();
		      """, context ) );
		assertThat( t.getMessage() ).contains( "Cannot instantiate an abstract class" );

		instance.executeSource(
		    """
		       clazz = new src.test.java.TestCases.phase3.ConcreteClass();
		    result1 = clazz.normal()
		    result2 = clazz.abstractMethod()
		       """, context );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( "normal" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "abstractMethod" );
	}

	@Test
	public void testAbstractClassMissingMethod() {
		Throwable t = assertThrows( AbstractClassException.class, () -> instance.executeSource(
		    """
		    clazz = new src.test.java.TestCases.phase3.ConcreteClassNoMethod();
		      """, context ) );
		assertThat( t.getMessage() ).contains( "does not implement method" );
	}

	@Test
	public void testAbstractClassWrongMethod() {
		Throwable t = assertThrows( AbstractClassException.class, () -> instance.executeSource(
		    """
		    clazz = new src.test.java.TestCases.phase3.ConcreteClassWrongMethod();
		      """, context ) );
		assertThat( t.getMessage() ).contains( "the signature doesn't match the signature of" );
	}

	@Test
	public void testAbstractClassCF() {
		Throwable t = assertThrows( AbstractClassException.class, () -> instance.executeSource(
		    """
		    clazz = new src.test.java.TestCases.phase3.AbstractClassCF();
		      """, context ) );
		assertThat( t.getMessage() ).contains( "Cannot instantiate an abstract class" );

		instance.executeSource(
		    """
		       clazz = new src.test.java.TestCases.phase3.ConcreteClassCF();
		    result1 = clazz.normal()
		    result2 = clazz.abstractMethod()
		       """, context );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( "normal" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "abstractMethod" );
	}

	@Test
	public void testCFGetterType() {
		instance.executeSource(
		    """
		       clazz = new src.test.java.TestCases.phase3.GetterTest();
		    result = clazz.getMyDate()
		       """, context );
		assertThat( variables.get( result ) ).isEqualTo( "" );
	}

	@Test
	public void testGetterOverrideInParent() {
		instance.executeSource(
		    """
		       clazz = new src.test.java.TestCases.phase3.GeneratedGetterChild();
		    result = clazz.getFoo()
		       """, context );
		assertThat( variables.get( result ) ).isEqualTo( "overriden" );
	}

	@Test
	public void testFinalClass() {
		instance.executeSource(
		    """
		    clazz = new src.test.java.TestCases.phase3.FinalClass();
		    """, context );
		Throwable t = assertThrows( BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        clazz = new src.test.java.TestCases.phase3.IllegalFinalExtends();
		        """, context ) );
		assertThat( t.getMessage() ).contains( "Cannot extend final class" );
	}

	@Test
	public void testPseduoConstructorError() {
		Array tagContext = null;
		try {
			instance.executeSource(
			    """
			    clazz = new src.test.java.TestCases.phase3.PseudoConstructorError();
			    """, context );
		} catch ( BoxRuntimeException e ) {
			tagContext = ExceptionUtil.buildTagContext( e );
			System.out.println( tagContext );
		}
		assertThat( tagContext ).isNotNull();
		assertThat( tagContext.size() ).isEqualTo( 1 );
		assertThat( ( ( IStruct ) tagContext.get( 0 ) ).getAsString( Key.template ) ).contains( "PseudoConstructorError.bx" );
	}

	@Test
	public void testBadProperty() {
		instance.executeSource(
		    """
		    clazz = new src.test.java.TestCases.phase3.BadProperty();
		    """, context );
	}

	@Test
	public void testCaseMisMatch() {
		instance.executeSource(
		    """
		    cfc = new src.TEST.java.testcases.phase3.mYcLASS();
		      """, context );
	}

	@Test
	public void testMethodPropConflict() {
		instance.executeSource(
		    """
		    	cfc = new src.test.java.TestCases.phase3.MethodPropConflict();
		    	result = cfc.spyFoo();
		    	result2 = cfc.spyBaz();
		    """, context );
		// propery value wins in variables scope of same name
		assertThat( variables.get( result ) ).isEqualTo( "bar" );
		// But variable set in super class's psedu constructor will get hammered by the concrete class's method
		assertThat( variables.get( "result2" ) ).isInstanceOf( Function.class );
		assertThat( ( ( Function ) variables.get( "result2" ) ).getName().getName() ).isEqualTo( "baz" );
	}

	@Test
	public void testBinderProperties() {
		instance.executeSource(
		    """
		      	cfc = new src.test.java.TestCases.phase3.ConcreteBinder("my injector");
		    properties = cfc.getProperties();
		    currentMapping = cfc.getCurrentMapping();
		      """, context );
		assertThat( variables.get( "properties" ) ).isInstanceOf( IStruct.class );
		assertThat( variables.get( "currentMapping" ) ).isInstanceOf( Array.class );
	}

	@Test
	public void testHypenInPath() {
		instance.executeSource(
		    """
		      	cfc = new "src.test.java.TestCases.phase3.sub-folder.Funky-Class"();
		    meta = cfc.$bx.meta;
		      """, context );
		assertThat( variables.get( "meta" ) ).isInstanceOf( IStruct.class );
		assertThat( variables.getAsStruct( Key.of( "meta" ) ).getAsString( Key.fullname ) )
		    .isEqualTo( "src.test.java.TestCases.phase3.sub-folder.Funky-Class" );
	}

	@Test
	public void testColdBoxRenderer() {
		instance.executeSource(
		    """
		    	cfc = new src.test.java.TestCases.phase3.MyRenderer();
		    """, context );
	}

	@DisplayName( "bx: resolver prefix on new" )
	@Test
	public void testBXResolverPrefixNew() {

		instance.executeSource(
		    """
		       cfc = new bx:src.test.java.TestCases.phase3.PropertyTestCF();
		    """,
		    context );
	}

	@DisplayName( "bx: resolver prefix on import" )
	@Test
	public void testBXResolverPrefixImport() {

		instance.executeSource(
		    """
		    import bx:src.test.java.TestCases.phase3.PropertyTestCF as brad;
		    new brad()
		      """,
		    context );
	}

	@DisplayName( "udf class has enclosing class reference" )
	@Test
	public void testUDFClassEnclosingClassReference() {

		instance.executeSource(
		    """
		       import bx:src.test.java.TestCases.phase3.PropertyTestCF as brad;
		       b = new brad()
		    outerClass = b.$bx.$class;
		    innerClass = b.init.getClass();
		    innerClassesOuterClass = b.init.getClass().getEnclosingClass();
		    println(outerclass)
		    println(innerClass)
		         """,
		    context );
		assertThat( ( ( Class<?> ) variables.get( "outerClass" ) ).getName() )
		    .isEqualTo( "boxgenerated.boxclass.src.test.java.testcases.phase3.Propertytestcf$cfc" );
		assertThat( ( ( Class<?> ) variables.get( "innerClassesOuterClass" ) ).getName() )
		    .isEqualTo( "boxgenerated.boxclass.src.test.java.testcases.phase3.Propertytestcf$cfc" );
		assertThat( ( ( Class<?> ) variables.get( "innerClass" ) ).getName() )
		    .isEqualTo( "boxgenerated.boxclass.src.test.java.testcases.phase3.Propertytestcf$cfc$Func_init" );
		assertThat( variables.get( "outerClass" ) ).isEqualTo( variables.get( "innerClassesOuterClass" ) );
	}

	@DisplayName( "udf class has enclosing class reference" )
	@Test
	public void testUDFClassEnclosingClassReferenceInTemplate() {

		instance.executeSource(
		    """
		    function test(){

		     }
		         result = test.getClass().getEnclosingClass();
		                """,
		    context );

		assertThat( variables.get( result ) ).isNotNull();
	}

	@DisplayName( "mixins should be public" )
	@Test
	public void testMixinsPublic() {

		instance.executeSource(
		    """
		       mt = new src.test.java.TestCases.phase3.MixinTest()
		    mt.includeMixin();
		    result = mt.mixed()
		         """,
		    context );
		assertThat( variables.get( "result" ) ).isEqualTo( "mixed up" );
	}

	@DisplayName( "class locator usage in static initializer" )
	@Test
	public void testClassLocatorInStaticInitializer() {
		instance.executeSource(
		    """
		    new src.test.java.TestCases.phase3.ClassLocatorInStaticInitializer()
		      """,
		    context );
	}

	@DisplayName( "Class with include" )
	@Test
	public void testClassWithInclude() {
		instance.executeSource(
		    """
		    new src.test.java.TestCases.phase3.ClassWithInclude()
		      """,
		    context );
	}

	@DisplayName( "properties not inherited in metadata" )
	@Test
	public void testPropertiesNotInheritedInMetadata() {
		instance.executeSource(
		    """
		    c = new src.test.java.TestCases.phase3.PropertiesNotInheritedInMetadata();
		       result = getMetaData( c )
		    result2 = c.$bx.meta
		         """,
		    context );

		// Legacy metadata
		Array childProps = variables.getAsStruct( Key.of( "result" ) ).getAsArray( Key.properties );
		assertThat( childProps ).hasSize( 2 );
		assertThat( ( ( IStruct ) childProps.get( 0 ) ).get( Key._NAME ) ).isEqualTo( "childProperty" );
		assertThat( ( ( IStruct ) childProps.get( 1 ) ).get( Key._NAME ) ).isEqualTo( "sharedProperty" );

		Array parentProps = variables.getAsStruct( Key.of( "result" ) ).getAsStruct( Key._EXTENDS ).getAsArray( Key.properties );
		assertThat( parentProps ).hasSize( 2 );
		assertThat( ( ( IStruct ) parentProps.get( 0 ) ).get( Key._NAME ) ).isEqualTo( "parentProperty" );
		assertThat( ( ( IStruct ) parentProps.get( 1 ) ).get( Key._NAME ) ).isEqualTo( "sharedProperty" );

		// $bx metadata
		childProps = variables.getAsStruct( Key.of( Key.of( "result2" ) ) ).getAsArray( Key.properties );
		assertThat( childProps ).hasSize( 2 );
		assertThat( ( ( IStruct ) childProps.get( 0 ) ).get( Key._NAME ) ).isEqualTo( "childProperty" );
		assertThat( ( ( IStruct ) childProps.get( 1 ) ).get( Key._NAME ) ).isEqualTo( "sharedProperty" );

		parentProps = variables.getAsStruct( Key.of( Key.of( "result2" ) ) ).getAsStruct( Key._EXTENDS ).getAsArray( Key.properties );
		assertThat( parentProps ).hasSize( 2 );
		assertThat( ( ( IStruct ) parentProps.get( 0 ) ).get( Key._NAME ) ).isEqualTo( "parentProperty" );
		assertThat( ( ( IStruct ) parentProps.get( 1 ) ).get( Key._NAME ) ).isEqualTo( "sharedProperty" );

	}

	@Test
	public void testUserASMError() {
		instance.executeSource(
		    """

		    new src.test.java.TestCases.phase3.ASMError()
		      """,
		    context );
	}

	@Test
	public void testUserStaticError() {
		instance.executeSource(
		    """
		    new src.test.java.TestCases.phase3.StaticError()
		      """,
		    context );
	}

	@Test
	public void testTrustedCache() {

		BoxRuntime instance = BoxRuntime.getInstance( true );
		instance.getConfiguration().trustedCache = true;
		IBoxContext	context			= new ScriptingRequestBoxContext( instance.getRuntimeContext() );

		//@formatter:off
		String		executionSource			= """
			request.calls = [];
			cfc = new src.test.java.TestCases.phase3.Child();
		""";
		String touchSource = "bx:execute variable=\"execResult\" name=\"touch\" arguments=\"#expandPath( '/src/test/java/TestCases/phase3/Child.cfc' )#\";";
		if( FileSystemUtil.IS_WINDOWS ){
			touchSource = "bx:execute variable=\"execResult\" name=\"powershell\" arguments=\"(ls #expandPath( '/src/test/java/TestCases/phase3/Child.cfc' )#).LastWriteTime = Get-Date\";";
		}
		//@formatter:on
		instance.executeSource(
		    touchSource,
		    context );
		long startTime = System.nanoTime();
		instance.executeSource(
		    executionSource,
		    context );
		long firstRunTime = System.nanoTime() - startTime;
		// System.out.println( "First Trusted Cache run time: " + firstRunTime );
		instance.executeSource(
		    touchSource,
		    context );
		startTime = System.nanoTime();
		instance.executeSource(
		    executionSource,
		    context );

		long secondRunTime = System.nanoTime() - startTime;
		instance.getConfiguration().trustedCache = false;
		// System.out.println( "Second Trusted Cache run time: " + secondRunTime );

		// The second run should be less than 1.5% of the first run
		assertThat( DoubleCaster.cast( secondRunTime / firstRunTime ) ).isLessThan( .015 );
	}

	@Test
	public void testOuputInApplication() {
		instance.executeSource(
		    """
		    bx:savecontent variable="result" {
		       	new src.test.java.TestCases.phase3.Application().run()
		    }

		    bx:savecontent variable="result2" {
		       	new src.test.java.TestCases.phase3.NotApplication().run()
		    }
		         """,
		    context );
		assertThat( variables.get( "result" ) ).isEqualTo( "Hello BradHello World" );
		assertThat( variables.get( "result2" ) ).isEqualTo( "" );
	}

	@Test
	public void testCFCNameSameAsType() {
		instance.executeSource(
		    """
		       function expectsAString( email address ) {
		    	return address.ucase()
		    }

		       function expectsAClass( email address ) {
		    	return address.getHost()
		    }

		    result = expectsAString( "brad@bradwood.com" );
		    result2 = expectsAClass( new src.test.java.TestCases.phase3.Email( email : "brad@bradwood.com" ) );
		            """,
		    context );
		assertThat( variables.get( "result" ) ).isEqualTo( "BRAD@BRADWOOD.COM" );
		assertThat( variables.get( "result2" ) ).isEqualTo( "bradwood.com" );
	}

	@Test
	public void testInvokeImplicitAccessors() {
		instance.executeSource(
		    """
		       iia = new src.test.java.TestCases.phase3.InvokeImplicitAccessors()
		    result0 = iia.name
		       iia.name = "brad"
		       result = iia.name;
		       iia.age = 21;
		       result2 = iia.age;
		    result2b = iia.supervisor
		    iia.supervisor = "luis"
		    result3 = iia.supervisor
		                   """,
		    context );
		assertThat( variables.get( "result0" ) ).isEqualTo( "default name" );
		assertThat( variables.get( "result" ) ).isEqualTo( "brad" );
		assertThat( variables.get( "result2" ) ).isEqualTo( 21 );
		assertThat( variables.get( "result2b" ) ).isEqualTo( "noj" );
		assertThat( variables.get( "result3" ) ).isEqualTo( "siul" );
	}

	@Test
	public void testInvokeImplicitAccessorsCF() {
		instance.executeSource(
		    """
		       iia = new src.test.java.TestCases.phase3.InvokeImplicitAccessorsCF()
		       iia.name = "brad"
		       result = iia.name;
		       iia.age = 21;
		       result2 = iia.age;
		    iia.supervisor = "luis"
		    result3 = iia.supervisor
		                   """,
		    context );
		assertThat( variables.get( "result" ) ).isEqualTo( "brad" );
		assertThat( variables.get( "result2" ) ).isEqualTo( 21 );
		assertThat( variables.get( "result3" ) ).isEqualTo( "luis" );
	}

	@Test
	public void testSemiAfteAnnotation() {
		instance.executeSource(
		    """
		    new src.test.java.TestCases.phase3.SemiAfteAnnotation()
		                """,
		    context );
	}

	@Test
	public void testMissingSuperScope() {
		instance.executeSource(
		    """
		    result = new src.test.java.TestCases.phase3.MissingSuperScopeChild().run()
		                """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "super" );
	}

	@Test
	public void testClassWithAtSignInName() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.foo@bar;
		    import src.test.java.TestCases.phase3.foo@bar as foobar;

		    new "src.test.java.TestCases.phase3.foo@bar"()
		    new src.test.java.TestCases.phase3.foo@bar()
		    new foo@bar()
		    new foobar()
		                           """,
		    context );
	}

	@Test
	public void testCurrentTemplateInStaticInitializer() {
		instance.executeSource(
		    """
		       import src.test.java.TestCases.phase3.CurrentTemplateInStaticInitializer;
		    result = CurrentTemplateInStaticInitializer.staticMethod();
		       """,
		    context );
		assertThat( variables.get( "result" ) )
		    .isEqualTo( Paths.get( "src/test/java/TestCases/phase3/CurrentTemplateInStaticInitializer.bx" ).toAbsolutePath().toString() );
	}

	@Test
	public void testStaticInitializerAboveProperties() {
		instance.executeSource(
		    """

		    result = new src.test.java.TestCases.phase3.StaticInitializerAboveProperties();
		       """,
		    context );
	}

	@Test
	public void testUDFVariableVisibility() {
		instance.executeSource(
		    """
		    clazz = new src.test.java.TestCases.phase3.UDFVariableVisibility();

		    result1 = clazz.test();
		    result2 = clazz.test2();
		    result3 = clazz.test3();
		    result4 = clazz.test4();
		    result6 = clazz.test6();

		    bx:savecontent variable="result5" {
		    	result5 = clazz.outer();
		    }
		         """,
		    context );
		assertThat( variables.get( "result1" ) ).isEqualTo( "class brad" );
		assertThat( variables.get( "result2" ) ).isEqualTo( "class brad" );
		assertThat( variables.get( "result3" ) ).isEqualTo( "function brad" );
		assertThat( variables.get( "result4" ) ).isEqualTo( "class brad" );
		assertThat( variables.get( "result6" ) ).isEqualTo( "N/A" );
		assertThat( variables.get( "result5" ) ).isEqualTo( "Outer function startInner functionOuter function end" );
	}

	@Test
	public void testStaticInitCallStaticMethod() {
		instance.executeSource(
		    """
		    result = new src.test.java.TestCases.phase3.StaticInitCallStaticMethod().someStruct.foo;
		    """,
		    context );
		assertThat( variables.get( "result" ) ).isEqualTo( "bar" );
	}

}
