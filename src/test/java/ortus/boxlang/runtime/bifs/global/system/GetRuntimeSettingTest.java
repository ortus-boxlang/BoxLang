package ortus.boxlang.runtime.bifs.global.system;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.Configuration;
import ortus.boxlang.runtime.config.util.RuntimeSettingsUtil;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.RequestScope;
import ortus.boxlang.runtime.scopes.ServerScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.unmodifiable.IUnmodifiable;
import ortus.boxlang.runtime.util.ConfigSecretUtil;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class GetRuntimeSettingTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	IStruct				originalSettings;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		originalSettings	= instance.getConfiguration().runtimeSettings;
		context				= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables			= context.getScopeNearby( VariablesScope.name );
	}

	@AfterEach
	public void teardownEach() {
		instance.getConfiguration().runtimeSettings = originalSettings;
		IScope server = instance.getRuntimeContext().getScopeNearby( ServerScope.name );
		server.remove( Key.of( "rsSecret" ) );
		server.remove( Key.of( "rsCounter" ) );
	}

	private void setGlobal( IStruct settings ) {
		instance.getConfiguration().runtimeSettings = RuntimeSettingsUtil.merge( null, settings );
	}

	@DisplayName( "Configuration loads, merges and exports runtimeSettings as an unmodifiable struct" )
	@Test
	public void testConfigurationProcess() {
		Configuration config = new Configuration();
		config.process( Struct.of( Key.runtimeSettings, Struct.of( "a", Struct.of( "x", 1, "y", 2 ), "keep", true ) ) );
		config.process( Struct.of( Key.runtimeSettings, Struct.of( "a", Struct.of( "x", 9 ) ) ) );

		assertThat( RuntimeSettingsUtil.get( config.runtimeSettings, "a.x" ) ).isEqualTo( 9 );
		assertThat( RuntimeSettingsUtil.get( config.runtimeSettings, "a.y" ) ).isEqualTo( 2 );
		assertThat( config.runtimeSettings ).isInstanceOf( IUnmodifiable.class );
		assertThat( RuntimeSettingsUtil.get( config.asStruct().getAsStruct( Key.runtimeSettings ), "keep" ) ).isEqualTo( true );
	}

	@DisplayName( "Configuration ignores a runtimeSettings value that is not a struct" )
	@Test
	public void testConfigurationNonStruct() {
		Configuration config = new Configuration();
		config.process( Struct.of( Key.runtimeSettings, "nope" ) );
		assertThat( config.runtimeSettings ).isEmpty();
	}

	@DisplayName( "It reads a global setting by key" )
	@Test
	public void testGlobal() {
		setGlobal( Struct.of( "name", "BoxLang", "features", Struct.of( "beta", true ), "limits", Array.of( 10, 20 ) ) );
		instance.executeSource(
		    """
		    result = getRuntimeSetting( "name" )
		    r2 = getRuntimeSetting( "features.beta" )
		    r3 = getRuntimeSetting( "limits.2" )
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "BoxLang" );
		assertThat( variables.get( Key.of( "r2" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "r3" ) ) ).isEqualTo( 20 );
	}

	@DisplayName( "It returns the whole struct when no key is passed" )
	@Test
	public void testAll() {
		setGlobal( Struct.of( "a", 1, "b", 2 ) );
		instance.executeSource( "result = getRuntimeSetting()", context );
		IStruct all = ( IStruct ) variables.get( result );
		assertThat( all.get( Key.of( "a" ) ) ).isEqualTo( 1 );
		assertThat( all.get( Key.of( "b" ) ) ).isEqualTo( 2 );
	}

	@DisplayName( "It returns the default for a missing key" )
	@Test
	public void testDefault() {
		setGlobal( Struct.of( "a", 1 ) );
		instance.executeSource(
		    """
		    result = getRuntimeSetting( "nope", "fallback" )
		    r2 = getRuntimeSetting( "a.deeper", 0 )
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "fallback" );
		assertThat( variables.get( Key.of( "r2" ) ) ).isEqualTo( 0 );
	}

	@DisplayName( "It throws when the key is missing and there is no default" )
	@Test
	public void testMissingThrows() {
		setGlobal( Struct.of( "a", 1 ) );
		assertThrows( BoxRuntimeException.class, () -> instance.executeSource( "result = getRuntimeSetting( 'nope' )", context ) );
	}

	@DisplayName( "It returns an empty struct when there are no settings" )
	@Test
	public void testEmpty() {
		instance.getConfiguration().runtimeSettings = Struct.EMPTY;
		instance.executeSource( "result = getRuntimeSetting()", context );
		assertThat( ( IStruct ) variables.get( result ) ).isEmpty();
	}

	@DisplayName( "Returned values are read-only" )
	@Test
	public void testReadOnly() {
		setGlobal( Struct.of( "features", Struct.of( "beta", true ) ) );
		assertThrows( RuntimeException.class, () -> instance.executeSource(
		    """
		    s = getRuntimeSetting( "features" )
		    s.beta = false
		    """,
		    context ) );
		assertThrows( RuntimeException.class, () -> instance.executeSource(
		    """
		    s = getRuntimeSetting()
		    s.newKey = 1
		    """,
		    context ) );
	}

	@DisplayName( "bx:application runtimeSettings deep merges over the global settings and resolves placeholders and secrets" )
	@Test
	public void testApplicationComponentOverride() {
		setGlobal( Struct.of( "name", "global", "features", Struct.of( "beta", false, "level", 1 ), "onlyGlobal", "g" ) );
		variables.put( Key.of( "enc" ), ConfigSecretUtil.encryptWithPrefix( "app-secret" ) );

		instance.executeSource(
		    """
		    bx:application name="runtimeSettingsComponent"
		        runtimeSettings={
		            features : { level : 7 },
		            secret : enc,
		            dir : "${user-dir}/x",
		            dflt : "${RS_COMPONENT_NOT_DEFINED:fallback}"
		        };
		    result = getRuntimeSetting()
		    """,
		    context );

		IStruct all = ( IStruct ) variables.get( result );
		assertThat( RuntimeSettingsUtil.get( all, "name" ) ).isEqualTo( "global" );
		assertThat( RuntimeSettingsUtil.get( all, "onlyGlobal" ) ).isEqualTo( "g" );
		assertThat( RuntimeSettingsUtil.get( all, "features.beta" ) ).isEqualTo( false );
		assertThat( RuntimeSettingsUtil.get( all, "features.level" ) ).isEqualTo( 7 );
		assertThat( RuntimeSettingsUtil.get( all, "secret" ) ).isEqualTo( "app-secret" );
		assertThat( RuntimeSettingsUtil.get( all, "dir" ) ).isEqualTo( System.getProperty( "user.dir" ) + "/x" );
		assertThat( RuntimeSettingsUtil.get( all, "dflt" ) ).isEqualTo( "fallback" );
		assertThat( all ).isInstanceOf( IUnmodifiable.class );
	}

	private IBoxContext appContext() {
		String root = new File( "src/test/java/TestCases/applications/runtimeSettings/" ).getAbsolutePath();
		return new ScriptingRequestBoxContext( new ortus.boxlang.runtime.context.ConfigOverrideBoxContext( instance.getRuntimeContext(), config -> {
			config.getAsStruct( Key.mappings ).put( "/", ortus.boxlang.runtime.util.Mapping.ofExternal( "/", root ) );
			return config;
		} ), false ).loadApplicationDescriptor( FileSystemUtil.createFileUri( "index.bxm" ) );
	}

	@DisplayName( "this.runtimeSettings in Application.bx merges over global, and is re-read and re-resolved on every request" )
	@Test
	public void testApplicationDescriptorPerRequest() {
		IScope server = instance.getRuntimeContext().getScopeNearby( ServerScope.name );
		setGlobal( Struct.of( "apiUrl", "global", "globalOnly", "g", "features", Struct.of( "beta", true ) ) );
		server.put( Key.of( "rsSecret" ), ConfigSecretUtil.encryptWithPrefix( "descriptor-secret" ) );

		// Request 1
		server.put( Key.of( "rsCounter" ), 1 );
		IBoxContext request1 = appContext();
		instance.executeTemplate( "index.bxm", request1 );
		IScope	req1	= request1.getScopeNearby( RequestScope.name );
		IStruct	all1	= ( IStruct ) req1.get( Key.of( "settings" ) );

		assertThat( RuntimeSettingsUtil.get( all1, "apiUrl" ) ).isEqualTo( System.getProperty( "user.dir" ) + "/api" );
		assertThat( RuntimeSettingsUtil.get( all1, "fallback" ) ).isEqualTo( "default-value" );
		assertThat( RuntimeSettingsUtil.get( all1, "secret" ) ).isEqualTo( "descriptor-secret" );
		assertThat( RuntimeSettingsUtil.get( all1, "counter" ) ).isEqualTo( 1 );
		assertThat( RuntimeSettingsUtil.get( all1, "globalOnly" ) ).isEqualTo( "g" );
		// deep merge keeps the global key next to the app keys
		assertThat( RuntimeSettingsUtil.get( all1, "features.beta" ) ).isEqualTo( false );
		assertThat( RuntimeSettingsUtil.get( all1, "features.level" ) ).isEqualTo( 5 );
		assertThat( req1.get( Key.of( "level" ) ) ).isEqualTo( 5 );
		assertThat( req1.get( Key.of( "second" ) ) ).isEqualTo( 2 );
		assertThat( req1.get( Key.of( "missing" ) ) ).isEqualTo( "dflt" );

		// Request 2: the descriptor value changed, the next request must see it
		server.put( Key.of( "rsCounter" ), 2 );
		IBoxContext request2 = appContext();
		instance.executeTemplate( "index.bxm", request2 );
		IStruct all2 = ( IStruct ) request2.getScopeNearby( RequestScope.name ).get( Key.of( "settings" ) );
		assertThat( RuntimeSettingsUtil.get( all2, "counter" ) ).isEqualTo( 2 );
	}
}
