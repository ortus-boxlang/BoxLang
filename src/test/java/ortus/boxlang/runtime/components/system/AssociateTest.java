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
import org.junit.jupiter.api.AfterEach;
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
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Struct;

public class AssociateTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );
	StringBuffer		buffer;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
		instance.getConfiguration().runtime.customTagsDirectory.add( "src/test/java/ortus/boxlang/runtime/components/system" );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
		buffer		= new StringBuffer();
		context.pushBuffer( buffer );
	}

	@AfterEach
	public void teardownEach() {
		context.popBuffer();
	}

	@Test
	public void testCanAssociate() {

		instance.executeSource(
		    """
		    <cfmodule name="OuterTag">
		    	<cfmodule name="InnerTag" brad="wood" luis="majano" />
		    	<cfmodule name="InnerTag" gavin="pickin" jorge="reyes" />
		    </cfmodule>
		    """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.get( result ) ).isInstanceOf( Array.class );
		Array assocttribs = variables.getAsArray( result );
		assertThat( assocttribs.size() ).isEqualTo( 2 );
		assertThat( ( ( Struct ) assocttribs.get( 0 ) ).getAsString( Key.of( "brad" ) ) ).isEqualTo( "wood" );
		assertThat( ( ( Struct ) assocttribs.get( 0 ) ).getAsString( Key.of( "luis" ) ) ).isEqualTo( "majano" );
		assertThat( ( ( Struct ) assocttribs.get( 1 ) ).getAsString( Key.of( "gavin" ) ) ).isEqualTo( "pickin" );
		assertThat( ( ( Struct ) assocttribs.get( 1 ) ).getAsString( Key.of( "jorge" ) ) ).isEqualTo( "reyes" );

	}

	@Test
	public void testCanAssociateCustom() {

		instance.executeSource(
		    """
		    <cf_OuterTag2>
		    	<cf_InnerTag2 brad="wood" luis="majano" />
		    	<cf_InnerTag2 gavin="pickin" jorge="reyes" />
		    </cf_OuterTag2>
		    """,
		    context, BoxSourceType.CFTEMPLATE );

		assertThat( variables.get( result ) ).isInstanceOf( Array.class );
		Array assocttribs = variables.getAsArray( result );
		assertThat( assocttribs.size() ).isEqualTo( 2 );
		assertThat( ( ( Struct ) assocttribs.get( 0 ) ).getAsString( Key.of( "brad" ) ) ).isEqualTo( "wood" );
		assertThat( ( ( Struct ) assocttribs.get( 0 ) ).getAsString( Key.of( "luis" ) ) ).isEqualTo( "majano" );
		assertThat( ( ( Struct ) assocttribs.get( 1 ) ).getAsString( Key.of( "gavin" ) ) ).isEqualTo( "pickin" );
		assertThat( ( ( Struct ) assocttribs.get( 1 ) ).getAsString( Key.of( "jorge" ) ) ).isEqualTo( "reyes" );

	}

	@Test
	public void testComponentsInRightOrder() {

		instance.executeSource(
		    """
		    <cf_CustomTagA>
		    	<cf_CustomTagB>
		    		<cf_CustomTagC>
		    			<cf_CustomTagD>
		    				<cfset result = getBoxContext().getComponents()>
		    			</cf_CustomTagD>
		    		</cf_CustomTagC>
		    	</cf_CustomTagB>
		    </cf_CustomTagA>
		                """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.get( result ) ).isInstanceOf( Object[].class );
		Object[] components = ( Object[] ) variables.get( result );
		assertThat( components.length ).isEqualTo( 4 );
		assertThat( components[ 0 ] ).isInstanceOf( Struct.class );
		assertThat( ( ( Struct ) components[ 0 ] ).getAsKey( Key.of( "customTagName" ) ).getName() ).isEqualTo( "CustomTagD" );
		assertThat( components[ 1 ] ).isInstanceOf( Struct.class );
		assertThat( ( ( Struct ) components[ 1 ] ).getAsKey( Key.of( "customTagName" ) ).getName() ).isEqualTo( "CustomTagC" );
		assertThat( components[ 2 ] ).isInstanceOf( Struct.class );
		assertThat( ( ( Struct ) components[ 2 ] ).getAsKey( Key.of( "customTagName" ) ).getName() ).isEqualTo( "CustomTagB" );
		assertThat( components[ 3 ] ).isInstanceOf( Struct.class );
		assertThat( ( ( Struct ) components[ 3 ] ).getAsKey( Key.of( "customTagName" ) ).getName() ).isEqualTo( "CustomTagA" );

	}

	@Test
	public void testComponentsInRightOrderAcrossContexts() {

		instance.executeSource(
		    """
		    <cffunction name="foo">
		       		<cf_CustomTagC>
		       			<cf_CustomTagD>
		       				<cfset result = getBoxContext().getComponents()>
		       			</cf_CustomTagD>
		       		</cf_CustomTagC>
		    </cffunction>

		       <cf_CustomTagA>
		       	<cf_CustomTagB>
		    		<cfset foo()>
		       	</cf_CustomTagB>
		       </cf_CustomTagA>
		                   """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.get( result ) ).isInstanceOf( Object[].class );
		Object[] components = ( Object[] ) variables.get( result );
		assertThat( components.length ).isEqualTo( 4 );
		assertThat( components[ 0 ] ).isInstanceOf( Struct.class );
		assertThat( ( ( Struct ) components[ 0 ] ).getAsKey( Key.of( "customTagName" ) ).getName() ).isEqualTo( "CustomTagD" );
		assertThat( components[ 1 ] ).isInstanceOf( Struct.class );
		assertThat( ( ( Struct ) components[ 1 ] ).getAsKey( Key.of( "customTagName" ) ).getName() ).isEqualTo( "CustomTagC" );
		assertThat( components[ 2 ] ).isInstanceOf( Struct.class );
		assertThat( ( ( Struct ) components[ 2 ] ).getAsKey( Key.of( "customTagName" ) ).getName() ).isEqualTo( "CustomTagB" );
		assertThat( components[ 3 ] ).isInstanceOf( Struct.class );
		assertThat( ( ( Struct ) components[ 3 ] ).getAsKey( Key.of( "customTagName" ) ).getName() ).isEqualTo( "CustomTagA" );

	}

}
