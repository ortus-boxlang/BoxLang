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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.RequestScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.meta.ClassMeta;

public class ClassTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	VariablesScope		variables;
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
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= ( VariablesScope ) context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "basic class" )
	@Test
	public void testBasicClass() {

		IClassRunnable	cfc			= ( IClassRunnable ) DynamicObject.of( RunnableLoader.getInstance().loadClass(
		    """
		                  import foo;
		         import java.lang.System;

		               /**
		                * This is my class description
		                *
		                * @brad wood
		                * @luis
		                */
		                  @foo "bar"
		                  component  implements="Luis,Jorge" singleton gavin="pickin" inject {
		                  	variables.setup=true;
		    	System.out.println( "word" );
		    	request.foo="bar";
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


		                    """, context ) ).invokeConstructor( context ).getTargetInstance();

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

		instance.executeStatement(
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
		                    		                  """, context );

	}

	@DisplayName( "legacy meta" )
	@Test
	public void testlegacyMeta() {

		instance.executeStatement(
		    """
		    	cfc = new src.test.java.TestCases.phase3.MyClass();
		    """, context );

		var	cfc		= variables.getClassRunnable( Key.of( "cfc" ) );
		var	meta	= cfc.getMetaData();
		assertThat( meta.get( Key.of( "name" ) ) ).isEqualTo( "src.test.java.TestCases.phase3.MyClass" );
		assertThat( meta.get( Key.of( "type" ) ) ).isEqualTo( "Component" );
		assertThat( meta.get( Key.of( "fullname" ) ) ).isEqualTo( "src.test.java.TestCases.phase3.MyClass" );
		assertThat( meta.getAsString( Key.of( "path" ) ).contains( "MyClass.cfc" ) ).isTrue();
		assertThat( meta.get( Key.of( "hashcode" ) ) ).isEqualTo( cfc.hashCode() );
		assertThat( meta.get( Key.of( "properties" ) ) ).isInstanceOf( Array.class );
		assertThat( meta.get( Key.of( "functions" ) ) instanceof Array ).isTrue();
		assertThat( meta.getAsArray( Key.of( "functions" ) ).size() ).isEqualTo( 4 );
		assertThat( meta.get( Key.of( "extends" ) ) instanceof IStruct ).isTrue();
		assertThat( meta.get( Key.of( "output" ) ) ).isEqualTo( false );
		assertThat( meta.get( Key.of( "persisent" ) ) ).isEqualTo( false );
		assertThat( meta.get( Key.of( "accessors" ) ) ).isEqualTo( false );
	}

	@DisplayName( "It should call onMissingMethod" )
	@Test
	public void testOnMissingMethod() {

		instance.executeStatement(
		    """
		      	cfc = new src.test.java.TestCases.phase3.OnMissingMethod();
		    result = cfc.someFunc();
		      """, context );

		String res = variables.getAsString( result );
		assertThat( res ).isEqualTo( "someFunc" );
	}

	@DisplayName( "box meta" )
	@Test
	public void testBoxMeta() {

		instance.executeStatement(
		    """
		    	cfc = new src.test.java.TestCases.phase3.MyClass();
		    """, context );

		var	cfc		= variables.getClassRunnable( Key.of( "cfc" ) );
		var	boxMeta	= ( ClassMeta ) cfc.getBoxMeta();
		var	meta	= boxMeta.meta;
		assertThat( meta.get( Key.of( "type" ) ) ).isEqualTo( "Component" );
		assertThat( meta.get( Key.of( "fullname" ) ) ).isEqualTo( "src.test.java.TestCases.phase3.MyClass" );
		assertThat( meta.getAsString( Key.of( "path" ) ).contains( "MyClass.cfc" ) ).isTrue();
		assertThat( meta.get( Key.of( "hashcode" ) ) ).isEqualTo( cfc.hashCode() );
		assertThat( meta.get( Key.of( "properties" ) ) instanceof Array ).isTrue();
		assertThat( meta.get( Key.of( "functions" ) ) instanceof Array ).isTrue();

		assertThat( meta.get( Key.of( "extends" ) ) instanceof IStruct ).isTrue();

		assertThat( meta.getAsArray( Key.of( "functions" ) ).size() ).isEqualTo( 4 );
		var fun1 = meta.getAsArray( Key.of( "functions" ) ).get( 0 );
		assertThat( fun1 ).isInstanceOf( Struct.class );
		assertThat( ( ( IStruct ) fun1 ).containsKey( Key.of( "name" ) ) ).isTrue();

		assertThat( meta.get( Key.of( "documentation" ) ) instanceof IStruct ).isTrue();
		var docs = meta.getAsStruct( Key.of( "documentation" ) );
		assertThat( docs.getAsString( Key.of( "brad" ) ).trim() ).isEqualTo( "wood" );
		assertThat( docs.get( Key.of( "luis" ) ) ).isEqualTo( "" );
		assertThat( docs.getAsString( Key.of( "hint" ) ).trim() ).isEqualTo( "This is my class description" );

		assertThat( meta.get( Key.of( "annotations" ) ) instanceof IStruct ).isTrue();
		var annos = meta.getAsStruct( Key.of( "annotations" ) );
		assertThat( annos.getAsString( Key.of( "foo" ) ).trim() ).isEqualTo( "bar" );
		assertThat( annos.getAsString( Key.of( "implements" ) ).trim() ).isEqualTo( "Luis,Jorge" );
		assertThat( annos.getAsString( Key.of( "singleton" ) ).trim() ).isEqualTo( "" );
		assertThat( annos.getAsString( Key.of( "gavin" ) ).trim() ).isEqualTo( "pickin" );
		assertThat( annos.getAsString( Key.of( "inject" ) ).trim() ).isEqualTo( "" );

	}

	@DisplayName( "properties" )
	@Test
	public void testProperties() {

		instance.executeStatement(
		    """
		      	cfc = new src.test.java.TestCases.phase3.PropertyTest();
		    nameGet = cfc.getMyProperty();
		    setResult = cfc.SetMyProperty( "anotherValue" );
		    nameGet2 = cfc.getMyProperty();
		      """, context );

		var cfc = variables.getClassRunnable( Key.of( "cfc" ) );

		assertThat( variables.get( Key.of( "nameGet" ) ) ).isEqualTo( "myDefaultValue" );
		assertThat( variables.get( Key.of( "nameGet2" ) ) ).isEqualTo( "anotherValue" );
		assertThat( variables.get( Key.of( "setResult" ) ) ).isEqualTo( cfc );

		var	boxMeta	= ( ClassMeta ) cfc.getBoxMeta();
		var	meta	= boxMeta.meta;

		assertThat( meta.getAsArray( Key.of( "properties" ) ).size() ).isEqualTo( 2 );

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

		var prop2Docs = prop2.getAsStruct( Key.of( "documentation" ) );
		assertThat( prop2Docs.size() ).isEqualTo( 3 );
		assertThat( prop2Docs.getAsString( Key.of( "brad" ) ).trim() ).isEqualTo( "wood" );
		assertThat( prop2Docs.getAsString( Key.of( "luis" ) ).trim() ).isEqualTo( "" );
		assertThat( prop2Docs.getAsString( Key.of( "hint" ) ).trim() ).isEqualTo( "This is my property" );

	}

	@DisplayName( "Implicit Constructor named" )
	@Test
	public void testImplicitConstructorNamed() {

		instance.executeStatement(
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

	@DisplayName( "Implicit Constructor positional" )
	@Test
	public void testImplicitConstructorPositional() {

		instance.executeStatement(
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

		instance.executeStatement(
		    """
		         	cfc = new src.test.java.TestCases.phase3.InitMethodTest( );

		    result = cfc.getInittedProperly();
		         """, context );

		assertThat( variables.get( Key.of( "result" ) ) ).isEqualTo( true );

	}

	@DisplayName( "PseudoConstructor can output" )
	@Test
	public void testPseudoConstructorOutput() {

		instance.executeStatement(
		    """
		      	cfc = new src.test.java.TestCases.phase3.PseudoConstructorOutput();
		    result = getBoxContext().getBuffer().toString()

		      """, context );

		assertThat( variables.get( Key.of( "result" ) ) ).isEqualTo( "PseudoConstructorOutput" );

	}

	@DisplayName( "PseudoConstructor will not output" )
	@Test
	public void testPseudoConstructorNoOutput() {

		instance.executeStatement(
		    """
		      	cfc = new src.test.java.TestCases.phase3.PseudoConstructorNoOutput();
		    result = getBoxContext().getBuffer().toString()

		      """, context );

		assertThat( variables.get( Key.of( "result" ) ) ).isEqualTo( "" );

	}

	@DisplayName( "can extend" )
	@Test
	public void testCanExtend() {

		instance.executeStatement(
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
		    // I'm calling super.init() first, so animal inits first. getCurrentTemplate() shows the concrete class.
		    "Animal init Chihuahua.cfc",
		    // Then dog inits as we work backwards. getCurrentTemplate() shows the concrete class.
		    "Dog init Chihuahua.cfc",
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

		var	cfc		= variables.getClassRunnable( Key.of( "cfc" ) );
		var	boxMeta	= ( ClassMeta ) cfc.getBoxMeta();
		var	meta	= boxMeta.meta;

		assertThat( meta.get( Key.of( "name" ) ) ).isEqualTo( "src.test.java.TestCases.phase3.Chihuahua" );

		IStruct extendsMeta = meta.getAsStruct( Key.of( "extends" ) );
		assertThat( extendsMeta.get( Key.of( "name" ) ) ).isEqualTo( "Dog" );

		extendsMeta = extendsMeta.getAsStruct( Key.of( "extends" ) );
		assertThat( extendsMeta.get( Key.of( "name" ) ) ).isEqualTo( "Animal" );

		extendsMeta = extendsMeta.getAsStruct( Key.of( "extends" ) );
		assertThat( extendsMeta ).hasSize( 0 );

	}

}