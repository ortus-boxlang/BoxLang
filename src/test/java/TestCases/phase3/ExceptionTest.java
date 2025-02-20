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
package TestCases.phase3;

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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class ExceptionTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );
	static Key			foo		= new Key( "foo" );

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
	public void testBoxMeta() {

		// TODO: I'm not sure what the point of this test is. It seems I got distracted before adding the actual assertions?
		instance.executeStatement(
		    """
		    include "src/test/java/TestCases/phase3/ExceptionThrower.cfs";
		       """, context );

		assertThat( "" ).isEqualTo( "" );

	}

	@Test
	public void testCatchInnerType() {

		instance.executeStatement(
		    """
		    try{
		    	try {
		    	throw( type="inner", message="inner" )
		    	} catch( e ) {
		    		throw ( type="outer", message="outer", object=e )
		    	}
		    } catch( inner e ) {
		    	result = e
		    }
		          """, context );

		Throwable t = ( Throwable ) variables.get( result );
		assertThat( t.getMessage() ).isEqualTo( "outer" );
		assertThat( t.getCause().getMessage() ).isEqualTo( "inner" );

	}

	@Test
	public void detailAndExtendedInfoStrings() {

		// @formatter:off
		instance.executeStatement(
		"""
			try{
				a = b;
			} catch( any e ) {
				result = e;
			}
			assert structKeyExists( result, "detail" );
			assert structKeyExists( result, "extendedInfo" );
		""", context );
		// @formatter:on
		BoxRuntimeException t = ( BoxRuntimeException ) variables.get( result );
		assertThat( t.getDetail() ).isEqualTo( "" );
		assertThat( t.getExtendedInfo() ).isEqualTo( "" );

	}

}
