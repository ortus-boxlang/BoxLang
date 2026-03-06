<cfcomponent output="false" hint="A MockBox awesome Component" implements="tests.resources.NestedInterface">
<cffunction access = "public" returnformat = "wddx" returntype = "any" output = "true" modifier = "" name = "testThisToo" owner = "/Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/tests/resources/NestedInterface.cfc" description = "">
<cfargument required = "true" name = "greeting" type = "any">
<cfargument required = "false" name = "name" type = "any">
</cffunction>
<cffunction access = "public" returnformat = "wddx" returntype = "any" output = "true" modifier = "" name = "testThis" owner = "/Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/tests/resources/MyInterface.cfc" description = "">
<cfargument required = "true" name = "name" type = "any">
<cfargument required = "false" name = "age" type = "any">
</cffunction>
</cfcomponent>