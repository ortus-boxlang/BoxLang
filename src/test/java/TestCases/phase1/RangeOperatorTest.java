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
package TestCases.phase1;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;

public class RangeOperatorTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			resultKey	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "range operator returns Range type" )
	@Test
	public void testRangeOperator() {
		Object result = instance.executeStatement( "1..5", context );
		assertThat( result ).isInstanceOf( ortus.boxlang.runtime.types.Range.class );
		assertThat( ( ( ortus.boxlang.runtime.types.Range<?> ) result ).toArray() ).isEqualTo( Array.of( 1, 2, 3, 4, 5 ) );

		result = instance.executeStatement( "(5..1)", context );
		assertThat( ( ( ortus.boxlang.runtime.types.Range<?> ) result ).toArray() ).isEqualTo( Array.of( 5, 4, 3, 2, 1 ) );

		instance.executeSource(
		    """
		    a = 2;
		    b = 4;
		    result = a..b;
		    """,
		    context );
		assertThat( ( ( ortus.boxlang.runtime.types.Range<?> ) variables.get( resultKey ) ).toArray() ).isEqualTo( Array.of( 2, 3, 4 ) );
	}

	@DisplayName( "non-empty range is truthy in if statement" )
	@Test
	public void testRangeIsTruthyInIf() {
		instance.executeSource(
		    """
		    result = false;
		    if( 1..5 ) {
		        result = true;
		    }
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( true );
	}

	@DisplayName( "non-empty range is truthy in ternary" )
	@Test
	public void testRangeIsTruthyInTernary() {
		instance.executeSource(
		    """
		    result = (1..5) ? "truthy" : "falsy";
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "truthy" );
	}

	@DisplayName( "single-element range is truthy" )
	@Test
	public void testSingleElementRangeIsTruthy() {
		instance.executeSource(
		    """
		    result = false;
		    if( 5..5 ) {
		        result = true;
		    }
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( true );
	}

	@DisplayName( "empty range is falsy" )
	@Test
	public void testEmptyRangeIsFalsy() {
		// step(-1) on ascending range produces empty range (no bound-swapping)
		instance.executeSource(
		    """
		    result = true;
		    if( (1..5).step(-1) ) {
		        result = "truthy";
		    } else {
		        result = "falsy";
		    }
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "falsy" );
	}

	@DisplayName( "asc() on descending range is falsy" )
	@Test
	public void testAscOnDescendingRangeIsFalsy() {
		instance.executeSource(
		    """
		    result = true;
		    if( (5..1).asc() ) {
		        result = "truthy";
		    } else {
		        result = "falsy";
		    }
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "falsy" );
	}

	@DisplayName( "range can be cast to array" )
	@Test
	public void testRangeCastToArray() {
		instance.executeSource(
		    """
		    myRange = 1..5;
		    result = arrayLen( myRange );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 5 );
	}

	@DisplayName( "range can be used where array is expected" )
	@Test
	public void testRangeUsedAsArray() {
		instance.executeSource(
		    """
		    result = arrayToList( 1..5, "," );
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "1,2,3,4,5" );
	}

	@DisplayName( "range cast to modifiable array via arrayAppend" )
	@Test
	public void testRangeCastToArrayIsModifiable() {
		instance.executeSource(
		    """
		    myRange = 1..3;
		    myArr = arrayAppend( myRange, 4 );
		    result = arrayLen( myArr );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 4 );
	}

	@DisplayName( "unbounded range errors when cast to array in BIF" )
	@Test
	public void testUnboundedRangeErrorsInArrayBIF() {
		ortus.boxlang.runtime.types.Range<?> unbounded = new ortus.boxlang.runtime.types.Range<>(
		    1, null, Integer::compare, ( current, step ) -> current + step.intValue() );
		variables.put( Key.of( "myRange" ), unbounded );
		org.junit.jupiter.api.Assertions.assertThrows( ortus.boxlang.runtime.types.exceptions.BoxRuntimeException.class, () -> {
			instance.executeSource(
			    """
			    result = arrayLen( myRange );
			    """,
			    context );
		} );
	}

	@DisplayName( "range precedence: arithmetic binds tighter than range" )
	@Test
	public void testRangePrecedence() {
		// 1 + 3 .. 5 * 2 should be (1+3)..(5*2) = 4..10
		instance.executeSource(
		    """
		    result = 1 + 3 .. 5 * 2;
		    """,
		    context );
		ortus.boxlang.runtime.types.Range<?> range = ( ortus.boxlang.runtime.types.Range<?> ) variables.get( resultKey );
		assertThat( ( ( Number ) range.getFrom() ).intValue() ).isEqualTo( 4 );
		assertThat( ( ( Number ) range.getTo() ).intValue() ).isEqualTo( 10 );
		assertThat( range.contains( null ) ).isFalse();
	}

	@DisplayName( "half-bounded range: open end (1..)" )
	@Test
	public void testOpenEndRange() {
		instance.executeSource(
		    """
		    result = 1..;
		    """,
		    context );
		ortus.boxlang.runtime.types.Range<?> range = ( ortus.boxlang.runtime.types.Range<?> ) variables.get( resultKey );
		assertThat( range.getFrom() ).isEqualTo( 1 );
		assertThat( range.getTo() ).isNull();
		assertThat( range.contains( 999 ) ).isTrue();
		assertThat( range.contains( null ) ).isFalse();
	}

	@DisplayName( "half-bounded range: open start (..5)" )
	@Test
	public void testOpenStartRange() {
		instance.executeSource(
		    """
		    result = ..5;
		    """,
		    context );
		ortus.boxlang.runtime.types.Range<?> range = ( ortus.boxlang.runtime.types.Range<?> ) variables.get( resultKey );
		assertThat( range.getFrom() ).isNull();
		assertThat( range.getTo() ).isEqualTo( 5 );
		assertThat( range.contains( 3 ) ).isTrue();
		assertThat( range.contains( null ) ).isFalse();
	}

	@DisplayName( "fully open range (..)" )
	@Test
	public void testFullyOpenRange() {
		instance.executeSource(
		    """
		    result = ..;
		    """,
		    context );
		ortus.boxlang.runtime.types.Range<?> range = ( ortus.boxlang.runtime.types.Range<?> ) variables.get( resultKey );
		assertThat( range.getFrom() ).isNull();
		assertThat( range.getTo() ).isNull();
		assertThat( range.contains( 42 ) ).isTrue();
		assertThat( range.contains( "anything" ) ).isTrue();
		assertThat( range.contains( null ) ).isFalse();
	}

	// ======================== Exclusive Range Operators ========================

	@DisplayName( "left exclusive range: 1>..5 excludes 1, includes 5" )
	@Test
	public void testLeftExclusiveRange() {
		instance.executeSource(
		    """
		    result = 1>..5;
		    """,
		    context );
		ortus.boxlang.runtime.types.Range<?> range = ( ortus.boxlang.runtime.types.Range<?> ) variables.get( resultKey );
		assertThat( range.isFromExclusive() ).isTrue();
		assertThat( range.isToExclusive() ).isFalse();
		assertThat( range.contains( 1 ) ).isFalse();
		assertThat( range.contains( 2 ) ).isTrue();
		assertThat( range.contains( 5 ) ).isTrue();
		assertThat( range.contains( null ) ).isFalse();
		// Iteration: 2, 3, 4, 5
		java.util.List<Object> values = new java.util.ArrayList<>();
		for ( Object v : range ) {
			values.add( v );
		}
		assertThat( values ).containsExactly( 2, 3, 4, 5 ).inOrder();
	}

	@DisplayName( "right exclusive range: 1..<5 includes 1, excludes 5" )
	@Test
	public void testRightExclusiveRange() {
		instance.executeSource(
		    """
		    result = 1..<5;
		    """,
		    context );
		ortus.boxlang.runtime.types.Range<?> range = ( ortus.boxlang.runtime.types.Range<?> ) variables.get( resultKey );
		assertThat( range.isFromExclusive() ).isFalse();
		assertThat( range.isToExclusive() ).isTrue();
		assertThat( range.contains( 1 ) ).isTrue();
		assertThat( range.contains( 4 ) ).isTrue();
		assertThat( range.contains( 5 ) ).isFalse();
		assertThat( range.contains( null ) ).isFalse();
		// Iteration: 1, 2, 3, 4
		java.util.List<Object> values = new java.util.ArrayList<>();
		for ( Object v : range ) {
			values.add( v );
		}
		assertThat( values ).containsExactly( 1, 2, 3, 4 ).inOrder();
	}

	@DisplayName( "full exclusive range: 1>..<5 excludes both bounds" )
	@Test
	public void testFullExclusiveRange() {
		instance.executeSource(
		    """
		    result = 1>..<5;
		    """,
		    context );
		ortus.boxlang.runtime.types.Range<?> range = ( ortus.boxlang.runtime.types.Range<?> ) variables.get( resultKey );
		assertThat( range.isFromExclusive() ).isTrue();
		assertThat( range.isToExclusive() ).isTrue();
		assertThat( range.contains( 1 ) ).isFalse();
		assertThat( range.contains( 2 ) ).isTrue();
		assertThat( range.contains( 4 ) ).isTrue();
		assertThat( range.contains( 5 ) ).isFalse();
		assertThat( range.contains( null ) ).isFalse();
		// Iteration: 2, 3, 4
		java.util.List<Object> values = new java.util.ArrayList<>();
		for ( Object v : range ) {
			values.add( v );
		}
		assertThat( values ).containsExactly( 2, 3, 4 ).inOrder();
	}

	@DisplayName( "exclusive range asString() shows correct operator" )
	@Test
	public void testExclusiveRangeAsString() {
		instance.executeSource(
		    """
		    leftEx = 1>..5;
		    rightEx = 1..<5;
		    fullEx = 1>..<5;
		    inclusive = 1..5;
		    """,
		    context );
		assertThat( variables.get( Key.of( "leftEx" ) ).toString() ).isEqualTo( "1>..5" );
		assertThat( variables.get( Key.of( "rightEx" ) ).toString() ).isEqualTo( "1..<5" );
		assertThat( variables.get( Key.of( "fullEx" ) ).toString() ).isEqualTo( "1>..<5" );
		assertThat( variables.get( Key.of( "inclusive" ) ).toString() ).isEqualTo( "1..5" );
	}

	@DisplayName( "exclusive range with arithmetic precedence" )
	@Test
	public void testExclusiveRangePrecedence() {
		instance.executeSource(
		    """
		    result = 1 + 1 >..<  2 * 3;
		    """,
		    context );
		ortus.boxlang.runtime.types.Range<?> range = ( ortus.boxlang.runtime.types.Range<?> ) variables.get( resultKey );
		// (1+1) >..< (2*3) = 2>..<6
		assertThat( ( ( Number ) range.getFrom() ).intValue() ).isEqualTo( 2 );
		assertThat( ( ( Number ) range.getTo() ).intValue() ).isEqualTo( 6 );
		assertThat( range.isFromExclusive() ).isTrue();
		assertThat( range.isToExclusive() ).isTrue();
	}

	// ======================== Range-in-Range Contains ========================

	@DisplayName( "inner range fully within outer range" )
	@Test
	public void testRangeContainsInnerRange() {
		instance.executeSource(
		    """
		    outer = 1..10;
		    inner = 3..7;
		    result = outer.contains( inner );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( true );
	}

	@DisplayName( "inner range equals outer range" )
	@Test
	public void testRangeContainsSameRange() {
		instance.executeSource(
		    """
		    outer = 1..10;
		    inner = 1..10;
		    result = outer.contains( inner );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( true );
	}

	@DisplayName( "inner range exceeds outer on high end" )
	@Test
	public void testRangeDoesNotContainExceedingHigh() {
		instance.executeSource(
		    """
		    outer = 1..10;
		    inner = 5..15;
		    result = outer.contains( inner );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( false );
	}

	@DisplayName( "inner range exceeds outer on low end" )
	@Test
	public void testRangeDoesNotContainExceedingLow() {
		instance.executeSource(
		    """
		    outer = 5..10;
		    inner = 1..7;
		    result = outer.contains( inner );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( false );
	}

	@DisplayName( "inner exclusive fits inside outer inclusive at same bounds" )
	@Test
	public void testExclusiveInnerFitsInInclusiveOuter() {
		instance.executeSource(
		    """
		    outer = 1..10;
		    inner = 1>..10;
		    result = outer.contains( inner );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( true );
	}

	@DisplayName( "inner inclusive does NOT fit in outer exclusive at same bounds" )
	@Test
	public void testInclusiveInnerNotInExclusiveOuter() {
		instance.executeSource(
		    """
		    outer = 1>..10;
		    inner = 1..10;
		    result = outer.contains( inner );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( false );
	}

	@DisplayName( "both exclusive same bounds — inner fits" )
	@Test
	public void testBothExclusiveSameBoundsContains() {
		instance.executeSource(
		    """
		    outer = 1>..<10;
		    inner = 1>..<10;
		    result = outer.contains( inner );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( true );
	}

	@DisplayName( "right exclusive inner fits in right exclusive outer" )
	@Test
	public void testRightExclusiveInnerInOuter() {
		instance.executeSource(
		    """
		    outer = 1..<10;
		    inner = 3..<10;
		    result = outer.contains( inner );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( true );
	}

	@DisplayName( "inner inclusive high does NOT fit in outer right exclusive same high" )
	@Test
	public void testInclusiveHighNotInRightExclusiveOuter() {
		instance.executeSource(
		    """
		    outer = 1..<10;
		    inner = 3..10;
		    result = outer.contains( inner );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( false );
	}

	@DisplayName( "unbounded outer contains any inner range" )
	@Test
	public void testUnboundedOuterContainsAnyRange() {
		instance.executeSource(
		    """
		    outer = ..;
		    inner = 1..100;
		    result = outer.contains( inner );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( true );
	}

	@DisplayName( "bounded outer does NOT contain unbounded inner" )
	@Test
	public void testBoundedDoesNotContainUnbounded() {
		instance.executeSource(
		    """
		    outer = 1..10;
		    inner = ..;
		    result = outer.contains( inner );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( false );
	}

	@DisplayName( "half-bounded outer contains matching half-bounded inner" )
	@Test
	public void testHalfBoundedContainsHalfBounded() {
		instance.executeSource(
		    """
		    outer = 1..;
		    inner = 5..;
		    result = outer.contains( inner );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( true );
	}

	@DisplayName( "half-bounded outer does NOT contain inner below its start" )
	@Test
	public void testHalfBoundedRejectsOutOfRange() {
		instance.executeSource(
		    """
		    outer = 5..;
		    inner = 1..3;
		    result = outer.contains( inner );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( false );
	}

	@DisplayName( "descending inner within outer" )
	@Test
	public void testDescendingInnerWithinOuter() {
		instance.executeSource(
		    """
		    outer = 1..10;
		    inner = 8..3;
		    result = outer.contains( inner );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( true );
	}

	@DisplayName( "character range contains inner character range" )
	@Test
	public void testCharRangeContainsInnerCharRange() {
		instance.executeSource(
		    """
		    outer = "a".."z";
		    inner = "d".."g";
		    result = outer.contains( inner );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( true );
	}

	// ======================== Numeric String Coercion ========================

	@DisplayName( "decimal range iterates with whole step" )
	@Test
	public void testDecimalRangeWholeStep() {
		instance.executeSource(
		    """
		    r = 1.5..4.5;
		    result = arrayToList( r, "," );
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "1.5,2.5,3.5,4.5" );
	}

	@DisplayName( "decimal range with decimal step" )
	@Test
	public void testDecimalRangeDecimalStep() {
		instance.executeSource(
		    """
		    r = (0..1).step(0.25);
		    result = arrayLen( r );
		    """,
		    context );
		// 0, 0.25, 0.50, 0.75, 1.00
		assertThat( variables.get( resultKey ) ).isEqualTo( 5 );
	}

	@DisplayName( "decimal range contains works" )
	@Test
	public void testDecimalRangeContains() {
		instance.executeSource(
		    """
		    r = 1.5..5.5;
		    c3 = r.contains( 3.0 );
		    cLow = r.contains( 1.5 );
		    cHigh = r.contains( 5.5 );
		    cBelow = r.contains( 1.0 );
		    cAbove = r.contains( 6.0 );
		    """,
		    context );
		assertThat( variables.get( Key.of( "c3" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "cLow" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "cHigh" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "cBelow" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "cAbove" ) ) ).isEqualTo( false );
	}

	@DisplayName( "descending decimal range" )
	@Test
	public void testDescendingDecimalRange() {
		instance.executeSource(
		    """
		    r = 3.5..1.5;
		    result = arrayToList( r, "," );
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "3.5,2.5,1.5" );
	}

	@DisplayName( "range from numeric strings coerces to number range" )
	@Test
	public void testNumericStringRange() {
		instance.executeSource(
		    """
		    a = "1";
		    b = "5";
		    r = a..b;
		    result = arrayToList( r, "," );
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "1,2,3,4,5" );
	}

	@DisplayName( "range from mixed number and numeric string" )
	@Test
	public void testMixedNumberAndStringRange() {
		instance.executeSource(
		    """
		    r = 1.."5";
		    result = arrayToList( r, "," );
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "1,2,3,4,5" );
	}

	@DisplayName( "single element range iterates once" )
	@Test
	public void testSingleElementRangeIteration() {
		instance.executeSource(
		    """
		    r = 5..5;
		    result = arrayToList( r, "," );
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "5" );
	}

	// ======================== Character Ranges ========================

	@DisplayName( "character range iterates a through e" )
	@Test
	public void testCharacterRangeIteration() {
		instance.executeSource(
		    """
		    r = "a".."e";
		    result = [];
		    for( c in r ) {
		        result.append( c );
		    }
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( Array.of( 'a', 'b', 'c', 'd', 'e' ) );
	}

	@DisplayName( "descending character range iterates z through v" )
	@Test
	public void testDescendingCharacterRange() {
		instance.executeSource(
		    """
		    r = "z".."v";
		    result = [];
		    for( c in r ) {
		        result.append( c );
		    }
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( Array.of( 'z', 'y', 'x', 'w', 'v' ) );
	}

	// ======================== Comparable String Ranges ========================

	@DisplayName( "comparable string range is not iterable but supports contains" )
	@Test
	public void testComparableStringRange() {
		instance.executeSource(
		    """
		    r = "aaa".."zzz";
		    result = r.contains( "foo" );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( true );
	}

	@DisplayName( "comparable string range contains boundary values" )
	@Test
	public void testComparableStringRangeBoundaries() {
		instance.executeSource(
		    """
		    r = "aaa".."zzz";
		    containsLow = r.contains( "aaa" );
		    containsHigh = r.contains( "zzz" );
		    containsBelow = r.contains( "000" );
		    """,
		    context );
		assertThat( variables.get( Key.of( "containsLow" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "containsHigh" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "containsBelow" ) ) ).isEqualTo( false );
	}

	@DisplayName( "comparable string range throws on iteration" )
	@Test
	public void testComparableStringRangeThrowsOnIteration() {
		org.junit.jupiter.api.Assertions.assertThrows( Exception.class, () -> {
			instance.executeSource(
			    """
			    r = "aaa".."zzz";
			    for( c in r ) {}
			    """,
			    context );
		} );
	}

	// ======================== Element Contains ========================

	@DisplayName( "integer range contains values in and out of bounds" )
	@Test
	public void testIntRangeContains() {
		instance.executeSource(
		    """
		    r = 1..10;
		    c5 = r.contains( 5 );
		    c1 = r.contains( 1 );
		    c10 = r.contains( 10 );
		    c0 = r.contains( 0 );
		    c11 = r.contains( 11 );
		    """,
		    context );
		assertThat( variables.get( Key.of( "c5" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "c1" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "c10" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "c0" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "c11" ) ) ).isEqualTo( false );
	}

	@DisplayName( "contains returns false for null" )
	@Test
	public void testContainsNull() {
		instance.executeSource(
		    """
		    r = 1..10;
		    result = r.contains( null );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( false );
	}

	@DisplayName( "contains returns false for incompatible type" )
	@Test
	public void testContainsIncompatibleType() {
		instance.executeSource(
		    """
		    r = 1..10;
		    result = r.contains( "not a number" );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( false );
	}

	@DisplayName( "contains coerces numeric strings for number range" )
	@Test
	public void testContainsCoercesNumericString() {
		instance.executeSource(
		    """
		    r = 1..10;
		    c5 = r.contains( "5" );
		    c11 = r.contains( "11" );
		    """,
		    context );
		assertThat( variables.get( Key.of( "c5" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "c11" ) ) ).isEqualTo( false );
	}

	@DisplayName( "character range contains single-char string" )
	@Test
	public void testCharRangeContains() {
		instance.executeSource(
		    """
		    r = "a".."z";
		    cm = r.contains( "m" );
		    ca = r.contains( "a" );
		    cz = r.contains( "z" );
		    """,
		    context );
		assertThat( variables.get( Key.of( "cm" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "ca" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "cz" ) ) ).isEqualTo( true );
	}

	@DisplayName( "descending range contains works" )
	@Test
	public void testDescendingRangeContains() {
		instance.executeSource(
		    """
		    r = 10..1;
		    c5 = r.contains( 5 );
		    c1 = r.contains( 1 );
		    c10 = r.contains( 10 );
		    c0 = r.contains( 0 );
		    c11 = r.contains( 11 );
		    """,
		    context );
		assertThat( variables.get( Key.of( "c5" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "c1" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "c10" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "c0" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "c11" ) ) ).isEqualTo( false );
	}

	// ======================== Copy-on-Write Modifiers ========================

	@DisplayName( "step() changes iteration step" )
	@Test
	public void testStepModifier() {
		instance.executeSource(
		    """
		    result = arrayToList( (1..10).step(2), "," );
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "1,3,5,7,9" );
	}

	@DisplayName( "step() with negative value on descending range" )
	@Test
	public void testNegativeStep() {
		instance.executeSource(
		    """
		    result = arrayToList( (10..1).step(-3), "," );
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "10,7,4,1" );
	}

	@DisplayName( "desc() on ascending range produces empty range" )
	@Test
	public void testDescOnAscendingIsEmpty() {
		instance.executeSource(
		    """
		    result = (1..5).desc().isEmpty();
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( true );
	}

	@DisplayName( "step(-1) on ascending range produces empty range" )
	@Test
	public void testNegativeStepOnAscendingIsEmpty() {
		instance.executeSource(
		    """
		    result = (1..5).step(-1).isEmpty();
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( true );
	}

	@DisplayName( "step(2) on descending range produces empty range" )
	@Test
	public void testPositiveStepOnDescendingIsEmpty() {
		instance.executeSource(
		    """
		    result = (5..1).step(2).isEmpty();
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( true );
	}

	@DisplayName( "step() returns new range, original unchanged" )
	@Test
	public void testStepReturnsNewInstance() {
		instance.executeSource(
		    """
		    original = 1..10;
		    stepped = original.step(3);
		    origStep = original.getStep();
		    newStep = stepped.getStep();
		    """,
		    context );
		assertThat( variables.get( Key.of( "origStep" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "newStep" ) ) ).isEqualTo( 3 );
	}

	// ======================== Boundary Queries ========================

	@DisplayName( "isBounded returns true when both bounds present" )
	@Test
	public void testIsBounded() {
		instance.executeSource(
		    """
		    r = 1..10;
		    bounded = r.isBounded();
		    unbounded = r.isUnbounded();
		    halfBounded = r.isHalfBounded();
		    hasFrom = r.hasFrom();
		    hasTo = r.hasTo();
		    """,
		    context );
		assertThat( variables.get( Key.of( "bounded" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "unbounded" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "halfBounded" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "hasFrom" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasTo" ) ) ).isEqualTo( true );
	}

	@DisplayName( "isUnbounded returns true for fully open range" )
	@Test
	public void testIsUnbounded() {
		instance.executeSource(
		    """
		    r = ..;
		    bounded = r.isBounded();
		    unbounded = r.isUnbounded();
		    halfBounded = r.isHalfBounded();
		    hasFrom = r.hasFrom();
		    hasTo = r.hasTo();
		    """,
		    context );
		assertThat( variables.get( Key.of( "bounded" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "unbounded" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "halfBounded" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "hasFrom" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "hasTo" ) ) ).isEqualTo( false );
	}

	@DisplayName( "isHalfBounded returns true for one-sided range" )
	@Test
	public void testIsHalfBounded() {
		instance.executeSource(
		    """
		    r = 5..;
		    bounded = r.isBounded();
		    unbounded = r.isUnbounded();
		    halfBounded = r.isHalfBounded();
		    hasFrom = r.hasFrom();
		    hasTo = r.hasTo();
		    """,
		    context );
		assertThat( variables.get( Key.of( "bounded" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "unbounded" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "halfBounded" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasFrom" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "hasTo" ) ) ).isEqualTo( false );
	}

	@DisplayName( "getFrom and getTo return correct values" )
	@Test
	public void testGetFromAndTo() {
		instance.executeSource(
		    """
		    r = 1..10;
		    f = r.getFrom();
		    t = r.getTo();
		    """,
		    context );
		assertThat( variables.get( Key.of( "f" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( Key.of( "t" ) ) ).isEqualTo( 10 );
	}

	@DisplayName( "getStep returns the step value" )
	@Test
	public void testGetStep() {
		instance.executeSource(
		    """
		    r = (1..10).step(3);
		    result = r.getStep();
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 3 );
	}

	// ======================== toArray ========================

	@DisplayName( "toArray with custom step" )
	@Test
	public void testToArrayWithStep() {
		instance.executeSource(
		    """
		    r = (1..10).step(3);
		    result = arrayToList( r, "," );
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "1,4,7,10" );
	}

	@DisplayName( "toArray throws on non-iterable comparable string range" )
	@Test
	public void testToArrayThrowsNonIterable() {
		org.junit.jupiter.api.Assertions.assertThrows( Exception.class, () -> {
			instance.executeSource(
			    """
			    result = arrayLen( "aaa".."zzz" );
			    """,
			    context );
		} );
	}

	// ======================== asString / toString ========================

	@DisplayName( "toString shows basic range notation" )
	@Test
	public void testToStringBasic() {
		instance.executeSource(
		    """
		    result = (1..10).toString();
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "1..10" );
	}

	@DisplayName( "toString shows desc() for descending range" )
	@Test
	public void testToStringDesc() {
		instance.executeSource(
		    """
		    result = (5..1).toString();
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "5..1.desc()" );
	}

	@DisplayName( "toString shows step() for non-default step" )
	@Test
	public void testToStringCustomStep() {
		instance.executeSource(
		    """
		    result = (1..10).step(3).toString();
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "1..10.step(3)" );
	}

	@DisplayName( "toString shows half-bounded range" )
	@Test
	public void testToStringHalfBounded() {
		instance.executeSource(
		    """
		    result = (5..).toString();
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "5.." );
	}

	// ======================== DateTime Ranges ========================

	@DisplayName( "DateTime range iterates days" )
	@Test
	public void testDateTimeRange() {
		instance.executeSource(
		    """
		    start = createDate( 2024, 1, 1 );
		    end = createDate( 2024, 1, 5 );
		    r = start..end;
		    result = arrayLen( r );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 5 );
	}

	@DisplayName( "DateTime range contains works" )
	@Test
	public void testDateTimeRangeContains() {
		instance.executeSource(
		    """
		    start = createDate( 2024, 1, 1 );
		    end = createDate( 2024, 1, 31 );
		    r = start..end;
		    c15 = r.contains( createDate( 2024, 1, 15 ) );
		    c1 = r.contains( createDate( 2024, 1, 1 ) );
		    c31 = r.contains( createDate( 2024, 1, 31 ) );
		    cBefore = r.contains( createDate( 2023, 12, 31 ) );
		    cAfter = r.contains( createDate( 2024, 2, 1 ) );
		    """,
		    context );
		assertThat( variables.get( Key.of( "c15" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "c1" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "c31" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "cBefore" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "cAfter" ) ) ).isEqualTo( false );
	}

	@DisplayName( "DateTime range contains works with string date" )
	@Test
	public void testDateTimeRangeContainsStringDate() {
		instance.executeSource(
		    """
		    start = createDate( 2024, 1, 1 );
		    end = createDate( 2024, 1, 31 );
		    r = start..end;
		    c15 = r.contains( "2024-01-15" );
		    cBefore = r.contains( "2023-12-31" );
		    cAfter = r.contains( "2024-02-01" );
		    """,
		    context );
		assertThat( variables.get( Key.of( "c15" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "cBefore" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "cAfter" ) ) ).isEqualTo( false );
	}

	@DisplayName( "DateTime range with step in months" )
	@Test
	public void testDateTimeStepMonths() {
		instance.executeSource(
		    """
		    start = createDate( 2024, 1, 15 );
		    end = createDate( 2024, 6, 15 );
		    r = (start..end).step( 1, "month" );
		    result = arrayLen( r );
		    """,
		    context );
		// Jan 15, Feb 15, Mar 15, Apr 15, May 15, Jun 15
		assertThat( variables.get( resultKey ) ).isEqualTo( 6 );
	}

	@DisplayName( "DateTime range with step in weeks" )
	@Test
	public void testDateTimeStepWeeks() {
		instance.executeSource(
		    """
		    start = createDate( 2024, 1, 1 );
		    end = createDate( 2024, 1, 29 );
		    r = (start..end).step( 1, "week" );
		    result = arrayLen( r );
		    """,
		    context );
		// Jan 1, 8, 15, 22, 29
		assertThat( variables.get( resultKey ) ).isEqualTo( 5 );
	}

	@DisplayName( "DateTime range with step in years" )
	@Test
	public void testDateTimeStepYears() {
		instance.executeSource(
		    """
		    start = createDate( 2020, 6, 1 );
		    end = createDate( 2024, 6, 1 );
		    r = (start..end).step( 1, "year" );
		    result = arrayLen( r );
		    """,
		    context );
		// 2020, 2021, 2022, 2023, 2024
		assertThat( variables.get( resultKey ) ).isEqualTo( 5 );
	}

	@DisplayName( "step(amount, unit) throws on non-DateTime range" )
	@Test
	public void testStepUnitThrowsOnIntRange() {
		org.junit.jupiter.api.Assertions.assertThrows( Exception.class, () -> {
			instance.executeSource(
			    """
			    r = (1..10).step( 5, "minutes" );
			    """,
			    context );
		} );
	}

	// ======================== Equality ========================

	@DisplayName( "equal ranges are equal" )
	@Test
	public void testRangeEquality() {
		instance.executeSource(
		    """
		    r1 = 1..10;
		    r2 = 1..10;
		    result = r1.equals( r2 );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( true );
	}

	@DisplayName( "unequal ranges are not equal" )
	@Test
	public void testRangeInequality() {
		instance.executeSource(
		    """
		    r1 = 1..10;
		    r2 = 1..5;
		    result = r1.equals( r2 );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( false );
	}

	// ======================== Lazy For-In Iteration ========================

	@DisplayName( "for-in over half-bounded range with break only iterates once" )
	@Test
	public void testForInHalfBoundedRangeWithBreak() {
		instance.executeSource(
		    """
		    count = 0;
		    for( i in 1.. ) {
		        count++;
		        result = i;
		        break;
		    }
		    """,
		    context );
		assertThat( variables.get( Key.of( "count" ) ) ).isEqualTo( 1 );
		assertThat( variables.get( resultKey ) ).isEqualTo( 1 );
	}

	@DisplayName( "for-in over half-bounded range iterates correct number of times before break" )
	@Test
	public void testForInHalfBoundedRangeMultipleIterations() {
		instance.executeSource(
		    """
		    result = [];
		    for( i in 1.. ) {
		        result.append( i );
		        if( i == 5 ) break;
		    }
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( Array.of( 1, 2, 3, 4, 5 ) );
	}

	@DisplayName( "for-in over huge range with break is fast and does not materialize" )
	@Test
	public void testForInHugeRangeIsLazy() {
		instance.executeSource(
		    """
		    start = getTickCount();
		    for( i in 1..100_000_000_000 ) {
		        result = i;
		        break;
		    }
		    elapsed = getTickCount() - start;
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 1 );
		// If the range were materialized, this would take many seconds and likely OOM.
		// Lazy iteration should complete in well under 100ms.
		assertThat( ( ( Number ) variables.get( Key.of( "elapsed" ) ) ).longValue() ).isLessThan( 100 );
	}

	@DisplayName( "stream on huge range with limit is fast and does not materialize" )
	@Test
	public void testStreamHugeRangeWithLimit() {
		instance.executeSource(
		    """
		    start = getTickCount();
		    result = (1..100_000_000_000).stream().limit( 5 ).toList();
		    elapsed = getTickCount() - start;
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( Array.of( 1, 2, 3, 4, 5 ) );
		assertThat( ( ( Number ) variables.get( Key.of( "elapsed" ) ) ).longValue() ).isLessThan( 100 );
	}

	@DisplayName( "stream on half-bounded range with limit is fast and does not materialize" )
	@Test
	public void testStreamHalfBoundedRangeWithLimit() {
		instance.executeSource(
		    """
		    start = getTickCount();
		    result = (1..).stream().limit( 5 ).toList();
		    elapsed = getTickCount() - start;
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( Array.of( 1, 2, 3, 4, 5 ) );
		assertThat( ( ( Number ) variables.get( Key.of( "elapsed" ) ) ).longValue() ).isLessThan( 100 );
	}

	// ======================== Open-start / fully-open ranges must fail iteration ========================

	@DisplayName( "for-in over open-start range (..5) throws" )
	@Test
	public void testForInOpenStartRangeThrows() {
		org.junit.jupiter.api.Assertions.assertThrows( Exception.class, () -> {
			instance.executeSource(
			    """
			    for( i in ..5 ) { break; }
			    """,
			    context );
		} );
	}

	@DisplayName( "for-in over fully-open range (..) throws" )
	@Test
	public void testForInFullyOpenRangeThrows() {
		org.junit.jupiter.api.Assertions.assertThrows( Exception.class, () -> {
			instance.executeSource(
			    """
			    for( i in .. ) { break; }
			    """,
			    context );
		} );
	}

	@DisplayName( "toArray on open-start range (..5) throws" )
	@Test
	public void testToArrayOpenStartRangeThrows() {
		org.junit.jupiter.api.Assertions.assertThrows( Exception.class, () -> {
			instance.executeSource(
			    """
			    result = (..5).toArray();
			    """,
			    context );
		} );
	}

	@DisplayName( "toArray on fully-open range (..) throws" )
	@Test
	public void testToArrayFullyOpenRangeThrows() {
		org.junit.jupiter.api.Assertions.assertThrows( Exception.class, () -> {
			instance.executeSource(
			    """
			    result = (..).toArray();
			    """,
			    context );
		} );
	}

	@DisplayName( "stream on open-start range (..5) throws" )
	@Test
	public void testStreamOpenStartRangeThrows() {
		org.junit.jupiter.api.Assertions.assertThrows( Exception.class, () -> {
			instance.executeSource(
			    """
			    result = (..5).stream().limit(1).toList();
			    """,
			    context );
		} );
	}

	@DisplayName( "stream on fully-open range (..) throws" )
	@Test
	public void testStreamFullyOpenRangeThrows() {
		org.junit.jupiter.api.Assertions.assertThrows( Exception.class, () -> {
			instance.executeSource(
			    """
			    result = (..).stream().limit(1).toList();
			    """,
			    context );
		} );
	}

	// ======================== Member Method Tests from BoxLang ========================

	@DisplayName( "isEmpty() returns false for non-empty range" )
	@Test
	public void testIsEmptyFalse() {
		instance.executeSource(
		    """
		    result = (1..5).isEmpty();
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( false );
	}

	@DisplayName( "isEmpty() returns true for empty exclusive range" )
	@Test
	public void testIsEmptyTrueExclusive() {
		instance.executeSource(
		    """
		    result = (1>..<1).isEmpty();
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( true );
	}

	@DisplayName( "isEmpty() returns true for ascending desc() range" )
	@Test
	public void testIsEmptyDescOnAscending() {
		instance.executeSource(
		    """
		    result = (1..5).desc().isEmpty();
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( true );
	}

	@DisplayName( "isAscending() returns true for ascending range" )
	@Test
	public void testIsAscending() {
		instance.executeSource(
		    """
		    a = (1..10).isAscending();
		    b = (10..1).isAscending();
		    """,
		    context );
		assertThat( variables.get( Key.of( "a" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "b" ) ) ).isEqualTo( false );
	}

	@DisplayName( "isIterable() returns true for iterable ranges, false for contains-only" )
	@Test
	public void testIsIterable() {
		instance.executeSource(
		    """
		    a = (1..5).isIterable();
		    b = ("aaa".."zzz").isIterable();
		    c = (1..).isIterable();
		    """,
		    context );
		assertThat( variables.get( Key.of( "a" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "b" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "c" ) ) ).isEqualTo( true );
	}

	@DisplayName( "isBounded() returns true when both bounds present" )
	@Test
	public void testIsBoundedMember() {
		instance.executeSource(
		    """
		    a = (1..5).isBounded();
		    b = (1..).isBounded();
		    c = (..5).isBounded();
		    d = (..).isBounded();
		    """,
		    context );
		assertThat( variables.get( Key.of( "a" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "b" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "c" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "d" ) ) ).isEqualTo( false );
	}

	@DisplayName( "isUnbounded() returns true only for fully open range" )
	@Test
	public void testIsUnboundedMember() {
		instance.executeSource(
		    """
		    a = (..).isUnbounded();
		    b = (1..).isUnbounded();
		    c = (1..5).isUnbounded();
		    """,
		    context );
		assertThat( variables.get( Key.of( "a" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "b" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "c" ) ) ).isEqualTo( false );
	}

	@DisplayName( "isHalfBounded() returns true for one-sided ranges" )
	@Test
	public void testIsHalfBoundedMember() {
		instance.executeSource(
		    """
		    a = (1..).isHalfBounded();
		    b = (..5).isHalfBounded();
		    c = (1..5).isHalfBounded();
		    d = (..).isHalfBounded();
		    """,
		    context );
		assertThat( variables.get( Key.of( "a" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "b" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "c" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "d" ) ) ).isEqualTo( false );
	}

	// ======================== Exclusive Range For-In from BoxLang Syntax ========================

	@DisplayName( "for-in over right exclusive range 1..<5" )
	@Test
	public void testForInRightExclusiveRange() {
		instance.executeSource(
		    """
		    result = [];
		    for( i in 1..<5 ) {
		        result.append( i );
		    }
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( Array.of( 1, 2, 3, 4 ) );
	}

	@DisplayName( "for-in over left exclusive range 1>..5" )
	@Test
	public void testForInLeftExclusiveRange() {
		instance.executeSource(
		    """
		    result = [];
		    for( i in 1>..5 ) {
		        result.append( i );
		    }
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( Array.of( 2, 3, 4, 5 ) );
	}

	@DisplayName( "for-in over full exclusive range 1>..<5" )
	@Test
	public void testForInFullExclusiveRange() {
		instance.executeSource(
		    """
		    result = [];
		    for( i in 1>..<5 ) {
		        result.append( i );
		    }
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( Array.of( 2, 3, 4 ) );
	}

	@DisplayName( "for-in over exclusive half-bounded range 1>.. with break" )
	@Test
	public void testForInExclusiveHalfBoundedRange() {
		instance.executeSource(
		    """
		    result = [];
		    for( i in 1>.. ) {
		        result.append( i );
		        if( i == 5 ) break;
		    }
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( Array.of( 2, 3, 4, 5 ) );
	}

	// ======================== step() on Half-Bounded Range ========================

	@DisplayName( "step() on half-bounded range iterates with custom step" )
	@Test
	public void testStepOnHalfBoundedRange() {
		instance.executeSource(
		    """
		    result = [];
		    for( i in (1..).step(3) ) {
		        result.append( i );
		        if( i >= 10 ) break;
		    }
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( Array.of( 1, 4, 7, 10 ) );
	}

	// ======================== asc() / desc() Iteration Verification ========================

	@DisplayName( "asc() on descending range produces empty range (bounds preserved)" )
	@Test
	public void testAscOnDescendingProducesEmpty() {
		instance.executeSource(
		    """
		    result = arrayLen( (5..1).asc() );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 0 );
	}

	@DisplayName( "desc() on ascending range produces empty iteration" )
	@Test
	public void testDescOnAscendingProducesEmpty() {
		instance.executeSource(
		    """
		    result = arrayLen( (1..5).desc() );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( 0 );
	}

	@DisplayName( "desc() on descending range keeps order" )
	@Test
	public void testDescOnDescendingKeepsOrder() {
		instance.executeSource(
		    """
		    result = arrayToList( (5..1).desc(), "," );
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "5,4,3,2,1" );
	}

	@DisplayName( "asc() on ascending range keeps order" )
	@Test
	public void testAscOnAscendingKeepsOrder() {
		instance.executeSource(
		    """
		    result = arrayToList( (1..5).asc(), "," );
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "1,2,3,4,5" );
	}

	// ======================== equals() with step and exclusivity ========================

	@DisplayName( "ranges with different steps are not equal" )
	@Test
	public void testRangesWithDifferentStepsNotEqual() {
		instance.executeSource(
		    """
		    r1 = (1..10).step(2);
		    r2 = (1..10).step(3);
		    result = r1.equals( r2 );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( false );
	}

	@DisplayName( "ranges with same step are equal" )
	@Test
	public void testRangesWithSameStepEqual() {
		instance.executeSource(
		    """
		    r1 = (1..10).step(2);
		    r2 = (1..10).step(2);
		    result = r1.equals( r2 );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( true );
	}

	@DisplayName( "inclusive and exclusive ranges with same bounds are not equal" )
	@Test
	public void testInclusiveVsExclusiveNotEqual() {
		instance.executeSource(
		    """
		    r1 = 1..5;
		    r2 = 1..<5;
		    result = r1.equals( r2 );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( false );
	}

	@DisplayName( "two exclusive ranges with same bounds and type are equal" )
	@Test
	public void testTwoExclusiveRangesEqual() {
		instance.executeSource(
		    """
		    r1 = 1..<5;
		    r2 = 1..<5;
		    result = r1.equals( r2 );
		    """,
		    context );
		assertThat( variables.get( resultKey ) ).isEqualTo( true );
	}

	// ======================== Comparable Ranges with Uncommon Types ========================

	@DisplayName( "boolean range contains works" )
	@Test
	public void testBooleanRange() {
		instance.executeSource(
		    """
		    r = false..true;
		    a = r.contains( false );
		    b = r.contains( true );
		    """,
		    context );
		assertThat( variables.get( Key.of( "a" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "b" ) ) ).isEqualTo( true );
	}

	// ======================== Invalid Inputs That Should Error ========================

	@DisplayName( "range from array to array throws" )
	@Test
	public void testRangeFromArraysThrows() {
		org.junit.jupiter.api.Assertions.assertThrows( Exception.class, () -> {
			instance.executeSource(
			    """
			    result = [1,2]..[3,4];
			    """,
			    context );
		} );
	}

	@DisplayName( "range from struct to struct throws" )
	@Test
	public void testRangeFromStructsThrows() {
		org.junit.jupiter.api.Assertions.assertThrows( Exception.class, () -> {
			instance.executeSource(
			    """
			    result = {a:1}..{b:2};
			    """,
			    context );
		} );
	}

	@DisplayName( "range from number to string throws" )
	@Test
	public void testRangeFromNumberToStringThrows() {
		org.junit.jupiter.api.Assertions.assertThrows( Exception.class, () -> {
			instance.executeSource(
			    """
			    result = 1.."hello";
			    """,
			    context );
		} );
	}

	@DisplayName( "range from string to number throws" )
	@Test
	public void testRangeFromStringToNumberThrows() {
		org.junit.jupiter.api.Assertions.assertThrows( Exception.class, () -> {
			instance.executeSource(
			    """
			    result = "hello"..5;
			    """,
			    context );
		} );
	}

	@DisplayName( "range from closure to closure throws" )
	@Test
	public void testRangeFromClosuresThrows() {
		org.junit.jupiter.api.Assertions.assertThrows( Exception.class, () -> {
			instance.executeSource(
			    """
			    a = () => 1;
			    b = () => 2;
			    result = a..b;
			    """,
			    context );
		} );
	}

	@DisplayName( "range from query to query throws" )
	@Test
	public void testRangeFromQueriesToQueryThrows() {
		org.junit.jupiter.api.Assertions.assertThrows( Exception.class, () -> {
			instance.executeSource(
			    """
			    q1 = queryNew( "id", "integer" );
			    q2 = queryNew( "id", "integer" );
			    result = q1..q2;
			    """,
			    context );
		} );
	}

	// ======================== Complex Expressions as Range Operands ========================

	@DisplayName( "range from method call results" )
	@Test
	public void testRangeFromMethodCalls() {
		instance.executeSource(
		    """
		    result = arrayToList( abs(-3)..abs(-7), "," );
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "3,4,5,6,7" );
	}

	@DisplayName( "range from arithmetic expressions" )
	@Test
	public void testRangeFromArithmeticExpressions() {
		instance.executeSource(
		    """
		    result = arrayToList( (2+1)..(2*5), "," );
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "3,4,5,6,7,8,9,10" );
	}

	@DisplayName( "range from ternary expressions" )
	@Test
	public void testRangeFromTernaryExpressions() {
		instance.executeSource(
		    """
		    x = true;
		    result = arrayToList( (x ? 1 : 10)..(x ? 5 : 20), "," );
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "1,2,3,4,5" );
	}

	@DisplayName( "range from parenthetical sub-expressions" )
	@Test
	public void testRangeFromParenthetical() {
		instance.executeSource(
		    """
		    result = arrayToList( (((1)))..(((5))), "," );
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "1,2,3,4,5" );
	}

	@DisplayName( "range from struct key access" )
	@Test
	public void testRangeFromStructKeyAccess() {
		instance.executeSource(
		    """
		    s = { low: 1, high: 5 };
		    result = arrayToList( s.low..s.high, "," );
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "1,2,3,4,5" );
	}

	@DisplayName( "range from array index access" )
	@Test
	public void testRangeFromArrayIndexAccess() {
		instance.executeSource(
		    """
		    arr = [3, 8];
		    result = arrayToList( arr[1]..arr[2], "," );
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "3,4,5,6,7,8" );
	}

	@DisplayName( "range from function call returning number" )
	@Test
	public void testRangeFromFunctionCall() {
		instance.executeSource(
		    """
		    function getStart() { return 2; }
		    function getEnd() { return 6; }
		    result = arrayToList( getStart()..getEnd(), "," );
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "2,3,4,5,6" );
	}

	@DisplayName( "range from string concatenation producing numeric strings" )
	@Test
	public void testRangeFromStringConcat() {
		instance.executeSource(
		    """
		    a = "1";
		    b = "0";
		    result = arrayToList( (a & b).."15", "," );
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "10,11,12,13,14,15" );
	}

	@DisplayName( "range from nested range length" )
	@Test
	public void testRangeFromNestedRangeLength() {
		instance.executeSource(
		    """
		    result = arrayToList( 1..arrayLen(1..5), "," );
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "1,2,3,4,5" );
	}

	@DisplayName( "range from null coalescing expression" )
	@Test
	public void testRangeFromElvisOperator() {
		instance.executeSource(
		    """
		    x = nullValue();
		    result = arrayToList( (x ?: 1)..(x ?: 5), "," );
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "1,2,3,4,5" );
	}

	@DisplayName( "range from unary negation" )
	@Test
	public void testRangeFromUnaryNegation() {
		instance.executeSource(
		    """
		    result = arrayToList( (-5)..(-1), "," );
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "-5,-4,-3,-2,-1" );
	}

	@DisplayName( "range from chained method calls" )
	@Test
	public void testRangeFromChainedMethodCalls() {
		instance.executeSource(
		    """
		    result = arrayToList( " 2 ".trim().. " 6 ".trim(), "," );
		    """,
		    context );
		assertThat( variables.getAsString( resultKey ) ).isEqualTo( "2,3,4,5,6" );
	}

	// ======================== Custom IRangeable: Roman Numeral ========================

	@DisplayName( "Roman numeral IRangeable: iteration, step, contains" )
	@Test
	public void testRomanNumeralIRangeable() {
		instance.executeSource(
		    """
		    class Roman implements="java:ortus.boxlang.runtime.types.IRangeable" {
		        property name="value" type="integer" default=0;

		        static {
		            VALUES = [1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1];
		            SYMBOLS = ["M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"];
		        }

		        function init( input ) {
		            variables.value = isNumeric( input ) ? int( input ) : static.fromRoman( input );
		            return this;
		        }

		        static function fromRoman( s ) {
		            s = uCase( s );
		            return static.VALUES.reduce( ( acc, val, idx ) => {
		                var sym = static.SYMBOLS[ idx ];
		                while( mid( s, acc.pos, sym.len() ) == sym ) {
		                    acc.result += val;
		                    acc.pos += sym.len();
		                }
		                return acc;
		            }, { result: 0, pos: 1 } ).result;
		        }

		        static function toRoman( num ) {
		            return static.VALUES.reduce( ( result, val, idx ) => {
		                var count = int( num / val );
		                num -= count * val;
		                return result & repeatString( static.SYMBOLS[ idx ], count );
		            }, "" );
		        }

		        function toString() { return static.toRoman( variables.value ); }

		        function rangeAdvance( step ) { return new Roman( variables.value + step ); }
		        function rangeCompare( other ) { return variables.value - other.getValue(); }

		        function rangeCoerce( val ) {
		            if( isInstanceOf( val, "Roman" ) ) return val;
		            if( isNumeric( val ) ) return new Roman( val );
		            if( isSimpleValue( val ) ) return new Roman( val );
		            return null;
		        }
		    }

		    // --- Iteration I..X ---
		    iterResult = [];
		    for( r in new Roman("I")..new Roman("X") ) {
		        iterResult.append( r.toString() );
		    }

		    // --- Step by 2 ---
		    stepResult = [];
		    for( r in (new Roman("I")..new Roman("X")).step(2) ) {
		        stepResult.append( r.toString() );
		    }

		    // --- Contains with Roman instances ---
		    range = new Roman("I")..new Roman("C");
		    c5 = range.contains( new Roman("V") );
		    c50 = range.contains( new Roman("L") );
		    cOut = range.contains( new Roman("D") );

		    // --- Contains with raw numbers (rangeCoerce) ---
		    cNum5 = range.contains( 5 );
		    cNum50 = range.contains( 50 );
		    cNum500 = range.contains( 500 );

		    // --- Contains with string (rangeCoerce) ---
		    cStrV = range.contains( "V" );
		    cStrL = range.contains( "L" );
		    cStrD = range.contains( "D" );
		    """,
		    context );

		// Iteration assertions
		assertThat( variables.get( Key.of( "iterResult" ) ) ).isEqualTo( Array.of( "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X" ) );

		// Step assertions
		assertThat( variables.get( Key.of( "stepResult" ) ) ).isEqualTo( Array.of( "I", "III", "V", "VII", "IX" ) );

		// Contains with Roman instances
		assertThat( variables.get( Key.of( "c5" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "c50" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "cOut" ) ) ).isEqualTo( false );

		// Contains with raw numbers
		assertThat( variables.get( Key.of( "cNum5" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "cNum50" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "cNum500" ) ) ).isEqualTo( false );

		// Contains with strings
		assertThat( variables.get( Key.of( "cStrV" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "cStrL" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "cStrD" ) ) ).isEqualTo( false );
	}

	// ======================== Custom IRangeable: Musical Note with Unit Stepping ========================

	@DisplayName( "Musical Note IRangeable: chromatic, whole, third, octave, major, minor" )
	@Test
	public void testMusicalNoteIRangeable() {
		instance.executeSource(
		    """
		    class Note implements="java:ortus.boxlang.runtime.types.IRangeable" {
		        property name="midi" type="integer" default=60;

		        static {
		            NOTES = ["C", "C##", "D", "D##", "E", "F", "F##", "G", "G##", "A", "A##", "B"];
		            MAJOR = [2, 2, 1, 2, 2, 2, 1];
		            MINOR = [2, 1, 2, 2, 1, 2, 2];
		            SHARP = "##";
		        }

		        function init( input ) {
		            variables.midi = isNumeric( input ) ? int( input ) : static.parseName( input );
		            return this;
		        }

		        static function parseName( name ) {
		            name = uCase( name );
		            var hasSharp = name.len() > 2 && mid( name, 2, 1 ) == static.SHARP;
		            var notePart = hasSharp ? left( name, 2 ) : left( name, 1 );
		            var octave = int( right( name, name.len() - notePart.len() ) );
		            var idx = arrayFind( static.NOTES, notePart );
		            return ( octave + 1 ) * 12 + idx - 1;
		        }

		        function toString() {
		            return static.NOTES[ variables.midi mod 12 + 1 ] & ( int( variables.midi / 12 ) - 1 );
		        }

		        function rangeAdvance( step ) { return new Note( variables.midi + step ); }
		        function rangeCompare( other ) { return variables.midi - other.getMidi(); }

		        function rangeCoerce( val ) {
		            if( isInstanceOf( val, "Note" ) ) return val;
		            if( isNumeric( val ) ) return new Note( val );
		            if( isSimpleValue( val ) ) return new Note( val );
		            return null;
		        }

		        function rangeStepFromUnit( amount, unit ) {
		            switch( unit ) {
		                case "chromatic": return amount;
		                case "whole": return amount * 2;
		                case "third": return amount * 4;
		                case "octave": return amount * 12;
		            }
		            throw( message: "Unsupported unit: " & unit );
		        }

		        function rangeUnitStepper( unit ) {
		            if( unit != "major" && unit != "minor" ) return null;
		            var root = variables.midi;
		            var intervals = ( unit == "major" ) ? static.MAJOR : static.MINOR;
		            var cumulative = [0];
		            for( var i = 1; i <= 7; i++ ) {
		                cumulative.append( cumulative[ i ] + intervals[ i ] );
		            }
		            return ( current, amount ) => {
		                var offset = current.getMidi() - root;
		                var octaves = int( offset / 12 );
		                var remainder = offset mod 12;
		                var degree = 0;
		                for( var i = 2; i <= cumulative.len(); i++ ) {
		                    if( cumulative[ i ] <= remainder ) degree = i - 1;
		                }
		                var newDegree = octaves * 7 + degree + amount;
		                var newOctaves = int( newDegree / 7 );
		                var newDegreeInOctave = newDegree mod 7;
		                return new Note( root + ( newOctaves * 12 ) + cumulative[ newDegreeInOctave + 1 ] );
		            };
		        }
		    }

		    // --- Chromatic: C4 to D4 ---
		    chromResult = [];
		    for( n in (new Note("C4")..new Note("D4")).step( 1, "chromatic" ) ) {
		        chromResult.append( n.toString() );
		    }

		    // --- Whole steps: C4 to C5 ---
		    wholeResult = [];
		    for( n in (new Note("C4")..new Note("C5")).step( 1, "whole" ) ) {
		        wholeResult.append( n.toString() );
		    }

		    // --- Major thirds: C4 to C6 ---
		    thirdResult = [];
		    for( n in (new Note("C4")..new Note("C6")).step( 1, "third" ) ) {
		        thirdResult.append( n.toString() );
		    }

		    // --- Octaves: C4 to C7 ---
		    octaveResult = [];
		    for( n in (new Note("C4")..new Note("C7")).step( 1, "octave" ) ) {
		        octaveResult.append( n.toString() );
		    }

		    // --- C Major scale: C4 to C5 ---
		    majorResult = [];
		    for( n in (new Note("C4")..new Note("C5")).step( 1, "major" ) ) {
		        majorResult.append( n.toString() );
		    }

		    // --- A Natural Minor scale: A3 to A4 ---
		    minorResult = [];
		    for( n in (new Note("A3")..new Note("A4")).step( 1, "minor" ) ) {
		        minorResult.append( n.toString() );
		    }

		    // --- Contains tests with all coercible input types ---
		    noteRange = new Note("C4")..new Note("C5");

		    // Contains with Note instances
		    cNoteE4   = noteRange.contains( new Note("E4") );
		    cNoteG4   = noteRange.contains( new Note("G4") );
		    cNoteC5   = noteRange.contains( new Note("C5") );
		    cNoteD5   = noteRange.contains( new Note("D5") );
		    cNoteB3   = noteRange.contains( new Note("B3") );

		    // Contains with MIDI numbers (integer coercion)
		    cMidi60   = noteRange.contains( 60 );  // C4
		    cMidi64   = noteRange.contains( 64 );  // E4
		    cMidi67   = noteRange.contains( 67 );  // G4
		    cMidi72   = noteRange.contains( 72 );  // C5
		    cMidi73   = noteRange.contains( 73 );  // C#5 (out)
		    cMidi59   = noteRange.contains( 59 );  // B3  (out)

		    // Contains with string note names (string coercion)
		    cStrE4    = noteRange.contains( "E4" );
		    cStrFs4   = noteRange.contains( "F##4" );  // F#4
		    cStrC4    = noteRange.contains( "C4" );
		    cStrC5    = noteRange.contains( "C5" );
		    cStrD5    = noteRange.contains( "D5" );
		    cStrB3    = noteRange.contains( "B3" );

		    // Contains with numeric strings (coerced as note name attempt)
		    cNumStr60 = noteRange.contains( "60" );

		    // --- Contains on a STEPPED range: bounds vs reachability ---
		    // Range C4..C5 stepped by thirds visits: C4, E4, G#4, C5
		    thirdRange = (new Note("C4")..new Note("C5")).step( 1, "third" );

		    // Values ON the step sequence
		    cStepC4   = thirdRange.contains( "C4" );   // start - on step
		    cStepE4   = thirdRange.contains( "E4" );   // 2nd step value
		    cStepGs4  = thirdRange.contains( "G##4" ); // 3rd step value
		    cStepC5   = thirdRange.contains( "C5" );   // end - on step

		    // Values WITHIN bounds but NOT on the step sequence
		    cStepD4   = thirdRange.contains( "D4" );   // between C4 and E4
		    cStepF4   = thirdRange.contains( "F4" );   // between E4 and G#4
		    cStepA4   = thirdRange.contains( "A4" );   // between G#4 and C5
		    cStepMidi62 = thirdRange.contains( 62 );   // D4 as MIDI

		    // Values OUTSIDE bounds
		    cStepB3   = thirdRange.contains( "B3" );   // below start
		    cStepD5   = thirdRange.contains( "D5" );   // above end

		    // --- Full chromatic scale from C4 using stream + limit ---
		    chromaticScale = (new Note("C4")..).step( 1, "chromatic" )
		        .stream()
		        .limit( 13 )
		        .map( n => n.toString() )
		        .toList();
		    """,
		    context );

		// Chromatic: C4, C#4, D4
		assertThat( variables.get( Key.of( "chromResult" ) ) ).isEqualTo( Array.of( "C4", "C#4", "D4" ) );

		// Whole steps: C4, D4, E4, F#4, G#4, A#4, C5
		assertThat( variables.get( Key.of( "wholeResult" ) ) ).isEqualTo( Array.of( "C4", "D4", "E4", "F#4", "G#4", "A#4", "C5" ) );

		// Major thirds: C4, E4, G#4, C5, E5, G#5, C6
		assertThat( variables.get( Key.of( "thirdResult" ) ) ).isEqualTo( Array.of( "C4", "E4", "G#4", "C5", "E5", "G#5", "C6" ) );

		// Octaves: C4, C5, C6, C7
		assertThat( variables.get( Key.of( "octaveResult" ) ) ).isEqualTo( Array.of( "C4", "C5", "C6", "C7" ) );

		// C Major: C4, D4, E4, F4, G4, A4, B4, C5
		assertThat( variables.get( Key.of( "majorResult" ) ) ).isEqualTo( Array.of( "C4", "D4", "E4", "F4", "G4", "A4", "B4", "C5" ) );

		// A Natural Minor: A3, B3, C4, D4, E4, F4, G4, A4
		assertThat( variables.get( Key.of( "minorResult" ) ) ).isEqualTo( Array.of( "A3", "B3", "C4", "D4", "E4", "F4", "G4", "A4" ) );

		// Contains with Note instances
		assertThat( variables.get( Key.of( "cNoteE4" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "cNoteG4" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "cNoteC5" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "cNoteD5" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "cNoteB3" ) ) ).isEqualTo( false );

		// Contains with MIDI numbers
		assertThat( variables.get( Key.of( "cMidi60" ) ) ).isEqualTo( true );   // C4
		assertThat( variables.get( Key.of( "cMidi64" ) ) ).isEqualTo( true );   // E4
		assertThat( variables.get( Key.of( "cMidi67" ) ) ).isEqualTo( true );   // G4
		assertThat( variables.get( Key.of( "cMidi72" ) ) ).isEqualTo( true );   // C5
		assertThat( variables.get( Key.of( "cMidi73" ) ) ).isEqualTo( false );  // C#5 out
		assertThat( variables.get( Key.of( "cMidi59" ) ) ).isEqualTo( false );  // B3 out

		// Contains with string note names
		assertThat( variables.get( Key.of( "cStrE4" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "cStrFs4" ) ) ).isEqualTo( true );   // F#4
		assertThat( variables.get( Key.of( "cStrC4" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "cStrC5" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "cStrD5" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "cStrB3" ) ) ).isEqualTo( false );

		// Contains with numeric string
		assertThat( variables.get( Key.of( "cNumStr60" ) ) ).isEqualTo( true ); // "60" coerced as MIDI number

		// Contains on STEPPED range: contains checks STEP REACHABILITY
		// thirdRange visits C4, E4, G#4, C5

		// Values on the step sequence — reachable, so true
		assertThat( variables.get( Key.of( "cStepC4" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "cStepE4" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "cStepGs4" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "cStepC5" ) ) ).isEqualTo( true );

		// Values within bounds but NOT on the step sequence — false (not reachable)
		assertThat( variables.get( Key.of( "cStepD4" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "cStepF4" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "cStepA4" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "cStepMidi62" ) ) ).isEqualTo( false );

		// Values outside bounds — false
		assertThat( variables.get( Key.of( "cStepB3" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "cStepD5" ) ) ).isEqualTo( false );

		// Full chromatic scale from C4 (12 notes)
		assertThat( variables.get( Key.of( "chromaticScale" ) ) )
		    .isEqualTo( Array.of( "C4", "C#4", "D4", "D#4", "E4", "F4", "F#4", "G4", "G#4", "A4", "A#4", "B4", "C5" ) );
	}

	@DisplayName( "Musical Note: step-reachability via contains() for bounded and half-bounded ranges" )
	@Test
	public void testMusicalNoteStepReachability() {
		instance.executeSource(
		    """
		    class Note implements="java:ortus.boxlang.runtime.types.IRangeable" {
		        property name="midi" type="integer" default=60;

		        static {
		            NOTES = ["C", "C##", "D", "D##", "E", "F", "F##", "G", "G##", "A", "A##", "B"];
		            MAJOR = [2, 2, 1, 2, 2, 2, 1];
		            MINOR = [2, 1, 2, 2, 1, 2, 2];
		            SHARP = "##";
		        }

		        function init( input ) {
		            variables.midi = isNumeric( input ) ? int( input ) : static.parseName( input );
		            return this;
		        }

		        static function parseName( name ) {
		            name = uCase( name );
		            var hasSharp = name.len() > 2 && mid( name, 2, 1 ) == static.SHARP;
		            var notePart = hasSharp ? left( name, 2 ) : left( name, 1 );
		            var octave = int( right( name, name.len() - notePart.len() ) );
		            var idx = arrayFind( static.NOTES, notePart );
		            return ( octave + 1 ) * 12 + idx - 1;
		        }

		        function toString() {
		            return static.NOTES[ variables.midi mod 12 + 1 ] & ( int( variables.midi / 12 ) - 1 );
		        }

		        function rangeAdvance( step ) { return new Note( variables.midi + step ); }
		        function rangeCompare( other ) { return variables.midi - other.getMidi(); }

		        function rangeCoerce( val ) {
		            if( isInstanceOf( val, "Note" ) ) return val;
		            if( isNumeric( val ) ) return new Note( val );
		            if( isSimpleValue( val ) ) return new Note( val );
		            return null;
		        }

		        function rangeStepFromUnit( amount, unit ) {
		            switch( unit ) {
		                case "chromatic": return amount;
		                case "whole": return amount * 2;
		                case "third": return amount * 4;
		                case "octave": return amount * 12;
		            }
		            throw( message: "Unsupported unit: " & unit );
		        }

		        function rangeUnitStepper( unit ) {
		            if( unit != "major" && unit != "minor" ) return null;
		            var root = variables.midi;
		            var intervals = ( unit == "major" ) ? static.MAJOR : static.MINOR;
		            var cumulative = [0];
		            for( var i = 1; i <= 7; i++ ) {
		                cumulative.append( cumulative[ i ] + intervals[ i ] );
		            }
		            return ( current, amount ) => {
		                var offset = current.getMidi() - root;
		                var octaves = int( offset / 12 );
		                var remainder = offset mod 12;
		                var degree = 0;
		                for( var i = 2; i <= cumulative.len(); i++ ) {
		                    if( cumulative[ i ] <= remainder ) degree = i - 1;
		                }
		                var newDegree = octaves * 7 + degree + amount;
		                var newOctaves = int( newDegree / 7 );
		                var newDegreeInOctave = newDegree mod 7;
		                return new Note( root + ( newOctaves * 12 ) + cumulative[ newDegreeInOctave + 1 ] );
		            };
		        }
		    }

		    // Bounded: Is E4 reachable by major thirds from C4 to C6?
		    // Thirds from C4: C4, E4, G#4, C5, E5, G#5, C6
		    e4OnThirds = (new Note("C4")..new Note("C6")).step( 1, "third" ).contains( "E4" );
		    d4OnThirds = (new Note("C4")..new Note("C6")).step( 1, "third" ).contains( "D4" );

		    // Bounded: C major scale notes
		    e4InMajor = (new Note("C4")..new Note("C5")).step( 1, "major" ).contains( "E4" );
		    eb4InMajor = (new Note("C4")..new Note("C5")).step( 1, "major" ).contains( "D##4" );

		    // Half-bounded: Is C6 reachable by octaves from C4?
		    g7OnOctaves = (new Note("C4")..).step( 1, "octave" ).contains( "G7" );
		    c6OnOctaves = (new Note("C4")..).step( 1, "octave" ).contains( "C6" );

		    // Half-bounded: major scale reachability
		    fs5InMajor = (new Note("C4")..).step( 1, "major" ).contains( "F##5" );
		    f5InMajor = (new Note("C4")..).step( 1, "major" ).contains( "F5" );
		    """,
		    context );

		// Bounded: thirds
		assertThat( variables.get( Key.of( "e4OnThirds" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "d4OnThirds" ) ) ).isEqualTo( false );

		// Bounded: major scale
		assertThat( variables.get( Key.of( "e4InMajor" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "eb4InMajor" ) ) ).isEqualTo( false );

		// Half-bounded: octaves
		assertThat( variables.get( Key.of( "g7OnOctaves" ) ) ).isEqualTo( false );  // G7 not on C octave ladder
		assertThat( variables.get( Key.of( "c6OnOctaves" ) ) ).isEqualTo( true );   // C6 IS on C octave ladder

		// Half-bounded: major scale
		assertThat( variables.get( Key.of( "fs5InMajor" ) ) ).isEqualTo( false );   // F#5 not in C major
		assertThat( variables.get( Key.of( "f5InMajor" ) ) ).isEqualTo( true );     // F5 IS in C major
	}

	// ======================== Custom IRangeable: Fibonacci Sequence ========================

	@DisplayName( "Fibonacci IRangeable: lazy infinite sequence with contains" )
	@Test
	public void testFibonacciIRangeable() {
		instance.executeSource(
		    """
		    class Fib implements="java:ortus.boxlang.runtime.types.IRangeable" {
		        property name="prev" type="integer" default=0;
		        property name="current" type="integer" default=1;

		        function rangeAdvance( step ) {
		            var result = this;
		            for( var i = 1; i <= step; i++ ) {
		                result = new Fib( prev: result.getCurrent(), current: result.getPrev() + result.getCurrent() );
		            }
		            return result;
		        }

		        function rangeCompare( other ) { return variables.current - other.getCurrent(); }

		        function rangeCoerce( val ) {
		            if( isInstanceOf( val, "Fib" ) ) return val;
		            if( isNumeric( val ) ) return new Fib( current: int(val) );
		            return null;
		        }
		    }

		    // First 10 Fibonacci numbers
		    first10 = (new Fib()..).stream().limit(10).map( .getCurrent() ).toList();

		    // Contains: is 13 a Fibonacci number?
		    has13 = (new Fib()..).contains( 13 );

		    // 14 is NOT a Fibonacci number
		    has14 = (new Fib()..).contains( 14 );

		    // Step by 2: every other Fibonacci number
		    everyOther = (new Fib()..).step(2).stream().limit(5).map( .getCurrent() ).toList();

		    // For-in with break: collect until > 100
		    under100 = [];
		    for( f in new Fib().. ) {
		        if( f.getCurrent() > 100 ) break;
		        under100.append( f.getCurrent() );
		    }
		    """,
		    context );

		// First 10: 1, 1, 2, 3, 5, 8, 13, 21, 34, 55
		assertThat( variables.get( Key.of( "first10" ) ) ).isEqualTo( Array.of( 1, 1, 2, 3, 5, 8, 13, 21, 34, 55 ) );

		// Contains checks
		assertThat( variables.get( Key.of( "has13" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "has14" ) ) ).isEqualTo( false );

		// Every other: 1, 2, 5, 13, 34
		assertThat( variables.get( Key.of( "everyOther" ) ) ).isEqualTo( Array.of( 1, 2, 5, 13, 34 ) );

		// Under 100: 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89
		assertThat( variables.get( Key.of( "under100" ) ) ).isEqualTo( Array.of( 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89 ) );
	}
}
