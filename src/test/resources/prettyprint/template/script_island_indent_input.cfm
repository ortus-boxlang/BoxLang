<cfscript>
	local.maxTop = 10;
	try {
		structKeyExists( session, "x" );
	} catch (any e) {
		local.sessionScopeExists = false;
	}
</cfscript>
