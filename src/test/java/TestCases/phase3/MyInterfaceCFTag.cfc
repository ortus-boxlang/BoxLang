<!---
This is my interface description 
--->
<cfinterface singleton gavin="pickin" inject foo="bar" brad=wood luis>

    <cffunction name="init">
    </cffunction>

    <cffunction  name="foo">
    </cffunction>

    <cffunction access="private" name="bar">
    </cffunction>

    <cffunction name="myDefaultMethod">
        <cfreturn this.foo()>
    </cffunction>

</cfinterface>