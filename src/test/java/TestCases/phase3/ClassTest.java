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
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.RequestScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
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
		context		= new ScriptingBoxContext( instance.getRuntimeContext() );
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
		                  component extends="com.brad.Wood" implements="Luis,Jorge" singleton gavin="pickin" inject {
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
		    	assert cfc.bx$.$class.getName() == cfc.getThis().bx$.$class.getName();

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
		// assertThat( meta.get( Key.of( "extends" ) ) ).isEqualTo( "" );
		assertThat( meta.get( Key.of( "type" ) ) ).isEqualTo( "Component" );
		assertThat( meta.get( Key.of( "fullname" ) ) ).isEqualTo( "src.test.java.TestCases.phase3.MyClass" );
		assertThat( meta.getAsString( Key.of( "path" ) ).contains( "MyClass.cfc" ) ).isTrue();
		assertThat( meta.get( Key.of( "hashcode" ) ) ).isEqualTo( cfc.hashCode() );
		assertThat( meta.get( Key.of( "properties" ) ) instanceof Array ).isTrue();
		assertThat( meta.get( Key.of( "functions" ) ) instanceof Array ).isTrue();
		assertThat( meta.getAsArray( Key.of( "functions" ) ).size() ).isEqualTo( 4 );
		assertThat( meta.get( Key.of( "extends" ) ) instanceof Struct ).isTrue();
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
		assertThat( meta.get( Key.of( "name" ) ) ).isEqualTo( "src.test.java.TestCases.phase3.MyClass" );
		assertThat( meta.get( Key.of( "type" ) ) ).isEqualTo( "Component" );
		assertThat( meta.get( Key.of( "fullname" ) ) ).isEqualTo( "src.test.java.TestCases.phase3.MyClass" );
		assertThat( meta.getAsString( Key.of( "path" ) ).contains( "MyClass.cfc" ) ).isTrue();
		assertThat( meta.get( Key.of( "hashcode" ) ) ).isEqualTo( cfc.hashCode() );
		assertThat( meta.get( Key.of( "properties" ) ) instanceof Array ).isTrue();
		assertThat( meta.get( Key.of( "functions" ) ) instanceof Array ).isTrue();

		assertThat( meta.get( Key.of( "extends" ) ) instanceof Struct ).isTrue();

		assertThat( meta.getAsArray( Key.of( "functions" ) ).size() ).isEqualTo( 4 );
		var fun1 = meta.getAsArray( Key.of( "functions" ) ).get( 0 );
		assertThat( fun1 ).isInstanceOf( Struct.class );
		assertThat( ( ( Struct ) fun1 ).containsKey( Key.of( "name" ) ) ).isTrue();

		assertThat( meta.get( Key.of( "documentation" ) ) instanceof Struct ).isTrue();
		var docs = meta.getAsStruct( Key.of( "documentation" ) );
		assertThat( docs.getAsString( Key.of( "brad" ) ).trim() ).isEqualTo( "wood" );
		assertThat( docs.get( Key.of( "luis" ) ) ).isEqualTo( "" );
		assertThat( docs.getAsString( Key.of( "hint" ) ).trim() ).isEqualTo( "This is my class description" );

		assertThat( meta.get( Key.of( "annotations" ) ) instanceof Struct ).isTrue();
		var annos = meta.getAsStruct( Key.of( "annotations" ) );
		assertThat( annos.getAsString( Key.of( "foo" ) ).trim() ).isEqualTo( "bar" );
		assertThat( annos.getAsString( Key.of( "extends" ) ).trim() ).isEqualTo( "com.brad.Wood" );
		assertThat( annos.getAsString( Key.of( "implements" ) ).trim() ).isEqualTo( "Luis,Jorge" );
		assertThat( annos.getAsString( Key.of( "singleton" ) ).trim() ).isEqualTo( "" );
		assertThat( annos.getAsString( Key.of( "gavin" ) ).trim() ).isEqualTo( "pickin" );
		assertThat( annos.getAsString( Key.of( "inject" ) ).trim() ).isEqualTo( "" );

	}

}