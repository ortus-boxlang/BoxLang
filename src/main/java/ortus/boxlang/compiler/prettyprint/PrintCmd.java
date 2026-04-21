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
package ortus.boxlang.compiler.prettyprint;

public class PrintCmd {

	public enum Mode {
		FLAT, BREAK
	}

	private Mode	mode;
	private int		indent;
	private Object	content; // content can be a String or a List or a Doc

	public PrintCmd( Mode mode, int indent, Object content ) {
		this.indent		= indent;
		this.mode		= mode;
		this.content	= content;
	}

	public Mode getMode() {
		return mode;
	}

	public int getIndent() {
		return indent;
	}

	public Object getContent() {
		return content;
	}
}
