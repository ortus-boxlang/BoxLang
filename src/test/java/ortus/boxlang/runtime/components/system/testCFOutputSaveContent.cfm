<cfscript>
	function emitter() {
		writeOutput( "hello world" )
	}
</cfscript>

<cfsavecontent variable="result">
	<cfset emitter()>
</cfsavecontent>