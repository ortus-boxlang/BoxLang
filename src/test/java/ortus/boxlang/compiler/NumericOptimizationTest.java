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

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import ortus.boxlang.compiler.asmboxpiler.ASMBoxpiler;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.runnables.RunnableLoader;

/**
 * Tests that the bytecode optimization for set-aware operators (Plus, Minus, Star, Power)
 * correctly uses the fast-path invoke(Number, Number) when both operands are known numeric,
 * and falls back to invoke(Object, Object) when operand types are unknown.
 */
public class NumericOptimizationTest {

	static BoxRuntime instance;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {
	}

	// ======================== ASM Boxpiler Tests ========================

	@DisplayName( "ASM: 1 * 2 binds to Multiply.invoke(Number, Number) descriptor" )
	@Test
	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	public void testASM_LiteralMultiplyUsesNumberOverload() {
		List<MethodInsnNode>	calls				= findOperatorCalls( "result = 1 * 2;", "Multiply" );
		boolean					hasNumberOverload	= calls.stream()
		    .anyMatch(
		        m -> m.desc.equals( Type.getMethodDescriptor( Type.getType( Number.class ), Type.getType( Number.class ), Type.getType( Number.class ) ) ) );
		assertThat( hasNumberOverload ).isTrue();
	}

	@DisplayName( "ASM: foo * bar binds to Multiply.invoke(Object, Object) descriptor" )
	@Test
	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	public void testASM_VariableMultiplyUsesObjectOverload() {
		List<MethodInsnNode>	calls				= findOperatorCalls( "foo = 1; bar = 2; result = foo * bar;", "Multiply" );
		boolean					hasObjectOverload	= calls.stream()
		    .anyMatch(
		        m -> m.desc.equals( Type.getMethodDescriptor( Type.getType( Object.class ), Type.getType( Object.class ), Type.getType( Object.class ) ) ) );
		assertThat( hasObjectOverload ).isTrue();
	}

	@DisplayName( "ASM: 2 ^ 3 binds to Power.invoke(Number, Number) descriptor" )
	@Test
	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	public void testASM_LiteralPowerUsesNumberOverload() {
		List<MethodInsnNode>	calls				= findOperatorCalls( "result = 2 ^ 3;", "Power" );
		boolean					hasNumberOverload	= calls.stream()
		    .anyMatch(
		        m -> m.desc.equals( Type.getMethodDescriptor( Type.getType( Number.class ), Type.getType( Number.class ), Type.getType( Number.class ) ) ) );
		assertThat( hasNumberOverload ).isTrue();
	}

	@DisplayName( "ASM: foo ^ bar binds to Power.invoke(Object, Object) descriptor" )
	@Test
	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	public void testASM_VariablePowerUsesObjectOverload() {
		List<MethodInsnNode>	calls				= findOperatorCalls( "foo = 2; bar = 3; result = foo ^ bar;", "Power" );
		boolean					hasObjectOverload	= calls.stream()
		    .anyMatch(
		        m -> m.desc.equals( Type.getMethodDescriptor( Type.getType( Object.class ), Type.getType( Object.class ), Type.getType( Object.class ) ) ) );
		assertThat( hasObjectOverload ).isTrue();
	}

	@DisplayName( "ASM: 1 + 2 binds to Plus.invoke(Number, Number) descriptor" )
	@Test
	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	public void testASM_LiteralPlusUsesNumberOverload() {
		List<MethodInsnNode>	calls				= findOperatorCalls( "result = 1 + 2;", "Plus" );
		boolean					hasNumberOverload	= calls.stream()
		    .anyMatch(
		        m -> m.desc.equals( Type.getMethodDescriptor( Type.getType( Number.class ), Type.getType( Number.class ), Type.getType( Number.class ) ) ) );
		assertThat( hasNumberOverload ).isTrue();
	}

