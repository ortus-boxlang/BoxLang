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
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import ortus.boxlang.runtime.types.IStruct;

/**
 * Tests for importing a directory of CFML custom tags under a prefix:
 * {@code <cfimport taglib="/path/to/tags" prefix="app">} followed by {@code <app:tagname>}.
 */
public class TaglibImportTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );
	StringBuffer		buffer;

	static final String	TAGLIB	= "src/test/java/ortus/boxlang/runtime/components/system/taglib";

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		this.context	= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		this.variables	= this.context.getScopeNearby( VariablesScope.name );
		this.buffer		= new StringBuffer();
		this.context.pushBuffer( this.buffer );
	}

	@AfterEach
	public void teardownEach() {
		this.context.popBuffer();
	}

	private String output() {
		return this.buffer.toString().replaceAll( "\\s", "" );
	}

	@Test
	public void testSelfClosingPrefixedTag() {
		instance.executeSource(
		    """
		    <cfimport taglib="%s" prefix="app">
		    <app:hello name="x" />
		    """.formatted( TAGLIB ),
		    this.context, BoxSourceType.CFTEMPLATE );
		assertThat( output() ).isEqualTo( "[hellox]" );
	}

	@Test
	public void testPrefixedTagWithoutSlash() {
		instance.executeSource(
		    """
		    <cfimport taglib="%s/" prefix="app">
		    <app:hello name="x">
		    """.formatted( TAGLIB ),
		    this.context, BoxSourceType.CFTEMPLATE );
		assertThat( output() ).isEqualTo( "[hellox]" );
	}

	@Test
	public void testPrefixAndTagNameAreCaseInsensitive() {
		instance.executeSource(
		    """
		    <CFIMPORT TAGLIB="%s" PREFIX="App">
		    <aPP:Hello name="x" />
		    """.formatted( TAGLIB ),
		    this.context, BoxSourceType.CFTEMPLATE );
		assertThat( output() ).isEqualTo( "[hellox]" );
	}

	@Test
	public void testPairedTagWithBodyAndEndExecution() {
		instance.executeSource(
		    """
		    <cfimport taglib="%s" prefix="app">
		    <app:wrap>inside</app:wrap>
		    """.formatted( TAGLIB ),
		    this.context, BoxSourceType.CFTEMPLATE );
		assertThat( output() ).isEqualTo( "(inside)" );
	}

	@Test
	public void testNestedPrefixedTagsAssociateWithTheirParent() {
		instance.executeSource(
		    """
		    <cfimport taglib="%s" prefix="app">
		    <app:location>
		    	<app:locationinput classcodename="news" />
		    	<app:locationinput classcodename="image" />
		    </app:location>
		    """.formatted( TAGLIB ),
		    this.context, BoxSourceType.CFTEMPLATE );
		IStruct	locationResult	= this.variables.getAsStruct( result );
		Array	inputs			= locationResult.getAsArray( Key.of( "inputs" ) );
		assertThat( inputs.size() ).isEqualTo( 2 );
		assertThat( ( ( IStruct ) inputs.get( 0 ) ).getAsString( Key.of( "classcodename" ) ) ).isEqualTo( "news" );
		assertThat( ( ( IStruct ) inputs.get( 1 ) ).getAsString( Key.of( "classcodename" ) ) ).isEqualTo( "image" );
		assertThat( locationResult.getAsString( Key.of( "baseTags" ) ).toLowerCase() ).isEqualTo( "cf_locationinput,cf_location" );
	}

	@Test
	public void testEndTagIsNotConfusedWithBuiltInComponentOfTheSameName() {
		// <app:location> must not pair with, or be validated as, the built-in cflocation component
		instance.executeSource(
		    """
		    <cfimport taglib="%s" prefix="app">
		    <app:wrap><app:location><app:locationinput classcodename="news" /></app:location>done</app:wrap>
		    """.formatted( TAGLIB ),
		    this.context, BoxSourceType.CFTEMPLATE );
		assertThat( output() ).isEqualTo( "(done)" );
	}

	@Test
	public void testTemplateAndNameAttributesArePassedToTheTag() {
		instance.executeSource(
		    """
		    <cfimport taglib="%s" prefix="app">
		    <app:include template="other.cfm" name="n" />
		    """.formatted( TAGLIB ),
		    this.context, BoxSourceType.CFTEMPLATE );
		assertThat( output() ).isEqualTo( "[includeother.cfm|n]" );
	}

	@Test
	public void testDynamicAttributeValues() {
		instance.executeSource(
		    """
		    <cfset who = "dyn">
		    <cfimport taglib="%s" prefix="app">
		    <app:hello name="#who#" />
		    """.formatted( TAGLIB ),
		    this.context, BoxSourceType.CFTEMPLATE );
		assertThat( output() ).isEqualTo( "[hellodyn]" );
	}

	@Test
	public void testUnimportedPrefixIsPlainText() {
		instance.executeSource(
		    """
		    <cfimport taglib="%s" prefix="app">
		    <other:hello name="x" />
		    """.formatted( TAGLIB ),
		    this.context, BoxSourceType.CFTEMPLATE );
		assertThat( output() ).isEqualTo( "<other:helloname=\"x\"/>" );
	}

	@Test
	public void testPrefixedTagBeforeTheImportIsPlainText() {
		instance.executeSource(
		    """
		    <app:hello name="x" />
		    <cfimport taglib="%s" prefix="app">
		    """.formatted( TAGLIB ),
		    this.context, BoxSourceType.CFTEMPLATE );
		assertThat( output() ).isEqualTo( "<app:helloname=\"x\"/>" );
	}

	@Test
	public void testPrefixedTagInsideCfoutput() {
		instance.executeSource(
		    """
		    <cfimport taglib="%s" prefix="app">
		    <cfset who = "out">
		    <cfoutput>#who#<app:hello name="#who#" /></cfoutput>
		    """.formatted( TAGLIB ),
		    this.context, BoxSourceType.CFTEMPLATE );
		assertThat( output() ).isEqualTo( "out[helloout]" );
	}

	@Test
	public void testKeywordTagNameAfterPrefix() {
		// "include" is a CF keyword after a plain cf prefix, but an ordinary tag name after a taglib prefix,
		// also as a paired tag whose end tag must not be lexed as </cfinclude>
		instance.executeSource(
		    """
		    <cfimport taglib="%s" prefix="app">
		    <app:wrap><app:include template="t" name="i"></app:include></app:wrap>
		    """.formatted( TAGLIB ),
		    this.context, BoxSourceType.CFTEMPLATE );
		assertThat( output() ).isEqualTo( "([includet|i])" );
	}

	@Test
	public void testDynamicTaglibIsAParseError() {
		// Tag libraries are resolved at compile time, as in Lucee and Adobe ColdFusion
		Throwable error = assertThrows( Throwable.class, () -> instance.executeSource(
		    """
		    <cfset tags = "%s">
		    <cfimport taglib="#tags#" prefix="app">
		    """.formatted( TAGLIB ),
		    this.context, BoxSourceType.CFTEMPLATE ) );
		assertThat( error.getMessage() ).contains( "string literal" );
	}

	@Test
	public void testPrefixedTagInsideCfif() {
		instance.executeSource(
		    """
		    <cfimport taglib="%s" prefix="app">
		    <cfif true><app:hello name="if" /></cfif>
		    """.formatted( TAGLIB ),
		    this.context, BoxSourceType.CFTEMPLATE );
		assertThat( output() ).isEqualTo( "[helloif]" );
	}

}
