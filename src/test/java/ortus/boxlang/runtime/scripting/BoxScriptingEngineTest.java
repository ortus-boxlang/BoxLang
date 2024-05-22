package ortus.boxlang.runtime.scripting;

import static com.google.common.truth.Truth.assertThat;

import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BoxScriptingEngineTest {

	static BoxScriptingEngine engine;

	@BeforeAll
	public static void setUp() {
		engine = new BoxScriptingEngine( new BoxScriptingFactory() );
	}

	@DisplayName( "Can build a new engine" )
	@Test
	public void testEngine() {
		assertThat( engine ).isNotNull();
	}

	@DisplayName( "Can create bindings" )
	@Test
	public void testBindings() {
		Bindings bindings = engine.createBindings();
		assertThat( bindings ).isInstanceOf( SimpleBindings.class );
		assertThat( bindings.size() ).isEqualTo( 0 );
	}

}
