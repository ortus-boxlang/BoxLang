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
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Listens on the provided port to allow connections from a debug tool. Each time a client connects a new DebugAdapter will be initialized and used to
 * marshal DAP messages.
 */
public class BoxLangRemoteDebugger implements IBoxLangDebugger {

	private int port;

	/**
	 * Constructor
	 * 
	 * @param port The port to listen for connections on
	 */
	public BoxLangRemoteDebugger( int port ) {
		this.port = port;
	}

	/**
	 * Starts the server
	 */
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
