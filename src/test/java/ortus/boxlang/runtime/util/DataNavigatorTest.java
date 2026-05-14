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
import static org.junit.Assert.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.DataNavigator.Navigator;

public class DataNavigatorTest {

	@DisplayName( "Test an invalid path" )
	@Test
	void testInvalidPath() {
		assertThrows( BoxIOException.class, () -> {
			DataNavigator.ofPath( "invalidpath" );
		} );
	}

	@DisplayName( "Test a valid path" )
	@Test
	void testValidPath() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );

		assertThat( nav.getAsString( "name" ) ).isEqualTo( "BoxLang Test Module" );
		assertThat( nav.getAsInteger( "count" ) ).isEqualTo( 1 );
		assertThat( nav.getAsBoolean( "isactive" ) ).isTrue();
		assertThat( nav.getAsBoolean( "isActiveTruthy" ) ).isTrue();
		assertThat( nav.getAsArray( "keywords" ) ).isNotEmpty();
		assertThat( nav.getAsStruct( "boxlang" ) ).isNotNull();

		assertThat( nav.from( "boxlang" ).get( "moduleName" ) ).isEqualTo( "test" );
	}

	@DisplayName( "Can navigate nested segments" )
	@Test
	void testNestedSegments() {
		Navigator	nav		= DataNavigator.of( "src/modules/test/box.json" );

		String		name	= nav
		    .from( "boxlang", "settings" )
		    .getAsString( "hello" );

		assertThat( name ).isEqualTo( "luis" );
	}

	@DisplayName( "Cannot navigate non-existent segments" )
	@Test
	void testNonExistentSegments() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );

		assertThat( nav.from( "boxlang", "settings", "nonexistent" ).get( "bogus", null ) ).isNull();
	}

	@DisplayName( "Can get nested segments" )
	@Test
	void testGetNestedSegments() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );
		assertThat( nav.get( "boxlang", "settings", "hello" ) ).isEqualTo( "luis" );
	}

	@DisplayName( "Can get nested segments that don't exist as null" )
	@Test
	void testGetNestedSegmentsThatDontExist() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );
		assertThat( nav.get( "boxlang", "settings", "bogus" ) ).isNull();
	}

	@DisplayName( "Test nested has" )
	@Test
	void testNestedHas() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );
		assertThat( nav.has( "bogus" ) ).isFalse();
		assertThat( nav.has( "boxlang", "settings", "hello" ) ).isTrue();
		assertThat( nav.has( "boxlang", "settings", "nonexistent" ) ).isFalse();
	}

	@DisplayName( "Can build a navigator from a JSON string" )
	@Test
	void testJsonString() {
		Navigator nav = DataNavigator.of( """
		                                  {
		                                  	"name": "BoxLang Test Module"
		                                  }
		                                  	""" );
		assertThat( nav.get( "name" ) ).isEqualTo( "BoxLang Test Module" );
	}

	@DisplayName( "Can build a navigator from a Java Map" )
	@Test
	void testJavaMap() {
		Navigator nav = DataNavigator.of( Map.of( "name", "BoxLang Test Module" ) );
		assertThat( nav.get( "name" ) ).isEqualTo( "BoxLang Test Module" );
	}

	@DisplayName( "Can build a navigator from a Struct" )
	@Test
	void testStruct() {
		Navigator nav = DataNavigator.of( Struct.of( "name", "BoxLang Test Module" ) );
		assertThat( nav.get( "name" ) ).isEqualTo( "BoxLang Test Module" );
	}

	@DisplayName( "Can test if the content has data" )
	@Test
	void testHasData() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );
		assertThat( nav.isEmpty() ).isFalse();
		assertThat( nav.isPresent() ).isTrue();
	}

	@DisplayName( "Can get or throw an exception" )
	@Test
	void testGetOrThrow() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );
		assertThat( nav.getOrThrow( "boxlang", "settings" ) ).isNotNull();
		assertThrows( BoxRuntimeException.class, () -> {
			nav.getOrThrow( "bogus" );
		} );
	}

	@DisplayName( "If present execute the consume" )
	@Test
	void testIfPresent() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );
		nav
		    .from( "boxlang" )
		    .ifPresent( "settings", settings -> {
			    assertThat( settings ).isInstanceOf( IStruct.class );
		    } );
	}

	@DisplayName( "If present execute the consume and if not the orElse" )
	@Test
	void testIfPresentOrElse() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );
		nav
		    .from( "boxlang" )
		    .ifPresentOrElse( "settings", settings -> {
			    assertThat( settings ).isInstanceOf( IStruct.class );
		    }, () -> {
			    throw new BoxRuntimeException( "Settings not found" );
		    } );
	}

	@DisplayName( "If not present execute orElse" )
	@Test
	void testOrElse() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );
		nav
		    .from( "boxlang" )
		    .ifPresentOrElse(
		        "bogus",
		        settings -> {
			        throw new BoxRuntimeException( "Settings found" );
		        },
		        () -> {
			        assertThat( true ).isTrue();
		        }
		    );
	}

	// -------------------------------------------------------------------------
	// Path expression tests — get()
	// -------------------------------------------------------------------------

	@DisplayName( "get() with a plain key is unchanged" )
	@Test
	void testGetPlainKey() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );
		assertThat( nav.get( "name" ) ).isEqualTo( "BoxLang Test Module" );
	}

	@DisplayName( "get() with a dot-path navigates nested structs" )
	@Test
	void testGetDotPath() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );
		assertThat( nav.get( "boxlang.settings.hello" ) ).isEqualTo( "luis" );
	}

	@DisplayName( "get() with a dot-path to a missing leaf returns null" )
	@Test
	void testGetDotPathMissingLeaf() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );
		assertThat( nav.get( "boxlang.settings.bogus" ) ).isNull();
	}

	@DisplayName( "get() with a dot-path and a default value returns the default when missing" )
	@Test
	void testGetDotPathWithDefault() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );
		assertThat( nav.get( "boxlang.settings.bogus", "default" ) ).isEqualTo( "default" );
	}

	@DisplayName( "get() with a 1-based bracket index returns the correct array element" )
	@Test
	void testGetBracketIndex() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );
		assertThat( nav.get( "keywords[1]" ) ).isEqualTo( "test" );
		assertThat( nav.get( "keywords[ 1  ]" ) ).isEqualTo( "test" );
		assertThat( nav.get( "keywords[   2]" ) ).isEqualTo( "example" );
	}

	@DisplayName( "get() with a bracket index out of range returns null" )
	@Test
	void testGetBracketIndexOutOfRange() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );
		assertThat( nav.get( "keywords[ 99 ]" ) ).isNull();
	}

	@DisplayName( "get() with recursive descent (..) returns the first matching value" )
	@Test
	void testGetRecursiveDescent() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );
		assertThat( nav.get( "..hello" ) ).isEqualTo( "luis" );
		assertThat( nav.get( "   ..hello" ) ).isEqualTo( "luis" );
	}

	@DisplayName( "get() with recursive descent scoped under a parent key" )
	@Test
	void testGetRecursiveDescentScoped() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );
		assertThat( nav.get( "boxlang..hello" ) ).isEqualTo( "luis" );
	}

	@DisplayName( "get() with recursive descent for a missing key returns null" )
	@Test
	void testGetRecursiveDescentMissing() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );
		assertThat( nav.get( "..nonexistent" ) ).isNull();
	}

	@DisplayName( "getAsString() transparently supports a dot-path expression" )
	@Test
	void testGetAsStringWithPath() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );
		assertThat( nav.getAsString( "boxlang.settings.hello" ) ).isEqualTo( "luis" );
	}

	// -------------------------------------------------------------------------
	// Path expression tests — has()
	// -------------------------------------------------------------------------

	@DisplayName( "has() with a dot-path returns true when the path exists" )
	@Test
	void testHasDotPathTrue() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );
		assertThat( nav.has( "boxlang.settings.hello" ) ).isTrue();
		assertThat( nav.has( " boxlang.settings.hello " ) ).isTrue();
	}

	@DisplayName( "has() with a dot-path returns false when the path is missing" )
	@Test
	void testHasDotPathFalse() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );
		assertThat( nav.has( "bogus.path" ) ).isFalse();
	}

	@DisplayName( "has() with recursive descent returns true when the key exists anywhere" )
	@Test
	void testHasRecursiveDescent() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );
		assertThat( nav.has( "..hello" ) ).isTrue();
		assertThat( nav.has( "   ..hello" ) ).isTrue();
		assertThat( nav.has( "..nonexistent" ) ).isFalse();
	}

	@DisplayName( "has() with a wildcard path returns true when any match exists" )
	@Test
	void testHasWildcardPath() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );
		assertThat( nav.has( "keywords[*]" ) ).isTrue();
		assertThat( nav.has( " boxlang.settings.* " ) ).isTrue();
		assertThat( nav.has( "missing[*]" ) ).isFalse();
	}

	@DisplayName( "has() with a slice path returns true when the slice yields matches" )
	@Test
	void testHasSlicePath() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );
		assertThat( nav.has( "keywords[1:2]" ) ).isTrue();
		assertThat( nav.has( "keywords[3:4]" ) ).isFalse();
	}

	@DisplayName( "has() with a filter path returns true only when filtered matches exist" )
	@Test
	void testHasFilterPath() {
		Navigator nav = DataNavigator.of(
		    Map.of(
		        "items", List.of(
		            Map.of( "name", "alpha", "active", true ),
		            Map.of( "name", "beta", "active", false )
		        )
		    )
		);

		assertThat( nav.has( "items[?(@.active == true)]" ) ).isTrue();
		assertThat( nav.has( "items[?(@.active == true)].name" ) ).isTrue();
		assertThat( nav.has( "items[?(@.active == null)]" ) ).isFalse();
	}

	@DisplayName( "has() with a path to a null value still reports the path as present" )
	@Test
	void testHasPathToNullValue() {
		Navigator nav = DataNavigator.of( Map.of( "settings", Struct.of( "nullable", null ) ) );
		assertThat( nav.has( "settings.nullable" ) ).isTrue();
	}

	@DisplayName( "getOrThrow() with a dot-path throws when the path is missing" )
	@Test
	void testGetOrThrowWithPath() {
		Navigator nav = DataNavigator.of( "src/modules/test/box.json" );
		assertThat( nav.getOrThrow( "boxlang.settings.hello" ) ).isEqualTo( "luis" );
		assertThrows( BoxRuntimeException.class, () -> nav.getOrThrow( "bogus.missing" ) );
	}

	// -------------------------------------------------------------------------
	// query() tests
	// -------------------------------------------------------------------------

	@DisplayName( "query() with an array wildcard returns all elements" )
	@Test
	void testQueryArrayWildcard() {
		Navigator	nav		= DataNavigator.of( "src/modules/test/box.json" );
		Array		result	= nav.query( "keywords[*]" );
		assertThat( result ).hasSize( 2 );
		assertThat( result ).containsExactly( "test", "example" ).inOrder();
	}

	@DisplayName( "query() with a 1-based inclusive slice returns the correct elements" )
	@Test
	void testQuerySlice() {
		Navigator	nav		= DataNavigator.of( "src/modules/test/box.json" );
		Array		result	= nav.query( "keywords[1:2]" );
		assertThat( result ).hasSize( 2 );
		assertThat( result ).containsExactly( "test", "example" ).inOrder();
	}

	@DisplayName( "query() with a struct wildcard returns all top-level values of that struct" )
	@Test
	void testQueryStructWildcard() {
		Navigator	nav		= DataNavigator.of( "src/modules/test/box.json" );
		Array		result	= nav.query( "boxlang.settings.*" );
		assertThat( result ).hasSize( 1 );
		assertThat( result.get( 0 ) ).isEqualTo( "luis" );
	}

	@DisplayName( "query() with a plain path wraps the single result in an Array" )
	@Test
	void testQueryPlainPath() {
		Navigator	nav		= DataNavigator.of( "src/modules/test/box.json" );
		Array		result	= nav.query( "name" );
		assertThat( result ).hasSize( 1 );
		assertThat( result.get( 0 ) ).isEqualTo( "BoxLang Test Module" );
	}

	@DisplayName( "query() preserves null matches for direct key paths" )
	@Test
	void testQueryPlainPathWithNullValue() {
		Navigator	nav		= DataNavigator.of( Map.of( "settings", Struct.of( "nullable", null ) ) );
		Array		result	= nav.query( "settings.nullable" );
		assertThat( result ).hasSize( 1 );
		assertThat( result.get( 0 ) ).isNull();
	}

	@DisplayName( "query() with recursive descent collects all matches" )
	@Test
	void testQueryRecursiveDescent() {
		Navigator	nav		= DataNavigator.of( "src/modules/test/box.json" );
		Array		result	= nav.query( "..hello" );
		assertThat( result ).hasSize( 1 );
		assertThat( result.get( 0 ) ).isEqualTo( "luis" );

		result = nav.query( "   ..hello" );
		assertThat( result ).hasSize( 1 );
		assertThat( result.get( 0 ) ).isEqualTo( "luis" );
	}

	@DisplayName( "query() tolerates whitespace around wildcard syntax" )
	@Test
	void testQueryWildcardWithWhitespace() {
		Navigator	nav		= DataNavigator.of( "src/modules/test/box.json" );
		Array		result	= nav.query( " keywords [ * ] " );
		assertThat( result ).hasSize( 2 );
		assertThat( result ).containsExactly( "test", "example" ).inOrder();
	}

	@DisplayName( "query() with a filter returns only matching array elements" )
	@Test
	void testQueryFilter() {
		Navigator	nav		= DataNavigator.of(
		    Map.of(
		        "items", List.of(
		            Map.of( "name", "alpha", "active", true ),
		            Map.of( "name", "beta", "active", false ),
		            Map.of( "name", "gamma", "active", true )
		        )
		    )
		);
		Array		result	= nav.query( "items[?(@.active == true)]" );
		assertThat( result ).hasSize( 2 );
	}

	@DisplayName( "query() filter with numeric comparison" )
	@Test
	void testQueryFilterNumeric() {
		Navigator	nav		= DataNavigator.of(
		    Map.of(
		        "products", List.of(
		            Map.of( "name", "cheap", "price", 5 ),
		            Map.of( "name", "mid", "price", 15 ),
		            Map.of( "name", "expensive", "price", 50 )
		        )
		    )
		);
		Array		result	= nav.query( "products[?(@.price < 20)]" );
		assertThat( result ).hasSize( 2 );
	}

	@DisplayName( "query() filter existence check returns elements that have the key" )
	@Test
	void testQueryFilterExistence() {
		Navigator	nav		= DataNavigator.of(
		    Map.of(
		        "items", List.of(
		            Map.of( "name", "has-tag", "tag", "x" ),
		            Map.of( "name", "no-tag" )
		        )
		    )
		);
		Array		result	= nav.query( "items[?(@.tag)]" );
		assertThat( result ).hasSize( 1 );
	}

}
