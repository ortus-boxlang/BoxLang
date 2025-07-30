package ortus.boxlang.runtime.bifs.global.jdbc;

public class DerbySleep {

	public static int sleep( int milliseconds ) {
		try {
			Thread.sleep( milliseconds );
		} catch ( InterruptedException e ) {
			// Handle interruption
		}
		return milliseconds;
	}

}
