package ortus.boxlang.runtime.jdbc;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;

public class QueryOptionsTest {

	@DisplayName( "Test empty struct defaults" )
	@Test
	public void testEmptyStructDefaults() {
		QueryOptions options = new QueryOptions( new Struct() );

		assertThat( options.datasource ).isNull();
		assertThat( options.resultVariableName ).isNull();
		assertThat( options.getReturnType() ).isEqualTo( "query" );
		assertThat( options.username ).isNull();
		assertThat( options.password ).isNull();
		assertThat( options.queryTimeout ).isNull();
		assertThat( options.maxRows ).isEqualTo( -1L );
		assertThat( options.fetchSize ).isEqualTo( 0 );
		assertThat( options.cache ).isFalse();
		assertThat( options.cacheKey ).isNull();
		assertThat( options.cacheTimeout ).isNull();
		assertThat( options.cacheLastAccessTimeout ).isNull();
	}

	@DisplayName( "Test numeric property casters" )
	@Test
	public void testCastingNumbers() {
		Struct optionsStruct = new Struct();
		optionsStruct.put( Key.timeout, "7" );
		optionsStruct.put( Key.maxRows, "2.0" );
		optionsStruct.put( Key.fetchSize, "100" );

		QueryOptions options = new QueryOptions( optionsStruct );

		assertThat( options.queryTimeout ).isEqualTo( 7 );
		assertThat( options.maxRows ).isEqualTo( 2L );
		assertThat( options.fetchSize ).isEqualTo( 100 );
	}

	@DisplayName( "Test 'cache' property boolean cast" )
	@Test
	public void testCacheBooleanCast() {
		Struct optionsStruct = new Struct();
		optionsStruct.put( Key.cache, "true" );
		QueryOptions options1 = new QueryOptions( optionsStruct );
		assertThat( options1.cache ).isTrue();

		optionsStruct.put( Key.cache, "yes" );
		QueryOptions options2 = new QueryOptions( optionsStruct );
		assertThat( options2.cache ).isTrue();

		optionsStruct.put( Key.cache, 1 );
		QueryOptions options3 = new QueryOptions( optionsStruct );
		assertThat( options3.cache ).isTrue();

		optionsStruct.put( Key.cache, "false" );
		QueryOptions options4 = new QueryOptions( optionsStruct );
		assertThat( options4.cache ).isFalse();
	}

}
