package ortus.boxlang.debugger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class BoxLangRemoteDebugger implements IBoxLangDebugger {

	int port;

	public BoxLangRemoteDebugger( int port ) {
		this.port = port;
	}

	@Override
	public void startDebugSession() {

		System.out.println( "starting the debug server" );

		try ( ServerSocket socket = new ServerSocket( this.port ) ) {
			while ( true ) {
				Socket			connectionSocket	= socket.accept();
				DebugAdapter	adapter				= new DebugAdapter( connectionSocket );

				while ( adapter.isRunning() ) {
					// wait until adapter is finished
				}

				connectionSocket.close();
				System.out.println( "Closing debug connection" );
			}
		} catch ( IOException e ) {
			e.printStackTrace();
		}
	}

}
