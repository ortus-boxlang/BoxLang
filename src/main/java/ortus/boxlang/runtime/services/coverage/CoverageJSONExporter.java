/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http: //www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.services.coverage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.JSONUtil;

/**
 * Serializes {@link CoverageDataBuilder} output to JSON files (or strings),
 * using the native BoxLang {@link Struct}/{@link Array} structures so the data
 * is directly usable in BoxLang before serialization.
 */
public final class CoverageJSONExporter {

	/**
	 * No construction - static utility.
	 */
	private CoverageJSONExporter() {
	}

	/**
	 * Serialize a coverage {@link IStruct} to a pretty-printed JSON string.
	 *
	 * @param data the coverage data (from {@link CoverageDataBuilder})
	 *
	 * @return the JSON string
	 */
	public static String toJSON( IStruct data ) {
		try {
			return JSONUtil.getJSONBuilder( true ).asString( data );
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Failed to serialize coverage JSON", e );
		}
	}

	/**
	 * Serialize a coverage {@link IStruct} to a JSON string.
	 *
	 * @param data   the coverage data (from {@link CoverageDataBuilder})
	 * @param pretty whether to pretty-print
	 *
	 * @return the JSON string
	 */
	public static String toJSON( IStruct data, boolean pretty ) {
		try {
			return JSONUtil.getJSONBuilder( pretty ).asString( data );
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Failed to serialize coverage JSON", e );
		}
	}

	/**
	 * Write coverage data to a JSON file, creating parent directories as needed.
	 *
	 * @param path   the output file path
	 * @param data   the coverage data (from {@link CoverageDataBuilder})
	 * @param pretty whether to pretty-print
	 *
	 * @return the JSON string written
	 */
	public static String write( String path, IStruct data, boolean pretty ) {
		String json = toJSON( data, pretty );
		writeString( path, json );
		return json;
	}

	/**
	 * Write coverage data to a pretty-printed JSON file.
	 *
	 * @param path the output file path
	 * @param data the coverage data (from {@link CoverageDataBuilder})
	 *
	 * @return the JSON string written
	 */
	public static String write( String path, IStruct data ) {
		return write( path, data, true );
	}

	/**
	 * Write a raw string to a file, creating parent directories as needed.
	 *
	 * @param path the output file path
	 * @param data the file content
	 */
	private static void writeString( String path, String data ) {
		try {
			Path	p		= Paths.get( path ).toAbsolutePath().normalize();
			Path	parent	= p.getParent();
			if ( parent != null ) {
				Files.createDirectories( parent );
			}
			Files.writeString( p, data );
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Failed to write coverage JSON to [" + path + "]", e );
		}
	}
}
