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
package ortus.boxlang.runtime.dynamic.casters;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.JavaMethod;
import ortus.boxlang.runtime.types.SampleUDF;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxLangException;

public class FunctionCasterTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

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

	@DisplayName( "It can cast a Function to a Function" )
	@Test
	void testItCanCastAFunction() {
		Function func = new SampleUDF( null, Key.of( "Func" ), null, null, null );
		assertThat( FunctionCaster.cast( func ).getName() ).isEqualTo( Key.of( "Func" ) );
	}

	@DisplayName( "It can not cast a non-function" )
	@Test
	void testItCanNotCastANonFunction() {
		Double k;
		assertThrows( BoxLangException.class, () -> FunctionCaster.cast( new HashMap<>() ) );
		assertThrows( BoxLangException.class, () -> FunctionCaster.cast( null ) );
		assertThrows( BoxLangException.class, () -> FunctionCaster.cast( new Object[] {} ) );
		assertThrows( BoxLangException.class, () -> FunctionCaster.cast( new Struct() ) );

	}

	@DisplayName( "It can attempt to cast" )
	@Test
	void testItCanAttemptToCast() {
		Function				func	= new SampleUDF( null, Key.of( "Func" ), null, null, null );
		CastAttempt<Function>	attempt	= FunctionCaster.attempt( func );
		assertThat( attempt.wasSuccessful() ).isTrue();
		assertThat( attempt.get().getName() ).isEqualTo( Key.of( "Func" ) );
		assertThat( attempt.ifSuccessful( ( v ) -> System.out.println( v ) ) );

		final CastAttempt<Function> attempt2 = FunctionCaster.attempt( new HashMap<>() );
		assertThat( attempt2.wasSuccessful() ).isFalse();

		assertThrows( BoxLangException.class, () -> attempt2.get() );
		assertThat( attempt2.ifSuccessful( ( v ) -> System.out.println( v ) ) );
	}

	@DisplayName( "It can cast a Java Lambda to a Function" )
	@Test
	void testItCanCastAJavaLambda() {
		Function myJavaPredicate = FunctionCaster.cast( ( Predicate<String> ) ( t ) -> t.equals( "brad" ), "Predicate" );
		assertThat( myJavaPredicate ).isInstanceOf( Function.class );
		assertThat( myJavaPredicate ).isInstanceOf( JavaMethod.class );
		assertThat( context.invokeFunction( myJavaPredicate, new Object[] { "brad" } ) ).isEqualTo( true );
		assertThat( context.invokeFunction( myJavaPredicate, new Object[] { "luis" } ) ).isEqualTo( false );
	}

	@DisplayName( "It can cast a Java Lambda to a Function" )
	@Test
	void testItCanCastAJavaLambdaAndUseInBL() {
		Predicate<String>												myJavaPredicate				= ( t ) -> t.equals( "brad" );
		Predicate<Map<Key, Object>>										myMapPredicate				= ( t ) -> t.get( Key.of( "name" ) ).equals( "Brad" );
		BiPredicate<String, Object>										myJavaBiPredicate			= ( k, v ) -> k.equals( "brad" );
		Consumer<String>												myJavaConsumer				= ( t ) -> System.out.println( t );
		java.util.function.Function<String, String>						myJavaFunction				= ( t ) -> t.toUpperCase();
		Consumer<Map<Key, Object>>										myJavaMapConsumer			= ( t ) -> System.out.println( t.toString() );
		java.util.function.Function<Map<Key, Object>, Map<Key, Object>>	myJavaMapFunction			= ( t ) -> {
																										t.put( Key.of( "name" ),
																										    t.get( Key.of( "name" ) ).toString()
																										        .toUpperCase() );
																										return t;
																									};
		java.util.function.BiFunction<String, String, String>			myJavaBiFunction			= ( acc, t ) -> acc.concat( t.toUpperCase() );
		java.util.function.BiFunction<Array, Map<Key, Object>, Array>	myJavaBiFunctionQueryReduce	= ( acc, t ) -> {
																										acc.add( t.get( Key.of( "name" ) ) );
																										return acc;
																									};
		BiConsumer<String, Object>										myJavaBiConsumer			= ( k, v ) -> System.out.println( k + " : " + v );
		Comparator<String>												myJavaComparator			= ( a, b ) -> a.compareTo( b );
		Comparator<Map<Key, Object>>									myJavaMapComparator			= ( a, b ) -> a.get( Key.of( "name" ) ).toString()
		    .compareTo( b.get( Key.of( "name" ) ).toString() );

		variables.put( "myJavaPredicate", myJavaPredicate );
		variables.put( "myMapPredicate", myMapPredicate );
		variables.put( "myJavaConsumer", myJavaConsumer );
		variables.put( "myJavaFunction", myJavaFunction );
		variables.put( "myJavaBiFunction", myJavaBiFunction );
		variables.put( "myJavaBiPredicate", myJavaBiPredicate );
		variables.put( "myJavaBiConsumer", myJavaBiConsumer );
		variables.put( "myJavaComparator", myJavaComparator );
		variables.put( "myJavaMapConsumer", myJavaMapConsumer );
		variables.put( "myJavaMapFunction", myJavaMapFunction );
		variables.put( "myJavaMapComparator", myJavaMapComparator );
		variables.put( "myJavaBiFunctionQueryReduce", myJavaBiFunctionQueryReduce );

		instance.executeSource(
		    """
		       castedPredicate = myJavaPredicate castas "function:predicate";
		       directInvoke = castedPredicate( "brad" );
		       castedPredicate2 = myJavaPredicate castas "function:java.util.function.predicate";
		       directInvoke2 = castedPredicate( "luis" );

		       myArry = [ "brad", "luis" ];
		       result = myArry.filter( myJavaPredicate );

		       compareToBrad = "brad" castas "function:java.lang.Comparable";
		       result2 = compareToBrad( "brad" );
		       result3 = compareToBrad( "luis" );
		       result4 = compareToBrad( "alf" );

		       // control
		       result5 = "brad".compareTo( "brad" )
		       result6 = "brad".compareTo( "luis" )
		       result7 = "brad".compareTo( "alf" )

		       myArry.each(myJavaConsumer);

		       result8 = [ "brad", "luis" ].map( myJavaFunction );

		       result9 = ["brad"].every( myJavaPredicate );

		       result10 = ["brad","luis"].reduce( myJavaBiFunction, "" );
		       result11 = ["brad","luis"].reduceRight( myJavaBiFunction, "" );


		       result12 = ["brad","luis"].some( myJavaPredicate );

		    result125 = ["b","a","d","c"].sort( myJavaComparator ).toList();

		       myList = "brad,luis";
		       result13 = myList.listFilter( myJavaPredicate );

		       myList.listEach(myJavaConsumer);

		       result14 = myList.listMap( myJavaFunction );

		       result15 = "brad".listEvery( myJavaPredicate );

		       result16 = myList.listReduce( myJavaBiFunction, "" );
		       result17 = myList.listReduceRight( myJavaBiFunction, "" );

		       result18 = myList.listSome( myJavaPredicate );

		       myStr = {
		       "brad" : "wood",
		       "luis" : "majano"
		       };

		       result19 = myStr.filter( myJavaBiPredicate );

		       result20 = myStr.every( myJavaBiPredicate );

		       result21 = myStr.some( myJavaBiPredicate );

		       result22 = myStr.each( myJavaBiConsumer );

		       result23 = myStr.map( myJavaBiFunction );

		       result24 = {"b":"", "a":"", "d":"", "c":""}.sort(myJavaComparator);

		       result25 = {"b":"", "a":"", "d":"", "c":""}.toSorted(myJavaComparator).keyArray();

		       sorttedStr = structNew( "sorted", myJavaComparator );
		       sortedStr.b = "";
		       sortedStr.a = "";
		       sortedStr.d = "";
		       sortedStr.c = "";
		       result26 = sortedStr.keyArray();

		       myQry = queryNew( "name,position", "varchar,varchar", [ ["Luis","CEO"], ["Jon","Architect"], ["Brad","Chaos Monkey"] ]);
		       result27 = myQry.filter( myMapPredicate );

		       result28 = myQry.every( myMapPredicate );

		       result29 = myQry.some( myMapPredicate );

		       myQry.each( myJavaMapConsumer );
		       result30 = myQry.map( myJavaMapFunction );

		       myQry = queryNew( "name,position", "varchar,varchar", [ ["Luis","CEO"], ["Jon","Architect"], ["Brad","Chaos Monkey"] ]);
		       result31 = myQry.sort( myJavaMapComparator ).columnData("name");

		       myQry = queryNew( "name,position", "varchar,varchar", [ ["Luis","CEO"], ["Jon","Architect"], ["Brad","Chaos Monkey"] ]);
		       result32 = myQry.reduce( myJavaBiFunctionQueryReduce, [] );


		                                   		                                 """,
		    context );
		assertThat( variables.get( "directInvoke" ) ).isEqualTo( true );
		assertThat( variables.get( "directInvoke2" ) ).isEqualTo( false );

		assertThat( variables.getAsArray( result ).size() ).isEqualTo( 1 );
		assertThat( variables.getAsArray( result ).get( 0 ) ).isEqualTo( "brad" );

		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( 0 );
		assertThat( variables.getAsInteger( Key.of( "result3" ) ) < 0 ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "result4" ) ) > 0 ).isTrue();

		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( 0 );
		assertThat( variables.getAsInteger( Key.of( "result6" ) ) < 0 ).isTrue();
		assertThat( variables.getAsInteger( Key.of( "result7" ) ) > 0 ).isTrue();

		assertThat( variables.get( Key.of( "result8" ) ) ).isEqualTo( Array.of( "BRAD", "LUIS" ) );

		assertThat( variables.get( Key.of( "result9" ) ) ).isEqualTo( true );

		assertThat( variables.get( Key.of( "result10" ) ) ).isEqualTo( "BRADLUIS" );
		assertThat( variables.get( Key.of( "result11" ) ) ).isEqualTo( "LUISBRAD" );

		assertThat( variables.get( Key.of( "result12" ) ) ).isEqualTo( true );

		assertThat( variables.get( Key.of( "result125" ) ) ).isEqualTo( "a,b,c,d" );

		assertThat( variables.get( Key.of( "result13" ) ) ).isEqualTo( "brad" );

		assertThat( variables.get( Key.of( "result14" ) ) ).isEqualTo( "BRAD,LUIS" );

		assertThat( variables.get( Key.of( "result15" ) ) ).isEqualTo( true );

		assertThat( variables.get( Key.of( "result16" ) ) ).isEqualTo( "BRADLUIS" );
		assertThat( variables.get( Key.of( "result17" ) ) ).isEqualTo( "LUISBRAD" );

		assertThat( variables.get( Key.of( "result18" ) ) ).isEqualTo( true );

		assertThat( variables.get( Key.of( "result19" ) ) ).isEqualTo( Struct.of( "brad", "wood" ) );

		assertThat( variables.get( Key.of( "result20" ) ) ).isEqualTo( false );

		assertThat( variables.get( Key.of( "result21" ) ) ).isEqualTo( true );

		assertThat( variables.get( Key.of( "result23" ) ) ).isEqualTo( Struct.of( "brad", "bradWOOD", "luis", "luisMAJANO" ) );

		assertThat( variables.get( Key.of( "result24" ) ) ).isEqualTo( Array.of( "a", "b", "c", "d" ) );

		assertThat( variables.get( Key.of( "result25" ) ) ).isEqualTo( Array.of( "a", "b", "c", "d" ) );

		assertThat( variables.get( Key.of( "result26" ) ) ).isEqualTo( Array.of( "a", "b", "c", "d" ) );

		assertThat( variables.getAsQuery( Key.of( "result27" ) ).size() ).isEqualTo( 1 );
		assertThat( variables.getAsQuery( Key.of( "result27" ) ).getRowAsStruct( 0 ).get( "name" ) ).isEqualTo( "Brad" );

		assertThat( variables.get( Key.of( "result28" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "result29" ) ) ).isEqualTo( true );

		assertThat( variables.getAsQuery( Key.of( "result30" ) ).size() ).isEqualTo( 3 );
		assertThat( variables.getAsQuery( Key.of( "result30" ) ).getRowAsStruct( 0 ).get( "name" ) ).isEqualTo( "LUIS" );
		assertThat( variables.getAsQuery( Key.of( "result30" ) ).getRowAsStruct( 1 ).get( "name" ) ).isEqualTo( "JON" );
		assertThat( variables.getAsQuery( Key.of( "result30" ) ).getRowAsStruct( 2 ).get( "name" ) ).isEqualTo( "BRAD" );

		assertThat( variables.getAsArray( Key.of( "result31" ) ).size() ).isEqualTo( 3 );
		assertThat( variables.getAsArray( Key.of( "result31" ) ).get( 0 ) ).isEqualTo( "Brad" );
		assertThat( variables.getAsArray( Key.of( "result31" ) ).get( 1 ) ).isEqualTo( "Jon" );
		assertThat( variables.getAsArray( Key.of( "result31" ) ).get( 2 ) ).isEqualTo( "Luis" );

		assertThat( variables.getAsArray( Key.of( "result32" ) ).size() ).isEqualTo( 3 );
		assertThat( variables.getAsArray( Key.of( "result32" ) ).get( 0 ) ).isEqualTo( "Luis" );
		assertThat( variables.getAsArray( Key.of( "result32" ) ).get( 1 ) ).isEqualTo( "Jon" );
		assertThat( variables.getAsArray( Key.of( "result32" ) ).get( 2 ) ).isEqualTo( "Brad" );

	}

}
