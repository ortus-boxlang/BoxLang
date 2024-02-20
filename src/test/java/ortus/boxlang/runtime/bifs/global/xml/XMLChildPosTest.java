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

package ortus.boxlang.runtime.bifs.global.xml;

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

public class XMLChildPosTest {

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

	@DisplayName( "It can get find a child" )
	@Test
	public void testCanFileAChild() {
		instance.executeSource(
		    """
		          xml = XMLParse( '<employee>
		    <!-- A list of employees -->
		    <name EmpType="Regular">
		    <first>Almanzo</first>
		    <last>Wilder</last>
		    <Status>Medical Absence</Status>
		    <Status>Extended Leave</Status>
		    </name>
		    <name EmpType="Contract">
		    <first>Laura</first>
		    <last>Ingalls</last>
		    </name>
		    </employee>' );


		    result = XMLChildPos(xml.employee.name[1], "Status",2);
		    result2 = XMLChildPos(xml.employee.name[1], "Status",20);
		          """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 4 );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( -1 );

	}

	@DisplayName( "It can get find a child member" )
	@Test
	public void testCanFileAChildMember() {
		instance.executeSource(
		    """
		          xml = XMLParse( '<employee>
		    <!-- A list of employees -->
		    <name EmpType="Regular">
		    <first>Almanzo</first>
		    <last>Wilder</last>
		    <Status>Medical Absence</Status>
		    <Status>Extended Leave</Status>
		    </name>
		    <name EmpType="Contract">
		    <first>Laura</first>
		    <last>Ingalls</last>
		    </name>
		    </employee>' );


		    result = xml.employee.name[1].childPos( "Status",2);
		    result2 = xml.employee.name[1].childPos( "Status",20);
		          """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 4 );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( -1 );

	}

}
