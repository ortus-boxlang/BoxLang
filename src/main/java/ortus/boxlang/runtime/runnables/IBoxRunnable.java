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
package ortus.boxlang.runtime.runnables;

import java.time.LocalDateTime;
import java.util.List;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.util.ResolvedFilePath;

public interface IBoxRunnable {

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The imports for this runnable
	 */
	public List<ImportDefinition> getImports();

	/**
	 * The version of the BoxLang runtime
	 */
	public long getRunnableCompileVersion();

	/**
	 * The date the template was compiled
	 */
	public LocalDateTime getRunnableCompiledOn();

	/**
	 * The AST (abstract syntax tree) of the runnable
	 */
	public Object getRunnableAST();

	/**
	 * The path to the template
	 */
	public ResolvedFilePath getRunnablePath();

	/**
	 * The original source type
	 */
	public BoxSourceType getSourceType();
}
