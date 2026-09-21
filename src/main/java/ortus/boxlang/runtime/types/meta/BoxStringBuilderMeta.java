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
package ortus.boxlang.runtime.types.meta;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.BoxStringBuilder;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * Metadata for the {@link BoxStringBuilder} type.
 */
public class BoxStringBuilderMeta extends BoxMeta<BoxStringBuilder> {

	@SuppressWarnings( "unused" )
	private BoxStringBuilder	target;
	public Class<?>				$class;
	public IStruct				meta;

	public BoxStringBuilderMeta( BoxStringBuilder target ) {
		super();
		this.target	= target;
		this.$class	= target.getClass();
		Struct metaStruct = new Struct();
		metaStruct.put( Key.type, "StringBuilder" );
		this.meta = metaStruct;
	}

	@Override
	public BoxStringBuilder getTarget() {
		return this.target;
	}

	@Override
	public IStruct getMeta() {
		return this.meta;
	}

}