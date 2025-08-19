package ortus.boxlang.runtime.jdbc.drivers;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;

@EnabledIf( "tools.JDBCTestUtils#hasMySQLModule" )
public class MySQLDriverTest extends AbstractDriverTest {

	/**
	 * Override to provide driver-specific datasource name
	 */
	@Override
	String getDatasourceName() {
		return "MySQLdatasource";
	}

	@DisplayName( "It can get a MySQL JDBC connection" )
	@Test
	void testMySQLConnection() throws SQLException {
		DataSource myDataSource = DataSource.fromStruct(
		    Key.of( "mysql" ),
		    Struct.of(
		        "username", "root",
		        "password", "123456Password",
		        "connectionString", "jdbc:mysql://localhost:3309"
		    ) );
		try ( Connection conn = myDataSource.getConnection() ) {
			assertThat( conn ).isInstanceOf( Connection.class );
		}
		myDataSource.shutdown();
	}

	@DisplayName( "It supports timestamp param types" )
	@Test
	public void testTimestampDateParam() {
		instance.executeSource(
		    """
		       queryExecute(
		    	"
		    	INSERT INTO developers ( id, name, role, createdAt )
		    	VALUES ( 100, 'Tony Skipponi', 'Engineer', :timestamp )",
		    	{
		    		timestamp : { sqltype : "cf_sql_timestamp", value : now() }
		    	},
		    	{ "datasource" : "mysqldatasource" }
		    );
		    result = queryExecute( "SELECT * FROM developers WHERE id = 100", [], { "datasource" : "mysqldatasource" } );
		       """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 1, query.size() );
	}

	@DisplayName( "It can use string values as timestamp params." )
	@Test
	public void testTimestampParamCompare() {
		instance.executeSource(
		    """
		      queryExecute( "INSERT INTO developers ( id, name, role, createdAt )
		      	VALUES ( 101, 'Tony Skipponi', 'Engineer', NOW() )", {}, { "datasource" : "mysqldatasource" } );
		      result = queryExecute(
		      	"
		      	SELECT * FROM developers
		    WHERE createdAt IS NOT NULL AND createdAt < :timestamp",
		      	{
		      		timestamp : { sqltype : "cf_sql_timestamp", value : "09/24/2099" }
		      	},
		      	{ "datasource" : "mysqldatasource" }
		      );
		      """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 1, query.size() );
	}

	@DisplayName( "It can execute multiple statements in a single queryExecute() call" )
	@Test
	public void testMultipleStatements() {
		assertDoesNotThrow( () -> instance.executeStatement(
		    """
		           result = queryExecute( '
		     	   TRUNCATE TABLE developers;
		               INSERT INTO developers (id) VALUES (111);
		               INSERT INTO developers (id) VALUES (222);
		               SELECT * FROM developers;
		               INSERT INTO developers (id) VALUES (333);
		               INSERT INTO developers (id) VALUES (444);
		               ',
		      [],
		      { "datasource" : "mysqldatasource" }
		           );
		    """, context )
		);
		Object multiStatementQueryReturn = variables.get( Key.of( "result" ) );
		assertThat( multiStatementQueryReturn ).isInstanceOf( Query.class );
		assertEquals( 2, ( ( Query ) multiStatementQueryReturn ).size(), "For compatibility, the last result should be returned" );

		Query newTableRows = ( Query ) instance
		    .executeStatement( "queryExecute( 'SELECT * FROM developers WHERE id IN (111,222)', [],{ 'datasource' : 'mysqldatasource' } );", context );
		assertEquals( 2, newTableRows.size() );
	}

	@DisplayName( "Gets empty tables query when unmatched database name is provided" )
	@Test
	public void testTablesTypeBadDBName() {
		getInstance().executeSource(
		    """
		        cfdbinfo( type='tables', name='result', datasource="mysqldatasource", dbname="foo" )
		    """,
		    getContext(), BoxSourceType.CFSCRIPT );
		Query resultQuery = ( Query ) getVariables().get( result );
		assertThat( resultQuery.size() ).isEqualTo( 0 );
	}

	@DisplayName( "Can get catalog and schema names" )
	@Test
	public void testDBNamesType() {
		getInstance().executeSource(
		    """
		        cfdbinfo( type='dbnames', name='result', datasource='mysqldatasource' )
		    """,
		    getContext(), BoxSourceType.CFSCRIPT );
		Object theResult = getVariables().get( result );
		assertThat( theResult ).isInstanceOf( Query.class );

		Query dbNamesQuery = ( Query ) theResult;
		assertThat( dbNamesQuery.size() ).isGreaterThan( 0 );
		assertEquals( 2, dbNamesQuery.getColumns().size() );

		IStruct ourDBRow = dbNamesQuery.stream()
		    .filter( row -> row.getAsString( Key.of( "DBNAME" ) ).equals( "myDB" ) )
		    .findFirst()
		    .orElse( null );

		assertNotNull( ourDBRow );
		assertEquals( "CATALOG", ourDBRow.getAsString( Key.type ) );
		assertEquals( "myDB", ourDBRow.getAsString( Key.of( "DBNAME" ) ) );
	}
}
