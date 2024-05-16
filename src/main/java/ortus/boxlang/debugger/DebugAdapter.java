/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.debugger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jdi.InvocationException;
import com.sun.jdi.Location;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.event.BreakpointEvent;

import ortus.boxlang.compiler.IBoxpiler;
import ortus.boxlang.compiler.SourceMap;
import ortus.boxlang.compiler.javaboxpiler.JavaBoxpiler;
import ortus.boxlang.debugger.BoxLangDebugger.StackFrameTuple;
import ortus.boxlang.debugger.JDITools.WrappedValue;
import ortus.boxlang.debugger.event.Event;
import ortus.boxlang.debugger.event.StoppedEvent;
import ortus.boxlang.debugger.request.ConfigurationDoneRequest;
import ortus.boxlang.debugger.request.ContinueRequest;
import ortus.boxlang.debugger.request.DisconnectRequest;
import ortus.boxlang.debugger.request.EvaluateRequest;
import ortus.boxlang.debugger.request.InitializeRequest;
import ortus.boxlang.debugger.request.LaunchRequest;
import ortus.boxlang.debugger.request.NextRequest;
import ortus.boxlang.debugger.request.PauseRequest;
import ortus.boxlang.debugger.request.ScopeRequest;
import ortus.boxlang.debugger.request.SetBreakpointsRequest;
import ortus.boxlang.debugger.request.SetExceptionBreakpointsRequest;
import ortus.boxlang.debugger.request.SetVariableRequest;
import ortus.boxlang.debugger.request.StackTraceRequest;
import ortus.boxlang.debugger.request.StepInRequest;
import ortus.boxlang.debugger.request.StepOutRequest;
import ortus.boxlang.debugger.request.TerminateRequest;
import ortus.boxlang.debugger.request.ThreadsRequest;
import ortus.boxlang.debugger.request.VariablesRequest;
import ortus.boxlang.debugger.response.ContinueResponse;
import ortus.boxlang.debugger.response.EvaluateResponse;
import ortus.boxlang.debugger.response.InitializeResponse;
import ortus.boxlang.debugger.response.NoBodyResponse;
import ortus.boxlang.debugger.response.ScopeResponse;
import ortus.boxlang.debugger.response.SetBreakpointsResponse;
import ortus.boxlang.debugger.response.SetVariableResponse;
import ortus.boxlang.debugger.response.StackTraceResponse;
import ortus.boxlang.debugger.response.ThreadsResponse;
import ortus.boxlang.debugger.response.VariablesResponse;
import ortus.boxlang.debugger.types.Breakpoint;
import ortus.boxlang.debugger.types.Scope;
import ortus.boxlang.debugger.types.Source;
import ortus.boxlang.debugger.types.StackFrame;
import ortus.boxlang.debugger.types.Variable;
import ortus.boxlang.runtime.BoxRuntime;

/**
 * Implements Microsoft's Debug Adapter Protocol https://microsoft.github.io/debug-adapter-protocol/
 */
public class DebugAdapter {

	private Thread							inputThread;
	private Logger							logger;
	private InputStream						inputStream;
	private OutputStream					outputStream;
	private BoxLangDebugger					debugger;
	private boolean							running		= true;
	private List<IAdapterProtocolMessage>	requestQueue;
	private IBoxpiler						boxpiler;
	private AdapterProtocolMessageReader	DAPReader;

	private List<BreakpointRequest>			breakpoints	= new ArrayList<BreakpointRequest>();

	public static void startDAPServer( int port ) {

		System.out.println( "starting the debug server" );

		try ( ServerSocket socket = new ServerSocket( port ) ) {
			if ( port == 0 ) {
				System.out.println( String.format( "Listening on port: %s", socket.getLocalPort() ) );
			}
			while ( true ) {
				Socket			connectionSocket	= socket.accept();
				DebugAdapter	adapter				= new DebugAdapter( connectionSocket.getInputStream(), connectionSocket.getOutputStream() );

				while ( adapter.isRunning() ) {
					// wait until adapter is finished
				}

				connectionSocket.close();
				System.out.println( "Closing debug connection" );
			}
		} catch ( IOException e ) {
			e.printStackTrace();
		}
	}

