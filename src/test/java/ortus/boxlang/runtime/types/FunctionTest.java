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
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;

public class FunctionTest {

	private IBoxContext context = new ScriptingRequestBoxContext();

	@DisplayName( "can define UDF" )
	@Test
	void testCanDefineUDF() {
		Argument[] args = new Argument[] {
		    new Argument( true, "String", Key.of( "firstName" ), "brad" ),
		    new Argument( true, "String", Key.of( "lastName" ), "wood" )
		};
		new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );

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
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );
		IScope		argscope	= udf.createArgumentsScope( context );

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
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );
		IScope		argscope	= udf.createArgumentsScope( context, new Object[] { "Luis", "Majano", "Extra" } );

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
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );
		IScope		argscope	= udf.createArgumentsScope( context, new Object[] { "Luis" } );

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
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );
		IScope		argscope	= udf
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
		UDF			udf		= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );

		// Explicit named arg
		assertThrows( Throwable.class, () -> udf.createArgumentsScope( context, Map.of( age, "sdf" ) ) );
		// Explicit positional arg
		assertThrows( Throwable.class, () -> udf.createArgumentsScope( context, new Object[] { "sdf" } ) );
		// Default postiional arg
		assertThrows( Throwable.class, () -> udf.createArgumentsScope( context ) );
		// Default named arg
		assertThrows( Throwable.class, () -> udf.createArgumentsScope( context, Map.of() ) );
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
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );
		IScope		argscope	= udf
		    .createArgumentsScope( context, Map.of( firstName, "Luis" ) );

		assertThat( argscope.get( firstName ) ).isEqualTo( "Luis" );
		assertThat( argscope.get( lastName ) ).isEqualTo( "wood" );
		assertThat( argscope.size() ).isEqualTo( 2 );
	}

	@DisplayName( "can process argumentCollection Struct" )
	@Test
	void testCanProcessArgumentCollectionStruct() {
		Key			firstName	= Key.of( "firstName" );
		Key			lastName	= Key.of( "lastName" );
		Key			extra		= Key.of( "extra" );
		Key			extraExtra	= Key.of( "extraExtra" );
		Argument[]	args		= new Argument[] {
		    new Argument( true, "String", firstName, "brad" ),
		    new Argument( true, "String", lastName, "wood" )
		};
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );
		IScope		argscope	= udf
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

	@DisplayName( "can process argumentCollection Array" )
	@Test
	void testCanProcessArgumentCollectionArray() {
		Key			firstName	= Key.of( "firstName" );
		Key			lastName	= Key.of( "lastName" );
		Key			key3		= Key.of( "3" );
		Key			extraExtra	= Key.of( "extraExtra" );
		Argument[]	args		= new Argument[] {
		    new Argument( true, "String", firstName, "brad" ),
		    new Argument( true, "String", lastName, "wood" )
		};
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );
		IScope		argscope	= udf
		    .createArgumentsScope( context, new HashMap<Key, Object>( Map.of(
		        Function.ARGUMENT_COLLECTION, List.of( "Luis", "Majano", "Gavin" ),
		        extraExtra, "Jorge"
		    ) ) );

		assertThat( argscope.get( firstName ) ).isEqualTo( "Luis" );
		assertThat( argscope.get( lastName ) ).isEqualTo( "Majano" );
		assertThat( argscope.size() ).isEqualTo( 4 );
		assertThat( argscope.get( key3 ) ).isEqualTo( "Gavin" );
		assertThat( argscope.get( extraExtra ) ).isEqualTo( "Jorge" );
	}

	@DisplayName( "can process argumentCollection Array override" )
	@Test
	void testCanProcessArgumentCollectionArrayOverride() {
		Key			param		= Key.of( "param" );
		Key			key2		= Key.of( "2" );
		Argument[]	args		= new Argument[] {
		    new Argument( true, "String", param, null )
		};
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );
		IScope		argscope	= udf
		    .createArgumentsScope( context, new HashMap<Key, Object>( Map.of(
		        Function.ARGUMENT_COLLECTION, List.of( "foo", "bar" ),
		        param, "42"
		    ) ) );

		assertThat( argscope.size() ).isEqualTo( 2 );
		assertThat( argscope.get( param ) ).isEqualTo( "42" );
		assertThat( argscope.get( key2 ) ).isEqualTo( "bar" );
	}

	@DisplayName( "can process argumentCollection Struct override" )
	@Test
	void testCanProcessArgumentCollectionStructOverride() {
		Key			param		= Key.of( "param" );
		Argument[]	args		= new Argument[] {};
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );
		IScope		argscope	= udf
		    .createArgumentsScope( context, new HashMap<Key, Object>( Map.of(
		        Function.ARGUMENT_COLLECTION, Map.of( param, "foo" ),
		        param, "42"
		    ) ) );

		assertThat( argscope.size() ).isEqualTo( 1 );
		assertThat( argscope.get( param ) ).isEqualTo( "42" );
	}

	@DisplayName( "can ignore invalid argumentCollection" )
	@Test
	void testCanIgnoreInvalidArgumentCollection() {
		Argument[]	args		= new Argument[] {};
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );
		IScope		argscope	= udf
		    .createArgumentsScope( context, new HashMap<Key, Object>( Map.of(
		        Function.ARGUMENT_COLLECTION, "sdf"
		    ) ) );

		assertThat( argscope.size() ).isEqualTo( 1 );
		assertThat( argscope.get( Function.ARGUMENT_COLLECTION ) ).isEqualTo( "sdf" );
	}

	@DisplayName( "can override argumentCollection with args" )
	@Test
	void testOverrideArgumentCollectionWithArgs() {
		Key			firstName	= Key.of( "firstName" );
		Argument[]	args		= new Argument[] {
		    new Argument( true, "String", firstName, "brad" )
		};
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );
		IScope		argscope	= udf
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
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );

		assertThrows( Throwable.class, () -> udf.createArgumentsScope( context ) );
		assertThrows( Throwable.class, () -> udf.createArgumentsScope( context, new Object[] { "Luis" } ) );
		assertThrows( Throwable.class, () -> udf.createArgumentsScope( context, Map.of( firstName, "Luis" ) ) );
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
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );
		IScope		argscope	= udf.createArgumentsScope( context );

		assertThat( argscope.get( firstName ) ).isEqualTo( "brad" );
		assertThat( argscope.get( lastName ) ).isEqualTo( "wood" );
		assertThat( argscope.size() ).isEqualTo( 2 );
	}

	@DisplayName( "can verify return types of functions" )
	@Test
	void testCanVerifyReturnTypes() {
		UDF					udf				= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "String", new Argument[] {}, "Brad" );
		ArgumentsScope		argscope		= udf.createArgumentsScope( context );

		IBoxContext			parentContext	= new ScriptingRequestBoxContext();
		FunctionBoxContext	context			= new FunctionBoxContext( parentContext, udf, argscope );
		Object				result			= udf.invoke( context );
		assertThat( result ).isEqualTo( "Brad" );

		udf		= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "integer", new Argument[] {}, "42" );

		result	= udf.invoke( context );
		assertThat( result ).isEqualTo( "42" );

		final UDF badUdf = new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "numeric", new Argument[] {}, "Luis" );

		// Can't function return value of string to numeric
		assertThrows( Throwable.class, () -> badUdf.invoke( context ) );
	}

	@DisplayName( "can get UDF metadata" )
	@Test
	void testCanGetUDFMetadata() {
		UDF		udf		= new SampleUDF(
		    UDF.Access.PRIVATE,
		    Key.of( "foo" ),
		    "String",
		    new Argument[] {
		        new Argument( true, "String", Key.of( "param1" ), null, Struct.of( "hint", "First Name" ) ),
		        new Argument( false, "any", Key.of( "param2" ), "wood" )
		    },
		    "42",
		    Struct.of( "hint", "Brad's func", "output", false )
		);
		IStruct	meta	= udf.getMetaData();
		assertThat( meta.get( Key.of( "name" ) ) ).isEqualTo( "foo" );
		assertThat( meta.get( Key.of( "returnType" ) ) ).isEqualTo( "String" );
		assertThat( meta.get( Key.of( "output" ) ) ).isEqualTo( false );
		assertThat( meta.get( Key.of( "hint" ) ) ).isEqualTo( "Brad's func" );
		assertThat( meta.get( Key.of( "access" ) ) ).isEqualTo( "private" );
		assertThat( meta.get( Key.of( "closure" ) ) ).isEqualTo( false );
		assertThat( meta.get( Key.of( "ANONYMOUSCLOSURE" ) ) ).isEqualTo( false );
		assertThat( meta.get( Key.of( "lambda" ) ) ).isEqualTo( false );
		assertThat( meta.get( Key.of( "ANONYMOUSLAMBDA" ) ) ).isEqualTo( false );

		Array arguments = ( Array ) meta.get( Key.of( "parameters" ) );
		assertThat( arguments.size() ).isEqualTo( 2 );

		IStruct arg1 = ( IStruct ) arguments.dereference( context, Key.of( "1" ), false );
		assertThat( arg1.get( Key.of( "name" ) ) ).isEqualTo( "param1" );
		assertThat( arg1.get( Key.of( "required" ) ) ).isEqualTo( true );
		assertThat( arg1.get( Key.of( "type" ) ) ).isEqualTo( "String" );
		assertThat( arg1.get( Key.of( "default" ) ) ).isEqualTo( null );
		assertThat( arg1.get( Key.of( "hint" ) ) ).isEqualTo( "First Name" );

		IStruct arg2 = ( IStruct ) arguments.dereference( context, Key.of( "2" ), false );
		assertThat( arg2.get( Key.of( "name" ) ) ).isEqualTo( "param2" );
		assertThat( arg2.get( Key.of( "required" ) ) ).isEqualTo( false );
		assertThat( arg2.get( Key.of( "type" ) ) ).isEqualTo( "any" );
		assertThat( arg2.get( Key.of( "default" ) ) ).isEqualTo( "wood" );
		assertThat( arg2.get( Key.of( "hint" ) ) ).isEqualTo( "" );

	}

}
