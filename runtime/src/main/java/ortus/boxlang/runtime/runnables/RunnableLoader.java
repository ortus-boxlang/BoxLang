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

import ortus.boxlang.runtime.context.IBoxContext;

/**
 * This class is responsible for taking a template on disk or arbitrary set of statements
 * and compiling them into an invokable class and loading that class.
 */
public class RunnableLoader {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Singleton instance
	 */
	private static RunnableLoader instance;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Private constructor
	 */
	private RunnableLoader() {
	}

	/**
	 * Get the singleton instance
	 *
	 * @return TemplateLoader
	 */
	public static synchronized RunnableLoader getInstance() {
		if ( instance == null ) {
			instance = new RunnableLoader();
		}
		return instance;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Public Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Load the class for a template, JIT compiling if needed
	 *
	 * @param path Absolute path on disk to the template
	 *
	 * @return
	 */
	public BoxTemplate loadTemplateAbsolute( IBoxContext context, String path ) {
		return null;
	}

	/**
	 * Load the class for a template, JIT compiling if needed
	 *
	 * @param path Relative path on disk to the template
	 *
	 * @return
	 */
	public BoxTemplate loadTemplateRelative( IBoxContext context, String path ) {
		BoxTemplate template = context.findClosestTemplate();
		if ( template != null ) {
			template.getRunnablePath();
		}
		return null;
	}

	/**
	 * Load the class for a script, JIT compiling if needed
	 *
	 * @param source The source to load
	 *
	 * @return
	 */
	public BoxScript loadScript( IBoxContext context, String source ) {
		return null;
	}
}
