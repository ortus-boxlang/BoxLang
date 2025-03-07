/**
 * Copyright Since 2005 TestBox Framework by Luis Majano and Ortus Solutions, Corp
 * www.ortussolutions.com
 * ---
 * A simple HTML reporter
 */
component {

	/**
	 * Get the name of the reporter
	 */
	function getName(){
		return "Simple";
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
	any function runReport(){
		if ( true ) {
			// content type
			// getPageContextResponse().setContentType( "text/html" );
		}

		// bundle stats
		variables.bundleStats = "";

		// prepare base links
		// variables.baseURL = "?";
		// if ( structKeyExists( url, "method" ) ) {
		// 	variables.baseURL &= "method=#urlEncodedFormat( url.method )#";
		// }
		// if ( structKeyExists( url, "output" ) ) {
		// 	variables.baseURL &= "output=#urlEncodedFormat( url.output )#";
		// }

		// prepare incoming params
		// prepareIncomingParams();

		// prepare the report
		savecontent variable="local.report" {
			include "template.cfm";
		}

		return local.report;
	}

}
