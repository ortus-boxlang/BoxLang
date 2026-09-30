<cfcomponent extends="ProfilerSuper">

	<cfproperty name="threshold" default="#throw("boom")#">
	<cfproperty name="other" default="#( 40 + 2 )#">

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

</cfcomponent>
