/**
 * [BoxLang]
 * <p>
 * Copyright [2023] [Ortus Solutions, Corp]
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.executor;

import javax.tools.*;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Hashtable;
import java.util.Map;

/**
 * In Memory File Manager
 */
public class JavaMemoryManager extends ForwardingJavaFileManager<JavaFileManager> {

    private final Map<String, JavaClassByteCode> compiledClasses;

    public JavaMemoryManager( StandardJavaFileManager standardFileManager ) {
        super( standardFileManager );
        this.compiledClasses = new Hashtable<>();
    }

    @Override
    public JavaFileObject getJavaFileForOutput( Location location, String className, JavaFileObject.Kind kind,
        FileObject sibling ) {

        JavaClassByteCode classAsBytes = new JavaClassByteCode(
            className, kind );
        compiledClasses.put( className, classAsBytes );

        return classAsBytes;
    }

    public Map<String, JavaClassByteCode> getBytesMap() {
        return compiledClasses;
    }
}
