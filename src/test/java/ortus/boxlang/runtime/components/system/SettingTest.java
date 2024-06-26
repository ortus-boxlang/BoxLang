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

package ortus.boxlang.runtime.components.system;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class SettingTest {

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
	public void testCfoutputQueryAsString() {

		instance.executeSource(
		    """
		    <cfsavecontent variable="result">
		    	before
		    	<cfsetting enableoutputonly="true">
		    	you should not see me
		    	<cfoutput>
		    		but you should see me
		    	</cfoutput>
		    	but not me
		    	<cfsetting enableoutputonly="false">
		    	I'm back!
		    </cfsavecontent>
		              """, context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.getAsString( result ).replaceAll( "\\s", "" ) ).isEqualTo( "beforebutyoushouldseemeI'mback!" );

	}

	@Test
	public void testBLOutputQueryAsString() {

		instance.executeSource(
		    """
		    <bx:savecontent variable="result">
		    	before
		    	<bx:setting enableoutputonly="true">
		    	you should not see me
		    	<bx:output>
		    		but you should see me
		    	</bx:output>
		    	but not me
		    	<bx:setting enableoutputonly="false">
		    	I'm back!
		    </bx:savecontent>
		    		  """, context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.getAsString( result ).replaceAll( "\\s", "" ) ).isEqualTo( "beforebutyoushouldseemeI'mback!" );

	}

}
