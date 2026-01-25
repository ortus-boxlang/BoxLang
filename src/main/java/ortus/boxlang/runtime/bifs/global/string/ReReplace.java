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
import ortus.boxlang.runtime.types.util.RegexUtil;
import ortus.boxlang.runtime.util.RegexBuilder;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF( description = "Replace matches of a regular expression in a string" )
@BoxBIF( alias = "reReplaceNoCase" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "ReReplace" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "ReReplaceNoCase" )
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

		// Default string if null
		if ( string == null ) {
			string = "";
		}
		if ( substring == null ) {
			substring = "";
		}
		if ( regex == null ) {
			regex = "";
		}

		// Posix replacement for character classes
		regex	= RegexUtil.posixReplace( regex, noCase );
		// Ignore non-quantifier curly braces like PERL
		regex	= RegexUtil.replaceNonQuantiferCurlyBraces( regex );

		StringBuffer	result					= new StringBuffer();
		Matcher			matcher					= RegexBuilder.of( string, regex, noCase, Pattern.DOTALL ).matcher();
		boolean			upperCase				= false;
		boolean			lowerCase				= false;
		boolean			upperCaseOne			= false;
		boolean			lowerCaseOne			= false;
		boolean			lastBackslashWasEscaped	= false;

		while ( matcher.find() ) {
			StringBuffer replacement = new StringBuffer( substring );
			for ( int i = 0; i < replacement.length() - 1; i++ ) {
				char currentChar = replacement.charAt( i );
				if ( currentChar == '\\' ) {
					// collapse double \\ IF they are before a special sequence
					if ( i > 0
					    && replacement.charAt( i - 1 ) == '\\'
					    && !lastBackslashWasEscaped
					    && nextCharsAreSpecial( replacement, i ) ) {
						lastBackslashWasEscaped = true;
						replacement.delete( i, i + 1 );
						i--;
						continue;
					}
					lastBackslashWasEscaped = false;

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
					} else if ( replacement.charAt( i + 1 ) == 'u' ) {
						upperCaseOne	= true;
						lowerCaseOne	= false;
						replacement.delete( i, i + 2 );
						i--;
						continue;
					} else if ( replacement.charAt( i + 1 ) == 'l' ) {
						lowerCaseOne	= true;
						upperCaseOne	= false;
						replacement.delete( i, i + 2 );
						i--;
						continue;
					} else if ( Character.isDigit( replacement.charAt( i + 1 ) ) ) {
						int		groupIndexLength	= findGroupIndexLength( replacement, i + 1 );
						int		groupIndex			= Integer.parseInt( replacement.substring( i + 1, i + groupIndexLength ) );
						// Ignore invalid group indexes - use empty string if out of range
						String	group				= groupIndex > matcher.groupCount() ? "" : matcher.group( groupIndex );

						if ( group != null ) {
							if ( upperCase ) {
								group = group.toUpperCase();
							} else if ( lowerCase ) {
								group = group.toLowerCase();
							} else if ( upperCaseOne ) {
								group			= Character.toUpperCase( group.charAt( 0 ) ) + group.substring( 1 );
								upperCaseOne	= false;
							} else if ( lowerCaseOne ) {
								group			= Character.toLowerCase( group.charAt( 0 ) ) + group.substring( 1 );
								lowerCaseOne	= false;
							}
							replacement.replace( i, i + groupIndexLength, group );
							i += group.length() - 1;
						} else {
							// Skip replacement if group is null
							replacement.delete( i, i + 2 );
							i -= 2;
						}
						continue;
					}
				}
				if ( upperCase || upperCaseOne ) {
					replacement.replace( i, i + 1, String.valueOf( currentChar ).toUpperCase() );
					if ( upperCaseOne ) {
						upperCaseOne = false; // Reset after one use
					}
				} else if ( lowerCase || lowerCaseOne ) {
					replacement.replace( i, i + 1, String.valueOf( currentChar ).toLowerCase() );
					if ( lowerCaseOne ) {
						lowerCaseOne = false; // Reset after one use
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

	/**
	 * Returns true if the next char is a special char that can follow \
	 * or if the next two chars are a valid \X escape sequence.
	 * Returns false if the index is out of bounds.
	 */
	private boolean nextCharsAreSpecial( StringBuffer str, int i ) {
		// Skip ahead all \ chars
		while ( i < str.length() && str.charAt( i ) == '\\' ) {
			i++;
		}

		// And see if the next non-\ char is a special one
		char c = str.charAt( i );
		return c == 'l' || c == 'L' || c == 'u' || c == 'U' || c == 'E' || Character.isDigit( c );

	}

	/**
	 * Keep searching so long as we keep finding digits
	 */
	private int findGroupIndexLength( StringBuffer replacement, int startIndex ) {
		// Start with the second digit since we already know the first one is a digit or we wouldn't be here
		int length = 1;
		while ( startIndex + length < replacement.length()
		    && Character.isDigit( replacement.charAt( startIndex + length ) ) ) {
			length++;
		}
		return length + 1; // +1 for the backslash
	}

}
