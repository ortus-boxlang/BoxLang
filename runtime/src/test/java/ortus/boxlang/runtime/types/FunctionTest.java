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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Function.Argument;
import ortus.boxlang.runtime.types.exceptions.CastException;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

import static com.google.common.truth.Truth.assertThat;

public class FunctionTest {

	@DisplayName( "can define UDF" )
	@Test
	void testCanDefineUDF() {
		Argument[]	args	= new Argument[] {
		    new Function.Argument( true, "String", Key.of( "firstName" ), "brad", "First Name" ),
		    new Function.Argument( true, "String", Key.of( "lastName" ), "wood", "Last Name" )
		};
		UDF			udf		= new func( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, "Cool function", false );

	}

	@DisplayName( "can default args" )
	@Test
	void testCanDefaultArgs() {
		Key			firstName	= Key.of( "firstName" );
		Key			lastName	= Key.of( "lastName" );
		Key			age			= Key.of( "age" );

		Argument[]	args		= new Argument[] {
		    new Function.Argument( true, "String", firstName, "brad", "First Name" ),
		    new Function.Argument( true, "String", lastName, "wood", "Last Name" ),
		    new Function.Argument( false, "String", age, 43, null )
		};
		UDF			udf			= new func( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, "Cool function", false );
		IScope		argscope	= udf.createArgumentsScope();

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
		    new Function.Argument( true, "String", firstName, "brad", "First Name" ),
		    new Function.Argument( true, "String", lastName, "wood", "Last Name" )
		};
		UDF			udf			= new func( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, "Cool function", false );
		IScope		argscope	= udf.createArgumentsScope( new Object[] { "Luis", "Majano", "Extra" } );

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
		    new Function.Argument( true, "String", firstName, "brad", "First Name" ),
		    new Function.Argument( true, "String", lastName, "wood", "Last Name" )
		};
		UDF			udf			= new func( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, "Cool function", false );
		IScope		argscope	= udf.createArgumentsScope( new Object[] { "Luis" } );

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
		    new Function.Argument( true, "String", firstName, "brad", "First Name" ),
		    new Function.Argument( true, "String", lastName, "wood", "Last Name" )
		};
		UDF			udf			= new func( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, "Cool function", false );
		IScope		argscope	= udf
		    .createArgumentsScope( Map.of( firstName, "Luis", lastName, "Majano", extra, "Gavin" ) );

		assertThat( argscope.get( firstName ) ).isEqualTo( "Luis" );
		assertThat( argscope.get( lastName ) ).isEqualTo( "Majano" );
		assertThat( argscope.size() ).isEqualTo( 3 );
		assertThat( argscope.get( extra ) ).isEqualTo( "Gavin" );
	}

	@DisplayName( "can default missing named args" )
	@Test
	void testCanDefaultMmissingNamedArgs() {
		Key			firstName	= Key.of( "firstName" );
		Key			lastName	= Key.of( "lastName" );
		Argument[]	args		= new Argument[] {
		    new Function.Argument( true, "String", firstName, "brad", "First Name" ),
		    new Function.Argument( true, "String", lastName, "wood", "Last Name" )
		};
		UDF			udf			= new func( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, "Cool function", false );
		IScope		argscope	= udf
		    .createArgumentsScope( Map.of( firstName, "Luis" ) );

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
		    new Function.Argument( true, "String", firstName, "brad", "First Name" ),
		    new Function.Argument( true, "String", lastName, "wood", "Last Name" )
		};
		UDF			udf			= new func( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, "Cool function", false );
		IScope		argscope	= udf
		    .createArgumentsScope( new HashMap<Key, Object>( Map.of(
		        Function.ARGUMENT_COLLECTION, Map.of( firstName, "Luis", lastName, "Majano", extra, "Gavin" ),
		        extraExtra, "Jorge"
		    ) ) );

		assertThat( argscope.get( firstName ) ).isEqualTo( "Luis" );
		assertThat( argscope.get( lastName ) ).isEqualTo( "Majano" );
		assertThat( argscope.size() ).isEqualTo( 4 );
		assertThat( argscope.get( extra ) ).isEqualTo( "Gavin" );
		assertThat( argscope.get( extraExtra ) ).isEqualTo( "Jorge" );
	}

	@DisplayName( "errors for required arg" )
	@Test
	void testErrorsForRequired() {
		Key			firstName	= Key.of( "firstName" );
		Key			lastName	= Key.of( "lastName" );

		Argument[]	args		= new Argument[] {
		    new Function.Argument( true, "String", firstName, null, "First Name" ),
		    new Function.Argument( true, "String", lastName, null, "Last Name" )
		};
		UDF			udf			= new func( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, "Cool function", false );

		assertThrows( Throwable.class, () -> udf.createArgumentsScope() );
		assertThrows( Throwable.class, () -> udf.createArgumentsScope( new Object[] { "Luis" } ) );
		assertThrows( Throwable.class, () -> udf.createArgumentsScope( Map.of( firstName, "Luis" ) ) );
	}

	class func extends UDF {

		public func( Access access, Key name, String returnType, Argument[] arguments, String description, boolean isAbstract ) {
			super( access, name, returnType, arguments, description, isAbstract );
		}

		@Override
		public Object invoke( FunctionBoxContext context ) {
			return null;
		}
	}

}
