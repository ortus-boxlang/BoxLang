/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ourtus.boxlang.transpiler.transformer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.Statement;
import org.apache.commons.text.StringSubstitutor;
import ourtus.boxlang.ast.BoxNode;

import java.util.Map;

public abstract class AbstractTransformer implements Transformer {
	protected static JavaParser javaParser= new JavaParser();
	@Override
	public abstract Node transform(BoxNode node) throws IllegalStateException;
	protected Node parseExpression(String template, Map<String,String> values) {
		StringSubstitutor sub = new StringSubstitutor(values);
		String code = sub.replace(template);
		ParseResult<Expression> result = javaParser.parseExpression(code);
		if(!result.isSuccessful()) {
			throw new IllegalStateException(result.toString());
		}
		return result.getResult().get();
	}

	protected Node parseStatement(String template, Map<String,String> values) {
		StringSubstitutor sub = new StringSubstitutor(values);
		String code = sub.replace(template);
		ParseResult<Statement> result = javaParser.parseStatement(code);
		if(!result.isSuccessful()) {
			throw new IllegalStateException(result.toString());
		}
		return result.getResult().get();
	}
}
