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
package ortus.boxlang.runtime.types;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;

public class ClosureTest {

	private IBoxContext context = new ScriptingRequestBoxContext();

	@DisplayName( "can define Closure" )
	@Test
	void testCanDefineClosure() {
		Argument[] args = new Argument[] {
		    new Argument( true, "String", Key.of( "firstName" ), "brad" ),
		    new Argument( true, "String", Key.of( "lastName" ), "wood" )
		};
		new SampleClosure( args, new ScriptingRequestBoxContext(), "Brad" );

	}

	@DisplayName( "can default args" )
	@Test
	void testCanDefaultArgs() {
		Key			firstName	= Key.of( "firstName" );
		Key			lastName	= Key.of( "lastName" );
		Key			age			= Key.of( "age" );

		Argument[]	args		= new Argument[] {
		    new Argument( true, "String", firstName, "brad" ),
		    new Argument( true, "String", lastName, "wood" ),
		    new Argument( false, "Numeric", age, 43 )
		};
		Closure		closure		= new SampleClosure( args, new ScriptingRequestBoxContext(), "Brad" );
		IScope		argscope	= closure.createArgumentsScope( context );

		assertThat( argscope.get( firstName ) ).isEqualTo( "brad" );
		assertThat( argscope.get( lastName ) ).isEqualTo( "wood" );
		assertThat( argscope.size() ).isEqualTo( 3 );
		assertThat( argscope.get( age ) ).isEqualTo( 43 );
	}

	@DisplayName( "can process positional args" )
	@Test
	void testCanProcessPositionalArgs() {
		Key			firstName	= Key.of( "firstName" );
		Key			lastName	= Key.of( "lastName" );
		Argument[]	args		= new Argument[] {
		    new Argument( true, "String", firstName, "brad" ),
		    new Argument( true, "String", lastName, "wood" )
		};
		Closure		closure		= new SampleClosure( args, new ScriptingRequestBoxContext(), "Brad" );
		IScope		argscope	= closure.createArgumentsScope( context, new Object[] { "Luis", "Majano", "Extra" } );

		assertThat( argscope.get( firstName ) ).isEqualTo( "Luis" );
		assertThat( argscope.get( lastName ) ).isEqualTo( "Majano" );
		assertThat( argscope.size() ).isEqualTo( 3 );
		assertThat( argscope.get( Key.of( "3" ) ) ).isEqualTo( "Extra" );
	}

	@DisplayName( "can default missing positional args" )
	@Test
	void testCanDefaultMmissingPositionalArgs() {
		Key			firstName	= Key.of( "firstName" );
		Key			lastName	= Key.of( "lastName" );
		Argument[]	args		= new Argument[] {
		    new Argument( true, "String", firstName, "brad" ),
		    new Argument( true, "String", lastName, "wood" )
		};
		Closure		closure		= new SampleClosure( args, new ScriptingRequestBoxContext(), "Brad" );
		IScope		argscope	= closure.createArgumentsScope( context, new Object[] { "Luis" } );

		assertThat( argscope.get( firstName ) ).isEqualTo( "Luis" );
		assertThat( argscope.get( lastName ) ).isEqualTo( "wood" );
		assertThat( argscope.size() ).isEqualTo( 2 );
	}

	@DisplayName( "can process named args" )
	@Test
	void testCanProcessNamedArgs() {
		Key			firstName	= Key.of( "firstName" );
		Key			lastName	= Key.of( "lastName" );
		Key			extra		= Key.of( "extra" );
		Argument[]	args		= new Argument[] {
		    new Argument( true, "String", firstName, "brad" ),
		    new Argument( true, "String", lastName, "wood" )
		};
		Closure		closure		= new SampleClosure( args, new ScriptingRequestBoxContext(), "Brad" );
		IScope		argscope	= closure
		    .createArgumentsScope( context, Map.of( firstName, "Luis", lastName, "Majano", extra, "Gavin" ) );

		assertThat( argscope.get( firstName ) ).isEqualTo( "Luis" );
		assertThat( argscope.get( lastName ) ).isEqualTo( "Majano" );
		assertThat( argscope.size() ).isEqualTo( 3 );
		assertThat( argscope.get( extra ) ).isEqualTo( "Gavin" );
	}

