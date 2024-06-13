<cfoutput>
	<cfquery name="myQry" datasource="myDSN">
		SELECT *
		FROM Users
		WHERE UserID = <cfqueryparam value="#URL.UserID#" cfsqltype="cf_sql_integer">
			AND active = #url.isActive#
		ORDER BY name #sortBy#
	</cfquery>
</cfoutput>