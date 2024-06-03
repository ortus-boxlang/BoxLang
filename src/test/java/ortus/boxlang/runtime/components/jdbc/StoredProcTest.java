package ortus.boxlang.runtime.components.jdbc;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.bifs.global.jdbc.BaseJDBCTest;
import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

public class StoredProcTest extends BaseJDBCTest {

	static Key result = new Key( "result" );

	public static void doNothing() {
		// A lazy fellow indeed.
		System.out.println( "Doing nothing." );
	}

	public static void withParam( Integer total ) {
		// return 42;
	}

	public static Integer withResult() {
		return 42;
	}

	@BeforeAll
	public static void setUpStoredProc() {
		DataSource ds = getDatasource();
		ds.execute(
		    """
		    CREATE PROCEDURE withParam( IN TOTAL Integer )
		    PARAMETER STYLE JAVA READS SQL DATA LANGUAGE JAVA EXTERNAL NAME
		    'ortus.boxlang.runtime.components.jdbc.StoredProcTest.withParam'
		      """
		);
		ds.execute(
		    """
		    CREATE PROCEDURE withResult( OUT int )
		    PARAMETER STYLE JAVA READS SQL DATA LANGUAGE JAVA EXTERNAL NAME
		    'ortus.boxlang.runtime.components.jdbc.StoredProcTest.withResult'
		      """
		);
		ds.execute(
		    """
		    CREATE PROCEDURE doNothing()
		    PARAMETER STYLE JAVA READS SQL DATA LANGUAGE JAVA EXTERNAL NAME
		    'ortus.boxlang.runtime.components.jdbc.StoredProcTest.doNothing'
		    """
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

		assertThat( getVariables().get( result ) ).isInstanceOf( ortus.boxlang.runtime.types.Query.class );
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

		assertThat( getVariables().get( result ) ).isInstanceOf( ortus.boxlang.runtime.types.Query.class );
	}

	@DisplayName( "It properly handles IN params" )
	@Test
	public void testParam() {
		getInstance().executeSource(
		    """
		    <bx:storedproc procedure="withParam">
		        <bx:procparam name="total" value="42">
		        <bx:procresult name="result">
		    </bx:storedproc>
		    """,
		    getContext(), BoxSourceType.BOXTEMPLATE );

		assertThat( getVariables().get( result ) ).isInstanceOf( ortus.boxlang.runtime.types.Query.class );
	}

	@Disabled( "Currently failing. Must fix." )
	@DisplayName( "It properly handles OUT params" )
	@Test
	public void testResults() {
		getInstance().executeSource(
		    """
		    <bx:storedproc procedure="withResult">
		        <bx:procparam type="out" variable="result">
		        <bx:procresult name="result">
		    </bx:storedproc>
		    """,
		    getContext(), BoxSourceType.BOXTEMPLATE );

		assertThat( getVariables().get( result ) ).isInstanceOf( ortus.boxlang.runtime.types.Query.class );
	}

	@Disabled( "Currently failing. Must fix." )
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
}