	@DisplayName( "can reject invalid named arg types" )
	@Test
	void testCanRejectInvalidNamedArgTypes() {
		Key			age		= Key.of( "age" );
		Argument[]	args	= new Argument[] {
		    new Argument( true, "numeric", age, "sdf" )
		};
		Closure		closure	= new SampleClosure( args, new ScriptingRequestBoxContext(), "Brad" );

		// Explicit named arg
		assertThrows( Throwable.class, () -> closure.createArgumentsScope( context, Map.of( age, "sdf" ) ) );
		// Explicit positional arg
		assertThrows( Throwable.class, () -> closure.createArgumentsScope( context, new Object[] { "sdf" } ) );
		// Default postiional arg
		assertThrows( Throwable.class, () -> closure.createArgumentsScope( context ) );
		// Default named arg
		assertThrows( Throwable.class, () -> closure.createArgumentsScope( context, Map.of() ) );
	}

	@DisplayName( "can default missing named args" )
	@Test
	void testCanDefaultMmissingNamedArgs() {
		Key			firstName	= Key.of( "firstName" );
		Key			lastName	= Key.of( "lastName" );
		Argument[]	args		= new Argument[] {
		    new Argument( true, "String", firstName, "brad" ),
		    new Argument( true, "String", lastName, "wood" )
		};
		Closure		closure		= new SampleClosure( args, new ScriptingRequestBoxContext(), "Brad" );
		IScope		argscope	= closure
		    .createArgumentsScope( context, Map.of( firstName, "Luis" ) );

		assertThat( argscope.get( firstName ) ).isEqualTo( "Luis" );
		assertThat( argscope.get( lastName ) ).isEqualTo( "wood" );
		assertThat( argscope.size() ).isEqualTo( 2 );
	}

	@DisplayName( "can process argumentCollection" )
	@Test
	void testCanProcessArgumentCollection() {
		Key			firstName	= Key.of( "firstName" );
		Key			lastName	= Key.of( "lastName" );
		Key			extra		= Key.of( "extra" );
		Key			extraExtra	= Key.of( "extraExtra" );
		Argument[]	args		= new Argument[] {
		    new Argument( true, "String", firstName, "brad" ),
		    new Argument( true, "String", lastName, "wood" )
		};
		Closure		closure		= new SampleClosure( args, new ScriptingRequestBoxContext(), "Brad" );
		IScope		argscope	= closure
		    .createArgumentsScope( context, new HashMap<Key, Object>( Map.of(
		        Function.ARGUMENT_COLLECTION, Map.of( firstName, "Luis", lastName, "Majano", extra, "Gavin" ),
		        extraExtra, "Jorge"
		    ) ) );

		assertThat( argscope.get( firstName ) ).isEqualTo( "Luis" );
		assertThat( argscope.get( lastName ) ).isEqualTo( "Majano" );
		assertThat( argscope.size() ).isEqualTo( 4 );
		assertThat( argscope.get( extra ) ).isEqualTo( "Gavin" );
		assertThat( argscope.get( extraExtra ) ).isEqualTo( "Jorge" );
	}

	@DisplayName( "can override argumentCollection with args" )
	@Test
	void testOverrideArgumentCollectionWithArgs() {
		Key			firstName	= Key.of( "firstName" );
		Argument[]	args		= new Argument[] {
		    new Argument( true, "String", firstName, "brad" )
		};
		Closure		closure		= new SampleClosure( args, new ScriptingRequestBoxContext(), "Brad" );
		IScope		argscope	= closure
		    .createArgumentsScope( context, new HashMap<Key, Object>( Map.of(
		        Function.ARGUMENT_COLLECTION, Map.of( firstName, "from collection" ),
		        firstName, "top level"
		    ) ) );

		assertThat( argscope.get( firstName ) ).isEqualTo( "top level" );
	}

