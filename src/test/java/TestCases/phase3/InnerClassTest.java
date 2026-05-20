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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

/**
 * Integration tests for inner classes defined inside .bx class files.
 *
 * An inner class is a named class ({@code class Foo {}}) defined inside the body of another
 * class in a .bx file. Inner classes are compiled as sibling JVM classes with {@code $} delimited
 * names (e.g., {@code OuterClass$Inner}).
 */
public class InnerClassTest {

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

	@DisplayName( "Inner class can be instantiated from outer class method" )
	@Test
	public void testInnerClassInstantiation() {
		instance.executeSource(
		    """
		    outer = new src.test.java.TestCases.phase3.InnerClassOuter();
		      result = outer.getInner().getValue();
		      """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "inner" );
	}

	@DisplayName( "Inner class with properties and init" )
	@Test
	public void testInnerClassWithProperties() {
		instance.executeSource(
		    """
		    outer = new src.test.java.TestCases.phase3.InnerClassOuter();
		      result = outer.getHelper( "custom" ).getLabel();
		      """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "custom" );
	}

	@DisplayName( "Inner class instance is IClassRunnable" )
	@Test
	public void testInnerClassIsIClassRunnable() {
		instance.executeSource(
		    """
		    outer = new src.test.java.TestCases.phase3.InnerClassOuter();
		      result = outer.getInner();
		      """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( IClassRunnable.class );
	}

	@DisplayName( "Inner class can extend another inner class" )
	@Test
	public void testInnerClassExtends() {
		instance.executeSource(
		    """
		    outer = new src.test.java.TestCases.phase3.InnerClassExtends();
		    result = outer.getDog().speak();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Woof!" );
	}

	@DisplayName( "Three levels deep: class inside class inside class" )
	@Test
	public void testThreeLevelsDeep() {
		instance.executeSource(
		    """
		    outer = new src.test.java.TestCases.phase3.InnerClassNested();
		    first = outer.getFirst();
		    result = first.getDepth();
		    result2 = first.getSecond().getDepth();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "first" );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( "second" );
	}

	@DisplayName( "Inner class defined and used in a script inside a class body" )
	@Test
	public void testInnerClassInScriptContext() {
		instance.executeSource(
		    """
		    class Outer {
		        class Inner {
		            function getValue() {
		                return "from inner";
		            }
		        }

		        function getInner() {
		            return new Inner();
		        }
		    }

		    result = new Outer().getInner().getValue();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "from inner" );
	}

	@DisplayName( "Multiple inner classes in same outer class" )
	@Test
	public void testMultipleInnerClasses() {
		instance.executeSource(
		    """
		    class Container {
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

		        function compute( a, b ) {
		            return new Multiplier().multiply( new Adder().add( a, b ), 2 );
		        }
		    }

		    result = new Container().compute( 3, 4 );
		    """,
		    context );
		assertThat( ( ( Number ) variables.get( result ) ).intValue() ).isEqualTo( 14 );
	}

	@DisplayName( "Inner class with static members" )
	@Test
	public void testInnerClassWithStatic() {
		instance.executeSource(
		    """
		    class Outer {
		        class Config {
		            static {
		                static.MAX = 100;
		            }
		        }

		        function getMax() {
		            return Config::MAX;
		        }
		    }

		    result = new Outer().getMax();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 100 );
	}

	@DisplayName( "Inner class hoisting: use before definition in class body" )
	@Test
	public void testInnerClassHoisting() {
		instance.executeSource(
		    """
		    class Outer {
		        function getWidget() {
		            return new Widget();
		        }

		        class Widget {
		            function getName() {
		                return "widget";
		            }
		        }
		    }

		    result = new Outer().getWidget().getName();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "widget" );
	}

	@DisplayName( "Inner class can access outer class statics via dot and double colon syntax" )
	@Test
	public void testInnerClassAccessOuterStatics() {
		instance.executeSource(
		    """
		    outer = new src.test.java.TestCases.phase3.InnerClassOuter();
		    resultDot = outer.getInner().getOuterNameDot();
		    resultColon = outer.getInner().getOuterNameColon();
		    """,
		    context );
		assertThat( variables.get( Key.of( "resultDot" ) ) ).isEqualTo( "OuterClass" );
		assertThat( variables.get( Key.of( "resultColon" ) ) ).isEqualTo( "OuterClass" );
	}

	@DisplayName( "Outer class can reference itself by name via dot and double colon syntax" )
	@Test
	public void testOuterClassSelfReference() {
		instance.executeSource(
		    """
		    outer = new src.test.java.TestCases.phase3.InnerClassOuter();
		    resultDot = outer.getOwnNameDot();
		    resultColon = outer.getOwnNameColon();
		    """,
		    context );
		assertThat( variables.get( Key.of( "resultDot" ) ) ).isEqualTo( "OuterClass" );
		assertThat( variables.get( Key.of( "resultColon" ) ) ).isEqualTo( "OuterClass" );
	}

	@DisplayName( "Inner class accesses outer class statics inline via dot and double colon" )
	@Test
	public void testInnerClassAccessOuterStaticsInline() {
		instance.executeSource(
		    """
		    class MyClass {
		        static {
		            static.GREETING = "hello";
		        }

		        class Inner {
		            function getGreetingDot() {
		                return MyClass.GREETING;
		            }
		            function getGreetingColon() {
		                return MyClass::GREETING;
		            }
		        }

		        function getInner() {
		            return new Inner();
		        }
		    }

		    resultDot = new MyClass().getInner().getGreetingDot();
		    resultColon = new MyClass().getInner().getGreetingColon();
		    """,
		    context );
		assertThat( variables.get( Key.of( "resultDot" ) ) ).isEqualTo( "hello" );
		assertThat( variables.get( Key.of( "resultColon" ) ) ).isEqualTo( "hello" );
	}

	@DisplayName( "Variable assignment with outer class name is an error when class has inner classes" )
	@Test
	public void testOuterClassNameReservedVariable() {
		assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    class MyClass {
		        class Inner {
		            function getValue() { return "inner"; }
		        }
		        function doStuff() {
		            MyClass = "oops";
		            return MyClass;
		        }
		    }
		    result = new MyClass().doStuff();
		    """,
		    context ) );
	}

	@DisplayName( "Function argument with outer class name is an error when class has inner classes" )
	@Test
	public void testOuterClassNameReservedArgument() {
		assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    class MyClass {
		        class Inner {
		            function getValue() { return "inner"; }
		        }
		        function doStuff( MyClass ) {
		            return MyClass;
		        }
		    }
		    result = new MyClass().doStuff( "test" );
		    """,
		    context ) );
	}

	@DisplayName( "File-based: outer class name reserved as argument" )
	@Test
	void testFileBasedReservedArgument() {
		assertThrows( ExpressionException.class, () -> instance.executeSource(
		    """
		    result = new src.test.java.TestCases.phase3.InnerClassReservedName();
		    """,
		    context ) );
	}

	@DisplayName( "File-based: outer class name reserved as variable" )
	@Test
	void testFileBasedReservedVariable() {
		assertThrows( ExpressionException.class, () -> instance.executeSource(
		    """
		    result = new src.test.java.TestCases.phase3.InnerClassReservedNameVar();
		    """,
		    context ) );
	}

	@DisplayName( "Inner class cannot have same name as enclosing file class" )
	@Test
	void testInnerClassCannotMatchFileName() {
		assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    result = new src.test.java.TestCases.phase3.InnerClassSameAsFile();
		    """,
		    context ) );
	}

}
