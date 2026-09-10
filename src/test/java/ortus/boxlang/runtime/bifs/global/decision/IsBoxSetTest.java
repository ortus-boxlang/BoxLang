/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.decision;

import static com.google.common.truth.Truth.assertThat;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxSet;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

public class IsBoxSetTest {

	@DisplayName( "isBoxSet returns true for BoxSet instances" )
	@Test
	public void testIsBoxSetWithBoxSet() {
		BoxSet		set			= new BoxSet();
		IsBoxSet	isBifSet	= new IsBoxSet();

		assertThat( isBifSet.isBoxSet( set ) ).isTrue();
	}

	@DisplayName( "isBoxSet returns true for empty BoxSet" )
	@Test
	public void testIsBoxSetWithEmptyBoxSet() {
		BoxSet		set			= BoxSet.EMPTY;
		IsBoxSet	isBifSet	= new IsBoxSet();

		assertThat( isBifSet.isBoxSet( set ) ).isTrue();
	}

	@DisplayName( "isBoxSet returns true for BoxSet with values" )
	@Test
	public void testIsBoxSetWithPopulatedBoxSet() {
		BoxSet set = new BoxSet();
		set.add( "value1" );
		set.add( "value2" );
		set.add( 42 );
		IsBoxSet isBifSet = new IsBoxSet();

		assertThat( isBifSet.isBoxSet( set ) ).isTrue();
	}

	@DisplayName( "isBoxSet returns true for java.util.HashSet" )
	@Test
	public void testIsBoxSetWithHashSet() {
		Set<Object>	javaSet		= new HashSet<>();
		IsBoxSet	isBifSet	= new IsBoxSet();

		assertThat( isBifSet.isBoxSet( javaSet ) ).isTrue();
	}

	@DisplayName( "isBoxSet returns true for java.util.LinkedHashSet" )
	@Test
	public void testIsBoxSetWithLinkedHashSet() {
		Set<Object>	javaSet		= new LinkedHashSet<>();
		IsBoxSet	isBifSet	= new IsBoxSet();

		assertThat( isBifSet.isBoxSet( javaSet ) ).isTrue();
	}

	@DisplayName( "isBoxSet returns true for java.util.TreeSet" )
	@Test
	public void testIsBoxSetWithTreeSet() {
		Set<Object>	javaSet		= new TreeSet<>();
		IsBoxSet	isBifSet	= new IsBoxSet();

		assertThat( isBifSet.isBoxSet( javaSet ) ).isTrue();
	}

	@DisplayName( "isBoxSet returns false for null" )
	@Test
	public void testIsBoxSetWithNull() {
		IsBoxSet isBifSet = new IsBoxSet();

		assertThat( isBifSet.isBoxSet( null ) ).isFalse();
	}

	@DisplayName( "isBoxSet returns false for string" )
	@Test
	public void testIsBoxSetWithString() {
		IsBoxSet isBifSet = new IsBoxSet();

		assertThat( isBifSet.isBoxSet( "not a set" ) ).isFalse();
	}

	@DisplayName( "isBoxSet returns false for array" )
	@Test
	public void testIsBoxSetWithArray() {
		Array		array		= new Array();
		IsBoxSet	isBifSet	= new IsBoxSet();

		assertThat( isBifSet.isBoxSet( array ) ).isFalse();
	}

	@DisplayName( "isBoxSet returns false for struct" )
	@Test
	public void testIsBoxSetWithStruct() {
		IStruct		struct		= new Struct();
		IsBoxSet	isBifSet	= new IsBoxSet();

		assertThat( isBifSet.isBoxSet( struct ) ).isFalse();
	}

	@DisplayName( "isBoxSet returns false for number" )
	@Test
	public void testIsBoxSetWithNumber() {
		IsBoxSet isBifSet = new IsBoxSet();

		assertThat( isBifSet.isBoxSet( 42 ) ).isFalse();
	}

	@DisplayName( "isBoxSet returns false for boolean" )
	@Test
	public void testIsBoxSetWithBoolean() {
		IsBoxSet isBifSet = new IsBoxSet();

		assertThat( isBifSet.isBoxSet( true ) ).isFalse();
	}

}
