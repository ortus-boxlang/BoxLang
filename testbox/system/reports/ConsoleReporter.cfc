/**
 * Copyright Since 2005 TestBox Framework by Luis Majano and Ortus Solutions, Corp
 * www.ortussolutions.com
 * ---
 * A text reporter that emits to the console via java System out.
 */
component extends="TextReporter" {

	variables.COLORS = {
		reset     : chr( 27 ) & "[0m",
		bold      : chr( 27 ) & "[1m",
		dim       : chr( 27 ) & "[2m",
		underline : chr( 27 ) & "[4m",
		blink     : chr( 27 ) & "[5m",
		reverse   : chr( 27 ) & "[7m",
		hidden    : chr( 27 ) & "[8m",
		black     : chr( 27 ) & "[30m",
		red       : chr( 27 ) & "[31m",
		green     : chr( 27 ) & "[32m",
		yellow    : chr( 27 ) & "[33m",
		blue      : chr( 27 ) & "[34m",
		magenta   : chr( 27 ) & "[35m",
		cyan      : chr( 27 ) & "[36m",
		white     : chr( 27 ) & "[37m",
		bgBlack   : chr( 27 ) & "[40m",
		bgRed     : chr( 27 ) & "[41m",
		bgGreen   : chr( 27 ) & "[42m",
		bgYellow  : chr( 27 ) & "[43m",
		bgBlue    : chr( 27 ) & "[44m",
		bgMagenta : chr( 27 ) & "[45m",
		bgCyan    : chr( 27 ) & "[46m",
		bgWhite   : chr( 27 ) & "[47m"
	};

	function init(){
		variables.out = createObject( "Java", "java.lang.System" ).out;
		return this;
	}

	/**
	 * Get the name of the reporter
	 */
	function getName(){
		return "Console";
	}

	/**
	 * Do the reporting thing here using the incoming test results
	 * The report should return back in whatever format they desire and should set any
	 * Specific browser types if needed.
	 *
	 * @results    The instance of the TestBox TestResult object to build a report on
	 * @testbox    The TestBox core object
	 * @options    A structure of options this reporter needs to build the report with
	 * @justReturn Boolean flag that if set just returns the content with no content type and buffer reset
	 */
	any function runReport(
		required testbox.system.TestResult results,
		required testbox.system.TestBox testbox,
		struct options     = {},
		boolean justReturn = false
	){
		if ( !arguments.justReturn ) {
			// content type
			getPageContextResponse().setContentType( "text/plain" );
		}

		// bundle stats
		variables.bundleStats = arguments.results.getBundleStats();
		// prepare incoming params
		prepareIncomingParams();

		// prepare the report
		savecontent variable="local.report" {
			include "assets/console.cfm";
		}

		// send to console
		variables.out.printLn(
			reReplace(
				trim( local.report ),
				"[\r\n]+",
				chr( 10 ),
				"all"
			)
		);

		return "";
	}

	/**
	 * Return a styled text
	 *
	 * @style The style to apply, more than one can be applied by using +, ie: bold+red
	 * @text  The text to style
	 *
	 * @return The styled text
	 */
	function color( required style, required text ){
		var styleArray  = listToArray( arguments.style, "+" );
		var styleString = "";

		for ( var thisStyle in styleArray ) {
			styleString &= variables.COLORS[ thisStyle ];
		}

		return "#variables.COLORS.reset##styleString##arguments.text##variables.COLORS.reset#";
	}

	function getStatusIndicator( required status ){
		if ( arguments.status == "error" ) {
			return "💥";
		} else if ( arguments.status == "failed" ) {
			return "❌";
		} else if ( arguments.status == "skipped" ) {
			return "⏭️ ";
		} else {
			return "✅";
		}
	}

	/**
	 * Returns a line by status, error = magenta+bold, failed = red+bold, skipped = dim+white, passed = green
	 */
	function printByStatus( required status, required text ){
		var thisStyle = "green";
		if ( arguments.status == "error" ) {
			thisStyle = "magenta+bold";
		} else if ( arguments.status == "failed" ) {
			thisStyle = "red+bold";
		} else if ( arguments.status == "skipped" ) {
			thisStyle = "dim+white";
		}
		return color( thisStyle, arguments.text );
	}

	function getBundleIndicator( required bundle ){
		var thisStatus      = "pass";
		var thisStatusStyle = "green";

		if ( arguments.bundle.totalFail > 0 || arguments.bundle.totalError > 0 ) {
			thisStatus      = "error";
			thisStatusStyle = "bold+red";
		}
		if ( arguments.bundle.totalSkipped == arguments.bundle.totalSpecs ) {
			thisStatus      = "skipped";
			thisStatusStyle = "dim+white";
		}

		return getStatusIndicator( thisStatus ) & " " & color( thisStatusStyle, arguments.bundle.name );
	}

}
