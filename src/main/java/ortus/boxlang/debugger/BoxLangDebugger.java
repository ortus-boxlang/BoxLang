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
import java.util.HashMap;
import java.util.List;
// import java.lang.StackWalker.StackFrame;
import java.util.Map;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Locatable;
import com.sun.jdi.Location;
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
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.StepRequest;

import ortus.boxlang.debugger.event.OutputEvent;
import ortus.boxlang.debugger.request.Breakpoint;
import ortus.boxlang.runtime.runnables.compiler.JavaBoxpiler;
import ortus.boxlang.runtime.runnables.compiler.JavaBoxpiler.ClassInfo;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class BoxLangDebugger implements IBoxLangDebugger {

	private Class					debugClass;
	private int[]					breakPointLines;
	private String					cliArgs;
	private OutputStream			debugAdapterOutput;
	Map<String, List<Breakpoint>>	breakpoints;
	private VirtualMachine			vm;
	private List<ReferenceType>		vmClasses;
	private JavaBoxpiler			javaBoxpiler;
	private Status					status;
	private DebugAdapter			debugAdapter;
	private InputStream				vmInput;
	private InputStream				vmErrorInput;

	public enum Status {
		NOT_STARTED,
		INITIALIZED,
		STARTED,
		RUNNING,
		STOPPED,
		DONE
	}

	public BoxLangDebugger( Class debugClass, String cliArgs, OutputStream debugAdapterOutput, DebugAdapter debugAdapter ) {
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

				if ( this.status == Status.RUNNING ) {
					vm.resume();
				}
			}
		} catch ( Exception e ) {
			e.printStackTrace();
		} finally {
			readVMInput();
			readVMErrorInput();
		}
	}

	private void processVMEvents( EventSet eventSet ) throws IncompatibleThreadStateException, AbsentInformationException {
		for ( Event event : eventSet ) {
			System.out.println( "Found event: " + event.toString() );

			if ( event instanceof VMDeathEvent de ) {
				this.status = Status.DONE;
			}
			if ( event instanceof ClassPrepareEvent cpe ) {
				vmClasses.add( cpe.referenceType() );
				setAllBreakpoints();
			}
			if ( event instanceof BreakpointEvent bpe ) {
				handleBreakPointEvent( bpe );
			}
			if ( event instanceof StepEvent ) {
				// displayVariables( ( StepEvent ) event );
			}

		}
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

	public void begin() {
		this.status = Status.RUNNING;
	}

	public void forceResume() {
		this.status = Status.RUNNING;
	}

	public void startDebugSession() {
		// this.vm = null;

		// try {
		// this.vm = connectAndLaunchVM();
		// enableClassPrepareRequest( vm );
		// // vm.setDebugTraceMode( VirtualMachine.TRACE_ALL );
		// EventSet eventSet = null;
		// InputStream vmInput = vm.process().getInputStream();
		// InputStream vmErrorInput = vm.process().getErrorStream();

		// this.status = Status.RUNNING;

		// while ( this.status != Status.DONE ) {
		// while ( ( eventSet = vm.eventQueue().remove() ) != null ) {

		// System.out.println( "checking available" );
		// int available = vmInput.available();
		// if ( available > 0 ) {
		// byte[] bytes = ByteBuffer.allocate( available ).array();
		// vmInput.read( bytes );

		// new OutputEvent( "stdout", new String( bytes ) ).send( this.debugAdapterOutput );
		// }
		// int errorAvailable = vmErrorInput.available();
		// if ( errorAvailable > 0 ) {
		// byte[] bytes = ByteBuffer.allocate( errorAvailable ).array();
		// vmErrorInput.read( bytes );

		// new OutputEvent( "stderr", new String( bytes ) ).send( this.debugAdapterOutput );
		// }

		// for ( Event event : eventSet ) {
		// System.out.println( "Found event: " + event.toString() );

		// if ( event instanceof VMDeathEvent de ) {
		// this.status = Status.DONE;
		// }
		// if ( event instanceof ClassPrepareEvent cpe ) {
		// vmClasses.add( cpe.referenceType() );
		// setAllBreakpoints();
		// }
		// if ( event instanceof BreakpointEvent bpe ) {
		// handleBreakPointEvent( bpe );
		// }
		// if ( event instanceof StepEvent ) {
		// displayVariables( ( StepEvent ) event );
		// }

		// if ( this.status == Status.RUNNING ) {
		// vm.resume();
		// }
		// }
		// }
		// }
		// } catch ( VMDisconnectedException e ) {
		// // pass
		// } catch ( Exception e ) {
		// e.printStackTrace();
		// } finally {
		// try {
		// System.out.println( "checking available" );
		// InputStream vmInput = vm.process().getInputStream();
		// int available = vmInput.available();
		// if ( available > 0 ) {
		// byte[] bytes = ByteBuffer.allocate( available ).array();
		// vmInput.read( bytes );

		// new OutputEvent( "stdout", new String( bytes ) ).send( this.debugAdapterOutput );
		// }
		// } catch ( IOException e ) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		// }

		// System.out.println( "done debugging" );
	}

	private void handleBreakPointEvent( BreakpointEvent bpe ) throws IncompatibleThreadStateException, AbsentInformationException {
		this.status = Status.STOPPED;
		this.debugAdapter.sendStoppedEventForBreakpoint( ( int ) bpe.thread().uniqueID() );
		// displayVariables( bpe );

		enableStepRequest( vm, bpe );
	}

	public VirtualMachine connectAndLaunchVM() throws Exception {

		LaunchingConnector				launchingConnector	= Bootstrap.virtualMachineManager()
		    .defaultConnector();
		Map<String, Connector.Argument>	arguments			= launchingConnector.defaultArguments();
		// arguments.get( "main" ).setValue( debugClass.getName() );
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
		// classPrepareRequest.addClassFilter( debugClass.getName() );
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

	public void displayVariables( LocatableEvent event ) throws IncompatibleThreadStateException,
	    AbsentInformationException {
		StackFrame					stackFrame			= ( StackFrame ) event.thread().frame( 0 );
		// if ( ( ( Locatable ) stackFrame ).location().toString().contains( debugClass.getName() ) ) {
		Map<LocalVariable, Value>	visibleVariables	= stackFrame.getValues( stackFrame.visibleVariables() );
		System.out.println( "Variables at " + ( ( Locatable ) stackFrame ).location().toString() + " > " );
		for ( Map.Entry<LocalVariable, Value> entry : visibleVariables.entrySet() ) {
			System.out.println( entry.getKey().name() + " = " + entry.getValue() );
		}
		// }
	}

	public void enableStepRequest( VirtualMachine vm, BreakpointEvent event ) {
		// enable step request for last break point
		// if ( event.location().toString().contains( debugClass.getName() + ":" + breakPointLines[ breakPointLines.length - 1 ] ) ) {
		StepRequest stepRequest = vm.eventRequestManager()
		    .createStepRequest( event.thread(), StepRequest.STEP_LINE, StepRequest.STEP_OVER );
		stepRequest.enable();
		// }
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

	private void setAllBreakpoints() {
		if ( this.vm == null ) {
			return;
		}

		this.vm.eventRequestManager().deleteAllBreakpoints();

		for ( String fileName : this.breakpoints.keySet() ) {
			ReferenceType vmClass = getVMReferenceType( fileName );

			if ( vmClass == null ) {
				continue;
			}

			for ( Breakpoint breakpoint : this.breakpoints.get( fileName ) ) {
				try {

					int			javaSourceLine	= javaBoxpiler.convertSourceLineToJavaLine( vmClass.name(), breakpoint.line );
					Location	location;
					try {
						location = vmClass.locationsOfLine( javaSourceLine ).get( 0 );
						BreakpointRequest bpReq = vm.eventRequestManager().createBreakpointRequest( location );
						bpReq.enable();
					} catch ( AbsentInformationException e ) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} catch ( BoxRuntimeException e ) {
					e.printStackTrace();
				}

			}
		}
	}

	private ReferenceType getVMReferenceType( String fileName ) {
		ClassInfo classInfo = ClassInfo.forTemplate( Path.of( fileName ), fileName );
		for ( ReferenceType vmClass : this.vmClasses ) {
			if ( classInfo.matchesFQNWithoutCompileCount( vmClass.name() ) ) {
				return vmClass;
			}
		}

		return null;
	}
}
