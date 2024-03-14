package ortus.boxlang.debugger;

import java.util.Map;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;

public class InlineStrategy implements IVMInitializationStrategy {

	String program;

	InlineStrategy( String program ) {
		this.program = program;
	}

	@Override
	public VirtualMachine initialize() throws Exception {
		LaunchingConnector				launchingConnector	= Bootstrap.virtualMachineManager().defaultConnector();
		Map<String, Connector.Argument>	arguments			= launchingConnector.defaultArguments();

		arguments.get( "options" ).setValue( "-cp " + System.getProperty( "java.class.path" ) );
		arguments.get( "main" ).setValue( "ortus.boxlang.runtime.BoxRunner" + " " + this.program );

		return launchingConnector.launch( arguments );
	}

	@Override
	public void disconnect( VirtualMachine vm ) {

	}

	@Override
	public void terminate( VirtualMachine vm ) {
		vm.exit( 0 );
	}

}
