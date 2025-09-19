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
 * \n * ColorPrint.color( "red" ).bold().print( "Error: " );\n * ColorPrint.color( "green" ).println( "Success!" );\n * ColorPrint.color( 196 ).background( "yellow" ).italic().print( "Custom colors" );\n * ColorPrint.red().bold().println( "Bold red text" );\n *
 * </pre>
 *
 * "
 */
public class ColorPrint {

	/**
	 * ----------------------------------------------------------------------------
	 * Constants
	 * ----------------------------------------------------------------------------
	 */

	/**
	 * ----------------------------------------------------------------------------
	 * Instance Props
	 * ----------------------------------------------------------------------------
	 */

	private StringBuilder ansiCodes = new StringBuilder();

	/**
	 * ----------------------------------------------------------------------------
	 * Constructors
	 * ----------------------------------------------------------------------------
	 */

	/**
	 * Private constructor - use static factory methods
	 */
	private ColorPrint() {
	}

	/**
	 * ----------------------------------------------------------------------------
	 * Common Colors Entry Point
	 * ----------------------------------------------------------------------------
	 */

	public static ColorPrint red() {
		return ColorPrint.color( "red" );
	}

	public static ColorPrint green() {
		return ColorPrint.color( "green" );
	}

	public static ColorPrint blue() {
		return ColorPrint.color( "blue" );
	}

	public static ColorPrint yellow() {
		return ColorPrint.color( "yellow" );
	}

	public static ColorPrint magenta() {
		return ColorPrint.color( "magenta" );
	}

	public static ColorPrint cyan() {
		return ColorPrint.color( "cyan" );
	}

	public static ColorPrint white() {
		return ColorPrint.color( "white" );
	}

	public static ColorPrint black() {
		return ColorPrint.color( "black" );
	}

	// Bright colors

	public static ColorPrint brightRed() {
		return ColorPrint.color( "brightRed" );
	}

	public static ColorPrint brightGreen() {
		return ColorPrint.color( "brightGreen" );
	}

	public static ColorPrint brightBlue() {
		return ColorPrint.color( "brightBlue" );
	}

	public static ColorPrint brightYellow() {
		return ColorPrint.color( "brightYellow" );
	}

	public static ColorPrint brightMagenta() {
		return ColorPrint.color( "brightMagenta" );
	}

	public static ColorPrint brightCyan() {
		return ColorPrint.color( "brightCyan" );
	}

	public static ColorPrint brightWhite() {
		return ColorPrint.color( "brightWhite" );
	}

	public static ColorPrint brightBlack() {
		return ColorPrint.color( "brightBlack" );
	}

	/**
	 * ----------------------------------------------------------------------------
	 * Generic static methods
	 * ----------------------------------------------------------------------------
	 */

	/**
	 * Set text color by name (instance method for chaining).
	 *
	 * @param colorName The name of the color
	 *
	 * @return A ColorPrint instance for method chaining
	 */
	public static ColorPrint color( String colorName ) {
		return new ColorPrint().setColor( colorName );
	}

	/**
	 * Set text color by ANSI code (0-255).
	 *
	 * @param ansiCode The ANSI color code (0-255)
	 *
	 * @return A ColorPrint instance for method chaining
	 */
	public static ColorPrint color( int ansiCode ) {
		return new ColorPrint().setColor( ansiCode );
	}

	/**
	 * Set background color by name (instance method for chaining).
	 *
	 * @param colorName The name of the color
	 *
	 * @return A ColorPrint instance for method chaining
	 */
	public static ColorPrint background( String colorName ) {
		return new ColorPrint().setBackground( colorName );
	}

	/**
	 * Set background color by ANSI code (0-255).
	 *
	 * @param ansiCode The ANSI color code (0-255)
	 *
	 * @return A ColorPrint instance for method chaining
	 */
	public static ColorPrint background( int ansiCode ) {
		return new ColorPrint().setBackground( ansiCode );
	}

	/**
	 * Create a ColorPrint instance for style-only operations.
	 *
	 * @return ColorPrint instance for method chaining
	 */
	public static ColorPrint style( String style ) {
		return new ColorPrint().setStyle( style );
	}

	/**
	 * ----------------------------------------------------------------------------
	 * Common Styles
	 * ----------------------------------------------------------------------------
	 */

	public static ColorPrint bold() {
		return ColorPrint.style( "bold" );
	}

	public static ColorPrint italic() {
		return ColorPrint.style( "italic" );
	}

	public static ColorPrint underline() {
		return ColorPrint.style( "underline" );
	}

	public static ColorPrint dim() {
		return ColorPrint.style( "dim" );
	}

	public static ColorPrint strikethrough() {
		return ColorPrint.style( "strikethrough" );
	}

	/**
	 * ----------------------------------------------------------------------------
	 * Instance Terminal output methods
	 * ----------------------------------------------------------------------------
	 */

	/**
	 * Prints text with the accumulated ANSI styles.
	 *
	 * @param text The text to print
	 */
	public void print( String text ) {
		System.out.print( ansiCodes.toString() + text + MiniConsole.CODES.RESET.code() );
	}

	/**
	 * Prints text with the accumulated ANSI styles and a newline.
	 *
	 * @param text The text to print
	 */
	public void println( String text ) {
		System.out.println( ansiCodes.toString() + text + MiniConsole.CODES.RESET.code() );
	}

	/**
	 * Prints formatted text with the accumulated ANSI styles.
	 *
	 * @param format The format string
	 * @param args   Arguments referenced by the format specifiers in the format string
	 */
	public void printf( String format, Object... args ) {
		System.out.printf( ansiCodes.toString() + format + MiniConsole.CODES.RESET.code(), args );
	}

