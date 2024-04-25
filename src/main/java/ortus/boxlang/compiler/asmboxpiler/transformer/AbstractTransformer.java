package ortus.boxlang.compiler.asmboxpiler.transformer;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxDocumentationAnnotation;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTransformer implements Transformer {

	protected Transpiler	transpiler;
	protected Logger		logger;

	public AbstractTransformer( Transpiler transpiler ) {
		this.transpiler	= transpiler;
		this.logger		= LoggerFactory.getLogger( this.getClass() );
	}

	protected List<AbstractInsnNode> createKey( BoxExpression expr ) {
		// If this key is a literal, we can optimize it
		if ( expr instanceof BoxStringLiteral || expr instanceof BoxIntegerLiteral ) {
			int pos = transpiler.registerKey( expr );
			// Instead of Key.of(), we'll reference a static array of pre-created keys on the class
			return List.of( new FieldInsnNode(
			    Opcodes.GETSTATIC,
			    transpiler.getProperty( "packageName" ).replace( '.', '/' )
			        + "/"
			        + transpiler.getProperty( "classname" ),
			    "keys",
			    Type.getDescriptor( Key[].class ) ), new LdcInsnNode( pos ), new InsnNode( Opcodes.AALOAD ) );
		} else {
			// TODO: likely needs to retain return type info on transformed expression or extract from "expr"
			// Dynamic values will be created at runtime
			List<AbstractInsnNode> nodes = new ArrayList<>();
			nodes.addAll( transpiler.transform( expr ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( Key.class ),
			    "of",
			    Type.getMethodDescriptor( Type.getType( Key.class ), Type.getType( Object.class ) ),
			    false ) );
			return nodes;
		}
	}

	protected List<AbstractInsnNode> createKey( String expr ) {
		return createKey( new BoxStringLiteral( expr, null, expr ) );
	}

	protected List<AbstractInsnNode> transformDocumentation( List<BoxDocumentationAnnotation> documentation ) {
		List<List<AbstractInsnNode>> members = new ArrayList<>();
		documentation.forEach( doc -> {
			List<AbstractInsnNode> annotationKey = createKey( doc.getKey().getValue() );
			members.add( annotationKey );
			List<AbstractInsnNode> value = transpiler.transform( doc.getValue() );
			members.add( value );
		} );
		if ( members.isEmpty() ) {
			return List.of( new FieldInsnNode( Opcodes.GETSTATIC,
			    Type.getInternalName( Struct.class ),
			    "EMPTY",
			    Type.getDescriptor( IStruct.class ) ) );
		} else {
			;
			List<AbstractInsnNode> nodes = new ArrayList<>();
			nodes.addAll( AsmHelper.array( Type.getType( Object.class ), members ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( Struct.class ),
			    "linkedOf",
			    Type.getMethodDescriptor( Type.getType( Struct.class ) ),
			    false ) );
			return nodes;
		}
	}

	protected List<AbstractInsnNode> transformAnnotations( List<BoxAnnotation> annotations, Boolean defaultTrue, boolean onlyLiteralValues ) {
		List<List<AbstractInsnNode>> members = new ArrayList<>();
		annotations.forEach( annotation -> {
			List<AbstractInsnNode> annotationKey = createKey( annotation.getKey().getValue() );
			members.add( annotationKey );
			BoxExpression			thisValue	= annotation.getValue();
			List<AbstractInsnNode>	value;
			if ( thisValue != null ) {
				// Literal values are transformed directly
				if ( thisValue.isLiteral() ) {
					value = transpiler.transform( thisValue );
				} else if ( onlyLiteralValues ) {
					// Runtime expressions we just put this place holder text in for
					value = List.of( new LdcInsnNode( "<Runtime Expression>" ) );
				} else {
					value = transpiler.transform( thisValue );
				}
			} else if ( defaultTrue ) {
				// Annotations in tags with no value default to true string (CF compat)
				value = List.of( new FieldInsnNode( Opcodes.GETSTATIC,
				    Type.getInternalName( Boolean.class ),
				    "TRUE",
				    Type.getDescriptor( Boolean.class ) ) );
			} else {
				// Annotations in script with no value default to empty string (CF compat)
				value = List.of( new LdcInsnNode( "" ) );
			}
			members.add( value );
		} );
		if ( annotations.isEmpty() ) {
			return List.of(
			    new TypeInsnNode( Opcodes.NEW, Type.getInternalName( Struct.class ) ),
			    new InsnNode( Opcodes.DUP ),
			    new MethodInsnNode( Opcodes.INVOKESPECIAL,
			        Type.getInternalName( Struct.class ),
			        "<init>",
			        Type.getMethodDescriptor( Type.VOID_TYPE ),
			        false )
			);
		} else {
			return List.of(
			    new MethodInsnNode( Opcodes.INVOKESTATIC,
			        Type.getInternalName( Struct.class ),
			        "linkedOf",
			        Type.getMethodDescriptor( Type.getType( Struct.class ) ),
			        false )
			);
		}
	}

	protected List<AbstractInsnNode> transformAnnotations( List<BoxAnnotation> annotations ) {
		return transformAnnotations( annotations, false, true );
	}
}
