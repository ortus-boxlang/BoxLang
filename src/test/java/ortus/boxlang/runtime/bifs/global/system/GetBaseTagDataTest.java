/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.system;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;

public class GetBaseTagDataTest {

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

	@DisplayName( "It gets tag data" )
	@Test
	public void testGetTagData() {
		instance.getConfiguration().customTagsDirectory.add( "src/test/java/ortus/boxlang/runtime/bifs/global/system" );
		instance.executeSource(
		    """
		    <bx:set thisIsCaller = true>
		    <bx:module template="/src/test/java/ortus/boxlang/runtime/bifs/global/system/TestCustomTag.bxm" level="outer">
		    	<bx:module name="TestCustomTag" level="middle">
		    		<bx:_TestCustomTag level="inner">
		    			<bx:loop times="1">
		    				<bx:savecontent variable="whatever">
		    					My "output" here
		    					<bx:output>
		    						<!--- duplicate so I get a copy of the data at this point since it will otherwise
		    						be passed by reference and changeby the time I run my assertions below --->
		    						<bx:set result = duplicate( GetBaseTagData( "bx_TestCustomTag" ) )>
		    						<bx:set result2 = duplicate( GetBaseTagData( "bx_TestCustomTag", 3 ) )>
		    					</bx:output>
		    				</bx:savecontent>
		    			</bx:loop>
		    		</bx:_TestCustomTag>
		    	</bx:module>
		    </bx:module>
		                           	    	       """,
		    context, BoxSourceType.BOXTEMPLATE );

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );

		IStruct resultStruct = variables.getAsStruct( result );

		assertThat( resultStruct.containsKey( Key.caller ) ).isTrue();
		assertThat( resultStruct.getAsStruct( Key.caller ).containsKey( Key.of( "thisIsCaller" ) ) ).isTrue();

		assertThat( resultStruct.containsKey( Key.thisTag ) ).isTrue();
		assertThat( resultStruct.getAsStruct( Key.thisTag ).containsKey( Key.generatedContent ) ).isTrue();
		assertThat( resultStruct.getAsStruct( Key.thisTag ).containsKey( Key.hasEndTag ) ).isTrue();
		assertThat( resultStruct.getAsStruct( Key.thisTag ).containsKey( Key.executionMode ) ).isTrue();
		assertThat( resultStruct.getAsStruct( Key.thisTag ).get( Key.executionMode ) ).isEqualTo( "inactive" );

		assertThat( resultStruct.containsKey( Key.attributes ) ).isTrue();
		assertThat( resultStruct.getAsStruct( Key.attributes ).containsKey( Key.of( "level" ) ) ).isTrue();
		assertThat( resultStruct.getAsStruct( Key.attributes ).get( Key.of( "level" ) ) ).isEqualTo( "inner" );

		assertThat( variables.get( Key.of( "result2" ) ) ).isInstanceOf( IStruct.class );

		IStruct result2Struct = variables.getAsStruct( Key.of( "result2" ) );
		assertThat( result2Struct.containsKey( Key.attributes ) ).isTrue();
		assertThat( result2Struct.getAsStruct( Key.attributes ).containsKey( Key.of( "level" ) ) ).isTrue();
		assertThat( result2Struct.getAsStruct( Key.attributes ).get( Key.of( "level" ) ) ).isEqualTo( "outer" );

	}
}
