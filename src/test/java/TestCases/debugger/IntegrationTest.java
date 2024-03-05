package TestCases.debugger;

import static TestCases.debugger.DebugMessages.delayStep;
import static TestCases.debugger.DebugMessages.readMessageStep;
import static TestCases.debugger.DebugMessages.sendMessageStep;
import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.jr.ob.JSONObjectException;

import TestCases.debugger.DebugMessages.TriConsumer;
import ortus.boxlang.debugger.AdapterProtocolMessageReader;
import ortus.boxlang.debugger.DebugAdapter;

public class IntegrationTest {

	private void runDebugger( List<TriConsumer<byte[], ByteArrayInputStream, AdapterProtocolMessageReader>> steps )
	    throws JSONObjectException, IOException, InterruptedException {
		byte[]					buffer		= new byte[ 2048 ];
		ByteArrayInputStream	inputStream	= new ByteArrayInputStream( buffer );
		ByteArrayOutputStream	output		= new ByteArrayOutputStream();
		Thread					task		= new Thread( new Runnable() {

												@Override
												public void run() {
													try {
														Thread.sleep( 500 );
														new DebugAdapter( inputStream, output );
													} catch ( InterruptedException e ) {
														// TODO Auto-generated catch block
														e.printStackTrace();
													}

												}

											} );

		task.start();

		for ( TriConsumer<byte[], ByteArrayInputStream, AdapterProtocolMessageReader> step : steps ) {
			AdapterProtocolMessageReader reader = new AdapterProtocolMessageReader( new ByteArrayInputStream( output.toByteArray() ) );
			reader.throwOnUnregisteredCommand = false;
			step.accept( buffer, inputStream, reader );
		}

		task.interrupt();
	}

	@DisplayName( "It should respond to an initialize request with capabilities" )
	@Test
	public void testRespondToInitializeRequest() {

		List<Map<String, Object>>														messages	= new ArrayList<Map<String, Object>>();

		List<TriConsumer<byte[], ByteArrayInputStream, AdapterProtocolMessageReader>>	steps		= Arrays.asList(
		    sendMessageStep( DebugMessages.getInitRequest( 1 ) ),
		    delayStep( 750 ),
		    readMessageStep( messages ),
		    readMessageStep( messages )
		);

		try {
			runDebugger( steps );

			Map<String, Object> initializedResponse = messages.get( 0 );
			assertThat( initializedResponse.get( "success" ) ).isEqualTo( true );
			assertThat( initializedResponse.get( "type" ) ).isEqualTo( "response" );
			assertThat( initializedResponse.get( "request_seq" ) ).isEqualTo( 1 );
		} catch ( Exception e ) {
			e.printStackTrace();
			assertThat( false ).isEqualTo( true );
		}
	}

	@DisplayName( "It should respond to a launch request" )
	@Test
	public void testRespondToLaunchRequest() {

		List<Map<String, Object>>														messages	= new ArrayList<Map<String, Object>>();

		List<TriConsumer<byte[], ByteArrayInputStream, AdapterProtocolMessageReader>>	steps		= Arrays.asList(
		    sendMessageStep( DebugMessages.getInitRequest( 1 ) ),
		    delayStep( 750 ),
		    readMessageStep( messages ),
		    readMessageStep( messages ),
		    sendMessageStep( DebugMessages.getLaunchRequest( 2 ) ),
		    delayStep( 750 ),
		    readMessageStep( messages )
		);

		try {
			runDebugger( steps );

			Map<String, Object> launchResponse = messages.get( 2 );
			assertThat( launchResponse.get( "type" ) ).isEqualTo( "response" );
			assertThat( launchResponse.get( "request_seq" ) ).isEqualTo( 2 );
		} catch ( Exception e ) {
			e.printStackTrace();
			assertThat( false ).isEqualTo( true );
		}
	}

