<cfscript>
function greet( required string name, string greeting = "Hello" ){
	return greeting & ", " & name & "!";
}
variables.items = [
	"apple",
	"banana",
	"cherry",
	"date",
	"elderberry"
];
</cfscript>
<cfoutput>
	<cfset message = greet( name = "World" )>
	<p>#message#</p>
	<cfloop array="#variables.items#" item="fruit">
		<cfif len( fruit ) GT 5>
			<p class="long">#fruit# is a long name</p>
		<cfelse>
			<p>#fruit#</p>
		</cfif>
	</cfloop>
</cfoutput>
