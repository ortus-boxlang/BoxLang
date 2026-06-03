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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

/**
 * Tests for class invocation patterns.
 * Covers:
 * - Expression invocation: (myClass)("args")
 * - Direct variable invocation: myClass("args")
 * - Explicit init: myClass.init("args")
 * - Both positional and named arguments
 * - BoxLang, file-based, inner, and Java classes
 */
public class ClassInvocationTest {

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

	// ==================== Expression invocation: (myClass)() ====================

	@DisplayName( "Expression invocation with no args" )
	@Test
	public void testExpressionInvocationNoArgs() {
		instance.executeSource(
		    """
		    class Foo {
		        function getValue() { return "hello"; }
		    }
		    myClass = Foo;
		    inst = (myClass)();
		    result = inst.getValue();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "hello" );
	}

	@DisplayName( "Expression invocation with positional args" )
	@Test
	public void testExpressionInvocationPositionalArgs() {
		instance.executeSource(
		    """
		    class Foo {
		        property name;
		        function init( name ) {
		            this.name = arguments.name;
		            return this;
		        }
		        function getName() { return this.name; }
		    }
		    myClass = Foo;
		    inst = (myClass)( "Brad" );
		    result = inst.getName();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Brad" );
	}

	@DisplayName( "Expression invocation with named args" )
	@Test
	public void testExpressionInvocationNamedArgs() {
		instance.executeSource(
		    """
		    class Foo {
		        property name;
		        function init( name ) {
		            this.name = arguments.name;
		            return this;
		        }
		        function getName() { return this.name; }
		    }
		    myClass = Foo;
		    inst = (myClass)( name="Luis" );
		    result = inst.getName();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Luis" );
	}

	// ==================== Direct variable invocation: myClass() ====================

	@DisplayName( "Direct variable invocation with no args" )
	@Test
	public void testDirectInvocationNoArgs() {
		instance.executeSource(
		    """
		    class Foo {
		        function getValue() { return "direct"; }
		    }
		    myClass = Foo;
		    result = myClass().getValue();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "direct" );
	}

	@DisplayName( "Direct variable invocation with positional args" )
	@Test
	public void testDirectInvocationPositionalArgs() {
		instance.executeSource(
		    """
		    class Foo {
		        property name;
		        function init( name ) {
		            this.name = arguments.name;
		            return this;
		        }
		        function getName() { return this.name; }
		    }
		    myClass = Foo;
		    result = myClass( "Jorge" ).getName();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Jorge" );
	}

	@DisplayName( "Direct variable invocation with named args" )
	@Test
	public void testDirectInvocationNamedArgs() {
		instance.executeSource(
		    """
		    class Foo {
		        property name;
		        function init( name ) {
		            this.name = arguments.name;
		            return this;
		        }
		        function getName() { return this.name; }
		    }
		    myClass = Foo;
		    result = myClass( name="Gavin" ).getName();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Gavin" );
	}

	// ==================== Explicit init: myClass.init() ====================

	@DisplayName( "Explicit init with no args" )
	@Test
	public void testExplicitInitNoArgs() {
		instance.executeSource(
		    """
		    class Foo {
		        function getValue() { return "inited"; }
		    }
		    myClass = Foo;
		    result = myClass.init().getValue();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "inited" );
	}

	@DisplayName( "Explicit init with positional args" )
	@Test
	public void testExplicitInitPositionalArgs() {
		instance.executeSource(
		    """
		    class Foo {
		        property name;
		        function init( name ) {
		            this.name = arguments.name;
		            return this;
		        }
		        function getName() { return this.name; }
		    }
		    myClass = Foo;
		    result = myClass.init( "Eric" ).getName();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Eric" );
	}

	@DisplayName( "Explicit init with named args" )
	@Test
	public void testExplicitInitNamedArgs() {
		instance.executeSource(
		    """
		    class Foo {
		        property name;
		        function init( name ) {
		            this.name = arguments.name;
		            return this;
		        }
		        function getName() { return this.name; }
		    }
		    myClass = Foo;
		    result = myClass.init( name="Jon" ).getName();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Jon" );
	}

	// ==================== Double colon init: myClass::init() ====================

	@DisplayName( "Double colon init with positional args" )
	@Test
	public void testDoubleColonInitPositionalArgs() {
		instance.executeSource(
		    """
		    class Foo {
		        property name;
		        function init( name ) {
		            this.name = arguments.name;
		            return this;
		        }
		        function getName() { return this.name; }
		    }
		    myClass = Foo;
		    result = myClass::init( "Sam" ).getName();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Sam" );
	}

	// ==================== File-based class references ====================

	@DisplayName( "Expression invocation on imported file-based class" )
	@Test
	public void testFileBasedExpressionInvocation() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.InnerClassOuter;
		    myClass = InnerClassOuter;
		    inst = (myClass)();
		    result = inst.getOwnNameDot();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "OuterClass" );
	}

	@DisplayName( "Direct invocation on imported file-based class" )
	@Test
	public void testFileBasedDirectInvocation() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.InnerClassOuter;
		    myClass = InnerClassOuter;
		    result = myClass().getOwnNameDot();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "OuterClass" );
	}

