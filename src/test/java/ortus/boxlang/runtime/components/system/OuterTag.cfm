<cfoutput>
	<cfif thisTag.executionMode == "end">
		<cfset caller.result = thisTag.AssocAttribs >
	</cfif>
</cfoutput>