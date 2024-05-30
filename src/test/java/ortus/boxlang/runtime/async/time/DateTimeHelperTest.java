package ortus.boxlang.runtime.async.time;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class DateTimeHelperTest {

	DateTimeHelper dateTimeHelper;

	@BeforeEach
	void setUp() {
		// Create a test fixture
	}

	@DisplayName( "Testing dateTimeAdd method" )
	@Test
	void testDateTimeAdd() {
		LocalDateTime		currentDateTime		= LocalDateTime.now().plusHours( 1L );

		DateTimeFormatter	formatter			= DateTimeFormatter.ofPattern( "yyyy-MM-dd HH:mm" );
		String				formattedDateTime1	= currentDateTime.format( formatter );

		LocalDateTime		result				= DateTimeHelper.dateTimeAdd( LocalDateTime.now(), 1, TimeUnit.HOURS );
		assertThat( result ).isInstanceOf( LocalDateTime.class );

		String formattedDateTime2 = result.format( formatter );
		assertEquals( formattedDateTime1, formattedDateTime2 );
	}

}