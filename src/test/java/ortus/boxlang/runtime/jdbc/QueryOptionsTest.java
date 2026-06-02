package ortus.boxlang.runtime.jdbc;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
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

	@DisplayName( "Test string property casters" )
	@Test
	public void testStringCasting() {
		Struct optionsStruct = new Struct();
		optionsStruct.put( Key.password, 12345 );
		optionsStruct.put( Key.dbtype, 50.5 );
		optionsStruct.put( Key.username, true );
		optionsStruct.put( Key.result, 999 );
		optionsStruct.put( Key.cacheKey, 1000 );
		optionsStruct.put( Key.returnType, "array" );

		QueryOptions options = new QueryOptions( optionsStruct );

		assertThat( options.password ).isEqualTo( "12345" );
		assertThat( options.dbtype ).isEqualTo( "50.5" );
		assertThat( options.username ).isEqualTo( "true" );
		assertThat( options.resultVariableName ).isEqualTo( "999" );
		assertThat( options.cacheKey ).isEqualTo( "1000" );
	}

	@DisplayName( "Test context-aware constructor applies config defaults" )
	@Test
	public void testContextAwareDefaults() {
		// Mock a context that returns a config struct with query defaults
		IBoxContext	mockContext	= mock( IBoxContext.class );
		IStruct		config		= Struct.of(
		    Key.timeout, 30,
		    Key.returnType, "array",
		    Key.fetchSize, 500,
		    Key.cacheProvider, "redis"
		);
		when( mockContext.getConfigItems( Key.applicationSettings, Key.queryOptions ) ).thenReturn( config );

		QueryOptions options = new QueryOptions( new Struct(), mockContext );

		assertThat( options.queryTimeout ).isEqualTo( 30 );
		assertThat( options.getReturnType() ).isEqualTo( "array" );
		assertThat( options.fetchSize ).isEqualTo( 500 );
		assertThat( options.cacheProvider ).isEqualTo( "redis" );
	}

	@DisplayName( "Test context-aware constructor: user options override config defaults" )
	@Test
	public void testContextAwareUserOverridesDefaults() {
		IBoxContext	mockContext	= mock( IBoxContext.class );
		IStruct		config		= Struct.of(
		    Key.timeout, 30,
		    Key.returnType, "array"
		);
		when( mockContext.getConfigItems( Key.applicationSettings, Key.queryOptions ) ).thenReturn( config );

		// User specifies their own options that override the defaults
		IStruct			userOptions	= Struct.of(
		    Key.timeout, 10,
		    Key.fetchSize, 200
		);
		QueryOptions	options		= new QueryOptions( userOptions, mockContext );

		// User value wins
		assertThat( options.queryTimeout ).isEqualTo( 10 );
		// Config default used since user didn't specify
		assertThat( options.getReturnType() ).isEqualTo( "array" );
		// User value
		assertThat( options.fetchSize ).isEqualTo( 200 );
	}

	@DisplayName( "Test context-aware constructor: maxRows=0 from config is normalized to -1" )
	@Test
	public void testContextAwareMaxRowsZeroNormalized() {
		IBoxContext	mockContext	= mock( IBoxContext.class );
		IStruct		config		= Struct.of(
		    Key.maxRows, 0
		);
		when( mockContext.getConfigItems( Key.applicationSettings, Key.queryOptions ) ).thenReturn( config );

		QueryOptions options = new QueryOptions( new Struct(), mockContext );

		// 0 from config should be normalized to -1 (meaning "all rows")
		assertThat( options.maxRows ).isEqualTo( -1L );
	}

	@DisplayName( "Test context-aware constructor: maxRows from user overrides normalized config default" )
	@Test
	public void testContextAwareMaxRowsUserOverride() {
		IBoxContext	mockContext	= mock( IBoxContext.class );
		IStruct		config		= Struct.of(
		    Key.maxRows, 0
		);
		when( mockContext.getConfigItems( Key.applicationSettings, Key.queryOptions ) ).thenReturn( config );

		IStruct			userOptions	= Struct.of(
		    Key.maxRows, 50
		);
		QueryOptions	options		= new QueryOptions( userOptions, mockContext );

		// User-specified value wins and is not normalized to -1
		assertThat( options.maxRows ).isEqualTo( 50L );
	}

	@DisplayName( "Test context-aware constructor: falls back when no config defaults available" )
	@Test
	public void testContextAwareNoConfigDefaults() {
		IBoxContext mockContext = mock( IBoxContext.class );
		when( mockContext.getConfigItems( Key.applicationSettings, Key.queryOptions ) ).thenReturn( null );

		QueryOptions options = new QueryOptions( new Struct(), mockContext );

		// Should behave like the old constructor — all defaults
		assertThat( options.queryTimeout ).isNull();
		assertThat( options.getReturnType() ).isEqualTo( "query" );
		assertThat( options.fetchSize ).isEqualTo( 0 );
		assertThat( options.maxRows ).isEqualTo( -1L );
	}

	@DisplayName( "Test context-aware constructor: explicit maxRows=0 in user options stays 0 (no normalization for user value)" )
	@Test
	public void testMaxRowsZeroExplicit() {
		// maxRows=0 explicitly passed by user (no config) should still be normalized to -1
		IStruct			userOptions	= Struct.of(
		    Key.maxRows, 0
		);
		QueryOptions	options		= new QueryOptions( userOptions );

		assertThat( options.maxRows ).isEqualTo( -1L );
	}

}
