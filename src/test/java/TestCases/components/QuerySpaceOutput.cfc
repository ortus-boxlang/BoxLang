<cfcomponent singleton displayname="test" output="true">

	<cffunction name="getQuery" access="public" output="true" returntype="query">
		<cfargument name="orderby" type="string" required="false" />
		<cfargument name="sortDirection" type="string" required="false" default="asc" />

		<cfset tbllock = queryNew("lock_name", "integer")>
		<cfquery name="qList" dbtype="query">
			SELECT * FROM tbllock
			WHERE 0=0
			ORDER BY #arguments.orderby# #arguments.sortDirection#
		</cfquery>

		<cfreturn qList />
	</cffunction>

</cfcomponent>