package ortus.boxlang.runtime.config.util;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.unmodifiable.IUnmodifiable;
import ortus.boxlang.runtime.util.ConfigSecretUtil;

public class RuntimeSettingsUtilTest {

	@BeforeAll
	public static void setUp() {
		BoxRuntime.getInstance( true );
	}

	@DisplayName( "It deep merges nested structs with the override winning by key" )
	@Test
	public void testDeepMerge() {
		IStruct	base		= Struct.of( "a", Struct.of( "x", 1, "y", 2 ), "keep", "me" );
		IStruct	override	= Struct.of( "a", Struct.of( "x", 9 ), "added", true );

		IStruct	merged		= RuntimeSettingsUtil.merge( base, override );

		assertThat( RuntimeSettingsUtil.get( merged, "a.x" ) ).isEqualTo( 9 );
		assertThat( RuntimeSettingsUtil.get( merged, "a.y" ) ).isEqualTo( 2 );
		assertThat( merged.get( Key.of( "keep" ) ) ).isEqualTo( "me" );
		assertThat( merged.get( Key.of( "added" ) ) ).isEqualTo( true );
		// inputs untouched
		assertThat( RuntimeSettingsUtil.get( base, "a.x" ) ).isEqualTo( 1 );
	}

	@DisplayName( "It replaces arrays and scalars instead of merging them" )
	@Test
	public void testMergeReplacesArraysAndScalars() {
		IStruct	base		= Struct.of( "list", Array.of( 1, 2, 3 ), "a", Struct.of( "x", 1 ) );
		IStruct	override	= Struct.of( "list", Array.of( 9 ), "a", "scalar" );

		IStruct	merged		= RuntimeSettingsUtil.merge( base, override );

		assertThat( ( ( Array ) merged.get( Key.of( "list" ) ) ).size() ).isEqualTo( 1 );
		assertThat( merged.get( Key.of( "a" ) ) ).isEqualTo( "scalar" );
	}

	@DisplayName( "It handles null base or override" )
	@Test
	public void testMergeNulls() {
		assertThat( RuntimeSettingsUtil.merge( null, null ) ).isEmpty();
		assertThat( RuntimeSettingsUtil.merge( null, Struct.of( "a", 1 ) ).get( Key.of( "a" ) ) ).isEqualTo( 1 );
		assertThat( RuntimeSettingsUtil.merge( Struct.of( "a", 1 ), null ).get( Key.of( "a" ) ) ).isEqualTo( 1 );
	}

	@DisplayName( "It freezes the whole tree" )
	@Test
	public void testImmutable() {
		IStruct	merged	= RuntimeSettingsUtil.merge( null, Struct.of( "a", Struct.of( "x", 1 ), "list", Array.of( 1, 2 ) ) );

		IStruct	nested	= ( IStruct ) merged.get( Key.of( "a" ) );
		Array	list	= ( Array ) merged.get( Key.of( "list" ) );

		assertThat( merged ).isInstanceOf( IUnmodifiable.class );
		assertThat( nested ).isInstanceOf( IUnmodifiable.class );
		assertThat( list ).isInstanceOf( IUnmodifiable.class );
		assertThrows( RuntimeException.class, () -> merged.put( Key.of( "z" ), 1 ) );
		assertThrows( RuntimeException.class, () -> nested.put( Key.of( "z" ), 1 ) );
		assertThrows( RuntimeException.class, () -> list.add( 3 ) );
	}

