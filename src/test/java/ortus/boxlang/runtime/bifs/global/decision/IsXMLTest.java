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

package ortus.boxlang.runtime.bifs.global.decision;

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

public class IsXMLTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		variables.clear();
	}

	@DisplayName( "It works on valid XML" )
	@Test
	public void testValidXML() {
		instance.executeSource(
		    """
		    result = isXML( "<root />" )
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "It works on more valid XML" )
	@Test
	public void testMoreValidXML() {
		instance.executeSource(
		    """
		       result = isXML( '<?xml version="1.0"?>
		    <xsl:stylesheet version="1.0"
		    	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
		    	<xsl:output method="html" doctype-public="-//W3C//DTD HTML 4.0 Transitional//EN" />
		    	<xsl:template match="/">
		    		<html>
		    			<body>
		    				<table border="2" bgcolor="yellow">
		    					<tr>
		    						<th>Name</th>
		    						<th>Price</th>
		    					</tr>
		    					<xsl:for-each select="breakfast_menu/food">
		    						<tr>
		    							<td>
		    								<xsl:value-of select="name"/>
		    							</td>
		    							<td>
		    								<xsl:value-of select="price"/>
		    							</td>
		    						</tr>
		    					</xsl:for-each>
		    				</table>
		    			</body>
		    		</html>
		    	</xsl:template>
		    </xsl:stylesheet>' )
		       """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "It rejects invalid XML" )
	@Test
	public void testRejectInvalid() {
		instance.executeSource(
		    """
		    result = isXML( null )
		    result2 = isXML( '' )
		    result3 = isXML( 56 )
		    result4 = isXML( {} )
		    result5 = isXML( [] )
		    result6 = isXML( '<foo>' )
		    result7 = isXML( 'This is a test of the emergency broadcast system.' )
		      """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "result6" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "result7" ) ) ).isEqualTo( false );
	}

}
