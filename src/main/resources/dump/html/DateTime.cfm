<!--- DateTime template --->
<cfoutput>
	<div title="#encodeForHTML( posInCode )#">Datetime: #encodeForHTML( DateTimeFormat(var, "full") )#</div>	
</cfoutput>