package ortus.boxlang.debugger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
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

public class BoxLangDebugger implements IBoxLangDebugger {

	private Class			debugClass;
	private int[]			breakPointLines;
	private String			cliArgs;
	private OutputStream	debugAdapterOutput;

	public BoxLangDebugger( Class debugClass, String cliArgs, OutputStream debugAdapterOutput ) {
		this.debugClass			= debugClass;
		this.breakPointLines	= new int[] {};
		this.cliArgs			= cliArgs;
		this.debugAdapterOutput	= debugAdapterOutput;
	}

	public void startDebugSession() {
		VirtualMachine vm = null;

		try {
			vm = connectAndLaunchVM();
			enableClassPrepareRequest( vm );
			// vm.setDebugTraceMode( VirtualMachine.TRACE_ALL );
			EventSet	eventSet	= null;
			InputStream	vmInput		= vm.process().getInputStream();
			boolean		done		= false;
			while ( !done ) {
				while ( ( eventSet = vm.eventQueue().remove() ) != null ) {

					System.out.println( "checking available" );
					int	available	= vmInput.available();
					int	a			= 4;
					if ( available > 0 ) {
						byte[] bytes = ByteBuffer.allocate( available ).array();
						vmInput.read( bytes );

						new OutputEvent( "stdout", new String( bytes ) ).send( this.debugAdapterOutput );
					}

					for ( Event event : eventSet ) {
						System.out.println( "Found event: " + event.toString() );

						if ( event instanceof VMDeathEvent de ) {
							System.out.println( "Found event: " + event.toString() );
							done = true;
						}
						if ( event instanceof ClassPrepareEvent cpe ) {
							System.out.println( "Preparing class: " + cpe.referenceType().name() );
							// setBreakPoints( vm, cpe );
						}
						if ( event instanceof BreakpointEvent ) {
							enableStepRequest( vm, ( BreakpointEvent ) event );
						}
						if ( event instanceof StepEvent ) {
							displayVariables( ( StepEvent ) event );
						}
						vm.resume();
						System.out.println( "done resuming" );
					}
				}
			}

			System.out.println( "out of hte while" );
		} catch ( Exception e ) {
			System.out.println( "eeeeeeeeeee" );
			e.printStackTrace();
		} finally {
			try {
				System.out.println( "checking available" );
				InputStream	vmInput		= vm.process().getInputStream();
				int			available	= vmInput.available();
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

		System.out.println( "done debugging" );
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
		StackFrame stackFrame = ( StackFrame ) event.thread().frame( 0 );
		if ( ( ( Locatable ) stackFrame ).location().toString().contains( debugClass.getName() ) ) {
			Map<LocalVariable, Value> visibleVariables = stackFrame.getValues( stackFrame.visibleVariables() );
			System.out.println( "Variables at " + ( ( Locatable ) stackFrame ).location().toString() + " > " );
			for ( Map.Entry<LocalVariable, Value> entry : visibleVariables.entrySet() ) {
				System.out.println( entry.getKey().name() + " = " + entry.getValue() );
			}
		}
	}

	public void enableStepRequest( VirtualMachine vm, BreakpointEvent event ) {
		// enable step request for last break point
		if ( event.location().toString().contains( debugClass.getName() + ":" + breakPointLines[ breakPointLines.length - 1 ] ) ) {
			StepRequest stepRequest = vm.eventRequestManager()
			    .createStepRequest( event.thread(), StepRequest.STEP_LINE, StepRequest.STEP_OVER );
			stepRequest.enable();
		}
	}
}
