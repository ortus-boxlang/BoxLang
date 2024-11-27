package ortus.boxlang.compiler.asmboxpiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.Transformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxAccessTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxArgumentDeclarationTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxArgumentTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxArrayLiteralTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxAssignmentTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxBinaryOperationTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxBooleanLiteralTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxBreakTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxClosureTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxComparisonOperationTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxContinueTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxDecimalLiteralTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxExpressionInvocationTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxExpressionStatementTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxFQNTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxFunctionInvocationTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxFunctionalBIFAccessTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxFunctionalMemberAccessTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxIdentifierTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxImportTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxIntegerLiteralTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxLambdaTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxMethodInvocationTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxNewTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxNullTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxParenthesisTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxReturnTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxScopeTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxStatementBlockTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxStaticAccessTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxStaticMethodInvocationTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxStringConcatTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxStringInterpolationTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxStringLiteralTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxStructLiteralTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxSwitchTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxTernaryOperationTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.expression.BoxUnaryOperationTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxAssertTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxBufferOutputTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxClassTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxComponentTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxDoTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxForInTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxForIndexTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxFunctionDeclarationTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxIfElseTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxInterfaceTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxParamTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxRethrowTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxScriptIslandTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxStaticInitializerTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxTemplateIslandTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxThrowTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxTryTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.statement.BoxWhileTransformer;
import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxInterface;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.BoxStaticInitializer;
import ortus.boxlang.compiler.ast.Source;
import ortus.boxlang.compiler.ast.SourceFile;
import ortus.boxlang.compiler.ast.expression.BoxArgument;
import ortus.boxlang.compiler.ast.expression.BoxArrayAccess;
import ortus.boxlang.compiler.ast.expression.BoxArrayLiteral;
import ortus.boxlang.compiler.ast.expression.BoxAssignment;
import ortus.boxlang.compiler.ast.expression.BoxBinaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxBooleanLiteral;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxComparisonOperation;
import ortus.boxlang.compiler.ast.expression.BoxDecimalLiteral;
import ortus.boxlang.compiler.ast.expression.BoxDotAccess;
import ortus.boxlang.compiler.ast.expression.BoxExpressionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxFunctionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxFunctionalBIFAccess;
import ortus.boxlang.compiler.ast.expression.BoxFunctionalMemberAccess;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxLambda;
import ortus.boxlang.compiler.ast.expression.BoxMethodInvocation;
import ortus.boxlang.compiler.ast.expression.BoxNew;
import ortus.boxlang.compiler.ast.expression.BoxNull;
import ortus.boxlang.compiler.ast.expression.BoxParenthesis;
import ortus.boxlang.compiler.ast.expression.BoxScope;
import ortus.boxlang.compiler.ast.expression.BoxStaticAccess;
import ortus.boxlang.compiler.ast.expression.BoxStaticMethodInvocation;
import ortus.boxlang.compiler.ast.expression.BoxStringConcat;
import ortus.boxlang.compiler.ast.expression.BoxStringInterpolation;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStructLiteral;
import ortus.boxlang.compiler.ast.expression.BoxTernaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxUnaryOperation;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxAssert;
import ortus.boxlang.compiler.ast.statement.BoxBreak;
import ortus.boxlang.compiler.ast.statement.BoxBufferOutput;
import ortus.boxlang.compiler.ast.statement.BoxContinue;
import ortus.boxlang.compiler.ast.statement.BoxDo;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.ast.statement.BoxForIn;
import ortus.boxlang.compiler.ast.statement.BoxForIndex;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxIfElse;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.compiler.ast.statement.BoxParam;
import ortus.boxlang.compiler.ast.statement.BoxProperty;
import ortus.boxlang.compiler.ast.statement.BoxRethrow;
import ortus.boxlang.compiler.ast.statement.BoxReturn;
import ortus.boxlang.compiler.ast.statement.BoxScriptIsland;
import ortus.boxlang.compiler.ast.statement.BoxStatementBlock;
import ortus.boxlang.compiler.ast.statement.BoxSwitch;
import ortus.boxlang.compiler.ast.statement.BoxThrow;
import ortus.boxlang.compiler.ast.statement.BoxTry;
import ortus.boxlang.compiler.ast.statement.BoxWhile;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;
import ortus.boxlang.compiler.ast.statement.component.BoxTemplateIsland;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.ClassVariablesScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.AbstractFunction;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.DefaultExpression;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Property;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;
import ortus.boxlang.runtime.types.util.MapHelper;
import ortus.boxlang.runtime.util.ResolvedFilePath;

public class AsmTranspiler extends Transpiler {

