
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

import ortus.boxlang.compiler.parser.BoxSourceType;
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
		assertThat( e.getDetail() ).isEqualTo( "boom detail" );
		assertThat( e.getErrorCode() ).isEqualTo( "boom code" );
		assertThat( e.getType() ).isEqualTo( "boom.type" );
		assertThat( e.getExtendedInfo() ).isInstanceOf( Array.class );
		assertThat( ( ( Array ) e.getExtendedInfo() ).toArray( new String[ 0 ] ) ).isEqualTo( new String[] { "boom", "extended", "info" } );
	}

	@Test
	public void testThrowEverything2() {
		instance.executeSource( """
		                        try {
		                        	throw(
		                        		message='boom message',
		                        		detail='boom detail',
		                        		errorcode='boom code',
		                        		type='boom.type',
		                        		extendedinfo=['boom','extended','info'],
		                        		object=new java:java.lang.Exception('boom inner')
		                        	);
		                        } catch ( e ) {
		                        	message = e.message;
		                        	detail = e.detail;
		                        	errorcode = e.errorCode;
		                        	type = e.type;
		                        	extendedinfo = e.extendedInfo;
		                        	cause = e.cause;
		                        }
		                        """, context );
		assertThat( variables.get( Key.of( "message" ) ) ).isEqualTo( "boom message" );
		assertThat( variables.get( Key.of( "detail" ) ) ).isEqualTo( "boom detail" );
		assertThat( variables.get( Key.of( "errorcode" ) ) ).isEqualTo( "boom code" );
		assertThat( variables.get( Key.of( "type" ) ) ).isEqualTo( "boom.type" );
		assertThat( variables.get( Key.of( "extendedinfo" ) ) ).isInstanceOf( Array.class );
		assertThat( ( variables.getAsArray( Key.of( "extendedinfo" ) ) ).toArray( new String[ 0 ] ) ).isEqualTo( new String[] { "boom", "extended", "info" } );
		assertThat( variables.get( Key.of( "cause" ) ) ).isInstanceOf( java.lang.Exception.class );
		assertThat( ( ( java.lang.Exception ) variables.get( Key.of( "cause" ) ) ).getMessage() ).isEqualTo( "boom inner" );
	}

	@Test
	public void testThrowJustType() {
		instance.executeSource( """
		                        try {
		                        	throw(
		                        		type='boom.type'
		                        	);
		                        } catch ( e ) {
		                        	type = e.type;
		                        }
		                        """, context );
		assertThat( variables.get( Key.of( "type" ) ) ).isEqualTo( "boom.type" );
	}

	@Test
	public void testThrowJustType2() {
		instance.executeSource( """
		                        try {
		                        	throw( type="DivideByZero" );
		                        } catch ( e ) {
		                        	type = e.type;
		                        }
		                        """, context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( Key.of( "type" ) ) ).isEqualTo( "DivideByZero" );
	}

	@Test
	public void testThrowObjectUnnamed() {
		//@formatter:off
		instance.executeSource( """
		try {
			throw( type="MyCustomException" );
		} catch ( e ) {
			try{
				throw( e );
			} catch( e2 ) {
				type = e2.type;
			}
		}
		""", context, BoxSourceType.CFSCRIPT );
		//@formatter:on
		assertThat( variables.get( Key.of( "type" ) ) ).isEqualTo( "MyCustomException" );
	}

}
