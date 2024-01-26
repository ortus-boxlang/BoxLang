package ortus.boxlang.debugger;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.jr.ob.JSONObjectException;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.util.JsonUtil;

public interface ISendable {

	Object logger = null;

	default public String toJSON() {
		try {
			return JsonUtil.getJsonBuilder().asString( this );
		} catch ( JSONObjectException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "";
	}

	public String getType();

	public String getName();

	default public void send( OutputStream out ) {
		String	payload	= this.toJSON();
		int		size	= payload.getBytes().length;
		String	header	= String.format( "Content-Length: %d\r\n\r\n", size );
		Logger	logger	= LoggerFactory.getLogger( BoxRuntime.class );

		try {
			logger.info( "Sending message of type: {}", this.getType() );
			logger.info( "Size is {}", size );
			logger.info( header + payload );
			out.write( ( header + payload ).getBytes() );
			out.flush();
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
