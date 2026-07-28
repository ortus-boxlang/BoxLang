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
package ortus.boxlang.compiler.ast;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ConcurrentModificationException;
import java.util.List;

import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.ast.expression.BoxIdentifier;

class SmallChildrenListTest {

	@Test
	void supportsInlineAndOverflowMutations() {
		SmallChildrenList	children	= new SmallChildrenList();
		BoxNode				first		= identifier( "first" );
		BoxNode				second		= identifier( "second" );
		BoxNode				third		= identifier( "third" );
		BoxNode				replacement	= identifier( "replacement" );

		children.add( first );
		children.add( 0, second );
		assertThat( children ).containsExactly( second, first ).inOrder();

		assertThat( children.set( 1, replacement ) ).isSameInstanceAs( first );
		children.add( third );
		assertThat( children ).containsExactly( second, replacement, third ).inOrder();

		assertThat( children.remove( 1 ) ).isSameInstanceAs( replacement );
		children.addAll( 1, List.of( first, replacement ) );
		assertThat( children ).containsExactly( second, first, replacement, third ).inOrder();
		children.removeIf( child -> child == first || child == third );
		assertThat( children ).containsExactly( second, replacement ).inOrder();
	}

	@Test
	void supportsSortingAndIteratorMutation() {
		SmallChildrenList children = new SmallChildrenList();
		children.addAll( List.of( identifier( "c" ), identifier( "a" ), identifier( "b" ) ) );

		children.sort( ( left, right ) -> ( ( BoxIdentifier ) left ).getName().compareTo( ( ( BoxIdentifier ) right ).getName() ) );
		var iterator = children.listIterator();
		assertThat( ( ( BoxIdentifier ) iterator.next() ).getName() ).isEqualTo( "a" );
		iterator.remove();

		assertThat( children.stream().map( child -> ( ( BoxIdentifier ) child ).getName() ).toList() ).containsExactly( "b", "c" ).inOrder();
	}

	@Test
	void iteratorsRemainFailFast() {
		SmallChildrenList children = new SmallChildrenList();
		children.add( identifier( "first" ) );
		var iterator = children.iterator();

		children.add( identifier( "second" ) );

		assertThrows( ConcurrentModificationException.class, iterator::next );
	}

	@Test
	void supportsInheritedListOperationsAfterOverflow() {
		SmallChildrenList	children	= new SmallChildrenList();
		BoxNode				first		= identifier( "first" );
		BoxNode				second		= identifier( "second" );
		BoxNode				third		= identifier( "third" );
		children.addAll( List.of( first, second, third ) );

		children.subList( 1, 3 ).clear();
		children.listIterator().add( second );
		children.add( third );
		children.retainAll( List.of( first, third ) );
		children.replaceAll( child -> child == first ? second : child );

		assertThat( children ).containsExactly( second, third ).inOrder();
		children.clear();
		assertThat( children ).isEmpty();
	}

	@Test
	void validatesIndexesLikeAStandardList() {
		SmallChildrenList children = new SmallChildrenList();

		assertThrows( IndexOutOfBoundsException.class, () -> children.get( 0 ) );
		assertThrows( IndexOutOfBoundsException.class, () -> children.add( 1, identifier( "value" ) ) );
	}

	private BoxIdentifier identifier( String name ) {
		return new BoxIdentifier( name, null, name );
	}
}
