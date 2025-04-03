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
package TestCases.applications;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.ConfigOverrideBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class ApplicationRequestStart {

	static BoxRuntime	instance;
	IBoxContext			context;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
	}

	@Test
	public void testAppOnRequestIimplicit() {
		context = getContext( "src/test/java/TestCases/applications/onrequestImplicit/", "index.bxm" );
		ByteArrayOutputStream	baos	= new ByteArrayOutputStream();
		PrintStream				out		= new PrintStream( baos );
		context.getRequestContext().setOut( out );
		instance.executeTemplate(
		    "index.bxm",
		    context );
		assertThat( baos.toString() ).isEqualTo( "My output" );
	}

	private IBoxContext getContext( String rootPath, String template ) {
		return new ScriptingRequestBoxContext( new ConfigOverrideBoxContext( instance.getRuntimeContext(), config -> {
			config.getAsStruct( Key.mappings ).put( "/", new java.io.File( rootPath ).getAbsolutePath() );
			return config;
		} ), FileSystemUtil.createFileUri( template ) );
	}

}
