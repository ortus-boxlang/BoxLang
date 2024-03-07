package TestCases.debugger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import com.fasterxml.jackson.jr.ob.JSONObjectException;

import ortus.boxlang.debugger.AdapterProtocolMessageReader;
import ortus.boxlang.debugger.IAdapterProtocolMessage;
import ortus.boxlang.debugger.event.ContinuedEvent;
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
import ortus.boxlang.runtime.types.util.JSONUtil;

public class DebugMessages {

	public static AdapterProtocolMessageReader messageReader = getMessageReader();

	public static AdapterProtocolMessageReader getMessageReader() {
		try {
			AdapterProtocolMessageReader reader = new AdapterProtocolMessageReader( new ByteArrayInputStream( new byte[ 2048 ] ) );

			reader.throwOnUnregisteredCommand = false;

			reader.register( "threads", ThreadsResponse.class );
			reader.register( "continue", ContinueResponse.class );
			reader.register( "initialize", InitializeResponse.class );
			reader.register( "scopes", ScopeResponse.class );
			reader.register( "setbreakpoints", SetBreakpointsResponse.class );
			reader.register( "stacktrace", StackTraceResponse.class );
			reader.register( "variables", VariablesResponse.class );
			reader.register( "evaluate", EvaluateResponse.class );
			reader.register( "launch", NoBodyResponse.class );
			reader.register( "continued", ContinuedEvent.class );
			reader.register( "exited", ExitEvent.class );
			reader.register( "output", OutputEvent.class );
			reader.register( "stopped", StoppedEvent.class );
			reader.register( "terminated", TerminatedEvent.class );

			return reader;

		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public static Predicate<Map<String, Object>> getMessageMatcher( String type, String name ) {
		return ( data ) -> {
			String key = type == "event" ? "event" : "command";

			return ( ( String ) data.get( "type" ) ).equalsIgnoreCase( type )
			    && ( ( String ) data.get( key ) ).equalsIgnoreCase( name );
		};
	}

	public static interface TriConsumer<A, B, C> {

		public void accept( A a, B b, C c );
	}

	public static <A, B, C> TriConsumer<byte[], ByteArrayInputStream, ByteArrayOutputStream> waitForSeq( int seq ) {
		return waitForMessage(
		    ( message ) -> {
			    Map<String, Object> data = message.getRawMessageData();

			    return ( ( int ) data.get( "seq" ) ) == seq;
		    },
		    ( message ) -> {
		    },
		    5000L
		);
	}

	public static <A, B, C> TriConsumer<byte[], ByteArrayInputStream, ByteArrayOutputStream> waitForMessage(
	    String type,
	    String command,
	    List<IAdapterProtocolMessage> messages,
	    int skip ) {
		var ref = new Object() {

			public int toSkip = skip;
		};

		return waitForMessage(
		    ( message ) -> {
			    Map<String, Object> data = message.getRawMessageData();

			    String			key		= type == "event" ? "event" : "command";

			    boolean			matches	= ( ( String ) data.get( "type" ) ).equalsIgnoreCase( type )
			        && ( ( String ) data.get( key ) ).equalsIgnoreCase( command );

			    if ( !matches ) {
				    return false;
			    }

			    if ( ref.toSkip > 0 ) {
				    ref.toSkip--;
				    return false;
			    }

			    return true;
		    },
		    ( message ) -> {
			    messages.add( message );
		    },
		    10000
		);
	}

	public static <A, B, C> TriConsumer<byte[], ByteArrayInputStream, ByteArrayOutputStream> waitForMessage(
	    String type,
	    String command,
	    long timeout ) {
		return waitForMessage(
		    ( message ) -> {
			    Map<String, Object> data = message.getRawMessageData();

			    String			key		= type == "event" ? "event" : "command";

			    return ( ( String ) data.get( "type" ) ).equalsIgnoreCase( type )
			        && ( ( String ) data.get( key ) ).equalsIgnoreCase( command );
		    },
		    ( message ) -> {
		    },
		    timeout
		);
	}

	public static <A, B, C, D> TriConsumer<byte[], ByteArrayInputStream, ByteArrayOutputStream> waitForMessageThenSend(
	    int seq,
	    Function func ) {
		return ( byteArray, inputStream, reader ) -> {
			waitForMessage(
			    ( message ) -> {
				    Map<String, Object> data = message.getRawMessageData();

				    return ( ( Integer ) data.get( "seq" ) ).equals( seq );
			    },
			    ( message ) -> {
				    sendMessageStep( ( Map<String, Object> ) func.apply( ( IAdapterProtocolMessage ) message ) ).accept( byteArray, inputStream, reader );
			    },
			    5000
			).accept( byteArray, inputStream, reader );
		};
	}

	public static <A, B, C, D> TriConsumer<byte[], ByteArrayInputStream, ByteArrayOutputStream> waitForMessageThenSend(
	    String type,
	    String command,
	    Function func ) {
		return ( byteArray, inputStream, reader ) -> {
			waitForMessage(
			    ( message ) -> {
				    Map<String, Object> data = message.getRawMessageData();

				    String			key		= type == "event" ? "event" : "command";

				    return ( ( String ) data.get( "type" ) ).equalsIgnoreCase( type )
				        && ( ( String ) data.get( key ) ).equalsIgnoreCase( command );
			    },
			    ( message ) -> {
				    sendMessageStep( ( Map<String, Object> ) func.apply( ( IAdapterProtocolMessage ) message ) ).accept( byteArray, inputStream, reader );
			    },
			    5000
			).accept( byteArray, inputStream, reader );
		};
	}

	public static <A, B, C> TriConsumer<byte[], ByteArrayInputStream, ByteArrayOutputStream> waitForMessage(
	    String type,
	    String command ) {
		return waitForMessage(
		    ( message ) -> {
			    Map<String, Object> data = message.getRawMessageData();

			    String			key		= type == "event" ? "event" : "command";

			    return ( ( String ) data.get( "type" ) ).equalsIgnoreCase( type )
			        && ( ( String ) data.get( key ) ).equalsIgnoreCase( command );
		    },
		    ( message ) -> {
		    },
		    5000L
		);
	}

	public static <A, B, C> TriConsumer<byte[], ByteArrayInputStream, ByteArrayOutputStream> waitForMessage( Predicate<IAdapterProtocolMessage> test ) {
		return waitForMessage( test, ( message ) -> {
		}, 5000L );
	}

	public static <A, B, C> TriConsumer<byte[], ByteArrayInputStream, ByteArrayOutputStream> waitForMessage( Predicate<IAdapterProtocolMessage> test,
	    Consumer<IAdapterProtocolMessage> onMessage ) {
		return waitForMessage( test, onMessage, 5000L );
	}

	public static <A, B, C> TriConsumer<byte[], ByteArrayInputStream, ByteArrayOutputStream> waitForMessage( Predicate<IAdapterProtocolMessage> test,
	    Consumer<IAdapterProtocolMessage> onMessage, long timeout ) {
		return ( a, b, output ) -> {
			long startTime = System.currentTimeMillis();

			while ( System.currentTimeMillis() - startTime <= timeout ) {
				try {
					messageReader.changeInputStream( new ByteArrayInputStream( output.toByteArray() ) );
					AdapterProtocolMessageReader reader = messageReader;
					reader.throwOnUnregisteredCommand = false;
					IAdapterProtocolMessage message = reader.read();

					while ( message != null ) {
						if ( test.test( message ) ) {
							onMessage.accept( message );
							return;
						}
						message = reader.read();
					}

					Thread.sleep( 50 );

				} catch ( IOException e ) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch ( InterruptedException e ) {
					// pass
				}
			}
		};
	}

	public static <A, B, C> TriConsumer<byte[], ByteArrayInputStream, ByteArrayOutputStream> readMessageStep( List<Map<String, Object>> messages ) {
		return ( a, b, output ) -> {
			try {
				AdapterProtocolMessageReader reader = new AdapterProtocolMessageReader( new ByteArrayInputStream( output.toByteArray() ) );
				reader.throwOnUnregisteredCommand = false;
				IAdapterProtocolMessage	message	= reader.read();
				int						i		= 0;

				while ( message != null ) {
					if ( i >= messages.size() ) {
						messages.add( message.getRawMessageData() );
					}
					i++;
					message = reader.read();
				}

			} catch ( IOException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		};
	}

	public static <A, B, C> TriConsumer<byte[], ByteArrayInputStream, ByteArrayOutputStream> delayStep( long delay ) {
		return ( a, b, reader ) -> {
			try {
				Thread.sleep( delay );
			} catch ( InterruptedException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		};
	}

	public static <A, B, C> TriConsumer<byte[], ByteArrayInputStream, ByteArrayOutputStream> sendMessageStep( Map<String, Object> map ) {
		return ( byteArray, inputStream, reader ) -> {

			// clear buffer
			for ( int i = 0; i < byteArray.length; i++ ) {
				byteArray[ i ] = 0;
			}

			String jsonMessage;
			try {
				jsonMessage = JSONUtil.getJSONBuilder().asString( map );

				// @formatter:off
				// prettier-ignore
				String protocolMessage = String.format("""
Content-Length: %d
				
%s""", jsonMessage.getBytes().length, jsonMessage );
				byte[] messageBytes = protocolMessage.getBytes();

				// write message to byte array
				int offset = byteArray.length - messageBytes.length - 1;
				for ( int i = 0; i < messageBytes.length; i++ ) {
					byteArray[ i + offset ] = messageBytes[ i ];
				}

				inputStream.reset();
				inputStream.skip( offset );

			} catch ( JSONObjectException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch ( IOException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		};
	}

	public static Map<String, Object> getInitRequest( int seq ) {
		Map<String, Object> initializeRequest = new HashMap<String, Object>();
		initializeRequest.put( "command", "initialize" );
		initializeRequest.put( "type", "request" );
		initializeRequest.put( "seq", seq );
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

		return initializeRequest;
	}

	public static Map<String, Object> getLaunchRequest( int seq ) {
		Map<String, Object> launchRequest = new HashMap<String, Object>();
		launchRequest.put( "command", "launch" );
		launchRequest.put( "type", "request" );
		launchRequest.put( "seq", seq );
		Map<String, Object> arguments = new HashMap<String, Object>();
		arguments.put( "type", "boxlang" );
		arguments.put( "request", "launch" );
		arguments.put( "name", "Run BoxLang Program" );
		arguments.put( "program", Paths.get( "src/test/java/TestCases/debugger/main.cfs" ).toAbsolutePath().toString() );
		arguments.put( "__configurationTarget", 6 );
		arguments.put( "__sessionId", "0e92688e-8cc0-47d6-ad83-74978ec3798f" );
		launchRequest.put( "arguments", arguments );

		return launchRequest;
	}

	public static Map<String, Object> getSetBreakpointsRequest( int seq, int[] lines ) {
		Map<String, Object> request = new HashMap<String, Object>();
		request.put( "command", "setBreakpoints" );
		request.put( "type", "request" );
		request.put( "seq", seq );

		Map<String, Object> arguments = new HashMap<String, Object>();
		request.put( "arguments", arguments );

		Map<String, Object> source = new HashMap<String, Object>();
		arguments.put( "source", source );
		source.put( "name", "main.cfs" );
		source.put( "path", Paths.get( "src/test/java/TestCases/debugger/main.cfs" ).toAbsolutePath().toString() );

		arguments.put( "lines", lines );
		List<Map<String,Object>> lineMaps = IntStream.of( lines ).mapToObj( ( lineNum ) -> {
			Map<String, Object> lineMap = new HashMap<String, Object>();
			lineMap.put( "line", lineNum );

			return lineMap;
		}).toList();
		
		arguments.put( "breakpoints", lineMaps );

		return request;
	}
	
	/*
	 * {"command":"configurationDone","type":"request","seq":6}
	 */
	public static Map<String, Object> getConfigurationDoneRequest( int seq ) {
		Map<String, Object> request = new HashMap<String, Object>();
		request.put( "command", "configurationDone" );
		request.put( "type", "request" );
		request.put( "seq", seq );

		return request;
	}

	public static Map<String, Object> getThreadsRequest( int seq ) {
		Map<String, Object> request = new HashMap<String, Object>();
		request.put( "command", "threads" );
		request.put( "type", "request" );
		request.put( "seq", seq );

		return request;
	}
	
	public static Map<String, Object> getStackTraceRequest( int seq ) {
		Map<String, Object> request = new HashMap<String, Object>();
		request.put( "command", "stacktrace" );
		request.put( "type", "request" );
		request.put( "seq", seq );

		Map<String, Object> arguments = new HashMap<String, Object>();
		request.put( "arguments", arguments );
		arguments.put( "threadId", 1 );

		return request;
	}
	
	public static Map<String, Object> getScopeRequest( int seq, int frameId ) {
		Map<String, Object> request = new HashMap<String, Object>();
		request.put( "command", "scopes" );
		request.put( "type", "request" );
		request.put( "seq", seq );

		Map<String, Object> arguments = new HashMap<String, Object>();
		request.put( "arguments", arguments );
		arguments.put( "frameId", frameId );

		return request;
	}

	public static Map<String, Object> getVariablesRequest( int seq, int variablesReference ) {
		Map<String, Object> request = new HashMap<String, Object>();
		request.put( "command", "variables" );
		request.put( "type", "request" );
		request.put( "seq", seq );

		Map<String, Object> arguments = new HashMap<String, Object>();
		request.put( "arguments", arguments );
		arguments.put( "variablesReference", variablesReference );

		return request;
	}
	
	public static Map<String, Object> getContinueRequest( int seq ) {
		Map<String, Object> request = new HashMap<String, Object>();
		request.put( "command", "continue" );
		request.put( "type", "request" );
		request.put( "seq", seq );

		Map<String, Object> arguments = new HashMap<String, Object>();
		request.put( "arguments", arguments );
		arguments.put( "threadId", 1 );

		return request;
	}

	public static Map<String, Object> getEvaluateRequest( int seq, String expression, int frameId ) {
		Map<String, Object> request = new HashMap<String, Object>();
		request.put( "command", "evaluate" );
		request.put( "type", "request" );
		request.put( "seq", seq );

		Map<String, Object> arguments = new HashMap<String, Object>();
		request.put( "arguments", arguments );
		arguments.put( "expression", expression );
		arguments.put( "frameId", frameId );
		arguments.put( "context", "repl" );

		return request;
	}
}
