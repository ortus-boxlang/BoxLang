package ortus.boxlang.compiler.asmboxpiler;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxDocumentationAnnotation;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

import java.util.*;

public abstract class Transpiler implements ITranspiler {

	private final HashMap<String, String>	properties		= new HashMap<String, String>();
	private Map<String, BoxExpression>		keys			= new LinkedHashMap<String, BoxExpression>();
	private Map<String, ClassNode>			auxiliaries		= new LinkedHashMap<String, ClassNode>();
	private int								lambdaCounter	= 0;
	private Map<String, LabelNode>			breaks			= new LinkedHashMap<>();
	private List<List<AbstractInsnNode>>			imports			= new ArrayList<>();
	private List<ImportDefinition>			definitions			= new ArrayList<>();

	/**
	 * Set a property
	 *
	 * @param key   key of the Property
	 * @param value value of the Property
	 */
	public void setProperty( String key, String value ) {
		properties.put( key, value );
	}

	/**
	 * Get a Propoerty
	 *
	 * @param key key of the Property
	 *
	 * @return the value of the property or null if not defined
	 */
	public String getProperty( String key ) {
		return ( String ) properties.get( key );
	}

	public static Transpiler getTranspiler() {
		return new AsmTranspiler();
	}

	public abstract List<AbstractInsnNode> transform( BoxNode node, TransformerContext context );

	public int registerKey( BoxExpression key ) {
		String name;
		if ( key instanceof BoxStringLiteral str ) {
			name = str.getValue();
		} else if ( key instanceof BoxIntegerLiteral intr ) {
			name = intr.getValue();
		} else {
			throw new IllegalStateException( "Key must be a string or integer literal" );
		}
		// check if exists
		if ( keys.containsKey( name ) ) {
			return new ArrayList<>( keys.keySet() ).indexOf( name );
		}
		keys.put( name, key );
		return keys.size() - 1;
	}

	public Map<String, BoxExpression> getKeys() {
		return keys;
	}

	public Map<String, ClassNode> getAuxiliary() {
		return auxiliaries;
	}

	public void setAuxiliary( String name, ClassNode classNode ) {
		if ( auxiliaries.putIfAbsent( name, classNode ) != null ) {
			// throw new IllegalArgumentException( "Auxiliary already registered: " + name );
		}
	}

	public int incrementAndGetLambdaCounter() {
		return ++lambdaCounter;
	}

	public List<AbstractInsnNode> createKey( BoxExpression expr ) {
		// If this key is a literal, we can optimize it
		if ( expr instanceof BoxStringLiteral || expr instanceof BoxIntegerLiteral ) {
			int pos = registerKey( expr );
			// Instead of Key.of(), we'll reference a static array of pre-created keys on the class
			return List.of( new FieldInsnNode(
			    Opcodes.GETSTATIC,
			    getProperty( "packageName" ).replace( '.', '/' )
			        + "/"
			        + getProperty( "classname" ),
			    "keys",
			    Type.getDescriptor( Key[].class ) ), new LdcInsnNode( pos ), new InsnNode( Opcodes.AALOAD ) );
		} else {
			// TODO: likely needs to retain return type info on transformed expression or extract from "expr"
			// Dynamic values will be created at runtime
			List<AbstractInsnNode> nodes = new ArrayList<>();
			nodes.addAll( transform( expr, TransformerContext.NONE ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( Key.class ),
			    "of",
			    Type.getMethodDescriptor( Type.getType( Key.class ), Type.getType( Object.class ) ),
			    false ) );
			return nodes;
		}
	}

	public List<AbstractInsnNode> createKey( String expr ) {
		return createKey( new BoxStringLiteral( expr, null, expr ) );
	}

	public List<AbstractInsnNode> transformDocumentation( List<BoxDocumentationAnnotation> documentation ) {
		List<List<AbstractInsnNode>> members = new ArrayList<>();
		documentation.forEach( doc -> {
			List<AbstractInsnNode> annotationKey = createKey( doc.getKey().getValue() );
			members.add( annotationKey );
			List<AbstractInsnNode> value = transform( doc.getValue(), TransformerContext.NONE );
			members.add( value );
		} );
		if ( members.isEmpty() ) {
			return List.of( new FieldInsnNode( Opcodes.GETSTATIC,
			    Type.getInternalName( Struct.class ),
			    "EMPTY",
			    Type.getDescriptor( IStruct.class ) ) );
		} else {
			List<AbstractInsnNode> nodes = new ArrayList<>();
			nodes.addAll( AsmHelper.array( Type.getType( Object.class ), members ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( Struct.class ),
			    "linkedOf",
			    Type.getMethodDescriptor( Type.getType( IStruct.class ), Type.getType( Object[].class ) ),
			    false ) );
			return nodes;
		}
	}

	public List<AbstractInsnNode> transformAnnotations( List<BoxAnnotation> annotations, Boolean defaultTrue, boolean onlyLiteralValues ) {
		List<List<AbstractInsnNode>> members = new ArrayList<>();
		annotations.forEach( annotation -> {
			List<AbstractInsnNode> annotationKey = createKey( annotation.getKey().getValue() );
			members.add( annotationKey );
			BoxExpression			thisValue	= annotation.getValue();
			List<AbstractInsnNode>	value;
			if ( thisValue != null ) {
				// Literal values are transformed directly
				if ( thisValue.isLiteral() ) {
					value = transform( thisValue, TransformerContext.NONE );
				} else if ( onlyLiteralValues ) {
					// Runtime expressions we just put this place holder text in for
					value = List.of( new LdcInsnNode( "<Runtime Expression>" ) );
				} else {
					value = transform( thisValue, TransformerContext.NONE );
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
			List<AbstractInsnNode> nodes = new ArrayList<>();
			nodes.addAll( AsmHelper.array( Type.getType( Object.class ), members ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( Struct.class ),
			    "linkedOf",
			    Type.getMethodDescriptor( Type.getType( IStruct.class ), Type.getType( Object[].class ) ),
			    false ) );
			return nodes;
		}
	}

	public List<AbstractInsnNode> transformAnnotations( List<BoxAnnotation> annotations ) {
		return transformAnnotations( annotations, false, true );
	}

	public LabelNode getCurrentBreak( String label ) {
		return breaks.get( label == null ? "" : label );
	}

	public void setCurrentBreak( String label, LabelNode labelNode ) {
		this.breaks.put( label == null ? "" : label, labelNode );
	}

	public void removeCurrentBreak( String label ) {
		this.breaks.remove( label == null ? "" : label );
	}

	public void addImport(BoxExpression expression, BoxIdentifier alias) {
		imports.add(transform( expression, TransformerContext.RIGHT ) );
		definitions.add(ImportDefinition.parse(alias == null ? expression.toString() : (expression + " as " + alias)));
	}

	public List<List<AbstractInsnNode>> getImports() {
		return imports;
	}

	public boolean matchesImport(String token) {
		/*
		 * Not supporting
		 * - java:System
		 * - java:java.lang.System
		 * - java.lang.System
		 *
		 * right now, just
		 *
		 * - System
		 *
		 * as all the other options require grammar changes or are more complicated to recognize
		 */
		return definitions.stream().anyMatch( i -> token.equalsIgnoreCase( i.alias() ) || token.equalsIgnoreCase( i.className() ) );
	}
}
