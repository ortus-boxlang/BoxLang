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
import java.util.List;
// import java.lang.StackWalker.StackFrame;
import java.util.Map;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Bootstrap;
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
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.StepRequest;

import ortus.boxlang.debugger.JDITools.WrappedValue;
import ortus.boxlang.debugger.event.ExitEvent;
import ortus.boxlang.debugger.event.OutputEvent;
import ortus.boxlang.debugger.event.TerminatedEvent;
import ortus.boxlang.debugger.types.Breakpoint;
import ortus.boxlang.debugger.types.Variable;
import ortus.boxlang.runtime.runnables.compiler.JavaBoxpiler;
import ortus.boxlang.runtime.runnables.compiler.JavaBoxpiler.ClassInfo;
import ortus.boxlang.runtime.runnables.compiler.SourceMap.SourceMapRecord;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class BoxLangDebugger implements IBoxLangDebugger {

	private static Map<Integer, StackFrameTuple> seenStacks;

	static {
		seenStacks = new HashMap<Integer, StackFrameTuple>();
	}

	public VirtualMachine			vm;
	private Class					debugClass;
	private int[]					breakPointLines;
	private String					cliArgs;
	private OutputStream			debugAdapterOutput;
	Map<String, List<Breakpoint>>	breakpoints;
	private List<ReferenceType>		vmClasses;
	private JavaBoxpiler			javaBoxpiler;
	private Status					status;
	private DebugAdapter			debugAdapter;
	private InputStream				vmInput;
	private InputStream				vmErrorInput;
	public BreakpointEvent			bpe;

	private ClassType				keyClassRef	= null;

	public enum Status {
		NOT_STARTED,
		INITIALIZED,
		STARTED,
		RUNNING,
		STOPPED,
		DONE
	}

	public BoxLangDebugger( Class<?> debugClass, String cliArgs, OutputStream debugAdapterOutput, DebugAdapter debugAdapter ) {
		this.debugClass			= debugClass;
		this.breakPointLines	= new int[] {};
		this.cliArgs			= cliArgs;
		this.debugAdapterOutput	= debugAdapterOutput;
		breakpoints				= new HashMap<>();
		vmClasses				= new ArrayList<>();
		this.javaBoxpiler		= JavaBoxpiler.getInstance();
		this.status				= Status.NOT_STARTED;
		this.debugAdapter		= debugAdapter;
	}

	public void initialize() {
		try {
			this.vm = connectAndLaunchVM();
		} catch ( Exception e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		enableClassPrepareRequest( vm );

		this.vmInput		= vm.process().getInputStream();
		this.vmErrorInput	= vm.process().getErrorStream();

		this.status			= Status.RUNNING;
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

	public void forceResume() {
		this.status = Status.RUNNING;
		this.bpe.thread().resume();
	}

	public void startDebugSession() {
		// pass
	}

	public VirtualMachine connectAndLaunchVM() throws Exception {

		LaunchingConnector				launchingConnector	= Bootstrap.virtualMachineManager()
		    .defaultConnector();
		Map<String, Connector.Argument>	arguments			= launchingConnector.defaultArguments();
		arguments.get( "options" ).setValue( "-cp " + System.getProperty( "java.class.path" ) );
		arguments.get( "main" ).setValue( debugClass.getName() + " " + cliArgs );
		VirtualMachine vm = launchingConnector.launch( arguments );

		return vm;
	}

	public void addBreakpoint( String filePath, Breakpoint breakpoint ) {
		List<Breakpoint> breakpoints = this.breakpoints.computeIfAbsent( filePath, ( key ) -> new ArrayList<Breakpoint>() );
		breakpoints.add( breakpoint );
		setAllBreakpoints();
	}

	public void enableClassPrepareRequest( VirtualMachine vm ) {
		ClassPrepareRequest classPrepareRequest = vm.eventRequestManager().createClassPrepareRequest();
		classPrepareRequest.addClassFilter( "boxgenerated.*" );
		classPrepareRequest.enable();
	}

	public void setBreakPoints( VirtualMachine vm, ClassPrepareEvent event ) throws AbsentInformationException {
		ReferenceType classType = event.referenceType();
		for ( int lineNumber : breakPointLines ) {
			Location			location	= classType.locationsOfLine( lineNumber ).get( 0 );
			BreakpointRequest	bpReq		= vm.eventRequestManager().createBreakpointRequest( location );
			bpReq.enable();
		}
	}

	public void enableStepRequest( VirtualMachine vm, BreakpointEvent event ) {
		// enable step request for last break point
		// if ( event.location().toString().contains( debugClass.getName() + ":" + breakPointLines[ breakPointLines.length - 1 ] ) ) {
		StepRequest stepRequest = vm.eventRequestManager()
		    .createStepRequest( event.thread(), StepRequest.STEP_LINE, StepRequest.STEP_OVER );
		stepRequest.enable();
		// }
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

	public StackFrame findStackFrame( int id ) {
		for ( ThreadReference thread : getAllThreadReferences() ) {
			try {
				for ( StackFrame stackFrame : getStackFrames( thread.hashCode() ) ) {
					if ( stackFrame.hashCode() == id ) {
						return stackFrame;
					}
				}
			} catch ( IncompatibleThreadStateException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
			return ref.newInstance( bpe.thread(), contstructor, Arrays.asList( this.vm.mirrorOf( name ) ), 0 );
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

	public record StackFrameTuple( StackFrame stackFrame, Location location, int id, Map<LocalVariable, Value> values ) {

	}

	public StackFrameTuple getSeenStack( int stackFrameId ) {
		return seenStacks.get( stackFrameId );
	}

	public List<StackFrameTuple> getBoxLangStackFrames( int threadId ) throws IncompatibleThreadStateException {
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

		return matchingThreadRef.frames()
		    .stream()
		    .filter( ( stackFrame ) -> stackFrame.location().declaringType().name().contains( "boxgenerated" ) )
		    .map( ( sf ) -> {
			    try {

				    seenStacks.put( sf.hashCode(), new StackFrameTuple( sf, sf.location(), sf.hashCode(), sf.getValues( sf.visibleVariables() ) ) );
			    } catch ( AbsentInformationException e ) {
				    // TODO handle exception
				    e.printStackTrace();
			    }

			    return seenStacks.get( sf.hashCode() );
		    } )
		    .toList();
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
				vmClasses.add( cpe.referenceType() );
			}
			if ( event instanceof BreakpointEvent bpe ) {
				handleBreakPointEvent( bpe );
			}
			if ( event instanceof StepEvent ) {
				// pass
			}

		}

		setAllBreakpoints();
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

	public WrappedValue findVariableyName( StackFrameTuple tuple, String name ) {
		Map<LocalVariable, Value> visibleVariables = tuple.values;

		for ( Map.Entry<LocalVariable, Value> entry : visibleVariables.entrySet() ) {
			if ( entry.getKey().name().compareTo( name ) == 0 ) {
				return JDITools.wrap( bpe.thread(), entry.getValue() );
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
	}

	private void setAllBreakpoints() {
		if ( this.vm == null ) {
			return;
		}

		this.vm.eventRequestManager().deleteAllBreakpoints();

		for ( String fileName : this.breakpoints.keySet() ) {

			for ( Breakpoint breakpoint : this.breakpoints.get( fileName ) ) {
				List<ReferenceType>	matchingTypes		= getMatchingReferenceTypes( fileName );
				List<Location>		possibleLocations	= new ArrayList<Location>();

				if ( matchingTypes.size() == 0 ) {
					continue;
				}

				for ( ReferenceType vmClass : matchingTypes ) {
					try {
						SourceMapRecord	foundMapRecord	= javaBoxpiler.getSourceMapFromFQN( vmClass.name() ).findClosestSourceMapRecord( breakpoint.line );
						String			sourceName		= foundMapRecord.javaSourceClassName.replaceAll( "(\\d)(\\.)", "$1\\$" );
						if ( sourceName.compareToIgnoreCase( vmClass.name() ) != 0 ) {
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
		ClassInfo classInfo = getClassInfo( fileName );
		return vmClasses.stream().filter( ( rt ) -> classInfo.matchesFQNWithoutCompileCount( rt.name() ) ).toList();
	}

	private ClassInfo getClassInfo( String fileName ) {
		if ( fileName.endsWith( "bx" ) ) {
			return ClassInfo.forClass( Path.of( fileName ), fileName );
		}

		return ClassInfo.forTemplate( Path.of( fileName ), fileName );
	}
}
