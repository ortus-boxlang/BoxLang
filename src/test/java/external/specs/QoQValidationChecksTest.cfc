/**
* Copied from test\tickets\LDEV3878.cfc in Lucee
*/
component extends="testbox.system.BaseSpec"{
  q = queryNew("id","numeric",[[1]]);

	function run( testResults , testBox ) {
		describe( title="LDEV-3878 QoQ should not fall back to HSQLDB and throw meaningful errors", body=function() {

			aroundEach(function( spec, suite ){
				try {
					arguments.spec.body();
				} catch( any e ) {
					return;
				}
				fail( 'Test did not throw exception as expected' );
			});

			it(title="should throw for mod zero ", body = function( currentSpec ) {
				queryExecute( "
					select 1%0
					from q
					",
					[],
					{dbtype:"query"}
				);
			});

			it(title="should throw for divide by zero ", body = function( currentSpec ) {
				queryExecute( "
					select 1/0
					from q
					",
					[],
					{dbtype:"query"}
				);
			});

			it(title="should throw for too many count() columns ", body = function( currentSpec ) {
				queryExecute( "
					select count( id, id )
					from q
					",
					[],
					{dbtype:"query"}
				);
			});

			it(title="should throw for too many count distinct columns ", body = function( currentSpec ) {
				queryExecute( "
					select count( distinct id, id )
					from q
					",
					[],
					{dbtype:"query"}
				);
			});

			it(title="should throw for cast missing the type ", body = function( currentSpec ) {
				queryExecute( "
					select cast( id )
					from q
					",
					[],
					{dbtype:"query"}
				);
			});

			it(title="should throw for Union with mismatched column counts ", body = function( currentSpec ) {
				queryExecute( "
					select id, ''
					from q
					UNION
					select id
					from q
					",
					[],
					{dbtype:"query"}
				);
			});

			it(title="should throw for union with invalid order by", body = function( currentSpec ) {
				queryExecute( "
					select id
					from q
					UNION
					select id
					from q
					ORDER BY 'TEST'
					",
					[],
					{dbtype:"query"}
				);
			});

			it(title="should throw for invalid order by ordinal position ", body = function( currentSpec ) {
				queryExecute( "
					select id
					from q
					ORDER BY 5
					",
					[],
					{dbtype:"query"}
				);
			});

			it(title="should throw for positional param value with wrong type", body = function( currentSpec ) {
				queryExecute( "
					select id
					from q
					where id= ?
					",
					[
						{type="integer", value=""}
					],
					{dbtype:"query"}
				);
			});

			it(title="should throw for named param value with wrong type", body = function( currentSpec ) {
				queryExecute("
					select id
					from q
					where id= :id
					",
					{
						'id': {type="integer", value=""}
					},
					{dbtype:"query"}
				);
			});

		});
	}
}

