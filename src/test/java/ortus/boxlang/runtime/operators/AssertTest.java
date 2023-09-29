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
package ortus.boxlang.runtime.operators;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.SampleUDF;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.types.exceptions.BoxLangException;

public class AssertTest {

	@DisplayName( "It can assert throw exceptions on null values" )
	@Test
	void testItCanThrowOnNullValues() {
		assertThrows( AssertionError.class, () -> Assert.invoke( new ScriptingBoxContext(), null ) );
	}

	@DisplayName( "It can assert throw exceptions on false values" )
	@Test
	void testItCanThrowOnFalseValues() {
		assertThrows( AssertionError.class, () -> Assert.invoke( new ScriptingBoxContext(), false ) );
	}

	@DisplayName( "It can assert" )
	@Test
	void testItCanAssert() {
		assertThat( Assert.invoke( new ScriptingBoxContext(), true ) ).isTrue();
	}

	@DisplayName( "It can assert UDF" )
	@Test
	void testItCanAssertUDF() {
		UDF udf = new SampleUDF( UDF.Access.PUBLIC, Key.of( "func" ), "any", new Function.Argument[] {}, "", false, true );
		assertThat( Assert.invoke( new ScriptingBoxContext(), udf ) ).isTrue();

		final UDF udf2 = new SampleUDF( UDF.Access.PUBLIC, Key.of( "func" ), "any", new Function.Argument[] {}, "", false, false );
		assertThrows( AssertionError.class, () -> Assert.invoke( new ScriptingBoxContext(), udf2 ) );

		final UDF udf3 = new SampleUDF( UDF.Access.PUBLIC, Key.of( "func" ), "any", new Function.Argument[] {}, "", false, "brad" );
		assertThrows( BoxLangException.class, () -> Assert.invoke( new ScriptingBoxContext(), udf3 ) );
	}

}
