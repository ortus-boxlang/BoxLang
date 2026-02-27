<cfcomponent>
		<cfscript>
			static function optMap_keepIsPresent( k, v ) {
				return v.isPresent()
			}
	</cfscript>
</cfcomponent>