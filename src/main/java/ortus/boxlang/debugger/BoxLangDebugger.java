package ortus.boxlang.debugger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
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
import com.sun.jdi.VMDisconnectedException;
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

public class BoxLangDebugger {

	private Class	debugClass;
	private int[]	breakPointLines;
	private String	cliArgs;

	private class CurrentJVMArgs {

		static String fullVMArguments() {
			String name = javaVmName();

			return ( contains( name, "Server" ) ? "-server "
			    : contains( name, "Client" ) ? "-client " : "" )
			    + String.join( " ", vmArguments() );
		}

		static List<String> vmArguments() {
			return ManagementFactory.getRuntimeMXBean().getInputArguments();
		}

		static boolean contains( String s, String b ) {
			return s != null && s.indexOf( b ) >= 0;
		}

		static String javaVmName() {
			return System.getProperty( "java.vm.name" );
		}

		// static String joinWithSpace( Collection<String> c ) {
		// return join( " ", c );
		// }

		// public static String join( String glue, Iterable<String> strings ) {
		// if ( strings == null )
		// return "";
		// StringBuilder buf = new StringBuilder();
		// Iterator<String> i = strings.iterator();
		// if ( i.hasNext() ) {
		// buf.append( i.next() );
		// while ( i.hasNext() )
		// buf.append( glue ).append( i.next() );
		// }
		// return buf.toString();
		// }
	}

	// public static void main( String[] args ) {
	// String ar = CurrentJVMArgs.fullVMArguments();
	// BoxLangDebugger bld = new BoxLangDebugger( ortus.boxlang.debugger.Debugee.class, new int[] { 9 } );
	// bld.startDebugSession();
	// }

	public BoxLangDebugger( Class debugClass, String cliArgs ) {
		this.debugClass			= debugClass;
		this.breakPointLines	= new int[] {};
		this.cliArgs			= cliArgs;
		// this.debugClass = ortus.boxlang.debugger.Debugee.class;
	}

	public void startDebugSession() {
		// String ar = CurrentJVMArgs.fullVMArguments();
		VirtualMachine vm = null;

		try {
			vm = connectAndLaunchVM();
			enableClassPrepareRequest( vm );
			// vm.setDebugTraceMode( VirtualMachine.TRACE_ALL );
			EventSet eventSet = null;
			while ( ( eventSet = vm.eventQueue().remove() ) != null ) {
				System.out.println( "as" );
				for ( Event event : eventSet ) {
					System.out.println( "Found event: " + event.toString() );
					if ( event instanceof VMDeathEvent de ) {
						System.out.println( "Found event: " + event.toString() );
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
			System.out.println( "out of hte while" );
		} catch ( VMDisconnectedException e ) {
			System.out.println( "Virtual Machine is disconnected." );
			e.printStackTrace();
		} catch ( Exception e ) {
			System.out.println( "eeeeeeeeeee" );
			e.printStackTrace();
		} finally {
			InputStreamReader	reader	= new InputStreamReader( vm.process().getInputStream() );
			OutputStreamWriter	writer	= new OutputStreamWriter( System.out );
			char[]				buf		= new char[ 1024 ];

			try {
				reader.read( buf );
				writer.write( buf );
				writer.flush();
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
		return launchingConnector.launch( arguments );
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
