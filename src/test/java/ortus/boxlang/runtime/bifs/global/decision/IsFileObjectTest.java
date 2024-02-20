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

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class IsFileObjectTest {

	static BoxRuntime		instance;
	static IBoxContext		context;
	static IScope			variables;
	private static String	tmpDirectory	= "src/test/resources/tmp/isFileObjectTest";
	private static String	testFile		= "src/test/resources/tmp/file-test.txt";

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() throws IOException {
		if ( FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.deleteDirectory( tmpDirectory, true );
		}
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() throws IOException {
		if ( !FileSystemUtil.exists( testFile ) ) {
			FileSystemUtil.write( testFile, "is file obj test!".getBytes( "UTF-8" ), true );
		}
		variables.clear();
	}

	@DisplayName( "It detects file objects" )
	@Test
	public void testTrueConditions() {
		assertThat( ( Boolean ) instance.executeStatement( "isFileObject( fileOpen( '" + testFile + "', 'write' ) )" ) ).isTrue();
	}

	@DisplayName( "It detects non-file objects" )
	@Test
	public void testFalseConditions() {
		assertThat( ( Boolean ) instance.executeStatement( "isFileObject( '" + testFile + "' )" ) ).isFalse();
		assertThat( ( Boolean ) instance.executeStatement( "isFileObject( createObject( 'java', 'java.io.File' ) )" ) ).isFalse();

	}

}
