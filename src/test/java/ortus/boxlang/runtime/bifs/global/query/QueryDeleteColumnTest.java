package ortus.boxlang.runtime.bifs.global.query;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class QueryDeleteColumnTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {
		// Clean up resources if necessary
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It should delete a column from the query" )
	@Test
	public void testDeleteColumn() {
		instance.executeSource(
		    """
		    query = QueryNew("name,age", "varchar,integer");
		    QueryAddRow(query, {name: "John", age: 30});
		    QueryAddRow(query, {name: "Jane", age: 25});

		    QueryDeleteColumn(query, "name");
		    columnList = query.columnList;
		    age1 = query.age[1];
		    age2 = query.age[2];
		    """,
		    context
		);

		assertThat( variables.get( Key.of( "columnList" ) ) ).isEqualTo( "age" );
		assertThat( variables.get( Key.of( "age1" ) ) ).isEqualTo( 30 );
		assertThat( variables.get( Key.of( "age2" ) ) ).isEqualTo( 25 );
	}

	@DisplayName( "It should work with member function" )
	@Test
	public void testDeleteColumnMemberFunction() {
		instance.executeSource(
		    """
		    query = QueryNew("name,age", "varchar,integer");
		    query.addRow({name: "John", age: 30});
		    query.addRow({name: "Jane", age: 25});

		    query.deleteColumn("name");
		    columnList = query.columnList;
		    age1 = query.age[1];
		    age2 = query.age[2];
		    """,
		    context
		);

		assertThat( variables.get( Key.of( "columnList" ) ) ).isEqualTo( "age" );
		assertThat( variables.get( Key.of( "age1" ) ) ).isEqualTo( 30 );
		assertThat( variables.get( Key.of( "age2" ) ) ).isEqualTo( 25 );
	}

	@DisplayName( "It should throw an error when trying to delete a column that does not exist" )
	@Test
	public void testDeleteNonExistentColumn() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        query = QueryNew("name", "varchar");
		        QueryAddRow(query, {name: "John"});

		        QueryDeleteColumn(query, "age");
		        """,
		        context
		    )
		);
	}

}
