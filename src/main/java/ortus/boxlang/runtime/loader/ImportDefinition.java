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

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Represents an import in BoxLang. Imports can have aliases, wildcards, and resolver prefixes.
 * <p>
 * However, they can also be linked to a specific BoxLang Module via the {@code @{moduleName}} syntax.
 * <p>
 * Normal Resolution:
 *
 * <pre>
 * import prefix:package.to.Class as alias
 * import package.to.Class
 * import package.to.Class as alias
 * import package.to.*
 * </pre>
 * <p>
 * Module Resolution:
 *
 * <pre>
 * import package.to.Class@ModuleName as alias
 * import package.to.Class@ModuleName
 * import package.to.Class@ModuleName as alias
 * import package.to.*@ModuleName
 * </pre>
 *
 * @param className      The class name
 * @param resolverPrefix The resolver prefix
 * @param alias          The alias
 */
public record ImportDefinition( String className, String resolverPrefix, String alias, String moduleName ) {

	// Compact constructor disallows null className
	public ImportDefinition {
		if ( className == null ) {
			throw new BoxRuntimeException( "Class name cannot be null." );
		}
	}

	/**
	 * Is this import a wildcard import?
	 *
	 * @return True if it is a wildcard import, false otherwise
	 */
	public Boolean isMultiImport() {
		return className.endsWith( ".*" );
	}

	/**
	 * Returns the package name of the import definition
	 *
	 * @return The package name
	 */
	public String getPackageName() {
		return className.substring( 0, className.lastIndexOf( "." ) );
	}

	/**
	 * Returns the fully qualified class name of the import definition
	 * considering if it is a wildcard import or not
	 *
	 * @param targetClass The class name in code that needed qualification
	 *
	 * @return The fully qualified class name
	 */
	public String getFullyQualifiedClass( String targetClass ) {
		if ( isMultiImport() ) {
			return String.format( "%s.%s", className.substring( 0, className.length() - 2 ), targetClass );
		} else {
			return className;
		}
	}

	/**
	 * Parses an import string into an ImportDefinition
	 *
	 * @param importStr The import string
	 *
	 * @return The ImportDefinition instance
	 */
	public static ImportDefinition parse( String importStr ) {
		String	className			= importStr;
		String	resolverPrefix		= null;
		String	alias				= null;
		String	module				= null;

		int		aliasDelimiterPos	= importStr.toLowerCase().lastIndexOf( " as " );
		if ( aliasDelimiterPos != -1 ) {
			alias		= importStr.substring( aliasDelimiterPos + 4 );
			className	= importStr.substring( 0, aliasDelimiterPos );
		} else {
			// If there is no alias, use the last part of the class name as the alias
			String[] parts = className.split( "\\." );
			alias = parts[ parts.length - 1 ];
			// If there is one or more $ chars, take the last segment (nested class)
			if ( alias.contains( "$" ) ) {
				alias = alias.substring( alias.lastIndexOf( "$" ) + 1 );
			}
			int moduleDelimiterPos = alias.indexOf( "@" );
			if ( moduleDelimiterPos != -1 ) {
				alias = alias.substring( 0, moduleDelimiterPos );
			}
		}

		// Check if the import is a module import, the class name must have a @moduleName
		// Parse the module name and remove it from the class name
		int moduleDelimiterPos = className.indexOf( "@" );
		if ( moduleDelimiterPos != -1 ) {
			module		= className.substring( moduleDelimiterPos + 1 );
			className	= className.substring( 0, moduleDelimiterPos );
		}

		int resolverDelimiterPos = className.indexOf( ":" );
		if ( resolverDelimiterPos != -1 ) {
			resolverPrefix	= className.substring( 0, resolverDelimiterPos );
			className		= className.substring( resolverDelimiterPos + 1 );
		}

		return new ImportDefinition( className, resolverPrefix, alias, module );
	}
}
