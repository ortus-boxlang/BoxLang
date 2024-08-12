/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.string;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF
@BoxBIF( alias = "reReplaceNoCase" )
@BoxMember( type = BoxLangType.STRING, name = "ReReplace" )
@BoxMember( type = BoxLangType.STRING, name = "ReReplaceNoCase" )
public class ReReplace extends BIF {

	private static final Key reFindNoCase = Key.of( "ReReplaceNoCase" );

	/**
	 * Constructor
	 */
	public ReReplace() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.string ),
		    new Argument( true, "string", Key.regex ),
		    new Argument( true, "string", Key.substring ),
		    new Argument( true, "string", Key.scope, "one", Set.of( Validator.valueOneOf( "one", "all" ) ) )
		};
	}

	/**
	 * 
	 * Uses a regular expression (regex) to search a string for a string pattern and replace it with another. The search is case-sensitive.
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @argument.string The string to search
	 * 
	 * @argument.regex The regular expression to search for
	 * 
	 * @argument.substring The string to replace regex with
	 * 
	 * @argument.scope The scope to search in (one, all)
	 * 
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	string		= arguments.getAsString( Key.string );
		String	regex		= arguments.getAsString( Key.regex );
		String	substring	= arguments.getAsString( Key.substring );
		String	scope		= arguments.getAsString( Key.scope ).toLowerCase();
		boolean	noCase		= arguments.get( BIF.__functionName ).equals( reFindNoCase );

		if ( noCase ) {
			regex = "(?i)" + regex;
		}

		// Define POSIX character classes and their Java regex equivalents
		Map<String, String> replacements = new HashMap<>();
		replacements.put( "[:upper:]", "A-Z" );
		replacements.put( "[:lower:]", "a-z" );
		replacements.put( "[:digit:]", "0-9" );
		replacements.put( "[:xdigit:]", "0-9a-fA-F" );
		replacements.put( "[:alnum:]", "a-zA-Z0-9" );
		replacements.put( "[:alpha:]", "a-zA-Z" );
		replacements.put( "[:blank:]", " \\t" );
		replacements.put( "[:space:]", "\\s" );
		replacements.put( "[:cntrl:]", "\\x00-\\x1F\\x7F" );
		replacements.put( "[:punct:]", "!\"#$%&'()*+,-./:;<=>?@\\[\\]^_`{|}~" );
		replacements.put( "[:graph:]", "\\x21-\\x7E" );
		replacements.put( "[:print:]", "\\x20-\\x7E" );

		// Use a regex to find POSIX character classes in the regex
		Pattern			pattern		= Pattern.compile( "\\[(.*?)\\]" );
		Matcher			matcher2	= pattern.matcher( regex );

		// Replace each POSIX character class with its Java regex equivalent
		StringBuffer	sb			= new StringBuffer();
		while ( matcher2.find() ) {
			String insideBrackets = matcher2.group( 1 ); // get the content inside the square brackets
			for ( Map.Entry<String, String> entry : replacements.entrySet() ) {
				insideBrackets = insideBrackets.replace( entry.getKey(), entry.getValue() );
			}
			matcher2.appendReplacement( sb, Matcher.quoteReplacement( "[" + insideBrackets + "]" ) );
		}
		matcher2.appendTail( sb );
		regex = sb.toString();

		// Replace POSIX character classes that are not inside square brackets
		for ( Map.Entry<String, String> entry : replacements.entrySet() ) {
			regex = regex.replace( entry.getKey(), "[" + entry.getValue() + "]" );
		}

		StringBuffer	result		= new StringBuffer();
		Matcher			matcher		= Pattern.compile( regex ).matcher( string );

		boolean			upperCase	= false;
		boolean			lowerCase	= false;

		while ( matcher.find() ) {
			StringBuffer replacement = new StringBuffer( substring );
			for ( int i = 0; i < replacement.length() - 1; i++ ) {
				if ( replacement.charAt( i ) == '\\' ) {
					// If the character before the \ is also a \, skip this iteration
					if ( i > 0 && replacement.charAt( i - 1 ) == '\\' ) {
						continue;
					}

					if ( replacement.charAt( i + 1 ) == 'U' ) {
						upperCase	= true;
						lowerCase	= false;
						replacement.delete( i, i + 2 );
						i--;
						continue;
					} else if ( replacement.charAt( i + 1 ) == 'L' ) {
						lowerCase	= true;
						upperCase	= false;
						replacement.delete( i, i + 2 );
						i--;
						continue;
					} else if ( replacement.charAt( i + 1 ) == 'E' ) {
						upperCase	= false;
						lowerCase	= false;
						replacement.delete( i, i + 2 );
						i--;
						continue;
					} else if ( Character.isDigit( replacement.charAt( i + 1 ) ) ) {
						int		groupIndex	= Character.getNumericValue( replacement.charAt( i + 1 ) );
						String	group		= matcher.group( groupIndex );

						if ( upperCase && group != null ) {
							group = group.toUpperCase();
						} else if ( lowerCase && group != null ) {
							group = group.toLowerCase();
						}
						// Check if the previous two characters were \\u or \\l
						if ( i >= 2 && replacement.charAt( i - 2 ) == '\\' && group != null ) {
							if ( replacement.charAt( i - 1 ) == 'u' ) {
								// Uppercase the first character of the group
								group = Character.toUpperCase( group.charAt( 0 ) ) + group.substring( 1 );
								replacement.delete( i - 2, i );
								i -= 2;
							} else if ( replacement.charAt( i - 1 ) == 'l' ) {
								// Lowercase the first character of the group
								group = Character.toLowerCase( group.charAt( 0 ) ) + group.substring( 1 );
								replacement.delete( i - 2, i );
								i -= 2;
							}
						}

						replacement.replace( i, i + 2, group );
						i += ( group != null ? group.length() : 0 ) - 2;
					}
				}
			}
			matcher.appendReplacement( result, Matcher.quoteReplacement( replacement.toString() ) );

			// If scope is "one", break after the first replacement
			if ( scope.equals( "one" ) ) {
				break;
			}
		}

		matcher.appendTail( result );
		return result.toString();
	}

}
