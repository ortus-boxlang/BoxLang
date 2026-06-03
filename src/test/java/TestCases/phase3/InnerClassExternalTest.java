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
import ortus.boxlang.runtime.types.IStruct;

/**
 * Tests for accessing inner classes externally via the $ separator syntax.
 * These tests verify that inner classes can be imported, instantiated, and
 * have their static members accessed from a completely different script or class.
 */
public class InnerClassExternalTest {

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

	@DisplayName( "Instantiate inner class externally via $ syntax with new" )
	@Test
	public void testNewInnerClassExternalDollarSyntax() {
		instance.executeSource(
		    """
		    result = new src.test.java.TestCases.phase3.InnerClassExternal$Widget( "my-widget" );
		    """,
		    context );
		Object res = variables.get( result );
		assertThat( res ).isInstanceOf( IClassRunnable.class );
		assertThat( ( ( IClassRunnable ) res ).dereferenceAndInvoke( context, Key.of( "getName" ), new Object[] {}, false ) )
		    .isEqualTo( "my-widget" );
	}

	@DisplayName( "Import inner class externally via $ syntax" )
	@Test
	public void testImportInnerClassExternalDollarSyntax() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.InnerClassExternal$Widget;
		    result = new Widget( "imported-widget" );
		    """,
		    context );
		Object res = variables.get( result );
		assertThat( res ).isInstanceOf( IClassRunnable.class );
		assertThat( ( ( IClassRunnable ) res ).dereferenceAndInvoke( context, Key.of( "getName" ), new Object[] {}, false ) )
		    .isEqualTo( "imported-widget" );
	}

	@DisplayName( "Import inner class with alias via $ syntax" )
	@Test
	public void testImportInnerClassWithAlias() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.InnerClassExternal$Widget as MyWidget;
		    result = new MyWidget( "aliased-widget" );
		    """,
		    context );
		Object res = variables.get( result );
		assertThat( res ).isInstanceOf( IClassRunnable.class );
		assertThat( ( ( IClassRunnable ) res ).dereferenceAndInvoke( context, Key.of( "getName" ), new Object[] {}, false ) )
		    .isEqualTo( "aliased-widget" );
	}

	@DisplayName( "Access inner class static field via $ and double colon syntax" )
	@Test
	public void testInnerClassStaticAccessDollarSyntax() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.InnerClassExternal$Widget;
		    result = Widget::WIDGET_TYPE;
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "gadget" );
	}

	@DisplayName( "Access inner class static field via fully qualified $ and double colon syntax" )
	@Test
	public void testInnerClassStaticAccessFullyQualifiedDollarSyntax() {
		instance.executeSource(
		    """
		    result = src.test.java.TestCases.phase3.InnerClassExternal$Widget::WIDGET_TYPE;
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "gadget" );
	}

	@DisplayName( "Instantiate second inner class externally via $ syntax" )
	@Test
	public void testSecondInnerClassExternal() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.InnerClassExternal$Util;
		    result = new Util().greet( "World" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Hello, World" );
	}

	@DisplayName( "Inline: import inner class from inline outer via $ syntax" )
	@Test
	public void testInlineInnerClassExternalAccess() {
		instance.executeSource(
		    """
		    class Outer {
		        class Nested {
		            function getValue() {
		                return "nested-value";
		            }
		        }
		        function getNested() {
		            return new Nested();
		        }
		    }

		    // Access from within the same script using $ syntax
		    result = new Outer$Nested().getValue();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "nested-value" );
	}

	@DisplayName( "Nested inner class accessed via chained $ syntax (Class$Inner$InnerAgain)" )
	@Test
	public void testNestedInnerClassDollarChain() {
		instance.executeSource(
		    """
		    result = new src.test.java.TestCases.phase3.InnerClassNested$First$Second().getDepth();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "second" );
	}

	@DisplayName( "getClassMetadata() with inner class via $ syntax" )
	@Test
	public void testGetClassMetadataInnerClassDollarSyntax() {
		instance.executeSource(
		    """
		    result = getClassMetadata( "src.test.java.TestCases.phase3.InnerClassExternal$Widget" );
		    """,
		    context );
		Object res = variables.get( result );
		assertThat( res ).isInstanceOf( IStruct.class );
		IStruct meta = ( IStruct ) res;
		assertThat( meta.getAsString( Key.of( "name" ) ) ).isEqualTo( "src.test.java.TestCases.phase3.InnerClassExternal$Widget" );
		assertThat( meta.get( Key.of( "type" ) ) ).isEqualTo( "Class" );
		assertThat( meta.get( Key.of( "properties" ) ) ).isNotNull();
		assertThat( meta.getAsArray( Key.of( "properties" ) ) ).hasSize( 1 );
		assertThat( meta.get( Key.of( "functions" ) ) ).isNotNull();
	}

	@DisplayName( "getClassMetadata() with imported inner class via $ syntax" )
	@Test
	public void testGetClassMetadataImportedInnerClass() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.InnerClassExternal$Util;
		    result = getClassMetadata( "Util" );
		    """,
		    context );
		Object res = variables.get( result );
		assertThat( res ).isInstanceOf( IStruct.class );
		IStruct meta = ( IStruct ) res;
		assertThat( meta.getAsString( Key.of( "name" ) ) ).isEqualTo( "src.test.java.TestCases.phase3.InnerClassExternal$Util" );
		assertThat( meta.get( Key.of( "type" ) ) ).isEqualTo( "Class" );
		assertThat( meta.get( Key.of( "functions" ) ) ).isNotNull();
	}

	@DisplayName( "getMetadata() on inner class instance via $ syntax" )
	@Test
	public void testGetMetadataInnerClassInstance() {
		instance.executeSource(
		    """
		    obj = new src.test.java.TestCases.phase3.InnerClassExternal$Widget( "test" );
		    result = getMetadata( obj );
		    """,
		    context );
		Object res = variables.get( result );
		assertThat( res ).isInstanceOf( IStruct.class );
		IStruct meta = ( IStruct ) res;
		assertThat( meta.getAsString( Key.of( "name" ) ) ).isEqualTo( "src.test.java.TestCases.phase3.InnerClassExternal$Widget" );
		assertThat( meta.get( Key.of( "type" ) ) ).isEqualTo( "Class" );
		assertThat( meta.getAsArray( Key.of( "properties" ) ) ).hasSize( 1 );
	}

	@DisplayName( "getMetadata() on imported inner class instance" )
	@Test
	public void testGetMetadataImportedInnerClassInstance() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.InnerClassExternal$Util;
		    obj = new Util();
		    result = getMetadata( obj );
		    """,
		    context );
		Object res = variables.get( result );
		assertThat( res ).isInstanceOf( IStruct.class );
		IStruct meta = ( IStruct ) res;
		assertThat( meta.getAsString( Key.of( "name" ) ) ).isEqualTo( "src.test.java.TestCases.phase3.InnerClassExternal$Util" );
		assertThat( meta.get( Key.of( "type" ) ) ).isEqualTo( "Class" );
		assertThat( meta.get( Key.of( "functions" ) ) ).isNotNull();
	}

	@DisplayName( "createObject() with inner class via $ syntax" )
	@Test
	public void testCreateObjectInnerClassDollarSyntax() {
		instance.executeSource(
		    """
		    result = createObject( "component", "src.test.java.TestCases.phase3.InnerClassExternal$Widget" );
		    """,
		    context );
		Object res = variables.get( result );
		assertThat( res ).isInstanceOf( IClassRunnable.class );
	}

	@DisplayName( "createObject() with inner class and method invocation" )
	@Test
	public void testCreateObjectInnerClassMethodInvocation() {
		instance.executeSource(
		    """
		    obj = createObject( "component", "src.test.java.TestCases.phase3.InnerClassExternal$Util" );
		    result = obj.greet( "BoxLang" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Hello, BoxLang" );
	}

	@DisplayName( "Inner class metadata has enclosingClass set to parent FQN" )
	@Test
	public void testInnerClassMetadataEnclosingClass() {
		instance.executeSource(
		    """
		    obj = new src.test.java.TestCases.phase3.InnerClassExternal$Widget( "test" );
		    result = getMetadata( obj );
		    """,
		    context );
		IStruct meta = ( IStruct ) variables.get( result );
		assertThat( meta.getAsString( Key.of( "enclosingClass" ) ) ).isEqualTo( "src.test.java.TestCases.phase3.InnerClassExternal" );
	}

	@DisplayName( "Outer class metadata has innerClasses struct with child FQNs" )
	@Test
	public void testOuterClassMetadataInnerClasses() {
		instance.executeSource(
		    """
		    obj = new src.test.java.TestCases.phase3.InnerClassExternal();
		    result = getMetadata( obj );
		    """,
		    context );
		IStruct	meta			= ( IStruct ) variables.get( result );
		Object	innerClasses	= meta.get( Key.of( "innerClasses" ) );
		assertThat( innerClasses ).isInstanceOf( IStruct.class );
		IStruct innerStruct = ( IStruct ) innerClasses;
		assertThat( innerStruct.getAsString( Key.of( "Widget" ) ) ).isEqualTo( "src.test.java.TestCases.phase3.InnerClassExternal$Widget" );
		assertThat( innerStruct.getAsString( Key.of( "Util" ) ) ).isEqualTo( "src.test.java.TestCases.phase3.InnerClassExternal$Util" );
	}

	@DisplayName( "Outer class metadata has empty enclosingClass" )
	@Test
	public void testOuterClassMetadataEnclosingClassEmpty() {
		instance.executeSource(
		    """
		    obj = new src.test.java.TestCases.phase3.InnerClassExternal();
		    result = getMetadata( obj );
		    """,
		    context );
		IStruct meta = ( IStruct ) variables.get( result );
		assertThat( meta.getAsString( Key.of( "enclosingClass" ) ) ).isEqualTo( "" );
	}

	@DisplayName( "Inner class metadata has empty innerClasses" )
	@Test
	public void testInnerClassMetadataInnerClassesEmpty() {
		instance.executeSource(
		    """
		    obj = new src.test.java.TestCases.phase3.InnerClassExternal$Util();
		    result = getMetadata( obj );
		    """,
		    context );
		IStruct	meta			= ( IStruct ) variables.get( result );
		Object	innerClasses	= meta.get( Key.of( "innerClasses" ) );
		// Inner class with no inner classes of its own gets Struct.EMPTY
		assertThat( innerClasses ).isInstanceOf( IStruct.class );
		assertThat( ( ( IStruct ) innerClasses ).size() ).isEqualTo( 0 );
	}

	@DisplayName( "Inner class simpleName strips $ prefix correctly" )
	@Test
	public void testInnerClassSimpleName() {
		instance.executeSource(
		    """
		    obj = new src.test.java.TestCases.phase3.InnerClassExternal$Widget( "test" );
		    result = getMetadata( obj );
		    """,
		    context );
		IStruct meta = ( IStruct ) variables.get( result );
		assertThat( meta.getAsString( Key.of( "simpleName" ) ) ).isEqualTo( "Widget" );
	}

	@DisplayName( "Class defined in script has empty enclosingClass" )
	@Test
	public void testClassInScriptHasEmptyEnclosingClass() {
		instance.executeSource(
		    """
		    class Person { function init() {} }
		    obj = new Person();
		    result = getMetadata( obj );
		    """,
		    context );
		IStruct meta = ( IStruct ) variables.get( result );
		assertThat( meta.getAsString( Key.of( "enclosingClass" ) ) ).isEqualTo( "" );
	}

	@DisplayName( "Class defined in script has simple fullname without template prefix" )
	@Test
	public void testClassInScriptHasSimpleFullname() {
		instance.executeSource(
		    """
		    class Person { function init() {} }
		    obj = new Person();
		    result = getMetadata( obj );
		    """,
		    context );
		IStruct meta = ( IStruct ) variables.get( result );
		assertThat( meta.getAsString( Key.of( "fullname" ) ) ).isEqualTo( "Person" );
	}

	@DisplayName( "Class defined in script has simple name matching class name" )
	@Test
	public void testClassInScriptHasSimpleName() {
		instance.executeSource(
		    """
		    class Person { function init() {} }
		    obj = new Person();
		    result = getMetadata( obj );
		    """,
		    context );
		IStruct meta = ( IStruct ) variables.get( result );
		assertThat( meta.getAsString( Key.of( "simpleName" ) ) ).isEqualTo( "Person" );
	}

	@DisplayName( "instanceOf works with inner class by simple name" )
	@Test
	public void testInstanceOfInnerClassSimpleName() {
		instance.executeSource(
		    """
		    obj = new src.test.java.TestCases.phase3.InnerClassExternal$Widget( "test" );
		    result = isInstanceOf( obj, "Widget" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "instanceOf works with inner class by FQN" )
	@Test
	public void testInstanceOfInnerClassFQN() {
		instance.executeSource(
		    """
		    obj = new src.test.java.TestCases.phase3.InnerClassExternal$Widget( "test" );
		    result = isInstanceOf( obj, "src.test.java.TestCases.phase3.InnerClassExternal$Widget" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "instanceOf works with inner class by parent name returns false" )
	@Test
	public void testInstanceOfInnerClassNotParent() {
		instance.executeSource(
		    """
		    obj = new src.test.java.TestCases.phase3.InnerClassExternal$Widget( "test" );
		    result = isInstanceOf( obj, "InnerClassExternal" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( false );
	}

	@DisplayName( "instanceOf works with class defined in script by simple name" )
	@Test
	public void testInstanceOfTemplateLocalClassSimpleName() {
		instance.executeSource(
		    """
		    class Animal {}
		    class Dog extends="Animal" {}
		    obj = new Dog();
		    result1 = isInstanceOf( obj, "Dog" );
		    result2 = isInstanceOf( obj, "Animal" );
		    result3 = isInstanceOf( obj, "Widget" );
		    """,
		    context );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( false );
	}

	@DisplayName( "Inner class 3 levels deep has correct metadata" )
	@Test
	public void testInnerClass3LevelsDeep() {
		instance.executeSource(
		    """
		    obj = new src.test.java.TestCases.phase3.InnerClassExternal$Widget$Button( "OK" );
		    result1 = isInstanceOf( obj, "Button" );
		    result2 = isInstanceOf( obj, "src.test.java.TestCases.phase3.InnerClassExternal$Widget$Button" );
		    result3 = isInstanceOf( obj, "Widget" );
		    """,
		    context );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( false );
	}

}
