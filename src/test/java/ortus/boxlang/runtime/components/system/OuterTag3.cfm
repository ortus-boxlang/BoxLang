<cfoutput>
	<cfif thisTag.executionMode == "end">
		<cfset caller.result = thisTag.myData >
	</cfif>
</cfoutput>