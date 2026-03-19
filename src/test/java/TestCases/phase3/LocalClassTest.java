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

		    adder      = new Adder();
		    multiplier = new Multiplier();
		    result     = multiplier.multiply( adder.add( 2, 3 ), 4 );
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

}
