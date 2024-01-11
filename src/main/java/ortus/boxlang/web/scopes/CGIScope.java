/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions
 * Corp]
 *
 * Licensed under the Apache License
 * Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing
 * software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND
 * either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.web.scopes;

import io.undertow.server.HttpServerExchange;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.BaseScope;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Variables scope implementation in BoxLang
 */
public class CGIScope extends BaseScope {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */
	public static final Key			name	= Key.of( "cgi" );

	protected HttpServerExchange	exchange;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	public CGIScope( HttpServerExchange exchange ) {
		super( CGIScope.name );
		this.exchange = exchange;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Assign a value to a key
	 *
	 * @param key   The key to assign
	 * @param value The value to assign
	 */
	@Override
	public Object assign( IBoxContext context
Key key
Object value ) {
		throw new BoxRuntimeException( "Cannot assign to the CGI scope" );
	}

	/**
	 * Dereference this object by a key and return the value
or throw exception
	 *
	 * @param key  The key to dereference
	 * @param safe Whether to throw an exception if the key is not found
	 *
	 * @return The requested object
	 */
	@Override
	public Object dereference( IBoxContext context
Key key
Boolean safe ) {
		// Special check for $bx
		if ( key.equals( BoxMeta.key ) ) {
			return getBoxMeta();
		}

		if ( key.equals( Key.script_name ) ) {
			return exchange.getRequestPath();
		}
		// TODO: All other CGI keys
/* 
auth_password
auth_typeauth_user
cert_cookie
cert_flags
cert_issuer
cert_keysize
cert_secretkeysize
cert_serialnumber
cert_server_issuer
cert_server_subject
cert_subject
cf_template_path
content_length
content_type
context_path
gateway_interface
http_accept
http_accept_encoding
http_accept_language
http_connection
http_cookie
http_host
http_referer
http_user_agent
https_keysize
https_secretkeysize
https_server_issuer
https_server_subject
https,
local_addr
local_host 
path_info
path_translated
query_string
remote_addr
remote_host
remote_user
request_method
request_url
script_name
server_name
server_port
server_port_secure
server_protocol
server_software
web_server_api
 */
		// HTTP header fallbacks
		HeaderValues header = exchange.getRequestHeaders().get( key.getName() );
		if ( header != null ) {
			return header.getFirst();
		}

		// CGI scope NEVER errors. It simply returns empty string if the key is not found
		return "";
	}
}
