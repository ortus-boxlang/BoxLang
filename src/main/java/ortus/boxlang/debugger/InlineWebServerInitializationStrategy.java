package ortus.boxlang.debugger;

import java.util.ArrayList;
import java.util.Map;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;

public class InlineWebServerInitializationStrategy implements IVMInitializationStrategy {

	Integer	port;
	String	webRoot;

	InlineWebServerInitializationStrategy( Integer port, String webRoot ) {
		this.port		= port;
		this.webRoot	= webRoot;
	}

	@Override
	public VirtualMachine initialize() throws Exception {
		LaunchingConnector				launchingConnector	= Bootstrap.virtualMachineManager().defaultConnector();
		Map<String, Connector.Argument>	arguments			= launchingConnector.defaultArguments();

		arguments.get( "options" ).setValue( "-cp " + System.getProperty( "java.class.path" ) );
		arguments.get( "main" ).setValue( getMainArgs() );

		return launchingConnector.launch( arguments );
	}

	@Override
	public void disconnect( VirtualMachine vm ) {

	}

	@Override
	public void terminate( VirtualMachine vm ) {
		vm.exit( 0 );
	}

	private String getMainArgs() {
		var args = new ArrayList<String>();

		args.add( "ortus.boxlang.web.Server" );

		if ( this.port != null ) {
			args.add( "--port" );
			args.add( Integer.toString( this.port ) );
		}

		if ( this.webRoot != null ) {
			args.add( "--webroot" );
			args.add( this.webRoot );
		}

		return String.join( " ", args );
	}
}
