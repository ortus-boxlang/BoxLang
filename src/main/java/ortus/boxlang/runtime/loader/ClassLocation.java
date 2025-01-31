
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
package ortus.boxlang.runtime.loader;

import java.time.Instant;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * This record represents a class location in the application
 *
 * @param name         The name of the class
 * @param path         The fully absolute path to the class
 * @param packageName  The package the class belongs to
 * @param type         The type of class it is: 1. Box class (this.BX_TYPE), 2. Java class (this.JAVA_TYPE)
 * @param clazz        The class object that represents the loaded class
 * @param module       The module the class belongs to, null if none
 * @param cacheable    If the class is cacheable or not
 * @param application  The application the class belongs to, null if none
 * @param lastModified The last modified date of the class on disk
 */
public record ClassLocation(
    String name,
    String path,
    String packageName,
    int type,
    Class<?> clazz,
    String module,
    Boolean cacheable,
    String application,
    Instant lastModified ) {

	/**
	 * Verify if the class is from a module
	 */
	public Boolean isFromModule() {
		return module != null;
	}

	/**
	 * Override the getter
	 * Java Classes just return the class
	 * BoxLang classes we ask the Loader to load it
	 */
	public Class<?> clazz( IBoxContext context ) {
		// Java classes are already loaded
		if ( ClassLocator.TYPE_JAVA == type ) {
			return clazz;
		}
		// BoxLang we delegate to the loader
		return RunnableLoader.getInstance().loadClass( ResolvedFilePath.of( path() ), context );
	}

	/**
	 * Show the state of this record as a string
	 */
	@Override
	public String toString() {
		return String.format(
		    "ClassLocation [name=%s, path=%s, packageName=%s, type=%s, module=%s, cacheable=%s]",
		    name,
		    path,
		    packageName,
		    type,
		    module,
		    cacheable
		);
	}
}
