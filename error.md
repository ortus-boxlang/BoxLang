franciscosierramunoz@Franciscos-MacBook-Air-4 BoxLang % box testbox run "bundles=tests.specs.ZipFileTest"
Executing tests http://127.0.0.1:59508/tests/runner.cfm?&directory=&recurse=true&reporter=json&bundles=tests.specs.ZipFileTest&verbose=true please wait...

!! tests.specs.ZipFileTest (65 ms)
[Passed: 4] [Failed: 0] [Errors: 6] [Skipped: 0] [Suites/Specs: 4/10]

    !! ZipFile 
        !! compress() 
            !! should compress a single file into a ZIP archive (19 ms) 
                -> Error: cannot load class through its string name, because no definition for the class with the specified name [ortus.boxlang.runtime.util.ZipFile] could be found caused by (java.lang.ClassNotFoundException:ortus.boxlang.runtime.util.ZipFile not found by lucee.core [40];java.lang.ClassNotFoundException:ortus.boxlang.runtime.util.ZipFile;)                                                                                                                                                                          
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/tests/specs/ZipFileTest.cfc:18 

                16:      */
                17:     private function newZip() {
                18:         return createObject( "java", "ortus.boxlang.runtime.util.ZipFile" ).init();
                19:     }
                20: 
                
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/tests/specs/ZipFileTest.cfc:51 
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/testbox/system/BaseSpec.cfc:1743 
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/testbox/system/BaseSpec.cfc:1293 
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/testbox/system/BaseSpec.cfc:1743 
            !! should compress a directory recursively into a ZIP archive (10 ms) 
                -> Error: cannot load class through its string name, because no definition for the class with the specified name [ortus.boxlang.runtime.util.ZipFile] could be found caused by (java.lang.ClassNotFoundException:ortus.boxlang.runtime.util.ZipFile;java.lang.ClassNotFoundException:ortus.boxlang.runtime.util.ZipFile not found by lucee.core [40];)                                                                                                                                                                          
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/tests/specs/ZipFileTest.cfc:18 

                16:      */
                17:     private function newZip() {
                18:         return createObject( "java", "ortus.boxlang.runtime.util.ZipFile" ).init();
                19:     }
                20: 
                
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/tests/specs/ZipFileTest.cfc:63 
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/testbox/system/BaseSpec.cfc:1743 
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/testbox/system/BaseSpec.cfc:1293 
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/testbox/system/BaseSpec.cfc:1743 
            √ should throw when the source path does not exist (5 ms) 
            √ should throw when destination is not set (1 ms) 
        !! extract() 
            !! should extract a ZIP archive and restore file contents (6 ms) 
                -> Error: cannot load class through its string name, because no definition for the class with the specified name [ortus.boxlang.runtime.util.ZipFile] could be found caused by (java.lang.ClassNotFoundException:ortus.boxlang.runtime.util.ZipFile not found by lucee.core [40];java.lang.ClassNotFoundException:ortus.boxlang.runtime.util.ZipFile;)                                                                                                                                                                          
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/tests/specs/ZipFileTest.cfc:18 

                16:      */
                17:     private function newZip() {
                18:         return createObject( "java", "ortus.boxlang.runtime.util.ZipFile" ).init();
                19:     }
                20: 
                
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/tests/specs/ZipFileTest.cfc:100 
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/testbox/system/BaseSpec.cfc:1743 
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/testbox/system/BaseSpec.cfc:1293 
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/testbox/system/BaseSpec.cfc:1743 
            !! should recreate nested directory structure on extraction (6 ms) 
                -> Error: cannot load class through its string name, because no definition for the class with the specified name [ortus.boxlang.runtime.util.ZipFile] could be found caused by (java.lang.ClassNotFoundException:ortus.boxlang.runtime.util.ZipFile;java.lang.ClassNotFoundException:ortus.boxlang.runtime.util.ZipFile not found by lucee.core [40];)                                                                                                                                                                          
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/tests/specs/ZipFileTest.cfc:18 

                16:      */
                17:     private function newZip() {
                18:         return createObject( "java", "ortus.boxlang.runtime.util.ZipFile" ).init();
                19:     }
                20: 
                
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/tests/specs/ZipFileTest.cfc:112 
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/testbox/system/BaseSpec.cfc:1743 
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/testbox/system/BaseSpec.cfc:1293 
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/testbox/system/BaseSpec.cfc:1743 
            √ should throw when the ZIP file does not exist (2 ms) 
        !! list() 
            !! should return all entry names from a ZIP archive (5 ms) 
                -> Error: cannot load class through its string name, because no definition for the class with the specified name [ortus.boxlang.runtime.util.ZipFile] could be found caused by (java.lang.ClassNotFoundException:ortus.boxlang.runtime.util.ZipFile;java.lang.ClassNotFoundException:ortus.boxlang.runtime.util.ZipFile not found by lucee.core [40];)                                                                                                                                                                          
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/tests/specs/ZipFileTest.cfc:18 

                16:      */
                17:     private function newZip() {
                18:         return createObject( "java", "ortus.boxlang.runtime.util.ZipFile" ).init();
                19:     }
                20: 
                
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/tests/specs/ZipFileTest.cfc:139 
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/testbox/system/BaseSpec.cfc:1743 
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/testbox/system/BaseSpec.cfc:1293 
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/testbox/system/BaseSpec.cfc:1743 
            !! should return an empty array for an empty ZIP archive (5 ms) 
                -> Error: cannot load class through its string name, because no definition for the class with the specified name [ortus.boxlang.runtime.util.ZipFile] could be found caused by (java.lang.ClassNotFoundException:ortus.boxlang.runtime.util.ZipFile not found by lucee.core [40];java.lang.ClassNotFoundException:ortus.boxlang.runtime.util.ZipFile;)                                                                                                                                                                          
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/tests/specs/ZipFileTest.cfc:18 

                16:      */
                17:     private function newZip() {
                18:         return createObject( "java", "ortus.boxlang.runtime.util.ZipFile" ).init();
                19:     }
                20: 
                
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/tests/specs/ZipFileTest.cfc:154 
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/testbox/system/BaseSpec.cfc:1743 
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/testbox/system/BaseSpec.cfc:1293 
                -> at /Users/franciscosierramunoz/Desktop/FIU/CIS_3590_Internship_Ready_Software_Development/BoxLang/BoxLang/testbox/system/BaseSpec.cfc:1743 
            √ should throw when source is not set (1 ms) 

╔═════════════════════════════════════════════════════════════════════╗
║ Passed  ║ Failed  ║ Errored ║ Skipped ║ Bundles ║ Suites  ║ Specs   ║
╠═════════════════════════════════════════════════════════════════════╣
║ 4       ║ 0       ║ 6       ║ 0       ║ 1       ║ 4       ║ 10      ║
╚═════════════════════════════════════════════════════════════════════╝

TestBox         v6.5.0
CFML Engine     Lucee v5.4.8.2
Duration        86ms
Labels          ---

√ Passed  - Skipped  !! Exception/Error  X Failure