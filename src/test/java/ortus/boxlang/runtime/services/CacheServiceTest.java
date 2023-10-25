package ortus.boxlang.runtime.services;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Spy;

import ortus.boxlang.runtime.BoxRuntime;

public class CacheServiceTest {

	CacheService		service;

	@Spy
	@InjectMocks
	private BoxRuntime	runtime;

	@BeforeEach
	public void setupBeforeEach() {
		service = new CacheService( runtime );
	}

	@DisplayName( "Test it can get an instance of the service" )
	@Test
	void testItCanGetInstance() {
		assertThat( service ).isNotNull();
	}
}