	@DisplayName( "Explicit init on imported file-based class" )
	@Test
	public void testFileBasedExplicitInit() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.InnerClassOuter;
		    myClass = InnerClassOuter;
		    result = myClass.init().getOwnNameDot();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "OuterClass" );
	}

	// ==================== Inner class references ====================

	@DisplayName( "Instantiate inner class from class reference" )
	@Test
	public void testInnerClassAsCallable() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.InnerClassOuter;
		    innerRef = InnerClassOuter::Inner;
		    inst = (innerRef)();
		    result = inst.getValue();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "inner" );
	}

	@DisplayName( "Instantiate inner class via init" )
	@Test
	public void testInnerClassExplicitInit() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.InnerClassOuter;
		    innerRef = InnerClassOuter::Inner;
		    result = innerRef.init().getValue();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "inner" );
	}

	// ==================== Java class references ====================

	@DisplayName( "Java class via expression invocation" )
	@Test
	public void testJavaClassExpressionInvocation() {
		instance.executeSource(
		    """
		    import java:java.lang.StringBuilder;
		    myClass = StringBuilder;
		    inst = (myClass)( "hello" );
		    result = inst.toString();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "hello" );
	}

	@DisplayName( "Java class via direct invocation" )
	@Test
	public void testJavaClassDirectInvocation() {
		instance.executeSource(
		    """
		    import java:java.lang.StringBuilder;
		    myClass = StringBuilder;
		    result = myClass( "direct" ).toString();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "direct" );
	}

	@DisplayName( "Java class via explicit init" )
	@Test
	public void testJavaClassExplicitInit() {
		instance.executeSource(
		    """
		    import java:java.lang.StringBuilder;
		    myClass = StringBuilder;
		    result = myClass.init( "world" ).toString();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "world" );
	}

	// ==================== Result is IClassRunnable ====================

	@DisplayName( "Callable class result is IClassRunnable" )
	@Test
	public void testResultIsIClassRunnable() {
		instance.executeSource(
		    """
		    class Foo {
		        function getValue() { return "test"; }
		    }
		    myClass = Foo;
		    result = myClass();
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( IClassRunnable.class );
	}

	// ==================== Lambda/closure factory pattern ====================

	@DisplayName( "Class reference stored in closure and invoked" )
	@Test
	public void testClassRefInClosure() {
		instance.executeSource(
		    """
		    class Widget {
		        property label;
		        function init( label ) {
		            this.label = arguments.label;
		            return this;
		        }
		        function getLabel() { return this.label; }
		    }
		    factory = ( clazz, label ) -> (clazz)( label );
		    result = factory( Widget, "myWidget" ).getLabel();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "myWidget" );
	}

	// ==================== Higher-order function usage ====================

	@DisplayName( "Class variable used in array map with closure callback" )
	@Test
	public void testClassVariableInMapWithClosure() {
		instance.executeSource(
		    """
		    class Person {
		        property name;
		        function init( name ) {
		            this.name = arguments.name;
		            return this;
		        }
		        function getName() { return this.name; }
		    }
		    myClass = Person;
		    names = [ "Alice", "Bob", "Charlie" ];
		    users = names.map( myClass );
		    result = users.map( ( item ) => item.getName() );
		    """,
		    context );
		assertThat( variables.getAsArray( result ).toArray() ).isEqualTo( new Object[] { "Alice", "Bob", "Charlie" } );
	}

	@DisplayName( "Class variable used in array map with lambda callback" )
	@Test
	public void testClassVariableInMapWithLambda() {
		instance.executeSource(
		    """
		    class Person {
		        property name;
		        function init( name ) {
		            this.name = arguments.name;
		            return this;
		        }
		        function getName() { return this.name; }
		    }
		    myClass = Person;
		    names = [ "Alice", "Bob", "Charlie" ];
		    users = names.map( myClass );
		    result = users.map( ( item ) -> item.getName() );
		    """,
		    context );
		assertThat( variables.getAsArray( result ).toArray() ).isEqualTo( new Object[] { "Alice", "Bob", "Charlie" } );
	}

	@DisplayName( "Import name used directly in array map" )
	@Test
	public void testImportNameInHigherOrderMap() {
		instance.executeSource(
		    """
		    class Person {
		        property name;
		        function init( name ) {
		            this.name = arguments.name;
		            return this;
		        }
		        function getName() { return this.name; }
		    }
		    names = [ "Alice", "Bob", "Charlie" ];
		    users = names.map( Person );
		    result = users.map( ( item ) -> item.getName() );
		    """,
		    context );
		assertThat( variables.getAsArray( result ).toArray() ).isEqualTo( new Object[] { "Alice", "Bob", "Charlie" } );
	}

	@DisplayName( "Map array of structs to class instances with multiple properties" )
	@Test
	public void testMapStructsToClassInstances() {
		instance.executeSource(
		    """
		    class Person {
		        property name;
		        property age;
		        property title;
		    }
		    data = [
		        { name: "Alice", age: 30, title: "Engineer" },
		        { name: "Bob", age: 25, title: "Designer" },
		        { name: "Charlie", age: 35, title: "Manager" }
		    ];
		    people = data.map( Person );
		    result = people.map( ( p ) -> p.getName() );
		    result2 = people.map( ( p ) -> p.getAge() );
		    result3 = people.map( ( p ) -> p.getTitle() );
		    """,
		    context );
		assertThat( variables.getAsArray( result ).toArray() ).isEqualTo( new Object[] { "Alice", "Bob", "Charlie" } );
		assertThat( variables.getAsArray( Key.of( "result2" ) ).toArray() ).isEqualTo( new Object[] { 30, 25, 35 } );
		assertThat( variables.getAsArray( Key.of( "result3" ) ).toArray() ).isEqualTo( new Object[] { "Engineer", "Designer", "Manager" } );
	}

	@DisplayName( "Map array of structs to class variable with multiple properties" )
	@Test
	public void testMapStructsToClassVariable() {
		instance.executeSource(
		    """
		    class Person {
		        property name;
		        property age;
		        property title;
		    }
		    myClass = Person;
		    data = [
		        { name: "Alice", age: 30, title: "Engineer" },
		        { name: "Bob", age: 25, title: "Designer" },
		        { name: "Charlie", age: 35, title: "Manager" }
		    ];
		    people = data.map( myClass );
		    result = people.map( ( p ) -> p.getName() );
		    result2 = people.map( ( p ) -> p.getAge() );
		    result3 = people.map( ( p ) -> p.getTitle() );
		    """,
		    context );
		assertThat( variables.getAsArray( result ).toArray() ).isEqualTo( new Object[] { "Alice", "Bob", "Charlie" } );
		assertThat( variables.getAsArray( Key.of( "result2" ) ).toArray() ).isEqualTo( new Object[] { 30, 25, 35 } );
		assertThat( variables.getAsArray( Key.of( "result3" ) ).toArray() ).isEqualTo( new Object[] { "Engineer", "Designer", "Manager" } );
	}

}
