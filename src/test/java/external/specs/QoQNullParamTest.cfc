/**
* Copied from test\tickets\LDEV0364.cfc in Lucee
*/
component extends="testbox.system.BaseSpec"{

	
	function beforeAll() {
		variables.queryWithDataIn = Querynew( 'id', 'integer', [[ 1 ]] );
	}

	function run( testResults , testBox ) {

		describe( 'QueryExecute' , function(){

			it( 'returns NULL when fed in through array parameter with nulls=true' , function() {

				/*
					I have no idea *why* this is this way, but in the source code I spotted this.
					It turns out that there is an ability to cast to null but the parameter attribute
					is "nulls" instead of "null", go figure!
				*/

				actual = QueryExecute(
					options = {
						dbtype: 'query'
					},
					params = [
						{ type: 'integer' , value: 1 , null: true }
					],
					sql = "
						SELECT 
							COALESCE( ? , 'isnull' ) AS value,
							COALESCE( NULL , 'isnull' ) AS control
						FROM queryWithDataIn
					"
				);

				expect( actual.control[1] ).toBe( 'isnull' );
				expect( actual.value[1] ).toBe( 'isnull' );

			});

			it( 'returns NULL when fed in through array parameter with null=true' , function() {

				actual = QueryExecute(
					options = {
						dbtype: 'query'
					},
					params = [
						{ type: 'integer' , value: 1 , null: true }
					],
					sql = "
						SELECT 
							COALESCE( ? , 'isnull' ) AS value,
							COALESCE( NULL , 'isnull' ) AS control
						FROM queryWithDataIn
					"
				);

				expect( actual.control[1] ).toBe( 'isnull' );
				expect( actual.value[1] ).toBe( 'isnull' );

			});

			it( 'returns NULL when fed in through array named parameter with null=true' , function() {

				actual = QueryExecute(
					options = {
						dbtype: 'query'
					},
					params = [
						{ name: 'input' , type: 'integer' , value: 1 , null: true }
					],
					sql = "
						SELECT 
							COALESCE( :input , 'isnull' ) AS value,
							COALESCE( NULL , 'isnull' ) AS control
						FROM queryWithDataIn
					"
				);

				expect( actual.control[1] ).toBe( 'isnull' );
				expect( actual.value[1] ).toBe( 'isnull' );

			});

			it( 'returns NULL when fed in through struct parameter with null=true' , function() {

				actual = QueryExecute(
					options = {
						dbtype: 'query'
					},
					params = {
						'input': { type: 'integer' , value: 42 , nulls: true }
					},
					sql = "
						SELECT 
							COALESCE( :input , 'isnull-value' ) AS value,
							COALESCE( NULL , 'isnull-control' ) AS control
						FROM queryWithDataIn
					"
				);

				expect( actual.control[1] ).toBe( 'isnull-control' );
				expect( actual.value[1] ).toBe( 'isnull-value' );


			});

		});

		describe( 'cfquery in script' , function(){

			it( 'returns NULL when fed in through parameter with null=true' , function() {

				query
					name = 'actual'
					dbtype = 'query' {

					WriteOutput( "

						SELECT 
							COALESCE( 
					" );

					queryparam
						value = 1
						sqltype = 'integer'
						null = true;

					WriteOutput( " , 'isnull' ) AS value,
							COALESCE( NULL , 'isnull' ) AS control
						FROM queryWithDataIn
					" );
				}

				expect( actual.control[1] ).toBe( 'isnull' );
				expect( actual.value[1] ).toBe( 'isnull' );

			});

		});

	}


}

