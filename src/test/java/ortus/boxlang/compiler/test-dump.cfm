<!--- Tag syntax: top should become depth --->
<cfset myVar = "hello world">
<cfdump var="#myVar#" top="3">

<cfscript>
    // Script dump() syntax: top should become depth
    dump( var=myVar, top=3 );

    // writeDump() alias syntax: top should become depth
    writeDump( var=myVar, top=3 );
</cfscript>

