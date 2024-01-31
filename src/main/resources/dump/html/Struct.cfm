<!--- Struct template --->
<cfoutput>
	<table border='1' cellpadding='5' cellspacing='0'>
		<tr><th colspan="2">Struct: #var.len()# items</th></tr>
		<cfscript>
			for ( key in var ) {
				```
					<tr><td>#key#</td><td>
					<cfset dump(var[key])>
					</td></tr>
				```
			}
		</cfscript>
	</table>
</cfoutput>