package ortus.boxlang.debugger;

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
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;

import javassist.bytecode.SignatureAttribute.ClassType;

public class BoxLangDebugger {

	private Class	debugClass;
	private int[]	breakPointLines;

	public BoxLangDebugger( Class debugClass, int[] breakPointLines ) {
		this.debugClass = debugClass;
	}

	public void startDebugSession() throws Exception {
		VirtualMachine vm = null;

		try {
			vm = connectAndLaunchVM();
			enableClassPrepareRequest( vm );
			EventSet eventSet = null;
			while ( ( eventSet = vm.eventQueue().remove() ) != null ) {
				for ( Event event : eventSet ) {
					if ( event instanceof ClassPrepareEvent ) {
						setBreakPoints( vm, ( ClassPrepareEvent ) event );
					}
					if ( event instanceof BreakpointEvent ) {
						displayVariables( ( BreakpointEvent ) event );
					}
					vm.resume();
				}
			}
		} catch ( VMDisconnectedException e ) {
			System.out.println( "Virtual Machine is disconnected." );
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}

	public VirtualMachine connectAndLaunchVM() throws Exception {

		LaunchingConnector				launchingConnector	= Bootstrap.virtualMachineManager()
		    .defaultConnector();
		Map<String, Connector.Argument>	arguments			= launchingConnector.defaultArguments();
		arguments.get( "main" ).setValue( debugClass.getName() );
		return launchingConnector.launch( arguments );
	}

	public void enableClassPrepareRequest( VirtualMachine vm ) {
		ClassPrepareRequest classPrepareRequest = vm.eventRequestManager().createClassPrepareRequest();
		classPrepareRequest.addClassFilter( debugClass.getName() );
		classPrepareRequest.enable();
	}

	public void setBreakPoints( VirtualMachine vm, ClassPrepareEvent event ) throws AbsentInformationException {
		ClassType classType = ( ClassType ) event.referenceType();
		for ( int lineNumber : breakPointLines ) {
			Location			location	= ( ( ReferenceType ) classType ).locationsOfLine( lineNumber ).get( 0 );
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
}
