package ortus.boxlang.debugger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.types.util.JSONUtil;

public class AdapterProtocolMessageReader {

	private Logger				logger;
	private BufferedReader		bufferedReader;
	private Map<String, Class>	parseMap;
	public boolean				throwOnUnregisteredCommand	= true;

	public AdapterProtocolMessageReader( InputStream inputStream ) throws IOException {
		this.logger			= LoggerFactory.getLogger( BoxRuntime.class );
		this.bufferedReader	= new BufferedReader( new InputStreamReader( inputStream ), 4096 );

		this.parseMap		= new HashMap<String, Class>();
	}

	/**
	 * Parse a debug request and deserialie it into its associated class.
	 * 
	 * @param json
	 * 
	 * @return
	 */
	private IAdapterProtocolMessage parseAdapterProtocolMessage( String json ) {
		Map<String, Object>	requestData	= ( Map<String, Object> ) JSONUtil.fromJSON( json );
		String				command		= ( String ) requestData.get( "command" );

		this.logger.info( "Received command {}", command );
		this.logger.info( "Received command {}", json );

		Class parseTarget = this.parseMap.get( command );

		if ( parseTarget != null ) {
			IAdapterProtocolMessage message = ( IAdapterProtocolMessage ) JSONUtil.fromJSON( parseTarget, json );
			message.setRawMessageData( requestData );

			return message;
		}

		if ( throwOnUnregisteredCommand ) {
			throw new NotImplementedException( command );
		}

		IAdapterProtocolMessage message = new MapAdapterProtocolMessage();
		message.setRawMessageData( requestData );
		return message;
	}

	public AdapterProtocolMessageReader register( String command, Class parseTarget ) {
		this.parseMap.put( command, parseTarget );

		return this;
	}

	public IAdapterProtocolMessage read() throws IOException {
		String line = bufferedReader.readLine();
		System.out.println( line );
		Pattern	p	= Pattern.compile( "Content-Length: (\\d+)" );
		Matcher	m	= p.matcher( line );

		if ( m.find() ) {
			int			contentLength	= Integer.parseInt( m.group( 1 ) );
			CharBuffer	buf				= CharBuffer.allocate( contentLength );

			bufferedReader.readLine();
			bufferedReader.read( buf );

			return parseAdapterProtocolMessage( new String( buf.array() ) );
		}

		return null;
	}
}
