
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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class LogTest {

	static BoxRuntime	instance;
	static String		logsDirectory;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance		= BoxRuntime.getInstance( true );
		logsDirectory	= instance.getConfiguration().runtime.logsDirectory;

	}

	@AfterAll
	public static void teardown() {
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It tests the BIF Log with Script parsing" )
	@Test
	public void testComponentScript() {
		instance.executeSource(
		    """
		    log text="Hello Logger!" log="Foo" file="foo.log";
		    """,
		    context, BoxSourceType.BOXSCRIPT );
	}

	@DisplayName( "It tests the BIF Log with CFML parsing" )
	@Test
	public void testComponentCF() {
		instance.executeSource(
		    """
		    <cflog text="Hello Logger!" log="Foo" file="foo.log" />
		    """,
		    context, BoxSourceType.CFTEMPLATE );
	}

	@DisplayName( "It tests the BIF Log with BoxLang parsing" )
	@Test
	public void testComponentBX() {
		instance.executeSource(
		    """
		    <bx:log text="Hello Logger!" log="Foo" file="foo.log" />
		    """,
		    context, BoxSourceType.BOXTEMPLATE );
	}

	@DisplayName( "It tests the BIF Log with Script parsing" )
	@Test
	public void testComponentCustomLogScript() {
		String logFilePath = Paths.get( logsDirectory, "/foo.log" ).normalize().toString();
		if ( FileSystemUtil.exists( logFilePath ) ) {
			FileSystemUtil.deleteFile( logFilePath );
		}

		instance.executeSource(
		    """
		    log text="Hello Logger!" log="Foo" file="foo.log";
		    """,
		    context, BoxSourceType.BOXSCRIPT );

		assertTrue( FileSystemUtil.exists( logFilePath ) );
		String fileContent = StringCaster.cast( FileSystemUtil.read( logFilePath ) );
		assertTrue( StringUtils.contains( fileContent, "Hello Logger!" ) );
	}

	@DisplayName( "It tests the BIF Log with CF tag parsing" )
	@Test
	public void testComponentCustomLogCF() {
		String logFilePath = Paths.get( logsDirectory, "/foo.log" ).normalize().toString();
		if ( FileSystemUtil.exists( logFilePath ) ) {
			FileSystemUtil.deleteFile( logFilePath );
		}

		instance.executeSource(
		    """
		    <cflog text="Hello Logger!" log="Foo" file="foo.log"/>
		    """,
		    context, BoxSourceType.CFTEMPLATE );

		assertTrue( FileSystemUtil.exists( logFilePath ) );
		String fileContent = StringCaster.cast( FileSystemUtil.read( logFilePath ) );
		assertTrue( StringUtils.contains( fileContent, "Hello Logger!" ) );
	}

	@DisplayName( "It tests the BIF Log with BX tag parsing" )
	@Test
	public void testComponentCustomLogBX() {
		String logFilePath = Paths.get( logsDirectory, "/foo.log" ).normalize().toString();
		if ( FileSystemUtil.exists( logFilePath ) ) {
			FileSystemUtil.deleteFile( logFilePath );
		}

		instance.executeSource(
		    """
		    <bx:log text="Hello Logger!" log="Foo" file="foo.log"/>
		    """,
		    context, BoxSourceType.BOXTEMPLATE );

		assertTrue( FileSystemUtil.exists( logFilePath ) );
		String fileContent = StringCaster.cast( FileSystemUtil.read( logFilePath ) );
		assertTrue( StringUtils.contains( fileContent, "Hello Logger!" ) );
	}

}
