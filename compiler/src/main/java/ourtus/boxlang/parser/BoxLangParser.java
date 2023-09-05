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
package ourtus.boxlang.parser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class BoxLangParser {

	public static BoxFileType detectFile(File file) {
		try {
			List<String> content = Files.readAllLines(file.toPath());
			if(content.stream().anyMatch(lines -> lines.contains("<cfcomponent") || lines.contains("<cfset") || lines.contains("<cfparam")
				|| lines.contains("<cfoutput") || lines.contains("<cfinterface")   )) {
				return BoxFileType.CFML;
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return BoxFileType.CF;

	}
	public ParsingResult parse(File file) throws IOException {
		BoxFileType fileType = detectFile(file);
		switch (fileType) {
			case CF -> {
				return new BoxCFParser().parse(file); }
			case CFML ->  {
				return new BoxCFMLParser().parse(file);
			}
			default -> {
				throw new RuntimeException("Unsupported file: " + file.getAbsolutePath());
			}
		}

	}
	public ParsingResult parse(String code,BoxFileType fileType) throws IOException {
		switch (fileType) {
			case CF -> {
				return new BoxCFParser().parse(code); }
			case CFML ->  {
				return new BoxCFMLParser().parse(code);
			}
			default -> {
				throw new RuntimeException("Unsupported language");
			}
		}

	}

	public ParsingResult parseExpression(String code) throws IOException {
		return new BoxCFParser().parseExpression(code);
	}

	public ParsingResult parseStatement(String code) throws IOException {
		return new BoxCFParser().parseStatement(code);
	}

}
