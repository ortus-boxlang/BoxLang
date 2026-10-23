<cfcomponent>

	<cfscript>
		static {
			static.scoped = "brad";
			unscoped = "wood";
			static foo = 9000;
		}
	</cfscript>

	<cfset variables.instanceVar = "instance">

	<cffunction name="getScoped">
		<cfreturn static.scoped>
	</cffunction>

	<cffunction name="getFoo">
		<cfreturn static.foo>
	</cffunction>

	<cffunction name="getInstanceVar">
		<cfreturn variables.instanceVar>
	</cffunction>

</cfcomponent>
