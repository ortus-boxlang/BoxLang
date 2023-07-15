package ortus.boxlang.runtime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BootstrapTest {

	@Test
	void appCanGreat() {
		Bootstrap runtime = new Bootstrap();
		assertTrue( runtime.getGreeting().contains( "Hello" ), "greeting should contain 'Hello'" );
	}
}
