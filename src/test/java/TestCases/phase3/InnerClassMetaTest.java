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

import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.meta.ClassMeta;

/**
 * Tests for inner class metadata access via $bx (ClassMeta): enclosing class,
 * inner classes, static access, and BoxClassSupport.dereferenceStatic.
 */
public class InnerClassMetaTest {

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

	@DisplayName( "ClassMeta.getInnerBoxClasses() returns inner class map via $bx" )
	@Test
	public void testClassMetaGetInnerBoxClasses() {
		instance.executeSource(
		    """
		    outer = new src.test.java.TestCases.phase3.InnerClassOuter();
		    result = outer.$bx;
		    """,
		    context );
		ClassMeta			meta	= ( ClassMeta ) variables.get( result );
		Map<Key, Class<?>>	inners	= meta.getInnerBoxClasses();
		assertThat( inners ).isNotEmpty();
		assertThat( inners.containsKey( Key.of( "Inner" ) ) ).isTrue();
		assertThat( inners.containsKey( Key.of( "Helper" ) ) ).isTrue();
	}

	@DisplayName( "ClassMeta.getEnclosingBoxClass() is null for top-level class" )
	@Test
	public void testClassMetaEnclosingBoxClassNullForTopLevel() {
		instance.executeSource(
		    """
		    outer = new src.test.java.TestCases.phase3.InnerClassOuter();
		    result = outer.$bx;
		    """,
		    context );
		ClassMeta meta = ( ClassMeta ) variables.get( result );
		assertThat( meta.getEnclosingBoxClass() ).isNull();
	}

	@DisplayName( "ClassMeta.getEnclosingBoxClass() returns outer class for inner class" )
	@Test
	public void testClassMetaEnclosingBoxClassForInner() {
		instance.executeSource(
		    """
		    outer = new src.test.java.TestCases.phase3.InnerClassOuter();
		    result = outer.getInner().$bx;
		    """,
		    context );
		ClassMeta meta = ( ClassMeta ) variables.get( result );
		assertThat( meta.getEnclosingBoxClass() ).isNotNull();
		assertThat( meta.getEnclosingBoxClass().getSimpleName() ).contains( "Innerclassouter" );
	}

	@DisplayName( "ClassMeta.getInnerBoxClasses() is empty for inner class with no children" )
	@Test
	public void testClassMetaInnerBoxClassesEmptyForLeaf() {
		instance.executeSource(
		    """
		    outer = new src.test.java.TestCases.phase3.InnerClassOuter();
		    result = outer.getInner().$bx;
		    """,
		    context );
		ClassMeta			meta	= ( ClassMeta ) variables.get( result );
		Map<Key, Class<?>>	inners	= meta.getInnerBoxClasses();
		assertThat( inners ).isEmpty();
	}

	@DisplayName( "Inner class names in metadata struct via $bx.meta" )
	@Test
	public void testInnerClassNamesInMetaStruct() {
		instance.executeSource(
		    """
		    outer = new src.test.java.TestCases.phase3.InnerClassOuter();
		    result = outer.$bx.meta.innerClasses;
		    """,
		    context );
		IStruct innerClasses = ( IStruct ) variables.get( result );
		assertThat( innerClasses.containsKey( Key.of( "Inner" ) ) ).isTrue();
		assertThat( innerClasses.containsKey( Key.of( "Helper" ) ) ).isTrue();
	}

	@DisplayName( "Enclosing class name in metadata struct via $bx.meta" )
	@Test
	public void testEnclosingClassNameInMetaStruct() {
		instance.executeSource(
		    """
		    outer = new src.test.java.TestCases.phase3.InnerClassOuter();
		    result = outer.getInner().$bx.meta.enclosingClass;
		    """,
		    context );
		String enclosing = ( String ) variables.get( result );
		assertThat( enclosing ).contains( "InnerClassOuter" );
	}

	@DisplayName( "Top-level class has empty enclosingClass in metadata" )
	@Test
	public void testTopLevelEnclosingClassEmpty() {
		instance.executeSource(
		    """
		    outer = new src.test.java.TestCases.phase3.InnerClassOuter();
		    result = outer.$bx.meta.enclosingClass;
		    """,
		    context );
		String enclosing = ( String ) variables.get( result );
		assertThat( enclosing ).isEmpty();
	}

	@DisplayName( "Inline class with inner classes exposes metadata correctly" )
	@Test
	public void testInlineClassInnerMetadata() {
		instance.executeSource(
		    """
		    class Container {
		        class Widget {
		            function getName() { return "widget"; }
		        }
		        function getWidget() { return new Widget(); }
		    }
		    outer = new Container();
		    result = outer.$bx;
		    """,
		    context );
		ClassMeta			meta	= ( ClassMeta ) variables.get( result );
		Map<Key, Class<?>>	inners	= meta.getInnerBoxClasses();
		assertThat( inners.containsKey( Key.of( "Widget" ) ) ).isTrue();
	}

	@DisplayName( "Static dereference of inner class name returns the actual Class" )
	@Test
	public void testStaticDereferenceInnerClass() {
		instance.executeSource(
		    """
		    import src.test.java.TestCases.phase3.InnerClassOuter;
		    result = InnerClassOuter::Inner;
		    """,
		    context );
		Object val = variables.get( result );
		assertThat( val ).isNotNull();
		assertThat( val ).isInstanceOf( Class.class );
		assertThat( ( ( Class<?> ) val ).getName() ).contains( "Inner" );
	}

	@DisplayName( "Cannot assign to a static key that is the name of an inner class via ::" )
	@Test
	public void testCannotAssignStaticInnerClassNameDoubleColon() {
		assertThrows( BoxRuntimeException.class, () -> {
			instance.executeSource(
			    """
			    import src.test.java.TestCases.phase3.InnerClassOuter;
			    InnerClassOuter::Inner = "oops";
			    """,
			    context );
		} );
	}

	@DisplayName( "Cannot assign to a static key that is the name of an inner class via dot" )
	@Test
	public void testCannotAssignStaticInnerClassNameDot() {
		assertThrows( BoxRuntimeException.class, () -> {
			instance.executeSource(
			    """
			    import src.test.java.TestCases.phase3.InnerClassOuter;
			    InnerClassOuter.Inner = "oops";
			    """,
			    context );
		} );
	}

}
