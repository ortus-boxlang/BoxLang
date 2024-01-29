<cfimport prefix="java" name="java.lang.String">
<cfcomponent output="false" >
	<cfproperty name="id" type="numeric" />

	<cfset variables.instance = {} />

	<cffunction name="init" access="public" returntype="any" output="false">
	</cffunction>

	<cffunction name="foo" access="public">
		<cfreturn "bar">
	</cffunction>

</cfcomponent>