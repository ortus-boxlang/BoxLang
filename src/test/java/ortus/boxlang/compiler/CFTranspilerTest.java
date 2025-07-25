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
package ortus.boxlang.compiler;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;

public class CFTranspilerTest {

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

	private void setupQuery() {
		instance.executeSource(
		    """
		    qry = queryNew("id,type,title", "integer,varchar,varchar");
		    queryAddRow(qry,[{
		    	id: 1,
		    	type: "book",
		    	title: "Cloud Atlas"
		    },{
		    	id: 2,
		    	type: "book",
		    	title: "Lord of The Rings"
		    },{
		    	id: 3,
		    	type: "film",
		    	title: "Men in Black"
		    }]);
		    """,
		    context, BoxSourceType.CFSCRIPT );
	}

	@DisplayName( "Test valueList() to queryColumnData().toList()" )
	@Test
	public void testValueListToQueryColumnDataToListDot() {
		setupQuery();
		instance.executeSource(
		    """
		    result = valueList( qry.id );
		       """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "1,2,3" );
	}

	@DisplayName( "Test valueList() to queryColumnData().toList( delimiter )" )
	@Test
	public void testValueListToQueryColumnDataToListDotDelim() {
		setupQuery();
		instance.executeSource(
		    """
		    result = valueList( qry.id, "|" );
		       """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "1|2|3" );
	}

	@DisplayName( "Test valueList() to queryColumnData().toList( delimiter ) array access" )
	@Test
	public void testValueListToQueryColumnDataToListArray() {
		setupQuery();
		instance.executeSource(
		    """
		    result = valueList( qry["id"] );
		       """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "1,2,3" );
	}

	@DisplayName( "Test QuotedvalueList() to queryColumnData().toList()" )
	@Test
	public void testQuotedValueListToQueryColumnDataToListDot() {
		setupQuery();
		instance.executeSource(
		    """
		    result = quotedValueList( qry.id );
		       """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "\"1\",\"2\",\"3\"" );
	}

	@DisplayName( "Test QuotedvalueList() to queryColumnData().toList( delimiter )" )
	@Test
	public void testQuotedValueListToQueryColumnDataToListDotDelim() {
		setupQuery();
		instance.executeSource(
		    """
		    result = quotedValueList( qry.id, "|" );
		       """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "\"1\"|\"2\"|\"3\"" );
	}

