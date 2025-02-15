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
import java.nio.file.Path;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

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
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.ThreadDeathEvent;
import com.sun.jdi.event.ThreadStartEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.ExceptionRequest;
import com.sun.jdi.request.MethodExitRequest;
import com.sun.jdi.request.ThreadDeathRequest;
import com.sun.jdi.request.ThreadStartRequest;

import ortus.boxlang.compiler.SourceMap;
import ortus.boxlang.compiler.SourceMap.SourceMapRecord;
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
import ortus.boxlang.runtime.types.util.JSONUtil;

public class BoxLangDebugger {

	public VirtualMachine								vm;
	private OutputStream								debugAdapterOutput;
	Map<String, List<Breakpoint>>						breakpoints;
	private List<ReferenceType>							vmClasses;
	private Status										status;
	private DebugAdapter								debugAdapter;
	private InputStream									vmInput;
	private InputStream									vmErrorInput;

	public Map<String, SourceMap>						sourceMaps			= new HashMap<String, SourceMap>();
	public static Map<String, SourceMap>				sourceMapsFromFQN	= new HashMap<String, SourceMap>();

	private ClassType									keyClassRef			= null;
	private IVMInitializationStrategy					initStrat;
	private Map<Integer, CachedThreadReference>			cachedThreads		= new HashMap<Integer, CachedThreadReference>();
	private Map<Integer, EventSet>						eventSets			= new HashMap<Integer, EventSet>();
	private IStepStrategy								stepStrategy;
	private ExceptionRequest							exceptionRequest;
	private boolean										any;
	private String										matcher;
	private ReferenceType								boxLangExceptionRef;

	private int											SUSPEND_POLICY		= ThreadStartRequest.SUSPEND_EVENT_THREAD;
	private MethodExitRequest							methodExitRequest	= null;

	private static final Pattern						NON_WORD_PATTERN	= Pattern.compile( "\\W" );

