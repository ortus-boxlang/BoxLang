<cfoutput>
<cfset x = "hello">
<cfset count = 10>
<cfloop from="1" to="#count#" index="i">
<cfif x EQ "hello">
<cfset y = x & " world">
<p>Iteration #i#: #y#</p>
<cfelseif x EQ "goodbye">
<cfset y = "bye">
<p>Saying #y#</p>
<cfelse>
<cfset y = "unknown">
<p>Unknown: #y#</p>
</cfif>
</cfloop>
<cfswitch expression="#x#">
<cfcase value="hello,hi" delimiters=",">
<cfset greeting = "informal">
</cfcase>
<cfcase value="greetings">
<cfset greeting = "formal">
</cfcase>
<cfdefaultcase>
<cfset greeting = "unknown">
</cfdefaultcase>
</cfswitch>
<cfdump var="#variables#" label="Debug Output" expand="true" output="browser">
<cftry>
<cfset result = doSomething( arg1="value1", arg2="value2" )>
<cfcatch type="any">
<cfset errorMsg = cfcatch.message>
<p>Error: #errorMsg#</p>
</cfcatch>
</cftry>
</cfoutput>
