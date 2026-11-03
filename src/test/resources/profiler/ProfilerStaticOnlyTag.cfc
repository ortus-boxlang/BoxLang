<cfcomponent extends="ProfilerSuper">

	<cfproperty name="threshold" default="#complexSeed#">

	 <cfscript>
        static {
            myStaticVar = "initialized";
        }
    </cfscript>

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

	<cffunction modifier="static" name="staticMethod">
		<cfreturn 42>
	</cffunction>

	<cffunction modifier="static" name="uncalledStaticMethod">
		<cfreturn 9001>
	</cffunction>

</cfcomponent>