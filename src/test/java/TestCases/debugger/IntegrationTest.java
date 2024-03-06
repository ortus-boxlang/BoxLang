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
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import TestCases.debugger.DebugMessages.TriConsumer;
import ortus.boxlang.debugger.AdapterProtocolMessageReader;
import ortus.boxlang.debugger.DebugAdapter;
import ortus.boxlang.debugger.IAdapterProtocolMessage;
import ortus.boxlang.debugger.event.StoppedEvent;
import ortus.boxlang.debugger.response.InitializeResponse;
import ortus.boxlang.debugger.response.NoBodyResponse;
import ortus.boxlang.debugger.response.SetBreakpointsResponse;
import ortus.boxlang.debugger.response.ThreadsResponse;
import ortus.boxlang.debugger.types.Breakpoint;

// @Disabled
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
			reader = DebugMessages.messageReader;

			reader.changeInputStream( new ByteArrayInputStream( output.toByteArray() ) );

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

		InitializeResponse message = ( InitializeResponse ) runDebugger( Arrays.asList(
		    sendMessageStep( DebugMessages.getInitRequest( 1 ) ),
		    waitForMessage( "response", "initialize" )
		) )
		    .stream()
		    .filter( ( m ) -> {
			    return m instanceof InitializeResponse;
		    } )
		    .findFirst()
		    .get();

		assertThat( message.success ).isTrue();
		assertThat( message.getCommand() ).isEqualTo( "initialize" );
		assertThat( message.getType() ).isEqualTo( "response" );
		assertThat( message.getSeq() ).isEqualTo( 1 );
	}

	@DisplayName( "It should respond to a launch request" )
	@Test
	public void testRespondToLaunchRequest() {

		NoBodyResponse message = ( NoBodyResponse ) runDebugger( Arrays.asList(
		    sendMessageStep( DebugMessages.getInitRequest( 1 ) ),
		    waitForMessage( "response", "initialize" ),
		    waitForMessage( "event", "initialized" ),
		    sendMessageStep( DebugMessages.getLaunchRequest( 2 ) ),
		    waitForMessage( "response", "launch" )
		) )
		    .stream()
		    .filter( ( m ) -> {
			    return m instanceof NoBodyResponse;
		    } )
		    .findFirst()
		    .get();

		assertThat( message.getType() ).isEqualTo( "response" );
		assertThat( message.getCommand() ).isEqualTo( "launch" );
		assertThat( message.getSeq() ).isEqualTo( 2 );
	}

	@DisplayName( "It should respond to a breakpoint request" )
	@Test
	public void testRespondToBreakpointRequest() {

		SetBreakpointsResponse message = ( SetBreakpointsResponse ) runDebugger( Arrays.asList(
		    sendMessageStep( DebugMessages.getInitRequest( 1 ) ),
		    waitForMessage( "response", "initialize" ),
		    waitForMessage( "event", "initialized" ),
		    sendMessageStep( DebugMessages.getLaunchRequest( 2 ) ),
		    waitForMessage( "response", "launch" ),
		    sendMessageStep( DebugMessages.getSetBreakpointsRequest( 3 ) ),
		    waitForMessage( "response", "setbreakpoints" )
		) )
		    .stream()
		    .filter( ( m ) -> {
			    return m instanceof SetBreakpointsResponse;
		    } )
		    .findFirst()
		    .get();

		assertThat( message.getType() ).isEqualTo( "response" );
		assertThat( message.getSeq() ).isEqualTo( 3 );
		Breakpoint[] breakpoints = message.body.breakpoints;

		assertThat( breakpoints.length ).isEqualTo( 1 );

		assertThat( breakpoints[ 0 ].column ).isEqualTo( 0 );
		assertThat( breakpoints[ 0 ].id ).isEqualTo( 0 );
		assertThat( breakpoints[ 0 ].line ).isEqualTo( 4 );
		assertThat( breakpoints[ 0 ].verified ).isEqualTo( true );
	}

	@DisplayName( "It should send an event when it hits a breakpoint" )
	@Test
	public void testBreakpointEvent() {

		StoppedEvent message = ( StoppedEvent ) runDebugger( Arrays.asList(
		    sendMessageStep( DebugMessages.getInitRequest( 1 ) ),
		    waitForMessage( "response", "initialize" ),
		    waitForMessage( "event", "initialized" ),
		    sendMessageStep( DebugMessages.getLaunchRequest( 2 ) ),
		    waitForMessage( "response", "launch" ),
		    sendMessageStep( DebugMessages.getSetBreakpointsRequest( 3 ) ),
		    waitForMessage( "response", "setbreakpoints" ),
		    sendMessageStep( DebugMessages.getConfigurationDoneRequest( 4 ) ),
		    waitForMessage( "response", "configurationdone" ),
		    waitForMessage( "event", "stopped", 10000 )
		) )
		    .stream()
		    .filter( ( m ) -> {
			    return m instanceof StoppedEvent;
		    } )
		    .findFirst()
		    .get();

		assertThat( message.getType() ).isEqualTo( "event" );
		assertThat( message.body.reason ).isEqualTo( "breakpoint" );
		assertThat( message.body.hitBreakpointIds.size() ).isGreaterThan( 0 );
		assertThat( message.body.hitBreakpointIds.get( 0 ) ).isEqualTo( 0 );
	}

	@DisplayName( "It should respond to a ThreadsRequest" )
	@Test
	public void testThreadsResponse() {

		ThreadsResponse message = ( ThreadsResponse ) runDebugger( Arrays.asList(
		    sendMessageStep( DebugMessages.getInitRequest( 1 ) ),
		    waitForMessage( "response", "initialize" ),
		    waitForMessage( "event", "initialized" ),
		    sendMessageStep( DebugMessages.getLaunchRequest( 2 ) ),
		    waitForMessage( "response", "launch" ),
		    sendMessageStep( DebugMessages.getSetBreakpointsRequest( 3 ) ),
		    waitForMessage( "response", "setbreakpoints" ),
		    sendMessageStep( DebugMessages.getConfigurationDoneRequest( 4 ) ),
		    waitForMessage( "response", "configurationdone" ),
		    waitForMessage( "event", "stopped", 20000 ),
		    sendMessageStep( DebugMessages.getThreadsRequest( 5 ) ),
		    waitForMessage( "response", "threads" )
		) )
		    .stream()
		    .filter( ( m ) -> {
			    return m instanceof ThreadsResponse;
		    } )
		    .findFirst()
		    .get();

		assertThat( message.getType() ).isEqualTo( "response" );
		assertThat( message.getCommand() ).isEqualTo( "threads" );

		assertThat( message.body.threads.size() ).isGreaterThan( 0 );

		Optional<ortus.boxlang.debugger.types.Thread> mainThread = message.body.threads.stream().filter( ( t ) -> t.name.equalsIgnoreCase( "main" ) )
		    .findFirst();
		assertThat( mainThread.isPresent() ).isTrue();
	}

	@DisplayName( "It should respond to a StackTraceRequest" )
	@Test
	public void testStackTraceResponse() {

		// TODO implement this!!!!!
	}

	@DisplayName( "It should respond to a ScopeRequest" )
	@Test
	public void testScopeResponse() {

		// TODO implement this!!!!!
	}

	@DisplayName( "It should respond to a VariablesRequest" )
	@Test
	public void testVariablesResponse() {

		// TODO implement this!!!!!
	}

	@DisplayName( "It should respond to a ContinueRequest" )
	@Test
	public void testContinueResponse() {

		// TODO implement this!!!!!
	}

	@DisplayName( "It should respond to an EvaluateRequest" )
	@Test
	public void testEvaluateResponse() {

		// TODO implement this!!!!!
	}

	@DisplayName( "It should send an OutputEvent" )
	@Test
	public void testOutputEvent() {

		// TODO implement this!!!!!
	}

	@DisplayName( "It should send an ExitEvent" )
	@Test
	public void testExitEvent() {

		// TODO implement this!!!!!
	}

	@DisplayName( "It should send a TerminatedEvent" )
	@Test
	public void testTerminatedEvent() {

		// TODO implement this!!!!!
	}

}
