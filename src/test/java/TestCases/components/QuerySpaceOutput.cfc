<cfcomponent singleton displayname="test" output="true">

	<cffunction name="getQuery" access="public" output="true" returntype="query">
		<cfargument name="orderby" type="string" required="false" />
		<cfargument name="sortDirection" type="string" required="false" default="asc" />

		<cfset tbllock = queryNew("lock_name,id", "integer,varchar")>
		<cfquery name="qList" dbtype="query">
			SELECT * FROM tbllock
			WHERE 0=0
			ORDER BY 
			<cfif structKeyExists(arguments, "orderby") and len(arguments.orderBy)>
				<cfloop list="#arguments.orderby#" item="item">
					#trim(item)# #arguments.sortOrder#,
				</cfloop>
			</cfif>

			id
		</cfquery>

		<cfreturn qList />
	</cffunction>

</cfcomponent>