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

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import ortus.boxlang.runtime.runnables.BoxScript;

public class BoxCompiledScript extends CompiledScript {

	private BoxScriptingEngine	scriptEngine;
	private BoxScript			boxScript;

	public BoxCompiledScript( BoxScriptingEngine scriptEngine, BoxScript boxScript ) {
		this.scriptEngine	= scriptEngine;
		this.boxScript		= boxScript;
	}

	@Override
	public Object eval( ScriptContext context ) throws ScriptException {
		return boxScript.invoke( scriptEngine.getBoxContext() );
	}

	@Override
	public ScriptEngine getEngine() {
		return scriptEngine;
	}

}