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

public class XMLTransformTest {

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

	@DisplayName( "It can transform" )
	@Test
	public void testCanTransform() {
		instance.executeSource(
		    """
		          xml = XMLParse( '<?xml version="1.0"?>
		    <breakfast_menu>
		     <food>
		      <name>Eggs Benedict</name>
		      <price>$8.95</price>
		     </food>
		     <food>
		      <name>Pancakes</name>
		      <price>$7.95</price>
		     </food>
		     <food>
		      <name>French Toast</name>
		      <price>$6.95</price>
		     </food>
		    </breakfast_menu>' );

		    xslt = '<?xml version="1.0"?>
		       <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
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
		       </xsl:stylesheet>';

		       result = XmlTransform(xml, xslt);
		          """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( String.class );
		String	transformed	= variables.getAsString( result );
		String	expected	= """
		                      <!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN"> <html> <body> <table border="2" bgcolor="yellow"> <tr> <th>Name</th><th>Price</th> </tr> <tr> <td>Eggs Benedict</td><td>$8.95</td> </tr> <tr> <td>Pancakes</td><td>$7.95</td> </tr> <tr> <td>French Toast</td><td>$6.95</td> </tr> </table> </body> </html>
		                                             """;
		assertThat( transformed.replaceAll( "\\s", "" ) ).isEqualTo( expected.replaceAll( "\\s", "" ) );

	}

	@DisplayName( "It can transform member" )
	@Test
	public void testCanTransformMember() {
		instance.executeSource(
		    """
		          xml = XMLParse( '<?xml version="1.0"?>
		    <breakfast_menu>
		     <food>
		      <name>Eggs Benedict</name>
		      <price>$8.95</price>
		     </food>
		     <food>
		      <name>Pancakes</name>
		      <price>$7.95</price>
		     </food>
		     <food>
		      <name>French Toast</name>
		      <price>$6.95</price>
		     </food>
		    </breakfast_menu>' );

		    xslt = '<?xml version="1.0"?>
		       <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
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
		       </xsl:stylesheet>';

		       result = xml.transform( xslt);
		          """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( String.class );
		String	transformed	= variables.getAsString( result );
		String	expected	= """
		                      <!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN"> <html> <body> <table border="2" bgcolor="yellow"> <tr> <th>Name</th><th>Price</th> </tr> <tr> <td>Eggs Benedict</td><td>$8.95</td> </tr> <tr> <td>Pancakes</td><td>$7.95</td> </tr> <tr> <td>French Toast</td><td>$6.95</td> </tr> </table> </body> </html>
		                                             """;
		assertThat( transformed.replaceAll( "\\s", "" ) ).isEqualTo( expected.replaceAll( "\\s", "" ) );

	}

	@DisplayName( "It can transform from file" )
	@Test
	public void testCanTransformFromFile() {
		instance.executeSource(
		    """
		    import java.io.File;
		              xml = XMLParse( '<?xml version="1.0"?>
		        <breakfast_menu>
		         <food>
		          <name>Eggs Benedict</name>
		          <price>$8.95</price>
		         </food>
		         <food>
		          <name>Pancakes</name>
		          <price>$7.95</price>
		         </food>
		         <food>
		          <name>French Toast</name>
		          <price>$6.95</price>
		         </food>
		        </breakfast_menu>' );

		        xslt = new File( "src/test/java/ortus/boxlang/runtime/bifs/global/xml/transformer.xsl" ).getAbsolutePath();

		           result = XmlTransform(xml, xslt);
		              """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( String.class );
		String	transformed	= variables.getAsString( result );
		String	expected	= """
		                      <!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN"> <html> <body> <table border="2" bgcolor="yellow"> <tr> <th>Name</th><th>Price</th> </tr> <tr> <td>Eggs Benedict</td><td>$8.95</td> </tr> <tr> <td>Pancakes</td><td>$7.95</td> </tr> <tr> <td>French Toast</td><td>$6.95</td> </tr> </table> </body> </html>
		                                             """;
		assertThat( transformed.replaceAll( "\\s", "" ) ).isEqualTo( expected.replaceAll( "\\s", "" ) );

	}

	@DisplayName( "It can transform from file member" )
	@Test
	public void testCanTransformFromFileMember() {
		instance.executeSource(
		    """
		    import java.io.File;
		              xml = XMLParse( '<?xml version="1.0"?>
		        <breakfast_menu>
		         <food>
		          <name>Eggs Benedict</name>
		          <price>$8.95</price>
		         </food>
		         <food>
		          <name>Pancakes</name>
		          <price>$7.95</price>
		         </food>
		         <food>
		          <name>French Toast</name>
		          <price>$6.95</price>
		         </food>
		        </breakfast_menu>' );

		        xslt = new File( "src/test/java/ortus/boxlang/runtime/bifs/global/xml/transformer.xsl" ).getAbsolutePath();

		           result = xml.transform(xslt);
		              """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( String.class );
		String	transformed	= variables.getAsString( result );
		String	expected	= """
		                      <!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN"> <html> <body> <table border="2" bgcolor="yellow"> <tr> <th>Name</th><th>Price</th> </tr> <tr> <td>Eggs Benedict</td><td>$8.95</td> </tr> <tr> <td>Pancakes</td><td>$7.95</td> </tr> <tr> <td>French Toast</td><td>$6.95</td> </tr> </table> </body> </html>
		                                             """;
		assertThat( transformed.replaceAll( "\\s", "" ) ).isEqualTo( expected.replaceAll( "\\s", "" ) );

	}

}
