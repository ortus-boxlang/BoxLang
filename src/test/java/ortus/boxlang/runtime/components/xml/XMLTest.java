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

package ortus.boxlang.runtime.components.xml;

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

public class XMLTest {

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

	@DisplayName( "It can create XML" )
	@Test
	public void testCanCreateXML() {

		instance.executeSource(
		    """
		    <bx:xml variable="result">
		    	<root>
		    		<foo attr="brad" />
		    		<foo attr="luis" />
		    		<foo attr="jon" />
		     	</root>
		    </bx:xml>
		            """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.get( result ) ).isInstanceOf( ortus.boxlang.runtime.types.XML.class );
		ortus.boxlang.runtime.types.XML xml = variables.getAsXML( result );
		assertThat( xml.toString() ).contains( "<root>" );
		assertThat( xml.toString() ).contains( "<foo attr=\"brad\"/>" );
		assertThat( xml.toString() ).contains( "<foo attr=\"luis\"/>" );
		assertThat( xml.toString() ).contains( "<foo attr=\"jon\"/>" );
		assertThat( xml.toString() ).contains( "</root>" );
	}

	@DisplayName( "It can create XML CF" )
	@Test
	public void testCanCreateXMLCF() {
		instance.executeSource(
		    """
		    <cfxml variable="result">
		    	<root>
		    		<foo attr="brad" />
		    		<foo attr="luis" />
		    		<foo attr="jon" />
		    	</root>
		    </cfxml>
		    		""",
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( variables.get( result ) ).isInstanceOf( ortus.boxlang.runtime.types.XML.class );
		ortus.boxlang.runtime.types.XML xml = variables.getAsXML( result );
		assertThat( xml.toString() ).contains( "<root>" );
		assertThat( xml.toString() ).contains( "<foo attr=\"brad\"/>" );
		assertThat( xml.toString() ).contains( "<foo attr=\"luis\"/>" );
		assertThat( xml.toString() ).contains( "<foo attr=\"jon\"/>" );
		assertThat( xml.toString() ).contains( "</root>" );
	}

	@DisplayName( "It can create XML CF script" )
	@Test
	public void testCanCreateXMLCFScript() {
		instance.executeSource(
		    """
		    cfxml( variable="result" ){
		    	echo( '<root>
		    		<foo attr="brad" />
		    		<foo attr="luis" />
		    		<foo attr="jon" />
		    	</root>' );
		    }
		    		""",
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isInstanceOf( ortus.boxlang.runtime.types.XML.class );
		ortus.boxlang.runtime.types.XML xml = variables.getAsXML( result );
		assertThat( xml.toString() ).contains( "<root>" );
		assertThat( xml.toString() ).contains( "<foo attr=\"brad\"/>" );
		assertThat( xml.toString() ).contains( "<foo attr=\"luis\"/>" );
		assertThat( xml.toString() ).contains( "<foo attr=\"jon\"/>" );
		assertThat( xml.toString() ).contains( "</root>" );
	}

	@DisplayName( "It can create XML BL script" )
	@Test
	public void testCanCreateXMLBLScript() {
		instance.executeSource(
		    """
		    xml variable="result" {
		    	echo( '<root>
		    		<foo attr="brad" />
		    		<foo attr="luis" />
		    		<foo attr="jon" />
		    	</root>' );
		    }
		    		""",
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isInstanceOf( ortus.boxlang.runtime.types.XML.class );
		ortus.boxlang.runtime.types.XML xml = variables.getAsXML( result );
		assertThat( xml.toString() ).contains( "<root>" );
		assertThat( xml.toString() ).contains( "<foo attr=\"brad\"/>" );
		assertThat( xml.toString() ).contains( "<foo attr=\"luis\"/>" );
		assertThat( xml.toString() ).contains( "<foo attr=\"jon\"/>" );
		assertThat( xml.toString() ).contains( "</root>" );
	}

}
