
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import ortus.boxlang.runtime.types.IStruct;

public class XMLValidateTest {

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

	@DisplayName( "It tests the BIF XMLValidate Success" )
	@Test
	public void testBif() {
		instance.executeSource(
		    """
		       xmlContent = '<?xml version="1.0" encoding="UTF-8"?>
		       <order xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="http://localhost:8500/something.xsd" id="4323251" >
		       <customer firstname="Philip" lastname="Cramer" accountNum="21"/>
		       <items>
		       <item id="43">
		       <name> Deluxe Carpenter&apos;s Hammer</name>
		       <quantity>1</quantity>
		       <unitprice>15.95</unitprice>
		       </item>
		       <item id="54">
		       <name> 36&quot; Plastic Rake</name>
		       <quantity>2</quantity>
		       <unitprice>6.95</unitprice>
		       </item>
		       <item id="68">
		       <name> Standard paint thinner</name>
		       <quantity>3</quantity>
		       <unitprice>8.95</unitprice>
		       </item>
		       </items>
		       </order>
		       ';

		       dtd = '<?xml version="1.0" encoding="UTF-8"?>
		    <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
		    elementFormDefault="qualified">
		    <xs:element name="customer">
		    <xs:complexType>
		    <xs:attribute name="firstname" type="xs:string" use="required"/>
		    <xs:attribute name="lastname" type="xs:string" use="required"/>
		    <xs:attribute name="accountNum" type="xs:string" use="required"/>
		    </xs:complexType>
		    </xs:element>
		    <xs:element name="name" type="xs:string"/>
		    <xs:element name="quantity" type="xs:string"/>
		    <xs:element name="unitprice" type="xs:string"/>
		    <xs:element name="item">
		    <xs:complexType>
		    <xs:sequence>
		    <xs:element ref="name"/>
		    <xs:element ref="quantity"/>
		    <xs:element ref="unitprice"/>
		    </xs:sequence>
		    <xs:attribute name="id" type="xs:integer" use="required">
		    </xs:attribute>
		    </xs:complexType>
		    </xs:element>
		    <xs:element name="items">
		    <xs:complexType>
		    <xs:sequence>
		    <xs:element ref="item" maxOccurs="unbounded"/>
		    </xs:sequence>
		    </xs:complexType>
		    </xs:element>
		    <xs:element name="order">
		    <xs:complexType>
		    <xs:sequence>
		    <xs:element ref="customer"/>
		    <xs:element ref="items"/>
		    </xs:sequence>
		    <xs:attribute name="id" type="xs:string" use="required"/>
		    </xs:complexType>
		    </xs:element>
		    </xs:schema>
		       '

		       result = XMLValidate( xmlContent, dtd );
		       """,
		    context );

		assertTrue( variables.get( result ) instanceof IStruct );
		IStruct response = variables.getAsStruct( result );
		assertTrue( response.containsKey( Key.status ) );
		assertTrue( response.containsKey( Key.warning ) );
		assertTrue( response.containsKey( Key.errors ) );
		assertTrue( response.containsKey( Key.fatalErrors ) );
		assertTrue( response.getAsBoolean( Key.status ) );

	}

	@DisplayName( "It tests the BIF XMLValidate Error" )
	@Test
	public void testBifError() {
		instance.executeSource(
		    """
		       xmlContent = '<?xml version="1.0" encoding="UTF-8"?>
		       <order xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="http://localhost:8500/something.xsd" id="4323251" >
		       <customer firstname="Philip" lastname="Cramer" accountNum="21"/>
		       <items>
		       <item id="43">
		       <name> Deluxe Carpenter&apos;s Hammer</name>
		       <quantity>1</quantity>
		       <unitprice>15.95</unitprice>
		       </item>
		       <item id="54">
		       <name> 36&quot; Plastic Rake</name>
		       <quantity>2</quantity>
		       <unitprice>6.95</unitprice>
		       </item>
		       <item id="68">
		       <name> Standard paint thinner</name>
		       <quantity>3</quantity>
		       <unitprice>8.95</unitprice>
		       </item>
		       </items>
		       </order>
		       ';

		    // Invalid DTD source
		       dtd = '<?xml version="1.0" encoding="UTF-8"?>
		    <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
		    elementFormDefault="qualified">
		    <xs:element name="customer">
		    <xs:complexType>
		    <xs:attribute name="firstname" type="xs:string" use="required"/>
		    <xs:attribute name="lastname" type="xs:string" use="required"/>
		    <xs:attribute name="accountNum" type="xs:string" use="required"/>
		    </xs:complexType>
		    </xs:element>
		    <xs:element name="name" type="xs:string"/>
		    <xs:element name="quantity" type="xs:string"/>
		    <xs:element name="unitprice" type="xs:string"/>
		    <xs:element name="item">
		    <xs:complexType>
		    <xs:sequence>
		    <xs:element ref="name"/>
		    <xs:element ref="quantity"/>
		    <xs:element ref="unitprice"/>
		    </xs:sequence>
		    <xs:attribute name="id" type="xs:integer" use="required">
		    </xs:attribute>
		    </xs:complexType>
		    </xs:element>
		    <xs:element name="items">
		    <xs:complexType>
		    <xs:sequence>
		    <xs:element ref="item" maxOccurs="unbounded"/>
		    </xs:sequence>
		    </xs:complexType>
		    </xs:element>
		    <xs:element name="order">
		       '

		       result = XMLValidate( xmlContent, dtd );
		       """,
		    context );

		assertTrue( variables.get( result ) instanceof IStruct );
		System.out.println( variables.getAsStruct( result ) );
		IStruct response = variables.getAsStruct( result );
		assertTrue( response.containsKey( Key.status ) );
		assertTrue( response.containsKey( Key.warning ) );
		assertTrue( response.containsKey( Key.errors ) );
		assertTrue( response.containsKey( Key.fatalErrors ) );
		assertEquals( response.getAsArray( Key.fatalErrors ).size(), 1 );
		assertFalse( response.getAsBoolean( Key.status ) );

	}

}
