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
package ortus.boxlang.runtime.util;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class NameValuePairTest {

	@Test
	@DisplayName( "Test creating pair with name and value" )
	public void testCreateWithNameAndValue() {
		NameValuePair pair = new NameValuePair( "Content-Type", "application/json" );

		assertThat( pair.getName() ).isEqualTo( "Content-Type" );
		assertThat( pair.getValue() ).isEqualTo( "application/json" );
	}

	@Test
	@DisplayName( "Test creating pair with name only (null value)" )
	public void testCreateWithNameOnly() {
		NameValuePair pair = new NameValuePair( "verbose", null );

		assertThat( pair.getName() ).isEqualTo( "verbose" );
		assertNull( pair.getValue() );
	}

	@Test
	@DisplayName( "Test creating pair with empty value string" )
	public void testCreateWithEmptyValue() {
		NameValuePair pair = new NameValuePair( "emptyParam", "" );

		assertThat( pair.getName() ).isEqualTo( "emptyParam" );
		assertThat( pair.getValue() ).isEqualTo( "" );
		assertThat( pair.getValue() ).isNotNull();
	}

	@Test
	@DisplayName( "Test fromNativeArray with two-element array" )
	public void testFromNativeArrayWithTwoElements() {
		String[]		array	= new String[] { "Authorization", "Bearer token123" };
		NameValuePair	pair	= NameValuePair.fromNativeArray( array );

		assertThat( pair.getName() ).isEqualTo( "Authorization" );
		assertThat( pair.getValue() ).isEqualTo( "Bearer token123" );
	}

	@Test
	@DisplayName( "Test fromNativeArray with single-element array" )
	public void testFromNativeArrayWithOneElement() {
		String[]		array	= new String[] { "flagParam" };
		NameValuePair	pair	= NameValuePair.fromNativeArray( array );

		assertThat( pair.getName() ).isEqualTo( "flagParam" );
		assertNull( pair.getValue() );
	}

	@Test
	@DisplayName( "Test fromNativeArray with array longer than 2 elements" )
	public void testFromNativeArrayWithMultipleElements() {
		// Should only use first two elements
		String[]		array	= new String[] { "key", "value", "extra", "ignored" };
		NameValuePair	pair	= NameValuePair.fromNativeArray( array );

		assertThat( pair.getName() ).isEqualTo( "key" );
		assertThat( pair.getValue() ).isEqualTo( "value" );
	}

	@Test
	@DisplayName( "Test fromNativeArray with empty array throws exception" )
	public void testFromNativeArrayWithEmptyArray() {
		String[] emptyArray = new String[] {};

		assertThrows( ArrayIndexOutOfBoundsException.class, () -> {
			NameValuePair.fromNativeArray( emptyArray );
		} );
	}

	@Test
	@DisplayName( "Test fromNativeArray with split string (common use case)" )
	public void testFromNativeArrayWithSplitString() {
		// Simulate parsing "key=value" format
		String			input	= "Content-Type=application/json";
		String[]		parts	= input.split( "=", 2 );
		NameValuePair	pair	= NameValuePair.fromNativeArray( parts );

		assertThat( pair.getName() ).isEqualTo( "Content-Type" );
		assertThat( pair.getValue() ).isEqualTo( "application/json" );
	}

	@Test
	@DisplayName( "Test fromNativeArray with split string containing no delimiter" )
	public void testFromNativeArrayWithSplitStringNoDelimiter() {
		// Simulate parsing a flag-style parameter
		String			input	= "verbose";
		String[]		parts	= input.split( "=", 2 );
		NameValuePair	pair	= NameValuePair.fromNativeArray( parts );

		assertThat( pair.getName() ).isEqualTo( "verbose" );
		assertNull( pair.getValue() );
	}

	@Test
	@DisplayName( "Test toString with value present" )
	public void testToStringWithValue() {
		NameValuePair pair = new NameValuePair( "User-Agent", "BoxLang/1.0" );

		assertThat( pair.toString() ).isEqualTo( "User-Agent=BoxLang/1.0" );
	}

	@Test
	@DisplayName( "Test toString with null value" )
	public void testToStringWithNullValue() {
		NameValuePair pair = new NameValuePair( "debug", null );

		assertThat( pair.toString() ).isEqualTo( "debug" );
	}

	@Test
	@DisplayName( "Test toString with empty value" )
	public void testToStringWithEmptyValue() {
		NameValuePair pair = new NameValuePair( "emptyParam", "" );

		assertThat( pair.toString() ).isEqualTo( "emptyParam=" );
	}

	@Test
	@DisplayName( "Test toString with value containing equals sign" )
	public void testToStringWithEqualsInValue() {
		NameValuePair pair = new NameValuePair( "equation", "x=y+z" );

		assertThat( pair.toString() ).isEqualTo( "equation=x=y+z" );
	}

	@Test
	@DisplayName( "Test toString with special characters" )
	public void testToStringWithSpecialCharacters() {
		NameValuePair pair = new NameValuePair( "message", "Hello, World!" );

		assertThat( pair.toString() ).isEqualTo( "message=Hello, World!" );
	}

	@Test
	@DisplayName( "Test toString with unicode characters" )
	public void testToStringWithUnicode() {
		NameValuePair pair = new NameValuePair( "greeting", "你好世界" );

		assertThat( pair.toString() ).isEqualTo( "greeting=你好世界" );
	}

	@Test
	@DisplayName( "Test immutability - name cannot be changed" )
	public void testImmutabilityName() {
		NameValuePair	pair	= new NameValuePair( "original", "value" );

		// Attempting to modify returned name should not affect the pair
		String			name	= pair.getName();
		assertThat( name ).isEqualTo( "original" );

		// Since String is immutable, we verify the getter returns the same reference
		assertThat( pair.getName() ).isSameInstanceAs( name );
	}

	@Test
	@DisplayName( "Test immutability - value cannot be changed" )
	public void testImmutabilityValue() {
		NameValuePair	pair	= new NameValuePair( "name", "originalValue" );

		// Verify value remains unchanged
		String			value	= pair.getValue();
		assertThat( value ).isEqualTo( "originalValue" );
		assertThat( pair.getValue() ).isSameInstanceAs( value );
	}

	@Test
	@DisplayName( "Test HTTP header use case" )
	public void testHttpHeaderUseCase() {
		NameValuePair contentType = new NameValuePair( "Content-Type", "text/html; charset=utf-8" );

		assertThat( contentType.getName() ).isEqualTo( "Content-Type" );
		assertThat( contentType.getValue() ).isEqualTo( "text/html; charset=utf-8" );
		assertThat( contentType.toString() ).isEqualTo( "Content-Type=text/html; charset=utf-8" );
	}

	@Test
	@DisplayName( "Test query parameter use case" )
	public void testQueryParameterUseCase() {
		NameValuePair param = new NameValuePair( "page", "1" );

		assertThat( param.getName() ).isEqualTo( "page" );
		assertThat( param.getValue() ).isEqualTo( "1" );
		assertThat( param.toString() ).isEqualTo( "page=1" );
	}

	@Test
	@DisplayName( "Test form field use case" )
	public void testFormFieldUseCase() {
		NameValuePair field = new NameValuePair( "username", "john.doe" );

		assertThat( field.getName() ).isEqualTo( "username" );
		assertThat( field.getValue() ).isEqualTo( "john.doe" );
	}

	@Test
	@DisplayName( "Test boolean flag use case" )
	public void testBooleanFlagUseCase() {
		NameValuePair flag = new NameValuePair( "verbose", null );

		assertThat( flag.getName() ).isEqualTo( "verbose" );
		assertNull( flag.getValue() );
		assertThat( flag.toString() ).isEqualTo( "verbose" );
	}

	@Test
	@DisplayName( "Test whitespace in name and value" )
	public void testWhitespaceHandling() {
		NameValuePair pair = new NameValuePair( " name ", " value " );

		// Should preserve whitespace as-is (no trimming)
		assertThat( pair.getName() ).isEqualTo( " name " );
		assertThat( pair.getValue() ).isEqualTo( " value " );
		assertThat( pair.toString() ).isEqualTo( " name = value " );
	}

	@Test
	@DisplayName( "Test case sensitivity" )
	public void testCaseSensitivity() {
		NameValuePair	lowercase	= new NameValuePair( "content-type", "text/plain" );
		NameValuePair	uppercase	= new NameValuePair( "CONTENT-TYPE", "text/plain" );

		// Names should be case-sensitive (no normalization)
		assertThat( lowercase.getName() ).isNotEqualTo( uppercase.getName() );
	}

	@Test
	@DisplayName( "Test multiple pairs with same name" )
	public void testMultiplePairsWithSameName() {
		// Valid use case: multiple headers or parameters with same name
		NameValuePair	cookie1	= new NameValuePair( "Cookie", "session=abc" );
		NameValuePair	cookie2	= new NameValuePair( "Cookie", "user=john" );

		assertThat( cookie1.getName() ).isEqualTo( cookie2.getName() );
		assertThat( cookie1.getValue() ).isNotEqualTo( cookie2.getValue() );
	}

	@Test
	@DisplayName( "Test fromNativeArray with value containing equals" )
	public void testFromNativeArrayWithEqualsInValue() {
		// Simulates parsing "key=value1=value2" with limit
		String			input	= "equation=x=y+z";
		String[]		parts	= input.split( "=", 2 );
		NameValuePair	pair	= NameValuePair.fromNativeArray( parts );

		assertThat( pair.getName() ).isEqualTo( "equation" );
		assertThat( pair.getValue() ).isEqualTo( "x=y+z" );
	}

	@Test
	@DisplayName( "Test getName returns non-null" )
	public void testGetNameNeverNull() {
		NameValuePair pair = new NameValuePair( "test", "value" );

		assertNotNull( pair.getName() );
	}

	@Test
	@DisplayName( "Test creating many pairs (no memory leaks)" )
	public void testCreatingManyPairs() {
		// Create many pairs to verify no issues with construction
		for ( int i = 0; i < 1000; i++ ) {
			NameValuePair pair = new NameValuePair( "param" + i, "value" + i );
			assertThat( pair.getName() ).isEqualTo( "param" + i );
			assertThat( pair.getValue() ).isEqualTo( "value" + i );
		}
	}

	// ============================================
	// equals() and hashCode() tests
	// ============================================

	@Test
	@DisplayName( "Test equals with identical pairs" )
	public void testEqualsIdentical() {
		NameValuePair	pair1	= new NameValuePair( "key", "value" );
		NameValuePair	pair2	= new NameValuePair( "key", "value" );

		assertThat( pair1.equals( pair2 ) ).isTrue();
		assertThat( pair2.equals( pair1 ) ).isTrue();
	}

	@Test
	@DisplayName( "Test equals with same instance (reflexive)" )
	public void testEqualsSameInstance() {
		NameValuePair pair = new NameValuePair( "key", "value" );

		assertThat( pair.equals( pair ) ).isTrue();
	}

	@Test
	@DisplayName( "Test equals with null" )
	public void testEqualsNull() {
		NameValuePair pair = new NameValuePair( "key", "value" );

		assertThat( pair.equals( null ) ).isFalse();
	}

	@Test
	@DisplayName( "Test equals with different type" )
	public void testEqualsDifferentType() {
		NameValuePair pair = new NameValuePair( "key", "value" );

		assertThat( pair.equals( "key=value" ) ).isFalse();
		assertThat( pair.equals( new Object() ) ).isFalse();
	}

	@Test
	@DisplayName( "Test equals with different names" )
	public void testEqualsDifferentNames() {
		NameValuePair	pair1	= new NameValuePair( "key1", "value" );
		NameValuePair	pair2	= new NameValuePair( "key2", "value" );

		assertThat( pair1.equals( pair2 ) ).isFalse();
	}

	@Test
	@DisplayName( "Test equals with different values" )
	public void testEqualsDifferentValues() {
		NameValuePair	pair1	= new NameValuePair( "key", "value1" );
		NameValuePair	pair2	= new NameValuePair( "key", "value2" );

		assertThat( pair1.equals( pair2 ) ).isFalse();
	}

	@Test
	@DisplayName( "Test equals with both null values" )
	public void testEqualsBothNullValues() {
		NameValuePair	pair1	= new NameValuePair( "key", null );
		NameValuePair	pair2	= new NameValuePair( "key", null );

		assertThat( pair1.equals( pair2 ) ).isTrue();
	}

	@Test
	@DisplayName( "Test equals with one null value" )
	public void testEqualsOneNullValue() {
		NameValuePair	pair1	= new NameValuePair( "key", "value" );
		NameValuePair	pair2	= new NameValuePair( "key", null );

		assertThat( pair1.equals( pair2 ) ).isFalse();
		assertThat( pair2.equals( pair1 ) ).isFalse();
	}

	@Test
	@DisplayName( "Test equals is case-sensitive for names" )
	public void testEqualsCaseSensitiveNames() {
		NameValuePair	pair1	= new NameValuePair( "Key", "value" );
		NameValuePair	pair2	= new NameValuePair( "key", "value" );

		assertThat( pair1.equals( pair2 ) ).isFalse();
	}

	@Test
	@DisplayName( "Test equals is case-sensitive for values" )
	public void testEqualsCaseSensitiveValues() {
		NameValuePair	pair1	= new NameValuePair( "key", "Value" );
		NameValuePair	pair2	= new NameValuePair( "key", "value" );

		assertThat( pair1.equals( pair2 ) ).isFalse();
	}

	@Test
	@DisplayName( "Test hashCode consistency" )
	public void testHashCodeConsistency() {
		NameValuePair	pair		= new NameValuePair( "key", "value" );

		int				hashCode1	= pair.hashCode();
		int				hashCode2	= pair.hashCode();

		assertThat( hashCode1 ).isEqualTo( hashCode2 );
	}

	@Test
	@DisplayName( "Test hashCode equal pairs have equal hash codes" )
	public void testHashCodeEqualPairs() {
		NameValuePair	pair1	= new NameValuePair( "key", "value" );
		NameValuePair	pair2	= new NameValuePair( "key", "value" );

		assertThat( pair1.hashCode() ).isEqualTo( pair2.hashCode() );
	}

	@Test
	@DisplayName( "Test hashCode different pairs typically have different hash codes" )
	public void testHashCodeDifferentPairs() {
		NameValuePair	pair1	= new NameValuePair( "key1", "value" );
		NameValuePair	pair2	= new NameValuePair( "key2", "value" );

		// While hash collisions are possible, these should be different
		assertThat( pair1.hashCode() ).isNotEqualTo( pair2.hashCode() );
	}

	@Test
	@DisplayName( "Test hashCode with null value" )
	public void testHashCodeWithNullValue() {
		NameValuePair	pair1	= new NameValuePair( "key", null );
		NameValuePair	pair2	= new NameValuePair( "key", null );

		// Equal pairs should have equal hash codes
		assertThat( pair1.hashCode() ).isEqualTo( pair2.hashCode() );
	}

	@Test
	@DisplayName( "Test hashCode different when one has null value" )
	public void testHashCodeDifferentNullValue() {
		NameValuePair	pair1	= new NameValuePair( "key", "value" );
		NameValuePair	pair2	= new NameValuePair( "key", null );

		// These pairs are not equal, so hash codes should typically differ
		assertThat( pair1.hashCode() ).isNotEqualTo( pair2.hashCode() );
	}

	@Test
	@DisplayName( "Test use in HashSet - add and contains" )
	public void testUseInHashSet() {
		java.util.Set<NameValuePair>	set		= new java.util.HashSet<>();

		NameValuePair					pair1	= new NameValuePair( "Content-Type", "application/json" );
		NameValuePair					pair2	= new NameValuePair( "Authorization", "Bearer token" );
		NameValuePair					pair3	= new NameValuePair( "Content-Type", "application/json" ); // duplicate

		set.add( pair1 );
		set.add( pair2 );
		set.add( pair3 ); // Should not add duplicate

		assertThat( set.size() ).isEqualTo( 2 ); // Only 2 unique pairs
		assertThat( set.contains( pair1 ) ).isTrue();
		assertThat( set.contains( pair2 ) ).isTrue();
		assertThat( set.contains( pair3 ) ).isTrue(); // Equal to pair1
	}

	@Test
	@DisplayName( "Test use as HashMap key" )
	public void testUseAsHashMapKey() {
		java.util.Map<NameValuePair, String>	map		= new java.util.HashMap<>();

		NameValuePair							key1	= new NameValuePair( "param", "value1" );
		NameValuePair							key2	= new NameValuePair( "param", "value2" );
		NameValuePair							key3	= new NameValuePair( "param", "value1" ); // duplicate of key1

		map.put( key1, "first" );
		map.put( key2, "second" );
		map.put( key3, "updated" ); // Should overwrite key1 value

		assertThat( map.size() ).isEqualTo( 2 );
		assertThat( map.get( key1 ) ).isEqualTo( "updated" ); // key3 overwrote key1
		assertThat( map.get( key2 ) ).isEqualTo( "second" );
		assertThat( map.get( key3 ) ).isEqualTo( "updated" );
	}

	@Test
	@DisplayName( "Test equals transitivity" )
	public void testEqualsTransitivity() {
		NameValuePair	pair1	= new NameValuePair( "key", "value" );
		NameValuePair	pair2	= new NameValuePair( "key", "value" );
		NameValuePair	pair3	= new NameValuePair( "key", "value" );

		// If pair1.equals(pair2) and pair2.equals(pair3), then pair1.equals(pair3)
		assertThat( pair1.equals( pair2 ) ).isTrue();
		assertThat( pair2.equals( pair3 ) ).isTrue();
		assertThat( pair1.equals( pair3 ) ).isTrue();
	}

	@Test
	@DisplayName( "Test equals and hashCode contract" )
	public void testEqualsHashCodeContract() {
		NameValuePair	pair1	= new NameValuePair( "key", "value" );
		NameValuePair	pair2	= new NameValuePair( "key", "value" );

		// Equal objects must have equal hash codes
		if ( pair1.equals( pair2 ) ) {
			assertThat( pair1.hashCode() ).isEqualTo( pair2.hashCode() );
		}
	}
}
