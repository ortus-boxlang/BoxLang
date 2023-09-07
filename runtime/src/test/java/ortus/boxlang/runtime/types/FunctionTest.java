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

import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.TemplateBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Function.Argument;

public class FunctionTest {

	@DisplayName( "can define UDF" )
	@Test
	void testCanDefineUDF() {
		Argument[] args = new Argument[] {
		    new Function.Argument( true, "String", Key.of( "firstName" ), "brad", "First Name" ),
		    new Function.Argument( true, "String", Key.of( "lastName" ), "wood", "Last Name" )
		};
		new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, "Cool function", false, null );

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
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, "Cool function", false, null );
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
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, "Cool function", false, null );
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
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, "Cool function", false, null );
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
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, "Cool function", false, null );
		IScope		argscope	= udf
		    .createArgumentsScope( Map.of( firstName, "Luis", lastName, "Majano", extra, "Gavin" ) );

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
		    new Function.Argument( true, "numeric", age, "sdf", "Age" )
		};
		UDF			udf		= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, "Cool function", false, null );

		// Explicit named arg
		assertThrows( Throwable.class, () -> udf.createArgumentsScope( Map.of( age, "sdf" ) ) );
		// Explicit positional arg
		assertThrows( Throwable.class, () -> udf.createArgumentsScope( new Object[] { "sdf" } ) );
		// Default postiional arg
		assertThrows( Throwable.class, () -> udf.createArgumentsScope() );
		// Default named arg
		assertThrows( Throwable.class, () -> udf.createArgumentsScope( Map.of() ) );
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
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, "Cool function", false, null );
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
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, "Cool function", false, null );
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

	@DisplayName( "can override argumentCollection with args" )
	@Test
	void testOverrideArgumentCollectionWithArgs() {
		Key			firstName	= Key.of( "firstName" );
		Argument[]	args		= new Argument[] {
		    new Function.Argument( true, "String", firstName, "brad", "First Name" )
		};
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, "Cool function", false, null );
		IScope		argscope	= udf
		    .createArgumentsScope( new HashMap<Key, Object>( Map.of(
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
		    new Function.Argument( true, "String", firstName, null, "First Name" ),
		    new Function.Argument( true, "String", lastName, null, "Last Name" )
		};
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, "Cool function", false, null );

		assertThrows( Throwable.class, () -> udf.createArgumentsScope() );
		assertThrows( Throwable.class, () -> udf.createArgumentsScope( new Object[] { "Luis" } ) );
		assertThrows( Throwable.class, () -> udf.createArgumentsScope( Map.of( firstName, "Luis" ) ) );
	}

	@DisplayName( "can process no args" )
	@Test
	void testCanProcessNoArgs() {
		Key			firstName	= Key.of( "firstName" );
		Key			lastName	= Key.of( "lastName" );
		Argument[]	args		= new Argument[] {
		    new Function.Argument( false, "String", firstName, "brad", "First Name" ),
		    new Function.Argument( false, "String", lastName, "wood", "Last Name" )
		};
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, "Cool function", false, null );
		IScope		argscope	= udf.createArgumentsScope();

		assertThat( argscope.get( firstName ) ).isEqualTo( "brad" );
		assertThat( argscope.get( lastName ) ).isEqualTo( "wood" );
		assertThat( argscope.size() ).isEqualTo( 2 );
	}

	@DisplayName( "can verify return types of functions" )
	@Test
	void testCanVerifyReturnTypes() {
		UDF					udf				= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "String", new Argument[] {}, "",
		    false, "Brad" );
		ArgumentsScope		argscope		= udf.createArgumentsScope();

		IBoxContext			parentContext	= new TemplateBoxContext();
		FunctionBoxContext	context			= new FunctionBoxContext( parentContext, udf, argscope );
		Object				result			= udf.invoke( context );
		assertThat( result ).isEqualTo( "Brad" );

		udf		= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "integer", new Argument[] {}, "",
		    false, "42" );

		result	= udf.invoke( context );
		assertThat( result ).isEqualTo( "42" );

		final UDF badUdf = new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "numeric", new Argument[] {}, "",
		    false, "Luis" );

		// Can't function return value of string to numeric
		assertThrows( Throwable.class, () -> badUdf.invoke( context ) );
	}

}
