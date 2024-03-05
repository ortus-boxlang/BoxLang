package TestCases.debugger;

import static TestCases.debugger.DebugMessages.sendMessageStep;
import static TestCases.debugger.DebugMessages.waitForMessage;
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

import TestCases.debugger.DebugMessages.TriConsumer;
import ortus.boxlang.debugger.AdapterProtocolMessageReader;
import ortus.boxlang.debugger.DebugAdapter;
import ortus.boxlang.debugger.IAdapterProtocolMessage;

public class IntegrationTest {

	private List<IAdapterProtocolMessage> runDebugger( List<TriConsumer<byte[], ByteArrayInputStream, ByteArrayOutputStream>> steps ) {
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

		for ( TriConsumer<byte[], ByteArrayInputStream, ByteArrayOutputStream> step : steps ) {
			step.accept( buffer, inputStream, output );
		}

		task.interrupt();

		List<IAdapterProtocolMessage>	messages	= new ArrayList<IAdapterProtocolMessage>();
		AdapterProtocolMessageReader	reader;
		try {
			reader								= new AdapterProtocolMessageReader( new ByteArrayInputStream( output.toByteArray() ) );

			reader.throwOnUnregisteredCommand	= false;

			IAdapterProtocolMessage message = reader.read();

			while ( message != null ) {
				messages.add( message );
				message = reader.read();
			}
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return messages;
	}

	@DisplayName( "It should respond to an initialize request with capabilities" )
	@Test
	public void testRespondToInitializeRequest() {

		List<TriConsumer<byte[], ByteArrayInputStream, ByteArrayOutputStream>>	steps				= Arrays.asList(
		    sendMessageStep( DebugMessages.getInitRequest( 1 ) ),
		    waitForMessage( "response", "initialize" )
		);

		List<IAdapterProtocolMessage>											messages			= runDebugger( steps );

		Map<String, Object>														initializedResponse	= messages.get( 0 ).getRawMessageData();
		assertThat( initializedResponse.get( "success" ) ).isEqualTo( true );
		assertThat( initializedResponse.get( "type" ) ).isEqualTo( "response" );
		assertThat( initializedResponse.get( "request_seq" ) ).isEqualTo( 1 );
	}

	@DisplayName( "It should respond to a launch request" )
	@Test
	public void testRespondToLaunchRequest() {

		List<TriConsumer<byte[], ByteArrayInputStream, ByteArrayOutputStream>> steps = Arrays.asList(
		    sendMessageStep( DebugMessages.getInitRequest( 1 ) ),
		    waitForMessage( "response", "initialize" ),
		    waitForMessage( "event", "initialized" ),
		    sendMessageStep( DebugMessages.getLaunchRequest( 2 ) ),
		    waitForMessage( "response", "launch" )
		);

		try {
			List<IAdapterProtocolMessage>	messages		= runDebugger( steps );

			Map<String, Object>				launchResponse	= messages.get( 2 ).getRawMessageData();
			assertThat( launchResponse.get( "type" ) ).isEqualTo( "response" );
			assertThat( launchResponse.get( "command" ) ).isEqualTo( "launch" );
			assertThat( launchResponse.get( "request_seq" ) ).isEqualTo( 2 );
		} catch ( Exception e ) {
			e.printStackTrace();
			assertThat( false ).isEqualTo( true );
		}
	}

	@DisplayName( "It should respond to a breakpoint request" )
	@Test
	public void testRespondToBreakpointRequest() {

		List<TriConsumer<byte[], ByteArrayInputStream, ByteArrayOutputStream>> steps = Arrays.asList(
		    sendMessageStep( DebugMessages.getInitRequest( 1 ) ),
		    waitForMessage( "response", "initialize" ),
		    waitForMessage( "event", "initialized" ),
		    sendMessageStep( DebugMessages.getLaunchRequest( 2 ) ),
		    waitForMessage( "response", "launch" ),
		    sendMessageStep( DebugMessages.getSetBreakpointsRequest( 3 ) ),
		    waitForMessage( "response", "setbreakpoints" )
		);

		try {
			List<IAdapterProtocolMessage>	messages		= runDebugger( steps );

			Map<String, Object>				launchResponse	= messages.get( 3 ).getRawMessageData();
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

		List<TriConsumer<byte[], ByteArrayInputStream, ByteArrayOutputStream>>	steps			= Arrays.asList(
		    sendMessageStep( DebugMessages.getInitRequest( 1 ) ),
		    waitForMessage( "response", "initialize" ),
		    waitForMessage( "event", "initialized" ),
		    sendMessageStep( DebugMessages.getLaunchRequest( 2 ) ),
		    waitForMessage( "response", "launch" ),
		    sendMessageStep( DebugMessages.getSetBreakpointsRequest( 3 ) ),
		    waitForMessage( "response", "setbreakpoints" ),
		    sendMessageStep( DebugMessages.getConfigurationDoneRequest( 4 ) ),
		    waitForMessage( "response", "configurationdone" ),
		    waitForMessage( "event", "stopped", 3000 )
		);

		List<IAdapterProtocolMessage>											messages		= runDebugger( steps );

		Map<String, Object>														debugMessage	= messages.stream().filter( ( message ) -> {
																									return DebugMessages.getMessageMatcher( "event", "stopped" )
																									    .test( message.getRawMessageData() );
																								} )
		    .findFirst().get().getRawMessageData();

		assertThat( debugMessage.get( "type" ) ).isEqualTo( "event" );
		assertThat( ( ( Map ) debugMessage.get( "body" ) ).get( "reason" ) ).isEqualTo( "breakpoint" );
		assertThat( ( ( List<Integer> ) ( ( Map ) debugMessage.get( "body" ) ).get( "hitBreakpointIds" ) ).get( 0 ) ).isEqualTo( 0 );
	}

}
