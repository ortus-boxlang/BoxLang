/**
 * TestBox BDD spec for ortus.boxlang.runtime.util.ZipFile
 *
 * Uses createObject("java", ...) because ZipFile is a Java class,
 * not a CFML component, so the `new` keyword cannot instantiate it.
 *
 * Tests cover:
 *   - compress()  : single file and directory
 *   - extract()   : full extraction and nested directories
 *   - list()      : listing entries without extraction
 */
component extends="testbox.system.BaseSpec" {

    /**
     * Helper: returns a fresh ZipFile Java instance
     */
    private function newZip() {
        return createObject( "java", "ortus.boxlang.runtime.util.ZipFile" ).init();
    }

    function run() {

        describe( "ZipFile", function() {

            var tmpDir     = expandPath( "/tests/tmp/zipfile_test" );
            var zipPath    = tmpDir & "/test.zip";
            var extractDir = tmpDir & "/extracted";

            beforeEach( function() {
                if ( directoryExists( tmpDir ) ) {
                    directoryDelete( tmpDir, true );
                }
                directoryCreate( tmpDir );
                directoryCreate( extractDir );
            } );

            afterEach( function() {
                if ( directoryExists( tmpDir ) ) {
                    directoryDelete( tmpDir, true );
                }
            } );

            // ── compress() ────────────────────────────────────────────────

            describe( "compress()", function() {

                it( "should compress a single file into a ZIP archive", function() {
                    var srcFile = tmpDir & "/hello.txt";
                    fileWrite( srcFile, "Hello, BoxLang!" );

                    newZip().source( srcFile ).to( zipPath ).compress();

                    expect( fileExists( zipPath ) ).toBeTrue( "ZIP file should exist after compress()" );
                    expect( getFileInfo( zipPath ).size ).toBeGT( 0, "ZIP file should not be empty" );
                } );

                it( "should compress a directory recursively into a ZIP archive", function() {
                    var srcDir = tmpDir & "/mydir";
                    directoryCreate( srcDir & "/sub", true );
                    fileWrite( srcDir & "/root.txt",     "root content" );
                    fileWrite( srcDir & "/sub/deep.txt", "deep content" );

                    newZip().source( srcDir ).to( zipPath ).compress();

                    expect( fileExists( zipPath ) ).toBeTrue();

                    var entries = newZip().source( zipPath ).list();
                    expect( entries ).toInclude( "root.txt" );
                    expect( entries ).toInclude( "sub/deep.txt" );
                } );

                it( "should throw when the source path does not exist", function() {
                    expect( function() {
                        newZip()
                            .source( tmpDir & "/nonexistent.txt" )
                            .to( zipPath )
                            .compress();
                    } ).toThrow();
                } );

                it( "should throw when destination is not set", function() {
                    var srcFile = tmpDir & "/hello.txt";
                    fileWrite( srcFile, "data" );

                    expect( function() {
                        newZip().source( srcFile ).compress();
                    } ).toThrow();
                } );

            } );

            // ── extract() ─────────────────────────────────────────────────

            describe( "extract()", function() {

                it( "should extract a ZIP archive and restore file contents", function() {
                    var srcFile = tmpDir & "/hello.txt";
                    fileWrite( srcFile, "Hello, BoxLang!" );

                    newZip().source( srcFile ).to( zipPath ).compress();
                    newZip().source( zipPath ).to( extractDir ).extract();

                    expect( fileExists( extractDir & "/hello.txt" ) ).toBeTrue( "Extracted file should exist" );
                    expect( fileRead( extractDir & "/hello.txt" ) ).toBe( "Hello, BoxLang!" );
                } );

                it( "should recreate nested directory structure on extraction", function() {
                    var srcDir = tmpDir & "/nested";
                    directoryCreate( srcDir & "/a/b", true );
                    fileWrite( srcDir & "/a/b/deep.txt", "deep!" );

                    newZip().source( srcDir ).to( zipPath ).compress();
                    newZip().source( zipPath ).to( extractDir ).extract();

                    expect( fileExists( extractDir & "/a/b/deep.txt" ) ).toBeTrue();
                } );

                it( "should throw when the ZIP file does not exist", function() {
                    expect( function() {
                        newZip()
                            .source( tmpDir & "/missing.zip" )
                            .to( extractDir )
                            .extract();
                    } ).toThrow();
                } );

            } );

            // ── list() ────────────────────────────────────────────────────

            describe( "list()", function() {

                it( "should return all entry names from a ZIP archive", function() {
                    var srcDir = tmpDir & "/listme";
                    directoryCreate( srcDir );
                    fileWrite( srcDir & "/one.txt", "1" );
                    fileWrite( srcDir & "/two.txt", "2" );

                    newZip().source( srcDir ).to( zipPath ).compress();

                    var entries = newZip().source( zipPath ).list();

                    expect( entries ).toBeArray();
                    expect( entries ).toHaveLength( 2 );
                    expect( entries ).toInclude( "one.txt" );
                    expect( entries ).toInclude( "two.txt" );
                } );

                it( "should return an empty array for an empty ZIP archive", function() {
                    var zos = createObject( "java", "java.util.zip.ZipOutputStream" )
                                  .init( createObject( "java", "java.io.FileOutputStream" ).init( zipPath ) );
                    zos.close();

                    var entries = newZip().source( zipPath ).list();

                    expect( entries ).toBeArray();
                    expect( entries ).toHaveLength( 0 );
                } );

                it( "should throw when source is not set", function() {
                    expect( function() {
                        newZip().list();
                    } ).toThrow();
                } );

            } );

        } ); // end describe ZipFile

    } // end run()

}
