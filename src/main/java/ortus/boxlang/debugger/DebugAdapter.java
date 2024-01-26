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

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.debugger.event.Event;
import ortus.boxlang.debugger.request.IDebugRequest;
import ortus.boxlang.debugger.request.InitializeRequest;
import ortus.boxlang.debugger.request.LaunchRequest;
import ortus.boxlang.debugger.response.InitializeResponse;
import ortus.boxlang.runtime.BoxRunner;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.util.JsonUtil;

public class DebugAdapter {

	private Socket				debugClient;
	private Thread				inputThread;
	private Logger				logger;
	private OutputStream		output;
	private IBoxLangDebugger	debugger;
	private boolean				running	= true;

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

	public boolean isRunning() {
		return this.running;
	}

	private void createOutputStream() throws IOException {
		this.output = debugClient.getOutputStream();
	}

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
						e.printStackTrace();
						break;
					}
				}
			}

		} );

		this.inputThread.start();
	}

	private IDebugRequest parseDebugRequest( String json ) {
		Map<String, Object>	requestData	= ( Map ) JsonUtil.fromJson( json );
		String				command		= ( String ) requestData.get( "command" );

		this.logger.info( "Received command {}", command );

		switch ( command ) {
			case "initialize" :
				return new InitializeRequest( requestData );
			case "launch" :
				return new LaunchRequest( requestData );
		}

		return null;
		// throw new NotImplementedException( command );
	}

	public void visit( IDebugRequest debugRequest ) {
		throw new NotImplementedException( debugRequest.getCommand() );
	}

	public void visit( InitializeRequest debugRequest ) {
		new InitializeResponse( debugRequest ).send( this.output );
		new Event( "initialized" ).send( this.output );
	}

	public void visit( LaunchRequest debugRequest ) {
		this.debugger = new BoxLangDebugger( BoxRunner.class, debugRequest.program, this.output );
		this.debugger.startDebugSession();
		this.running = false;
	}

}
