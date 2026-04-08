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
package ortus.boxlang.runtime.dynamic.casters;

import java.io.File;
import java.nio.file.Path;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.BoxFile;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;
import ortus.boxlang.runtime.util.FileSystemUtil;

/**
 * I handle casting values to a BoxFile without opening the file.
 * Accepts a String, java.nio.file.Path, java.io.File, or BoxFile.
 * When a new BoxFile is created by this caster, the {@code implicitlyCast} flag is set to {@code true}
 * so BIFs know to close it after use.
 */
public class BoxFileCaster implements IBoxCaster {

	/**
	 * Tests to see if the value can be cast to a BoxFile.
	 * Returns a {@code CastAttempt<BoxFile>} which will contain the result if casting was
	 * successful, or can be interrogated to proceed otherwise.
	 *
	 * @param context The context for path expansion
	 * @param object  The value to cast to a BoxFile
	 *
	 * @return A CastAttempt containing the BoxFile if successful
	 */
	public static CastAttempt<BoxFile> attempt( IBoxContext context, Object object ) {
		return CastAttempt.ofNullable( cast( context, object, false ) );
	}

	/**
	 * Used to cast a value to a BoxFile, throwing an exception if we fail.
	 *
	 * @param context The context for path expansion
	 * @param object  The value to cast to a BoxFile
	 *
	 * @return The BoxFile value
	 */
	public static BoxFile cast( IBoxContext context, Object object ) {
		return cast( context, object, true );
	}

	/**
	 * Used to cast a value to a BoxFile.
	 *
	 * @param context The context for path expansion
	 * @param object  The value to cast to a BoxFile
	 * @param fail    True to throw exception when failing
	 *
	 * @return The BoxFile value, or null if fail is false and casting is not possible
	 */
	public static BoxFile cast( IBoxContext context, Object object, boolean fail ) {
		if ( object == null ) {
			if ( fail ) {
				throw new BoxCastException( "Can't cast null to a BoxFile." );
			} else {
				return null;
			}
		}

		object = DynamicObject.unWrap( object );

		if ( object instanceof BoxFile boxFile ) {
			return boxFile;
		}

		BoxFile result;

		if ( object instanceof Path path ) {
			result					= new BoxFile( path );
			result.implicitlyCast	= true;
			return result;
		}

		if ( object instanceof File file ) {
			result					= new BoxFile( file.toPath() );
			result.implicitlyCast	= true;
			return result;
		}

		if ( object instanceof String str ) {
			result					= new BoxFile( FileSystemUtil.expandPath( context, str ).absolutePath() );
			result.implicitlyCast	= true;
			return result;
		}

		if ( fail ) {
			throw new BoxCastException(
			    "Can't cast [" + object.getClass().getName() + "] to a BoxFile. Supported types: String, Path, File, BoxFile."
			);
		}

		return null;
	}
}
