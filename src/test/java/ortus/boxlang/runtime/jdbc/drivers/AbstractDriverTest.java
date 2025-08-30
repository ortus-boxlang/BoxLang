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
package ortus.boxlang.runtime.jdbc.drivers;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.bifs.global.jdbc.BaseJDBCTest;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

public abstract class AbstractDriverTest extends BaseJDBCTest {

	static Key result = new Key( "result" );

	/**
	 * Subclasses must implement to provide driver-specific datasource name
	 */
	abstract String getDatasourceName();

	@DisplayName( "It sets generatedKey in query meta" )
	@Test
	public void testGeneratedKey() {
		instance.executeStatement(
		    String.format( """
		                                          queryExecute( "
		                                          	INSERT INTO generatedKeyTest (name) VALUES ( 'Michael' ), ( 'Michael2');
		                                          	INSERT INTO generatedKeyTest (name) VALUES ( 'Brad' ), ( 'Brad2' );
		                                          	INSERT INTO generatedKeyTest (name) VALUES ( 'Luis' );
		                                          	INSERT INTO generatedKeyTest (name) VALUES ( 'Jon' ), ( 'Jon2' ), ( 'Jon3' );
		                   ",
		                                          	{},
		                                          	{ "result": "variables.result", "datasource" : "%s" }
		                                          );
		                                                         """, getDatasourceName() ),
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		IStruct meta = variables.getAsStruct( result );

		assertThat( DoubleCaster.cast( meta.get( Key.generatedKey ), false ) ).isEqualTo( 1.0d );

		Array generatedKeys = meta.getAsArray( Key.generatedKeys );

		assertThat( generatedKeys ).hasSize( 4 );
		// These keys are coming back as BigDecimal, so let's massage them into an array of ints for easier comparision
		Integer[] firstKeys = ( ( Array ) generatedKeys.get( 0 ) ).stream().map( IntegerCaster::cast ).toArray( Integer[]::new );
		assertThat( firstKeys ).isEqualTo( new Integer[] { 1, 2 } );

		Integer[] secondKeys = ( ( Array ) generatedKeys.get( 1 ) ).stream().map( IntegerCaster::cast ).toArray( Integer[]::new );
		assertThat( secondKeys ).isEqualTo( new Integer[] { 3, 4 } );

		Integer[] thirdKeys = ( ( Array ) generatedKeys.get( 2 ) ).stream().map( IntegerCaster::cast ).toArray( Integer[]::new );
		assertThat( thirdKeys ).isEqualTo( new Integer[] { 5 } );

		Integer[] fourthKeys = ( ( Array ) generatedKeys.get( 3 ) ).stream().map( IntegerCaster::cast ).toArray( Integer[]::new );
		assertThat( fourthKeys ).isEqualTo( new Integer[] { 6, 7, 8 } );

		assertThat( meta.get( "updateCount" ) ).isEqualTo( 8 );
		Array updateCounts = meta.getAsArray( Key.of( "updateCounts" ) );
		assertThat( updateCounts.toArray() ).isEqualTo( new Integer[] { 2, 2, 1, 3 } );
	}

}