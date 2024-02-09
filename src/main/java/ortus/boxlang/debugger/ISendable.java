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
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.jr.ob.JSONObjectException;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.util.JSONUtil;

/**
 * Interface for messages that will be sent to the debugger tool.
 */
public interface ISendable {

	Object logger = null;

	/**
	 * Serialize the instance to JSON
	 * 
	 * @return
	 */
	default public String toJSON() {
		try {
			return JSONUtil.getJSONBuilder().asString( this );
		} catch ( JSONObjectException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "";
	}

	/**
	 * The type; either: request, event, or response.
	 * 
	 * @return
	 */
	public String getType();

	/**
	 * Used internally for logging
	 * 
	 * @return
	 */
	public String getName();

	/**
	 * Sends this object through the OutputStream
	 * 
	 * @param out
	 */
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
