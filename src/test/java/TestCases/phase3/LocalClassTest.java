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
package TestCases.phase3;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;

/**
 * Integration tests for named local class definitions inside BoxLang scripts and templates.
 *
 * A local class ({@code class Foo {}}) is defined inline within a .bxs script or .bxm template and is
 * only available within that compilation unit. It is hoisted to the top so it can be used anywhere in
 * the script even before its textual definition.
 */
public class LocalClassTest {

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

	@DisplayName( "Local class can be defined and instantiated in a script" )
	@Test
	public void testDefineAndInstantiate() {
		instance.executeSource(
		    """
		    class Person {
		        function getName() {
		            return "Brad";
		        }
		    }
		    result = new Person().getName();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Brad" );
	}

	@DisplayName( "Local class instance is IClassRunnable" )
	@Test
	public void testInstanceIsIClassRunnable() {
		instance.executeSource(
		    """
		    class Animal {
		        function speak() {
		            return "...";
		        }
		    }
		    result = new Animal();
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( IClassRunnable.class );
	}

	@DisplayName( "Local class with properties" )
	@Test
	public void testLocalClassWithProperties() {
		instance.executeSource(
		    """
		    class Counter {
		        property numeric count default=0;

		        function increment() {
		            variables.count++;
		        }

		        function getCount() {
		            return variables.count;
		        }
		    }

		    c = new Counter();
		    c.increment();
		    c.increment();
		    c.increment();
		    result = c.getCount();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 3 );
	}

	@DisplayName( "Local class hoisting: new before definition" )
	@Test
	public void testHoisting() {
		// The class is defined textually after the new expression, but it must still be usable
		// because local classes are pre-compiled before _invoke body execution.
		instance.executeSource(
		    """
		    result = new Greeter().greet( "World" );

		    class Greeter {
		        function greet( name ) {
		            return "Hello, " & name & "!";
		        }
		    }
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Hello, World!" );
	}

	@DisplayName( "Multiple local classes in the same script" )
	@Test
	public void testMultipleLocalClasses() {
		instance.executeSource(
		    """
		      class Adder {
		          function add( a, b ) {
		              return a + b;
		          }
		      }

		      class Multiplier {
		          function multiply( a, b ) {
		              return a * b;
		          }
		      }

		    myAdder      = new Adder();
		     myMultiplier = new Multiplier();
		     result       = myMultiplier.multiply( myAdder.add( 2, 3 ), 4 );
		      """,
		    context );
		assertThat( ( ( Number ) variables.get( result ) ).intValue() ).isEqualTo( 20 );
	}

	@DisplayName( "Local class with init function" )
	@Test
	public void testLocalClassWithInit() {
		instance.executeSource(
		    """
		    class Box {
		        function init( value ) {
		            variables.value = value;
		            return this;
		        }

		        function getValue() {
		            return variables.value;
		        }
		    }

		    result = new Box( 42 ).getValue();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 42 );
	}

	@DisplayName( "Local class in a BoxTemplate (bxm) script island" )
	@Test
	public void testLocalClassInTemplate() {
		instance.executeSource(
		    """
		    <bx:script>
		        class Point {
		            function init( x, y ) {
		                variables.x = x;
		                variables.y = y;
		                return this;
		            }
		            function toString() {
		                return "(" & variables.x & "," & variables.y & ")";
		            }
		        }
		        result = new Point( 3, 4 ).toString();
		    </bx:script>
		    """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.get( result ) ).isEqualTo( "(3,4)" );
	}

	@DisplayName( "Local class can use imports from the enclosing script" )
	@Test
	public void testLocalClassCanUseEnclosingImports() {
		// @formatter:off
		instance.executeSource(
		    """
		    import java.util.Date;
			class Event {
		        function init( name ) {
		            variables.name = name;
		            variables.timestamp = new Date();
		            return this;
		        }
		        function getInfo() {
		            return variables.name & " at " & variables.timestamp.toString();
		        }
		    }
			result = new Event( "Party" ).getInfo();
		    """,
			context
		);
		// @formatter:on
		String info = ( String ) variables.get( result );
		assertThat( info ).startsWith( "Party at " );
	}

	@DisplayName( "Local class can extend another local class" )
	@Test
	public void testLocalClassExtendsAnotherLocalClass() {
		// @formatter:off
		instance.executeSource(
			"""
				class Animal {
					function speak() {
						return "...";
					}
				}

				class Dog extends="Animal" {
					function speak() {
						return "Woof!";
					}
				}

				result = new Dog().speak();
			""",
			context
		);
		// @formatter:on
		assertThat( variables.get( result ) ).isEqualTo( "Woof!" );
	}

	@DisplayName( "Local class can extend a top-level BoxClass" )
	@Test
	@Disabled
	public void testLocalClassExtendsBoxClass() {
		// @formatter:off
		instance.executeSource(
			"""
				test = new src.test.bx.SimpleUser()
				println( test.toJson() )

				class CoolUser extends="src.test.bx.SimpleUser" {

					property age;

					function init( name="", email="", isActive=true, age=0 ) {
						super.init()
						variables.age = age;
						return this;
					}
				}

				result = new CoolUser( "luis", "lmajano@lmajano.com", true, 30 ).getName()
			""",
			context
		);
		// @formatter:on
		assertThat( variables.get( result ) ).isEqualTo( "luis" );
	}

	@DisplayName( "Local class with static variables" )
	@Test
	public void testLocalClassStaticVariables() {
		instance.executeSource(
		    """
		    class Config {
		        static {
		            static.MAX_RETRIES = 5;
		            static.APP_NAME = "MyApp";
		        }
		    }

		    result = Config::MAX_RETRIES;
		    result2 = Config::APP_NAME;
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 5 );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "MyApp" );
	}

	@DisplayName( "Local class with static methods" )
	@Test
	public void testLocalClassStaticMethods() {
		instance.executeSource(
		    """
		    class MathUtil {
		        static function add( a, b ) {
		            return a + b;
		        }
		        static function multiply( a, b ) {
		            return a * b;
		        }
		    }

		    result = MathUtil::add( 3, 4 );
		    result2 = MathUtil::multiply( 5, 6 );
		    """,
		    context );
		assertThat( ( ( Number ) variables.get( result ) ).intValue() ).isEqualTo( 7 );
		assertThat( ( ( Number ) variables.get( Key.of( "result2" ) ) ).intValue() ).isEqualTo( 30 );
	}

	@DisplayName( "Final local class cannot be extended" )
	@Test
	public void testFinalLocalClass() {
		instance.executeSource(
		    """
		    final class Immutable {
		        function getValue() {
		            return "fixed";
		        }
		    }

		    result = new Immutable().getValue();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "fixed" );
	}

	@DisplayName( "Extending a final local class throws an error" )
	@Test
	public void testExtendingFinalLocalClassThrows() {
		assertThrows( Exception.class, () -> {
			instance.executeSource(
			    """
			    final class Base {
			        function getValue() {
			            return "base";
			        }
			    }

			    class Child extends="Base" {
			        function getValue() {
			            return "child";
			        }
			    }

			    result = new Child().getValue();
			    """,
			    context );
		} );
	}

	@DisplayName( "Abstract local class cannot be instantiated directly" )
	@Test
	public void testAbstractLocalClassCannotInstantiate() {
		assertThrows( Exception.class, () -> {
			instance.executeSource(
			    """
			    @abstract class Shape {
			        abstract function area();
			    }

			    result = new Shape();
			    """,
			    context );
		} );
	}

	@DisplayName( "Abstract local class can be extended and instantiated" )
	@Test
	public void testAbstractLocalClassExtended() {
		instance.executeSource(
		    """
		    @abstract class Shape {
		        function describe() {
		            return "I am a shape";
		        }
		    }

		    class Circle extends="Shape" {
		        function init( radius ) {
		            variables.radius = radius;
		            return this;
		        }
		        function getRadius() {
		            return variables.radius;
		        }
		    }

		    myCircle = new Circle( 5 );
		    result = myCircle.describe();
		    result2 = myCircle.getRadius();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "I am a shape" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( 5 );
	}

	@DisplayName( "Local class extends another local class with super call" )
	@Test
	public void testLocalClassExtendsWithSuper() {
		instance.executeSource(
		    """
		    class Vehicle {
		        function init( make ) {
		            variables.make = make;
		            return this;
		        }
		        function getMake() {
		            return variables.make;
		        }
		    }

		    class Car extends="Vehicle" {
		        function init( make, model ) {
		            super.init( make );
		            variables.model = model;
		            return this;
		        }
		        function getInfo() {
		            return this.getMake() & " " & variables.model;
		        }
		    }

		    result = new Car( "Toyota", "Camry" ).getInfo();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Toyota Camry" );
	}

	@DisplayName( "Local class extends another local class - multi-level inheritance" )
	@Test
	public void testMultiLevelInheritance() {
		instance.executeSource(
		    """
		    class A {
		        function getValue() {
		            return "A";
		        }
		    }

		    class B extends="A" {
		        function getValueB() {
		            return this.getValue() & "B";
		        }
		    }

		    class C extends="B" {
		        function getValueC() {
		            return this.getValueB() & "C";
		        }
		    }

		    result = new C().getValueC();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "ABC" );
	}

	@DisplayName( "Local class metadata via getMetadata()" )
	@Test
	public void testLocalClassGetMetadata() {
		instance.executeSource(
		    """
		    class Person {
		        property name="firstName" default="John";
		        property name="lastName" default="Doe";

		        function fullName() {
		            return this.getFirstName() & " " & this.getLastName();
		        }
		    }

		    result = getMetaData( new Person() );
		    """,
		    context );
		Object res = variables.get( result );
		assertThat( res ).isInstanceOf( IStruct.class );
		IStruct meta = ( IStruct ) res;
		assertThat( meta.getAsString( Key.of( "name" ) ) ).contains( "Person" );
		assertThat( meta.get( Key.of( "type" ) ) ).isEqualTo( "Class" );
		assertThat( meta.get( Key.of( "functions" ) ) ).isNotNull();
		assertThat( meta.get( Key.of( "properties" ) ) ).isNotNull();
		assertThat( meta.getAsArray( Key.of( "properties" ) ) ).hasSize( 2 );
	}

	@DisplayName( "Local class implements a Java interface" )
	@Test
	public void testLocalClassImplementsJavaInterface() {
		instance.executeSource(
		    """
		    import java:java.lang.Thread;

		    class MyRunnable implements="java:java.lang.Runnable" {
		        property name="didRun" default=false;

		        @overrideJava
		        void function run() {
		            variables.didRun = true;
		        }
		    }

		    r = new MyRunnable();
		    assert r instanceof "java.lang.Runnable";
		    jThread = new java:Thread( r );
		    jThread.start();
		    jThread.join();
		    result = r.getDidRun();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "Local class extends a Java class" )
	@Test
	public void testLocalClassExtendsJavaClass() {
		instance.executeSource(
		    """
		    class MyTask extends="java:java.util.TimerTask" {

		        @overrideJava
		        void function run() {
		            println( "Hello from local TimerTask!" );
		        }
		    }

		    task = new MyTask();
		    assert task instanceof "java.util.TimerTask";
		    result = true;
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "Local class implements Comparable Java interface" )
	@Test
	public void testLocalClassImplementsComparable() {
		instance.executeSource(
		    """
		    class Ranked implements="java:java.lang.Comparable" {
		        property name="rank" default=0;

		        function init( rank ) {
		            variables.rank = rank;
		            return this;
		        }

		        @overrideJava
		        int function compareTo( other ) {
		            return variables.rank - other.getRank();
		        }
		    }

		    a = new Ranked( 3 );
		    b = new Ranked( 7 );
		    result = a.compareTo( b );
		    """,
		    context );
		assertThat( ( ( Number ) variables.get( result ) ).intValue() ).isLessThan( 0 );
	}

	@DisplayName( "Local class metadata via $bx.meta" )
	@Test
	public void testLocalClassDollarBxMeta() {
		instance.executeSource(
		    """
		    class Animal {
		        property name="species" default="Unknown";

		        function speak() {
		            return "...";
		        }
		    }

		    inst = new Animal();
		    result = inst.$bx.meta;
		    """,
		    context );
		Object res = variables.get( result );
		assertThat( res ).isInstanceOf( IStruct.class );
		IStruct meta = ( IStruct ) res;
		assertThat( meta.getAsString( Key.of( "name" ) ) ).contains( "Animal" );
		assertThat( meta.get( Key.of( "type" ) ) ).isEqualTo( "Class" );
		assertThat( meta.get( Key.of( "functions" ) ) ).isNotNull();
		assertThat( meta.get( Key.of( "properties" ) ) ).isNotNull();
		assertThat( meta.getAsArray( Key.of( "properties" ) ) ).hasSize( 1 );
	}

	@DisplayName( "getClassMetadata() with local class name" )
	@Test
	public void testGetClassMetadataByName() {
		instance.executeSource(
		    """
		    class Widget {
		        property name="label" default="default";

		        function getLabel() {
		            return variables.label;
		        }
		    }

		    result = getClassMetadata( "Widget" );
		    """,
		    context );
		Object res = variables.get( result );
		assertThat( res ).isInstanceOf( IStruct.class );
		IStruct meta = ( IStruct ) res;
		assertThat( meta.getAsString( Key.of( "name" ) ) ).contains( "Widget" );
		assertThat( meta.get( Key.of( "type" ) ) ).isEqualTo( "Class" );
		assertThat( meta.getAsArray( Key.of( "properties" ) ) ).hasSize( 1 );
		assertThat( meta.get( Key.of( "functions" ) ) ).isNotNull();
	}

	@DisplayName( "getClassMetadata() with multiple local classes by name" )
	@Test
	public void testGetClassMetadataMultipleLocalClasses() {
		instance.executeSource(
		    """
		    class Alpha {
		        property name="x" default=1;
		        function getX() { return variables.x; }
		    }

		    class Beta {
		        property name="y" default=2;
		        property name="z" default=3;
		        function getY() { return variables.y; }
		    }

		    result1 = getClassMetadata( "Alpha" );
		    result2 = getClassMetadata( "Beta" );
		    """,
		    context );
		IStruct meta1 = ( IStruct ) variables.get( Key.of( "result1" ) );
		assertThat( meta1.getAsString( Key.of( "name" ) ) ).contains( "Alpha" );
		assertThat( meta1.getAsArray( Key.of( "properties" ) ) ).hasSize( 1 );

		IStruct meta2 = ( IStruct ) variables.get( Key.of( "result2" ) );
		assertThat( meta2.getAsString( Key.of( "name" ) ) ).contains( "Beta" );
		assertThat( meta2.getAsArray( Key.of( "properties" ) ) ).hasSize( 2 );
	}

	@DisplayName( "Nested local class inside another local class is not allowed" )
	@Test
	public void testNestedLocalClassErrors() {
		assertThrows( Exception.class, () -> {
			instance.executeSource(
			    """
			    class Outer {
			        class Inner {
			            function getValue() {
			                return "inner";
			            }
			        }
			    }
			    result = new Outer();
			    """,
			    context );
		} );
	}

	@DisplayName( "Local class inside a .bx class body is not allowed" )
	@Test
	public void testLocalClassInsideBxClassFileErrors() {
		assertThrows( Exception.class, () -> {
			instance.executeSource(
			    """
			    class Outer {
			        class Nested {
			            function doStuff() {
			                return "stuff";
			            }
			        }

			        function getFoo() {
			            return "foo";
			        }
			    }
			    result = new Outer().getFoo();
			    """,
			    context );
		} );
	}

	@DisplayName( "Local class inside a function inside another local class is not allowed" )
	@Test
	public void testLocalClassInsideFunctionInsideClassErrors() {
		assertThrows( Exception.class, () -> {
			instance.executeSource(
			    """
			    class Wrapper {
			        function factory() {
			            class Product {
			                function getName() {
			                    return "widget";
			                }
			            }
			            return new Product();
			        }
			    }
			    result = new Wrapper().factory();
			    """,
			    context );
		} );
	}

	@DisplayName( "Local class inside template script island with nested class errors" )
	@Test
	public void testNestedLocalClassInTemplateErrors() {
		assertThrows( Exception.class, () -> {
			instance.executeSource(
			    """
			    <bx:script>
			        class Parent {
			            class Child {
			                function greet() {
			                    return "hi";
			                }
			            }
			        }
			        result = new Parent();
			    </bx:script>
			    """,
			    context, BoxSourceType.BOXTEMPLATE );
		} );
	}

	@DisplayName( "Import with same name as local class should error" )
	@Test
	public void testImportConflictsWithLocalClass() {
		assertThrows( Exception.class, () -> {
			instance.executeSource(
			    """
			    import java:java.util.HashMap as Widget;

			    class Widget {
			        function getName() {
			            return "widget";
			        }
			    }

			    result = new Widget().getName();
			    """,
			    context );
		} );
	}

	@DisplayName( "Local class with same name as import should error" )
	@Test
	public void testLocalClassConflictsWithImport() {
		assertThrows( Exception.class, () -> {
			instance.executeSource(
			    """
			    class HashMap {
			        function getName() {
			            return "my hashmap";
			        }
			    }

			    import java:java.util.HashMap;

			    result = new HashMap().getName();
			    """,
			    context );
		} );
	}

}
