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
package ortus.boxlang.runtime.dynamic.casters;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.BoxStringBuilder;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;

public class StringBuilderCasterTest {

	@DisplayName( "It can cast a BoxStringBuilder" )
	@Test
	void testItCanCastABoxStringBuilder() {
		BoxStringBuilder bsb = new BoxStringBuilder( "hello" );
		assertThat( StringBuilderCaster.cast( bsb ) ).isSameInstanceAs( bsb );
	}

	@DisplayName( "It can cast a java StringBuilder" )
	@Test
	void testItCanCastAJavaStringBuilder() {
		StringBuilder		javaBuilder	= new StringBuilder( "hello" );
		BoxStringBuilder	casted		= StringBuilderCaster.cast( javaBuilder );
		assertThat( casted ).isNotNull();
		assertThat( casted.toString() ).isEqualTo( "hello" );

		casted.append( " world" );
		assertThat( javaBuilder.toString() ).isEqualTo( "hello world" );

		javaBuilder.append( "!" );
		assertThat( casted.toString() ).isEqualTo( "hello world!" );
	}

	@DisplayName( "It can attempt to cast a java StringBuilder" )
	@Test
	void testItCanAttemptToCastAJavaStringBuilder() {
		CastAttempt<BoxStringBuilder> attempt = StringBuilderCaster.attempt( new StringBuilder( "hello" ) );
		assertThat( attempt.wasSuccessful() ).isTrue();
		assertThat( attempt.get().toString() ).isEqualTo( "hello" );
	}

	@DisplayName( "It cannot cast null" )
	@Test
	void testItCanNotCastNull() {
		assertThrows( BoxCastException.class, () -> StringBuilderCaster.cast( null ) );
	}

}
