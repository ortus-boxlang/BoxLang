<cfset md = getMetaData( var )>
<cfoutput>
	<table border='1' cellpadding='5' cellspacing='0' title="#encodeForHTML( posInCode )#">
		<tr><th colspan="2">
			<b>Box Class: #encodeForHTML( md.name )#</b><br>
			#encodeForHTML( md.hint )#
		</th></tr>
		<tr><td>Extends</td><td>#encodeForHTML( md.extends ?: '' )#</td></tr>
		<tr><td>Properties</td><td>
			<table border='1' cellpadding='5' cellspacing='0'>
				<tr><th>Name</th><th>Value</th></tr>
				<cfset variablesScope = var.$bx.getVariablesScope()>
				<cfloop array="#md.properties#" index="prop">
					<tr><td>#encodeForHTML( prop.name )#</td><td><cfdump var="#(variablesScope[ prop.name ] ?: null)#"></td></tr>
				</cfloop>
			</table>
		</td></tr>
		<!--- TODO: Functions --->
	</table>
</cfoutput>