	private boolean										vmUsesJavaBoxpiler	= false;
	private Map<String, Map<Integer, List<Location>>>	locations			= new HashMap<String, Map<Integer, List<Location>>>();

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
		this.status				= Status.NOT_STARTED;
		this.debugAdapter		= debugAdapter;
	}

	public boolean isJavaBoxpiler() {
		return vmUsesJavaBoxpiler;
	}

	public void initialize() {
		try {
			this.vm = this.initStrat.initialize();
			this.forceBoxLangClassSearch();
		} catch ( Exception e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		enableClassPrepareRequest();
		enableThreadRequests();
		setAllBreakpoints();

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
		continueExecution( threadId, singleThread, false );
	}

	public void continueExecution( int threadId, boolean singleThread, boolean force ) {
		clearCachedThreads();
		JDITools.clearMemory();

		if ( force && this.stepStrategy != null ) {
			this.stepStrategy.dispose();
		}

		if ( !singleThread ) {
			vm.resume();
			return;
		}

		boolean wasResumed = resumeThreadEventSet( threadId );

		if ( !wasResumed ) {
			vm.resume();
		}

		this.status = Status.RUNNING;
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
		startRequest.setSuspendPolicy( SUSPEND_POLICY );
		startRequest.enable();

		ThreadDeathRequest deathRequest = vm.eventRequestManager().createThreadDeathRequest();
		deathRequest.setSuspendPolicy( SUSPEND_POLICY );
		startRequest.enable();
	}

	private void enableClassPrepareRequest() {
		ClassPrepareRequest classPrepareRequest = vm.eventRequestManager().createClassPrepareRequest();
		classPrepareRequest.addClassFilter( "*.BoxLangException" );
		classPrepareRequest.setSuspendPolicy( SUSPEND_POLICY );
		classPrepareRequest.enable();

		classPrepareRequest = vm.eventRequestManager().createClassPrepareRequest();
		classPrepareRequest.addClassFilter( "boxgenerated.*" );
		classPrepareRequest.setSuspendPolicy( SUSPEND_POLICY );
		classPrepareRequest.enable();
	}

	public WrappedValue getObjectFromStackFrame( StackFrame stackFrame ) {
		return JDITools.wrap( stackFrame.thread(), stackFrame.thisObject() );
	}

	public BoxLangType determineBoxLangType( ReferenceType type ) {
		return JDITools.determineBoxLangType( type );
	}

	public WrappedValue getContextForStackFrame( int id ) {
		return findNearestContext( getSeenStack( id ) );
	}

	public WrappedValue getContextForStackFrame( StackFrameTuple tuple ) {
		return findNearestContext( tuple );
	}

	public CompletableFuture<List<WrappedValue>> getVisibleScopes( int frameId ) {
		WrappedValue context = getContextForStackFrame( frameId );

		if ( context == null ) {
			return CompletableFuture.completedFuture( new ArrayList<WrappedValue>() );
		}

		return context.invokeAsync(
		    "getVisibleScopes",
		    new ArrayList<String>(),
		    new ArrayList<com.sun.jdi.Value>()
		).thenApply( ( visibleScopes ) -> {

			if ( visibleScopes == null ) {
				return null;
			}

			return visibleScopes.invokeByNameAndArgs( "get", Arrays.asList( "java.lang.String" ),
			    Arrays.asList( this.vm.mirrorOf( "contextual" ) ) );
		} )
		    .thenApplyAsync( ( contextualScopes ) -> {
			    if ( contextualScopes == null ) {
				    return new ArrayList<WrappedValue>();
			    }

			    if ( this.exceptionRequest != null ) {
				    this.exceptionRequest.disable();
			    }

			    List<WrappedValue> result = ( List<WrappedValue> ) contextualScopes.invoke( "values" )
			        .invoke( "toArray" )
			        .asArrayReference()
			        .getValues()
			        .stream()
			        .map( wv -> ( JDITools.wrap( context.thread(), wv ) ) )
			        .collect( Collectors.toList() );

			    if ( this.exceptionRequest != null ) {
				    this.exceptionRequest.enable();
			    }

			    return result;
		    } );
	}

	public CompletableFuture<WrappedValue> evaluateInContext( String expression, int frameId ) {
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

	public void disconnectFromVM() {
		this.vm.eventRequestManager().deleteAllBreakpoints();
		this.breakpoints.clear();
		this.status = Status.DONE;
		this.vm.dispose();
		this.vm								= null;
		this.sourceMaps						= new HashMap<String, SourceMap>();
		BoxLangDebugger.sourceMapsFromFQN	= new HashMap<String, SourceMap>();
		this.cachedThreads					= new HashMap<Integer, CachedThreadReference>();
		this.eventSets						= new HashMap<Integer, EventSet>();
		this.locations						= new HashMap<String, Map<Integer, List<Location>>>();
	}

	public boolean hasSeen( long variableReference ) {
		return JDITools.hasSeen( variableReference );
	}

	public List<Variable> getVariablesFromSeen( long variableReference ) {
		return JDITools.getVariablesFromSeen( variableReference );
	}

	public void terminate() {
		this.initStrat.terminate( this );
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
			return ref.newInstance( getPausedThreadReference(), contstructor, Arrays.asList( this.vm.mirrorOf( name ) ), 0 );
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

	public record StackFrameTuple( BoxLangDebugger debugger, StackFrame stackFrame, Location location, int id, Map<LocalVariable, Value> values,
	    ThreadReference thread ) {

		private static Pattern isTemplate;

		static {
			isTemplate = Pattern.compile( "(.cfs|.cfm|.bx|.bxs)$" );
		}

		public String sourceFile() {
			if ( debugger.vmUsesJavaBoxpiler ) {
				return sourceMapsFromFQN.get( this.location().declaringType().name() ).getSource();
			}

			try {
				return location.sourceName();
			} catch ( AbsentInformationException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return "";
		}

		public int sourceLine() {
			if ( debugger.vmUsesJavaBoxpiler ) {

				SourceMap sourceMap = sourceMapsFromFQN.get( this.location().declaringType().name() );

				return sourceMap.convertJavaLineToSourceLine( this.location().lineNumber() );
			}

			return location.lineNumber();
		}

		public String getFileName() {
			return Path.of( sourceFile() ).getFileName().toString();
		}

		public boolean isTemplate() {
			return isTemplate.matcher( sourceFile() ).find();
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

		if ( this.stepStrategy != null ) {
			this.stepStrategy.dispose();
		}

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
			    bpReq.setSuspendPolicy( SUSPEND_POLICY );
			    bpReq.enable();

			    continueExecution( threadId, false );

			    return topFrame.location();
		    } );
	}

	public List<StackFrameTuple> getBoxLangStackFrames( int threadId ) {
		return this.cacheOrGetThread( threadId ).getBoxLangStackFrames();
	}

	private CachedThreadReference cacheOrGetThread( int threadId ) {
		return cacheOrGetThread( getThreadReference( threadId ).get() );

	}

	private CachedThreadReference cacheOrGetThread( ThreadReference thread ) {
		int threadId = ( int ) thread.uniqueID();

		if ( !this.cachedThreads.containsKey( threadId ) ) {
			CachedThreadReference ref = new CachedThreadReference( this, thread );

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
			// this if statement is only used to catch the event initiated by forceBoxLangClassSearch
			if ( event instanceof MethodExitEvent mee ) {
				this.methodExitRequest.disable();
				lookForBoxLangClasses( mee.thread() );
				setAllBreakpoints();

				eventSet.resume();
			} else if ( event instanceof VMDeathEvent de ) {
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
		return StringUtils.containsIgnoreCase( threadRef.name(), "BL-Thread" );
	}

	private void handleExceptionEvent( EventSet eventSet, ExceptionEvent exceptionEvent ) {
		String	type		= JDITools.wrap( exceptionEvent.thread(), exceptionEvent.exception() ).invoke( "getType" ).asStringReference().value();
		String	message		= JDITools.wrap( exceptionEvent.thread(), exceptionEvent.exception() ).invoke( "getMessage" ).asStringReference().value();

		String	text		= type + ": " + message;
		String	description	= "Paused on exception";

		new StoppedEvent( "exception", ( int ) exceptionEvent.thread().uniqueID(), text, description ).send( this.debugAdapterOutput );
		this.status = Status.STOPPED;

		trackThreadEvent( exceptionEvent.thread(), eventSet );
		clearCachedThreads();
	}

	private void trackThreadEvent( ThreadReference thread, EventSet eventSet ) {
		if ( this.eventSets.containsKey( thread.uniqueID() ) ) {
			this.eventSets.get( thread.uniqueID() ).resume();
		}

		this.eventSets.put( ( int ) thread.uniqueID(), eventSet );
	}

	private void resumeThreadEventSet( ThreadReference thread ) {
		resumeThreadEventSet( ( int ) thread.uniqueID() );
	}

	private boolean resumeThreadEventSet( int threadId ) {
		if ( !this.eventSets.containsKey( threadId ) ) {
			return false;
		}

		this.eventSets.remove( threadId ).resume();
		return true;
	}

	private void handleStepEvent( EventSet eventSet, StepEvent stepEvent ) {
		if ( this.stepStrategy == null ) {
			eventSet.resume();
			resumeThreadEventSet( stepEvent.thread() );
			this.vm.eventRequestManager().stepRequests().forEach( ( sr ) -> {
				sr.disable();
			} );

			this.vm.eventRequestManager().deleteEventRequests( this.vm.eventRequestManager().stepRequests() );

			return;
		}

		this.stepStrategy.checkStepEvent( new CachedThreadReference( this, stepEvent.thread() ) )
		    .ifPresentOrElse( ( sft ) -> {
			    new StoppedEvent( "step", ( int ) stepEvent.thread().uniqueID() ).send( this.debugAdapterOutput );

			    this.status = Status.STOPPED;
			    trackThreadEvent( stepEvent.thread(), eventSet );
			    clearCachedThreads();

			    this.stepStrategy.dispose();
			    this.stepStrategy = null;
		    }, () -> eventSet.resume() );
	}

	private void forceBoxLangClassSearch() {
		this.methodExitRequest = vm.eventRequestManager().createMethodExitRequest();
		methodExitRequest.enable();
	}

	private void lookForBoxLangClasses( ThreadReference threadRef ) {
		this.vm.allClasses().stream()
		    .filter( ( refType ) -> refType.name().toLowerCase().contains( "boxgenerated" ) )
		    .forEach( ( refType ) -> {
			    vmClasses.add( refType );
			    findSourceMapByFQN( threadRef, refType.name() );

			    try {
				    for ( var loc : refType.allLineLocations() ) {
					    trackLocation( loc );
				    }
			    } catch ( AbsentInformationException e ) {
				    // TODO Auto-generated catch block
				    e.printStackTrace();
			    }
		    } );
	}

	private void clearOldLocations( ReferenceType rt ) {
		try {
			String lcased = rt.sourceName().toLowerCase();

			if ( !locations.containsKey( lcased ) ) {
				locations.put( lcased, new HashMap<Integer, List<Location>>() );
			}

			var map = locations.get( lcased );

			for ( List<Location> locs : map.values() ) {
				List<Location> toRemove = new ArrayList<Location>();

				for ( Location loc : locs ) {
					if ( !loc.sourceName().equalsIgnoreCase( rt.sourceName() ) ) {
						continue;
					}

					if ( !loc.declaringType().name().equalsIgnoreCase( rt.name() ) ) {
						continue;
					}

					if ( rt.classLoader().uniqueID() == loc.declaringType().classLoader().uniqueID() ) {
						continue;
					}

					toRemove.add( loc );
				}
				locs.removeAll( toRemove );
			}

		} catch ( AbsentInformationException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void trackLocation( Location loc ) throws AbsentInformationException {
		String lcased = loc.sourceName().toLowerCase();

		if ( !locations.containsKey( lcased ) ) {
			locations.put( lcased, new HashMap<Integer, List<Location>>() );
		}

		var map = locations.get( lcased );

		if ( !map.containsKey( loc.lineNumber() ) ) {
			map.put( loc.lineNumber(), new ArrayList<Location>() );
		}

		map.get( loc.lineNumber() ).add( loc );
	}

	private void handleClassPrepareEvent( EventSet eventSet, ClassPrepareEvent event ) {

		if ( event.referenceType().name().contains( "BoxLangException" ) ) {
			this.boxLangExceptionRef = event.referenceType();
			this.configureExceptionBreakpoints( this.any, this.matcher );
		}

		if ( event.referenceType().name().contains( "boxgenerated" ) ) {
			try {
				clearOldLocations( event.referenceType() );
				for ( var loc : event.referenceType().allLineLocations() ) {
					trackLocation( loc );
				}
			} catch ( AbsentInformationException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		vmClasses.add( event.referenceType() );
		findSourceMapByFQN( event.thread(), event.referenceType().name() );

		setAllBreakpoints();

		eventSet.resume();
	}

	public SourceMap findSourceMapByFQN( ThreadReference thread, String fqn ) {

		if ( BoxLangDebugger.sourceMapsFromFQN.containsKey( fqn ) ) {
			return BoxLangDebugger.sourceMapsFromFQN.get( fqn );
		}

		Optional<SourceMap> val = getSourceMapFromVM( thread, fqn );

		val.ifPresent( ( map ) -> {
			this.sourceMaps.put( map.source.toLowerCase(), map );
			BoxLangDebugger.sourceMapsFromFQN.put( fqn, map );
		} );

		return val.orElse( null );
	}

	private Optional<SourceMap> getSourceMapFromVM( ThreadReference thread, String fqn ) {
		ClassType boxRuntime = ( ClassType ) this.vm.allClasses().stream().filter( ( refType ) -> {
			return refType.name().equalsIgnoreCase( "ortus.boxlang.compiler.javaboxpiler.JavaBoxpiler" );
		} ).findFirst().orElse( null );

		if ( boxRuntime == null ) {
			return Optional.empty();
		}
		Method getInstance = JDITools.findMethodByNameAndArgs( boxRuntime, "getInstance", new ArrayList<String>() );

		try {
			Value			boxpilerInstance	= boxRuntime.invokeMethod( thread, getInstance, new ArrayList<Value>(), ClassType.INVOKE_SINGLE_THREADED );

			WrappedValue	wrappedBoxpiler		= JDITools.wrap( thread, boxpilerInstance );

			WrappedValue	wrappedSourceMap	= wrappedBoxpiler.invokeAsync( "getSourceMapFromFQN", List.of( "java.lang.String" ), Arrays.asList(
			    this.vm.mirrorOf( fqn.replaceFirst( "(\\$\\w+)\\$.+$", "$1" ) )
			) ).get();

			if ( wrappedSourceMap.value() == null ) {
				return Optional.empty();
			}

			SourceMap map = JSONUtil.fromJSON( SourceMap.class, wrappedSourceMap.invoke( "toJSON" ).asStringReference().value() );

			return Optional.of( map );
		} catch ( Exception e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return Optional.empty();
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

	public WrappedValue findNearestContext( StackFrameTuple tuple ) {
		Map<LocalVariable, Value> visibleVariables = tuple.values;

		for ( Map.Entry<LocalVariable, Value> entry : visibleVariables.entrySet() ) {
			var	val		= entry.getValue();
			var	type	= val.type();

			if ( ! ( type instanceof ClassType ) ) {
				continue;
			}

			var isBoxContext = ( ( ClassType ) type ).allInterfaces()
			    .stream()
			    .anyMatch( iname -> iname.name().equalsIgnoreCase( "ortus.boxlang.runtime.context.IBoxContext" ) );

			if ( isBoxContext ) {
				return JDITools.wrap( tuple.thread, entry.getValue() );
			}
		}

		return null;

	}

	public String getStackFrameName( StackFrameTuple tuple ) {
		BoxLangType blType = determineBoxLangType( tuple.location().declaringType() );

		if ( blType == BoxLangType.UDF ) {
			return getObjectFromStackFrame( tuple.stackFrame )
			    .property( "name" )
			    .property( "originalValue" )
			    .asStringReference().value();
		} else if ( blType == BoxLangType.CLOSURE ) {
			String calledName = getContextForStackFrame( tuple )
			    .invoke( "findClosestFunctionName" )
			    .invoke( "getOriginalValue" )
			    .asStringReference()
			    .value();

			return calledName + " (closure)";
		} else if ( blType == BoxLangType.LAMBDA ) {
			String calledName = getContextForStackFrame( tuple )
			    .invoke( "findClosestFunctionName" )
			    .invoke( "getOriginalValue" )
			    .asStringReference()
			    .value();

			return calledName + " (lambda)";
		} else if ( tuple.isTemplate() ) {
			return tuple.getFileName();
		}

		return tuple.location().method().name();
	}

	public WrappedValue upateVariableByReference( int variableReference, String key, String value ) {
		WrappedValue container = JDITools.getSeen( variableReference );

		if ( container == null ) {
			return null;
		}

		// todo get context
		container.invokeByNameAndArgs(
		    "assign",
		    Arrays.asList( "ortus.boxlang.runtime.context.IBoxContext", "ortus.boxlang.runtime.scopes.Key", "java.lang.Object" ),
		    Arrays.asList( getContextForStackFrame( this.cacheOrGetThread( getPausedThreadReference() ).getBoxLangStackFrames().get( 0 ) ).value(),
		        mirrorOfKey( key ), vm.mirrorOf( value ) )
		);

		return null;
	}

	public void runStrategyToDisconnect() {
		this.initStrat.disconnect( this );
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
		String	sourcePath	= bpe.location().sourceName();
		int		lineNumber	= bpe.location().lineNumber();

		if ( vmUsesJavaBoxpiler ) {
			SourceMap map = findSourceMapByFQN( bpe.thread(), bpe.location().declaringType().name() );
			sourcePath	= map.source.toLowerCase();
			lineNumber	= -1;
		}

		this.status = Status.STOPPED;
		this.debugAdapter.sendStoppedEventForBreakpoint( ( int ) bpe.thread().uniqueID(), sourcePath, lineNumber );
		clearCachedThreads();
		this.cacheOrGetThread( ( int ) bpe.thread().uniqueID() );
		trackThreadEvent( bpe.thread(), eventSet );
	}

	private void clearCachedThreads() {
		this.cachedThreads = new HashMap<Integer, CachedThreadReference>();
	}

	public String getInternalExceptionMessage( InvocationException e ) {
		try {
			return JDITools.wrap( e.exception().owningThread(), e.exception() ).invoke( "getMessage" ).asStringReference().value();
		} catch ( IncompatibleThreadStateException e1 ) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		return null;
	}

	private ThreadReference getPausedThreadReference() {
		return this.vm.allThreads().stream().filter( ( tr ) -> tr.isSuspended() ).findFirst().get();
	}

	private void setAllBreakpoints() {
		if ( this.vm == null ) {
			return;
		}

		this.vm.eventRequestManager().deleteAllBreakpoints();

		for ( String fileName : this.breakpoints.keySet() ) {

			for ( Breakpoint breakpoint : this.breakpoints.get( fileName ) ) {

				List<Location> locations = findLocation( fileName, breakpoint );

				locations.stream().limit( 1 ).forEach( location -> {
					BreakpointRequest bpReq = vm.eventRequestManager().createBreakpointRequest( location );
					bpReq.setSuspendPolicy( SUSPEND_POLICY );
					bpReq.enable();
				} );
			}
		}
	}

	private List<Location> findLocation( String fileName, Breakpoint breakpoint ) {

		if ( vmUsesJavaBoxpiler ) {
			List<ReferenceType> matchingTypes = getMatchingReferenceTypes( fileName );

			return matchingTypes.stream().map( referenceType -> findLocationUsingJavaBoxpiler( referenceType, breakpoint ) ).toList();
		}

		String lcased = fileName.toLowerCase();

		if ( !locations.containsKey( lcased ) ) {
			return new ArrayList<Location>();
		}

		var map = locations.get( lcased );

		if ( !map.containsKey( breakpoint.line ) ) {
			return new ArrayList<Location>();
		}

		return map.get( breakpoint.line );
	}

	private Location findLocationUsingJavaBoxpiler( ReferenceType vmClass, Breakpoint breakpoint ) {
		SourceMap sourceMap = findSourceMapByFQN( this.vm.allThreads().getFirst(), vmClass.name() );

		if ( sourceMap == null ) {
			return null;
		}

		SourceMapRecord foundMapRecord = sourceMap.findClosestSourceMapRecord( breakpoint.line );

		if ( foundMapRecord == null ) {
			return null;
		}

		String sourceName = normalizeName( foundMapRecord.javaSourceClassName );

		if ( !sourceName.equals( normalizeName( vmClass.name() ) ) ) {
			return null;
		}

		try {
			for ( Location loc : vmClass.allLineLocations() ) {
				if ( loc.lineNumber() >= foundMapRecord.javaSourceLineStart && loc.lineNumber() <= foundMapRecord.javaSourceLineEnd ) {
					return loc;
				}
			}
		} catch ( AbsentInformationException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		return null;
	}

	private List<ReferenceType> getMatchingReferenceTypes( String fileName ) {
		String lCaseFileName = fileName.toLowerCase();
		if ( vmUsesJavaBoxpiler ) {

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

		String	normalizedFileName	= normalizeName( fileName );

		var		x					= vmClasses.stream().filter( rt -> normalizeName( rt.name() ).contains( "appbxs" ) ).collect( Collectors.toList() );
		;

		return vmClasses.stream().filter( rt -> normalizeName( rt.name() ).equals( normalizedFileName ) ).collect( Collectors.toList() );
	}

	private String normalizeName( String className ) {
		return NON_WORD_PATTERN.matcher( className ).replaceAll( "" ).toLowerCase();
	}
}
