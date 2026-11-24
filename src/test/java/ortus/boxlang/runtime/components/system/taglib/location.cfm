<cfif thisTag.executionMode == "end">
	<cfset caller.result = { baseTags : caller.baseTags, inputs : thisTag.locationinput }>
</cfif>
