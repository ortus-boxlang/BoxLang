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
 *
 * Implementation based on the printer implementation in the prettier project:
 * https://github.com/prettier/prettier/blob/main/src/document/printer.js
 * 
 */
package ortus.boxlang.compiler.prettyprint;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Printer {

	private final Config config;

	public Printer( Config config ) {
		this.config = config;
	}

	public int trim( final StringBuilder sb ) {
		int	originalLength	= sb.length();
		int	newLength		= originalLength;
		while ( newLength > 0 && ( sb.charAt( newLength - 1 ) == ' ' || sb.charAt( newLength - 1 ) == '\t' ) ) {
			newLength--;
		}
		sb.setLength( newLength );
		return originalLength - newLength;
	}

	public boolean fits( PrintCmd nextCmd, Stack<PrintCmd> restCmds, int remainingSpace, boolean hasLineSuffix ) {
		Stack<PrintCmd>	cmdStack	= new Stack<>();
		int				restCmdsIdx	= restCmds.size();
		cmdStack.push( nextCmd );

		while ( remainingSpace >= 0 ) {
			if ( cmdStack.isEmpty() ) {
				if ( restCmdsIdx == 0 ) {
					return true;
				}
				cmdStack.push( restCmds.get( --restCmdsIdx ) );
			}

			PrintCmd	cmd		= cmdStack.pop();
			var			content	= cmd.getContent();

			if ( content instanceof String str ) {
				remainingSpace -= str.length();
			} else if ( content instanceof List<?> listContent ) {
				for ( int i = listContent.size() - 1; i >= 0; i-- ) {
					cmdStack.push( new PrintCmd( cmd.getMode(), cmd.getIndent(), listContent.get( i ) ) );
				}
			} else if ( content instanceof Doc docContent ) {
				switch ( docContent.getDocType() ) {
					case ARRAY, INDENT -> {
						var indent = docContent.getDocType() == DocType.INDENT ? cmd.getIndent() + 1 : cmd.getIndent();
						cmdStack.push( new PrintCmd( cmd.getMode(), indent, docContent.getContents() ) );
					}
					case GROUP -> {
						var groupMode = docContent.willBreak() ? PrintCmd.Mode.BREAK : cmd.getMode();
						cmdStack.push( new PrintCmd( groupMode, cmd.getIndent(), docContent.getContents() ) );
					}
					case LINE_SUFFIX -> hasLineSuffix = true; // might be important later
					default -> {
						// do nothing for other doc types
					}
				}
			} else if ( content instanceof Line lineContent ) {
				if ( cmd.getMode() == PrintCmd.Mode.BREAK || lineContent == Line.HARD ) {
					return true;
				} else if ( lineContent == Line.LINE ) {
					remainingSpace--;
				}
			}
		}
		return false;
	}

	public String print( Doc doc ) {
		StringBuilder	sb				= new StringBuilder();
		int				currentColumn	= 0;
		Stack<PrintCmd>	cmdStack		= new Stack<>();
		List<PrintCmd>	lineSuffix		= new ArrayList<>();

		// doc.propagateWillBreak();

		cmdStack.push( new PrintCmd( PrintCmd.Mode.BREAK, 0, doc ) );

		while ( !cmdStack.isEmpty() ) {
			PrintCmd	cmd		= cmdStack.pop();
			var			content	= cmd.getContent();

			if ( content instanceof String str ) {
				sb.append( str );
				currentColumn += str.length();
			} else if ( content instanceof List<?> listContent ) {
				// iterate in reverse order to maintain order in the stack
				for ( int i = listContent.size() - 1; i >= 0; i-- ) {
					cmdStack.push( new PrintCmd( cmd.getMode(), cmd.getIndent(), listContent.get( i ) ) );
				}
			} else if ( content instanceof Doc docContent ) {
				var docType = docContent.getDocType();
				switch ( docType ) {
					case ARRAY, INDENT -> {
						var indent = docType == DocType.INDENT ? cmd.getIndent() + 1 : cmd.getIndent();
						cmdStack.push( new PrintCmd( cmd.getMode(), indent, docContent.getContents() ) );
					}
					case GROUP -> {
						switch ( cmd.getMode() ) {
							case FLAT -> {
								var mode = docContent.willBreak() ? PrintCmd.Mode.BREAK : PrintCmd.Mode.FLAT;
								cmdStack.push( new PrintCmd( mode, cmd.getIndent(), docContent.getContents() ) );
							}
							case BREAK -> {
								var	nextCmd			= new PrintCmd( PrintCmd.Mode.FLAT, cmd.getIndent(), docContent.getContents() );
								var	remainingSpace	= config.getMaxLineLength() - currentColumn;
								var	hasLineSuffix	= lineSuffix.size() > 0;
								if ( !docContent.willBreak() && fits( nextCmd, cmdStack, remainingSpace, hasLineSuffix ) ) {
									cmdStack.push( nextCmd );
								} else {
									cmdStack.push( new PrintCmd( PrintCmd.Mode.BREAK, cmd.getIndent(), docContent.getContents() ) );
								}
							}
						}
					}
					case LINE_SUFFIX -> {
						lineSuffix.add( new PrintCmd( cmd.getMode(), cmd.getIndent(), docContent.getContents() ) );
					}
					default -> {
						// do nothing for other doc types
					}
				}
			} else if ( content instanceof Line lineContent ) {
				if ( lineContent != Line.BREAK_PARENT ) {
					switch ( cmd.getMode() ) {
						case FLAT -> {
							if ( lineContent == Line.LINE ) {
								sb.append( " " );
								currentColumn++;
							}
						}
						case BREAK -> {
							if ( !lineSuffix.isEmpty() ) {
								cmdStack.push( cmd );
								for ( int i = lineSuffix.size() - 1; i >= 0; i-- ) {
									cmdStack.push( lineSuffix.get( i ) );
								}
								lineSuffix.clear();
							} else {
								trim( sb ); // trim the current line
								// print a new line, followed by indent
								sb.append( config.lineSeparator() );
								sb.append( config.indentToLevel( cmd.getIndent() ) );
								currentColumn = cmd.getIndent() * config.getIndentSize();
							}
						}
					}
				}
			}

			// Flush remaining line-suffix contents at the end of the document
			if ( cmdStack.isEmpty() && !lineSuffix.isEmpty() ) {
				for ( int i = lineSuffix.size() - 1; i >= 0; i-- ) {
					cmdStack.push( lineSuffix.get( i ) );
				}
				lineSuffix.clear();
			}
		}

		return sb.toString();
	}

}
