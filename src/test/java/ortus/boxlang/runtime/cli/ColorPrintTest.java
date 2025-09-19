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
package ortus.boxlang.runtime.cli;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the fluent ColorPrint API
 */
public class ColorPrintTest {

	@Test
	@DisplayName( "Test fluent color API examples" )
	void testFluentColorAPI() {
		System.out.println( "\n=== ColorPrint Fluent API Demo ===" );

		// Named colors
		ColorPrint.color( "red" ).bold().println( "Bold Red Error Message" );
		ColorPrint.color( "green" ).println( "Green Success Message" );
		ColorPrint.color( "yellow" ).italic().println( "Yellow Italic Warning" );
		ColorPrint.color( "blue" ).underline().println( "Blue Underlined Info" );

		// ANSI color codes (0-255)
		ColorPrint.color( 196 ).bold().println( "Bright Red (196) with Bold" );
		ColorPrint.color( 46 ).println( "Bright Green (46)" );
		ColorPrint.color( 33 ).italic().println( "Blue (33) with Italic" );

		// Background colors
		ColorPrint.color( "white" ).background( "red" ).bold().println( "White on Red Background" );
		ColorPrint.color( "black" ).background( "yellow" ).println( "Black on Yellow Background" );

		// Style factory methods
		ColorPrint.red().bold().println( "Red Bold Text" );
		ColorPrint.green().italic().println( "Green Italic Text" );
		ColorPrint.blue().underline().println( "Blue Underlined Text" );

		// Chaining multiple styles
		ColorPrint
		    .color( "magenta" )
		    .bold()
		    .italic()
		    .underline()
		    .println( "Magenta Bold Italic Underlined" );

		System.out.println( "=== Demo Complete ===" );
	}

	@Test
	@DisplayName( "Test color name mapping" )
	void testColorNameMapping() {
		System.out.println( "\n=== Color Name Mapping Test ===" );

		// Test all basic colors
		ColorPrint.color( "black" ).print( "Black " );
		ColorPrint.color( "red" ).print( "Red " );
		ColorPrint.color( "green" ).print( "Green " );
		ColorPrint.color( "yellow" ).print( "Yellow " );
		ColorPrint.color( "blue" ).print( "Blue " );
		ColorPrint.color( "magenta" ).print( "Magenta " );
		ColorPrint.color( "cyan" ).print( "Cyan " );
		ColorPrint.color( "white" ).println( "White" );

		// Test bright colors
		ColorPrint.color( "brightRed" ).print( "BrightRed " );
		ColorPrint.color( "brightGreen" ).print( "BrightGreen " );
		ColorPrint.color( "brightBlue" ).print( "BrightBlue " );
		ColorPrint.color( "brightYellow" ).println( "BrightYellow" );
	}

	@Test
	@DisplayName( "Test style combinations" )
	void testStyleCombinations() {
		System.out.println( "\n=== Style Combinations Test ===" );

		// Various style combinations
		ColorPrint.bold().println( "Bold only" );
		ColorPrint.italic().println( "Italic only" );
		ColorPrint.underline().println( "Underline only" );
		ColorPrint.dim().println( "Dim only" );
		ColorPrint.strikethrough().println( "Strikethrough only" );

		// Combined styles
		ColorPrint.bold().italic().println( "Bold + Italic" );
		ColorPrint.bold().underline().println( "Bold + Underline" );
		ColorPrint.italic().underline().println( "Italic + Underline" );
	}
}