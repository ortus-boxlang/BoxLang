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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

class ListUtilTest {

	@Test
	void testInsertAt() {
		String	list	= "apple,banana,carrot";
		String	result	= ListUtil.insertAt( list, 2, "orange", "," );
		assertEquals( "apple,orange,banana,carrot", result );
	}

	@Test
	void testInsertAtOutOfBounds() {
		String list = "apple,banana,carrot";
		assertThrows( BoxRuntimeException.class, () -> {
			ListUtil.insertAt( list, 5, "orange", "," );
		} );
	}

	@Test
	void testDeleteAt() {
		String	list	= "apple,banana,carrot";
		String	result	= ListUtil.deleteAt( list, 2, "," );
		assertEquals( "apple,carrot", result );
	}

	@Test
	void testDeleteAtOutOfBounds() {
		String list = "apple,banana,carrot";
		assertThrows( BoxRuntimeException.class, () -> {
			ListUtil.deleteAt( list, 4, "," );
		} );
	}

	@Test
	void testAppend() {
		String	list	= "apple,banana,carrot";
		String	result	= ListUtil.append( list, "orange", "," );
		assertEquals( "apple,banana,carrot,orange", result );
	}

	@Test
	void testPrepend() {
		String	list	= "apple,banana,carrot";
		String	result	= ListUtil.prepend( list, "orange", "," );
		assertEquals( "orange,apple,banana,carrot", result );
	}

	@Test
	void testContains() {
		String	list	= "apple,banana,carrot";
		int		result	= ListUtil.indexOf( list, "banana", "," );
		assertEquals( 2, result );
	}

	@Test
	void testContainsNotThere() {
		String	list	= "apple,banana,carrot";
		int		result	= ListUtil.indexOf( list, "orange", "," );
		assertEquals( 0, result );
	}

	@Test
	void testContainsNoCase() {
		String	list	= "apple,banana,carrot";
		int		result	= ListUtil.indexOf( list, "BANANA", "," );
		assertEquals( 0, result );
	}

	@Test
	void testContainsNotThereNoCase() {
		String	list	= "apple,banana,carrot";
		int		result	= ListUtil.indexOfNoCase( list, "ORANGE", "," );
		assertEquals( 0, result );
	}

}