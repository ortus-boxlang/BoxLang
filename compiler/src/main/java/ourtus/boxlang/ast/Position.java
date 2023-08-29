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
package ourtus.boxlang.ast;

public class Position {
	private Point start;
	private Point end;
	private Source source;
	public Position(Point start, Point end) {
		this.start = start;
		this.end = end;
		this.source = null;
	}

	public Position(Point start, Point end, Source source) {
		this.start = start;
		this.end = end;
		this.source = source;
	}

	public Point getStart() {
		return start;
	}

	public Point getEnd() {
		return end;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}
}
