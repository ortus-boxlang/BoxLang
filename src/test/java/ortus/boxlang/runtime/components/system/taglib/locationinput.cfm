<cfif thisTag.executionMode == "start">
	<cfset caller.baseTags = getBaseTagList()>
	<cfassociate basetag="cf_location" datacollection="locationinput">
</cfif>
