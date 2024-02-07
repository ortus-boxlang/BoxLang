package TestCases.debugger;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.jr.ob.JSONObjectException;

import ortus.boxlang.debugger.AdapterProtocolMessageReader;
import ortus.boxlang.debugger.DebugAdapter;
import ortus.boxlang.debugger.IAdapterProtocolMessage;
import ortus.boxlang.runtime.util.JsonUtil;

public class IntegrationTest {

	private AdapterProtocolMessageReader runDebugger( Map<String, Object> map ) throws JSONObjectException, IOException, InterruptedException {
		String					jsonMessage	= JsonUtil.getJsonBuilder().asString( map );
		// @formatter:off
		// prettier-ignore
		String test = String.format("""
Content-Length: %d
	
%s""", jsonMessage.getBytes().length, jsonMessage );
	// @formatter:on
		ByteArrayOutputStream	output		= new ByteArrayOutputStream();
		Thread					task		= new Thread( new Runnable() {

												@Override
												public void run() {
													new DebugAdapter( new ByteArrayInputStream( test.getBytes() ), output );
												}

											} );
		task.start();
		Thread.sleep( 2000 );
		task.interrupt();

		AdapterProtocolMessageReader reader = new AdapterProtocolMessageReader( new ByteArrayInputStream( output.toByteArray() ) );
		reader.throwOnUnregisteredCommand = false;

		return reader;
	}

	@DisplayName( "It should respond to an initialize request with capabilities" )
	@Test
	public void testRespondToInitializeRequest() {

		Map<String, Object> initializeRequest = new HashMap<String, Object>();
		initializeRequest.put( "command", "initialize" );
		initializeRequest.put( "type", "request" );
		initializeRequest.put( "seq", 1 );
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put( "clientID", "vscode" );
		arguments.put( "clientName", "Visual Studio Code" );
		arguments.put( "adapterID", "boxlang" );
		arguments.put( "pathFormat", "path" );
		arguments.put( "linesStartAt1", true );
		arguments.put( "columnsStartAt1", true );
		arguments.put( "supportsVariableType", true );
		arguments.put( "supportsVariablePaging", true );
		arguments.put( "supportsRunInTerminalRequest", true );
		arguments.put( "locale", "en" );
		arguments.put( "supportsProgressReporting", true );
		arguments.put( "supportsInvalidatedEvent", true );
		arguments.put( "supportsMemoryReferences", true );
		arguments.put( "supportsArgsCanBeInterpretedByShell", true );
		arguments.put( "supportsMemoryEvent", true );
		arguments.put( "supportsStartDebuggingRequest", true );
		initializeRequest.put( "arguments", arguments );

		try {
			AdapterProtocolMessageReader	reader	= runDebugger( initializeRequest );
			IAdapterProtocolMessage			message	= reader.read();
			Map<String, Object>				data	= message.getRawMessageData();

			assertThat( data.get( "success" ) ).isEqualTo( true );
			assertThat( data.get( "type" ) ).isEqualTo( "response" );
			assertThat( data.get( "request_seq" ) ).isEqualTo( 1 );
		} catch ( Exception e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
