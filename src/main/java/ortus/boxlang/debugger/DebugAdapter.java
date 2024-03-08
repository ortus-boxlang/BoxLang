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

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.Location;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.event.BreakpointEvent;

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
import ortus.boxlang.debugger.request.StackTraceRequest;
import ortus.boxlang.debugger.request.StepInRequest;
import ortus.boxlang.debugger.request.StepOutRequest;
import ortus.boxlang.debugger.request.ThreadsRequest;
import ortus.boxlang.debugger.request.VariablesRequest;
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
import ortus.boxlang.debugger.types.Source;
import ortus.boxlang.debugger.types.StackFrame;
import ortus.boxlang.debugger.types.Variable;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.runnables.compiler.JavaBoxpiler;
import ortus.boxlang.runtime.runnables.compiler.SourceMap;
import ortus.boxlang.runtime.types.BoxLangType;

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
	private JavaBoxpiler					javaBoxpiler;
	private AdapterProtocolMessageReader	DAPReader;

	private List<BreakpointRequest>			breakpoints	= new ArrayList<BreakpointRequest>();

	public static void startDAPServer( int port ) {

		System.out.println( "starting the debug server" );

		try ( ServerSocket socket = new ServerSocket( port ) ) {
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
	 * 
	 * @param debugClient The socket that handles communication with the debug tool
	 */
	public DebugAdapter( InputStream inputStream, OutputStream outputStream ) {
		this.logger			= LoggerFactory.getLogger( BoxRuntime.class );
		this.inputStream	= inputStream;
		this.outputStream	= outputStream;
		this.requestQueue	= new ArrayList<IAdapterProtocolMessage>();
		this.javaBoxpiler	= JavaBoxpiler.getInstance();

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
			    .register( "pause", PauseRequest.class )
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
		this.debugger.continueExecution( debugRequest.arguments.threadId, debugRequest.arguments.singleThread );

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

		try {
			WrappedValue	value		= this.debugger.evaluateInContext( debugRequest.arguments.expression, debugRequest.arguments.frameId );
			Variable		varValue	= JDITools.getVariable( "evaluated", value );
			new EvaluateResponse( debugRequest, varValue ).send( this.outputStream );
		} catch ( InvocationException e ) {
			String message = JDITools.wrap( this.debugger.bpe.thread(), e.exception() ).invoke( "getMessage" ).asStringReference().value();
			new EvaluateResponse( debugRequest, message ).send( this.outputStream );
		} catch ( Exception e ) {
			new EvaluateResponse( debugRequest, e.toString() ).send( this.outputStream );
		}

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
		    .toList();

		try {

			new ThreadsResponse( debugRequest, threads ).send( this.outputStream );
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}

	/**
	 * Visit ThreadRequest instances. Should send a ThreadResponse contianing basic information about all vm threds.
	 * 
	 * @param debugRequest
	 */
	public void visit( StackTraceRequest debugRequest ) {
		try {

			List<StackFrame> stackFrames = this.debugger.getBoxLangStackFrames( debugRequest.arguments.threadId ).stream()
			    .map( ( tuple ) -> {
				    com.sun.jdi.StackFrame stackFrame = tuple.stackFrame();
				    com.sun.jdi.Location location	= tuple.location();
				    StackFrame			sf			= new StackFrame();
				    SourceMap			map			= javaBoxpiler.getSourceMapFromFQN( location.declaringType().name() );
				    String				fileName	= Path.of( map.source ).getFileName().toString();

				    sf.id	= tuple.id();
				    sf.line	= location.lineNumber();
				    sf.column = 1;
				    sf.name	= location.method().name();

				    Integer sourceLine = map.convertJavaLineToSourceLine( sf.line );
				    if ( sourceLine != null ) {
					    sf.line = sourceLine;
				    }

				    BoxLangType blType = this.debugger.determineBoxLangType( location.declaringType() );

				    if ( blType == BoxLangType.UDF ) {
					    sf.name	= this.debugger.getObjectFromStackFrame( stackFrame )
					        .property( "name" )
					        .property( "originalValue" )
					        .asStringReference().value();
					    sf.source = new Source();
				    } else if ( blType == BoxLangType.CLOSURE ) {
					    // TODO figure out how to get the name of the closure from a parent context
					    String calledName = this.debugger.getContextForStackFrame( tuple ).invoke( "findClosestFunctionName" ).invoke( "getOriginalValue" )
					        .asStringReference().value();

					    sf.name	= calledName + " (closure)";
					    sf.source = new Source();
				    } else if ( blType == BoxLangType.LAMBDA ) {
					    String calledName = this.debugger.getContextForStackFrame( tuple ).invoke( "findClosestFunctionName" ).invoke( "getOriginalValue" )
					        .asStringReference().value();
					    sf.name	= calledName + " (lambda)";
					    sf.source = new Source();
				    } else if ( map != null && map.isTemplate() ) {
					    sf.name	= map.getFileName();
					    sf.source = new Source();

				    }

				    if ( sf.source != null ) {
					    sf.source.path = map.source.toString();
					    sf.source.name = fileName;
				    }

				    return sf;
			    } )
			    .toList();

			new StackTraceResponse( debugRequest, stackFrames ).send( this.outputStream );
		} catch ( IncompatibleThreadStateException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void visit( ScopeRequest debugRequest ) {
		try {
			WrappedValue	context				= this.debugger.getContextForStackFrame( debugRequest.arguments.frameId );

			List<Scope>		scopes				= new ArrayList<Scope>();

			WrappedValue	visibleScopes		= context.invokeByNameAndArgs( "getVisibleScopes", new ArrayList<String>(),
			    new ArrayList<com.sun.jdi.Value>() );
			WrappedValue	contextualScopes	= visibleScopes.invokeByNameAndArgs( "get", Arrays.asList( "java.lang.String" ),
			    Arrays.asList( this.debugger.vm.mirrorOf( "contextual" ) ) );

			scopes = contextualScopes.invoke( "getKeysAsStrings" )
			    .invoke( "toArray" )
			    .asArrayReference()
			    .getValues()
			    .stream()
			    .map( ( scopeNameValue ) -> ( String ) ( ( com.sun.jdi.StringReference ) scopeNameValue ).value() )
			    .map( ( scopeName ) -> scopeByName( context, scopeName ) )
			    .filter( ( scope ) -> scope != null )
			    .toList();

			new ScopeResponse( debugRequest, scopes ).send( this.outputStream );
		} catch ( Exception e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private IVMInitializationStrategy getInitStrategy( LaunchRequest launchRequest ) {

		if ( launchRequest.arguments.program != null ) {
			return new InlineStrategy( launchRequest.arguments.program );
		} else if ( launchRequest.arguments.serverPort != null ) {
			return new AttachStrategy( launchRequest.arguments.serverPort );
		}

		throw new RuntimeException( "Invalid launch request arguments" );
	}

	private Scope scopeByName( WrappedValue context, String key ) {
		WrappedValue scopeValue = context.invokeByNameAndArgs(
		    "getScopeNearby",
		    Arrays.asList( "ortus.boxlang.runtime.scopes.Key", "boolean" ),
		    Arrays.asList( this.debugger.mirrorOfKey( key ), this.debugger.vm.mirrorOf( false ) ) );

		if ( scopeValue == null ) {
			return null;
		}

		Scope scope = new Scope();
		scope.name					= key;
		scope.variablesReference	= ( int ) scopeValue.id();

		if ( key == "arguments" ) {
			scope.presentationHint = "arguments";
		} else if ( key == "local" ) {
			scope.presentationHint = "locals";
		}

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
	}

	private SourceMap getSourceMapFromJavaLocation( Location location ) {
		return javaBoxpiler.getSourceMapFromFQN( location.declaringType().name() );
	}

	// ===================================================
	// ================= EVENTS ==========================
	// ===================================================

	public void sendStoppedEventForBreakpoint( BreakpointEvent breakpointEvent ) {
		SourceMap			map			= javaBoxpiler.getSourceMapFromFQN( breakpointEvent.location().declaringType().name() );
		String				sourcePath	= map.source.toLowerCase();

		BreakpointRequest	bp			= null;

		for ( BreakpointRequest b : this.breakpoints ) {
			if ( b.source.compareToIgnoreCase( sourcePath ) == 0 ) {
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
