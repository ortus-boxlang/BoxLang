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

/**
 * Represents an import
 *
 * import prefix:package.to.Class as alias
 *
 * @param className      The class name
 * @param resolverPrefix The resolver prefix
 * @param alias          The alias
 */
public record ImportRecord( String className, String resolverPrefix, String alias ) {

	// Compact constructor disallows null className
	public ImportRecord {
		if ( className == null ) {
			throw new IllegalArgumentException( "Class name cannot be null." );
		}
	}

	public static ImportRecord parse( String importStr ) {
		String	className			= importStr;
		String	resolverPrefix		= null;
		String	alias				= null;

		int		aliasDelimiterPos	= importStr.toLowerCase().lastIndexOf( " as " );
		if ( aliasDelimiterPos != -1 ) {
			alias		= importStr.substring( aliasDelimiterPos + 4 );
			className	= importStr.substring( 0, aliasDelimiterPos );
		} else {
			// If there is no alias, use the last part of the class name as the alias
			String[] parts = className.split( "\\." );
			alias = parts[ parts.length - 1 ];
		}

		int resolverDelimiterPos = className.indexOf( ":" );
		if ( resolverDelimiterPos != -1 ) {
			resolverPrefix	= className.substring( 0, resolverDelimiterPos );
			className		= className.substring( resolverDelimiterPos + 1 );
		}

		return new ImportRecord( className, resolverPrefix, alias );
	}
}