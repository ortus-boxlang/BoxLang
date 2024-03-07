package TestCases.debugger;

import static TestCases.debugger.DebugMessages.sendMessageStep;
import static TestCases.debugger.DebugMessages.waitForMessage;
import static TestCases.debugger.DebugMessages.waitForMessageThenSend;
import static TestCases.debugger.DebugMessages.waitForSeq;
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
import ortus.boxlang.debugger.event.ExitEvent;
import ortus.boxlang.debugger.event.OutputEvent;
import ortus.boxlang.debugger.event.StoppedEvent;
import ortus.boxlang.debugger.event.TerminatedEvent;
import ortus.boxlang.debugger.response.ContinueResponse;
import ortus.boxlang.debugger.response.EvaluateResponse;
import ortus.boxlang.debugger.response.InitializeResponse;
import ortus.boxlang.debugger.response.NoBodyResponse;
import ortus.boxlang.debugger.response.ScopeResponse;
import ortus.boxlang.debugger.response.SetBreakpointsResponse;
import ortus.boxlang.debugger.response.StackTraceResponse;
import ortus.boxlang.debugger.response.ThreadsResponse;
import ortus.boxlang.debugger.response.VariablesResponse;
import ortus.boxlang.debugger.types.Breakpoint;
import ortus.boxlang.debugger.types.Scope;
import ortus.boxlang.debugger.types.StackFrame;
import ortus.boxlang.debugger.types.Variable;

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
		    sendMessageStep( DebugMessages.getSetBreakpointsRequest( 3, new int[] { 6 } ) ),
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
		assertThat( breakpoints[ 0 ].line ).isEqualTo( 6 );
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
		    sendMessageStep( DebugMessages.getSetBreakpointsRequest( 3, new int[] { 6 } ) ),
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
		    sendMessageStep( DebugMessages.getSetBreakpointsRequest( 3, new int[] { 6 } ) ),
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

		Optional<ortus.boxlang.debugger.types.Thread> mainThread = message.body.threads.stream()
		    .filter( ( t ) -> t.name.equalsIgnoreCase( "main" ) )
		    .findFirst();

		assertThat( mainThread.isPresent() ).isTrue();
	}

	@DisplayName( "It should respond to a StackTraceRequest" )
	@Test
	public void testStackTraceResponse() {

		StackTraceResponse message = ( StackTraceResponse ) runDebugger( Arrays.asList(
		    sendMessageStep( DebugMessages.getInitRequest( 1 ) ),
		    waitForMessage( "response", "initialize" ),
		    waitForMessage( "event", "initialized" ),
		    sendMessageStep( DebugMessages.getLaunchRequest( 2 ) ),
		    waitForMessage( "response", "launch" ),
		    sendMessageStep( DebugMessages.getSetBreakpointsRequest( 3, new int[] { 6 } ) ),
		    waitForMessage( "response", "setbreakpoints" ),
		    sendMessageStep( DebugMessages.getConfigurationDoneRequest( 4 ) ),
		    waitForMessage( "response", "configurationdone" ),
		    waitForMessage( "event", "stopped", 20000 ),
		    sendMessageStep( DebugMessages.getThreadsRequest( 5 ) ),
		    waitForMessage( "response", "threads" ),
		    sendMessageStep( DebugMessages.getStackTraceRequest( 6 ) ),
		    waitForMessage( "response", "stacktrace" )
		) )
		    .stream()
		    .filter( ( m ) -> {
			    return m instanceof StackTraceResponse;
		    } )
		    .findFirst()
		    .get();

		assertThat( message.getType() ).isEqualTo( "response" );
		assertThat( message.getCommand() ).isEqualTo( "stacktrace" );

		assertThat( message.body.stackFrames.size() ).isGreaterThan( 0 );

		StackFrame frame = message.body.stackFrames.get( 0 );

		assertThat( frame.name ).isEqualTo( "main.cfs" );
		assertThat( frame.line ).isEqualTo( 4 );
	}

	@DisplayName( "It should respond to a ScopeRequest" )
	@Test
	public void testScopeResponse() {

		ScopeResponse message = ( ScopeResponse ) runDebugger( Arrays.asList(
		    sendMessageStep( DebugMessages.getInitRequest( 1 ) ),
		    waitForMessage( "response", "initialize" ),
		    waitForMessage( "event", "initialized" ),
		    sendMessageStep( DebugMessages.getLaunchRequest( 2 ) ),
		    waitForMessage( "response", "launch" ),
		    sendMessageStep( DebugMessages.getSetBreakpointsRequest( 3, new int[] { 6 } ) ),
		    waitForMessage( "response", "setbreakpoints" ),
		    sendMessageStep( DebugMessages.getConfigurationDoneRequest( 4 ) ),
		    waitForMessage( "response", "configurationdone" ),
		    waitForMessage( "event", "stopped", 20000 ),
		    sendMessageStep( DebugMessages.getThreadsRequest( 5 ) ),
		    waitForMessage( "response", "threads" ),
		    sendMessageStep( DebugMessages.getStackTraceRequest( 6 ) ),
		    waitForMessageThenSend(
		        "response",
		        "stacktrace",
		        ( found ) -> {
			        StackTraceResponse m = ( StackTraceResponse ) found;

			        return DebugMessages.getScopeRequest( 7, m.body.stackFrames.get( 0 ).id );
		        }
		    ),
		    waitForMessage( "response", "scopes" )
		) )
		    .stream()
		    .filter( ( m ) -> {
			    return m instanceof ScopeResponse;
		    } )
		    .findFirst()
		    .get();

		assertThat( message.getType() ).isEqualTo( "response" );
		assertThat( message.getCommand() ).isEqualTo( "scopes" );

		assertThat( message.body.scopes.size() ).isGreaterThan( 0 );

		Scope variables = message.body.scopes.stream().filter( ( scope ) -> scope.name.equalsIgnoreCase( "variables" ) ).findFirst().get();

		assertThat( variables.name ).isEqualTo( "variables" );
	}

	@DisplayName( "It should respond to a VariablesRequest" )
	@Test
	public void testVariablesResponse() {

		VariablesResponse message = ( VariablesResponse ) runDebugger( Arrays.asList(
		    sendMessageStep( DebugMessages.getInitRequest( 1 ) ),
		    waitForMessage( "response", "initialize" ),
		    waitForMessage( "event", "initialized" ),
		    sendMessageStep( DebugMessages.getLaunchRequest( 2 ) ),
		    waitForMessage( "response", "launch" ),
		    sendMessageStep( DebugMessages.getSetBreakpointsRequest( 3, new int[] { 6 } ) ),
		    waitForMessage( "response", "setbreakpoints" ),
		    sendMessageStep( DebugMessages.getConfigurationDoneRequest( 4 ) ),
		    waitForMessage( "response", "configurationdone" ),
		    waitForMessage( "event", "stopped", 20000 ),
		    sendMessageStep( DebugMessages.getThreadsRequest( 5 ) ),
		    waitForMessage( "response", "threads" ),
		    sendMessageStep( DebugMessages.getStackTraceRequest( 6 ) ),
		    waitForMessageThenSend(
		        "response",
		        "stacktrace",
		        ( found ) -> {
			        StackTraceResponse m = ( StackTraceResponse ) found;

			        return DebugMessages.getScopeRequest( 7, m.body.stackFrames.get( 0 ).id );
		        }
		    ),
		    waitForMessageThenSend(
		        "response",
		        "scopes",
		        ( found ) -> {
			        ScopeResponse m		= ( ScopeResponse ) found;

			        Scope	variables	= m.body.scopes.stream().filter( ( scope ) -> scope.name.equalsIgnoreCase( "variables" ) ).findFirst().get();

			        return DebugMessages.getVariablesRequest( 7, variables.variablesReference );
		        }
		    ),
		    waitForMessage( "response", "variables" )
		) )
		    .stream()
		    .filter( ( m ) -> {
			    return m instanceof VariablesResponse;
		    } )
		    .findFirst()
		    .get();

		assertThat( message.getType() ).isEqualTo( "response" );
		assertThat( message.getCommand() ).isEqualTo( "variables" );

		assertThat( message.body.variables.size() ).isGreaterThan( 0 );

		Variable	value	= message.body.variables.stream().filter( ( variable ) -> variable.name.equalsIgnoreCase( "value" ) ).findFirst().get();
		Variable	color	= message.body.variables.stream().filter( ( variable ) -> variable.name.equalsIgnoreCase( "color" ) ).findFirst().get();

		assertThat( value.name ).isEqualTo( "value" );
		assertThat( value.type ).isEqualTo( "numeric" );
		assertThat( value.value ).isEqualTo( "4" );

		assertThat( color.name ).isEqualTo( "color" );
		assertThat( color.type ).isEqualTo( "String" );
		assertThat( color.value ).isEqualTo( "\"green\"" );
	}

	@DisplayName( "It should respond with the variables of a struct" )
	@Test
	public void testStructVariablesResponse() {

		VariablesResponse message = ( VariablesResponse ) runDebugger( Arrays.asList(
		    sendMessageStep( DebugMessages.getInitRequest( 1 ) ),
		    waitForMessage( "response", "initialize" ),
		    waitForMessage( "event", "initialized" ),
		    sendMessageStep( DebugMessages.getLaunchRequest( 2 ) ),
		    waitForMessage( "response", "launch" ),
		    sendMessageStep( DebugMessages.getSetBreakpointsRequest( 3, new int[] { 6 } ) ),
		    waitForMessage( "response", "setbreakpoints" ),
		    sendMessageStep( DebugMessages.getConfigurationDoneRequest( 4 ) ),
		    waitForMessage( "response", "configurationdone" ),
		    waitForMessage( "event", "stopped", 20000 ),
		    sendMessageStep( DebugMessages.getThreadsRequest( 5 ) ),
		    waitForMessage( "response", "threads" ),
		    sendMessageStep( DebugMessages.getStackTraceRequest( 6 ) ),
		    waitForMessageThenSend(
		        "response",
		        "stacktrace",
		        ( found ) -> {
			        StackTraceResponse m = ( StackTraceResponse ) found;

			        return DebugMessages.getScopeRequest( 7, m.body.stackFrames.get( 0 ).id );
		        }
		    ),
		    waitForMessageThenSend(
		        "response",
		        "scopes",
		        ( found ) -> {
			        ScopeResponse m		= ( ScopeResponse ) found;

			        Scope	variables	= m.body.scopes.stream().filter( ( scope ) -> scope.name.equalsIgnoreCase( "variables" ) ).findFirst().get();

			        return DebugMessages.getVariablesRequest( 8, variables.variablesReference );
		        }
		    ),
		    waitForMessageThenSend(
		        8,
		        ( found ) -> {
			        VariablesResponse m	= ( VariablesResponse ) found;

			        Variable	value	= m.body.variables.stream().filter( ( variable ) -> variable.name.equalsIgnoreCase( "data" ) ).findFirst().get();

			        return DebugMessages.getVariablesRequest( 9, value.variablesReference );
		        }
		    ),
		    waitForSeq( 9 )
		) )
		    .stream()
		    .filter( ( m ) -> {
			    return m instanceof VariablesResponse vr && vr.getSeq() == 9;
		    } )
		    .findFirst()
		    .get();

		assertThat( message.getType() ).isEqualTo( "response" );
		assertThat( message.getCommand() ).isEqualTo( "variables" );

		assertThat( message.body.variables.size() ).isGreaterThan( 0 );

		Variable test = message.body.variables.stream().filter( ( variable ) -> variable.name.equalsIgnoreCase( "test" ) ).findFirst().get();

		assertThat( test.name ).isEqualTo( "test" );
		assertThat( test.type ).isEqualTo( "boolean" );
		assertThat( test.value ).isEqualTo( "true" );
	}

	@DisplayName( "It should respond to a ContinueRequest" )
	@Test
	public void testContinueResponse() {

		ContinueResponse message = ( ContinueResponse ) runDebugger( Arrays.asList(
		    sendMessageStep( DebugMessages.getInitRequest( 1 ) ),
		    waitForMessage( "response", "initialize" ),
		    waitForMessage( "event", "initialized" ),
		    sendMessageStep( DebugMessages.getLaunchRequest( 2 ) ),
		    waitForMessage( "response", "launch" ),
		    sendMessageStep( DebugMessages.getSetBreakpointsRequest( 3, new int[] { 6 } ) ),
		    waitForMessage( "response", "setbreakpoints" ),
		    sendMessageStep( DebugMessages.getConfigurationDoneRequest( 4 ) ),
		    waitForMessage( "response", "configurationdone" ),
		    waitForMessage( "event", "stopped", 20000 ),
		    sendMessageStep( DebugMessages.getThreadsRequest( 5 ) ),
		    waitForMessage( "response", "threads" ),
		    sendMessageStep( DebugMessages.getStackTraceRequest( 6 ) ),
		    waitForMessageThenSend(
		        "response",
		        "stacktrace",
		        ( found ) -> {
			        StackTraceResponse m = ( StackTraceResponse ) found;

			        return DebugMessages.getScopeRequest( 7, m.body.stackFrames.get( 0 ).id );
		        }
		    ),
		    waitForMessageThenSend(
		        "response",
		        "scopes",
		        ( found ) -> {
			        ScopeResponse m		= ( ScopeResponse ) found;

			        Scope	variables	= m.body.scopes.stream().filter( ( scope ) -> scope.name.equalsIgnoreCase( "variables" ) ).findFirst().get();

			        return DebugMessages.getVariablesRequest( 8, variables.variablesReference );
		        }
		    ),
		    waitForMessageThenSend(
		        8,
		        ( found ) -> {
			        VariablesResponse m	= ( VariablesResponse ) found;

			        Variable	value	= m.body.variables.stream().filter( ( variable ) -> variable.name.equalsIgnoreCase( "data" ) ).findFirst().get();

			        return DebugMessages.getVariablesRequest( 9, value.variablesReference );
		        }
		    ),
		    waitForSeq( 9 ),
		    sendMessageStep( DebugMessages.getContinueRequest( 10 ) ),
		    waitForMessage( "response", "continue" )
		) )
		    .stream()
		    .filter( ( m ) -> {
			    return m instanceof ContinueResponse;
		    } )
		    .findFirst()
		    .get();

		assertThat( message.getType() ).isEqualTo( "response" );
		assertThat( message.getCommand() ).isEqualTo( "continue" );
		assertThat( message.getSeq() ).isEqualTo( 10 );

		assertThat( message.body.allThreadsContinued ).isTrue();
	}

	@DisplayName( "It should respond to an EvaluateRequest" )
	@Test
	public void testEvaluateResponse() {

		EvaluateResponse message = ( EvaluateResponse ) runDebugger( Arrays.asList(
		    sendMessageStep( DebugMessages.getInitRequest( 1 ) ),
		    waitForMessage( "response", "initialize" ),
		    waitForMessage( "event", "initialized" ),
		    sendMessageStep( DebugMessages.getLaunchRequest( 2 ) ),
		    waitForMessage( "response", "launch" ),
		    sendMessageStep( DebugMessages.getSetBreakpointsRequest( 3, new int[] { 6 } ) ),
		    waitForMessage( "response", "setbreakpoints" ),
		    sendMessageStep( DebugMessages.getConfigurationDoneRequest( 4 ) ),
		    waitForMessage( "response", "configurationdone" ),
		    waitForMessage( "event", "stopped", 10000 ),
		    sendMessageStep( DebugMessages.getThreadsRequest( 5 ) ),
		    waitForMessage( "response", "threads" ),
		    sendMessageStep( DebugMessages.getStackTraceRequest( 6 ) ),
		    waitForMessageThenSend(
		        "response",
		        "stacktrace",
		        ( found ) -> {
			        StackTraceResponse m = ( StackTraceResponse ) found;

			        return DebugMessages.getEvaluateRequest( 7, "value * 2", m.body.stackFrames.get( 0 ).id );
		        }
		    ),
		    waitForMessage( "response", "evaluate", 10000 )

		) )
		    .stream()
		    .filter( ( m ) -> {
			    return m instanceof EvaluateResponse;
		    } )
		    .findFirst()
		    .get();

		assertThat( message.getType() ).isEqualTo( "response" );
		assertThat( message.body.result ).isEqualTo( "8" );
		assertThat( message.body.type ).isEqualTo( "numeric" );
		assertThat( message.body.variablesReference ).isEqualTo( 0 );
	}

	@DisplayName( "It should respond to an EvaluateRequest that produces an error" )
	@Test
	public void testEvaluateErrorResponse() {

		EvaluateResponse message = ( EvaluateResponse ) runDebugger( Arrays.asList(
		    sendMessageStep( DebugMessages.getInitRequest( 1 ) ),
		    waitForMessage( "response", "initialize" ),
		    waitForMessage( "event", "initialized" ),
		    sendMessageStep( DebugMessages.getLaunchRequest( 2 ) ),
		    waitForMessage( "response", "launch" ),
		    sendMessageStep( DebugMessages.getSetBreakpointsRequest( 3, new int[] { 6 } ) ),
		    waitForMessage( "response", "setbreakpoints" ),
		    sendMessageStep( DebugMessages.getConfigurationDoneRequest( 4 ) ),
		    waitForMessage( "response", "configurationdone" ),
		    waitForMessage( "event", "stopped", 10000 ),
		    sendMessageStep( DebugMessages.getThreadsRequest( 5 ) ),
		    waitForMessage( "response", "threads" ),
		    sendMessageStep( DebugMessages.getStackTraceRequest( 6 ) ),
		    waitForMessageThenSend(
		        "response",
		        "stacktrace",
		        ( found ) -> {
			        StackTraceResponse m = ( StackTraceResponse ) found;

			        return DebugMessages.getEvaluateRequest( 7, "foo * 2", m.body.stackFrames.get( 0 ).id );
		        }
		    ),
		    waitForMessage( "response", "evaluate", 10000 )

		) )
		    .stream()
		    .filter( ( m ) -> {
			    return m instanceof EvaluateResponse;
		    } )
		    .findFirst()
		    .get();

		assertThat( message.success ).isFalse();
		assertThat( message.getType() ).isEqualTo( "response" );
		assertThat( message.body.error.format ).containsMatch( "(?i)the requested key \\[foo\\]" );
	}

	@DisplayName( "It should send an OutputEvent" )
	@Test
	public void testOutputEvent() {

		OutputEvent message = ( OutputEvent ) runDebugger( Arrays.asList(
		    sendMessageStep( DebugMessages.getInitRequest( 1 ) ),
		    waitForMessage( "response", "initialize" ),
		    waitForMessage( "event", "initialized" ),
		    sendMessageStep( DebugMessages.getLaunchRequest( 2 ) ),
		    waitForMessage( "response", "launch" ),
		    sendMessageStep( DebugMessages.getSetBreakpointsRequest( 3, new int[] { 6 } ) ),
		    waitForMessage( "response", "setbreakpoints" ),
		    sendMessageStep( DebugMessages.getConfigurationDoneRequest( 4 ) ),
		    waitForMessage( "response", "configurationdone" ),
		    waitForMessage( "event", "stopped", 20000 ),
		    sendMessageStep( DebugMessages.getThreadsRequest( 5 ) ),
		    waitForMessage( "response", "threads" ),
		    sendMessageStep( DebugMessages.getStackTraceRequest( 6 ) ),
		    waitForMessageThenSend(
		        "response",
		        "stacktrace",
		        ( found ) -> {
			        StackTraceResponse m = ( StackTraceResponse ) found;

			        return DebugMessages.getScopeRequest( 7, m.body.stackFrames.get( 0 ).id );
		        }
		    ),
		    waitForMessageThenSend(
		        "response",
		        "scopes",
		        ( found ) -> {
			        ScopeResponse m		= ( ScopeResponse ) found;

			        Scope	variables	= m.body.scopes.stream().filter( ( scope ) -> scope.name.equalsIgnoreCase( "variables" ) ).findFirst().get();

			        return DebugMessages.getVariablesRequest( 8, variables.variablesReference );
		        }
		    ),
		    waitForMessageThenSend(
		        8,
		        ( found ) -> {
			        VariablesResponse m	= ( VariablesResponse ) found;

			        Variable	value	= m.body.variables.stream().filter( ( variable ) -> variable.name.equalsIgnoreCase( "data" ) ).findFirst().get();

			        return DebugMessages.getVariablesRequest( 9, value.variablesReference );
		        }
		    ),
		    waitForSeq( 9 ),
		    sendMessageStep( DebugMessages.getContinueRequest( 10 ) ),
		    waitForMessage( "response", "continue" ),
		    waitForMessage(
		        ( m ) -> {
			        return m instanceof OutputEvent oe
			            && oe.body.output.contains( "The value is: 6" );
		        },
		        ( m ) -> {
		        },
		        5000L
		    )
		) )
		    .stream()
		    .filter( ( m ) -> {
			    return m instanceof OutputEvent oe
			        && oe.body.output.contains( "The value is: 6" );
		    } )
		    .findFirst()
		    .get();

		assertThat( message.getType() ).isEqualTo( "event" );
		assertThat( message.getCommand() ).isEqualTo( "output" );
		assertThat( message.getSeq() ).isEqualTo( -1 );

		assertThat( message.body.output ).contains( "The value is: 6" );
	}

	@DisplayName( "It should send an ExitEvent" )
	@Test
	public void testExitEvent() {

		ExitEvent message = ( ExitEvent ) runDebugger( Arrays.asList(
		    sendMessageStep( DebugMessages.getInitRequest( 1 ) ),
		    waitForMessage( "response", "initialize" ),
		    waitForMessage( "event", "initialized" ),
		    sendMessageStep( DebugMessages.getLaunchRequest( 2 ) ),
		    waitForMessage( "response", "launch" ),
		    sendMessageStep( DebugMessages.getSetBreakpointsRequest( 3, new int[] { 6 } ) ),
		    waitForMessage( "response", "setbreakpoints" ),
		    sendMessageStep( DebugMessages.getConfigurationDoneRequest( 4 ) ),
		    waitForMessage( "response", "configurationdone" ),
		    waitForMessage( "event", "stopped", 20000 ),
		    sendMessageStep( DebugMessages.getThreadsRequest( 5 ) ),
		    waitForMessage( "response", "threads" ),
		    sendMessageStep( DebugMessages.getStackTraceRequest( 6 ) ),
		    waitForMessageThenSend(
		        "response",
		        "stacktrace",
		        ( found ) -> {
			        StackTraceResponse m = ( StackTraceResponse ) found;

			        return DebugMessages.getScopeRequest( 7, m.body.stackFrames.get( 0 ).id );
		        }
		    ),
		    waitForMessageThenSend(
		        "response",
		        "scopes",
		        ( found ) -> {
			        ScopeResponse m		= ( ScopeResponse ) found;

			        Scope	variables	= m.body.scopes.stream().filter( ( scope ) -> scope.name.equalsIgnoreCase( "variables" ) ).findFirst().get();

			        return DebugMessages.getVariablesRequest( 8, variables.variablesReference );
		        }
		    ),
		    waitForMessageThenSend(
		        8,
		        ( found ) -> {
			        VariablesResponse m	= ( VariablesResponse ) found;

			        Variable	value	= m.body.variables.stream().filter( ( variable ) -> variable.name.equalsIgnoreCase( "data" ) ).findFirst().get();

			        return DebugMessages.getVariablesRequest( 9, value.variablesReference );
		        }
		    ),
		    waitForSeq( 9 ),
		    sendMessageStep( DebugMessages.getContinueRequest( 10 ) ),
		    waitForMessage( "response", "continue" ),
		    waitForMessage( "event", "exited", 5000 )
		) )
		    .stream()
		    .filter( ( m ) -> {
			    return m instanceof ExitEvent;
		    } )
		    .findFirst()
		    .get();

		assertThat( message.getType() ).isEqualTo( "event" );
		assertThat( message.getCommand() ).isEqualTo( "exited" );
		assertThat( message.getSeq() ).isEqualTo( -1 );

		assertThat( message.body.exitCode ).isEqualTo( 0 );
	}

	@DisplayName( "It should send a TerminatedEvent" )
	@Test
	public void testTerminatedEvent() {

		TerminatedEvent message = ( TerminatedEvent ) runDebugger( Arrays.asList(
		    sendMessageStep( DebugMessages.getInitRequest( 1 ) ),
		    waitForMessage( "response", "initialize" ),
		    waitForMessage( "event", "initialized" ),
		    sendMessageStep( DebugMessages.getLaunchRequest( 2 ) ),
		    waitForMessage( "response", "launch" ),
		    sendMessageStep( DebugMessages.getSetBreakpointsRequest( 3, new int[] { 6 } ) ),
		    waitForMessage( "response", "setbreakpoints" ),
		    sendMessageStep( DebugMessages.getConfigurationDoneRequest( 4 ) ),
		    waitForMessage( "response", "configurationdone" ),
		    waitForMessage( "event", "stopped", 20000 ),
		    sendMessageStep( DebugMessages.getThreadsRequest( 5 ) ),
		    waitForMessage( "response", "threads" ),
		    sendMessageStep( DebugMessages.getStackTraceRequest( 6 ) ),
		    waitForMessageThenSend(
		        "response",
		        "stacktrace",
		        ( found ) -> {
			        StackTraceResponse m = ( StackTraceResponse ) found;

			        return DebugMessages.getScopeRequest( 7, m.body.stackFrames.get( 0 ).id );
		        }
		    ),
		    waitForMessageThenSend(
		        "response",
		        "scopes",
		        ( found ) -> {
			        ScopeResponse m		= ( ScopeResponse ) found;

			        Scope	variables	= m.body.scopes.stream().filter( ( scope ) -> scope.name.equalsIgnoreCase( "variables" ) ).findFirst().get();

			        return DebugMessages.getVariablesRequest( 8, variables.variablesReference );
		        }
		    ),
		    waitForMessageThenSend(
		        8,
		        ( found ) -> {
			        VariablesResponse m	= ( VariablesResponse ) found;

			        Variable	value	= m.body.variables.stream().filter( ( variable ) -> variable.name.equalsIgnoreCase( "data" ) ).findFirst().get();

			        return DebugMessages.getVariablesRequest( 9, value.variablesReference );
		        }
		    ),
		    waitForSeq( 9 ),
		    sendMessageStep( DebugMessages.getContinueRequest( 10 ) ),
		    waitForMessage( "response", "continue" ),
		    waitForMessage( "event", "terminated", 5000 )
		) )
		    .stream()
		    .filter( ( m ) -> {
			    return m instanceof TerminatedEvent;
		    } )
		    .findFirst()
		    .get();

		assertThat( message.getType() ).isEqualTo( "event" );
		assertThat( message.getCommand() ).isEqualTo( "terminated" );
		assertThat( message.getSeq() ).isEqualTo( -1 );

		assertThat( message.body.restart ).isEqualTo( false );
	}

}
