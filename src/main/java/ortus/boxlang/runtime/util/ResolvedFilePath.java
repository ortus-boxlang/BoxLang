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

package ortus.boxlang.runtime.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public record ResolvedFilePath( String mappingName, String mappingPath, String relativePath, Path absolutePath ) {

	public static ResolvedFilePath of( String mappingName, String mappingPath, String relativePath, Path absolutePath ) {
		return new ResolvedFilePath(
		    mappingName,
		    mappingPath,
		    relativePath,
		    absolutePath
		);
	}

	public static ResolvedFilePath of( String mappingName, String mappingPath, String relativePath, String absolutePath ) {
		return ResolvedFilePath.of(
		    mappingName,
		    mappingPath,
		    relativePath,
		    Path.of( absolutePath )
		);
	}

	public static ResolvedFilePath of( Path absolutePath ) {
		return new ResolvedFilePath(
		    null,
		    null,
		    absolutePath.toString(),
		    absolutePath
		);
	}

	public static ResolvedFilePath of( String absolutePath ) {
		return ResolvedFilePath.of( Path.of( absolutePath ) );
	}

	public boolean resovledViaMapping() {
		return mappingName != null;
	}

	public FQN getPackage() {
		return FQN.of( relativePath != null ? Path.of( relativePath ) : absolutePath );
	}

	public FQN getPackage( String prefix ) {
		return FQN.of( prefix, relativePath != null ? Path.of( relativePath ) : absolutePath );
	}

	public ResolvedFilePath newFromRelative( String relativePath ) {
		return ResolvedFilePath.of(
		    mappingName,
		    mappingPath,
		    Paths.get( relativePath() ).resolveSibling( relativePath ).toString(),
		    absolutePath.getParent().resolve( relativePath )
		);
	}

}
