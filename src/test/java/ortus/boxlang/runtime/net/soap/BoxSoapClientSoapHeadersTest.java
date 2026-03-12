package ortus.boxlang.runtime.net.soap;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.HttpService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class BoxSoapClientSoapHeadersTest {

	static BoxRuntime instance;
	private IBoxContext context;
	private HttpService httpService;
	private static String calculatorWsdlUrl;

	@BeforeAll
	public static void setUpAll() {
		instance = BoxRuntime.getInstance( true );
		calculatorWsdlUrl = Paths.get( "src/test/resources/wsdl/calculator.wsdl" )
		    .toAbsolutePath()
		    .toUri()
		    .toString();
	}

	@BeforeEach
	public void setUp() {
		this.context = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		this.httpService = BoxRuntime.getInstance().getHttpService();
		this.httpService.clearAllSoapClients();
	}

	@AfterEach
	public void tearDown() {
		this.httpService.clearAllSoapClients();
	}

	@DisplayName( "withSoapHeaders accepts simple String header" )
	@Test
	void withSoapHeaders_string_accepts() {
		BoxSoapClient client = httpService.getOrCreateSoapClient( calculatorWsdlUrl, context );

		client.withSoapHeaders(
		    Struct.of( Key.of( "AuthToken" ), "abc123" )
		);

		assertThat( client ).isNotNull();
	}

	@DisplayName( "withSoapHeaders throws on null headers" )
	@Test
	void withSoapHeaders_null_throws() {
		BoxSoapClient client = httpService.getOrCreateSoapClient( calculatorWsdlUrl, context );
		org.junit.jupiter.api.Assertions.assertThrows( BoxRuntimeException.class, () -> client.withSoapHeaders( null ) );
	}

	@DisplayName( "withSoapHeaders throws on non-scalar value" )
	@Test
	void withSoapHeaders_non_scalar_value_throws() {
		BoxSoapClient client = httpService.getOrCreateSoapClient( calculatorWsdlUrl, context );
		IStruct headers = Struct.of( Key.of( "Nested" ), new Struct() );

		org.junit.jupiter.api.Assertions.assertThrows( BoxRuntimeException.class, () -> client.withSoapHeaders( headers ) );
	}

	@DisplayName( "withSoapHeaders accepts number value" )
	@Test
	void withSoapHeaders_number_accepts() {
		BoxSoapClient client = httpService.getOrCreateSoapClient( calculatorWsdlUrl, context );

		client.withSoapHeaders(
		    Struct.of( Key.of( "RetryCount" ), 3 )
		);

		assertThat( client ).isNotNull();
	}

	@DisplayName( "withSoapHeaders accepts boolean value" )
	@Test
	void withSoapHeaders_boolean_accepts() {
		BoxSoapClient client = httpService.getOrCreateSoapClient( calculatorWsdlUrl, context );

		client.withSoapHeaders(
		    Struct.of( Key.of( "Enabled" ), true )
		);

		assertThat( client ).isNotNull();
	}

	@DisplayName( "withSoapHeaders accepts null as a header value" )
	@Test
	void withSoapHeaders_nullValue_accepts() {
		BoxSoapClient client = httpService.getOrCreateSoapClient( calculatorWsdlUrl, context );

		client.withSoapHeaders(
		    Struct.of( Key.of( "OptionalNote" ), null )
		);

		assertThat( client ).isNotNull();
	}

	@DisplayName( "withSoapHeaders accepts multiple header keys" )
	@Test
	void withSoapHeaders_multipleKeys_accepts() {
		BoxSoapClient client = httpService.getOrCreateSoapClient( calculatorWsdlUrl, context );

		client.withSoapHeaders(
		    Struct.of(
		        Key.of( "AuthToken" ), "abc123",
		        Key.of( "RetryCount" ), 2,
		        Key.of( "Enabled" ), false
		    )
		);

		assertThat( client ).isNotNull();
	}

	@DisplayName( "no headers set and withSoapHeaders never called will not throw" )
	@Test
	void noHeaders_withSoapHeadersNeverCalled_doesNotThrow() {
		BoxSoapClient client = httpService.getOrCreateSoapClient( calculatorWsdlUrl, context );
		assertThat( client ).isNotNull();
	}
}