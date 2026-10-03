package ortus.boxlang.runtime.jdbc.drivers;

import static com.google.common.truth.Truth.assertThat;

import java.sql.SQLException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

@EnabledIf( "tools.JDBCTestUtils#hasPostgresModule" )
public class PostgreSQLDriverTest extends AbstractDriverTest {

	public static DataSource	postgresqlDatasource;

	protected static Key		datasourceName		= Key.of( "postgresqlDatasource" );

	protected static IStruct	datasourceConfig	= Struct.of(
	    "username", "postgres",
	    "password", "123456Password",
	    "host", "localhost",
	    "port", "5432",
	    "driver", "postgresql",
	    "database", "myDB"
	);

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
		IBoxContext setUpContext = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		postgresqlDatasource = AbstractDriverTest.setupTestDatasource( instance, setUpContext, datasourceName, datasourceConfig );
		PostgreSQLDriverTest.createGeneratedKeyTable( postgresqlDatasource, setUpContext );
	}

	@AfterAll
	public static void teardown() throws SQLException {
		IBoxContext tearDownContext = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		AbstractDriverTest.teardownTestDatasource( tearDownContext, postgresqlDatasource );
	}

	/**
	 * Create a table that uses generated keys so we can test our generated key retrieval in BL.
	 *
	 * @param dataSource Datasource object
	 * @param context    Box context
	 */
	public static void createGeneratedKeyTable( DataSource dataSource, IBoxContext context ) {
		try {
			dataSource.execute( "CREATE TABLE generatedKeyTest( id SERIAL PRIMARY KEY, name VARCHAR(155))", context );
		} catch ( DatabaseException ignored ) {
		}
	}

	/**
	 * Override to provide driver-specific datasource name
	 */
	@Override
	String getDatasourceName() {
		return "postgresqlDatasource";
	}

	/**
	 * pgjdbc executes a multi-statement batch as one composite statement: the rows of the first
	 * INSERT come back as a result set, the update counts of the others follow and no generated
	 * keys are produced at all. The generic expectations of the base test therefore do not hold
	 * on PostgreSQL; the single-statement behaviour is covered below.
	 */
	@Override
	@Disabled( "pgjdbc returns no generated keys for a multi-statement batch" )
	public void testGeneratedKey() {
	}

	@DisplayName( "INSERT ... RETURNING returns the rows as the query and still sets generatedKey" )
	@Test
	public void testInsertReturningRows() {
		instance.executeStatement(
		    String.format( """
		                   variables.q = queryExecute(
		                   	"INSERT INTO generatedKeyTest (name) VALUES ( 'Michael' ), ( 'Michael2' ) RETURNING id, name",
		                   	{},
		                   	{ "result": "variables.result", "datasource": "%s" }
		                   );
		                   """, getDatasourceName() ),
		    context );

		Query q = ( Query ) variables.get( Key.of( "q" ) );
		assertThat( q.size() ).isEqualTo( 2 );
		assertThat( q.getColumnList().toLowerCase() ).isEqualTo( "id,name" );
		assertThat( q.getRowAsStruct( 0 ).getAsString( Key._NAME ) ).isEqualTo( "Michael" );
		assertThat( q.getRowAsStruct( 1 ).getAsString( Key._NAME ) ).isEqualTo( "Michael2" );
		int		firstId	= IntegerCaster.cast( q.getRowAsStruct( 0 ).get( Key.id ) );

		IStruct	meta	= variables.getAsStruct( result );
		assertThat( IntegerCaster.cast( meta.get( Key.generatedKey ) ) ).isEqualTo( firstId );
		Array generatedKeys = meta.getAsArray( Key.generatedKeys );
		assertThat( generatedKeys ).hasSize( 1 );
		assertThat( ( ( Array ) generatedKeys.get( 0 ) ).stream().map( IntegerCaster::cast ).toArray( Integer[]::new ) )
		    .isEqualTo( new Integer[] { firstId, firstId + 1 } );
		assertThat( meta.get( Key.recordCount ) ).isEqualTo( 2 );
		assertThat( meta.get( Key.updateCount ) ).isEqualTo( 2 );
	}

	@DisplayName( "UPDATE ... RETURNING and a bound parameter return the rows as the query" )
	@Test
	public void testUpdateReturningRows() {
		instance.executeStatement(
		    String.format( """
		                   queryExecute( "INSERT INTO generatedKeyTest (name) VALUES ( 'Luis' )", {}, { "datasource": "%1$s" } );
		                   variables.q = queryExecute(
		                   	"UPDATE generatedKeyTest SET name = :newName WHERE name = 'Luis' RETURNING id, name",
		                   	{ newName : "Luis Majano" },
		                   	{ "result": "variables.result", "datasource": "%1$s" }
		                   );
		                   """, getDatasourceName() ),
		    context );

		Query q = ( Query ) variables.get( Key.of( "q" ) );
		assertThat( q.size() ).isEqualTo( 1 );
		assertThat( q.getRowAsStruct( 0 ).getAsString( Key._NAME ) ).isEqualTo( "Luis Majano" );
		IStruct meta = variables.getAsStruct( result );
		assertThat( IntegerCaster.cast( meta.get( Key.generatedKey ) ) ).isEqualTo( IntegerCaster.cast( q.getRowAsStruct( 0 ).get( Key.id ) ) );
		assertThat( meta.get( Key.updateCount ) ).isEqualTo( 1 );
	}

	@DisplayName( "A plain INSERT without RETURNING keeps an empty query, even when the word appears in a literal or comment" )
	@Test
	public void testInsertWithoutReturningStaysEmpty() {
		instance.executeStatement(
		    String.format( """
		                   variables.q = queryExecute(
		                   	"INSERT INTO generatedKeyTest (name) VALUES ( 'returning is just a word' ) -- returning nothing",
		                   	{},
		                   	{ "result": "variables.result", "datasource": "%s" }
		                   );
		                   """, getDatasourceName() ),
		    context );

		Query q = ( Query ) variables.get( Key.of( "q" ) );
		assertThat( q.size() ).isEqualTo( 0 );
		IStruct meta = variables.getAsStruct( result );
		assertThat( meta.get( Key.generatedKey ) ).isNotNull();
		assertThat( meta.get( Key.updateCount ) ).isEqualTo( 1 );
	}
}
