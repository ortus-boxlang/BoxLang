

<!--- Recursive Output --->
<cffunction name="genSuiteReport" output="false">
	<cfsavecontent variable="local.report">
		<!--- Do we have nested suites --->
        <cfif arrayLen( x )>
            <cfloop array="#x#" index="local.nestedSuite">
            </cfloop>
        </cfif>
	</cfsavecontent>
</cffunction>
