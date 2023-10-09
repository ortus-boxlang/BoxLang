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
package ortus.boxlang.runtime.types;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.scopes.Key;

public class SampleUDF extends UDF {

	Object						returnVal	= null;

	// These are not static just because this is a test class that is always transient! Do not copy this implementation.
	private Key					name;
	private Argument[]			arguments;
	private String				returnType;
	private String				hint;
	private boolean				output;
	private Access				access;
	private Map<Key, Object>	metadata	= new HashMap<Key, Object>();

	public Key getName() {
		return name;
	}

	public Argument[] getArguments() {
		return arguments;
	}

	public String getReturnType() {
		return returnType;
	}

	public String getHint() {
		return hint;
	}

	public boolean isOutput() {
		return output;
	}

	public Map<Key, Object> getAdditionalMetadata() {
		return metadata;
	}

	public Access getAccess() {
		return access;
	}

	@Override
	public long getRunnableCompileVersion() {
		return 0;
	}

	@Override
	public LocalDateTime getRunnableCompiledOn() {
		return null;
	}

	@Override
	public Object getRunnableAST() {
		return null;
	}

	public IBoxRunnable getDeclaringRunnable() {
		return null;
	}

	public SampleUDF( Access access, Key name, String returnType, Argument[] arguments, String hint, boolean output,
	    Object returnVal ) {
		super();
		this.access		= access;
		this.name		= name;
		this.returnType	= returnType;
		this.arguments	= arguments;
		this.hint		= hint;
		this.output		= output;
		this.returnVal	= returnVal;
	}

	@Override
	public Object _invoke( FunctionBoxContext context ) {
		return returnVal;
	}
}