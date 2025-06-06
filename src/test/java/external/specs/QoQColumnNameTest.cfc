/**
* Copied from test\tickets\_LDEV3615.cfc in Lucee
*/
component extends="testbox.system.BaseSpec"{


	function testQoQunion (){
		var q1 = queryNew(
			"subtype",
			"varchar",
			[["RECORD3_TEMPLATE"]]
		);
		var q2 = queryNew(
			"id",
			"int",
			[[1]]
		);
			
		```
			<cfquery name="local.result" dbtype="query">
					SELECT 	subtype,
							subtype AS subject
					FROM 	q1
					UNION
					SELECT 	NULL AS subtype,
							NULL AS subject
					FROM	q2
			</cfquery>
		
		```
		// remove white space
		expect( serializeJson( local.result ).reReplace( '\s', '', 'all' ) ).toBe('{"COLUMNS":["subtype","subject"],"DATA":[["RECORD3_TEMPLATE","RECORD3_TEMPLATE"],[null,null]]}');
	}

	function testNullAliases (){
		var q1 = queryNew( "col" )

		```
			<cfquery name="local.result" dbtype="query">
					SELECT 	NULL AS a,
							NULL AS b
					FROM	q1
			</cfquery>
		
		```
		expect( local.result.columnList ).toBe('A,B');
	}

	function testNullNoAlias (){
		var q1 = queryNew( "col" )

		```
			<cfquery name="local.result" dbtype="query">
					SELECT 	NULL,
							NULL
					FROM	q1
			</cfquery>
		
		```
		expect( local.result.columnList ).toBe('COLUMN_0,COLUMN_1');
	}

	function testBooleanAliases (){
		var q1 = queryNew( "col" )

		```
			<cfquery name="local.result" dbtype="query">
					SELECT 	true AS a,
							true AS b,
							false AS c,
							false AS d
					FROM	q1
			</cfquery>
		
		```
		expect( local.result.columnList ).toBe('A,B,C,D');
	}

	function testBooleanNoAlias (){
		var q1 = queryNew( "col" )

		```
			<cfquery name="local.result" dbtype="query">
					SELECT 	true,
							false
					FROM	q1
			</cfquery>
		
		```
		expect( local.result.columnList ).toBe('COLUMN_0,COLUMN_1');
	}
}

