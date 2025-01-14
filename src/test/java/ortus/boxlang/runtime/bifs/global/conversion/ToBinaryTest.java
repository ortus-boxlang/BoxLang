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

package ortus.boxlang.runtime.bifs.global.conversion;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class ToBinaryTest {

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

	@Test
	public void testCanConvertBytes() {
		instance.executeSource(
		    """
		    result = toBinary( "Hello World".getBytes() )
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Hello World".getBytes() );
	}

	@Test
	public void testCanConvertString() {
		instance.executeSource(
		    """
		    result = toBinary( "SGVsbG8gV29ybGQ=" )
		    """,
		    context );
		assertThat( new String( ( byte[] ) variables.get( result ) ) ).isEqualTo( "Hello World" );
	}

	@Test
	public void testIt() {
		// @formatter:off
		instance.executeSource(
		    """
				result = toBinary( "rO0ABXNyACJvcnR1cy5ib3hsYW5nLnJ1bnRpbWUudHlwZXMuU3RydWN0AAAAAAAAAAECAARMAAMkYnh0ACpMb3J0dXMvYm94bGFuZy9ydW50aW1lL3R5cGVzL21ldGEvQm94TWV0YTtMAAlsaXN0ZW5lcnN0AA9MamF2YS91dGlsL01hcDtMAAR0eXBldAArTG9ydHVzL2JveGxhbmcvcnVudGltZS90eXBlcy9JU3RydWN0JFRZUEVTO0wAB3dyYXBwZWRxAH4AAnhwcHB+cgApb3J0dXMuYm94bGFuZy5ydW50aW1lLnR5cGVzLklTdHJ1Y3QkVFlQRVMAAAAAAAAAABIAAHhyAA5qYXZhLmxhbmcuRW51bQAAAAAAAAAAEgAAeHB0AAdERUZBVUxUc3IAJmphdmEudXRpbC5jb25jdXJyZW50LkNvbmN1cnJlbnRIYXNoTWFwZJneEp2HKT0DAANJAAtzZWdtZW50TWFza0kADHNlZ21lbnRTaGlmdFsACHNlZ21lbnRzdAAxW0xqYXZhL3V0aWwvY29uY3VycmVudC9Db25jdXJyZW50SGFzaE1hcCRTZWdtZW50O3hwAAAADwAAABx1cgAxW0xqYXZhLnV0aWwuY29uY3VycmVudC5Db25jdXJyZW50SGFzaE1hcCRTZWdtZW50O1J3P0Eymzl0AgAAeHAAAAAQc3IALmphdmEudXRpbC5jb25jdXJyZW50LkNvbmN1cnJlbnRIYXNoTWFwJFNlZ21lbnQfNkyQWJMpPQIAAUYACmxvYWRGYWN0b3J4cgAoamF2YS51dGlsLmNvbmN1cnJlbnQubG9ja3MuUmVlbnRyYW50TG9ja2ZVqCwsyGrrAgABTAAEc3luY3QAL0xqYXZhL3V0aWwvY29uY3VycmVudC9sb2Nrcy9SZWVudHJhbnRMb2NrJFN5bmM7eHBzcgA0amF2YS51dGlsLmNvbmN1cnJlbnQubG9ja3MuUmVlbnRyYW50TG9jayROb25mYWlyU3luY2WIMudTe78LAgAAeHIALWphdmEudXRpbC5jb25jdXJyZW50LmxvY2tzLlJlZW50cmFudExvY2skU3luY7geopSqRFp8AgAAeHIANWphdmEudXRpbC5jb25jdXJyZW50LmxvY2tzLkFic3RyYWN0UXVldWVkU3luY2hyb25pemVy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cHB4" )
		    	writedump( result )
	    	""",
	    context );
		// @formatter:on
	}

}
