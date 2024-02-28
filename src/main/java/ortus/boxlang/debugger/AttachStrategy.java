package ortus.boxlang.debugger;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;

public class AttachStrategy implements IVMInitializationStrategy {

	private int port;

	public AttachStrategy( int port ) {
		this.port = port;
	}

	@Override
	public VirtualMachine initialize() throws IOException, IllegalConnectorArgumentsException {
		List<AttachingConnector>		connectors		= Bootstrap.virtualMachineManager().attachingConnectors();

		// TODO select the socketConnector through a more reliable method
		AttachingConnector				socketConnector	= connectors.get( 1 );
		Map<String, Connector.Argument>	connectorArgs	= socketConnector.defaultArguments();
		connectorArgs.get( "port" ).setValue( Integer.toString( this.port ) );

		return socketConnector.attach( connectorArgs );
	}

}
