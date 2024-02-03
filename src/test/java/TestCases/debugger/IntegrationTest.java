package TestCases.debugger;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.debugger.AdapterProtocolMessageReader;
import ortus.boxlang.debugger.DebugAdapter;
import ortus.boxlang.debugger.IAdapterProtocolMessage;

public class IntegrationTest {

	@DisplayName( "It should respond to an initialize request with capabilities" )
	@Test
	public void testRespondToInitializeRequest() {

		// @formatter:off
		// prettier-ignore
		String jsonMessge = """
{"command":"initialize","arguments":{"clientID":"vscode","clientName":"Visual Studio Code","adapterID":"boxlang","pathFormat":"path","linesStartAt1":true,"columnsStartAt1":true,"supportsVariableType":true,"supportsVariablePaging":true,"supportsRunInTerminalRequest":true,"locale":"en","supportsProgressReporting":true,"supportsInvalidatedEvent":true,"supportsMemoryReferences":true,"supportsArgsCanBeInterpretedByShell":true,"supportsMemoryEvent":true,"supportsStartDebuggingRequest":true},"type":"request","seq":1}""";
String test = String.format("""
Content-Length: %d

%s""", jsonMessge.getBytes().length, jsonMessge );
// @formatter:on
		ByteArrayOutputStream	output		= new ByteArrayOutputStream();
		Thread					task		= new Thread( new Runnable() {

												@Override
												public void run() {
													new DebugAdapter( new ByteArrayInputStream( test.getBytes() ), output );
												}

											} );
		task.start();
		try {
			Thread.sleep( 2000 );
		} catch ( InterruptedException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		task.interrupt();

		try {
			AdapterProtocolMessageReader reader = new AdapterProtocolMessageReader( new ByteArrayInputStream( output.toByteArray() ) );
			reader.throwOnUnregisteredCommand = false;
			IAdapterProtocolMessage	message	= reader.read();
			Map<String, Object>		data	= message.getRawMessageData();

			assertThat( data.get( "success" ) ).isEqualTo( true );
			assertThat( data.get( "type" ) ).isEqualTo( "response" );
			assertThat( data.get( "request_seq" ) ).isEqualTo( 1 );
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