	@DisplayName( "Test QuotedvalueList() to queryColumnData().toList( delimiter ) array access" )
	@Test
	public void testQuotedValueListToQueryColumnDataToListArray() {
		setupQuery();
		instance.executeSource(
		    """
		    result = quotedValueList( qry["id"] );
		       """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( "\"1\",\"2\",\"3\"" );
	}

	@DisplayName( "Test new java()" )
	@Test
	public void testNewJava() {
		instance.executeSource(
		    """
		    clazz = new java("java.lang.String");
		    instance = clazz.init( "foo bar" );
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( Key.of( "clazz" ) ) ).isInstanceOf( DynamicObject.class );
		assertThat( ( ( DynamicObject ) variables.get( Key.of( "clazz" ) ) ).getTargetClass().getName() ).isEqualTo( "java.lang.String" );
		assertThat( ( ( DynamicObject ) variables.get( Key.of( "clazz" ) ) ).hasInstance() ).isTrue();

		assertThat( variables.get( Key.of( "instance" ) ) ).isInstanceOf( String.class );
		assertThat( variables.getAsString( Key.of( "instance" ) ) ).isEqualTo( "foo bar" );
	}

	@DisplayName( "Test new component()" )
	@Test
	public void testNewComponent() {
		instance.executeSource(
		    """
		    	clazz = new  component("src.test.java.TestCases.phase3.MyClassCF");
		    """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( Key.of( "clazz" ) ) ).isInstanceOf( IClassRunnable.class );
		assertThat( ( ( IClassRunnable ) variables.get( Key.of( "clazz" ) ) ).bxGetName().getName() ).isEqualTo( "src.test.java.TestCases.phase3.MyClassCF" );
	}

	@DisplayName( "Test BIF return value" )
	@Test
	public void testBIFReturnValue() {
		instance.executeSource(
		    """
		      	arr = []
		    result = arrayPrepend( arr, "foo" );
		      """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( Array.of( "foo" ) );
	}

	@DisplayName( "Test BIF return value CF" )
	@Test
	public void testBIFReturnValueCF() {
		instance.executeSource(
		    """
		         	arr = []
		       result = arrayPrepend( arr, "foo" );

		    ifstmt = false
		    if( arrayPrepend( arr, "foo" ) ) {
		    	ifstmt = true
		    }
		         """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "ifstmt" ) ) ).isEqualTo( true );
	}

	@DisplayName( "Test BIF return value CF named" )
	@Test
	public void testBIFReturnValueCFName() {
		instance.executeSource(
		    """
		      	arr =  []
		    result = arrayPrepend( value="foo", array=arr );

		      """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@Test
	public void testBIFReturnValueCFStructDelete() {
		instance.executeSource(
		    """
		    str = {}
		    result = structDelete( str, "foo" );
		    result2 = structDelete( str, "foo", false );
		    result3 = structDelete( str, "foo", true );


		    str.foo = "bar"
		    result4 = structDelete( str, "foo" );
		    str.foo = "bar"
		    result5 = structDelete( str, "foo", false );
		    str.foo = "bar"
		    result6 = structDelete( str, "foo", true  );
		      """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( false );

		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result6" ) ) ).isEqualTo( true );
	}

	@Test
	public void testBIFReturnValueCFStructDeleteNamed() {
		instance.executeSource(
		    """
		    str = {}
		    result = structDelete( key="foo", struct=str );
		    result2 = structDelete( indicateNotExisting=false, key="foo", struct=str );
		    result3 = structDelete( indicateNotExisting=true, key="foo", struct=str );


		    str.foo = "bar"
		    result4 = structDelete( key="foo", struct=str );
		    str.foo = "bar"
		    result5 = structDelete( indicateNotExisting=false, key="foo", struct=str );
		    str.foo = "bar"
		    result6 = structDelete( indicateNotExisting=true, key="foo", struct=str );
		      """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( false );

		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "result6" ) ) ).isEqualTo( true );
	}

	@Test
	public void testBIFReturnValueCFArrayDelete() {
		instance.executeSource(
		    """
		    arr = []
		    result = arrayDelete( arr, "foo" );

		    arr.1 = "foo"
		    result2 = arrayDelete( arr, "foo" );
		      """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( true );
	}

	@Test
	public void testBIFReturnValueCFArrayDeleteNamed() {
		instance.executeSource(
		    """
		       arr = []
		       result = arrayDelete( value="foo", array=arr );

		       arr.1 = "foo"
		       result2 = arrayDelete( value="foo", array=arr );
		    println( arr.asString() );
		         """,
		    context, BoxSourceType.CFSCRIPT );
		assertThat( variables.get( result ) ).isEqualTo( false );

	}

	@Test
	public void testUnquotedAttributeCF() {
		instance.executeSource(
		    """
		    <cfquery dbtype="query" maxrows=1>
		    	select 42
		    </cfquery>
		           """,
		    context, BoxSourceType.CFTEMPLATE );
	}

	@Test
	public void testUnquotedAttributeCF2() {
		instance.executeSource(
		    """
		    <cfquery dbtype="query" maxrows="1">
		    	select 42
		    </cfquery>
		    	""",
		    context, BoxSourceType.CFTEMPLATE );
	}

	@Test
	public void testUnquotedAttributeCF3() {
		instance.executeSource(
		    """
		    <cfquery dbtype="query" datasource=foo>
		    	select 42
		    </cfquery>
		    	""",
		    context, BoxSourceType.CFTEMPLATE );
	}

	@Test
	public void testUnquotedAttributeCF4() {
		instance.executeSource(
		    """
		    <cfquery dbtype="query" foo=>
		    	select 42
		    </cfquery>
		    	""",
		    context, BoxSourceType.CFTEMPLATE );
	}

	@Test
	public void testUnquotedAttribute() {
		instance.executeSource(
		    """
		    <bx:query dbtype="query" name="NewDate" maxrows=1>
		    	select 42
		    </bx:query>
		           """,
		    context, BoxSourceType.BOXTEMPLATE );
	}

	@Test
	public void testUnquotedAttribute2() {
		instance.executeSource(
		    """
		    <bx:query dbtype="query" maxrows="1">
		    	select 42
		    </bx:query>
		    	""",
		    context, BoxSourceType.CFTEMPLATE );
	}

	@Test
	public void testUnquotedAttribute3() {
		instance.executeSource(
		    """
		    <bx:query dbtype="query" datasource=foo>
		    	select 42
		    </bx:query>
		    	""",
		    context, BoxSourceType.CFTEMPLATE );
	}

	@Test
	public void testUnquotedAttribute4() {
		instance.executeSource(
		    """
		       <bx:query dbtype="query" foo=>
		    select 42
		       </bx:query>
		       	""",
		    context, BoxSourceType.CFTEMPLATE );
	}

	@Test
	public void testVariableRewriteInResultString() {
		instance.executeSource(
		    """
		    cfhttp( url = "https://www.google.com", result = "local.cfhttp" );
		    result = local.cfhttp;
		      	""",
		    context, BoxSourceType.CFSCRIPT );
	}

	@Test
	public void testCFSQLType() {
		ParsingResult result = instance.parse( """
		                                       <cfquery name="news">
		                                       	SELECT id,title,story
		                                       	FROM news
		                                       	WHERE id = <cfqueryparam value="#url.id#" cfsqltype="cf_sql_integer">
		                                       </cfquery>
		                                       <cfscript>
		                                       	result = queryExecute( "SELECT * FROM exampleData WHERE id = :id AND title = :title", 	{
		                                       		title={
		                                       				value="Man walks on Moon",
		                                       				cfsqltype="cf_sql_varchar"
		                                       			},
		                                       		id={
		                                       				value=2,
		                                       				'cfsqltype'="cf_sql_integer"
		                                       			}
		                                       		},	{
		                                       			dbtype="query"
		                                       	} 	);

		                                       	result1 = queryExecute(	"SELECT * FROM exampleData WHERE id = ? AND title = ?", [
		                                       			{
		                                       				value="Man walks on Moon",
		                                       				cfsqltype="cf_sql_varchar"
		                                       			},
		                                       			{
		                                       				value=2,
		                                       				'cfsqltype'="cf_sql_integer"
		                                       			}
		                                       		],	{
		                                       			dbtype="query"
		                                       		} );
		                                       </cfscript>
		                                       """, BoxSourceType.CFTEMPLATE );
		assertThat( result.isCorrect() ).isTrue();
		String trasnpiledSource = result.getRoot().toString();
		assertThat( trasnpiledSource ).contains( "sqltype" );
		assertThat( trasnpiledSource ).contains( "varchar" );
		assertThat( trasnpiledSource ).contains( "integer" );
		assertThat( trasnpiledSource ).doesNotContain( "cfsqltype" );
		assertThat( trasnpiledSource ).doesNotContain( "cf_sql_" );
	}

}
