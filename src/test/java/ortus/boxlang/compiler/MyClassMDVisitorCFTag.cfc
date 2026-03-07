<cfcomponent singleton gavin="pickin" inject foo="bar">

    <cffunction name="foo">
        <cfargument name="bar" type="string" required="false" default="#5+5#" />      
    </cffunction>


 </cfcomponent>