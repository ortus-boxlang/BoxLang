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

}
