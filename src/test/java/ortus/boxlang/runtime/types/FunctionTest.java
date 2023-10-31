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
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Function.Argument;

public class FunctionTest {

	@DisplayName( "can define UDF" )
	@Test
	void testCanDefineUDF() {
		Argument[] args = new Argument[] {
		    new Function.Argument( true, "String", Key.of( "firstName" ), "brad" ),
		    new Function.Argument( true, "String", Key.of( "lastName" ), "wood" )
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
		    new Function.Argument( true, "String", firstName, "brad" ),
		    new Function.Argument( true, "String", lastName, "wood" ),
		    new Function.Argument( false, "String", age, 43 )
		};
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );
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
		    new Function.Argument( true, "String", firstName, "brad" ),
		    new Function.Argument( true, "String", lastName, "wood" )
		};
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );
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
		    new Function.Argument( true, "String", firstName, "brad" ),
		    new Function.Argument( true, "String", lastName, "wood" )
		};
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );
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
		    new Function.Argument( true, "String", firstName, "brad" ),
		    new Function.Argument( true, "String", lastName, "wood" )
		};
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );
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
		    new Function.Argument( true, "numeric", age, "sdf" )
		};
		UDF			udf		= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );

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
		    new Function.Argument( true, "String", firstName, "brad" ),
		    new Function.Argument( true, "String", lastName, "wood" )
		};
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );
		IScope		argscope	= udf
		    .createArgumentsScope( Map.of( firstName, "Luis" ) );

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
		    new Function.Argument( true, "String", firstName, "brad" ),
		    new Function.Argument( true, "String", lastName, "wood" )
		};
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );
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

	@DisplayName( "can process argumentCollection Array" )
	@Test
	void testCanProcessArgumentCollectionArray() {
		Key			firstName	= Key.of( "firstName" );
		Key			lastName	= Key.of( "lastName" );
		Key			key3		= Key.of( "3" );
		Key			extraExtra	= Key.of( "extraExtra" );
		Argument[]	args		= new Argument[] {
		    new Function.Argument( true, "String", firstName, "brad" ),
		    new Function.Argument( true, "String", lastName, "wood" )
		};
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );
		IScope		argscope	= udf
		    .createArgumentsScope( new HashMap<Key, Object>( Map.of(
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
		    new Function.Argument( true, "String", param, null )
		};
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );
		IScope		argscope	= udf
		    .createArgumentsScope( new HashMap<Key, Object>( Map.of(
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
		    .createArgumentsScope( new HashMap<Key, Object>( Map.of(
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
		    .createArgumentsScope( new HashMap<Key, Object>( Map.of(
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
		    new Function.Argument( true, "String", firstName, "brad" )
		};
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );
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
		    new Function.Argument( true, "String", firstName, null ),
		    new Function.Argument( true, "String", lastName, null )
		};
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );

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
		    new Function.Argument( false, "String", firstName, "brad" ),
		    new Function.Argument( false, "String", lastName, "wood" )
		};
		UDF			udf			= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "any", args, null );
		IScope		argscope	= udf.createArgumentsScope();

		assertThat( argscope.get( firstName ) ).isEqualTo( "brad" );
		assertThat( argscope.get( lastName ) ).isEqualTo( "wood" );
		assertThat( argscope.size() ).isEqualTo( 2 );
	}

	@DisplayName( "can verify return types of functions" )
	@Test
	void testCanVerifyReturnTypes() {
		UDF					udf				= new SampleUDF( UDF.Access.PUBLIC, Key.of( "foo" ), "String", new Argument[] {}, "Brad" );
		ArgumentsScope		argscope		= udf.createArgumentsScope();

		IBoxContext			parentContext	= new ScriptingBoxContext();
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
		        new Function.Argument( true, "String", Key.of( "param1" ), null, Struct.of( "hint", "First Name" ) ),
		        new Function.Argument( false, "any", Key.of( "param2" ), "wood" )
		    },
		    "42",
		    Struct.of( "hint", "Brad's func", "output", false )
		);
		Struct	meta	= udf.getMetaData();
		assertThat( meta.dereference( Key.of( "name" ), false ) ).isEqualTo( Key.of( "foo" ) );
		assertThat( meta.dereference( Key.of( "returnType" ), false ) ).isEqualTo( "String" );
		assertThat( meta.dereference( Key.of( "output" ), false ) ).isEqualTo( false );
		assertThat( meta.dereference( Key.of( "hint" ), false ) ).isEqualTo( "Brad's func" );
		assertThat( meta.dereference( Key.of( "access" ), false ) ).isEqualTo( "private" );
		assertThat( meta.dereference( Key.of( "closure" ), false ) ).isEqualTo( false );
		assertThat( meta.dereference( Key.of( "ANONYMOUSCLOSURE" ), false ) ).isEqualTo( false );
		assertThat( meta.dereference( Key.of( "lambda" ), false ) ).isEqualTo( false );
		assertThat( meta.dereference( Key.of( "ANONYMOUSLAMBDA" ), false ) ).isEqualTo( false );

		Array arguments = ( Array ) meta.dereference( Key.of( "parameters" ), false );
		assertThat( arguments.size() ).isEqualTo( 2 );

		Struct arg1 = ( Struct ) arguments.dereference( Key.of( "1" ), false );
		assertThat( arg1.dereference( Key.of( "name" ), false ) ).isEqualTo( Key.of( "param1" ) );
		assertThat( arg1.dereference( Key.of( "required" ), false ) ).isEqualTo( true );
		assertThat( arg1.dereference( Key.of( "type" ), false ) ).isEqualTo( "String" );
		assertThat( arg1.dereference( Key.of( "default" ), false ) ).isEqualTo( null );
		assertThat( arg1.dereference( Key.of( "hint" ), false ) ).isEqualTo( "First Name" );

		Struct arg2 = ( Struct ) arguments.dereference( Key.of( "2" ), false );
		assertThat( arg2.dereference( Key.of( "name" ), false ) ).isEqualTo( Key.of( "param2" ) );
		assertThat( arg2.dereference( Key.of( "required" ), false ) ).isEqualTo( false );
		assertThat( arg2.dereference( Key.of( "type" ), false ) ).isEqualTo( "any" );
		assertThat( arg2.dereference( Key.of( "default" ), false ) ).isEqualTo( "wood" );
		assertThat( arg2.dereference( Key.of( "hint" ), false ) ).isEqualTo( "" );

	}

}