	@DisplayName( "It should respond to a breakpoint request" )
	@Test
	public void testRespondToBreakpointRequest() {

		List<Map<String, Object>>														messages	= new ArrayList<Map<String, Object>>();

		List<TriConsumer<byte[], ByteArrayInputStream, AdapterProtocolMessageReader>>	steps		= Arrays.asList(
		    sendMessageStep( DebugMessages.getInitRequest( 1 ) ),
		    delayStep( 750 ),
		    readMessageStep( messages ),
		    readMessageStep( messages ),
		    sendMessageStep( DebugMessages.getLaunchRequest( 2 ) ),
		    delayStep( 750 ),
		    readMessageStep( messages ),
		    sendMessageStep( DebugMessages.getSetBreakpointsRequest( 3 ) ),
		    delayStep( 750 ),
		    readMessageStep( messages )
		);

		try {
			runDebugger( steps );

			Map<String, Object> launchResponse = messages.get( 3 );
			assertThat( launchResponse.get( "type" ) ).isEqualTo( "response" );
			assertThat( launchResponse.get( "request_seq" ) ).isEqualTo( 3 );
			Map<String, Object> breakpoint = ( Map ) ( ( List ) ( ( Map ) launchResponse.get( "body" ) ).get( "breakpoints" ) ).get( 0 );
			assertThat( breakpoint.get( "column" ) ).isEqualTo( 0 );
			assertThat( breakpoint.get( "id" ) ).isEqualTo( 0 );
			assertThat( breakpoint.get( "line" ) ).isEqualTo( 4 );
			assertThat( breakpoint.get( "verified" ) ).isEqualTo( true );
		} catch ( Exception e ) {
			e.printStackTrace();
			assertThat( false ).isEqualTo( true );
		}
	}

	@DisplayName( "It should send an event when it hits a breakpoint" )
	@Test
	public void testBreakpointEvent() {

		List<Map<String, Object>>														messages	= new ArrayList<Map<String, Object>>();

		List<TriConsumer<byte[], ByteArrayInputStream, AdapterProtocolMessageReader>>	steps		= Arrays.asList(
		    sendMessageStep( DebugMessages.getInitRequest( 1 ) ),
		    delayStep( 750 ),
		    readMessageStep( messages ),
		    readMessageStep( messages ),
		    sendMessageStep( DebugMessages.getLaunchRequest( 2 ) ),
		    delayStep( 750 ),
		    readMessageStep( messages ),
		    sendMessageStep( DebugMessages.getSetBreakpointsRequest( 3 ) ),
		    delayStep( 750 ),
		    readMessageStep( messages ),
		    sendMessageStep( DebugMessages.getConfigurationDoneRequest( 4 ) ),
		    delayStep( 750 ),
		    readMessageStep( messages ),
		    delayStep( 3000 ),
		    readMessageStep( messages ),
		    readMessageStep( messages ),
		    readMessageStep( messages )
		);

		// todo add step to handle outputs skip/collect
		// todo add way to target the message I want to test other than index

		try {
			runDebugger( steps );

			Map<String, Object> debugMessage = messages.stream()
			    .filter( ( m ) -> m.containsKey( "type" ) && ( ( String ) m.get( "type" ) ).compareToIgnoreCase( "event" ) == 0
			        && ( ( String ) m.get( "event" ) ).compareToIgnoreCase( "stopped" ) == 0 )
			    .findFirst()
			    .get();

			assertThat( debugMessage.get( "type" ) ).isEqualTo( "event" );
			assertThat( ( ( Map ) debugMessage.get( "body" ) ).get( "reason" ) ).isEqualTo( "breakpoint" );
			assertThat( ( ( List<Integer> ) ( ( Map ) debugMessage.get( "body" ) ).get( "hitBreakpointIds" ) ).get( 0 ) ).isEqualTo( 0 );
		} catch ( Exception e ) {
			e.printStackTrace();
			assertThat( false ).isEqualTo( true );
		}
	}

}
