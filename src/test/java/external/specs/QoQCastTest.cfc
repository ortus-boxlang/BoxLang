/**
* Copied from test\tickets\_LDEV3615.cfc in Lucee
*/
component extends="testbox.system.BaseSpec"{
	function run( testResults , testBox ) {

        describe("testcase for LDEV-3522", function(){

            it(title="Can cast as date", body=function( currentSpec ){
                qry = QueryNew('foo','integer',[['1/1/2025']]);
                var actual = queryExecute(
                    "SELECT cast( foo as date ) as asDate,
											convert( foo, date ) as asDate2,
											convert( foo, 'date' ) as asDate3
                    FROM qry",
                    [],
                    {dbtype="query"} );
								expect( actual.asDate ).toBeDate();
								expect( actual.asDate ).toBeInstanceOf( 'DateTime' );
								expect( actual.asDate2 ).toBeDate();
								expect( actual.asDate2 ).toBeInstanceOf( 'DateTime' );
								expect( actual.asDate3 ).toBeDate();
								expect( actual.asDate3 ).toBeInstanceOf( 'DateTime' );
            });

            it(title="Can cast as string", body=function( currentSpec ){
                qry = QueryNew('foo','date',[[now()]]);
                var actual = queryExecute(
                    "SELECT foo,
											cast( foo as string ) as asString,
											convert( foo, string ) as asString2,
											convert( foo, 'string' ) as asString3
                    FROM qry",
                    [],
                    {dbtype="query"} );
								expect( actual.foo ).toBeDate();
								expect( actual.foo ).toBeInstanceOf( 'DateTime' );
								expect( actual.asString ).toBeString();
								expect( actual.asString ).toBeInstanceOf( 'java.lang.String' );
								expect( actual.asString2 ).toBeString();
								expect( actual.asString2 ).toBeInstanceOf( 'java.lang.String' );
								expect( actual.asString3 ).toBeString();
								expect( actual.asString3 ).toBeInstanceOf( 'java.lang.String' );
            });

            it(title="Can cast as number", body=function( currentSpec ){
                qry = QueryNew('foo','string',[['40']]);
                var actual = queryExecute(
                    "SELECT foo,
											cast( foo as number ) as asNumber,
											convert( foo, number ) as asNumber2,
											convert( foo, 'number' ) as asNumber3
                    FROM qry",
                    [],
                    {dbtype="query"} );
								expect( actual.foo ).toBeString();
								expect( actual.foo ).toBeInstanceOf( 'java.lang.String' );
								expect( actual.asNumber ).toBeNumeric();
								expect( actual.asNumber ).toBeInstanceOf( 'java.lang.Number' );
								expect( actual.asNumber2 ).toBeNumeric();
								expect( actual.asNumber2 ).toBeInstanceOf( 'java.lang.Number' );
								expect( actual.asNumber3 ).toBeNumeric();
								expect( actual.asNumber3 ).toBeInstanceOf( 'java.lang.Number' );
            });

            it(title="Can cast as boolean", body=function( currentSpec ){
                qry = QueryNew('foo','string',[['true']]);
                var actual = queryExecute(
                    "SELECT foo,
											cast( foo as boolean ) as asBoolean,
											convert( foo, boolean ) as asBoolean2,
											convert( foo, 'bool' ) as asBool3
                    FROM qry",
                    [],
                    {dbtype="query"} );
								expect( actual.foo ).toBeString();
								expect( actual.foo ).toBeInstanceOf( 'java.lang.String' );
								expect( actual.asBoolean ).toBeBoolean();
								expect( actual.asBoolean ).toBeInstanceOf( 'java.lang.Boolean' );
								expect( actual.asBoolean2 ).toBeBoolean();
								expect( actual.asBoolean2 ).toBeInstanceOf( 'java.lang.Boolean' );
								expect( actual.asBool3 ).toBeBoolean();
								expect( actual.asBool3 ).toBeInstanceOf( 'java.lang.Boolean' );
            });

          
       });

	}
}

