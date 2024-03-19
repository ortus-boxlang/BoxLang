<cfset md = getMetaData( var )>
<cfoutput>
	<table border='1' cellpadding='3' cellspacing='0' title="#encodeForHTML( posInCode )#">
		<tr><th colspan="2">
			<b>Box Class: #encodeForHTML( md.name )#</b><br>
			<cfif find(".", md.hint ) >
				#left(encodeForHTML( md.hint ), find(".", md.hint ) - 1)#
			<cfelse>
				#encodeForHTML( md.hint )#
			</cfif>
		</th></tr>
		<tr><td valign="top" onclick="this.nextElementSibling.style.display = this.nextElementSibling.style.display === 'none' ? 'block' : 'none'">Extends</td><td>
			<cfdump var="#( md.extends ?: '' )#">
		</td></tr>
		<tr><td valign="top" onclick="this.nextElementSibling.style.display = this.nextElementSibling.style.display === 'none' ? 'block' : 'none'">Properties</td><td>
			<table border='1' cellpadding='3' cellspacing='0'>
				<tr><th>Name</th><th>Value</th></tr>
				<cfset variablesScope = var.$bx.getVariablesScope()>
				<cfloop array="#md.properties#" index="prop">
					<tr><td valign="top" onclick="this.nextElementSibling.style.display = this.nextElementSibling.style.display === 'none' ? 'block' : 'none'">#encodeForHTML( prop.name )#</td><td><cfdump var="#(variablesScope[ prop.name ] ?: null)#"></td></tr>
				</cfloop>
			</table>
		</td></tr>
		<!--- TODO: Functions --->
	</table>
</cfoutput>