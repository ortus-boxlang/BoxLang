<cfoutput>
	always
	<cfif thisTag.executionMode == "start">
		My Tag start #attributes.foo# #caller.brad#
		<cfset caller.result = "hey you guys">
	</cfif>
	<cfif thisTag.executionMode == "end">
		#reverse( thisTag.generatedContent )#
		<cfset thisTag.generatedContent = "">
		My Tag End
	</cfif>
</cfoutput>