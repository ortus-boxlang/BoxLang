<cfif thisTag.executionMode == "end">
	<cfset thisTag.generatedContent = "(" & trim( thisTag.generatedContent ) & ")">
</cfif>