	private static final int[]						STACK_SIZE_DELTA			= {
	    0, // nop = 0 (0x0)
	    1, // aconst_null = 1 (0x1)
	    1, // iconst_m1 = 2 (0x2)
	    1, // iconst_0 = 3 (0x3)
	    1, // iconst_1 = 4 (0x4)
	    1, // iconst_2 = 5 (0x5)
	    1, // iconst_3 = 6 (0x6)
	    1, // iconst_4 = 7 (0x7)
	    1, // iconst_5 = 8 (0x8)
	    2, // lconst_0 = 9 (0x9)
	    2, // lconst_1 = 10 (0xa)
	    1, // fconst_0 = 11 (0xb)
	    1, // fconst_1 = 12 (0xc)
	    1, // fconst_2 = 13 (0xd)
	    2, // dconst_0 = 14 (0xe)
	    2, // dconst_1 = 15 (0xf)
	    1, // bipush = 16 (0x10)
	    1, // sipush = 17 (0x11)
	    Integer.MIN_VALUE, // ldc = 18 (0x12)
	    Integer.MIN_VALUE, // ldc_w = 19 (0x13)
	    Integer.MIN_VALUE, // ldc2_w = 20 (0x14)
	    1, // iload = 21 (0x15)
	    2, // lload = 22 (0x16)
	    1, // fload = 23 (0x17)
	    2, // dload = 24 (0x18)
	    1, // aload = 25 (0x19)
	    1, // iload_0 = 26 (0x1a)
	    1, // iload_1 = 27 (0x1b)
	    1, // iload_2 = 28 (0x1c)
	    1, // iload_3 = 29 (0x1d)
	    2, // lload_0 = 30 (0x1e)
	    2, // lload_1 = 31 (0x1f)
	    2, // lload_2 = 32 (0x20)
	    2, // lload_3 = 33 (0x21)
	    1, // fload_0 = 34 (0x22)
	    1, // fload_1 = 35 (0x23)
	    1, // fload_2 = 36 (0x24)
	    1, // fload_3 = 37 (0x25)
	    2, // dload_0 = 38 (0x26)
	    2, // dload_1 = 39 (0x27)
	    2, // dload_2 = 40 (0x28)
	    2, // dload_3 = 41 (0x29)
	    1, // aload_0 = 42 (0x2a)
	    1, // aload_1 = 43 (0x2b)
	    1, // aload_2 = 44 (0x2c)
	    1, // aload_3 = 45 (0x2d)
	    -1, // iaload = 46 (0x2e)
	    0, // laload = 47 (0x2f)
	    -1, // faload = 48 (0x30)
	    0, // daload = 49 (0x31)
	    -1, // aaload = 50 (0x32)
	    -1, // baload = 51 (0x33)
	    -1, // caload = 52 (0x34)
	    -1, // saload = 53 (0x35)
	    -1, // istore = 54 (0x36)
	    -2, // lstore = 55 (0x37)
	    -1, // fstore = 56 (0x38)
	    -2, // dstore = 57 (0x39)
	    -1, // astore = 58 (0x3a)
	    -1, // istore_0 = 59 (0x3b)
	    -1, // istore_1 = 60 (0x3c)
	    -1, // istore_2 = 61 (0x3d)
	    -1, // istore_3 = 62 (0x3e)
	    -2, // lstore_0 = 63 (0x3f)
	    -2, // lstore_1 = 64 (0x40)
	    -2, // lstore_2 = 65 (0x41)
	    -2, // lstore_3 = 66 (0x42)
	    -1, // fstore_0 = 67 (0x43)
	    -1, // fstore_1 = 68 (0x44)
	    -1, // fstore_2 = 69 (0x45)
	    -1, // fstore_3 = 70 (0x46)
	    -2, // dstore_0 = 71 (0x47)
	    -2, // dstore_1 = 72 (0x48)
	    -2, // dstore_2 = 73 (0x49)
	    -2, // dstore_3 = 74 (0x4a)
	    -1, // astore_0 = 75 (0x4b)
	    -1, // astore_1 = 76 (0x4c)
	    -1, // astore_2 = 77 (0x4d)
	    -1, // astore_3 = 78 (0x4e)
	    -3, // iastore = 79 (0x4f)
	    -4, // lastore = 80 (0x50)
	    -3, // fastore = 81 (0x51)
	    -4, // dastore = 82 (0x52)
	    -3, // aastore = 83 (0x53)
	    -3, // bastore = 84 (0x54)
	    -3, // castore = 85 (0x55)
	    -3, // sastore = 86 (0x56)
	    -1, // pop = 87 (0x57)
	    -2, // pop2 = 88 (0x58)
	    1, // dup = 89 (0x59)
	    1, // dup_x1 = 90 (0x5a)
	    1, // dup_x2 = 91 (0x5b)
	    2, // dup2 = 92 (0x5c)
	    2, // dup2_x1 = 93 (0x5d)
	    2, // dup2_x2 = 94 (0x5e)
	    0, // swap = 95 (0x5f)
	    -1, // iadd = 96 (0x60)
	    -2, // ladd = 97 (0x61)
	    -1, // fadd = 98 (0x62)
	    -2, // dadd = 99 (0x63)
	    -1, // isub = 100 (0x64)
	    -2, // lsub = 101 (0x65)
	    -1, // fsub = 102 (0x66)
	    -2, // dsub = 103 (0x67)
	    -1, // imul = 104 (0x68)
	    -2, // lmul = 105 (0x69)
	    -1, // fmul = 106 (0x6a)
	    -2, // dmul = 107 (0x6b)
	    -1, // idiv = 108 (0x6c)
	    -2, // ldiv = 109 (0x6d)
	    -1, // fdiv = 110 (0x6e)
	    -2, // ddiv = 111 (0x6f)
	    -1, // irem = 112 (0x70)
	    -2, // lrem = 113 (0x71)
	    -1, // frem = 114 (0x72)
	    -2, // drem = 115 (0x73)
	    0, // ineg = 116 (0x74)
	    0, // lneg = 117 (0x75)
	    0, // fneg = 118 (0x76)
	    0, // dneg = 119 (0x77)
	    -1, // ishl = 120 (0x78)
	    -1, // lshl = 121 (0x79)
	    -1, // ishr = 122 (0x7a)
	    -1, // lshr = 123 (0x7b)
	    -1, // iushr = 124 (0x7c)
	    -1, // lushr = 125 (0x7d)
	    -1, // iand = 126 (0x7e)
	    -2, // land = 127 (0x7f)
	    -1, // ior = 128 (0x80)
	    -2, // lor = 129 (0x81)
	    -1, // ixor = 130 (0x82)
	    -2, // lxor = 131 (0x83)
	    0, // iinc = 132 (0x84)
	    1, // i2l = 133 (0x85)
	    0, // i2f = 134 (0x86)
	    1, // i2d = 135 (0x87)
	    -1, // l2i = 136 (0x88)
	    -1, // l2f = 137 (0x89)
	    0, // l2d = 138 (0x8a)
	    0, // f2i = 139 (0x8b)
	    1, // f2l = 140 (0x8c)
	    1, // f2d = 141 (0x8d)
	    -1, // d2i = 142 (0x8e)
	    0, // d2l = 143 (0x8f)
	    -1, // d2f = 144 (0x90)
	    0, // i2b = 145 (0x91)
	    0, // i2c = 146 (0x92)
	    0, // i2s = 147 (0x93)
	    -3, // lcmp = 148 (0x94)
	    -1, // fcmpl = 149 (0x95)
	    -1, // fcmpg = 150 (0x96)
	    -3, // dcmpl = 151 (0x97)
	    -3, // dcmpg = 152 (0x98)
	    -1, // ifeq = 153 (0x99)
	    -1, // ifne = 154 (0x9a)
	    -1, // iflt = 155 (0x9b)
	    -1, // ifge = 156 (0x9c)
	    -1, // ifgt = 157 (0x9d)
	    -1, // ifle = 158 (0x9e)
	    -2, // if_icmpeq = 159 (0x9f)
	    -2, // if_icmpne = 160 (0xa0)
	    -2, // if_icmplt = 161 (0xa1)
	    -2, // if_icmpge = 162 (0xa2)
	    -2, // if_icmpgt = 163 (0xa3)
	    -2, // if_icmple = 164 (0xa4)
	    -2, // if_acmpeq = 165 (0xa5)
	    -2, // if_acmpne = 166 (0xa6)
	    0, // goto = 167 (0xa7)
	    1, // jsr = 168 (0xa8)
	    0, // ret = 169 (0xa9)
	    -1, // tableswitch = 170 (0xaa)
	    -1, // lookupswitch = 171 (0xab)
	    -1, // ireturn = 172 (0xac)
	    -2, // lreturn = 173 (0xad)
	    -1, // freturn = 174 (0xae)
	    -2, // dreturn = 175 (0xaf)
	    -1, // areturn = 176 (0xb0)
	    0, // return = 177 (0xb1)
	    Integer.MIN_VALUE, // getstatic = 178 (0xb2)
	    Integer.MIN_VALUE, // putstatic = 179 (0xb3)
	    Integer.MIN_VALUE, // getfield = 180 (0xb4)
	    Integer.MIN_VALUE, // putfield = 181 (0xb5)
	    Integer.MIN_VALUE, // invokevirtual = 182 (0xb6)
	    Integer.MIN_VALUE, // invokespecial = 183 (0xb7)
	    Integer.MIN_VALUE, // invokestatic = 184 (0xb8)
	    Integer.MIN_VALUE, // invokeinterface = 185 (0xb9)
	    Integer.MIN_VALUE, // invokedynamic = 186 (0xba)
	    1, // new = 187 (0xbb)
	    0, // newarray = 188 (0xbc)
	    0, // anewarray = 189 (0xbd)
	    0, // arraylength = 190 (0xbe)
	    -1, // athrow = 191 (0xbf)
	    0, // checkcast = 192 (0xc0)
	    0, // instanceof = 193 (0xc1)
	    -1, // monitorenter = 194 (0xc2)
	    -1, // monitorexit = 195 (0xc3)
	    Integer.MIN_VALUE, // wide = 196 (0xc4)
	    Integer.MIN_VALUE, // multianewarray = 197 (0xc5)
	    -1, // ifnull = 198 (0xc6)
	    -1, // ifnonnull = 199 (0xc7)
	    Integer.MIN_VALUE, // goto_w = 200 (0xc8)
	    Integer.MIN_VALUE // jsr_w = 201 (0xc9)
	};