	/**
	 * Constructor
	 */
	public DebugAdapter( InputStream inputStream, OutputStream outputStream ) {
		this.logger			= LoggerFactory.getLogger( BoxRuntime.class );
		this.inputStream	= inputStream;
		this.outputStream	= outputStream;
		this.requestQueue	= new ArrayList<IAdapterProtocolMessage>();
		this.boxpiler		= JavaBoxpiler.getInstance();

		try {
			this.DAPReader = new AdapterProtocolMessageReader( inputStream );

			this.DAPReader.register( "initialize", InitializeRequest.class )
			    .register( "launch", LaunchRequest.class )
			    .register( "evaluate", EvaluateRequest.class )
			    .register( "setBreakpoints", SetBreakpointsRequest.class )
			    .register( "configurationDone", ConfigurationDoneRequest.class )
			    .register( "threads", ThreadsRequest.class )
			    .register( "stackTrace", StackTraceRequest.class )
			    .register( "next", NextRequest.class )
			    .register( "setexceptionbreakpoints", SetExceptionBreakpointsRequest.class )
			    .register( "stepin", StepInRequest.class )
			    .register( "stepout", StepOutRequest.class )
			    .register( "scopes", ScopeRequest.class )
			    .register( "variables", VariablesRequest.class )
			    .register( "continue", ContinueRequest.class )
			    .register( "setvariable", SetVariableRequest.class )
			    .register( "pause", PauseRequest.class )
			    .register( "terminate", TerminateRequest.class )
			    .register( "disconnect", DisconnectRequest.class );
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			createInputListenerThread();
			startMainLoop();
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Used to determin if the debug session has completed.
	 *
	 * @return
	 */
	public boolean isRunning() {
		return this.running;
	}

	private void startMainLoop() {
		while ( this.running ) {
			processDebugProtocolMessages();

			if ( this.debugger != null ) {
				this.debugger.keepWorking();
			}
		}
	}

	private void processDebugProtocolMessages() {
		synchronized ( this ) {
			while ( requestQueue.size() > 0 ) {

				IAdapterProtocolMessage request = null;
				if ( requestQueue.size() > 0 ) {
					request = requestQueue.remove( 0 );
				}

				if ( request != null ) {
					request.accept( this );
				}
			}

		}
	}

	/**
	 * Starts a new thread to wait for messages from the debug client. Each message will deserialized and then visited.
	 *
	 * @throws IOException
	 */
	private void createInputListenerThread() throws IOException {
		InputStreamReader	iSR		= new InputStreamReader( this.inputStream );
		BufferedReader		bR		= new BufferedReader( iSR );
		DebugAdapter		adapter	= this;

		this.inputThread = new Thread( new Runnable() {

			@Override
			public void run() {
				while ( true ) {

					try {
						IAdapterProtocolMessage message = DAPReader.read();
						if ( message != null ) {
							synchronized ( adapter ) {
								requestQueue.add( message );
							}
						}
					} catch ( SocketException e ) {
						logger.info( "Socket was closed" );
						break;
					} catch ( IOException e ) {
						// TODO handle this exception
						e.printStackTrace();
						break;
					}

				}
			}

		} );

		this.inputThread.start();
	}

	/**
	 * Default visit handler
	 *
	 * @param debugRequest
	 */
	public void visit( IAdapterProtocolMessage debugRequest ) {
		throw new NotImplementedException( debugRequest.getCommand() );
	}

	public void visit( TerminateRequest debugRequest ) {
		new NoBodyResponse( debugRequest ).send( this.outputStream );

		this.debugger.terminate();
	}

	public void visit( SetVariableRequest debugRequest ) {
		this.debugger.upateVariableByReference(
		    debugRequest.arguments.variablesReference,
		    debugRequest.arguments.name,
		    debugRequest.arguments.value
		);

		var variables = JDITools.getVariablesFromSeen( debugRequest.arguments.variablesReference );

		variables.stream()
		    .filter( v -> v.name.equalsIgnoreCase( debugRequest.arguments.name ) )
		    .findFirst()
		    .ifPresent( ( variable ) -> {
			    new SetVariableResponse( debugRequest, variable ).send( this.outputStream );
		    } );
	}

	/**
	 * Visit InitializeRequest instances. Respond to the initialize request and send an initialized event.
	 *
	 * @param debugRequest
	 */
	public void visit( InitializeRequest debugRequest ) {
		new InitializeResponse( debugRequest ).send( this.outputStream );
		new Event( "initialized" ).send( this.outputStream );
	}

	/**
	 * Visit InitializeRequest instances. Respond to the initialize request and send an initialized event.
	 *
	 * @param debugRequest
	 */
	public void visit( ContinueRequest debugRequest ) {
		this.debugger.continueExecution( debugRequest.arguments.threadId, debugRequest.arguments.singleThread, true );

		new ContinueResponse( debugRequest, true ).send( this.outputStream );
	}

	public void visit( SetExceptionBreakpointsRequest debugRequest ) {
		boolean	any		= false;
		String	matcher	= null;

		for ( String filter : debugRequest.arguments.filters ) {
			if ( filter.equalsIgnoreCase( "any" ) ) {
				any = true;
				continue;
			}

			if ( filter.equalsIgnoreCase( "matcher" ) ) {
				matcher = "";
				continue;
			}
		}

		this.debugger.configureExceptionBreakpoints( any, matcher );

		new NoBodyResponse( debugRequest ).send( this.outputStream );
	}

	/**
	 * Visit InitializeRequest instances. Respond to the initialize request and send an initialized event.
	 *
	 * @param debugRequest
	 */
	public void visit( NextRequest debugRequest ) {
		this.debugger.startStepping( debugRequest.arguments.threadId, new NextStepStrategy() );

		new NoBodyResponse( debugRequest ).send( this.outputStream );
	}

	public void visit( StepInRequest debugRequest ) {
		this.debugger.startStepping( debugRequest.arguments.threadId, new StepInStrategy() );

		new NoBodyResponse( debugRequest ).send( this.outputStream );
	}

	public void visit( StepOutRequest debugRequest ) {
		this.debugger.startStepping( debugRequest.arguments.threadId, new StepOutStrategy() );

		new NoBodyResponse( debugRequest ).send( this.outputStream );
	}

	/**
	 * Visit LaunchRequest instances. Send a NobodyResponse and setup a BoxLangDebugger.
	 *
	 * @param debugRequest
	 */
	public void visit( LaunchRequest debugRequest ) {
		new NoBodyResponse( debugRequest ).send( this.outputStream );

		this.debugger = new BoxLangDebugger( getInitStrategy( debugRequest ), this.outputStream, this );
	}

	/**
	 * Visit SetBreakpointsRequest instances. Send a response.
	 *
	 * @param debugRequest
	 */
	public void visit( SetBreakpointsRequest debugRequest ) {
		this.debugger.setBreakpointsForFile( debugRequest.arguments.source.path, Arrays.asList( debugRequest.arguments.breakpoints ) );

		for ( Breakpoint bp : debugRequest.arguments.breakpoints ) {
			this.breakpoints.add( new BreakpointRequest( bp.id, bp.line, debugRequest.arguments.source.path.toLowerCase() ) );
		}

		new SetBreakpointsResponse( debugRequest ).send( this.outputStream );
	}

	/**
	 * Visit ConfigurationDoneRequest instances. After responding the debugger can begin executing.
	 *
	 * @param debugRequest
	 */
	public void visit( ConfigurationDoneRequest debugRequest ) {
		new NoBodyResponse( debugRequest ).send( this.outputStream );

		this.debugger.initialize();
	}

	/**
	 * Visit ConfigurationDoneRequest instances. After responding the debugger can begin executing.
	 *
	 * @param debugRequest
	 */
	public void visit( PauseRequest debugRequest ) {
		new NoBodyResponse( debugRequest ).send( this.outputStream );

		this.debugger.pauseThread( debugRequest.arguments.threadId ).ifPresent( ( location ) -> {
			var sourceMap = getSourceMapFromJavaLocation( location );
			this.breakpoints.add( new BreakpointRequest( -1, 0, sourceMap.source ) );
		} );
	}

	/**
	 * Visit EvaluateRequest instances. Will evalauate the expression in either the global scope or a specific stackframe.
	 *
	 * @param debugRequest
	 */
	public void visit( EvaluateRequest debugRequest ) {
		this.debugger.evaluateInContext(
		    debugRequest.arguments.expression,
		    debugRequest.arguments.frameId
		)
		    .whenCompleteAsync( ( wrappedValue, ex ) -> {
			    if ( ex != null ) {
				    var message = ex instanceof InvocationException ie
				        ? this.debugger.getInternalExceptionMessage( ie )
				        : ex.toString();

				    new EvaluateResponse( debugRequest, message ).send( this.outputStream );
				    return;
			    }

			    Variable varValue = JDITools.getVariable( "evaluated", wrappedValue );
			    new EvaluateResponse( debugRequest, varValue ).send( this.outputStream );
		    } );
	}

	/**
	 * Visit ThreadRequest instances. Should send a ThreadResponse contianing basic information about all vm threds.
	 *
	 * @param debugRequest
	 */
	public void visit( ThreadsRequest debugRequest ) {
		List<ortus.boxlang.debugger.types.Thread> threads = this.debugger.getAllThreadReferences()
		    .stream()
		    .filter( ( ctr ) -> ctr.getBoxLangStackFrames().size() > 0 )
		    .map( ( ctr ) -> {
			    ortus.boxlang.debugger.types.Thread t = new ortus.boxlang.debugger.types.Thread();
			    t.id = ( int ) ctr.threadReference.uniqueID();
			    t.name = ctr.threadReference.name();

			    return t;
		    } )
		    .collect( Collectors.toList() );

		new ThreadsResponse( debugRequest, threads ).send( this.outputStream );
	}

	/**
	 * Visit ThreadRequest instances. Should send a ThreadResponse contianing basic information about all vm threds.
	 *
	 * @param debugRequest
	 */
	public void visit( StackTraceRequest debugRequest ) {
		List<StackFrame> stackFrames = this.debugger.getBoxLangStackFrames( debugRequest.arguments.threadId ).stream()
		    .map( convertStackFrameTupleToDAPStackFrame( boxpiler, debugger ) )
		    .collect( Collectors.toList() );
		new StackTraceResponse( debugRequest, stackFrames ).send( this.outputStream );
	}

	public void visit( ScopeRequest debugRequest ) {
		this.debugger.getVisibleScopes( debugRequest.arguments.frameId )
		    .thenAccept( ( scopes ) -> {
			    new ScopeResponse(
			        debugRequest,
			        scopes.stream().map( DebugAdapter::convertScopeToDAPScope ).toList()
			    ).send( this.outputStream );
		    } );
	}

	private IVMInitializationStrategy getInitStrategy( LaunchRequest launchRequest ) {

		if ( launchRequest.arguments.program != null ) {
			return new InlineStrategy( launchRequest.arguments.program );
		} else if ( launchRequest.arguments.serverPort != null ) {
			return new AttachStrategy( launchRequest.arguments.serverPort );
		} else if ( launchRequest.arguments.debugType != null && launchRequest.arguments.debugType.equalsIgnoreCase( "local_web" ) ) {
			return new InlineWebServerInitializationStrategy( launchRequest.arguments.webPort, launchRequest.arguments.webRoot );
		}

		throw new RuntimeException( "Invalid launch request arguments" );
	}

	public Function<StackFrameTuple, StackFrame> convertStackFrameTupleToDAPStackFrame( IBoxpiler boxpiler, BoxLangDebugger debugger ) {
		return ( tuple ) -> {
			StackFrame sf = new StackFrame();
			sf.id		= tuple.id();
			sf.column	= 1;

			SourceMap map = boxpiler.getSourceMapFromFQN( tuple.location().declaringType().name() );

			sf.line = tuple.location().lineNumber();
			Integer sourceLine = map.convertJavaLineToSourceLine( sf.line );
			if ( sourceLine != null ) {
				sf.line = sourceLine;
			}

			sf.name = tuple.location().method().name();
			String stackFrameName = debugger.getStackFrameName( tuple );
			if ( stackFrameName != null ) {
				sf.name = stackFrameName;
			} else if ( map != null && map.isTemplate() ) {
				sf.name = map.getFileName();
			}

			sf.source = new Source();
			if ( sf.source != null ) {
				sf.source.path	= map.source.toString();
				sf.source.name	= Path.of( map.source ).getFileName().toString();
			}

			return sf;
		};
	}

	public static Scope convertScopeToDAPScope( WrappedValue scopeValue ) {
		Scope scope = new Scope();
		scope.name					= scopeValue.invoke( "getName" ).invoke( "getName" ).asStringReference().value();
		scope.variablesReference	= ( int ) scopeValue.id();

		return scope;
	}

	public void visit( VariablesRequest debugRequest ) {
		List<Variable> ideVars = new ArrayList<Variable>();

		if ( this.debugger.hasSeen( debugRequest.arguments.variablesReference ) ) {
			ideVars = this.debugger.getVariablesFromSeen( debugRequest.arguments.variablesReference );
		}

		new VariablesResponse( debugRequest, ideVars ).send( this.outputStream );
	}

	public void visit( DisconnectRequest debugRequest ) {
		this.running = false;
		new NoBodyResponse( debugRequest ).send( this.outputStream );

		this.debugger.handleDisconnect();
	}

	private SourceMap getSourceMapFromJavaLocation( Location location ) {
		return boxpiler.getSourceMapFromFQN( location.declaringType().name() );
	}

	// ===================================================
	// ================= EVENTS ==========================
	// ===================================================

	public void sendStoppedEventForBreakpoint( BreakpointEvent breakpointEvent ) {
		SourceMap			map			= boxpiler.getSourceMapFromFQN( breakpointEvent.location().declaringType().name() );
		String				sourcePath	= map.source.toLowerCase();

		BreakpointRequest	bp			= null;

		for ( BreakpointRequest b : this.breakpoints ) {
			if ( b.source.equalsIgnoreCase( sourcePath ) ) {
				bp = b;
				break;
			}
		}

		if ( bp == null ) {
			return;
		}
		// TODO convert this file/line number to boxlang
		StoppedEvent.breakpoint( breakpointEvent, bp.id ).send( this.outputStream );
	}

	record ScopeCache( com.sun.jdi.StackFrame stackFrame, ObjectReference scope ) {
	};

	record BreakpointRequest( int id, int line, String source ) {

	}
}
