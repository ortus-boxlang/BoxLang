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
package ortus.boxlang.runtime.scripting;

import static com.google.common.truth.Truth.assertThat;

import javax.script.ScriptEngine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BoxScriptingFactoryTest {

	@Test
	public void testGetEngineName() {
		BoxScriptingFactory factory = new BoxScriptingFactory();
		assertThat( factory.getEngineName() ).isEqualTo( "BoxLang" );
	}

	@Test
	public void testGetEngineVersion() {
		BoxScriptingFactory factory = new BoxScriptingFactory();
		assertThat( factory.getEngineVersion() ).isNotEmpty();
	}

	@Test
	public void testGetLanguageName() {
		BoxScriptingFactory factory = new BoxScriptingFactory();
		assertThat( factory.getLanguageName() ).isEqualTo( "BoxLang" );
	}

	@Test
	public void testGetLanguageVersion() {
		BoxScriptingFactory factory = new BoxScriptingFactory();
		assertThat( factory.getLanguageVersion() ).isNotEmpty();
	}

	@Test
	public void testGetScriptEngine() {
		BoxScriptingFactory factory = new BoxScriptingFactory();
		assertThat( factory.getScriptEngine() ).isInstanceOf( BoxScriptingEngine.class );
	}

	@Test
	public void testGetExtensions() {
		BoxScriptingFactory factory = new BoxScriptingFactory();
		assertThat( factory.getExtensions() ).containsExactly( "bx", "cfm", "cfc", "cfs", "bxs", "bxm" );
	}

	@Test
	public void testGetMimeTypes() {
		BoxScriptingFactory factory = new BoxScriptingFactory();
		assertThat( factory.getMimeTypes() ).isEmpty();
	}

	@Test
	public void testGetNames() {
		BoxScriptingFactory factory = new BoxScriptingFactory();
		assertThat( factory.getNames() ).containsExactly( "BoxLang", "BL", "BX" );
	}

	@DisplayName( "Can get a new BoxLang ScriptEngine" )
	@Test
	public void testGetParameter() {
		ScriptEngine engine = new BoxScriptingFactory().getScriptEngine();
		assertThat( engine ).isNotNull();
	}

}
