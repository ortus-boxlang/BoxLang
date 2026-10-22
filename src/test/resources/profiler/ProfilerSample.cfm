<!--- Plain statements, one per line --->
<cfset i = 0>
<cfset j = 0>

<!--- A while loop that runs --->
<cfwhile condition="i < 3">
    <cfset i++>
</cfwhile>

<!--- A while loop with a sleep body (timing) --->
<cfset i = 0>
<cfwhile condition="i < 3">
    <cfset sleep( 100 )>
    <cfset i++>
</cfwhile>

<!--- A function declared and invoked --->
<cffunction name="foo">
    <cfargument name="n">
    <cfset x = 1>
    <cfreturn x>
</cffunction>
<cfset result = foo()>

<!--- A function declared but NEVER invoked (body stays missed) --->
<cffunction name="neverCalled">
    <cfset z = 1>
    <cfreturn z>
</cffunction>

<!--- A conditional branch — only one side runs --->
<cfset doubled = 100>
<cfif doubled GT 40>
    <cfset j = 1>
<cfelse>
    <cfset j = 2>
</cfif>

<!--- An if/else-on-one-line — only one side runs --->
<cfif true><cfset now()><cfelse><cfset now()></cfif>

<!--- An if/else-if chain — only the matching branch runs --->
<cfset x = 2>
<cfif x EQ 1>
    <cfset a = 10>
<cfelseif x EQ 2>
    <cfset a = 20>
<cfelseif x EQ 3>
    <cfset a = 30>
<cfelse>
    <cfset a = 99>
</cfif>

<!--- An if/else-if chain where the final else runs --->
<cfset x = 99>
<cfif x EQ 1>
    <cfset a = 10>
<cfelseif x EQ 2>
    <cfset a = 20>
<cfelse>
    <cfset a = 99>
</cfif>

<!--- A switch — only the matching case runs --->
<cfset x = 2>
<cfswitch expression="#x#">
    <cfcase value="1">
    <cfset a = 10>
    </cfcase>
    <cfcase value="2">
    <cfset a = 20>
    </cfcase>
    <cfdefaultcase>
    <cfset a = 30>
    </cfdefaultcase>
</cfswitch>

<!--- A try/catch/finally — only try and finally run --->
<cftry>
    <cfset a = 1>
    <cfcatch>
    <cfset c = 3>
    </cfcatch>
    <cffinally>
    <cfset d = 4>
    </cffinally>
</cftry>

<!--- A try/catch that THROWS — so the catch body actually runs --->
<cftry>
    <cfset a = 1>
    <cfthrow message="boom">
    <cfset b = 2>
    <cfcatch>
    <cfset c = 3>
    </cfcatch>
    <cffinally>
    <cfset d = 4>
    </cffinally>
</cftry>

<!--- A for-in loop --->
<cfset arr = [ 10, 20, 30 ]>
<cfloop array="#arr#" item="item">
    <cfset y = item>
</cfloop>

<!--- A numeric for loop --->
<cfset j = 0>
<cfloop from="0" to="2" index="i">
    <cfset j = j + i>
</cfloop>

<!--- Short-circuit: right side never runs --->
<cfset a = false>
<cfset b = 1>
<cfset x = a && b>

<!--- String interpolation --->
<cfset a = 1>
<cfset x = "val #a# now">

<!--- Unary not, negation, parens --->
<cfset a = false>
<cfset b = 5>
<cfset c = 1>
<cfset d = 2>
<cfset x = !a>
<cfset y = -b>
<cfset z = ( c + d )>

<!--- Ternary --->
<cfset bar = true>
<cfset baz = 1>
<cfset bum = 2>
<cfset x = bar ? baz : bum>

<!--- Text output + interpolation --->
<cfoutput>
foo
#x#
bar
</cfoutput>

<!--- param: default is set when the variable is missing ... (complex default,
      compiled to a deferred closure — GREEN when run, RED when skipped) --->
<cfparam name="paramMissing" default=#now()#>
<!--- ... and is a no-op (default skipped — RED) when the variable already exists. --->
<cfset paramMissing = 1>
<cfparam name="paramMissing" default=#now()#>

<!--- param with a LITERAL default stays fused inline (always covered) --->
<cfparam name="paramLiteral" default="bar">

<cfscript>
	foo = "bar"
	now();
</cfscript>

<cfset i = 0>
<!--- A loop with condition --->
<cfloop condition="i < 3">    
	<cfset i++>
</cfloop>