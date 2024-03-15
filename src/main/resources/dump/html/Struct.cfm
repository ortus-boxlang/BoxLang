<!--- Struct template --->
<cfoutput>
	<table border='1' cellpadding='5' cellspacing='0' title="#posInCode#">
		<!--- TODO: Special handling of CGI scope to show all keys --->
		<cfif var instanceof "ortus.boxlang.runtime.scopes.IScope">
			<tr><th colspan="2">#encodeForHTML( var.getName() )# Scope: #var.len()# items</th></tr>
		<cfelse>
			<tr><th colspan="2">Struct: #var.len()# items</th></tr>
		</cfif>
		<cfscript>
			for ( key in var ) {
				```
				<cfif NOT isCustomFunction( var[key] ) >
					<tr><td>#encodeForHTML( key )#</td><td>
					<cfset dump(var[key])>
					</td></tr>
				</cfif>
				```
			}
		</cfscript>
	</table>
</cfoutput>