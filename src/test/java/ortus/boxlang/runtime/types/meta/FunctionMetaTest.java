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
package ortus.boxlang.runtime.types.meta;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.SampleUDF;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class FunctionMetaTest {

	private IBoxContext context = new ScriptingBoxContext();

	@DisplayName( "Test function meta" )
	@Test
	void testFunctionMeta() {

		UDF				udf	= new SampleUDF(
		    UDF.Access.PUBLIC,
		    Key.of( "foo" ),
		    "String",
		    new Argument[] {
		        new Argument(
		            true,
		            "String",
		            Key.of( "param1" ),
		            "my default",
		            Struct.of( "inject", "myService" ),
		            Struct.of( "hint", "First Name" )
		        )
		    },
		    "Brad",
		    // annotations
		    Struct.of(
		        "foo", "bar",
		        "output", true
		    ),
		    // documentation
		    Struct.of(
		        "hint", "my Function",
		        "author", "Brad Wood",
		        "returns", "awesomeness"
		    )
		);
		FunctionMeta	$bx	= ( FunctionMeta ) Referencer.get( context, udf, BoxMeta.key, false );

		assertThat( $bx.$class ).isEqualTo( SampleUDF.class );
		assertThat( $bx.meta instanceof Struct ).isTrue();
		assertThat( $bx.meta.containsKey( "name" ) ).isTrue();
		assertThat( $bx.meta.containsKey( "access" ) ).isTrue();
		assertThat( $bx.meta.containsKey( "returnType" ) ).isTrue();
		assertThat( $bx.meta.containsKey( "parameters" ) ).isTrue();
		assertThat( $bx.meta.containsKey( "annotations" ) ).isTrue();
		assertThat( $bx.meta.containsKey( "documentation" ) ).isTrue();
		assertThat( $bx.meta.containsKey( "closure" ) ).isTrue();
		assertThat( $bx.meta.containsKey( "lambda" ) ).isTrue();

		assertThat( $bx.meta.get( "name" ) ).isEqualTo( "foo" );
		assertThat( $bx.meta.get( "access" ) ).isEqualTo( "public" );
		assertThat( $bx.meta.get( "returnType" ) ).isEqualTo( "String" );
		assertThat( $bx.meta.get( "closure" ) ).isEqualTo( false );
		assertThat( $bx.meta.get( "lambda" ) ).isEqualTo( false );

		assertThat( $bx.meta.get( "parameters" ) instanceof Array ).isTrue();
		assertThat( $bx.meta.get( "annotations" ) instanceof Struct ).isTrue();
		assertThat( $bx.meta.get( "documentation" ) instanceof Struct ).isTrue();

		Struct annotations = ( Struct ) $bx.meta.get( "annotations" );
		assertThat( annotations.containsKey( "foo" ) ).isTrue();
		assertThat( annotations.containsKey( "output" ) ).isTrue();
		assertThat( annotations.get( "foo" ) ).isEqualTo( "bar" );
		assertThat( annotations.get( "output" ) ).isEqualTo( true );

		Struct documentation = ( Struct ) $bx.meta.get( "documentation" );
		assertThat( documentation.containsKey( "hint" ) ).isTrue();
		assertThat( documentation.containsKey( "author" ) ).isTrue();
		assertThat( documentation.containsKey( "returns" ) ).isTrue();
		assertThat( documentation.get( "hint" ) ).isEqualTo( "my Function" );
		assertThat( documentation.get( "author" ) ).isEqualTo( "Brad Wood" );
		assertThat( documentation.get( "returns" ) ).isEqualTo( "awesomeness" );

		Array params = ( Array ) $bx.meta.get( "parameters" );
		assertThat( params.size() ).isEqualTo( 1 );
		Struct param = ( Struct ) params.get( 0 );

		assertThat( param.containsKey( "name" ) ).isTrue();
		assertThat( param.containsKey( "required" ) ).isTrue();
		assertThat( param.containsKey( "type" ) ).isTrue();
		assertThat( param.containsKey( "default" ) ).isTrue();
		assertThat( param.containsKey( "documentation" ) ).isTrue();
		assertThat( param.containsKey( "annotations" ) ).isTrue();

		assertThat( param.get( "name" ) ).isEqualTo( "param1" );
		assertThat( param.get( "required" ) ).isEqualTo( true );
		assertThat( param.get( "type" ) ).isEqualTo( "String" );
		assertThat( param.get( "default" ) ).isEqualTo( "my default" );
		assertThat( param.get( "documentation" ) instanceof Struct ).isTrue();
		assertThat( param.get( "annotations" ) instanceof Struct ).isTrue();

		annotations = ( Struct ) param.get( "annotations" );
		assertThat( annotations.containsKey( "inject" ) ).isTrue();
		assertThat( annotations.get( "inject" ) ).isEqualTo( "myService" );

		documentation = ( Struct ) param.get( "documentation" );
		assertThat( documentation.containsKey( "hint" ) ).isTrue();
		assertThat( documentation.get( "hint" ) ).isEqualTo( "First Name" );
	}

	@DisplayName( "Test function listener" )
	@Test
	void testFunctionListener() {

		UDF				udf	= new SampleUDF(
		    UDF.Access.PUBLIC,
		    Key.of( "foo" ),
		    "String",
		    new Argument[] {},
		    "Brad"
		);
		FunctionMeta	$bx	= ( FunctionMeta ) Referencer.get( context, udf, BoxMeta.key, false );

		// A function is not listenable
		assertThrows( BoxRuntimeException.class, () -> $bx.registerChangeListener( ( key, newValue, oldValue ) -> {
			return newValue;
		} ) );

	}

}
