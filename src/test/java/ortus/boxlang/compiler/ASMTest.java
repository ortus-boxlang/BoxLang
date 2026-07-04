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
package ortus.boxlang.compiler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.compiler.asmboxpiler.ASMBoxpiler;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.util.ResolvedFilePath;

public class ASMTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@Test
	public void testIntegerLiteralShouldNotMessUpStack() {
		instance.executeSource(
		    """
		       function doTest(){
		    	if( true ){
		    		if( true ){
		    			1
		    		}
		    	}
		    }
		          """,
		    context );
	}

	@Test
	public void testTernaryShouldNotMessUpStack() {
		instance.executeSource(
		    """
		       function doTest(){
		    	if( true ){
		    		if( true ){
		    			1 ? "a" : "b"
		    		}
		    	}
		    }
		          """,
		    context );
	}

	@Test
	public void testSimpleImport() {
		// @formatter:off
		instance.executeSource(
		    """
		    	import java.lang.System;
		    """, context );
		// @formatter:on
	}

	@Test
	public void testTryCatchAndFunction() {
		// @formatter:off
		instance.executeSource(
		    """
		    	try{

				}
				catch( any e ){
				}
				function doTest(){
				}
		    """, context );
		// @formatter:on
	}

	@Test
	public void testGetInstancePattern() {
		// Test that getInstance methods work correctly for functions and lambdas
		instance.executeSource(
		    """
		    	// Test function getInstance via compilation
		    	function testFunc() {
		    		return "function result";
		    	}

		    	// Test lambda getInstance via compilation
		    	lambda = () => {
		    		return "lambda result";
		    	};

		    	result = testFunc();
		    	lambdaResult = lambda();
		    """, context );

		// Verify the functions execute correctly (indicating getInstance works)
		var	result			= context.getScopeNearby( VariablesScope.name ).get( new Key( "result" ) );
		var	lambdaResult	= context.getScopeNearby( VariablesScope.name ).get( new Key( "lambdaResult" ) );

		assert result.equals( "function result" );
		assert lambdaResult.equals( "lambda result" );
	}

	@DisplayName( "large switch template should compile without recursive splitting" )
	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	@Test
	public void testSplit1() {
		assertDoesNotThrow( () -> RunnableLoader.getInstance().getBoxpiler().compileTemplate( createLargeComponentBodyTemplate() ) );
	}

	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	@DisplayName( "large switch template should compile without recursive splitting" )
	@Test
	public void testLargeSwitchTemplateShouldCompileWithoutRecursiveSplitting() {
		ResolvedFilePath resolvedPath = ResolvedFilePath.of( Path.of( "src/test/resources/test-templates/overflow.cfm" ) );

		assertDoesNotThrow( () -> RunnableLoader.getInstance().getBoxpiler().compileTemplate( resolvedPath ) );
	}

	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	@DisplayName( "very large switch template should compile" )
	@Test
	public void testVeryLargeSwitchTemplateShouldCompile() {
		assertDoesNotThrow( () -> RunnableLoader.getInstance().getBoxpiler().compileScript( buildVeryLargeSwitchTemplate(), BoxSourceType.CFTEMPLATE ) );
	}

	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	@DisplayName( "large 99-case switch template should compile" )
	@Test
	public void testLargeNinetyNineCaseSwitchTemplateShouldCompile() {
		assertDoesNotThrow(
		    () -> RunnableLoader.getInstance().getBoxpiler().compileScript( buildLargeNinetyNineCaseSwitchTemplate(), BoxSourceType.CFTEMPLATE ) );
	}

	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	@DisplayName( "large if body inside loop should compile after outlining large if branches" )
	@Test
	public void testLargeIfBodyInsideLoopShouldCompileAfterOutliningLargeIfBranches() {
		assertDoesNotThrow( () -> RunnableLoader.getInstance().getBoxpiler().compileScript( buildLargeIfBodyInsideLoopTemplate(), BoxSourceType.CFTEMPLATE ) );
	}

	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	@DisplayName( "outlined component if branch should preserve variables for later branches" )
	@Test
	public void testOutlinedComponentIfBranchShouldPreserveVariablesForLaterBranches() {
		instance.executeSource( buildOutlinedComponentBranchScopeTemplate(), context, BoxSourceType.CFTEMPLATE );

		assertEquals( "from outlined branch", variables.get( Key.of( "result" ) ) );
	}

	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	@DisplayName( "outlined component if branch should report source line numbers" )
	@Test
	public void testOutlinedComponentIfBranchShouldReportSourceLineNumbers() {
		KeyNotFoundException exception = assertThrows(
		    KeyNotFoundException.class,
		    () -> instance.executeSource( buildOutlinedComponentBranchMissingVariableTemplate(), context, BoxSourceType.CFTEMPLATE )
		);

		assertThatFirstTagContextLineEquals( exception, 6006 );
	}

	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	@Test
	public void testLabeledBreakInsideBreakingSwitchEmitsLabeledBodyResult() {
		BodyResultInvocation invocation = findBodyResultInvocation(
		    "ofBreak",
		    """
		    <cfloop from="1" to="5" index="outer" label="outerLoop">
		    	<cfloop from="1" to="5" index="inner">
		    		<cfswitch expression="go">
		    			<cfcase value="go">
		    				<cfbreak outerLoop>
		    			</cfcase>
		    		</cfswitch>
		    	</cfloop>
		    </cfloop>
		    """
		);

		assertNotNull( invocation );
		assertEquals( "outerLoop", invocation.argument() );
	}

	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	@Test
	public void testLabeledContinueInsideBreakingSwitchEmitsLabeledBodyResult() {
		BodyResultInvocation invocation = findBodyResultInvocation(
		    "ofContinue",
		    """
		    <cfloop from="1" to="4" index="outer" label="outerLoop">
		    	<cfloop from="1" to="2" index="inner">
		    		<cfswitch expression="go">
		    			<cfcase value="go">
		    				<cfif inner EQ 1>
		    					<cfcontinue outerLoop>
		    				</cfif>
		    			</cfcase>
		    		</cfswitch>
		    	</cfloop>
		    </cfloop>
		    """
		);

		assertNotNull( invocation );
		assertEquals( "outerLoop", invocation.argument() );
	}

	private String buildVeryLargeSwitchTemplate() {
		StringBuilder source = new StringBuilder();

		source.append( "<cfset result = ''>\n" );
		source.append( "<cfswitch expression=\"case0\">\n" );

		for ( int i = 0; i < 1200; i++ ) {
			source.append( "<cfcase value=\"case" ).append( i ).append( "\">\n" );
			source.append( "<cfset result = 'case" ).append( i ).append( "'>\n" );
			source.append( "<cfbreak>\n" );
			source.append( "</cfcase>\n" );
		}

		source.append( "<cfdefaultcase>\n" );
		source.append( "<cfset result = 'default'>\n" );
		source.append( "</cfdefaultcase>\n" );
		source.append( "</cfswitch>\n" );

		return source.toString();
	}

	private String buildLargeIfBodyInsideLoopTemplate() {
		StringBuilder source = new StringBuilder();

		source.append( "<cfset result = ''>\n" );
		source.append( "<cfloop from=\"1\" to=\"1\" index=\"taskIdx\">\n" );
		source.append( "<cfif true>\n" );

		for ( int i = 0; i < 5000; i++ ) {
			source.append( "<cfset result = 'segment" ).append( i ).append( "'>\n" );
		}

		source.append( "</cfif>\n" );
		source.append( "</cfloop>\n" );

		return source.toString();
	}

	private String buildOutlinedComponentBranchScopeTemplate() {
		StringBuilder source = new StringBuilder();

		source.append( "<cfoutput>\n" );
		source.append( "<cfif true>\n" );
		for ( int i = 0; i < 6000; i++ ) {
			source.append( "<cfset splitFiller = " ).append( i ).append( ">\n" );
		}
		source.append( "<cfset branchValue = 'from outlined branch'>\n" );
		source.append( "</cfif>\n" );
		source.append( "<cfif true>\n" );
		source.append( "<cfset result = branchValue>\n" );
		source.append( "</cfif>\n" );
		source.append( "</cfoutput>\n" );

		return source.toString();
	}

	private String buildOutlinedComponentBranchMissingVariableTemplate() {
		StringBuilder source = new StringBuilder();

		source.append( "<cfoutput>\n" );
		source.append( "<cfif true>\n" );
		for ( int i = 0; i < 6003; i++ ) {
			source.append( "<cfset filler = " ).append( i ).append( ">\n" );
		}
		source.append( "<cfset result = missingOutlinedBranchVariable>\n" );
		source.append( "</cfif>\n" );
		source.append( "</cfoutput>\n" );

		return source.toString();
	}

	private void assertThatFirstTagContextLineEquals( KeyNotFoundException exception, int line ) {
		assertNotNull( exception.getTagContext() );
		assertEquals( line, ( ( IStruct ) exception.getTagContext().get( 0 ) ).getAsInteger( Key.line ) );
	}

	private String buildLargeComponentBodyTemplate() {
		String			block	= readLargeComponentBodyBlock();
		StringBuilder	source	= new StringBuilder();

		source.append( "<cfparam name=\"variables.datasource\" default=\"testDatasource\">\n" );
		source.append( "<cfparam name=\"variables.username\" default=\"testUser\">\n" );
		source.append( "<cfparam name=\"variables.password\" default=\"testPassword\">\n" );
		source.append(
		    "<cfstoredproc procedure=\"schema.generic_large_proc\" datasource=\"#variables.datasource#\" username=\"#variables.username#\" password=\"#variables.password#\">\n" );

		for ( int i = 0; i < 500; i++ ) {
			source.append( block );
		}

		source.append( "</cfstoredproc>\n" );

		return source.toString();
	}

	private String readLargeComponentBodyBlock() {
		try {
			return Files.readString( Path.of( "src/test/resources/test-templates/genericLargeComponentBodyBlock.cfm" ) );
		} catch ( IOException e ) {
			throw new RuntimeException( "Unable to read generic large component body block fixture", e );
		}
	}

	private ResolvedFilePath createLargeComponentBodyTemplate() {
		try {
			Path tempFile = Files.createTempFile( "generic-large-component-body-", ".cfm" );
			Files.writeString( tempFile, buildLargeComponentBodyTemplate() );
			tempFile.toFile().deleteOnExit();
			return ResolvedFilePath.of( tempFile );
		} catch ( IOException e ) {
			throw new RuntimeException( "Unable to create generic large component body template", e );
		}
	}

	private String buildLargeNinetyNineCaseSwitchTemplate() {
		StringBuilder source = new StringBuilder();

		source.append( "<cfset result = ''>\n" );
		source.append( "<cfswitch expression=\"case0\">\n" );

		for ( int i = 0; i < 99; i++ ) {
			source.append( "<cfcase value=\"case" ).append( i ).append( "\">\n" );
			for ( int j = 0; j < 30; j++ ) {
				source.append( "<cfset result = 'case" ).append( i ).append( "-segment" ).append( j ).append( "'>\n" );
			}
			source.append( "<cfbreak>\n" );
			source.append( "</cfcase>\n" );
		}

		source.append( "<cfdefaultcase>\n" );
		source.append( "<cfset result = 'default'>\n" );
		source.append( "</cfdefaultcase>\n" );
		source.append( "</cfswitch>\n" );

		return source.toString();
	}

	private BodyResultInvocation findBodyResultInvocation( String methodName, String source ) {
		ASMBoxpiler boxpiler = ( ASMBoxpiler ) RunnableLoader.getInstance().getBoxpiler();
		boxpiler.clearPagePool();

		ClassInfo classInfo = ClassInfo.forScript( source, BoxSourceType.CFTEMPLATE, boxpiler );
		boxpiler.getClassPool( classInfo.classPoolName() ).put( classInfo.fqn().toString(), classInfo );

		List<byte[]> compiledClasses = boxpiler.compileClassInfo( classInfo.classPoolName(), classInfo.fqn().toString() );

		for ( byte[] compiledClass : compiledClasses ) {
			if ( !isClassFile( compiledClass ) ) {
				continue;
			}

			ClassNode classNode = new ClassNode();
			new ClassReader( compiledClass ).accept( classNode, 0 );

			BodyResultInvocation invocation = findBodyResultInvocation( classNode, methodName );
			if ( invocation != null ) {
				return invocation;
			}
		}

		return null;
	}

	private BodyResultInvocation findBodyResultInvocation( ClassNode classNode, String methodName ) {
		String owner = Type.getInternalName( Component.BodyResult.class );

		for ( MethodNode method : classNode.methods ) {
			for ( AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext() ) {
				if ( ! ( instruction instanceof MethodInsnNode methodInstruction ) ) {
					continue;
				}

				if ( !owner.equals( methodInstruction.owner ) || !methodName.equals( methodInstruction.name ) ) {
					continue;
				}

				AbstractInsnNode argumentInstruction = getPreviousMeaningfulInstruction( methodInstruction );
				if ( argumentInstruction instanceof LdcInsnNode ldcInstruction ) {
					return new BodyResultInvocation( ( String ) ldcInstruction.cst );
				}

				if ( argumentInstruction instanceof InsnNode insnNode && insnNode.getOpcode() == Opcodes.ACONST_NULL ) {
					return new BodyResultInvocation( null );
				}
			}
		}

		return null;
	}

	private AbstractInsnNode getPreviousMeaningfulInstruction( AbstractInsnNode instruction ) {
		AbstractInsnNode previous = instruction.getPrevious();

		while ( previous instanceof LabelNode || previous instanceof LineNumberNode || previous instanceof FrameNode ) {
			previous = previous.getPrevious();
		}

		return previous;
	}

	private boolean isClassFile( byte[] bytes ) {
		return bytes.length >= 4
		    && bytes[ 0 ] == ( byte ) 0xCA
		    && bytes[ 1 ] == ( byte ) 0xFE
		    && bytes[ 2 ] == ( byte ) 0xBA
		    && bytes[ 3 ] == ( byte ) 0xBE;
	}

	private record BodyResultInvocation( String argument ) {
	}

}
