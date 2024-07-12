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
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

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
		assertThat( ( ( IClassRunnable ) variables.get( Key.of( "clazz" ) ) ).getName().getName() ).isEqualTo( "src.test.java.testcases.phase3.MyClassCF" );
	}
}
