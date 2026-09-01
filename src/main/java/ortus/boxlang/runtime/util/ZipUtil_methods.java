// ============================================================================
// Methods to ADD to the existing ZipUtil class
// (ortus.boxlang.runtime.util.ZipUtil)
//
// The ZipUtil class already exists in the BoxLang codebase. You need to add
// these three static methods to it. Do NOT create a new ZipUtil class.
// ============================================================================

/**
 * Compresses a file or directory into a ZIP archive.
 *
 * <p>
 * If the source is a directory, it recursively walks the directory tree
 * and preserves relative paths inside the ZIP file. If the source is a
 * single file, it creates a ZIP with just that file.
 * </p>
 *
 * @param source      Absolute path to the source file or directory
 * @param destination Absolute path to the output ZIP file
 *
 * @throws BoxRuntimeException if the source does not exist or compression fails
 */
public static void compress( String source, String destination ) {
    Path sourcePath = Paths.get( source );
    Path zipPath    = Paths.get( destination );

    if ( !Files.exists( sourcePath ) ) {
        throw new BoxRuntimeException( "Source path does not exist: " + source );
    }

    try ( ZipOutputStream zos = new ZipOutputStream( new FileOutputStream( zipPath.toFile() ) ) ) {

        if ( Files.isDirectory( sourcePath ) ) {
            try ( var paths = Files.walk( sourcePath ) ) {
                paths
                    .filter( path -> !Files.isDirectory( path ) )
                    .forEach( path -> {
                        ZipEntry zipEntry = new ZipEntry( sourcePath.relativize( path ).toString() );
                        try {
                            zos.putNextEntry( zipEntry );
                            Files.copy( path, zos );
                            zos.closeEntry();
                        } catch ( IOException e ) {
                            throw new BoxRuntimeException( "Error compressing file: " + path, e );
                        }
                    } );
            }
        } else {
            ZipEntry zipEntry = new ZipEntry( sourcePath.getFileName().toString() );
            zos.putNextEntry( zipEntry );
            Files.copy( sourcePath, zos );
            zos.closeEntry();
        }

    } catch ( IOException e ) {
        throw new BoxRuntimeException( "Compression failed", e );
    }
}

/**
 * Extracts a ZIP archive into the specified destination directory.
 *
 * <p>
 * Includes protection against ZIP Slip vulnerability by normalizing
 * resolved paths and verifying they remain within the destination directory.
 * </p>
 *
 * @param source      Absolute path to the ZIP file
 * @param destination Absolute path to the extraction directory
 *
 * @throws BoxRuntimeException if extraction fails or a ZIP Slip attack is detected
 */
public static void extract( String source, String destination ) {
    Path zipPath = Paths.get( source );
    Path destDir = Paths.get( destination );

    try ( ZipInputStream zis = new ZipInputStream( new FileInputStream( zipPath.toFile() ) ) ) {
        ZipEntry entry;

        while ( ( entry = zis.getNextEntry() ) != null ) {
            Path newFile = destDir.resolve( entry.getName() ).normalize();

            // Prevent ZIP Slip vulnerability
            if ( !newFile.startsWith( destDir ) ) {
                throw new BoxRuntimeException(
                    "Invalid ZIP entry (possible ZIP Slip attack): " + entry.getName()
                );
            }

            if ( entry.isDirectory() ) {
                Files.createDirectories( newFile );
            } else {
                Files.createDirectories( newFile.getParent() );
                try ( FileOutputStream fos = new FileOutputStream( newFile.toFile() ) ) {
                    zis.transferTo( fos );
                }
            }

            zis.closeEntry();
        }

    } catch ( IOException e ) {
        throw new BoxRuntimeException( "Extraction failed", e );
    }
}

/**
 * Lists the contents of a ZIP archive without extracting it.
 *
 * @param source Absolute path to the ZIP file
 *
 * @return List of entry names inside the ZIP archive
 *
 * @throws BoxRuntimeException if listing fails
 */
public static List<String> list( String source ) {
    List<String> entries = new ArrayList<>();

    try ( java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile( source ) ) {
        zipFile.stream()
            .forEach( entry -> entries.add( entry.getName() ) );
    } catch ( IOException e ) {
        throw new BoxRuntimeException( "Failed to list zip contents", e );
    }

    return entries;
}
