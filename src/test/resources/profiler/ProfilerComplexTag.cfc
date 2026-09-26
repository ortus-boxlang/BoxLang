<!--- Profiler disk-file CLASS test in CFML tag form: static-ish pseudo-constructor
      body, member method, and property default — each tracked under THIS key.
      CFTEMPLATE class = <cfcomponent> with <cfproperty> + <cffunction> --->
<cfcomponent>

	<cfproperty name="threshold" default="40">

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
