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
package ortus.boxlang.runtime.cli;

/**
 * Fluent API for ANSI color printing with method chaining.
 *
 * This class provides a dynamic and fluent approach to colored console output.
 * Colors can be specified by name or ANSI code (0-255), and styles can be chained.
 *
 * <h3>Usage Examples:</h3>
 *
 * <pre>
 * ColorPrint.color( "red" ).bold().print( "Error: " );
 * ColorPrint.color( "green" ).println( "Success!" );
 * ColorPrint.color( 196 ).background( "yellow" ).italic().print( "Custom colors" );
 * ColorPrint.bold().blue().println( "Bold blue text" );
 * </pre>
 */
public class ColorPrint {

	private StringBuilder ansiCodes = new StringBuilder();

	// Private constructor - use static factory methods
	private ColorPrint() {
	}

	// Factory methods for color by name
	public ColorPrint color( String colorName ) {
		ansiCodes.append( getColorCode( colorName, false ) );
		return this;
	}

	// Factory methods for color by ANSI code (0-255)
	public ColorPrint color( int ansiCode ) {
		if ( ansiCode >= 0 && ansiCode <= 255 ) {
			ansiCodes.append( "\033[38;5;" ).append( ansiCode ).append( "m" );
		}
		return this;
	}

	// Background color methods
	public ColorPrint background( String colorName ) {
		ansiCodes.append( getColorCode( colorName, true ) );
		return this;
	}

	public ColorPrint background( int ansiCode ) {
		if ( ansiCode >= 0 && ansiCode <= 255 ) {
			ansiCodes.append( "\033[48;5;" ).append( ansiCode ).append( "m" );
		}
		return this;
	}

	// Style methods
	public ColorPrint bold() {
		ansiCodes.append( "\033[1m" );
		return this;
	}

	public ColorPrint italic() {
		ansiCodes.append( "\033[3m" );
		return this;
	}

	public ColorPrint underline() {
		ansiCodes.append( "\033[4m" );
		return this;
	}

	public ColorPrint dim() {
		ansiCodes.append( "\033[2m" );
		return this;
	}

	public ColorPrint strikethrough() {
		ansiCodes.append( "\033[9m" );
		return this;
	}

	// Convenient color methods (for fluent API)
	public ColorPrint black() {
		return color( "black" );
	}

	public ColorPrint red() {
		return color( "red" );
	}

	public ColorPrint green() {
		return color( "green" );
	}

	public ColorPrint yellow() {
		return color( "yellow" );
	}

	public ColorPrint blue() {
		return color( "blue" );
	}

	public ColorPrint magenta() {
		return color( "magenta" );
	}

	public ColorPrint cyan() {
		return color( "cyan" );
	}

	public ColorPrint white() {
		return color( "white" );
	}

	public ColorPrint brightBlack() {
		return color( "brightBlack" );
	}

	public ColorPrint brightRed() {
		return color( "brightRed" );
	}

	public ColorPrint brightGreen() {
		return color( "brightGreen" );
	}

	public ColorPrint brightYellow() {
		return color( "brightYellow" );
	}

	public ColorPrint brightBlue() {
		return color( "brightBlue" );
	}

	public ColorPrint brightMagenta() {
		return color( "brightMagenta" );
	}

	public ColorPrint brightCyan() {
		return color( "brightCyan" );
	}

	public ColorPrint brightWhite() {
		return color( "brightWhite" );
	}

	// Output methods
	public void print( String text ) {
		System.out.print( ansiCodes.toString() + text + "\033[0m" );
	}

	public void println( String text ) {
		System.out.println( ansiCodes.toString() + text + "\033[0m" );
	}

	public void printf( String format, Object... args ) {
		System.out.printf( ansiCodes.toString() + format + "\033[0m", args );
	}

	// Helper method to map color names to ANSI codes
	private String getColorCode( String colorName, boolean background ) {
		String	prefix			= background ? "\033[4" : "\033[3";
		String	brightPrefix	= background ? "\033[10" : "\033[9";

		return switch ( colorName.toLowerCase() ) {
			case "black" -> prefix + "0m";
			case "red" -> prefix + "1m";
			case "green" -> prefix + "2m";
			case "yellow" -> prefix + "3m";
			case "blue" -> prefix + "4m";
			case "magenta" -> prefix + "5m";
			case "cyan" -> prefix + "6m";
			case "white" -> prefix + "7m";
			case "brightblack", "bright_black" -> brightPrefix + "0m";
			case "brightred", "bright_red" -> brightPrefix + "1m";
			case "brightgreen", "bright_green" -> brightPrefix + "2m";
			case "brightyellow", "bright_yellow" -> brightPrefix + "3m";
			case "brightblue", "bright_blue" -> brightPrefix + "4m";
			case "brightmagenta", "bright_magenta" -> brightPrefix + "5m";
			case "brightcyan", "bright_cyan" -> brightPrefix + "6m";
			case "brightwhite", "bright_white" -> brightPrefix + "7m";
			default -> ""; // Unknown color, no code
		};
	}

	// Static factory methods for fluent API
	public static ColorPrint withColor( String colorName ) {
		return new ColorPrint().color( colorName );
	}

	public static ColorPrint withColor( int ansiCode ) {
		return new ColorPrint().color( ansiCode );
	}

	public static ColorPrint style() {
		return new ColorPrint();
	}

	// Static color factory methods with unique names to avoid conflicts
	public static ColorPrint redText() {
		return new ColorPrint().red();
	}

	public static ColorPrint greenText() {
		return new ColorPrint().green();
	}

	public static ColorPrint blueText() {
		return new ColorPrint().blue();
	}

	public static ColorPrint yellowText() {
		return new ColorPrint().yellow();
	}

	public static ColorPrint magentaText() {
		return new ColorPrint().magenta();
	}

	public static ColorPrint cyanText() {
		return new ColorPrint().cyan();
	}

	public static ColorPrint whiteText() {
		return new ColorPrint().white();
	}

	public static ColorPrint blackText() {
		return new ColorPrint().black();
	}
}