<cfimport prefix="java" name="ortus.boxlang.runtime.types.exceptions.ExceptionUtil">
<!--- Throwable template --->
<cfoutput>
	<cfset isBXError = var instanceof "ortus.boxlang.runtime.types.exceptions.BoxLangException">
	<table border='1' cellpadding='3' cellspacing='0' title="#posInCode#">
		<cfif isBXError>
			<tr><th colspan="2">Error: #encodeForHTML( var.getType() )#</th></tr>
		<cfelse>
			<tr><th colspan="2">Error: #var.getClass().getName()#</th></tr>
		</cfif>
		<tr><td>Message</td><td>#encodeForHTML( var.getMessage() )#</td></tr>
		<cfif isBXError>
			<tr><td>Detail</td><td>#encodeForHTML( var.getDetail() )#</td></tr>
			<tr><td>Tag Congtext</td><td>
				<cfdump var="#var.getTagContext()#">
			</td></tr>
			<!--- TODO: Details from other exception subclasses --->
		</cfif>
		<cfif var.getCause() != null >
			<tr><td>Cause</td><td>
				<cfdump var="#var.getCause()#">
			</td></tr>
		</cfif>
		<tr><td>StackTrace</td><td>
			<pre>#encodeForHTML( ExceptionUtil.getStackTraceAsString( var ) )#</pre>
		</td></tr>
	</table>
</cfoutput>