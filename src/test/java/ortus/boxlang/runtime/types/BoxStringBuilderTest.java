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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.dynamic.casters.StringCaster;

public class BoxStringBuilderTest {

	// -------------------------------------------------------------------------
	// Construction
	// -------------------------------------------------------------------------

	@DisplayName( "Default constructor creates an empty buffer" )
	@Test
	void testDefaultConstructor() {
		BoxStringBuilder sb = new BoxStringBuilder();
		assertThat( sb.length() ).isEqualTo( 0 );
		assertThat( sb.isEmpty() ).isTrue();
		assertThat( sb.toString() ).isEqualTo( "" );
	}

	@DisplayName( "String constructor seeds the buffer" )
	@Test
	void testStringConstructor() {
		BoxStringBuilder sb = new BoxStringBuilder( "Hello" );
		assertThat( sb.toString() ).isEqualTo( "Hello" );
		assertThat( sb.length() ).isEqualTo( 5 );
	}

	@DisplayName( "Null in string constructor is treated as empty string" )
	@Test
	void testNullConstructor() {
		BoxStringBuilder sb = new BoxStringBuilder( ( String ) null );
		assertThat( sb.toString() ).isEqualTo( "" );
	}

	@DisplayName( "Capacity constructor creates an empty buffer with given capacity" )
	@Test
	void testCapacityConstructor() {
		BoxStringBuilder sb = new BoxStringBuilder( 256 );
		assertThat( sb.isEmpty() ).isTrue();
	}

	@DisplayName( "Initial-value + capacity constructor seeds correctly" )
	@Test
	void testInitialValueAndCapacityConstructor() {
		BoxStringBuilder sb = new BoxStringBuilder( "Hi", 512 );
		assertThat( sb.toString() ).isEqualTo( "Hi" );
	}

	@DisplayName( "Java StringBuilder wrapper constructor shares the same underlying buffer" )
	@Test
	void testJavaStringBuilderWrapperConstructor() {
		StringBuilder		javaBuilder	= new StringBuilder( "abc" );
		BoxStringBuilder	sb			= new BoxStringBuilder( javaBuilder );

		sb.append( "123" );
		assertThat( javaBuilder.toString() ).isEqualTo( "abc123" );

		javaBuilder.append( "!" );
		assertThat( sb.toString() ).isEqualTo( "abc123!" );
	}

	@DisplayName( "Java StringBuilder wrapper constructor rejects null" )
	@Test
	void testJavaStringBuilderWrapperConstructorRejectsNull() {
		assertThrows( NullPointerException.class, () -> new BoxStringBuilder( ( StringBuilder ) null ) );
	}

	@DisplayName( "of() factory casts any value to string" )
	@Test
	void testOfFactory() {
		assertThat( BoxStringBuilder.of( "hello" ).toString() ).isEqualTo( "hello" );
		assertThat( BoxStringBuilder.of( 42 ).toString() ).isEqualTo( "42" );
		assertThat( BoxStringBuilder.of( null ).toString() ).isEqualTo( "" );
	}

	// -------------------------------------------------------------------------
	// IType contract
	// -------------------------------------------------------------------------

	@DisplayName( "getBoxTypeName returns 'StringBuilder'" )
	@Test
	void testGetBoxTypeName() {
		assertThat( new BoxStringBuilder().getBoxTypeName() ).isEqualTo( "StringBuilder" );
	}

	@DisplayName( "asString returns buffer content" )
	@Test
	void testAsString() {
		BoxStringBuilder sb = new BoxStringBuilder( "test" );
		assertThat( sb.asString() ).isEqualTo( "test" );
	}

	@DisplayName( "getBoxMeta returns metadata with type 'StringBuilder'" )
	@Test
	void testGetBoxMeta() {
		BoxStringBuilder sb = new BoxStringBuilder( "x" );
		assertThat( sb.getBoxMeta() ).isNotNull();
		assertThat( sb.getBoxMeta().getMeta().get( "type" ) ).isEqualTo( "StringBuilder" );
	}

	// -------------------------------------------------------------------------
	// hashCode / equals / compareTo
	// -------------------------------------------------------------------------

	@DisplayName( "equals: two builders with same content are equal" )
	@Test
	void testEqualsToBuilder() {
		BoxStringBuilder	a	= new BoxStringBuilder( "foo" );
		BoxStringBuilder	b	= new BoxStringBuilder( "foo" );
		assertThat( a.equals( b ) ).isTrue();
	}

	@DisplayName( "equals: compares equal to a plain String with the same content" )
	@Test
	void testEqualsToString() {
		BoxStringBuilder sb = new BoxStringBuilder( "foo" );
		assertThat( sb.equals( "foo" ) ).isTrue();
		assertThat( sb.equals( "bar" ) ).isFalse();
	}

	@DisplayName( "hashCode matches the content string's hashCode" )
	@Test
	void testHashCode() {
		BoxStringBuilder sb = new BoxStringBuilder( "hello" );
		assertThat( sb.hashCode() ).isEqualTo( "hello".hashCode() );
	}

