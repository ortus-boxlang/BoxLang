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

import com.google.common.truth.ThrowableSubject;
import com.google.common.truth.Truth;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.CantCastException;

import static com.google.common.truth.Truth.assertThat;

public class StructTest {

	@DisplayName( "Test equals and hash code with no data" )
	@Test
	void testEqualsAndHashCode() {
		Struct	struct1	= new Struct();
		Struct	struct2	= new Struct();

		// Test equals()
		assertThat( struct1 ).isEqualTo( struct2 );

		// Test hashCode()
		assertThat( struct1.hashCode() ).isEqualTo( struct2.hashCode() );
	}

	@DisplayName( "Test equals and hash code with data" )
	@Test
	void testEqualsAndHashCodeWithData() {
		Struct struct1 = new Struct();
		struct1.put( Key.of( "name" ), "boxlang" );
		Struct struct2 = new Struct();
		struct2.put( Key.of( "name" ), "boxlang" );

		// Test equals()
		assertThat( struct1 ).isEqualTo( struct2 );

		// Test hashCode()
		assertThat( struct1.hashCode() ).isEqualTo( struct2.hashCode() );
	}

	@Test
	void testToString() {
		Struct struct = new Struct();
		struct.put( Key.of( "name" ), "BoxLang" );
		struct.put( Key.of( "AgE" ), 1 );
		struct.put( Key.of( "Location" ), "Spain" );

		// Test toString()
		String actual = struct.toString();
		System.out.println( actual );
		assertThat( actual ).contains( "NAME=BoxLang" );
		assertThat( actual ).contains( "AGE=1" );
		assertThat( actual ).contains( "LOCATION=Spain" );
	}

	@Test
	void testToStringWithCase() {
		Struct struct = new Struct();
		struct.put( Key.of( "name" ), "BoxLang" );
		struct.put( Key.of( "AgE" ), 1 );
		struct.put( Key.of( "Location" ), "Spain" );

		// Test toString()
		String actual = struct.toStringWithCase();
		System.out.println( actual );
		assertThat( actual ).contains( "name=BoxLang" );
		assertThat( actual ).contains( "AgE=1" );
		assertThat( actual ).contains( "Location=Spain" );
	}

	@Test
	void testAsStringThrowsException() {
		Struct				struct		= new Struct();

		// Test that the method throws the expected exception
		CantCastException	exception	= assertThrows( CantCastException.class, () -> {
											struct.asString();
										} );

		assertThat( exception.getMessage() ).isEqualTo( "Can't cast a struct to a string. Try serializing it" );
	}
}
