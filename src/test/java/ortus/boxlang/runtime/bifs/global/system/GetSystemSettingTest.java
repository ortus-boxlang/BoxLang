package ortus.boxlang.runtime.bifs.global.system;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		variables.clear();
	}

	@DisplayName( "It can get a valid system property" )
	@Test
	public void testGetSystemSetting() {
		instance.executeSource(
		    """
		    result = getSystemSetting( "java.version" )
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( System.getProperty( "java.version" ) );
	}

	@DisplayName( "It can get a valid system env variables" )
	@Test
	public void testGetSystemSettingEnv() {
		instance.executeSource(
		    """
		    result = getSystemSetting( "PATH" )
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( System.getenv( "PATH" ) );
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
