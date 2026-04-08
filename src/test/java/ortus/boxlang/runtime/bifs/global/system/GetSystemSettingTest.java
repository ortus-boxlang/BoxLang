package ortus.boxlang.runtime.bifs.global.system;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class GetSystemSettingTest {

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

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It can get a valid system property with any case" )
	@Test
	public void testGetSystemSetting() {
		instance.executeSource(
		    """
		    result = getSystemSetting( "Java.Version" )
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( System.getProperty( "java.version" ) );
	}

	@DisplayName( "It can get a valid system env variables" )
	@Test
	@Disabled( "Not running in CI correctly" )
	public void testGetSystemSettingEnv() {
		instance.executeSource(
		    """
		    result = getSystemSetting( "CLASSPATH" )
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( System.getenv( "CLASSPATH" ) );
	}

	@DisplayName( "It can get an invalid system property with a default value" )
	@Test
	public void testGetSystemSettingWithDefault() {
		instance.executeSource(
		    """
		    result = getSystemSetting( "java.verrsssion", "17" )
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "17" );
	}

	@DisplayName( "It will throw an exception when using an invalid property and no default value" )
	@Test
	public void testGetSystemSettingWithNoDefault() {
		assertThrows( BoxRuntimeException.class, () -> {
			instance.executeSource(
			    """
			    result = getSystemSetting( "java.verrsssion" )
			    """,
			    context );
		} );
	}

	@Test
	public void testDefaultComplexValue() {
		instance.executeSource(
		    """
		    result = getSystemSetting( "sdf", [1,2,3] )
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Array.class );
		Array arr = variables.getAsArray( result );
		assertThat( arr.size() ).isEqualTo( 3 );
		assertThat( arr.get( 0 ) ).isEqualTo( 1 );
		assertThat( arr.get( 1 ) ).isEqualTo( 2 );
		assertThat( arr.get( 2 ) ).isEqualTo( 3 );
	}

	@Test
	public void testBLProviderOverride() {
		System.getProperties().put( "brad-test", "brad-value" );
		instance.executeSource(
		    """
		    getBoxRuntime().getConfiguration().registerSystemSettingProvider( "", name -> name == "mySpecialKey" ? "my special value" : null );
		       result = getSystemSetting( "mySpecialKey" )
		    result2 = getSystemSetting( "brad-test" )

		    	getBoxRuntime().getConfiguration().unregisterSystemSettingProvider( "" );

		    	// Should no longer be found
		    	result3 = getSystemSetting( "mySpecialKey", "brad-default" )
		       """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "my special value" );
		assertThat( variables.getAsString( Key.of( "result2" ) ) ).isEqualTo( "brad-value" );
		assertThat( variables.getAsString( Key.of( "result3" ) ) ).isEqualTo( "brad-default" );
	}

	@Test
	public void testProviderOverrideMockSecretStore() {
		instance.executeSource(
		    """
		       getBoxRuntime().getConfiguration().registerSystemSettingProvider( "secret-provider", name -> {
		    	switch( name ) {
		    		case "DB_PASSWORD":
		    			return "my db pass";
		    			break;
		    		case "MAIL_PASSWORD":
		    			return "my mail pass";
		    			break;
		    		case "API_KEY":
		    			return "my api key";
		    			break;
		    		default:
		    			return null;
		    	}
		    } );
		    result = getSystemSetting( "secret-provider.DB_PASSWORD" )
		       result2 = getSystemSetting( "secret-provider.MAIL_PASSWORD" )
		       result3 = getSystemSetting( "secret-provider.API_KEY" )

		       getBoxRuntime().getConfiguration().unregisterSystemSettingProvider( "secret-provider" );
		          """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "my db pass" );
		assertThat( variables.getAsString( Key.of( "result2" ) ) ).isEqualTo( "my mail pass" );
		assertThat( variables.getAsString( Key.of( "result3" ) ) ).isEqualTo( "my api key" );
	}

	@Test
	public void testJavaProviderOverride() {
		System.getProperties().put( "brad-test", "brad-value" );
		instance.getConfiguration().registerSystemSettingProvider( Key._EMPTY,
		    ( name, context ) -> name == "mySpecialKey" ? "my special value" : null );
		instance.executeSource(
		    """
		    	result = getSystemSetting( "mySpecialKey" )
		    	result2 = getSystemSetting( "brad-test" )
		    """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( "my special value" );
		assertThat( variables.getAsString( Key.of( "result2" ) ) ).isEqualTo( "brad-value" );

		instance.getConfiguration().unregisterSystemSettingProvider( Key._EMPTY );
		instance.executeSource(
		    """
		    	// Should no longer be found
		    	result3 = getSystemSetting( "mySpecialKey", "brad-default" )
		    """,
		    context );
		assertThat( variables.getAsString( Key.of( "result3" ) ) ).isEqualTo( "brad-default" );

	}

}
