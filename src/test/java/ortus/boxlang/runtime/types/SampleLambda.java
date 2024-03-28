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

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.scopes.Key;

public class SampleLambda extends Lambda {

	Object				returnVal	= null;

	// These are not static just because this is a test class that is always transient! Do not copy this implementation.
	private Key			name		= Lambda.defaultName;
	private Argument[]	arguments;
	private String		returnType	= "any";
	private Access		access		= Access.PUBLIC;

	public Key getName() {
		return name;
	}

	public Argument[] getArguments() {
		return arguments;
	}

	public String getReturnType() {
		return returnType;
	}

	public IStruct getAnnotations() {
		return Struct.EMPTY;
	}

	public IStruct getDocumentation() {
		return Struct.EMPTY;
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

	public List<ImportDefinition> getImports() {
		return null;
	}

	public Path getRunnablePath() {
		return Path.of( "unknown" );
	}

	public BoxSourceType getSourceType() {
		return BoxSourceType.BOXSCRIPT;
	}

	public SampleLambda( Argument[] arguments, Object returnVal ) {
		super();
		this.returnVal	= returnVal;
		this.arguments	= arguments;
	}

	@Override
	public Object _invoke( FunctionBoxContext context ) {
		return returnVal;
	}
}