	@DisplayName( "compareTo orders lexicographically" )
	@Test
	void testCompareTo() {
		BoxStringBuilder	a	= new BoxStringBuilder( "apple" );
		BoxStringBuilder	b	= new BoxStringBuilder( "banana" );
		assertThat( a.compareTo( b ) ).isLessThan( 0 );
		assertThat( b.compareTo( a ) ).isGreaterThan( 0 );
		assertThat( a.compareTo( new BoxStringBuilder( "apple" ) ) ).isEqualTo( 0 );
	}

	// -------------------------------------------------------------------------
	// Fluent mutations
	// -------------------------------------------------------------------------

	@DisplayName( "append adds text to the end" )
	@Test
	void testAppend() {
		BoxStringBuilder	sb			= new BoxStringBuilder( "Hello" );
		BoxStringBuilder	returned	= sb.append( " World" );
		assertThat( sb.toString() ).isEqualTo( "Hello World" );
		assertThat( returned ).isSameInstanceAs( sb );
	}

	@DisplayName( "append coerces non-string values" )
	@Test
	void testAppendNumber() {
		BoxStringBuilder sb = new BoxStringBuilder( "count=" );
		sb.append( 42 );
		assertThat( sb.toString() ).isEqualTo( "count=42" );
	}

	@DisplayName( "append treats null as empty string" )
	@Test
	void testAppendNull() {
		BoxStringBuilder sb = new BoxStringBuilder( "x" );
		sb.append( null );
		assertThat( sb.toString() ).isEqualTo( "x" );
	}

	@DisplayName( "prepend inserts at the start" )
	@Test
	void testPrepend() {
		BoxStringBuilder	sb			= new BoxStringBuilder( "World" );
		BoxStringBuilder	returned	= sb.prepend( "Hello " );
		assertThat( sb.toString() ).isEqualTo( "Hello World" );
		assertThat( returned ).isSameInstanceAs( sb );
	}

	@DisplayName( "insert places text at the given 1-based position" )
	@Test
	void testInsert() {
		BoxStringBuilder sb = new BoxStringBuilder( "HelloWorld" );
		sb.insert( 6, " " );
		assertThat( sb.toString() ).isEqualTo( "Hello World" );
	}

	@DisplayName( "insert at position 1 is equivalent to prepend" )
	@Test
	void testInsertAtOne() {
		BoxStringBuilder sb = new BoxStringBuilder( "World" );
		sb.insert( 1, "Hello " );
		assertThat( sb.toString() ).isEqualTo( "Hello World" );
	}

	@DisplayName( "delete removes the inclusive range" )
	@Test
	void testDelete() {
		BoxStringBuilder sb = new BoxStringBuilder( "Hello World" );
		sb.delete( 6, 11 );
		assertThat( sb.toString() ).isEqualTo( "Hello" );
	}

	@DisplayName( "replace substitutes the inclusive range" )
	@Test
	void testReplace() {
		BoxStringBuilder sb = new BoxStringBuilder( "Hello World" );
		sb.replace( 7, 11, "BoxLang" );
		assertThat( sb.toString() ).isEqualTo( "Hello BoxLang" );
	}

	@DisplayName( "reverse reverses the buffer in place" )
	@Test
	void testReverse() {
		BoxStringBuilder	sb			= new BoxStringBuilder( "abc" );
		BoxStringBuilder	returned	= sb.reverse();
		assertThat( sb.toString() ).isEqualTo( "cba" );
		assertThat( returned ).isSameInstanceAs( sb );
	}

	@DisplayName( "clear empties the buffer" )
	@Test
	void testClear() {
		BoxStringBuilder	sb			= new BoxStringBuilder( "Hello" );
		BoxStringBuilder	returned	= sb.clear();
		assertThat( sb.toString() ).isEqualTo( "" );
		assertThat( sb.isEmpty() ).isTrue();
		assertThat( returned ).isSameInstanceAs( sb );
	}

	@DisplayName( "trim strips whitespace from both ends" )
	@Test
	void testTrim() {
		BoxStringBuilder	sb			= new BoxStringBuilder( "  hello  " );
		BoxStringBuilder	returned	= sb.trim();
		assertThat( sb.toString() ).isEqualTo( "hello" );
		assertThat( returned ).isSameInstanceAs( sb );
	}

	@DisplayName( "fluent chaining works across multiple calls" )
	@Test
	void testChaining() {
		String result = new BoxStringBuilder()
		    .append( "Hello" )
		    .append( " " )
		    .append( "World" )
		    .trim()
		    .toString();
		assertThat( result ).isEqualTo( "Hello World" );
	}

	// -------------------------------------------------------------------------
	// StringCaster integration
	// -------------------------------------------------------------------------

	@DisplayName( "StringCaster casts BoxStringBuilder to its string content" )
	@Test
	void testStringCaster() {
		BoxStringBuilder	sb		= new BoxStringBuilder( "world" );
		String				result	= StringCaster.cast( sb );
		assertThat( result ).isEqualTo( "world" );
	}

}