	private static HashMap<Class<?>, Transformer>	registry					= new HashMap<>();
	private static final String						EXTENDS_ANNOTATION_MARKER	= "overrideJava";

	public AsmTranspiler() {
		// TODO: instance write to static field. Seems like an oversight in Java version (retained until clarified).
		registry.put( BoxStringLiteral.class, new BoxStringLiteralTransformer( this ) );
		registry.put( BoxIntegerLiteral.class, new BoxIntegerLiteralTransformer( this ) );
		registry.put( BoxExpressionStatement.class, new BoxExpressionStatementTransformer( this ) );
		registry.put( BoxAssignment.class, new BoxAssignmentTransformer( this ) );
		registry.put( BoxArrayLiteral.class, new BoxArrayLiteralTransformer( this ) );
		registry.put( BoxFunctionDeclaration.class, new BoxFunctionDeclarationTransformer( this ) );
		registry.put( BoxFunctionInvocation.class, new BoxFunctionInvocationTransformer( this ) );
		registry.put( BoxArgument.class, new BoxArgumentTransformer( this ) );
		registry.put( BoxStringConcat.class, new BoxStringConcatTransformer( this ) );
		registry.put( BoxStringInterpolation.class, new BoxStringInterpolationTransformer( this ) );
		registry.put( BoxMethodInvocation.class, new BoxMethodInvocationTransformer( this ) );
		registry.put( BoxReturn.class, new BoxReturnTransformer( this ) );
		registry.put( BoxStructLiteral.class, new BoxStructLiteralTransformer( this ) );
		registry.put( BoxIdentifier.class, new BoxIdentifierTransformer( this ) );
		registry.put( BoxBinaryOperation.class, new BoxBinaryOperationTransformer( this ) );
		registry.put( BoxDotAccess.class, new BoxAccessTransformer( this ) );
		registry.put( BoxArrayAccess.class, new BoxAccessTransformer( this ) );
		registry.put( BoxArgumentDeclaration.class, new BoxArgumentDeclarationTransformer( this ) );
		registry.put( BoxFQN.class, new BoxFQNTransformer( this ) );
		registry.put( BoxLambda.class, new BoxLambdaTransformer( this ) );
		registry.put( BoxBooleanLiteral.class, new BoxBooleanLiteralTransformer( this ) );
		registry.put( BoxNull.class, new BoxNullTransformer( this ) );
		registry.put( BoxNew.class, new BoxNewTransformer( this ) );
		registry.put( BoxUnaryOperation.class, new BoxUnaryOperationTransformer( this ) );
		registry.put( BoxDecimalLiteral.class, new BoxDecimalLiteralTransformer( this ) );
		registry.put( BoxStatementBlock.class, new BoxStatementBlockTransformer( this ) );
		registry.put( BoxIfElse.class, new BoxIfElseTransformer( this ) );
		registry.put( BoxComparisonOperation.class, new BoxComparisonOperationTransformer( this ) );
		registry.put( BoxTernaryOperation.class, new BoxTernaryOperationTransformer( this ) );
		registry.put( BoxSwitch.class, new BoxSwitchTransformer( this ) );
		registry.put( BoxScope.class, new BoxScopeTransformer( this ) );
		registry.put( BoxBreak.class, new BoxBreakTransformer( this ) );
		registry.put( BoxContinue.class, new BoxContinueTransformer( this ) );
		registry.put( BoxThrow.class, new BoxThrowTransformer( this ) );
		registry.put( BoxTry.class, new BoxTryTransformer( this ) );
		registry.put( BoxRethrow.class, new BoxRethrowTransformer( this ) );
		registry.put( BoxAssert.class, new BoxAssertTransformer( this ) );
		registry.put( BoxParenthesis.class, new BoxParenthesisTransformer( this ) );
		registry.put( BoxImport.class, new BoxImportTransformer( this ) );
		registry.put( BoxBufferOutput.class, new BoxBufferOutputTransformer( this ) );
		registry.put( BoxWhile.class, new BoxWhileTransformer( this ) );
		registry.put( BoxDo.class, new BoxDoTransformer( this ) );
		registry.put( BoxForIn.class, new BoxForInTransformer( this ) );
		registry.put( BoxForIndex.class, new BoxForIndexTransformer( this ) );
		registry.put( BoxClosure.class, new BoxClosureTransformer( this ) );
		registry.put( BoxComponent.class, new BoxComponentTransformer( this ) );
		registry.put( BoxStaticInitializer.class, new BoxStaticInitializerTransformer( this ) );
		registry.put( BoxStaticAccess.class, new BoxStaticAccessTransformer( this ) );
		registry.put( BoxStaticMethodInvocation.class, new BoxStaticMethodInvocationTransformer( this ) );
		registry.put( BoxScriptIsland.class, new BoxScriptIslandTransformer( this ) );
		registry.put( BoxTemplateIsland.class, new BoxTemplateIslandTransformer( this ) );
		registry.put( BoxExpressionInvocation.class, new BoxExpressionInvocationTransformer( this ) );
		registry.put( BoxParam.class, new BoxParamTransformer( this ) );
		registry.put( BoxFunctionalBIFAccess.class, new BoxFunctionalBIFAccessTransformer( this ) );
		registry.put( BoxFunctionalMemberAccess.class, new BoxFunctionalMemberAccessTransformer( this ) );
	}

