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
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ortus.boxlang.runtime.services.Blueprint;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Exports per-line coverage as SonarQube <a href=
 * "https://docs.sonarsource.com/sonarqube/latest/analyzing-source-code/test-coverage/generic-test-data/">Generic
 * Coverage</a> XML (the same format TestBox writes via
 * {@code testbox.system.coverage.sonarQube.SonarQube}).
 * <p>
 * Output shape:
 * 
 * <pre>
 * &lt;coverage version="1"&gt;
 *   &lt;file path="/abs/path/to/file.bx"&gt;
 *     &lt;lineToCover lineNumber="8" covered="true"/&gt;
 *     &lt;lineToCover lineNumber="22" covered="false"/&gt;
 *   &lt;/file&gt;
 * &lt;/coverage&gt;
 * </pre>
 *
 * Only executable lines that were touched by a span are emitted (lines without an
 * executable span are not part of coverage).
 */
public final class CoverageSonarQubeExporter {

	/**
	 * No construction - static utility.
	 */
	private CoverageSonarQubeExporter() {
	}

	/**
	 * Generate the SonarQube Generic Coverage XML for the per-line coverage data
	 * in the given {@link Struct}, which is assumed to be the output of
	 * {@link CoverageDataBuilder#buildLineCoverage(Blueprint.Kind...)}.
	 *
	 * @param lineCoverage the per-line coverage data
	 *
	 * @return the XML string
	 */
	public static String generateXML( IStruct lineCoverage ) {
		try {
			Document	doc			= DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element		coverage	= doc.createElement( "coverage" );
			coverage.setAttribute( "version", "1" );
			doc.appendChild( coverage );

			lineCoverage.forEach( ( key, value ) -> {
				IStruct	fileData	= ( IStruct ) value;
				Element	fileNode	= doc.createElement( "file" );
				fileNode.setAttribute( "path", fileData.get( "filePath" ).toString() );
				IStruct lines = ( IStruct ) fileData.get( "lines" );
				lines.forEach( ( lineKey, lineVal ) -> {
					IStruct	lineData	= ( IStruct ) lineVal;
					Element	lineNode	= doc.createElement( "lineToCover" );
					lineNode.setAttribute( "lineNumber", lineKey.toString() );
					lineNode.setAttribute( "covered", Boolean.toString( ( Boolean ) lineData.get( "covered" ) ) );
					fileNode.appendChild( lineNode );
				} );
				coverage.appendChild( fileNode );
			} );

			return serialize( doc );
		} catch ( ParserConfigurationException e ) {
			throw new BoxRuntimeException( "Failed to build SonarQube coverage XML", e );
		}
	}

	/**
	 * Generate and write the SonarQube XML to {@code path}, creating parent
	 * directories as needed.
	 *
	 * @param lineCoverage the per-line coverage data
	 * @param path         the output file path
	 *
	 * @return the XML string written
	 */
	public static String write( IStruct lineCoverage, String path ) {
		String xml = generateXML( lineCoverage );
		try {
			Path	p		= Paths.get( path ).toAbsolutePath().normalize();
			Path	parent	= p.getParent();
			if ( parent != null ) {
				Files.createDirectories( parent );
			}
			Files.writeString( p, xml );
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Failed to write SonarQube coverage XML to [" + path + "]", e );
		}
		return xml;
	}

	/**
	 * Turn a DOM document into an indented XML string.
	 */
	private static String serialize( Document doc ) {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
			transformer.setOutputProperty( OutputKeys.ENCODING, "UTF-8" );
			StringWriter writer = new StringWriter();
			transformer.transform( new DOMSource( doc ), new StreamResult( writer ) );
			return writer.toString();
		} catch ( TransformerException e ) {
			throw new BoxRuntimeException( "Failed to serialize SonarQube coverage XML", e );
		}
	}
}
