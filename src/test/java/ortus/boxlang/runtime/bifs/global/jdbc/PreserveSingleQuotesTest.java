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
	public void testNormalScript() {
		instance.executeSource(
		    """
		    query name="result" {
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
		       query name="result" {
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
		    	query name="result" {
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
		    query name="result" {
		    	echo( "SELECT *
		    	FROM developers
		    	WHERE name = '" & name & "'" )
		    }
		        		""",
		    context, BoxSourceType.BOXSCRIPT );
		assertThat( variables.getAsQuery( result ).size() ).isEqualTo( 1 );
		assertThat( variables.getAsQuery( result ).getRowAsStruct( 0 ).get( "name" ) ).isEqualTo( "Bob O'Reily" );
	}

}
