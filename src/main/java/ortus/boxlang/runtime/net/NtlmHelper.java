package ortus.boxlang.runtime.net;

public class NtlmHelper {
	/*
	 * This is a placeholder for NTLM authentication. Not real NTLM
	 */

	public static String createType1( String domain, String workstation ) {
		return "TYPE1FAKE";
	}

	public static String createType3( String username, String password, String domain, String workstation, String challenge ) {
		return "TYPE3FAKE-" + challenge;
	}
}
