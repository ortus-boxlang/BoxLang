<cfinterface displayname="ProfilerInterfaceTag">

	<cffunction name="bump" returntype="numeric">
		<cfreturn 1>
	</cffunction>

	<cffunction name="display" returntype="string">
		<cfset var x = 1 + 2>
		<cfreturn displayname>
	</cffunction>

	<cffunction name="greet" returntype="string">
		<cfset var msg = "hello from tag interface">
		<cfreturn msg>
	</cffunction>

	<cffunction name="greetUnused" returntype="string">
		<cfset var unusedPrefix = "unused">
		<cfreturn "#unusedPrefix#!">
	</cffunction>

	<cffunction name="abstractOnly" returntype="string">
	</cffunction>

</cfinterface>