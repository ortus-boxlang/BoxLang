<cfoutput>
#color( "bold+cyan", "█▓▒▒░░░ " )##color( "bold+green", "TestBox v" & testbox.getVersion() )##color( "bold+cyan", " ░░░▒▒▓█" )#
<!--- Iterate over each bundle tested --->
<cfloop array="#variables.bundleStats#" index="thisBundle">
<!--- Skip if not in the includes list --->
<cfif len( url.testBundles ) and !listFindNoCase( url.testBundles, thisBundle.path )>
<cfcontinue>
</cfif>
#space()#
<!--- Bundle Name --->
#getBundleIndicator( thisBundle )# (#thisBundle.totalDuration# ms)
<!--- Bundle Report --->
[Passed: #thisBundle.totalPass#] [Failed: #thisBundle.totalFail#] [Errors: #thisBundle.totalError#] [Skipped: #thisBundle.totalSkipped#] [Suites/Specs: #thisBundle.totalSuites#/#thisBundle.totalSpecs#]
#space()#<!--- Bundle Exception Output --->
<cfif !isSimpleValue( thisBundle.globalException )>
#color( "red+bold", "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" )#
#color( "red+magenta", "<GLOBAL BUNDLE EXCEPTION>" )#
#color( "red+bold", "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" )#
#space()#
#color( "bold+white", "#thisBundle.globalException.type#:#thisBundle.globalException.message#:#thisBundle.globalException.detail#")#
<cfloop array="#thisBundle.globalException.tagContext#" item="thisContext">
<cfif findNoCase( thisBundle.path, reReplace( thisContext.template, "(/|\\)", ".", "all" ) )>
#color( "red+bold", "#thisContext.template#:#thisContext.line#" )#
#color( "bold+white", "#thisContext.codePrintPlain ?: ""#")#
</cfif>
</cfloop>
#color( "red+bold", "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!" )#
</cfif><!--- Generate Suite Reports --->
<cfloop array="#thisBundle.suiteStats#" index="suiteStats">#genSuiteReport( suiteStats, thisBundle )#</cfloop>
</cfloop>
<!--- Final Stats --->
#color( "bold+white", repeatString( "=", 80 ) )#
#color( "bold+cyan", "Final Stats" )#
#color( "bold+white", repeatString( "=", 80 ) )#
[ ✅ #color( "green", "Passed:" )# #color( "white", results.getTotalPass() )# ]
[ ❌ #color( "red", "Failed:" )# #color( "white", results.getTotalFail() )# ]
[ 💥 #color( "magenta", "Errors:" )# #color( "white", results.getTotalError() )# ]
[ ⏭️  #color( "yellow", "Skipped:" )# #color( "white", results.getTotalSkipped() )# ]
[ ⏱️  #color( "white+dim", "Duration:" )# #color( "white", "#numberFormat( results.getTotalDuration() )# ms" )# ]
[ 📦 #color( "white+dim", "Bundles/Suites/Specs:" )# #color( "white", results.getTotalBundles() & "/" & results.getTotalSuites() & "/" & results.getTotalSpecs() )# ]
[ 🏷️  #color( "white+dim", "Labels:")# #arrayToList( results.getLabels() )#<cfif !arrayLen( results.getLabels() )>None</cfif>]
#color( "bold+white", repeatString( "=", 80 ) )#
<cfif results.getCoverageEnabled()>
#space()#
=================================================================================
Code Coverage
=================================================================================
[Total Coverage: #numberFormat( results.getCoverageData().stats.percTotalCoverage*100, '9.9' )#%]
#space()#
</cfif>
#color( "dim", "TestBox:" )# #space( 1 )# v#testbox.getVersion()#
#color( "dim", "Engine:" )# #space( 2 )# #results.getCFMLEngine()# #results.getCFMLEngineVersion()#
<cffunction name="genSuiteReport" output="false">
	<cfargument name="suiteStats">
	<cfargument name="bundleStats">
	<cfargument name="level" default=0>
	<cfsetting enablecfoutputonly="true">
	<cfset var tabs = repeatString( tab(), arguments.level )>
	<cfset var tabsNext = repeatString( tab(), arguments.level + 1 )>
	<cfsavecontent variable="local.report"><cfoutput><!---

			Suite Name

		--->#tabs##getStatusIndicator( arguments.suiteStats.status )# #printByStatus( arguments.suiteStats.status, arguments.suiteStats.name )# #chr(13)#<!---

			Specs

		---><cfloop array="#arguments.suiteStats.specStats#" index="local.thisSpec"><!---
		--->#tabsNext##getStatusIndicator( local.thisSpec.status )# #printByStatus( local.thisSpec.status, local.thisSpec.displayName)# #color( "dim", "(#local.thisSpec.totalDuration# ms)")# #chr(13)#<!---

			If Spec Failed

		---><cfif local.thisSpec.status eq "failed"><!---
		--->#space()##tabsNext# ! Failure: #local.thisSpec.failMessage# #local.thisSpec.failDetail# #chr(13)#
		   #space()#
<!---
		---></cfif><!---

			If Spec Errored Out

		---><cfif local.thisSpec.status eq "error"><!---
		--->#space()#
 #tabsNext# #color( "bold+magenta", "💥 Error: #local.thisSpec.error.message# #local.thisSpec.error.detail# #chr(13)#" )#<!---
 ---><cfloop array="#local.thisSpec.error.tagContext#" index="thisStack"><!---
Only show non testbox template paths
---><cfif !reFindNoCase( "testbox(\/|\\)system(\/|\\)", thisStack.template )>#tabsNext#  #color( "bold+red", "-> #thisStack.template#:#thisStack.line#")#
</cfif><!---
---></cfloop>
#space()#
#color( "bold+white", left( local.thisSpec.error.stackTrace, 1500 ) )# #chr(13)##chr(13)#
#space()#
<!---
---></cfif><!---
		---></cfloop><!---

			Do we have nested suites

		---><cfif arrayLen( arguments.suiteStats.suiteStats )><!---
			---><cfloop array="#arguments.suiteStats.suiteStats#" index="local.nestedSuite"><!---
			--->#genSuiteReport( local.nestedSuite, arguments.bundleStats, arguments.level + 1 )#<!---
			---></cfloop><!---
		---></cfif><!---
		---></cfoutput><!---
	---></cfsavecontent>
	<cfreturn local.report>
</cffunction>
</cfoutput>