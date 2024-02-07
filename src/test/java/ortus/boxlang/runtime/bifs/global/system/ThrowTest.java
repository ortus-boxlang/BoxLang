
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
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.CustomException;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

public class ThrowTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		variables.clear();
	}

	@DisplayName( "It can throw nothing" )
	@Test
	public void testThrowNothing() {
		Throwable e = assertThrows( CustomException.class, () -> instance.executeSource( " throw() ", context ) );
		assertThat( e.getMessage() ).isNull();
	}

	@DisplayName( "It can throw checked object" )
	@Test
	public void testThrowCheckedObject() {
		Throwable e = assertThrows( CustomException.class, () -> instance.executeSource( " throw( object=new java:java.lang.Exception('boom') ) ", context ) );
		assertThat( e.getMessage() ).isEqualTo( "boom" );
		assertThat( e.getCause() ).isNotNull();
		assertThat( e.getCause() ).isInstanceOf( java.lang.Exception.class );
	}

	@DisplayName( "It can throw unchecked object" )
	@Test
	public void testThrowunCheckedObject() {
		Throwable e = assertThrows( KeyNotFoundException.class,
		    () -> instance.executeSource( " throw( object=new java:ortus.boxlang.runtime.types.exceptions.KeyNotFoundException('boom') ) ", context ) );
		assertThat( e.getMessage() ).isEqualTo( "boom" );
		assertThat( e.getCause() ).isNull();
	}

	@DisplayName( "It can throw message" )
	@Test
	public void testThrowMessage() {
		Throwable e = assertThrows( CustomException.class,
		    () -> instance.executeSource( " throw( 'boom' ) ", context ) );
		assertThat( e.getMessage() ).isEqualTo( "boom" );
		assertThat( e.getCause() ).isNull();
	}

	@DisplayName( "It can throw message and object" )
	@Test
	public void testThrowMessageAndObject() {
		Throwable e = assertThrows( CustomException.class,
		    () -> instance.executeSource( " throw( message='boom outer', object=new java:java.lang.Exception('boom inner') ) ", context ) );
		assertThat( e.getMessage() ).isEqualTo( "boom outer" );
		assertThat( e.getCause() ).isNotNull();
		assertThat( e.getCause().getMessage() ).isEqualTo( "boom inner" );
	}

	@DisplayName( "It can throw everthing" )
	@Test
	public void testThrowEverything() {
		CustomException e = assertThrows( CustomException.class,
		    () -> instance.executeSource( """
		                                  throw(
		                                  	message='boom message',
		                                  	detail='boom detail',
		                                  	errorcode='boom code',
		                                  	type='boom.type',
		                                  	extendedinfo=['boom','extended','info'],
		                                  	object=new java:java.lang.Exception('boom inner')
		                                  );
		                                                                  	""", context ) );
		assertThat( e.getMessage() ).isEqualTo( "boom message" );
		assertThat( e.getCause() ).isNotNull();
		assertThat( e.getCause() ).isInstanceOf( java.lang.Exception.class );
		assertThat( e.detail ).isEqualTo( "boom detail" );
		assertThat( e.errorCode ).isEqualTo( "boom code" );
		assertThat( e.type ).isEqualTo( "boom.type" );
		assertThat( e.extendedInfo ).isInstanceOf( Array.class );
		assertThat( ( ( Array ) e.extendedInfo ).toArray( new String[ 0 ] ) ).isEqualTo( new String[] { "boom", "extended", "info" } );

	}

}
