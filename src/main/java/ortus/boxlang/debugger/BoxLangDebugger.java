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
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;

import ortus.boxlang.debugger.JDITools.WrappedValue;
import ortus.boxlang.debugger.event.ExitEvent;
import ortus.boxlang.debugger.event.OutputEvent;
import ortus.boxlang.debugger.event.StoppedEvent;
import ortus.boxlang.debugger.event.TerminatedEvent;
import ortus.boxlang.debugger.types.Breakpoint;
import ortus.boxlang.debugger.types.Variable;
import ortus.boxlang.runtime.runnables.compiler.JavaBoxpiler;
import ortus.boxlang.runtime.runnables.compiler.SourceMap;
import ortus.boxlang.runtime.runnables.compiler.SourceMap.SourceMapRecord;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class BoxLangDebugger {

	public VirtualMachine						vm;
	private OutputStream						debugAdapterOutput;
	Map<String, List<Breakpoint>>				breakpoints;
	private List<ReferenceType>					vmClasses;
	private JavaBoxpiler						javaBoxpiler;
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
	private NextStepStrategy					stepStrategy;

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
		this.javaBoxpiler		= JavaBoxpiler.getInstance();
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
		enableClassPrepareRequest( vm );

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

			EventSet eventSet = null;
			while ( ( eventSet = vm.eventQueue().remove( 300 ) ) != null ) {

				readVMInput();
				readVMErrorInput();

				processVMEvents( eventSet );

				if ( this.status == Status.DONE ) {
					break;
				}
			}

			if ( this.status == Status.RUNNING ) {
				vm.resume();
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
		this.cachedThreads = new HashMap<Integer, CachedThreadReference>();
		JDITools.clearMemory();
		this.bpe		= null;
		this.lastThread	= null;

		if ( singleThread && this.bpe != null ) {
			this.bpe.thread().resume();
			this.bpe = null;
		} else {
			vm.resume();
		}

		this.status = Status.RUNNING;
	}

	public void forceResume() {
		this.status = Status.RUNNING;
		this.bpe.thread().resume();
	}

	public void startDebugSession() {
		// pass
	}

	public void setBreakpointsForFile( String filePath, List<Breakpoint> breakpoints ) {
		this.breakpoints.put( filePath, new ArrayList<Breakpoint>() );
		this.breakpoints.get( filePath ).addAll( breakpoints );
		setAllBreakpoints();
	}

	public void enableClassPrepareRequest( VirtualMachine vm ) {
		ClassPrepareRequest classPrepareRequest = vm.eventRequestManager().createClassPrepareRequest();
		classPrepareRequest.addClassFilter( "boxgenerated.*" );
		classPrepareRequest.enable();
	}

	public WrappedValue getObjectFromStackFrame( StackFrame stackFrame ) {
		return JDITools.wrap( this.bpe.thread(), stackFrame.thisObject() );
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

	public WrappedValue evaluateInContext( String expression, int frameId )
	    throws InvalidTypeException, ClassNotLoadedException, IncompatibleThreadStateException, InvocationException {
		// get current stackframe of breakpoint thread
		StackFrameTuple	sf		= getSeenStack( frameId );
		// get the context
		WrappedValue	context	= findVariableyName( sf, "context" );
		// get the runtime
		WrappedValue	runtime	= getRuntime();
		// execute the expression

		return runtime.invokeByNameAndArgsWithError(
		    "executeStatement",
		    Arrays.asList( "java.lang.String", "ortus.boxlang.runtime.context.IBoxContext" ),
		    Arrays.asList(
		        this.vm.mirrorOf( expression ),
		        context.value()
		    )
		);
	}

	private WrappedValue getRuntime() {
		ClassType	boxRuntime	= ( ClassType ) this.vm.allClasses().stream().filter( ( refType ) -> {
									return refType.name().equalsIgnoreCase( "ortus.boxlang.runtime.BoxRuntime" );
								} ).findFirst().get();
		Method		getInstance	= JDITools.findMethodByNameAndArgs( boxRuntime, "getInstance", new ArrayList<String>() );

		try {
			Value boxRuntimeInstance = boxRuntime.invokeMethod( this.bpe.thread(), getInstance, new ArrayList<Value>(), 0 );

			return JDITools.wrap( this.bpe.thread(), boxRuntimeInstance );
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

	public Value mirrorOfKey( String name ) {
		ClassType	ref				= getKeyClassType();

		Method		contstructor	= ref.allMethods()
		    .stream()
		    .filter( ( method ) -> method.name().compareToIgnoreCase( "<init>" ) == 0 )
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

	public List<ThreadReference> getAllThreadReferences() {
		return this.vm.allThreads();
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

	public void startStepping( int threadId ) {

		this.stepStrategy = new NextStepStrategy();

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
			    .filter( ( type ) -> type.name().compareToIgnoreCase( "ortus.boxlang.runtime.scopes.key" ) == 0 )
			    .findFirst()
			    .get();
		}

		return keyClassRef;
	}

	private void processVMEvents( EventSet eventSet ) throws IncompatibleThreadStateException, AbsentInformationException {
		for ( Event event : eventSet ) {
			System.out.println( "Found event: " + event.toString() );

			if ( event instanceof VMDeathEvent de ) {
				// this.status = Status.DONE;
				handleDeathEvent( de );
			}
			if ( event instanceof ClassPrepareEvent cpe ) {

				handleClassPrepareEvent( cpe );
				setAllBreakpoints();
			}
			if ( event instanceof BreakpointEvent bpe ) {
				handleBreakPointEvent( bpe );
			}
			if ( event instanceof StepEvent stepEvent ) {
				if ( this.stepStrategy == null ) {
					this.vm.eventRequestManager().stepRequests().forEach( ( sr ) -> {
						sr.disable();
					} );

					this.vm.eventRequestManager().deleteEventRequests( this.vm.eventRequestManager().stepRequests() );

					continue;
				}

				this.stepStrategy.checkStepEvent( new CachedThreadReference( stepEvent.thread() ) )
				    .ifPresent( ( sft ) -> {
					    new StoppedEvent( "step", ( int ) stepEvent.thread().uniqueID() ).send( this.debugAdapterOutput );

					    this.status		= Status.STOPPED;
					    this.lastThread	= stepEvent.thread();
					    this.cachedThreads = new HashMap<Integer, CachedThreadReference>();

					    this.stepStrategy.dispose();
					    this.stepStrategy = null;
				    } );
			}

		}

		setAllBreakpoints();
	}

	private void lookForBoxLangClasses() {
		this.vm.allClasses().stream()
		    .filter( ( refType ) -> refType.name().toLowerCase().contains( "boxgenerated" ) )
		    .forEach( ( refType ) -> {
			    vmClasses.add( refType );
			    SourceMap map = javaBoxpiler.getSourceMapFromFQN( refType.name() );
			    if ( map == null ) {
				    return;
			    }
			    this.sourceMaps.put( map.source.toLowerCase(), map );
			    this.sourceMapsFromFQN.put( refType.name(), map );
		    } );
	}

	private void handleClassPrepareEvent( ClassPrepareEvent event ) {
		vmClasses.add( event.referenceType() );
		SourceMap map = javaBoxpiler.getSourceMapFromFQN( event.referenceType().name() );
		if ( map == null ) {
			return;
		}
		this.sourceMaps.put( map.source.toLowerCase(), map );
		this.sourceMapsFromFQN.put( event.referenceType().name(), map );
	}

	private void handleDeathEvent( VMDeathEvent de ) {

		this.status = Status.DONE;

		this.vm.process().onExit().join();
		new ExitEvent( this.vm.process().exitValue() ).send( this.debugAdapterOutput );
		new TerminatedEvent().send( this.debugAdapterOutput );
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
			if ( entry.getKey().name().compareTo( name ) == 0 ) {
				return JDITools.wrap( tuple.thread, entry.getValue() );
			}
		}

		return null;

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

	private void handleBreakPointEvent( BreakpointEvent bpe ) throws IncompatibleThreadStateException, AbsentInformationException {
		this.status = Status.STOPPED;
		this.debugAdapter.sendStoppedEventForBreakpoint( bpe );
		this.bpe = bpe;
		this.cacheOrGetThread( ( int ) this.bpe.thread().uniqueID() );
		this.lastThread = this.bpe.thread();
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
						SourceMapRecord	foundMapRecord	= javaBoxpiler.getSourceMapFromFQN( vmClass.name() ).findClosestSourceMapRecord( breakpoint.line );
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

		return vmClasses.stream().filter( ( rt ) -> referenceTypes.contains( normalizeName( rt.name() ) ) ).toList();
	}

	private String normalizeName( String className ) {
		return className.replaceAll( "\\W", "" ).toLowerCase();
	}
}
