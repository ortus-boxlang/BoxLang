package ortus.boxlang.runtime.bifs.global.jdbc;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.scopes.Key;

public class PreserveSingleQuotesTest extends BaseJDBCTest {

	static Key result = new Key( "result" );

	@Test
	public void testNormalTags() {
		instance.executeSource(
		    """
		    <bx:query name="result">
		    	SELECT *
		    	FROM developers
		    	WHERE name = 'Bob O''Reily'
		    </bx:query>
		           """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsQuery( result ).size() ).isEqualTo( 1 );
		assertThat( variables.getAsQuery( result ).getRowAsStruct( 0 ).get( "name" ) ).isEqualTo( "Bob O'Reily" );
	}

	@Test
	public void testEntireSQLInterpolatedTags() {
		instance.executeSource(
		    """
		    <bx:output>
		    	<bx:set sql = "SELECT *
		    		FROM developers
		    		WHERE name = 'Bob O''Reily'">
		    	<bx:query name="result">
		    		#preserveSingleQuotes( sql )#
		    	</bx:query>
		    </bx:output>
		    		  """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsQuery( result ).size() ).isEqualTo( 1 );
		assertThat( variables.getAsQuery( result ).getRowAsStruct( 0 ).get( "name" ) ).isEqualTo( "Bob O'Reily" );
	}

	@Test
	public void testNameSQLInterpolatedTags() {
		instance.executeSource(
		    """
		    <bx:output>
		    	<bx:set name = "Bob O'Reily">
		    	<bx:query name="result">
		    		SELECT *
		    		FROM developers
		    		WHERE name = '#name#'
		    	</bx:query>
		    </bx:output>
		    		  """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsQuery( result ).size() ).isEqualTo( 1 );
		assertThat( variables.getAsQuery( result ).getRowAsStruct( 0 ).get( "name" ) ).isEqualTo( "Bob O'Reily" );
	}

	@Test
	public void testNameSQLInterpolatedTagsTwoQuotes() {
		instance.executeSource(
		    """
		    <bx:output>
		    	<bx:set name = "Billy 'The Fish' Bob">
		    	<bx:query name="result">
		    		SELECT '#name#' as name
		    		FROM developers
		    		WHERE name = 'Bob O''Reily'
		    	</bx:query>
		    </bx:output>
		    		  """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsQuery( result ).size() ).isEqualTo( 1 );
		assertThat( variables.getAsQuery( result ).getRowAsStruct( 0 ).get( "name" ) ).isEqualTo( "Billy 'The Fish' Bob" );
	}

	@Test
	public void testNormalScript() {
		instance.executeSource(
		    """
		    bx:query name="result" {
		    	echo( "SELECT *
		    	FROM developers
		    	WHERE name = 'Bob O''Reily' ")
		    }
		    	   """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.getAsQuery( result ).size() ).isEqualTo( 1 );
		assertThat( variables.getAsQuery( result ).getRowAsStruct( 0 ).get( "name" ) ).isEqualTo( "Bob O'Reily" );
	}

	@Test
	public void testEntireSQLInterpolatedScript() {
		instance.executeSource(
		    """
		    sql = "SELECT *
		    	FROM developers
		    	WHERE name = 'Bob O''Reily'";
		       bx:query name="result" {
		    	   echo( preserveSingleQuotes( sql ) )
		       }
		    		  """,
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.getAsQuery( result ).size() ).isEqualTo( 1 );
		assertThat( variables.getAsQuery( result ).getRowAsStruct( 0 ).get( "name" ) ).isEqualTo( "Bob O'Reily" );
	}

	@Test
	public void testNameSQLInterpolatedScript() {
		instance.executeSource(
		    """
		       name = "Bob O'Reily";
		    bx:query name="result" {
		     	 echo( "SELECT *
		     FROM developers
		     WHERE name = '#name#'" )
		     	}
		     		""",
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.getAsQuery( result ).size() ).isEqualTo( 1 );
		assertThat( variables.getAsQuery( result ).getRowAsStruct( 0 ).get( "name" ) ).isEqualTo( "Bob O'Reily" );
	}

	@Test
	public void testNameSQLConcatScript() {
		instance.executeSource(
		    """
		    name = "Bob O'Reily";
		    bx:query name="result" {
		    	echo( "SELECT *
		    	FROM developers
		    	WHERE name = '" & name & "'" )
		    }
		        		""",
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.getAsQuery( result ).size() ).isEqualTo( 1 );
		assertThat( variables.getAsQuery( result ).getRowAsStruct( 0 ).get( "name" ) ).isEqualTo( "Bob O'Reily" );
	}

	@Test
	public void testListQualifyWithSingleQuoteQualifier() {
		instance.executeSource(
		    """
		    <bx:output>
		    	<bx:set qry = queryNew( "name", "varchar", [["Brad"],["Luis"],["O'Neil"]] )>
		    	<bx:set names = "Brad,O'Neil">
		    	<bx:query name="result" dbtype="query">
		    		SELECT *
		    		FROM qry
		    		WHERE name in ( #listQualify( names, "'" )# )
		    	</bx:query>
		    </bx:output>
		    		  """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsQuery( result ).size() ).isEqualTo( 2 );
		assertThat( variables.getAsQuery( result ).getRowAsStruct( 0 ).get( "name" ) ).isEqualTo( "Brad" );
		assertThat( variables.getAsQuery( result ).getRowAsStruct( 1 ).get( "name" ) ).isEqualTo( "O'Neil" );
	}

	@Test
	public void testListQualifyWithNonSingleQuoteQualifier() {
		instance.executeSource(
		    """
		    <bx:output>
		    	<bx:set qry = queryNew( "name", "varchar", [["*Brad*,*O'Neil*"]] )>
		    	<bx:set names = "Brad,O'Neil">
		    	<bx:query name="result" dbtype="query">
		    		SELECT *
		    		FROM qry
		    		WHERE name in ( '#listQualify( names, "*" )#' )
		    	</bx:query>
		    </bx:output>
		    		  """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsQuery( result ).size() ).isEqualTo( 1 );
		assertThat( variables.getAsQuery( result ).getRowAsStruct( 0 ).get( "name" ) ).isEqualTo( "*Brad*,*O'Neil*" );
	}

	@Test
	public void testODBCDateBIFs() {
		instance.executeSource(
		    """
		    <bx:output>
		    	<bx:set qry = queryNew( "date", "date", [[now()]] )>
		    	<bx:query name="result" dbtype="query">
		    		SELECT *
		    		FROM qry
		    		WHERE date <= #createODBCDateTime( now() )#
		    			AND date <= #createODBCDate( now() )#
		    			AND date <= #createODBCTime( now() )#
		    	</bx:query>
		    </bx:output>
		    		  """,
		    context, BoxSourceType.BOXTEMPLATE );
	}

	@Test
	public void testODBCDateValue() {
		instance.executeSource(
		    """
		       <bx:output>
		    <!--- remove milliseconds --->
		     <bx:set theDate = now().toString() castas Date >
		       	<bx:set qry = queryNew( "date", "date", [[theDate]] )>
		       	<bx:query name="result" dbtype="query">
		       		SELECT *
		       		FROM qry
		       		WHERE date = #theDate#
		       	</bx:query>
		       </bx:output>
		       		  """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( variables.getAsQuery( result ).size() ).isEqualTo( 1 );
	}

}
