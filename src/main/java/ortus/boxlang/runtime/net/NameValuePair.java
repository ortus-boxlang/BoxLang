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
package ortus.boxlang.runtime.net;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class NameValuePair {

	private final @NonNull String	name;
	private final @Nullable String	value;

	public NameValuePair( @NonNull String name, @Nullable String value ) {
		this.name	= name;
		this.value	= value;
	}

	public static NameValuePair fromNativeArray( String[] nameAndValue ) {
		if ( nameAndValue.length > 1 ) {
			return new NameValuePair( nameAndValue[ 0 ], nameAndValue[ 1 ] );
		}
		return new NameValuePair( nameAndValue[ 0 ], null );
	}

	@NonNull
	public String getName() {
		return name;
	}

	@Nullable
	public String getValue() {
		return value;
	}

	public String toString() {
		if ( this.value == null ) {
			return this.name;
		}
		return this.name + "=" + this.value;
	}

}
