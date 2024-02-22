package ortus.boxlang.runtime.runnables.compiler;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class SourceMap {

	public SourceMapRecord[]	sourceMapRecords;
	public String				source;
	private static Pattern		isTemplate;

	static {
		isTemplate = Pattern.compile( "(.cfs|.cfm|.bx|.bxs)$" );
	}

	public static class SourceMapRecord {

		public int		originSourceLineStart;
		public int		originSourceLineEnd;
		public int		javaSourceLineStart;
		public int		javaSourceLineEnd;
		public String	originSourceNode;
		public String	javaSourceNode;
		public String	javaSourceClassName;
	}

	public int convertSourceLineToJavaLine( int sourceLine ) {
		int	result				= -1;
		int	endOfClosestWrapper	= -1;
		for ( SourceMapRecord sourceMapRecord : sourceMapRecords ) {
			// Move our pointer so long as we haven't passed the source line
			if ( sourceMapRecord.originSourceLineStart <= sourceLine && sourceMapRecord.originSourceLineEnd >= sourceLine ) {
				result				= sourceMapRecord.javaSourceLineStart;
				endOfClosestWrapper	= sourceMapRecord.javaSourceLineEnd;
			} else if ( result > -1 && result > endOfClosestWrapper ) {
				// If we don't match, but we matched before, then we're past the previous correct result
				break;
			}
		}
		return result;
	}

	public int convertJavaLineToSourceLine( int javaLine ) {
		int						result					= -1;
		int						endOfClosestWrapper		= -1;
		List<SourceMapRecord>	sortedSourceMapRecords	= Arrays.asList( sourceMapRecords );
		// Sort sortedSourceMapRecords by javaSourceLine asc
		sortedSourceMapRecords.sort( ( a, b ) -> a.javaSourceLineStart - b.javaSourceLineStart );
		for ( SourceMapRecord sourceMapRecord : sortedSourceMapRecords ) {
			// Move our pointer so long as we haven't passed the source line
			if ( sourceMapRecord.javaSourceLineStart <= javaLine && sourceMapRecord.javaSourceLineEnd >= javaLine ) {
				result				= sourceMapRecord.originSourceLineStart;
				endOfClosestWrapper	= sourceMapRecord.originSourceLineEnd;
			} else if ( result > -1 && result > endOfClosestWrapper ) {
				// If we don't match, but we matched before, then we're past the previous correct result
				break;
			}

		}
		return result;
	}

	public boolean isTemplate() {
		return isTemplate.matcher( source ).find();
	}

	public String getFileName() {
		return Path.of( source ).getFileName().toString();
	}

	public String getSource() {
		return source;
	}
}
