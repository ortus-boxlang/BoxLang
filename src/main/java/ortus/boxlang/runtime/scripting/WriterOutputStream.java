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

import java.io.IOException;
import java.io.OutputStream;

import javax.script.ScriptContext;

public class WriterOutputStream extends OutputStream {

	private ScriptContext JSRScriptingContext;

	public WriterOutputStream( ScriptContext JSRScriptingContext ) {
		this.JSRScriptingContext = JSRScriptingContext;
	}

	@Override
	public void write( int b ) throws IOException {
		JSRScriptingContext.getWriter().write( b );
	}

	@Override
	public void flush() throws IOException {
		JSRScriptingContext.getWriter().flush();
	}

	@Override
	public void close() throws IOException {
		JSRScriptingContext.getWriter().close();
	}
}