	@Override
	public ClassNode transpile( BoxScript boxScript ) throws BoxRuntimeException {
		Type type = Type.getType( "L" + getProperty( "packageName" ).replace( '.', '/' ) + "/" + getProperty( "classname" ) + ";" );
		setProperty( "classType", type.getDescriptor() );
		setProperty( "classTypeInternal", type.getInternalName() );
		ClassNode	classNode		= new ClassNode();
		String		mappingName		= getProperty( "mappingName" );
		String		mappingPath		= getProperty( "mappingPath" );
		String		relativePath	= getProperty( "relativePath" );
		Source		source			= boxScript.getPosition().getSource();
		String		filePath		= source instanceof SourceFile file && file.getFile() != null ? file.getFile().getAbsolutePath() : "unknown";
		setProperty( "filePath", filePath );
		classNode.visitSource( filePath, null );

		String		baseClassName	= getProperty( "baseclass" ) != null ? getProperty( "baseclass" ) : "BoxScript";

		Class<?>	baseClass		= switch ( baseClassName.toUpperCase() ) {
										case "BOXTEMPLATE" -> ortus.boxlang.runtime.runnables.BoxTemplate.class;
										default -> ortus.boxlang.runtime.runnables.BoxScript.class;
									};

		String		returnTypeName	= baseClass.equals( "BoxScript" ) ? "Object" : "void";
		returnTypeName = getProperty( "returnType" ) != null ? getProperty( "returnType" ) : returnTypeName;

		Type returnType = switch ( returnTypeName.toUpperCase() ) {
			case "OBJECT" -> Type.getType( Object.class );
			default -> Type.VOID_TYPE;
		};

		AsmHelper.init( classNode, true, type, Type.getType( baseClass ), methodVisitor -> {
		} );
		classNode.visitField( Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_STATIC,
		    "keys",
		    Type.getDescriptor( ( Key[].class ) ),
		    null,
		    null ).visitEnd();
		AsmHelper.addStaticFieldGetter( classNode,
		    type,
		    "imports",
		    "getImports",
		    Type.getType( List.class ),
		    null );
		AsmHelper.addStaticFieldGetter( classNode,
		    type,
		    "path",
		    "getRunnablePath",
		    Type.getType( ResolvedFilePath.class ),
		    null );
		AsmHelper.addStaticFieldGetter( classNode,
		    type,
		    "sourceType",
		    "getSourceType",
		    Type.getType( BoxSourceType.class ),
		    null );

		AsmHelper.methodWithContextAndClassLocator(
		    classNode,
		    "_invoke",
		    Type.getType( IBoxContext.class ),
		    returnType,
		    false,
		    this,
		    false,
		    () -> AsmHelper.transformBodyExpressions( this, boxScript.getStatements(), TransformerContext.NONE,
		        returnType == Type.VOID_TYPE ? ReturnValueContext.EMPTY : ReturnValueContext.VALUE_OR_NULL )
		);

		AsmHelper.complete( classNode, type, methodVisitor -> {
			AsmHelper.array( Type.getType( ImportDefinition.class ), getImports(), ( raw, index ) -> {
				List<AbstractInsnNode> nodes = new ArrayList<>();
				nodes.addAll( raw );
				nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
				    Type.getInternalName( ImportDefinition.class ),
				    "parse",
				    Type.getMethodDescriptor( Type.getType( ImportDefinition.class ), Type.getType( String.class ) ),
				    false ) );
				return nodes;
			} ).forEach( node -> node.accept( methodVisitor ) );
			methodVisitor.visitMethodInsn( Opcodes.INVOKESTATIC,
			    Type.getInternalName( List.class ),
			    "of",
			    Type.getMethodDescriptor( Type.getType( List.class ), Type.getType( Object[].class ) ),
			    true );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "imports",
			    Type.getDescriptor( List.class ) );

