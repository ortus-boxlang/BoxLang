<!--- table of class data --->
<cfoutput>
	<table border='1' cellpadding='3' cellspacing='0' title="#encodeForHTML( posInCode )#">
	<tr><th colspan="2"><b>#encodeForHTML( var.getClass().getSimpleName() )#</b></th></tr>
	<tr><td>Class</td><td>#encodeForHTML( var.getClass().getName() )#</td></tr>
	<tr><td>Fields</td><td>
		<cfset fields = var.getClass().getDeclaredFields()>
			<table border='1' cellpadding='3' cellspacing='0'>
			<tr><th><b>Name</b></th><th><b>Signature</b></th></tr>
				<cfscript>		
					for( field in fields ) {
						```
							<tr><td>#encodeForHTML( field.getName() )#</td><td>#encodeForHTML( field.toString() )#</td></tr>
						```
					}
				</cfscript>
			</table>		
	</td></tr>
	<tr><td>Constructors</td><td>
		<cfset constructors = var.getClass().getDeclaredConstructors()>
		<table border='1' cellpadding='3' cellspacing='0'>
		<tr><th><b>Name</b></th><th><b>Signature</b></th></tr>	
			<cfscript>		
				for( constructor in constructors ) {
					```
						<tr><td>#encodeForHTML( constructor.getName() )#</td><td>#encodeForHTML( constructor.toString() )#</td></tr>
					```
					}
			</cfscript>
		</table>
	</td></tr>

	<tr><td>Methods</td><td>
		<cfset methods = var.getClass().getDeclaredMethods()>
		<table border='1' cellpadding='3' cellspacing='0'>
		<tr><th><b>Name</b></th><th><b>Signature</b></th></tr>
			<cfscript>		
				for( method in methods ) {
					```
						<tr><td>#encodeForHTML( method.getName() )#</td><td>#encodeForHTML( method.toString() )#</td></tr>
					```
				}
			</cfscript>
		</table>
	</td></tr>

	</table>
</cfoutput>