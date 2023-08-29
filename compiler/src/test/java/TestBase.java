import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestBase {

	protected String testboxDirectory = System.getProperty( "testboxdir" );
	protected String contentboxDirectory = System.getProperty( "contentboxdir" );
	protected String coldboxDirectory = System.getProperty( "coldboxdir" );

	protected List<Path> scanForFiles( String path, Set<String> extensions ) {
		List<Path> fileList = new ArrayList<Path>();

		try {
			Files.walkFileTree( Paths.get( path ), new HashSet<>(), Integer.MAX_VALUE, new FileVisitor<>() {

				private boolean match( String fileName ) {
					int index = fileName.lastIndexOf( '.' );
					if ( index > 0 ) {
						String ext = fileName.substring( index + 1 );
						return extensions.contains( ext );
					}
					return false;
				}

				@Override
				public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs ) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException {
					if ( match( file.getFileName().toString() ) )
						fileList.add( file );
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed( Path file, IOException exc ) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory( Path dir, IOException exc ) throws IOException {
					return FileVisitResult.CONTINUE;
				}
			} );
		} catch ( Exception e ) {

		}
		return fileList;
	}

}
