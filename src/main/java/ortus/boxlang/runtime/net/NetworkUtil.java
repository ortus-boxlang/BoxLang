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
package ortus.boxlang.runtime.net;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * NetworkUtil is a utility class for network related functions such as getting the local IP address, hostname, and mac address.
 */
public class NetworkUtil {

	private static volatile String[]	localIpsCache;

	private static final Pattern	IPV4_PATTERN	= Pattern.compile(
	    "^(?:(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$"
	);

	private static final Pattern	IPV6_PATTERN	= Pattern.compile(
	    "^(?:"
	        + "(?:[0-9A-Fa-f]{1,4}:){7}[0-9A-Fa-f]{1,4}|"
	        + "(?:[0-9A-Fa-f]{1,4}:){1,7}:|"
	        + "(?:[0-9A-Fa-f]{1,4}:){1,6}:[0-9A-Fa-f]{1,4}|"
	        + "(?:[0-9A-Fa-f]{1,4}:){1,5}(?::[0-9A-Fa-f]{1,4}){1,2}|"
	        + "(?:[0-9A-Fa-f]{1,4}:){1,4}(?::[0-9A-Fa-f]{1,4}){1,3}|"
	        + "(?:[0-9A-Fa-f]{1,4}:){1,3}(?::[0-9A-Fa-f]{1,4}){1,4}|"
	        + "(?:[0-9A-Fa-f]{1,4}:){1,2}(?::[0-9A-Fa-f]{1,4}){1,5}|"
	        + "[0-9A-Fa-f]{1,4}:(?:(?::[0-9A-Fa-f]{1,4}){1,6})|"
	        + ":(?:(?::[0-9A-Fa-f]{1,4}){1,7}|:)"
	        + ")$"
	);

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
	 * Get all local IP addresses (IPv4 and IPv6) for the machine.
	 *
	 * Results are lazily cached and can be refreshed by passing true.
	 *
	 * @param refresh If true, forces a reload of local IP addresses.
	 *
	 * @return An array of local IP address literals.
	 */
	public static String[] getLocalIps( boolean refresh ) {
		if ( !refresh && localIpsCache != null ) {
			return localIpsCache.clone();
		}

		synchronized ( NetworkUtil.class ) {
			if ( !refresh && localIpsCache != null ) {
				return localIpsCache.clone();
			}

			Set<String> localIps = new LinkedHashSet<>();
			try {
				var interfaces = NetworkInterface.getNetworkInterfaces();
				if ( interfaces != null ) {
					while ( interfaces.hasMoreElements() ) {
						var networkInterface = interfaces.nextElement();
						var addresses = networkInterface.getInetAddresses();
						while ( addresses.hasMoreElements() ) {
							var hostAddress = addresses.nextElement().getHostAddress();
							if ( hostAddress == null || hostAddress.isBlank() ) {
								continue;
							}

							// Remove zone index suffix from IPv6 values like fe80::1%en0
							int zoneSeparator = hostAddress.indexOf( '%' );
							if ( zoneSeparator > 0 ) {
								hostAddress = hostAddress.substring( 0, zoneSeparator );
							}

							if ( isIpv4( hostAddress ) || isIpv6( hostAddress ) ) {
								localIps.add( hostAddress );
							}
						}
					}
				}
			} catch ( SocketException e ) {
				// Falls back to single local host address below.
			}

			if ( localIps.isEmpty() ) {
				localIps.add( getLocalIPAddress() );
			}

			localIpsCache = localIps.toArray( String[]::new );
			return localIpsCache.clone();
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

	/**
	 * Validate if a string is a literal IPv4 address.
	 *
	 * @param ip The value to test.
	 *
	 * @return true if the value is a valid IPv4 literal.
	 */
	public static boolean isIpv4( String ip ) {
		if ( ip == null || ip.isBlank() ) {
			return false;
		}

		return IPV4_PATTERN.matcher( ip ).matches();
	}

	/**
	 * Validate if a string is a literal IPv6 address.
	 *
	 * @param ip The value to test.
	 *
	 * @return true if the value is a valid IPv6 literal.
	 */
	public static boolean isIpv6( String ip ) {
		if ( ip == null || ip.isBlank() ) {
			return false;
		}

		return IPV6_PATTERN.matcher( ip ).matches();
	}

}
