<cfcomponent extends="ProfilerSuper">

	<cfproperty name="threshold" default="#complexSeed#">

	<cfset instanceInit = 0>

	<cffunction name="doubleIt">
		<cfargument name="n">
		<cfreturn n * 2>
	</cffunction>

	<cffunction name="member">
		<cfargument name="x">
		<cfset sleep( 100 )>
		<cfreturn x * 3>
	</cffunction>

</cfcomponent>