	@DisplayName( "errors for required arg" )
	@Test
	void testErrorsForRequired() {
		Key			firstName	= Key.of( "firstName" );
		Key			lastName	= Key.of( "lastName" );

		Argument[]	args		= new Argument[] {
		    new Argument( true, "String", firstName, null ),
		    new Argument( true, "String", lastName, null )
		};
		Closure		closure		= new SampleClosure( args, new ScriptingRequestBoxContext(), "Brad" );

		assertThrows( Throwable.class, () -> closure.createArgumentsScope( context ) );
		assertThrows( Throwable.class, () -> closure.createArgumentsScope( context, new Object[] { "Luis" } ) );
		assertThrows( Throwable.class, () -> closure.createArgumentsScope( context, Map.of( firstName, "Luis" ) ) );
	}

	@DisplayName( "can process no args" )
	@Test
	void testCanProcessNoArgs() {
		Key			firstName	= Key.of( "firstName" );
		Key			lastName	= Key.of( "lastName" );
		Argument[]	args		= new Argument[] {
		    new Argument( false, "String", firstName, "brad" ),
		    new Argument( false, "String", lastName, "wood" )
		};
		Closure		closure		= new SampleClosure( args, new ScriptingRequestBoxContext(), "Brad" );
		IScope		argscope	= closure.createArgumentsScope( context );

		assertThat( argscope.get( firstName ) ).isEqualTo( "brad" );
		assertThat( argscope.get( lastName ) ).isEqualTo( "wood" );
		assertThat( argscope.size() ).isEqualTo( 2 );
	}

	@DisplayName( "can get Closure metadata" )
	@Test
	void testCanGetClosureMetadata() {

		Key			firstName	= Key.of( "firstName" );
		Key			lastName	= Key.of( "lastName" );
		Argument[]	args		= new Argument[] {
		    new Argument( false, "String", Key.of( "firstname" ), "brad", Struct.of( "hint", "First Name" ) ),
		    new Argument( false, "String", lastName, "wood", Struct.of( "hint", "Last Name" ) )
		};
		Closure		closure		= new SampleClosure( args, new ScriptingRequestBoxContext(), "Brad" );

		IStruct		meta		= closure.getMetaData();
		assertThat( meta.get( Key.of( "name" ) ) ).isEqualTo( "Closure" );
		assertThat( meta.get( Key.of( "returnType" ) ) ).isEqualTo( "any" );
		assertThat( meta.get( Key.of( "output" ) ) ).isEqualTo( false );
		assertThat( meta.get( Key.of( "hint" ) ) ).isEqualTo( "" );
		assertThat( meta.get( Key.of( "access" ) ) ).isEqualTo( "public" );
		assertThat( meta.get( Key.of( "closure" ) ) ).isEqualTo( true );
		assertThat( meta.get( Key.of( "ANONYMOUSCLOSURE" ) ) ).isEqualTo( true );
		assertThat( meta.get( Key.of( "lambda" ) ) ).isEqualTo( false );
		assertThat( meta.get( Key.of( "ANONYMOUSLAMBDA" ) ) ).isEqualTo( false );

		Array arguments = ( Array ) meta.get( Key.of( "parameters" ) );
		assertThat( arguments.size() ).isEqualTo( 2 );

		IStruct arg1 = ( IStruct ) arguments.dereference( context, Key.of( "1" ), false );
		assertThat( arg1.get( Key.of( "name" ) ) ).isEqualTo( "firstname" );
		assertThat( arg1.get( Key.of( "required" ) ) ).isEqualTo( false );
		assertThat( arg1.get( Key.of( "type" ) ) ).isEqualTo( "String" );
		assertThat( arg1.get( Key.of( "default" ) ) ).isEqualTo( "brad" );
		assertThat( arg1.get( Key.of( "hint" ) ) ).isEqualTo( "First Name" );

		IStruct arg2 = ( IStruct ) arguments.dereference( context, Key.of( "2" ), false );
		assertThat( arg2.get( Key.of( "name" ) ) ).isEqualTo( "lastName" );
		assertThat( arg2.get( Key.of( "required" ) ) ).isEqualTo( false );
		assertThat( arg2.get( Key.of( "type" ) ) ).isEqualTo( "String" );
		assertThat( arg2.get( Key.of( "default" ) ) ).isEqualTo( "wood" );
		assertThat( arg2.get( Key.of( "hint" ) ) ).isEqualTo( "Last Name" );

	}

}