	/**
	 * ----------------------------------------------------------------------------
	 * Private Helpers
	 * ----------------------------------------------------------------------------
	 */

	/**
	 * Helper method to set style by name (used by static methods).
	 *
	 * @param style The name of the style (e.g., "bold", "italic", etc.)
	 *
	 * @return this for method chaining
	 */
	private ColorPrint setStyle( String style ) {
		switch ( style.toLowerCase() ) {
			case "bold" :
				ansiCodes.append( MiniConsole.CODES.BOLD.code() );
				break;
			case "italic" :
				ansiCodes.append( MiniConsole.CODES.ITALIC.code() );
				break;
			case "underline" :
				ansiCodes.append( MiniConsole.CODES.UNDERLINE.code() );
				break;
			case "dim" :
				ansiCodes.append( MiniConsole.CODES.DIM.code() );
				break;
			case "strikethrough" :
				ansiCodes.append( MiniConsole.CODES.STRIKETHROUGH.code() );
				break;
			default :
				// Unknown style, do nothing
				break;
		}
		return this;
	}

	/**
	 * Helper method to set background color by name (used by static methods).
	 *
	 * @param colorName The name of the color
	 *
	 * @return this for method chaining
	 */
	private ColorPrint setBackground( String colorName ) {
		ansiCodes.append( getColorCode( colorName, true ) );
		return this;
	}

	/**
	 * Helper method to set background color by ANSI code (used by static methods).
	 *
	 * @param ansiCode The ANSI color code (0-255)
	 *
	 * @return this for method chaining
	 */
	private ColorPrint setBackground( int ansiCode ) {
		if ( ansiCode >= 0 && ansiCode <= 255 ) {
			ansiCodes.append( MiniConsole.CODES.BACKGROUND.code() ).append( ansiCode ).append( "m" );
		}
		return this;
	}

	/**
	 * Helper method to set color by name (used by static methods).
	 *
	 * @param colorName The name of the color
	 *
	 * @return this for method chaining
	 */
	private ColorPrint setColor( String colorName ) {
		ansiCodes.append( getColorCode( colorName, false ) );
		return this;
	}

	/**
	 * Helper method to set color by ANSI code (used by static methods).
	 *
	 * @param ansiCode The ANSI color code (0-255)
	 *
	 * @return this for method chaining
	 */
	private ColorPrint setColor( int ansiCode ) {
		if ( ansiCode >= 0 && ansiCode <= 255 ) {
			ansiCodes.append( MiniConsole.CODES.FOREGROUND.code() ).append( ansiCode ).append( "m" );
		}
		return this;
	}

	/**
	 * Helper method to map color names to ANSI codes
	 *
	 * @param colorName  A name of the color (e.g., "red", "green", "brightRed", etc.)
	 * @param background Whether the color is for the background
	 *
	 * @return The ANSI code for the specified color
	 */
	private String getColorCode( String colorName, boolean background ) {
		return switch ( colorName.toLowerCase() ) {
			case "black" -> background ? MiniConsole.CODES.BG_BLACK.code() : MiniConsole.CODES.BLACK.code();
			case "red" -> background ? MiniConsole.CODES.BG_RED.code() : MiniConsole.CODES.RED.code();
			case "green" -> background ? MiniConsole.CODES.BG_GREEN.code() : MiniConsole.CODES.GREEN.code();
			case "yellow" -> background ? MiniConsole.CODES.BG_YELLOW.code() : MiniConsole.CODES.YELLOW.code();
			case "blue" -> background ? MiniConsole.CODES.BG_BLUE.code() : MiniConsole.CODES.BLUE.code();
			case "magenta" -> background ? MiniConsole.CODES.BG_MAGENTA.code() : MiniConsole.CODES.MAGENTA.code();
			case "cyan" -> background ? MiniConsole.CODES.BG_CYAN.code() : MiniConsole.CODES.CYAN.code();
			case "white" -> background ? MiniConsole.CODES.BG_WHITE.code() : MiniConsole.CODES.WHITE.code();
			case "brightblack", "bright_black" -> background ? MiniConsole.CODES.BG_BRIGHT_BLACK.code() : MiniConsole.CODES.BRIGHT_BLACK.code();
			case "brightred", "bright_red" -> background ? MiniConsole.CODES.BG_BRIGHT_RED.code() : MiniConsole.CODES.BRIGHT_RED.code();
			case "brightgreen", "bright_green" -> background ? MiniConsole.CODES.BG_BRIGHT_GREEN.code() : MiniConsole.CODES.BRIGHT_GREEN.code();
			case "brightyellow", "bright_yellow" -> background ? MiniConsole.CODES.BG_BRIGHT_YELLOW.code() : MiniConsole.CODES.BRIGHT_YELLOW.code();
			case "brightblue", "bright_blue" -> background ? MiniConsole.CODES.BG_BRIGHT_BLUE.code() : MiniConsole.CODES.BRIGHT_BLUE.code();
			case "brightmagenta", "bright_magenta" -> background ? MiniConsole.CODES.BG_BRIGHT_MAGENTA.code() : MiniConsole.CODES.BRIGHT_MAGENTA.code();
			case "brightcyan", "bright_cyan" -> background ? MiniConsole.CODES.BG_BRIGHT_CYAN.code() : MiniConsole.CODES.BRIGHT_CYAN.code();
			case "brightwhite", "bright_white" -> background ? MiniConsole.CODES.BG_BRIGHT_WHITE.code() : MiniConsole.CODES.BRIGHT_WHITE.code();
			// Unknown color, no code
			default -> "";
		};
	}
}