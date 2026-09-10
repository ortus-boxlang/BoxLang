<cfparam name="variables.currentValue" default="">
<cfif len( variables.currentValue )>
	<cfprocparam type="in" dbvarname=":p_value" value="#variables.currentValue#" cfsqltype="CF_SQL_VARCHAR">
<cfelse>
	<cfprocparam type="in" dbvarname=":p_value" null="Yes" cfsqltype="CF_SQL_VARCHAR">
</cfif>