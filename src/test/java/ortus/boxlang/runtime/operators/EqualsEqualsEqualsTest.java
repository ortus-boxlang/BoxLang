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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class EqualsEqualsEqualsTest {

	@DisplayName( "It can compare objects" )
	@Test
	void testItCanCompareObjects() {
		assertThat( EqualsEqualsEquals.invoke( "Brad", "Brad" ) ).isTrue();
		assertThat( EqualsEqualsEquals.invoke( true, "true" ) ).isFalse();
		assertThat( EqualsEqualsEquals.invoke( true, true ) ).isTrue();
		assertThat( EqualsEqualsEquals.invoke( 1, "1" ) ).isFalse();
		assertThat( EqualsEqualsEquals.invoke( 1, 1 ) ).isTrue();
	}

	@DisplayName( "It can obey inheritance" )
	@Test
	void testItCanObeyInheritance() {
		Human	brad		= new Human( "Brad" );
		Human	otherBrad	= new Human( "Brad" );
		assertThat( EqualsEqualsEquals.invoke( brad, otherBrad ) ).isTrue();

		Man yetAnotherBrad = new Man( "Brad" );
		assertThat( EqualsEqualsEquals.invoke( brad, yetAnotherBrad ) ).isTrue();
		assertThat( EqualsEqualsEquals.invoke( yetAnotherBrad, brad ) ).isTrue();
	}

}

class Human implements Comparable<Human> {

	public String name;

	public Human( String name ) {
		this.name = name;
	}

	@Override
	public int compareTo( Human other ) {
		return this.name.equals( other.name ) ? 0 : 1;
	}

}

class Man extends Human {

	public Man( String name ) {
		super( name );
	}

}