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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.CharBuffer;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.debugger.event.Event;
import ortus.boxlang.debugger.request.ConfigurationDoneRequest;
import ortus.boxlang.debugger.request.IDebugRequest;
import ortus.boxlang.debugger.request.InitializeRequest;
import ortus.boxlang.debugger.request.LaunchRequest;
import ortus.boxlang.debugger.request.SetBreakpointsRequest;
import ortus.boxlang.debugger.response.InitializeResponse;
import ortus.boxlang.debugger.response.NoBodyResponse;
import ortus.boxlang.debugger.response.SetBreakpointsResponse;
import ortus.boxlang.runtime.BoxRunner;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.util.JsonUtil;

/**
 * Implements Microsoft's Debug Adapter Protocol https://microsoft.github.io/debug-adapter-protocol/
 */
public class DebugAdapter {

	private Socket				debugClient;
	private Thread				inputThread;
	private Logger				logger;
	private OutputStream		output;
	private IBoxLangDebugger	debugger;
	private boolean				running	= true;

	/**
	 * Constructor
	 * 
	 * @param debugClient The socket that handles communication with the debug tool
	 */
	public DebugAdapter( Socket debugClient ) {
		this.debugClient	= debugClient;
		this.logger			= LoggerFactory.getLogger( BoxRuntime.class );

		try {
			createInputListenerThread();
			createOutputStream();
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Used to determin if the debug session has completed.
	 * 
	 * @return
	 */
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Creates an OutputStream from the debug client socket
	 * 
	 * @throws IOException
	 */
	private void createOutputStream() throws IOException {
		this.output = debugClient.getOutputStream();
	}

	/**
	 * Starts a new thread to wait for messages from the debug client. Each message will deserialized and then visited.
	 * 
	 * @throws IOException
	 */
	private void createInputListenerThread() throws IOException {
		Socket				socket	= this.debugClient;
		InputStream			iS		= socket.getInputStream();
		InputStreamReader	iSR		= new InputStreamReader( iS );
		BufferedReader		bR		= new BufferedReader( iSR );
		DebugAdapter		adapter	= this;

		this.inputThread = new Thread( new Runnable() {

			@Override
			public void run() {
				while ( true ) {
					try {
						String line = bR.readLine();
						System.out.println( line );
						Pattern	p	= Pattern.compile( "Content-Length: (\\d+)" );
						Matcher	m	= p.matcher( line );

						if ( m.find() ) {
							int			contentLength	= Integer.parseInt( m.group( 1 ) );
							CharBuffer	buf				= CharBuffer.allocate( contentLength );

							bR.readLine();
							bR.read( buf );

							IDebugRequest request = parseDebugRequest( new String( buf.array() ) );

							if ( request != null ) {
								request.accept( adapter );
							}
						}
					} catch ( IOException e ) {
						// TODO handle this exception
						e.printStackTrace();
						break;
					}
				}
			}

		} );

		this.inputThread.start();
	}

	/**
	 * Parse a debug request and deserialie it into its associated class.
	 * 
	 * @param json
	 * 
	 * @return
	 */
	private IDebugRequest parseDebugRequest( String json ) {
		Map<String, Object>	requestData	= ( Map<String, Object> ) JsonUtil.fromJson( json );
		String				command		= ( String ) requestData.get( "command" );

		this.logger.info( "Received command {}", command );

		switch ( command ) {
			case "initialize" :
				return JsonUtil.fromJson( InitializeRequest.class, json );
			case "launch" :
				return JsonUtil.fromJson( LaunchRequest.class, json );
			case "setBreakpoints" :
				return JsonUtil.fromJson( SetBreakpointsRequest.class, json );
			case "configurationDone" :
				return JsonUtil.fromJson( ConfigurationDoneRequest.class, json );
		}

		return null;
		// throw new NotImplementedException( command );
	}

	/**
	 * Default visit handler
	 * 
	 * @param debugRequest
	 */
	public void visit( IDebugRequest debugRequest ) {
		// throw new NotImplementedException( debugRequest.getCommand() );
	}

	/**
	 * Visit InitializeRequest instances. Respond to the initialize request and send an initialized event.
	 * 
	 * @param debugRequest
	 */
	public void visit( InitializeRequest debugRequest ) {
		new InitializeResponse( debugRequest ).send( this.output );
		new Event( "initialized" ).send( this.output );
	}

	/**
	 * Visit LaunchRequest instances. Send a NobodyResponse and setup a BoxLangDebugger.
	 * 
	 * @param debugRequest
	 */
	public void visit( LaunchRequest debugRequest ) {
		new NoBodyResponse( debugRequest ).send( this.output );
		this.debugger = new BoxLangDebugger( BoxRunner.class, debugRequest.arguments.program, this.output );
	}

	/**
	 * Visit SetBreakpointsRequest instances. Send a response.
	 * 
	 * @param debugRequest
	 */
	public void visit( SetBreakpointsRequest debugRequest ) {
		new SetBreakpointsResponse( debugRequest ).send( this.output );
	}

	/**
	 * Visit ConfigurationDoneRequest instances. After responding the debugger can begin executing.
	 * 
	 * @param debugRequest
	 */
	public void visit( ConfigurationDoneRequest debugRequest ) {
		new NoBodyResponse( debugRequest ).send( this.output );

		this.debugger.startDebugSession();
		this.running = false;
	}
}
