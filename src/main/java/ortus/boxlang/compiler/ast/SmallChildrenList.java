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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.RandomAccess;

/**
 * Mutable child list that stores its first two entries inline and spills larger lists into an {@link ArrayList}.
 */
final class SmallChildrenList extends AbstractList<BoxNode> implements RandomAccess {

	private BoxNode				first;
	private BoxNode				second;
	private ArrayList<BoxNode>	overflow;
	private int					size;

	@Override
	public BoxNode get( int index ) {
		checkElementIndex( index );
		if ( this.overflow != null ) {
			return this.overflow.get( index );
		}
		return index == 0 ? this.first : this.second;
	}

	@Override
	public int size() {
		return this.size;
	}

	@Override
	public BoxNode set( int index, BoxNode element ) {
		checkElementIndex( index );
		if ( this.overflow != null ) {
			return this.overflow.set( index, element );
		}
		BoxNode previous;
		if ( index == 0 ) {
			previous	= this.first;
			this.first	= element;
		} else {
			previous	= this.second;
			this.second	= element;
		}
		return previous;
	}

	@Override
	public void add( int index, BoxNode element ) {
		checkPositionIndex( index );
		if ( this.overflow != null ) {
			this.overflow.add( index, element );
		} else if ( this.size == 0 ) {
			this.first = element;
		} else if ( this.size == 1 ) {
			if ( index == 0 ) {
				this.second	= this.first;
				this.first	= element;
			} else {
				this.second = element;
			}
		} else {
			this.overflow = new ArrayList<>( 3 );
			this.overflow.add( this.first );
			this.overflow.add( this.second );
			this.first	= null;
			this.second	= null;
			this.overflow.add( index, element );
		}
		this.size++;
		this.modCount++;
	}

	@Override
	public BoxNode remove( int index ) {
		checkElementIndex( index );
		BoxNode previous;
		if ( this.overflow != null ) {
			previous = this.overflow.remove( index );
		} else if ( index == 0 ) {
			previous	= this.first;
			this.first	= this.second;
			this.second	= null;
		} else {
			previous	= this.second;
			this.second	= null;
		}
		this.size--;
		this.modCount++;
		return previous;
	}

	private void checkElementIndex( int index ) {
		if ( index < 0 || index >= this.size ) {
			throw new IndexOutOfBoundsException( index );
		}
	}

	private void checkPositionIndex( int index ) {
		if ( index < 0 || index > this.size ) {
			throw new IndexOutOfBoundsException( index );
		}
	}
}