	@DisplayName( "ASM: foo + bar binds to Plus.invoke(Object, Object) descriptor" )
	@Test
	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	public void testASM_VariablePlusUsesObjectOverload() {
		List<MethodInsnNode>	calls				= findOperatorCalls( "foo = 1; bar = 2; result = foo + bar;", "Plus" );
		boolean					hasObjectOverload	= calls.stream()
		    .anyMatch(
		        m -> m.desc.equals( Type.getMethodDescriptor( Type.getType( Object.class ), Type.getType( Object.class ), Type.getType( Object.class ) ) ) );
		assertThat( hasObjectOverload ).isTrue();
	}

	// ======================== AST returnsNumber() Tests ========================

	@DisplayName( "AST: 1 * 2 returnsNumber() = true" )
	@Test
	public void testAST_LiteralMultiplyReturnsNumberTrue() throws IOException {
		assertThat( parseBoxExpression( "1 * 2" ).returnsNumber() ).isTrue();
	}

	@DisplayName( "AST: foo * bar returnsNumber() = false" )
	@Test
	public void testAST_VariableMultiplyReturnsNumberFalse() throws IOException {
		assertThat( parseBoxExpression( "foo * bar" ).returnsNumber() ).isFalse();
	}

	@DisplayName( "AST: (1+2) * (3+4) nested literals returnsNumber() = true" )
	@Test
	public void testAST_NestedLiteralReturnsNumberTrue() throws IOException {
		assertThat( parseBoxExpression( "(1+2) * (3+4)" ).returnsNumber() ).isTrue();
	}

	@DisplayName( "AST: (foo+1) * 2 mixed returnsNumber() = false" )
	@Test
	public void testAST_MixedReturnsNumberFalse() throws IOException {
		assertThat( parseBoxExpression( "(foo+1) * 2" ).returnsNumber() ).isFalse();
	}

	// ======================== Helpers ========================

	/**
	 * Compiles source via ASM boxpiler and returns all INVOKESTATIC calls to the given operator class.
	 */
	private List<MethodInsnNode> findOperatorCalls( String source, String operatorSimpleName ) {
		ASMBoxpiler boxpiler = ( ASMBoxpiler ) RunnableLoader.getInstance().getBoxpiler();
		boxpiler.clearPagePool();

		ClassInfo classInfo = ClassInfo.forScript( source, BoxSourceType.BOXSCRIPT, boxpiler );
		boxpiler.getClassPool( classInfo.classPoolName() ).put( classInfo.fqn().toString(), classInfo );

		List<byte[]>			compiledClasses	= boxpiler.compileClassInfo( classInfo.classPoolName(), classInfo.fqn().toString() );
		List<MethodInsnNode>	operatorCalls	= new ArrayList<>();

		for ( byte[] compiledClass : compiledClasses ) {
			if ( !isClassFile( compiledClass ) ) {
				continue;
			}

			ClassNode classNode = new ClassNode();
			new ClassReader( compiledClass ).accept( classNode, 0 );

			for ( MethodNode method : classNode.methods ) {
				for ( AbstractInsnNode instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext() ) {
					if ( instruction instanceof MethodInsnNode methodInsn ) {
						if ( methodInsn.name.equals( "invoke" ) && methodInsn.owner.endsWith( "/" + operatorSimpleName ) ) {
							operatorCalls.add( methodInsn );
						}
					}
				}
			}
		}

		return operatorCalls;
	}

	/**
	 * Parses a BoxLang expression and returns the AST node.
	 */
	private BoxExpression parseBoxExpression( String expression ) throws IOException {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression( expression );
		assertThat( result.isCorrect() ).isTrue();
		return ( BoxExpression ) result.getRoot();
	}

	private boolean isClassFile( byte[] bytes ) {
		return bytes.length >= 4
		    && bytes[ 0 ] == ( byte ) 0xCA
		    && bytes[ 1 ] == ( byte ) 0xFE
		    && bytes[ 2 ] == ( byte ) 0xBA
		    && bytes[ 3 ] == ( byte ) 0xBE;
	}

}
