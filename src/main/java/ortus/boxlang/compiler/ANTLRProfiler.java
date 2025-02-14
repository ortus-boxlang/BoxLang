package ortus.boxlang.compiler;

import java.io.File;
import java.io.IOException;

import ortus.boxlang.compiler.parser.CFParser;
import ortus.boxlang.compiler.parser.ParsingResult;

public class ANTLRProfiler {

	public static void main( String[] args ) {
		CFParser	parser		= new CFParser();
		long		startTime	= System.currentTimeMillis();

		try {
			parser.setDebugMode( true );
			ParsingResult res = parser.parse( new File( args[ 0 ] ), true );
			parser.getProfilingResults().writeCSV( args[ 1 ] );
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long	stopTime	= System.currentTimeMillis();
		long	totalTime	= ( stopTime - startTime ) / 1000;

		System.out.println( "Time taken: " + totalTime );
	}
}
