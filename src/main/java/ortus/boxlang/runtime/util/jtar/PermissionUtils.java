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
 *
 * Vendored and adapted from https://github.com/xiaoxindada/jtar.
 * Original JTar copyright 2012 Kamran Zafar; Apache License 2.0.
 */
package ortus.boxlang.runtime.util.jtar;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Map;
import java.util.Set;

/** Resolves portable TAR permission modes from source files. */
public final class PermissionUtils {

	private static final Map<PosixFilePermission, Integer> POSIX_MODES = Map.of(
	    PosixFilePermission.OWNER_READ, 0400,
	    PosixFilePermission.OWNER_WRITE, 0200,
	    PosixFilePermission.OWNER_EXECUTE, 0100,
	    PosixFilePermission.GROUP_READ, 0040,
	    PosixFilePermission.GROUP_WRITE, 0020,
	    PosixFilePermission.GROUP_EXECUTE, 0010,
	    PosixFilePermission.OTHERS_READ, 0004,
	    PosixFilePermission.OTHERS_WRITE, 0002,
	    PosixFilePermission.OTHERS_EXECUTE, 0001
	);

	private PermissionUtils() {
	}

	/**
	 * Returns the source mode, preserving POSIX permissions when available and using a safe fallback otherwise.
	 *
	 * @param file The source file
	 *
	 * @return The TAR permission mode
	 */
	public static int permissions( File file ) {
		if ( Files.getFileAttributeView( file.toPath(), java.nio.file.attribute.PosixFileAttributeView.class ) != null ) {
			try {
				Set<PosixFilePermission> permissions = Files.getPosixFilePermissions( file.toPath() );
				return permissions.stream().mapToInt( permission -> POSIX_MODES.get( permission ) ).sum();
			} catch ( Exception ignored ) {
				// Fall through to the portable Java File fallback.
			}
		}
		int mode = file.isDirectory() ? 0700 : 0600;
		if ( file.canRead() )
			mode |= file.isDirectory() ? 0100 : 0400;
		if ( file.canWrite() )
			mode |= file.isDirectory() ? 0100 : 0200;
		if ( file.canExecute() )
			mode |= 0111;
		return mode;
	}
}
