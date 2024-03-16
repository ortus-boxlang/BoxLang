<!--- array template --->
<cfoutput>
	<table border='1' cellpadding='3' cellspacing='0' title="#encodeForHTML( posInCode )#">
		<tr><th colspan="2">Array: #var.len()# items</th></tr>
		<cfscript>
			for ( i = 1; i <= var.len(); i++ ) {
				```
					<tr><td valign="top" onclick="this.nextElementSibling.style.display = this.nextElementSibling.style.display === 'none' ? 'block' : 'none'">#i#</td><td>
					<cfset dump(var[i])>
					</td></tr>
				```
			}
		</cfscript>
	</table>
</cfoutput>