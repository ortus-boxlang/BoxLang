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

package ortus.boxlang.runtime.bifs.global.system;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class CreateObjectTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "Test BIF CreateObject With BX" )
	@Test
	void testBIFBX() {
		Object test = instance.executeStatement( "createObject( 'class', 'src.test.java.TestCases.phase3.MyClass' )" );
		assertTrue( test instanceof IClassRunnable );
	}

	@DisplayName( "Test BIF CreateObject With BX no type" )
	@Test
	void testBIFBXNoType() {
		Object test = instance.executeStatement( "createObject( 'src.test.java.TestCases.phase3.MyClass' )" );
		assertTrue( test instanceof IClassRunnable );
	}

	@DisplayName( "Test BIF CreateObject Java" )
	@Test
	void testBIFJava() {
		Object test = instance.executeStatement( "createObject( 'java', 'java.lang.String' )" );
		assertTrue( test instanceof DynamicObject );
		test = instance.executeStatement( "createObject( 'java', 'java.lang.String' ).init()" );
		assertTrue( test instanceof String );
	}

	@DisplayName( "It can createobject java with one class path as string" )
	@Test
	void testBIFJavaClassPathAsString() {
		DynamicObject test = ( DynamicObject ) instance.executeStatement(
		    "createObject( 'java', 'HelloWorld', '/src/test/resources/libs/helloworld.jar' )"
		);
		assertThat( test.getTargetClass().getName() ).isEqualTo( "HelloWorld" );
	}

	@DisplayName( "It can createobject java with one class path as an array" )
	@Test
	void testBIFJavaClassPathAsArray() {
		DynamicObject test = ( DynamicObject ) instance.executeStatement(
		    "createObject( 'java', 'HelloWorld', ['/src/test/resources/libs/helloworld.jar'] )"
		);
		assertThat( test.getTargetClass().getName() ).isEqualTo( "HelloWorld" );
	}

}
