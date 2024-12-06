package ortus.boxlang.runtime.components.jdbc;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.global.jdbc.BaseJDBCTest;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

public class StoredProcTest extends BaseJDBCTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	static Key			result	= new Key( "result" );

	private DataSource	mysqlDatasource;

	public static void doNothing() {
		// A lazy fellow indeed.
		System.out.println( "Doing nothing." );
	}

	public static void withInParam( Integer total ) {
		// return 42;
	}

	public static Integer withOutParam() {
		return 42;
	}

	public static void withResultSet( ResultSet[] rs ) {
		try {
			rs[ 0 ] = getDatasource().getConnection().createStatement().executeQuery( "SELECT * FROM developers" );
		} catch ( SQLException e ) {
			// Handle the exception
		}
	}

	@BeforeAll
	public static void setUpStoredProc() {
		instance = BoxRuntime.getInstance( true );
		IBoxContext	setUpContext	= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		DataSource	ds				= getDatasource();
		ds.execute(
		    """
		    CREATE PROCEDURE withInParam( IN TOTAL Integer )
		    PARAMETER STYLE JAVA
		    READS SQL DATA
		    LANGUAGE JAVA
		    EXTERNAL NAME 'ortus.boxlang.runtime.components.jdbc.StoredProcTest.withInParam'
		    """,
		    setUpContext
		);
		ds.execute(
		    """
		    CREATE PROCEDURE withOutParam( OUT int )
		    PARAMETER STYLE JAVA
		    READS SQL DATA
		    LANGUAGE JAVA
		    EXTERNAL NAME 'ortus.boxlang.runtime.components.jdbc.StoredProcTest.withOutParam'
		    """,
		    setUpContext
		);
		ds.execute(
		    """
		    CREATE PROCEDURE withResultSet()
		    PARAMETER STYLE JAVA
		    READS SQL DATA
		    LANGUAGE JAVA
		    EXTERNAL NAME 'ortus.boxlang.runtime.components.jdbc.StoredProcTest.withResultSet'
		    DYNAMIC RESULT SETS 1
		    """,
		    setUpContext
		);
		ds.execute(
		    """
		    CREATE PROCEDURE doNothing()
		    PARAMETER STYLE JAVA READS SQL DATA LANGUAGE JAVA EXTERNAL NAME
		    'ortus.boxlang.runtime.components.jdbc.StoredProcTest.doNothing'
		    """,
		    setUpContext
		);
	}

	@BeforeEach
	public void setupEach() {
		context = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
	}

	public void setupMySQLTest() {
		mysqlDatasource = DataSource.fromStruct(
		    "MysqlStoredProcTest",
		    Struct.of(
		        "database", "MysqlStoredProcTest",
		        "driver", "mysql",
		        "connectionString", "jdbc:mysql//localhost:3306/mysqlStoredProc"
		    )
		);
		getDatasourceService().register( Key.of( "MysqlStoredProcTest" ), mysqlDatasource );

		mysqlDatasource.execute(
		    """
		    CREATE DEFINER=`root`@`%` PROCEDURE `sp_multi_result_set` (IN `companyName` VARCHAR(255))   BEGIN
		    	SELECT *
		    	 FROM company
		    	WHERE name <> companyName
		    	order by name asc;

		    	SELECT *
		    	 FROM company
		    	WHERE name <> companyName
		    	order by name desc;
		    END$$
		       """,
		    context
		);
		mysqlDatasource.execute(
		    """
		    		CREATE TABLE `company` (
		      `id` int NOT NULL,
		      `name` text NOT NULL,
		      `active` tinyint(1) NOT NULL DEFAULT '1'
		    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
	}

	@DisplayName( "It requires the 'procedure' argument" )
	@Test
	public void testProcedureValidation() {
		BoxValidationException e = assertThrows( BoxValidationException.class, () -> getInstance().executeSource(
		    """
		      <bx:storedproc></bx:storedproc>
		    """,
		    getContext(), BoxSourceType.BOXTEMPLATE ) );

		assertThat( e.getMessage() ).contains( "Input [procedure] for component [StoredProc] is required." );
		assertNull( getVariables().get( result ) );
	}

	@DisplayName( "It throws if the stored procedure could not be located." )
	@Test
	public void testMissingProcedure() {
		DatabaseException e = assertThrows( DatabaseException.class, () -> getInstance().executeSource(
		    """
		      <bx:storedproc procedure="foo"></bx:storedproc>
		    """,
		    getContext(), BoxSourceType.BOXTEMPLATE ) );

		assertThat( e.getMessage() ).contains( "'FOO' is not recognized as a function or procedure." );
	}

	@DisplayName( "It tests CFML tag compat syntax" )
	@Test
	public void testCompatScriptSyntax() {
		getInstance().executeSource(
		    """
		    cfstoredproc( procedure="doNothing" ){
		        cfprocresult( name="result" );
		    }
		    """,
		    getContext(), BoxSourceType.CFSCRIPT );

		assertThat( getVariables().get( result ) ).isInstanceOf( Query.class );
	}

	@DisplayName( "It tests CFML tag compat syntax" )
	@Test
	public void testCompatTagSyntax() {
		getInstance().executeSource(
		    """
		    <cfstoredproc procedure="doNothing">
		        <cfprocresult name="result">
		    </cfstoredproc>
		    """,
		    getContext(), BoxSourceType.CFTEMPLATE );

		assertThat( getVariables().get( result ) ).isInstanceOf( Query.class );
	}

	@DisplayName( "It properly handles IN params" )
	@Test
	public void testInParam() {
		getInstance().executeSource(
		    """
		    <bx:storedproc procedure="withInParam">
		        <bx:procparam name="total" value="42">
		        <bx:procresult name="result">
		    </bx:storedproc>
		    """,
		    getContext(), BoxSourceType.BOXTEMPLATE );

		assertThat( getVariables().get( result ) ).isInstanceOf( Query.class );
	}

	@Disabled( "Currently failing. Must fix." )
	@DisplayName( "It properly handles OUT params" )
	@Test
	public void testOutParams() {
		getInstance().executeSource(
		    """
		    <bx:storedproc procedure="withOutParam">
		        <bx:procparam type="out" variable="foo">
		    </bx:storedproc>
		    """,
		    getContext(), BoxSourceType.BOXTEMPLATE );

		assertEquals( 42, getVariables().getAsInteger( Key.of( "foo" ) ) );
	}

	@Disabled( "Currently failing. Must fix." )
	@DisplayName( "It properly returns ResultSet objects" )
	@Test
	public void testResultSet() {
		getInstance().executeSource(
		    """
		    <bx:storedproc procedure="withResultSet">
		        <bx:procresult name="result">
		    </bx:storedproc>
		    """,
		    getContext(), BoxSourceType.BOXTEMPLATE );

		assertThat( getVariables().get( result ) ).isInstanceOf( Query.class );
		Query query = getVariables().getAsQuery( result );
		assertEquals( 3, query.size() );
	}

	@DisplayName( "It closes connection on completion" )
	@Test
	public void testConnectionClose() {
		Integer initiallyActive = getDatasource().getPoolStats().getAsInteger( Key.of( "ActiveConnections" ) );
		getInstance().executeSource(
		    """
		      <bx:storedproc procedure="doNothing">
		          <bx:procresult name="result">
		      </bx:storedproc>
		    """,
		    getContext(), BoxSourceType.BOXTEMPLATE );
		Integer subsequentActive = getDatasource().getPoolStats().getAsInteger( Key.of( "ActiveConnections" ) );
		assertEquals( initiallyActive, subsequentActive );
	}

	// Derby does a relatively poor job of implementing the stored procedure spec, so we'll only run this test if a MySQL instance is reachable.
	@EnabledIf( "tools.JDBCTestUtils#isMySQLReachable" )
	@Test
	public void testMultiResultSets() {
		// Because we can't do an `@EnabledIf` on a `@BeforeAll` method, we have to set up the datasource here.
		if ( mysqlDatasource == null ) {
			setupMySQLTest();
		}
		getInstance().executeSource(
		    """
		        storedproc dataSource = "MysqlStoredProcTest" procedure = "sp_multi_result_set" {
		        	procparam sqltype="varchar" value="SEGA";
		        	procresult name="names_asc" resultSet=1 maxRows=1;
		        	procresult name="names_desc" resultSet=2 maxRows=2;
		        };
		    """,
		    getContext(), BoxSourceType.BOXTEMPLATE );
		assertThat( getVariables().get( Key.of( "names_asc" ) ) ).isInstanceOf( Query.class );
		assertThat( getVariables().get( Key.of( "names_desc" ) ) ).isInstanceOf( Query.class );
	}

}
