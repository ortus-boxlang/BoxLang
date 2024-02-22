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
package ortus.boxlang.transpiler;

import com.github.javaparser.ast.Node;
import com.github.javaparser.printer.DefaultPrettyPrinter;
import com.github.javaparser.printer.configuration.DefaultConfigurationOption;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration.ConfigOption;
import com.github.javaparser.printer.configuration.Indentation;
import com.github.javaparser.printer.configuration.Indentation.IndentType;

public class CustomPrettyPrinter extends DefaultPrettyPrinter {

	private final PrettyPrintVisitor visitor;

	public CustomPrettyPrinter() {
		super();

		this.visitor = new PrettyPrintVisitor( new DefaultPrinterConfiguration()
		    .addOption( new DefaultConfigurationOption( ConfigOption.COLUMN_ALIGN_FIRST_METHOD_CHAIN ) )
		    .addOption( new DefaultConfigurationOption( ConfigOption.COLUMN_ALIGN_PARAMETERS ) )
		    .addOption( new DefaultConfigurationOption( ConfigOption.INDENTATION, new Indentation( IndentType.SPACES, 1 ) ) )
		);

	}

	@Override
	public String print( Node node ) {
		node.accept( visitor, null );
		return visitor.toString();
	}

	/**
	 * Get visitor
	 */
	public PrettyPrintVisitor getVisitor() {
		return visitor;
	}
}