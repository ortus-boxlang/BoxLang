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
package ortus.boxlang.runtime.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

public class NetworkUtil {

	/**
	 * Get the local host
	 *
	 * @return InetAddress
	 */
	public static InetAddress getLocalHost() throws UnknownHostException {
		return InetAddress.getLocalHost();
	}

	/**
	 * Get the local IP address of the machine
	 */
	public static String getLocalIPAddress() {
		try {
			return getLocalHost().getHostAddress();
		} catch ( UnknownHostException e ) {
			return "127.0.0.1";
		}
	}

	/**
	 * Get the local hostname of the machine
	 */
	public static String getLocalHostname() {
		try {
			return getLocalHost().getHostName();
		} catch ( UnknownHostException e ) {
			return "localhost";
		}
	}

	/**
	 * Get the local mac address of the machine
	 *
	 * @return The mac address of the machine or empty if not found
	 */
	public static String getLocalMacAddress() {
		NetworkInterface network;
		try {
			network = NetworkInterface.getByInetAddress( getLocalHost() );
		} catch ( SocketException | UnknownHostException e ) {
			return "";
		}

		if ( network == null ) {
			return "";
		}

		byte[] macAddress;
		try {
			macAddress = network.getHardwareAddress();
		} catch ( SocketException e ) {
			return "";
		}

		if ( macAddress == null ) {
			return "";
		}

		// Convert the mac address to a string
		StringBuilder macAddressString = new StringBuilder();
		for ( int i = 0; i < macAddress.length; i++ ) {
			macAddressString.append( String.format( "%02X%s", macAddress[ i ], ( i < macAddress.length - 1 ) ? "-" : "" ) );
		}

		return macAddressString.toString();
	}

}
