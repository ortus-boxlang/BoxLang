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
package ortus.boxlang.runtime.scopes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class IntKeyTest {

	@Test
	public void testEquals() {
		IntKey	key1	= new IntKey( 5 );
		IntKey	key2	= new IntKey( 5 );
		IntKey	key3	= new IntKey( 10 );

		assertTrue( key1.equals( key2 ), "Expected keys with same int value to be equal" );
		assertFalse( key1.equals( key3 ), "Expected keys with different int values to be unequal" );

		assertTrue( key1.equals( key1 ), "Expected key to be equal to itself" );

		assertFalse( key1.equals( null ), "Expected key to be unequal to null" );
		assertTrue( key1.equals( Integer.valueOf( 5 ) ), "Expected key to be equal to Integer with same value" );
	}

	@Test
	public void testStaticConstructors() {
		Key	key1	= Key.of( 5 );
		Key	key2	= Key.of( Double.valueOf( 5 ) );
		Key	key3	= Key.of( 10 );
		Key	key4	= Key.of( Double.valueOf( "3.14" ) );
		Key	key5	= Key.of( "5" );

		assertTrue( key1.equals( key2 ), "Expected keys with same int value to be equal" );
		assertFalse( key1.equals( key3 ), "Expected keys with different int values to be unequal" );

		assertTrue( key1.equals( key1 ), "Expected key to be equal to itself" );

		assertFalse( key1.equals( null ), "Expected key to be unequal to null" );
		assertTrue( key1.equals( Integer.valueOf( 5 ) ), "Expected key to be equal to Integer with same value" );

		assertFalse( key4 instanceof IntKey );
		assertTrue( key1.equals( key5 ) );
	}

	@Test
	public void testStaticConstructorsStringOptimizations() {
		Key	key1	= Key.of( "1" );
		Key	key2	= Key.of( "2" );
		Key	key3	= Key.of( "10" );
		Key	key4	= Key.of( "999" );
		Key	key5	= Key.of( "5234324234" );
		Key	key6	= Key.of( "5d" );

		assertTrue( key1 instanceof IntKey );
		assertTrue( key2 instanceof IntKey );
		assertTrue( key3 instanceof IntKey );
		assertTrue( key4 instanceof IntKey );
		assertFalse( key5 instanceof IntKey );
		assertFalse( key6 instanceof IntKey );
	}
}
