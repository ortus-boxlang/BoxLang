/**
 * Tests for mappings processing in Configuration
 */
package ortus.boxlang.runtime.config.segments;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.Configuration;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.util.Mapping;

class MappingsConfigTest {

	@BeforeAll
	public static void setUp() {
		BoxRuntime.getInstance( true );
	}

	@DisplayName( "Simple mapping registers path and external defaults to true" )
	@Test
	void testSimpleMappingRegistersAsExternal() {
		IStruct mappings = Struct.ofNonConcurrent( Key.mappings, Struct.ofNonConcurrent(
		    Key.of( "/app" ), "${user-dir}" ) );
		mappings = PlaceholderHelper.resolveAll( mappings );

		Configuration	cfg	= new Configuration().process( mappings );

		// Mapping should be present
		Mapping			map	= cfg.mappings.getAs( Mapping.class, Key.of( "/app/" ) );
		assertThat( map ).isNotNull();
		assertThat( map.path() ).isEqualTo( System.getProperty( "user.dir" ) );
		assertThat( map.external() ).isTrue();
	}

	@DisplayName( "Complex mapping with explicit external and placeholders" )
	@Test
	void testComplexMappingWithExternalAndPlaceholders() {
		IStruct mappings = Struct.ofNonConcurrent( Key.mappings, Struct.ofNonConcurrent(
		    Key.of( "/site" ), Struct.ofNonConcurrent(
		        Key.of( "path" ), "${user-dir}/site",
		        Key.of( "external" ), false
		    )
		) );
		mappings = PlaceholderHelper.resolveAll( mappings );

		Configuration	cfg	= new Configuration().process( mappings );
		Mapping			map	= cfg.mappings.getAs( Mapping.class, Key.of( "/site/" ) );
		assertThat( map ).isNotNull();
		assertThat( map.path() ).isEqualTo( System.getProperty( "user.dir" ) + File.separator + "site" );
		// external value should be false
		assertThat( map.external() ).isFalse();
	}

	@DisplayName( "Complex mapping missing path is stopped" )
	@Test
	void testComplexMappingMissingPathIgnored() {
		IStruct		mappings	= Struct.ofNonConcurrent( Key.mappings, Struct.ofNonConcurrent(
		    Key.of( "/bad" ), Struct.ofNonConcurrent(
		        Key.of( "external" ), false
		    )
		) );

		Throwable	t			= assertThrows( Throwable.class, () -> new Configuration().process( mappings ) );
		assertThat( t.getMessage() ).contains( "Path is required" );

	}
}
