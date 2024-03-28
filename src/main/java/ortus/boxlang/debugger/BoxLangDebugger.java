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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
// import java.lang.StackWalker.StackFrame;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.StringReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.ExceptionRequest;
import com.sun.jdi.request.ThreadDeathRequest;
import com.sun.jdi.request.ThreadStartRequest;

import ortus.boxlang.compiler.IBoxpiler;
import ortus.boxlang.compiler.SourceMap;
import ortus.boxlang.compiler.SourceMap.SourceMapRecord;
import ortus.boxlang.compiler.javaboxpiler.JavaBoxpiler;
import ortus.boxlang.debugger.JDITools.WrappedValue;
import ortus.boxlang.debugger.event.ExitEvent;
import ortus.boxlang.debugger.event.OutputEvent;
import ortus.boxlang.debugger.event.StoppedEvent;
import ortus.boxlang.debugger.event.TerminatedEvent;
import ortus.boxlang.debugger.event.ThreadEvent;
import ortus.boxlang.debugger.types.Breakpoint;
import ortus.boxlang.debugger.types.Variable;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class BoxLangDebugger {

	public VirtualMachine						vm;
	private OutputStream						debugAdapterOutput;
	Map<String, List<Breakpoint>>				breakpoints;
	private List<ReferenceType>					vmClasses;
	private IBoxpiler							boxpiler;
	private Status								status;
	private DebugAdapter						debugAdapter;
	private InputStream							vmInput;
	private InputStream							vmErrorInput;
	public BreakpointEvent						bpe;
	public ThreadReference						lastThread;

	public Map<String, SourceMap>				sourceMaps			= new HashMap<String, SourceMap>();
	public static Map<String, SourceMap>		sourceMapsFromFQN	= new HashMap<String, SourceMap>();

	private ClassType							keyClassRef			= null;
	private IVMInitializationStrategy			initStrat;
	private Map<Integer, CachedThreadReference>	cachedThreads		= new HashMap<Integer, CachedThreadReference>();
	private IStepStrategy						stepStrategy;
	private ExceptionRequest					exceptionRequest;
	private boolean								any;
	private String								matcher;
	private ReferenceType						boxLangExceptionRef;
	private EventSet							lastEventSet;

	public enum Status {
		NOT_STARTED,
		INITIALIZED,
		STARTED,
		RUNNING,
		STOPPED,
		DONE
	}

	public BoxLangDebugger( IVMInitializationStrategy initStrat, OutputStream debugAdapterOutput, DebugAdapter debugAdapter ) {
		this.initStrat			= initStrat;
		this.debugAdapterOutput	= debugAdapterOutput;
		breakpoints				= new HashMap<>();
		vmClasses				= new ArrayList<>();
		this.boxpiler			= JavaBoxpiler.getInstance();
		this.status				= Status.NOT_STARTED;
		this.debugAdapter		= debugAdapter;
	}

	public void initialize() {
		try {
			this.vm = this.initStrat.initialize();

			lookForBoxLangClasses();
		} catch ( Exception e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		enableClassPrepareRequest();
		enableThreadRequests();

		if ( vm.process() != null ) {
			this.vmInput		= vm.process().getInputStream();
			this.vmErrorInput	= vm.process().getErrorStream();
		}

		this.status = Status.RUNNING;
	}

	public void keepWorking() {
		try {
			if ( this.status == Status.NOT_STARTED || this.status == Status.DONE ) {
				return;
			}

			EventSet eventSet = vm.eventQueue().remove( 300 );

			readVMInput();
			readVMErrorInput();

			if ( eventSet != null ) {
				processVMEvents( eventSet );
			}

			// if ( this.status == Status.RUNNING && eventSet != null ) {
			if ( eventSet != null && eventSet.suspendPolicy() == EventRequest.SUSPEND_ALL ) {
				eventSet.resume();
			}
		} catch ( com.sun.jdi.VMDisconnectedException e ) {
			this.status = Status.DONE;
		} catch ( Exception e ) {
			e.printStackTrace();
		} finally {
			readVMInput();
			readVMErrorInput();
		}
	}

	public void begin() {
		this.status = Status.RUNNING;
	}

	public void continueExecution( int threadId, boolean singleThread ) {
		clearCachedThreads();
		JDITools.clearMemory();
		this.bpe		= null;
		this.lastThread	= null;

		if ( this.lastEventSet != null ) {
			this.lastEventSet.resume();
		}

		if ( singleThread && this.bpe != null ) {
			this.bpe.thread().resume();
			this.bpe = null;
		} else {
			vm.resume();
		}

		// this.status = Status.RUNNING;
	}

	public void setBreakpointsForFile( String filePath, List<Breakpoint> breakpoints ) {
		this.breakpoints.put( filePath, new ArrayList<Breakpoint>() );
		this.breakpoints.get( filePath ).addAll( breakpoints );
		this.setAllBreakpoints();
	}

	public void configureExceptionBreakpoints( boolean any, String matcher ) {
		this.any		= any;
		this.matcher	= matcher;

		if ( this.vm == null ) {
			return;
		}

		if ( !any && this.matcher == null && this.exceptionRequest != null ) {
			this.exceptionRequest.disable();
			this.vm.eventRequestManager().deleteEventRequest( this.exceptionRequest );
			this.exceptionRequest = null;
			return;
		}

		this.exceptionRequest = this.vm.eventRequestManager().createExceptionRequest( getBoxLangExceptionRef(), true, true );
		// exceptionRequest.addClassFilter( "boxgenerated.*" );
		exceptionRequest.enable();
	}

	private ReferenceType getBoxLangExceptionRef() {
		if ( this.boxLangExceptionRef == null ) {
			this.boxLangExceptionRef = this.vm.allClasses()
			    .stream()
			    .filter( ( ref ) -> ref.name().toLowerCase().contains( "boxlangexception" ) )
			    .findFirst()
			    .orElse( null );
		}

		return this.boxLangExceptionRef;
	}

	private void enableThreadRequests() {
		ThreadStartRequest startRequest = vm.eventRequestManager().createThreadStartRequest();
		startRequest.setSuspendPolicy( ThreadStartRequest.SUSPEND_EVENT_THREAD );
		startRequest.enable();

		ThreadDeathRequest deathRequest = vm.eventRequestManager().createThreadDeathRequest();
		deathRequest.setSuspendPolicy( ThreadDeathRequest.SUSPEND_EVENT_THREAD );
		startRequest.enable();
	}

	private void enableClassPrepareRequest() {
		ClassPrepareRequest classPrepareRequest = vm.eventRequestManager().createClassPrepareRequest();
		classPrepareRequest.addClassFilter( "*.BoxLangException" );
		classPrepareRequest.setSuspendPolicy( ClassPrepareRequest.SUSPEND_EVENT_THREAD );
		classPrepareRequest.enable();

		classPrepareRequest = vm.eventRequestManager().createClassPrepareRequest();
		classPrepareRequest.addClassFilter( "boxgenerated.*" );
		classPrepareRequest.setSuspendPolicy( ClassPrepareRequest.SUSPEND_EVENT_THREAD );
		classPrepareRequest.enable();
	}

	public WrappedValue getObjectFromStackFrame( StackFrame stackFrame ) {
		return JDITools.wrap( this.lastThread, stackFrame.thisObject() );
	}

	public BoxLangType determineBoxLangType( ReferenceType type ) {
		return JDITools.determineBoxLangType( type );
	}

	public WrappedValue getContextForStackFrame( int id ) {
		return findVariableyName( getSeenStack( id ), "context" );
	}

	public WrappedValue getContextForStackFrame( StackFrameTuple tuple ) {
		return findVariableyName( tuple, "context" );
	}

	public CompletableFuture<WrappedValue> evaluateInContext( String expression, int frameId )
	    throws InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException, InvocationException {
		// get current stackframe of breakpoint thread
		StackFrameTuple sf = getSeenStack( frameId );
		// get the context
		WrappedValue context = findVariableyName( sf, "context" );
		// get the runtime
		WrappedValue runtime = getRuntime( sf.thread );
		// execute the expression
		return runtime.invokeAsync(
		    "executeStatement",
		    Arrays.asList( "java.lang.String", "ortus.boxlang.runtime.context.IBoxContext" ),
		    Arrays.asList(
		        this.vm.mirrorOf( expression ),
		        context.value()
		    )
		);
	}

	private WrappedValue getRuntime( ThreadReference thread ) {
		ClassType	boxRuntime	= ( ClassType ) this.vm.allClasses().stream().filter( ( refType ) -> {
									return refType.name().equalsIgnoreCase( "ortus.boxlang.runtime.BoxRuntime" );
								} ).findFirst().get();
		Method		getInstance	= JDITools.findMethodByNameAndArgs( boxRuntime, "getInstance", new ArrayList<String>() );

		try {
			Value boxRuntimeInstance = boxRuntime.invokeMethod( thread, getInstance, new ArrayList<Value>(), 0 );

			return JDITools.wrap( thread, boxRuntimeInstance );
		} catch ( InvalidTypeException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch ( ClassNotLoadedException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch ( IncompatibleThreadStateException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch ( InvocationException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public boolean hasSeen( long variableReference ) {
		return JDITools.hasSeen( variableReference );
	}

	public List<Variable> getVariablesFromSeen( long variableReference ) {
		return JDITools.getVariablesFromSeen( variableReference );
	}

	public void terminate() {
		this.initStrat.terminate( this.vm );
		new TerminatedEvent().send( this.debugAdapterOutput );
	}

	public Value mirrorOfKey( String name ) {
		ClassType	ref				= getKeyClassType();

		Method		contstructor	= ref.allMethods()
		    .stream()
		    .filter( ( method ) -> method.name().equalsIgnoreCase( "<init>" ) )
		    .findFirst()
		    .get();

		try {
			return ref.newInstance( lastThread, contstructor, Arrays.asList( this.vm.mirrorOf( name ) ), 0 );
		} catch ( InvalidTypeException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch ( ClassNotLoadedException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch ( IncompatibleThreadStateException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch ( InvocationException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;

	}

	public List<CachedThreadReference> getAllThreadReferences() {
		return this.vm.allThreads().stream().map( ( tr ) -> cacheOrGetThread( tr ) ).collect( Collectors.toList() );
	}

	public List<StackFrame> getStackFrames( int threadId ) throws IncompatibleThreadStateException {
		ThreadReference matchingThreadRef = null;

		for ( ThreadReference threadReference : this.vm.allThreads() ) {
			if ( threadReference.uniqueID() == threadId ) {
				matchingThreadRef = threadReference;
				break;
			}
		}

		if ( matchingThreadRef == null ) {
			throw ( new BoxRuntimeException( "Couldn't find thread: " + threadId ) );
		}

		return matchingThreadRef.frames();
	}

	public record StackFrameTuple( StackFrame stackFrame, Location location, int id, Map<LocalVariable, Value> values, ThreadReference thread ) {

		public String sourceFile() {
			var sourceMap = sourceMapsFromFQN.get( this.location().declaringType().name() );

			return sourceMap.getSource();
		}

		public int sourceLine() {
			SourceMap sourceMap = sourceMapsFromFQN.get( this.location().declaringType().name() );

			return sourceMap.convertJavaLineToSourceLine( this.location().lineNumber() );
		}
	}

	public StackFrameTuple getSeenStack( int stackFrameId ) {
		for ( CachedThreadReference ref : cachedThreads.values() ) {
			for ( StackFrameTuple sft : ref.getBoxLangStackFrames() ) {
				if ( sft.id == stackFrameId ) {
					return sft;
				}
			}
		}

		return null;
	}

	public void startStepping( int threadId, IStepStrategy stepStrategy ) {

		this.stepStrategy = stepStrategy;

		this.stepStrategy.startStepping( this.cacheOrGetThread( threadId ) );

		this.continueExecution( 0, true );
	}

	public Optional<Location> pauseThread( int threadId ) {
		return this.vm.allThreads()
		    .stream().filter( ( tr ) -> ( int ) tr.uniqueID() == threadId )
		    .findFirst()
		    .map( ( tr ) -> {
			    tr.suspend();
			    this.status = Status.STOPPED;
			    return tr;
		    } )
		    .map( ( tr ) -> this.cacheOrGetThread( threadId ) )
		    .map( ( ctr ) -> {
			    var				topFrame	= ctr.getBoxLangStackFrames().get( 0 );

			    BreakpointRequest bpReq		= vm.eventRequestManager().createBreakpointRequest( topFrame.location() );
			    bpReq.setSuspendPolicy( BreakpointRequest.SUSPEND_EVENT_THREAD );
			    bpReq.enable();

			    continueExecution( threadId, false );

			    return topFrame.location();
		    } );
	}

	public List<StackFrameTuple> getBoxLangStackFrames( int threadId ) throws IncompatibleThreadStateException {
		return this.cacheOrGetThread( threadId ).getBoxLangStackFrames();
	}

	private CachedThreadReference cacheOrGetThread( int threadId ) {
		return cacheOrGetThread( getThreadReference( threadId ).get() );

	}

	private CachedThreadReference cacheOrGetThread( ThreadReference thread ) {
		int threadId = ( int ) thread.uniqueID();

		if ( !this.cachedThreads.containsKey( threadId ) ) {
			CachedThreadReference ref = new CachedThreadReference( thread );

			this.cachedThreads.put( threadId, ref );
		}

		return this.cachedThreads.get( threadId );
	}

	private ClassType getKeyClassType() {
		if ( keyClassRef == null ) {
			keyClassRef = ( ClassType ) this.vm.allClasses()
			    .stream()
			    .filter( ( type ) -> type.name().equalsIgnoreCase( "ortus.boxlang.runtime.scopes.key" ) )
			    .findFirst()
			    .get();
		}

		return keyClassRef;
	}

	private void processVMEvents( EventSet eventSet ) throws IncompatibleThreadStateException, AbsentInformationException {
		for ( Event event : eventSet ) {
			System.out.println( "Found event: " + event.toString() );

			if ( event instanceof VMDeathEvent de ) {
				handleDeathEvent( eventSet, de );
			} else if ( event instanceof ClassPrepareEvent cpe ) {
				handleClassPrepareEvent( eventSet, cpe );
			} else if ( event instanceof BreakpointEvent bpe ) {
				handleBreakPointEvent( eventSet, bpe );
			} else if ( event instanceof StepEvent stepEvent ) {
				handleStepEvent( eventSet, stepEvent );
			} else if ( event instanceof ExceptionEvent exceptionEvent ) {
				handleExceptionEvent( eventSet, exceptionEvent );
			} else if ( event instanceof ThreadStartEvent threadStartEvent ) {
				handleThreadStartEvent( eventSet, threadStartEvent );
			} else if ( event instanceof ThreadDeathEvent threadDeathEvent ) {
				handleThreadDeathEvent( eventSet, threadDeathEvent );
			}
		}

	}

	private void handleThreadStartEvent( EventSet eventSet, ThreadStartEvent event ) {
		if ( isBoxlangThread( event.thread() ) ) {
			cacheOrGetThread( event.thread() );
			new ThreadEvent( "started", ( int ) event.thread().uniqueID() ).send( this.debugAdapterOutput );
		}

		eventSet.resume();
	}

	private void handleThreadDeathEvent( EventSet eventSet, ThreadDeathEvent event ) {
		if ( isBoxlangThread( event.thread() ) ) {
			new ThreadEvent( "exited", ( int ) event.thread().uniqueID() ).send( this.debugAdapterOutput );
		}

		eventSet.resume();
	}

	private boolean isBoxlangThread( ThreadReference threadRef ) {
		return threadRef.name().matches( "BL-Thread" );
	}

	private void handleExceptionEvent( EventSet eventSet, ExceptionEvent exceptionEvent ) {
		String	type		= JDITools.wrap( exceptionEvent.thread(), exceptionEvent.exception() ).invoke( "getType" ).asStringReference().value();
		String	message		= JDITools.wrap( exceptionEvent.thread(), exceptionEvent.exception() ).invoke( "getMessage" ).asStringReference().value();

		String	text		= type + ": " + message;
		String	description	= "Paused on exception";

		new StoppedEvent( "exception", ( int ) exceptionEvent.thread().uniqueID(), text, description ).send( this.debugAdapterOutput );
		this.status			= Status.STOPPED;
		this.lastThread		= exceptionEvent.thread();
		this.lastEventSet	= eventSet;
		clearCachedThreads();
	}

	private void trackThreadEvent( ThreadReference thread, EventSet eventSet ) {

	}

	private void handleStepEvent( EventSet eventSet, StepEvent stepEvent ) {
		if ( this.stepStrategy == null ) {
			eventSet.resume();
			this.vm.eventRequestManager().stepRequests().forEach( ( sr ) -> {
				sr.disable();
			} );

			this.vm.eventRequestManager().deleteEventRequests( this.vm.eventRequestManager().stepRequests() );

			return;
		}

		this.stepStrategy.checkStepEvent( new CachedThreadReference( stepEvent.thread() ) )
		    .ifPresentOrElse( ( sft ) -> {
			    new StoppedEvent( "step", ( int ) stepEvent.thread().uniqueID() ).send( this.debugAdapterOutput );

			    this.status		= Status.STOPPED;
			    this.lastThread	= stepEvent.thread();
			    this.lastEventSet = eventSet;
			    clearCachedThreads();

			    this.stepStrategy.dispose();
			    this.stepStrategy = null;
		    }, () -> eventSet.resume() );
	}

	private void lookForBoxLangClasses() {
		this.vm.allClasses().stream()
		    .filter( ( refType ) -> refType.name().toLowerCase().contains( "boxgenerated" ) )
		    .forEach( ( refType ) -> {
			    vmClasses.add( refType );
			    SourceMap map = boxpiler.getSourceMapFromFQN( refType.name() );
			    if ( map == null ) {
				    return;
			    }
			    this.sourceMaps.put( map.source.toLowerCase(), map );
			    this.sourceMapsFromFQN.put( refType.name(), map );
		    } );
	}

	private void handleClassPrepareEvent( EventSet eventSet, ClassPrepareEvent event ) {

		if ( event.referenceType().name().contains( "BoxLangException" ) ) {
			this.boxLangExceptionRef = event.referenceType();
			this.configureExceptionBreakpoints( this.any, this.matcher );
		}

		vmClasses.add( event.referenceType() );
		SourceMap map = boxpiler.getSourceMapFromFQN( event.referenceType().name() );
		if ( map == null || map.source == null ) {
			eventSet.resume();
			return;
		}

		this.sourceMaps.put( map.source.toLowerCase(), map );
		this.sourceMapsFromFQN.put( event.referenceType().name(), map );

		setAllBreakpoints();

		eventSet.resume();
	}

	private void handleDeathEvent( EventSet eventSet, VMDeathEvent de ) {

		this.status = Status.DONE;

		this.vm.process().onExit().join();
		new ExitEvent( this.vm.process().exitValue() ).send( this.debugAdapterOutput );
		new TerminatedEvent().send( this.debugAdapterOutput );
		eventSet.resume();
	}

	private void readVMInput() {
		if ( this.vmInput == null ) {
			return;
		}

		try {
			int available = vmInput.available();

			if ( available > 0 ) {
				byte[] bytes = ByteBuffer.allocate( available ).array();
				vmInput.read( bytes );

				new OutputEvent( "stdout", new String( bytes ) ).send( this.debugAdapterOutput );
			}
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Optional<ThreadReference> getThreadReference( int threadId ) {
		return this.vm.allThreads().stream().filter( ( ref ) -> ref.uniqueID() == threadId ).findFirst();
	}

	public WrappedValue findVariableyName( StackFrameTuple tuple, String name ) {
		Map<LocalVariable, Value> visibleVariables = tuple.values;

		for ( Map.Entry<LocalVariable, Value> entry : visibleVariables.entrySet() ) {
			if ( entry.getKey().name().equalsIgnoreCase( name ) ) {
				return JDITools.wrap( tuple.thread, entry.getValue() );
			}
		}

		return null;

	}

	public WrappedValue upateVariableByReference( int variableReference, Value key, StringReference value ) {
		WrappedValue container = JDITools.getSeen( variableReference );

		if ( container == null ) {
			return null;
		}

		// todo get context
		container.invokeByNameAndArgs(
		    "assign",
		    Arrays.asList( "ortus.boxlang.runtime.context.IBoxContext", "ortus.boxlang.runtime.scopes.Key", "java.lang.Object" ),
		    Arrays.asList( getContextForStackFrame( this.cacheOrGetThread( this.lastThread ).getBoxLangStackFrames().get( 0 ) ).value(), key, value )
		);

		return null;
	}

	public void handleDisconnect() {
		this.initStrat.disconnect( this.vm );
		this.vm		= null;
		this.status	= Status.DONE;

		new ExitEvent( 0 ).send( this.debugAdapterOutput );
	}

	private void readVMErrorInput() {
		if ( this.vmErrorInput == null ) {
			return;
		}

		try {
			int errorAvailable = vmErrorInput.available();
			if ( errorAvailable > 0 ) {
				byte[] bytes = ByteBuffer.allocate( errorAvailable ).array();
				vmErrorInput.read( bytes );

				new OutputEvent( "stderr", new String( bytes ) ).send( this.debugAdapterOutput );
			}
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void handleBreakPointEvent( EventSet eventSet, BreakpointEvent bpe ) throws IncompatibleThreadStateException, AbsentInformationException {
		this.status = Status.STOPPED;
		this.debugAdapter.sendStoppedEventForBreakpoint( bpe );
		clearCachedThreads();
		this.bpe = bpe;
		this.cacheOrGetThread( ( int ) this.bpe.thread().uniqueID() );
		this.lastThread		= this.bpe.thread();
		this.lastEventSet	= eventSet;
	}

	private void clearCachedThreads() {
		this.cachedThreads = new HashMap<Integer, CachedThreadReference>();
	}

	private void setAllBreakpoints() {
		if ( this.vm == null ) {
			return;
		}

		this.vm.eventRequestManager().deleteAllBreakpoints();

		for ( String fileName : this.breakpoints.keySet() ) {

			for ( Breakpoint breakpoint : this.breakpoints.get( fileName ) ) {
				List<ReferenceType> matchingTypes = getMatchingReferenceTypes( fileName );

				if ( matchingTypes.size() == 0 ) {
					continue;
				}

				for ( ReferenceType vmClass : matchingTypes ) {
					try {
						SourceMapRecord	foundMapRecord	= boxpiler.getSourceMapFromFQN( vmClass.name() ).findClosestSourceMapRecord( breakpoint.line );
						String			sourceName		= normalizeName( foundMapRecord.javaSourceClassName );

						if ( !sourceName.equals( normalizeName( vmClass.name() ) ) ) {
							continue;
						}

						Location foundLoc = null;
						for ( Location loc : vmClass.allLineLocations() ) {
							if ( loc.lineNumber() >= foundMapRecord.javaSourceLineStart && loc.lineNumber() <= foundMapRecord.javaSourceLineEnd ) {
								foundLoc = loc;
								break;
							}
						}

						if ( foundLoc == null ) {
							break;
						}

						BreakpointRequest bpReq = vm.eventRequestManager().createBreakpointRequest( foundLoc );
						bpReq.setSuspendPolicy( BreakpointRequest.SUSPEND_EVENT_THREAD );
						bpReq.enable();
						break;

					} catch ( BoxRuntimeException e ) {
						e.printStackTrace();
					} catch ( AbsentInformationException e ) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	private List<ReferenceType> getMatchingReferenceTypes( String fileName ) {
		String lCaseFileName = fileName.toLowerCase();

		if ( !this.sourceMaps.containsKey( lCaseFileName ) ) {
			return new ArrayList<ReferenceType>();
		}

		SourceMap	map				= this.sourceMaps.get( lCaseFileName );

		Set<String>	referenceTypes	= new HashSet<String>();

		for ( SourceMapRecord record : map.sourceMapRecords ) {
			referenceTypes.add( normalizeName( record.javaSourceClassName ) );
		}

		return vmClasses.stream().filter( ( rt ) -> referenceTypes.contains( normalizeName( rt.name() ) ) ).collect( Collectors.toList() );
	}

	private String normalizeName( String className ) {
		return className.replaceAll( "\\W", "" ).toLowerCase();
	}
}
