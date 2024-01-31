<!--- array template --->
<cfoutput>
	<table border='1' cellpadding='5' cellspacing='0'>
		<tr><th colspan="2">Array: #var.len()# items</th></tr>
		<cfscript>
			for ( i = 1; i <= var.len(); i++ ) {
				```
					<tr><td>#i#</td><td>
					<cfset dump(var[i])>
					</td></tr>
				```
			}
		</cfscript>
	</table>
</cfoutput>