			AsmHelper.resolvedFilePath( methodVisitor, mappingName, mappingPath, relativePath, filePath );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "path",
			    Type.getDescriptor( ResolvedFilePath.class ) );

			methodVisitor.visitFieldInsn( Opcodes.GETSTATIC,
			    Type.getInternalName( BoxSourceType.class ),
			    getProperty( "sourceType" ).toUpperCase(),
			    Type.getDescriptor( BoxSourceType.class ) );
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "sourceType",
			    Type.getDescriptor( BoxSourceType.class ) );

			methodVisitor.visitLdcInsn( getKeys().size() );
			methodVisitor.visitTypeInsn( Opcodes.ANEWARRAY, Type.getInternalName( Key.class ) );
			int index = 0;
			for ( BoxExpression expression : getKeys().values() ) {
				methodVisitor.visitInsn( Opcodes.DUP );
				methodVisitor.visitLdcInsn( index++ );
				transform( expression, TransformerContext.NONE, ReturnValueContext.VALUE ).forEach( methodInsnNode -> methodInsnNode.accept( methodVisitor ) );
				methodVisitor.visitMethodInsn( Opcodes.INVOKESTATIC,
				    Type.getInternalName( Key.class ),
				    "of",
				    Type.getMethodDescriptor( Type.getType( Key.class ), Type.getType( Object.class ) ),
				    false );
				methodVisitor.visitInsn( Opcodes.AASTORE );
			}
			methodVisitor.visitFieldInsn( Opcodes.PUTSTATIC,
			    type.getInternalName(),
			    "keys",
			    Type.getDescriptor( Key[].class ) );
		} );

		return classNode;
	}

	@Override
	public ClassNode transpile( BoxClass boxClass ) throws BoxRuntimeException {
		return BoxClassTransformer.transpile( this, boxClass );
	}

	public ClassNode transpile( BoxInterface boxClass ) throws BoxRuntimeException {
		return BoxInterfaceTransformer.transpile( this, boxClass );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnValueContext ) {
		Transformer transformer = registry.get( node.getClass() );
		if ( transformer != null ) {
			List<AbstractInsnNode> nodes = transformer.transform( node, context, returnValueContext );
			if ( ASMBoxpiler.DEBUG ) {
				int delta = 0;
				for ( AbstractInsnNode value : nodes ) {
					if ( value.getOpcode() == -1 ) {
						continue;
					} else if ( value instanceof FieldInsnNode fieldInsnNode ) {
						Type type = Type.getType( fieldInsnNode.desc );
						delta += switch ( fieldInsnNode.getOpcode() ) {
							case Opcodes.GETSTATIC -> type.getSize();
							case Opcodes.PUTSTATIC -> -type.getSize();
							case Opcodes.GETFIELD -> type.getSize() - 1;
							case Opcodes.PUTFIELD -> -type.getSize() - 1;
							default -> throw new IllegalStateException();
						};
					} else if ( value instanceof MethodInsnNode methodInsnNode ) {
						Type type = Type.getMethodType( methodInsnNode.desc );
						for ( Type argument : type.getArgumentTypes() ) {
							delta -= argument.getSize();
						}
						delta	+= type.getReturnType().getSize();
						delta	+= switch ( methodInsnNode.getOpcode() ) {
									case Opcodes.INVOKESTATIC -> 0;
									case Opcodes.INVOKEVIRTUAL, Opcodes.INVOKEINTERFACE, Opcodes.INVOKESPECIAL -> -1;
									default -> throw new IllegalStateException();
								};
					} else if ( value instanceof InvokeDynamicInsnNode invokeDynamicInsnNode ) {
						Type type = Type.getMethodType( invokeDynamicInsnNode.desc );
						for ( Type argument : type.getArgumentTypes() ) {
							delta -= argument.getSize();
						}
						delta += type.getReturnType().getSize();
					} else if ( value instanceof LdcInsnNode ldcInsnNode ) {
						delta += ldcInsnNode.cst instanceof Double || ldcInsnNode.cst instanceof Long ? 2 : 1;
					} else if ( value instanceof MultiANewArrayInsnNode multiANewArrayInsnNode ) {
						delta -= multiANewArrayInsnNode.dims + 1;
					} else {
						if ( STACK_SIZE_DELTA[ value.getOpcode() ] == Integer.MIN_VALUE ) {
							throw new IllegalStateException();
						}
						delta += STACK_SIZE_DELTA[ value.getOpcode() ];
					}
				}
				int expectation = switch ( returnValueContext ) {
					case EMPTY, EMPTY_UNLESS_JUMPING -> 0;
					case VALUE, VALUE_OR_NULL -> 1;
				};
				if ( expectation != delta ) {
					throw new IllegalStateException( node.getClass() + " with " + returnValueContext + " yielded a stack delta of " + delta );
				}
			}
			return nodes;
		}
		throw new IllegalStateException( "unsupported: " + node.getClass().getSimpleName() + " : " + node.getSourceText() );
	}

	@Override
	public List<List<AbstractInsnNode>> transformProperties( Type declaringType, List<BoxProperty> properties, String sourceType ) {
		List<List<AbstractInsnNode>>	members			= new ArrayList<>();
		List<List<AbstractInsnNode>>	getterLookup	= new ArrayList<>();
		List<List<AbstractInsnNode>>	setterLookup	= new ArrayList<>();
		properties.forEach( prop -> {
			List<AbstractInsnNode>	documentationStruct	= transformDocumentation( prop.getDocumentation() );
			/*
			 * normalize annotations to allow for
			 * property String userName;
			 */
			List<BoxAnnotation>		finalAnnotations	= normlizePropertyAnnotations( prop );
			// Start wiith all inline annotatinos

			BoxAnnotation			nameAnnotation		= finalAnnotations.stream().filter( it -> it.getKey().getValue().equalsIgnoreCase( "name" ) )
			    .findFirst()
			    .orElseThrow( () -> new ExpressionException( "Property [" + prop.getSourceText() + "] missing name annotation", prop ) );
			BoxAnnotation			typeAnnotation		= finalAnnotations.stream().filter( it -> it.getKey().getValue().equalsIgnoreCase( "type" ) )
			    .findFirst()
			    .orElseThrow( () -> new ExpressionException( "Property [" + prop.getSourceText() + "] missing type annotation", prop ) );
			BoxAnnotation			defaultAnnotation	= finalAnnotations.stream().filter( it -> it.getKey().getValue().equalsIgnoreCase( "default" ) )
			    .findFirst()
			    .orElse( null );

			// List<AbstractInsnNode> defaultLiteral = List.of( new InsnNode( Opcodes.ACONST_NULL ) );
			// List<AbstractInsnNode> defaultExpression = List.of( new InsnNode( Opcodes.ACONST_NULL ) );
			// if ( defaultAnnotation.getValue() != null ) {
			// if ( defaultAnnotation.getValue().isLiteral() ) {
			// defaultLiteral = transform( defaultAnnotation.getValue(), TransformerContext.NONE );
			// } else {
			// defaultExpression = AsmHelper.getDefaultExpression( this, defaultAnnotation.getValue() );
			// }
			// }

			List<AbstractInsnNode>	annotationStruct	= transformAnnotations( finalAnnotations );
			List<AbstractInsnNode>	init, initLambda;
			if ( defaultAnnotation.getValue() != null ) {

				if ( defaultAnnotation.getValue().isLiteral() ) {
					init		= transform( defaultAnnotation.getValue(), TransformerContext.NONE, ReturnValueContext.EMPTY );
					initLambda	= List.of( new InsnNode( Opcodes.ACONST_NULL ) );
				} else {
					init = List.of( new InsnNode( Opcodes.ACONST_NULL ) );

					Type					type		= Type.getType( "L" + getProperty( "packageName" ).replace( '.', '/' )
					    + "/" + getProperty( "classname" )
					    + "$Lambda_" + incrementAndGetLambdaCounter() + ";" );

					List<AbstractInsnNode>	body		= transform( defaultAnnotation.getValue(), TransformerContext.NONE, ReturnValueContext.EMPTY );
					ClassNode				classNode	= new ClassNode();
					AsmHelper.init( classNode, false, type, Type.getType( Object.class ), methodVisitor -> {
					}, Type.getType( DefaultExpression.class ) );
					AsmHelper.methodWithContextAndClassLocator( classNode, "evaluate", Type.getType( IBoxContext.class ), Type.getType( Object.class ), false,
					    this, false,
					    () -> body );
					setAuxiliary( type.getClassName(), classNode );

					initLambda = List.of(
					    new TypeInsnNode( Opcodes.NEW, type.getInternalName() ),
					    new InsnNode( Opcodes.DUP ),
					    new MethodInsnNode( Opcodes.INVOKESPECIAL, type.getInternalName(), "<init>", Type.getMethodDescriptor( Type.VOID_TYPE ), false ) );
				}
			} else {
				init		= List.of( new InsnNode( Opcodes.ACONST_NULL ) );
				initLambda	= List.of( new InsnNode( Opcodes.ACONST_NULL ) );
			}

			String	type;
			// name and type must be simple values
			String	name;
			if ( nameAnnotation != null && nameAnnotation.getValue() instanceof BoxStringLiteral namelit ) {
				name = namelit.getValue().trim();
				if ( name.isEmpty() )
					throw new ExpressionException( "Property [" + prop.getSourceText() + "] name cannot be empty", nameAnnotation );
			} else {
				throw new ExpressionException( "Property [" + prop.getSourceText() + "] name must be a simple value", nameAnnotation );
			}
			if ( typeAnnotation != null && typeAnnotation.getValue() instanceof BoxStringLiteral typelit ) {
				type = typelit.getValue().trim();
				if ( type.isEmpty() )
					throw new ExpressionException( "Property [" + prop.getSourceText() + "] type cannot be empty", typeAnnotation );
			} else {
				throw new ExpressionException( "Property [" + prop.getSourceText() + "] type must be a simple value", typeAnnotation );
			}
			List<AbstractInsnNode>	jNameKey	= createKey( name );
			List<AbstractInsnNode>	jGetNameKey	= createKey( "get" + name );
			List<AbstractInsnNode>	jSetNameKey	= createKey( "set" + name );

			List<AbstractInsnNode>	javaExpr	= new ArrayList<>();
			javaExpr.add( new TypeInsnNode( Opcodes.NEW, Type.getInternalName( Property.class ) ) );
			javaExpr.add( new InsnNode( Opcodes.DUP ) );
			javaExpr.addAll( jNameKey );
			javaExpr.add( new LdcInsnNode( type ) );
			javaExpr.addAll( init );
			javaExpr.addAll( initLambda );
			javaExpr.addAll( annotationStruct );
			javaExpr.addAll( documentationStruct );

			javaExpr.add( new FieldInsnNode( Opcodes.GETSTATIC,
			    Type.getInternalName( BoxSourceType.class ),
			    sourceType.toUpperCase(),
			    Type.getDescriptor( BoxSourceType.class ) ) );

			javaExpr.add( new MethodInsnNode( Opcodes.INVOKESPECIAL,
			    Type.getInternalName( Property.class ),
			    "<init>",
			    Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( Key.class ), Type.getType( String.class ), Type.getType( Object.class ),
			        Type.getType( DefaultExpression.class ), Type.getType( IStruct.class ), Type.getType( IStruct.class ),
			        Type.getType( BoxSourceType.class ) ),
			    false ) );

			members.add( jNameKey );
			members.add( javaExpr );

			// Check if getter key annotation is defined in finalAnnotations and false. I don't love this as annotations can technically be any literal
			boolean getter = !finalAnnotations.stream()
			    .anyMatch( it -> it.getKey().getValue().equalsIgnoreCase( "getter" ) && !BooleanCaster.cast( getBoxExprAsString( it.getValue() ) ) );
			if ( getter ) {
				getterLookup.add( jGetNameKey );
				List<AbstractInsnNode> get = new ArrayList<>();
				get.add( new FieldInsnNode( Opcodes.GETSTATIC, declaringType.getInternalName(), "properties", Type.getDescriptor( Map.class ) ) );
				get.addAll( jNameKey );
				get.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE, Type.getInternalName( Map.class ), "get",
				    Type.getMethodDescriptor( Type.getType( Object.class ), Type.getType( Object.class ) ), true ) );
				getterLookup.add( get );
			}
			// Check if setter key annotation is defined in finalAnnotations and false. I don't love this as annotations can technically be any literal
			boolean setter = !finalAnnotations.stream()
			    .anyMatch( it -> it.getKey().getValue().equalsIgnoreCase( "setter" ) && !BooleanCaster.cast( getBoxExprAsString( it.getValue() ) ) );
			if ( setter ) {
				setterLookup.add( jSetNameKey );
				List<AbstractInsnNode> set = new ArrayList<>();
				set.add( new FieldInsnNode( Opcodes.GETSTATIC, declaringType.getInternalName(), "properties", Type.getDescriptor( Map.class ) ) );
				set.addAll( jNameKey );
				set.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE, Type.getInternalName( Map.class ), "get",
				    Type.getMethodDescriptor( Type.getType( Object.class ), Type.getType( Object.class ) ), true ) );
				setterLookup.add( set );
			}
		} );
		if ( members.isEmpty() ) {
			List<AbstractInsnNode>	linked	= List.of(
			    new LdcInsnNode( 0 ),
			    new TypeInsnNode( Opcodes.ANEWARRAY, Type.getInternalName( Object.class ) ),
			    new MethodInsnNode( Opcodes.INVOKESTATIC,
			        Type.getInternalName( MapHelper.class ),
			        "LinkedHashMapOfProperties",
			        Type.getMethodDescriptor( Type.getType( Map.class ), Type.getType( Object[].class ) ),
			        false ) );
			List<AbstractInsnNode>	hashed	= List.of(
			    new LdcInsnNode( 0 ),
			    new TypeInsnNode( Opcodes.ANEWARRAY, Type.getInternalName( Object.class ) ),
			    new MethodInsnNode( Opcodes.INVOKESTATIC,
			        Type.getInternalName( MapHelper.class ),
			        "HashMapOfProperties",
			        Type.getMethodDescriptor( Type.getType( Map.class ), Type.getType( Object[].class ) ),
			        false ) );
			return List.of( linked, hashed, hashed );
		} else {
			List<AbstractInsnNode> propertiesStruct = new ArrayList<>();
			propertiesStruct.addAll( AsmHelper.array( Type.getType( Object.class ), members ) );
			propertiesStruct.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( MapHelper.class ),
			    "LinkedHashMapOfProperties",
			    Type.getMethodDescriptor( Type.getType( Map.class ), Type.getType( Object[].class ) ),
			    false ) );
			List<AbstractInsnNode> getterStruct = new ArrayList<>();
			getterStruct.addAll( AsmHelper.array( Type.getType( Object.class ), getterLookup ) );
			getterStruct.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( MapHelper.class ),
			    "HashMapOfProperties",
			    Type.getMethodDescriptor( Type.getType( Map.class ), Type.getType( Object[].class ) ),
			    false ) );
			List<AbstractInsnNode> setterStruct = new ArrayList<>();
			setterStruct.addAll( AsmHelper.array( Type.getType( Object.class ), setterLookup ) );
			setterStruct.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( MapHelper.class ),
			    "HashMapOfProperties",
			    Type.getMethodDescriptor( Type.getType( Map.class ), Type.getType( Object[].class ) ),
			    false ) );
			return List.of( propertiesStruct, getterStruct, setterStruct );
		}
	}

	public static List<BoxAnnotation> normlizePropertyAnnotations( BoxProperty prop ) {

		/**
		 * normalize annotations to allow for
		 * property String userName;
		 * This means all inline and pre annotations are treated as post annotations
		 */
		List<BoxAnnotation>	finalAnnotations	= new ArrayList<>();
		// Start wiith all inline annotatinos
		List<BoxAnnotation>	annotations			= prop.getPostAnnotations();
		// Add in any pre annotations that have a value, which allows type, name, or default to be set before
		annotations.addAll( prop.getAnnotations().stream().filter( it -> it.getValue() != null ).toList() );

		// Find the position of the name, type, and default annotations
		int					namePosition			= annotations.stream()
		    .filter( it -> it.getKey().getValue().equalsIgnoreCase( "name" ) && it.getValue() != null )
		    .findFirst()
		    .map( annotations::indexOf ).orElse( -1 );
		int					typePosition			= annotations.stream()
		    .filter( it -> it.getKey().getValue().equalsIgnoreCase( "type" ) && it.getValue() != null )
		    .findFirst()
		    .map( annotations::indexOf ).orElse( -1 );
		int					defaultPosition			= annotations.stream()
		    .filter( it -> it.getKey().getValue().equalsIgnoreCase( "default" ) && it.getValue() != null )
		    .findFirst()
		    .map( annotations::indexOf ).orElse( -1 );

		// Count the number of non-valued keys to determine how to handle them by position later
		int					numberOfNonValuedKeys	= ( int ) annotations.stream()
		    .map( BoxAnnotation::getValue )
		    .filter( Objects::isNull )
		    .count();
		List<BoxAnnotation>	nonValuedKeys			= annotations.stream()
		    .filter( it -> it.getValue() == null )
		    .collect( java.util.stream.Collectors.toList() );

		// Find the name, type, and default annotations
		BoxAnnotation		nameAnnotation			= null;
		BoxAnnotation		typeAnnotation			= null;
		BoxAnnotation		defaultAnnotation		= null;
		if ( namePosition > -1 )
			nameAnnotation = annotations.get( namePosition );
		if ( typePosition > -1 )
			typeAnnotation = annotations.get( typePosition );
		if ( defaultPosition > -1 )
			defaultAnnotation = annotations.get( defaultPosition );

		/**
		 * If there is no name, if there is more than one nonvalued keys and no type, use the first nonvalued key
		 * as the type and second nonvalued key as the name. Otherwise, if there are more than one non-valued key, use the first as the name.
		 */
		if ( namePosition == -1 ) {
			if ( numberOfNonValuedKeys > 1 && typePosition == -1 ) {
				typeAnnotation	= new BoxAnnotation( new BoxFQN( "type", null, null ),
				    new BoxStringLiteral( nonValuedKeys.get( 0 ).getKey().getValue(), null, null ), null,
				    null );
				nameAnnotation	= new BoxAnnotation( new BoxFQN( "name", null, null ),
				    new BoxStringLiteral( nonValuedKeys.get( 1 ).getKey().getValue(), null, null ), null,
				    null );
				finalAnnotations.add( nameAnnotation );
				finalAnnotations.add( typeAnnotation );
				annotations.remove( nonValuedKeys.get( 0 ) );
				annotations.remove( nonValuedKeys.get( 1 ) );
			} else if ( numberOfNonValuedKeys > 0 ) {
				nameAnnotation = new BoxAnnotation( new BoxFQN( "name", null, null ),
				    new BoxStringLiteral( nonValuedKeys.get( 0 ).getKey().getValue(), null, null ), null,
				    null );
				finalAnnotations.add( nameAnnotation );
				annotations.remove( nonValuedKeys.get( 0 ) );
			} else {
				throw new ExpressionException( "Property [" + prop.getSourceText() + "] has no name", prop );
			}
		}

		// add type with value of any if not present
		if ( typeAnnotation == null ) {
			typeAnnotation = new BoxAnnotation( new BoxFQN( "type", null, null ), new BoxStringLiteral( "any", null, null ), null,
			    null );
			finalAnnotations.add( typeAnnotation );
		}

		// add default with value of null if not present
		if ( defaultPosition == -1 ) {
			defaultAnnotation = new BoxAnnotation( new BoxFQN( "default", null, null ), new BoxNull( null, null ), null,
			    null );
			finalAnnotations.add( defaultAnnotation );
		}

		// add remaining annotations
		finalAnnotations.addAll( annotations );
		// Now that name, type, and default are finalized, add in any remaining non-valued keys
		finalAnnotations.addAll( prop.getAnnotations().stream().filter( it -> it.getValue() == null ).toList() );

		return finalAnnotations;
	}

	private static String getBoxExprAsString( BoxExpression expr ) {
		if ( expr == null ) {
			return "";
		}
		if ( expr instanceof BoxStringLiteral str ) {
			return str.getValue();
		}
		if ( expr instanceof BoxBooleanLiteral bool ) {
			return bool.getValue() ? "true" : "false";
		} else {
			throw new BoxRuntimeException( "Unsupported BoxExpr type: " + expr.getClass().getSimpleName() );
		}
	}

	public List<AbstractInsnNode> createAbstractFunction( BoxFunctionDeclaration func ) {
		List<AbstractInsnNode> nodes = new ArrayList<AbstractInsnNode>();

		// public AbstractFunction( Key name, Argument[] arguments, String returnType, Access access, IStruct annotations, IStruct documentation,
		// String sourceObjectName, String sourceObjectType ) {
		// this.name = name;
		// this.arguments = arguments;
		// this.returnType = returnType;
		// this.access = access;
		// this.annotations = annotations;
		// this.documentation = documentation;
		// this.sourceObjectName = sourceObjectName;
		// this.sourceObjectType = sourceObjectType;
		// }

		nodes.add( new TypeInsnNode( Opcodes.NEW, Type.getInternalName( AbstractFunction.class ) ) );
		nodes.add( new InsnNode( Opcodes.DUP ) );

		// args
		// Key name
		nodes.addAll( createKey( func.getName() ) );
		// Argument[] arguments
		List<List<AbstractInsnNode>> argList = func.getArgs()
		    .stream()
		    .map( arg -> transform( arg, TransformerContext.NONE ) )
		    .toList();
		nodes.addAll( AsmHelper.array( Type.getType( Argument.class ), argList ) );
		// String returnType
		nodes.addAll( transform( func.getType(), TransformerContext.NONE ) );
		// Access access
		nodes.add(
		    new FieldInsnNode(
		        Opcodes.GETSTATIC,
		        Type.getDescriptor( Function.Access.class ),
		        func.getAccessModifier().name().toUpperCase(),
		        Type.getDescriptor( Function.Access.class )
		    )
		);
		// IStruct annotations
		// TODO
		nodes.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    Type.getInternalName( Struct.class ),
		    "EMPTY",
		    Type.getDescriptor( IStruct.class ) ) );
		// IStruct documentation
		// TODO
		nodes.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    Type.getInternalName( Struct.class ),
		    "EMPTY",
		    Type.getDescriptor( IStruct.class ) ) );
		// String sourceObjectName
		nodes.add( new LdcInsnNode( getProperty( "boxClassName" ) ) );
		// String sourceObjectType
		nodes.add( new LdcInsnNode( "class" ) );

		nodes.add(
		    new MethodInsnNode(
		        Opcodes.INVOKESPECIAL,
		        Type.getInternalName( ClassVariablesScope.class ),
		        "<init>",
		        Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( IClassRunnable.class ) ),
		        false
		    )
		);

		return nodes;
	}

	private List<AbstractInsnNode> generateMapOfAbstractMethodNames( BoxClass boxClass ) {
		List<List<AbstractInsnNode>>	methodKeyLists	= boxClass.getDescendantsOfType( BoxFunctionDeclaration.class )
		    .stream()
		    .filter( func -> func.getBody() == null )
		    .map( func -> {
															    List<List<AbstractInsnNode>> absFunc = List.of(
															        createKey( func.getName() ),
															        createAbstractFunction( func )
															    );

															    return absFunc;
														    } )
		    .flatMap( x -> x.stream() )
		    .collect( java.util.stream.Collectors.toList() );

		List<AbstractInsnNode>			nodes			= new ArrayList<AbstractInsnNode>();

		nodes.addAll( AsmHelper.array( Type.getType( Key.class ), methodKeyLists ) );

		nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
		    Type.getInternalName( MapHelper.class ),
		    "LinkedHashMapOfProperties",
		    Type.getMethodDescriptor( Type.getType( Map.class ), Type.getType( Object[].class ) ),
		    false ) );

		return nodes;
	}

	private List<AbstractInsnNode> generateSetOfCompileTimeMethodNames( BoxClass boxClass ) {
		List<List<AbstractInsnNode>>	methodKeyLists	= boxClass.getDescendantsOfType( BoxFunctionDeclaration.class )
		    .stream()
		    .map( BoxFunctionDeclaration::getName )
		    .map( this::createKey )
		    .collect( java.util.stream.Collectors.toList() );

		List<AbstractInsnNode>			nodes			= new ArrayList<AbstractInsnNode>();

		nodes.addAll( AsmHelper.array( Type.getType( Key.class ), methodKeyLists ) );
		nodes.add(
		    new MethodInsnNode(
		        Opcodes.INVOKESTATIC,
		        Type.getInternalName( Set.class ),
		        "of",
		        Type.getMethodDescriptor( Type.getType( Set.class ), Type.getType( Object[].class ) ),
		        true
		    )
		);

		return nodes;

	}
}
