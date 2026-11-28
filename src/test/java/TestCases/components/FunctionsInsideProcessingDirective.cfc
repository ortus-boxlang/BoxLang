<cfcomponent output="false">
<cfprocessingdirective suppresswhitespace="true">
	<cffunction name="find" returntype="string" output="false">
		<cfargument name="wanted" type="string" required="true">
		<cfset var item = "">
		<cfloop list="a,b,c" index="item">
			<cfif item IS arguments.wanted>
				<cfreturn "found-" & item>
			</cfif>
		</cfloop>
		<cfreturn "none">
	</cffunction>
	<cffunction name="firstEven" returntype="numeric" output="false">
		<cfset var i = 0>
		<cfloop from="1" to="9" index="i">
			<cfif i MOD 2 IS 0>
				<cfreturn i>
			</cfif>
		</cfloop>
		<cfreturn 0>
	</cffunction>
</cfprocessingdirective>
</cfcomponent>
