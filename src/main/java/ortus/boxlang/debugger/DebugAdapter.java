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
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.event.BreakpointEvent;

import ortus.boxlang.debugger.JDITools.WrappedValue;
import ortus.boxlang.debugger.event.Event;
import ortus.boxlang.debugger.event.StoppedEvent;
import ortus.boxlang.debugger.request.ConfigurationDoneRequest;
import ortus.boxlang.debugger.request.ContinueRequest;
import ortus.boxlang.debugger.request.DisconnectRequest;
import ortus.boxlang.debugger.request.InitializeRequest;
import ortus.boxlang.debugger.request.LaunchRequest;
import ortus.boxlang.debugger.request.ScopeRequest;
import ortus.boxlang.debugger.request.SetBreakpointsRequest;
import ortus.boxlang.debugger.request.StackTraceRequest;
import ortus.boxlang.debugger.request.ThreadsRequest;
import ortus.boxlang.debugger.request.VariablesRequest;
import ortus.boxlang.debugger.response.ContinueResponse;
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
import ortus.boxlang.runtime.BoxRunner;
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

	private Map<Integer, BreakpointRequest>	breakpoints	= new HashMap<Integer, BreakpointRequest>();

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
			    .register( "setBreakpoints", SetBreakpointsRequest.class )
			    .register( "configurationDone", ConfigurationDoneRequest.class )
			    .register( "threads", ThreadsRequest.class )
			    .register( "stackTrace", StackTraceRequest.class )
			    .register( "scopes", ScopeRequest.class )
			    .register( "variables", VariablesRequest.class )
			    .register( "continue", ContinueRequest.class )
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
		this.debugger.forceResume();
		new ContinueResponse( debugRequest, true ).send( this.outputStream );
	}

	/**
	 * Visit LaunchRequest instances. Send a NobodyResponse and setup a BoxLangDebugger.
	 * 
	 * @param debugRequest
	 */
	public void visit( LaunchRequest debugRequest ) {
		new NoBodyResponse( debugRequest ).send( this.outputStream );
		this.debugger = new BoxLangDebugger( BoxRunner.class, debugRequest.arguments.program, this.outputStream, this );
	}

	/**
	 * Visit SetBreakpointsRequest instances. Send a response.
	 * 
	 * @param debugRequest
	 */
	public void visit( SetBreakpointsRequest debugRequest ) {
		for ( Breakpoint bp : debugRequest.arguments.breakpoints ) {
			this.debugger.addBreakpoint( debugRequest.arguments.source.path, bp );
			this.breakpoints.put( bp.id, new BreakpointRequest( bp.id, bp.line, debugRequest.arguments.source.path.toLowerCase() ) );
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
	 * Visit ThreadRequest instances. Should send a ThreadResponse contianing basic information about all vm threds.
	 * 
	 * @param debugRequest
	 */
	public void visit( ThreadsRequest debugRequest ) {
		List<ortus.boxlang.debugger.types.Thread> threads = this.debugger.getAllThreadReferences()
		    .stream()
		    .filter( ( threadReference ) -> {
			    return threadReference.name().compareToIgnoreCase( "main" ) == 0;
		    } )
		    .map( ( threadReference ) -> {
			    ortus.boxlang.debugger.types.Thread t = new ortus.boxlang.debugger.types.Thread();
			    t.id = ( int ) threadReference.uniqueID();
			    t.name = threadReference.name();

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
			// TODO convert from java info to boxlang info when possible
			// TODO decide if we should filter out java stack or make it available

			/*
			 * You cannot get a stackfrme, call invoke, and then try to reuse that stackframe as the thread will resume and invalidate the stackframe
			 * we have some options
			 * 
			 * 1) do not use invoke while working with a stackframe
			 * pros - keeps the vm state nice and clean
			 * cons - tightly couples implementation, lots of work to navigate, won't be able to access stackinformation after eval either
			 * 
			 * 2) gather all the necessary information for a stack ahead of time before it is invalidated
			 * pros - makes sure we have all the data early, lets us keep invoke
			 * cons - overly eager implementation, will cause problems when vscode requests a stackframe later
			 * 
			 * 3) track stackframes our own way and when a stackframe is invalidated get its' equivalent from the vm
			 * pros - lets us do whatever we want
			 * cons - how do we match them up? a stackframe may not be findable again... what do we do when null?
			 */

			List<StackFrame> stackFrames = this.debugger.getBoxLangStackFrames( debugRequest.arguments.threadId ).stream()
			    .map( ( tuple ) -> {
				    com.sun.jdi.StackFrame stackFrame = tuple.stackFrame();
				    com.sun.jdi.Location location	= tuple.location();
				    StackFrame			sf			= new StackFrame();
				    SourceMap			map			= javaBoxpiler.getSourceMapFromFQN( location.declaringType().name() );

				    sf.id	= tuple.id();
				    sf.line	= location.lineNumber();
				    sf.column = 0;
				    sf.name	= location.method().name();

				    Integer sourceLine = map.convertJavaLineToSourceLine( sf.line );
				    if ( sourceLine != null ) {
					    sf.line = sourceLine;
				    }

				    BoxLangType blType = this.debugger.determineBoxLangType( location.declaringType() );

				    if ( blType == BoxLangType.UDF ) {
					    sf.name		= this.debugger.getObjectFromStackFrame( stackFrame )
					        .property( "name" )
					        .property( "originalValue" )
					        .asStringReference().value();
					    sf.source	= new Source();
					    sf.source.path = map.source.toString();
					    sf.source.name = sf.name + "(UDF)";
				    } else if ( blType == BoxLangType.CLOSURE ) {
					    // TODO figure out how to get the name of the closure from a parent context
					    String calledName = this.debugger.getContextForStackFrame( tuple ).invoke( "findClosestFunctionName" ).invoke( "getOriginalValue" )
					        .asStringReference().value();

					    sf.name		= calledName + "(closure)";
					    sf.source	= new Source();
					    sf.source.path = map.source.toString();
					    sf.source.name = sf.name;
				    } else if ( blType == BoxLangType.LAMBDA ) {
					    sf.name		= this.debugger.getObjectFromStackFrame( stackFrame )
					        .property( "name" )
					        .property( "originalValue" )
					        .asStringReference().value();
					    sf.source	= new Source();
					    sf.source.path = map.source.toString();
					    sf.source.name = sf.name + "(LAMBDA)";
				    } else if ( map != null && map.isTemplate() ) {
					    sf.name		= map.getFileName();
					    sf.source	= new Source();
					    sf.source.path = map.source.toString();
					    sf.source.name = sf.name;
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
			WrappedValue	context			= this.debugger.getContextForStackFrame( debugRequest.arguments.frameId );

			List<Scope>		scopes			= new ArrayList<Scope>();

			Scope			argumentScope	= scopeByName( context, "arguments" );
			if ( argumentScope != null ) {
				argumentScope.presentationHint = "arguments";
				scopes.add( argumentScope );
			}
			Scope localScope = scopeByName( context, "local" );
			if ( localScope != null ) {
				localScope.presentationHint = "locals";
				scopes.add( localScope );
			}
			Scope variablesScope = scopeByName( context, "variables" );
			if ( variablesScope != null ) {
				scopes.add( variablesScope );
			}

			new ScopeResponse( debugRequest, scopes ).send( this.outputStream );
		} catch ( Exception e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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

	// ===================================================
	// ================= EVENTS ==========================
	// ===================================================

	public void sendStoppedEventForBreakpoint( BreakpointEvent breakpointEvent ) {
		SourceMap			map			= javaBoxpiler.getSourceMapFromFQN( breakpointEvent.location().declaringType().name() );
		String				sourcePath	= map.source.toLowerCase();

		BreakpointRequest	bp			= null;

		for ( BreakpointRequest b : this.breakpoints.values() ) {
			if ( b.source.compareToIgnoreCase( sourcePath ) == 0 ) {
				bp = b;
				break;
			}
		}
		// TODO convert this file/line number to boxlang
		StoppedEvent.breakpoint( breakpointEvent, bp.id ).send( this.outputStream );
	}

	record ScopeCache( com.sun.jdi.StackFrame stackFrame, ObjectReference scope ) {
	};

	record BreakpointRequest( int id, int line, String source ) {

	}
}
