package ortus.boxlang.runtime.jdbc.drivers;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.jdbc.BoxConnection;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import tools.JDBCTestUtils;

@EnabledIf( "tools.JDBCTestUtils#hasMySQLModule" )
public class MySQLDriverTest extends AbstractDriverTest {

	public static DataSource	mysqlDatasource;

	protected static Key		datasourceName		= Key.of( "MySQLdatasource" );

	protected static IStruct	datasourceConfig	= Struct.of(
	    "username", "root",
	    "password", "123456Password",
	    "host", "localhost",
	    "port", "3309",
	    "driver", "mysql",
	    "database", "myDB",
	    "custom", "allowMultiQueries=true"
	);

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
		IBoxContext setUpContext = new ScriptingRequestBoxContext( instance.getRuntimeContext() );

		mysqlDatasource = AbstractDriverTest.setupTestDatasource( instance, setUpContext, datasourceName, datasourceConfig );
		MySQLDriverTest.createGeneratedKeyTable( mysqlDatasource, setUpContext );

		// @TODO: Move mysql-specific StoredProcTest here and uncomment this setup.
		// setupStoredProcTests( setUpContext );
	}

	@AfterAll
	public static void teardown() throws SQLException {
		IBoxContext tearDownContext = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		JDBCTestUtils.dropTestTable( mysqlDatasource, tearDownContext, "company", true );
		AbstractDriverTest.teardownTestDatasource( tearDownContext, mysqlDatasource );
	}

	/***
	 * Set up stored procedure and supporting table for testing. See StoredProcTest.
	 */
	public static void setupStoredProcTests( IBoxContext context ) {
		mysqlDatasource.execute(
		    """
		    CREATE TABLE `company` (
		    `id` int NOT NULL,
		    `name` text NOT NULL,
		    `active` tinyint(1) NOT NULL DEFAULT '1'
		    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
		    	""",
		    context
		);
		mysqlDatasource.execute(
		    """
		    INSERT INTO `company` (`id`, `name`, `active`) VALUES
		    (1, 'Nintendo', 1),
		    (2, 'SEGA', 0),
		    (3, 'Sony', 1),
		    (4, 'Microsoft', 1);
		    	""",
		    context
		);
		// formatter:off
		mysqlDatasource.execute(
		    """
		    DELIMITER //
		    CREATE PROCEDURE sp_multi_result_set(IN companyName VARCHAR(255))
		    BEGIN
		        SELECT *
		        FROM company
		        WHERE name <> companyName
		        order by name asc;

		        SELECT *
		        FROM company
		        WHERE name <> companyName
		        order by name desc;
		    END //
		    DELIMITER ;
		    """,
		    context
		);
		// @formatter:on
	}

	/**
	 * Create a table that uses generated keys so we can test our generated key retrieval in BL.
	 * 
	 * @param dataSource Datasource object
	 * @param context    Box context
	 */
	public static void createGeneratedKeyTable( DataSource dataSource, IBoxContext context ) {
		try {
			dataSource.execute( "CREATE TABLE generatedKeyTest( id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(155))", context );
		} catch ( DatabaseException ignored ) {
		}
	}

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
		try ( BoxConnection conn = myDataSource.getBoxConnection() ) {
			assertThat( conn ).isInstanceOf( BoxConnection.class );
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
		    println( result )
		       """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 5, query.size() );
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

	@DisplayName( "It can raise SQL error" )
	@Test
	public void testSQLError() {
		Throwable t = assertThrows( Throwable.class, () -> instance.executeStatement(
		    """
		        queryExecute( "
		    		SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Boom!';
		        ", {}, { "datasource" : "MySQLdatasource" } );
		    """, context ) );
		assertThat( t.getMessage() ).contains( "Boom!" );
		assertThat( t.getCause() ).isNotNull();
		assertThat( t.getCause().getMessage() ).contains( "Boom!" );
	}

	@DisplayName( "It can raise SQL error after success" )
	@Test
	public void testSQLErrorAfterSuccess() {
		Throwable t = assertThrows( Throwable.class, () -> instance.executeStatement(
		    """
		          queryExecute( "
		    SELECT 'brad' as dev;
		      		SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Boom!';
		          ", {}, { "datasource" : "MySQLdatasource" } );
		      """, context ) );
		assertThat( t.getMessage() ).contains( "Boom!" );
		assertThat( t.getCause() ).isNotNull();
		assertThat( t.getCause().getMessage() ).contains( "Boom!" );
	}

	@Disabled( "Stored procedure initialization needs work; is failing with a syntax error on the first line." )
	@Test
	public void testMultiResultSets() {
		getInstance().executeSource(
		    String.format(
		        """
		            storedproc dataSource = "%s" procedure = "sp_multi_result_set" {
		            	procparam sqltype="varchar" value="SEGA";
		            	procresult name="names_asc" resultSet=1 maxRows=1;
		            	procresult name="names_desc" resultSet=2 maxRows=2;
		            };
		        """, getDatasourceName() ),
		    getContext(), BoxSourceType.BOXTEMPLATE );
		assertThat( getVariables().get( Key.of( "names_asc" ) ) ).isInstanceOf( Query.class );
		assertThat( getVariables().get( Key.of( "names_desc" ) ) ).isInstanceOf( Query.class );
	}

	@DisplayName( "It can select from char 15 field" )
	@Test
	public void testSelectFromCharFields() {
		instance.executeStatement(
		    """
		    queryExecute( "
		    	CREATE TABLE IF NOT EXISTS char15Test ( char15field CHAR(15) )
		    ",{},{ "datasource" : "MySQLdatasource" }
		    );

		    queryExecute( "TRUNCATE TABLE char15Test",{},{ "datasource" : "MySQLdatasource" } );
		    queryExecute( "INSERT INTO char15Test ( char15field ) VALUES ( 'value' )",{},{ "datasource" : "MySQLdatasource" } );

		    result = queryExecute( "
		    	SELECT * FROM char15Test where char15field = ?
		    	",[ {
		    		sqltype : "varchar",
		    		value: "value"
		    	}],{ "datasource" : "MySQLdatasource" }
		    );
		    """,
		    context );

		// Verify that the query found the row without needing RTRIM
		assertThat( variables.getAsQuery( result ).size() ).isEqualTo( 1 );

	}

	@DisplayName( "It can handle large blob and clob columns" )
	@Test
	public void testLargeBlobAndClobColumns() {
		instance.executeStatement(
		    """
		    queryExecute( "
		    	CREATE TABLE IF NOT EXISTS large_lob (
		    		id INT PRIMARY KEY,
		    		blob_data LONGBLOB,
		    		clob_data LONGTEXT
		    	)
		    ",{},{ "datasource" : "MySQLdatasource" }
		    );

		    queryExecute( "TRUNCATE TABLE large_lob", {}, { "datasource" : "MySQLdatasource" } );
		    """, context );
		// @formatter:off
		instance.executeStatement(
			"""
				newLobData = "";
				// ~300 chars
				quote = "
		               	Farewell, Aragorn! Go to Minas Tirith and save my people! I
		               	have failed.
		               	No! said Aragorn, taking his hand and kissing his brow. You
		               	have conquered. Few have gained such a victory. Be at peace! Minas
		               	Tirith shall not fall!
		        ";
				// times 1000 = ~300,000 chars
				for( i=1; i <= 1000; i = i + 1 ) {
					newLobData = newLobData & quote;
				}
				
				insert = queryExecute( "INSERT INTO large_lob ( id, blob_data, clob_data ) VALUES ( 1, ?, ? )", [ 
					{ value: newLobData, sqltype: "blob" },
					{ value: newLobData, sqltype: "clob" }
				], { "datasource" : "MySQLdatasource" } );
			""", context );
		instance.executeStatement(
			"""
				result = queryExecute( "SELECT * FROM large_lob WHERE id = 1", {}, { "datasource" : "MySQLdatasource" } );
			""", context );
		// @formatter:on
		assertThat( variables.get( result ) ).isInstanceOf( Query.class );
		Query query = variables.getAsQuery( result );
		assertEquals( 1, query.size() );
		IStruct	firstRow	= query.getRowAsStruct( 0 );

		// Test BLOB data (LONGBLOB in MySQL)
		Object	blobData	= firstRow.get( Key.of( "blob_data" ) );
		assertThat( blobData ).isNotNull();
		// BLOB data may be returned as byte[] or other types depending on driver
		if ( blobData instanceof byte[] ) {
			String retrieved = new String( ( byte[] ) blobData, java.nio.charset.StandardCharsets.UTF_8 );
			assertThat( retrieved.length() ).isAtLeast( 300000 );
		}

		// Test CLOB data (LONGTEXT in MySQL)
		Object clobData = firstRow.get( Key.of( "clob_data" ) );
		assertThat( clobData ).isNotNull();
		assertThat( clobData ).isInstanceOf( String.class );
		String clobString = ( String ) clobData;
		assertThat( clobString.length() ).isAtLeast( 300000 );
		// Use containsMatch to handle whitespace variations
		assertThat( clobString ).containsMatch( "Farewell,\\s+Aragorn!" );
		assertThat( clobString ).containsMatch( "Minas\\s+Tirith\\s+shall\\s+not\\s+fall!" );
	}

}