	@DisplayName( "It resolves dotted paths, array indexes and literal dotted keys" )
	@Test
	public void testGet() {
		IStruct settings = RuntimeSettingsUtil.merge( null, Struct.of(
		    "features", Struct.of( "beta", true ),
		    "limits", Array.of( 10, 20, 30 ),
		    "a.b", "literal"
		) );

		assertThat( RuntimeSettingsUtil.get( settings, "features.beta" ) ).isEqualTo( true );
		assertThat( RuntimeSettingsUtil.get( settings, "limits.2" ) ).isEqualTo( 20 );
		assertThat( RuntimeSettingsUtil.get( settings, "a.b" ) ).isEqualTo( "literal" );
		assertThat( RuntimeSettingsUtil.get( settings, "limits.0" ) ).isNull();
		assertThat( RuntimeSettingsUtil.get( settings, "limits.4" ) ).isNull();
		assertThat( RuntimeSettingsUtil.get( settings, "features.nope" ) ).isNull();
		assertThat( RuntimeSettingsUtil.get( settings, "nope.deeper" ) ).isNull();
		assertThat( RuntimeSettingsUtil.has( settings, "features.beta" ) ).isTrue();
		assertThat( RuntimeSettingsUtil.has( settings, "features.nope" ) ).isFalse();
		assertThat( RuntimeSettingsUtil.get( null, "x" ) ).isNull();
	}

	@DisplayName( "It resolves placeholders, defaults and secrets at any depth without mutating the source" )
	@Test
	public void testResolve() {
		String	secret		= ConfigSecretUtil.encryptWithPrefix( "s3cret" );
		IStruct	raw			= Struct.of(
		    "dir", "${user-dir}/x",
		    "dflt", "${RS_UTIL_NOT_DEFINED:fallback}",
		    "secret", secret,
		    "nested", Struct.of( "list", Array.of( "${RS_UTIL_NOT_DEFINED:in-array}", secret ) ),
		    "number", 42,
		    "bool", true
		);

		IStruct	resolved	= RuntimeSettingsUtil.resolve( raw );

		assertThat( resolved.get( Key.of( "dir" ) ) ).isEqualTo( System.getProperty( "user.dir" ) + "/x" );
		assertThat( resolved.get( Key.of( "dflt" ) ) ).isEqualTo( "fallback" );
		assertThat( resolved.get( Key.of( "secret" ) ) ).isEqualTo( "s3cret" );
		assertThat( RuntimeSettingsUtil.get( resolved, "nested.list.1" ) ).isEqualTo( "in-array" );
		assertThat( RuntimeSettingsUtil.get( resolved, "nested.list.2" ) ).isEqualTo( "s3cret" );
		assertThat( resolved.get( Key.of( "number" ) ) ).isEqualTo( 42 );
		assertThat( resolved.get( Key.of( "bool" ) ) ).isEqualTo( true );
		assertThat( resolved ).isInstanceOf( IUnmodifiable.class );
		// source untouched
		assertThat( raw.get( Key.of( "dflt" ) ) ).isEqualTo( "${RS_UTIL_NOT_DEFINED:fallback}" );
		assertThat( raw.get( Key.of( "secret" ) ) ).isEqualTo( secret );
	}

	@DisplayName( "It resolves a placeholder that points at an environment variable" )
	@Test
	public void testResolveEnv() {
		String path = System.getenv( "PATH" );
		if ( path == null ) {
			return;
		}
		IStruct resolved = RuntimeSettingsUtil.resolve( Struct.of( "p", "${env.PATH}" ) );
		assertThat( resolved.get( Key.of( "p" ) ) ).isEqualTo( path );
	}

	@DisplayName( "resolveInPlace only touches runtimeSettings and ignores non-structs" )
	@Test
	public void testResolveInPlace() {
		IStruct app = Struct.of(
		    Key.runtimeSettings, Struct.of( "a", "${RS_UTIL_NOT_DEFINED:ok}" ),
		    Key.of( "other" ), "${RS_UTIL_NOT_DEFINED:untouched}"
		);
		RuntimeSettingsUtil.resolveInPlace( app );
		assertThat( RuntimeSettingsUtil.get( ( IStruct ) app.get( Key.runtimeSettings ), "a" ) ).isEqualTo( "ok" );
		assertThat( app.get( Key.of( "other" ) ) ).isEqualTo( "${RS_UTIL_NOT_DEFINED:untouched}" );

		IStruct bad = Struct.of( Key.runtimeSettings, "not a struct" );
		RuntimeSettingsUtil.resolveInPlace( bad );
		assertThat( bad.get( Key.runtimeSettings ) ).isEqualTo( "not a struct" );
	}
}
