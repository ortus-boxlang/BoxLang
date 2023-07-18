package ortus.boxlang.runtime;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

class BootstrapTest {

	@Test
	void appCanGreat() {
		Bootstrap runtime = new Bootstrap();
		assertThat( runtime.getGreeting() ).contains( "Hello" );
	}
}
