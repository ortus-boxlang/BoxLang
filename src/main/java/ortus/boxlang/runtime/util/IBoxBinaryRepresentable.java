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

/**
 * This allows any Box object to be represnted as a byte array. Used for BoxImage or BoxFile, etc so toBinary() and toBase64() knows how to use them.
 */
public interface IBoxBinaryRepresentable {

	/**
	 * Get the byte array representation of this object.
	 * 
	 * @return The byte array representation of this object.
	 */
	public byte[] toByteArray();

}
