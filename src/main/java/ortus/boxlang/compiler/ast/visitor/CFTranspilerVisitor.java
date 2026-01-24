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
package ortus.boxlang.compiler.ast.visitor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.BoxTemplate;
import ortus.boxlang.compiler.ast.SourceFile;
import ortus.boxlang.compiler.ast.comment.BoxSingleLineComment;
import ortus.boxlang.compiler.ast.expression.BoxAccess;
import ortus.boxlang.compiler.ast.expression.BoxArgument;
import ortus.boxlang.compiler.ast.expression.BoxArrayAccess;
import ortus.boxlang.compiler.ast.expression.BoxArrayLiteral;
import ortus.boxlang.compiler.ast.expression.BoxAssignment;
import ortus.boxlang.compiler.ast.expression.BoxAssignmentOperator;
import ortus.boxlang.compiler.ast.expression.BoxBinaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxBinaryOperator;
import ortus.boxlang.compiler.ast.expression.BoxBooleanLiteral;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxComparisonOperation;
import ortus.boxlang.compiler.ast.expression.BoxComparisonOperator;
import ortus.boxlang.compiler.ast.expression.BoxDotAccess;
import ortus.boxlang.compiler.ast.expression.BoxExpressionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxFunctionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxMethodInvocation;
import ortus.boxlang.compiler.ast.expression.BoxNew;
import ortus.boxlang.compiler.ast.expression.BoxParenthesis;
import ortus.boxlang.compiler.ast.expression.BoxScope;
import ortus.boxlang.compiler.ast.expression.BoxStringConcat;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStructLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStructType;
import ortus.boxlang.compiler.ast.expression.BoxTernaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxUnaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxUnaryOperator;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxBufferOutput;
import ortus.boxlang.compiler.ast.statement.BoxDo;
import ortus.boxlang.compiler.ast.statement.BoxDocumentationAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.ast.statement.BoxForIndex;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxIfElse;
import ortus.boxlang.compiler.ast.statement.BoxProperty;
import ortus.boxlang.compiler.ast.statement.BoxReturn;
import ortus.boxlang.compiler.ast.statement.BoxScriptIsland;
import ortus.boxlang.compiler.ast.statement.BoxStatementBlock;
import ortus.boxlang.compiler.ast.statement.BoxSwitch;
import ortus.boxlang.compiler.ast.statement.BoxTryCatch;
import ortus.boxlang.compiler.ast.statement.BoxWhile;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;
import ortus.boxlang.compiler.ast.statement.component.BoxTemplateIsland;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.ModuleService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.util.ListUtil;
import ortus.boxlang.runtime.util.RegexBuilder;

/**
 * CF Transpiler Visitor for BoxLang AST Transformation
 *
 * This visitor transforms CFML/ColdFusion syntax into BoxLang-compatible syntax by traversing
 * the Abstract Syntax Tree (AST) and performing various transformations including:
 *
 * <ul>
 * <li>Renaming CF-specific variables and identifiers (cfcatch -> bxcatch, thisTag -> thisComponent)</li>
 * <li>Converting CF BIFs to BoxLang equivalents (chr -> char, asc -> ascii)</li>
 * <li>Transforming component names and attributes</li>
 * <li>Handling case-sensitivity requirements (uppercase struct keys)</li>
 * <li>Converting operator precedence issues</li>
 * <li>Merging documentation annotations</li>
 * <li>Adding output annotations for compatibility</li>
 * </ul>
 *
 * <h2>Architecture</h2>
 * The visitor extends {@link ReplacingBoxVisitor} and uses the visitor pattern to traverse
 * the AST. Each visit method handles a specific type of AST node and applies the necessary
 * transformations.
 *
 * <h2>Configuration</h2>
 * The transpiler can be configured via module settings in the compat module:
 *
 * <pre>
 * {
 *   "transpiler": {
 *     "upperCaseKeys": true,           // Convert struct keys to uppercase
 *     "forceOutputTrue": true,         // Add @output true to functions/classes
 *     "mergeDocsIntoAnnotations": true, // Convert doc comments to annotations
 *     "isLucee": true                  // Enable Lucee-specific compatibility
 *   }
 * }
 * </pre>
 *
 * <h2>How to Add New Transformations</h2>
 *
 * <h3>1. Adding BIF Mappings</h3>
 * Add entries to the static {@code BIFMap} in the static initializer:
 *
 * <pre>
 * BIFMap.put( "oldname", "newname" );
 * </pre>
 *
 * <h3>2. Adding Variable Renamings</h3>
 * Add entries to the static {@code identifierMap}:
 *
 * <pre>
 * identifierMap.put( "cfvariable", "bxvariable" );
 * </pre>
 *
 * <h3>3. Adding Component Transformations</h3>
 * For component name changes, add to {@code componentMap}:
 *
 * <pre>
 * componentMap.put( "oldcomponent", "newcomponent" );
 * </pre>
 *
 * For attribute name changes, add to {@code componentAttrMap}:
 *
 * <pre>
 * componentAttrMap.put( "componentname", Map.of( "oldattr", "newattr" ) );
 * </pre>
 *
 * <h3>4. Adding New Visit Methods</h3>
 * Override visit methods for specific AST node types:
 *
 * <pre>
 * {@code @Override}
 * public BoxNode visit(YourNodeType node) {
 *     // Perform transformations
 *     return super.visit(node); // Continue traversal
 * }
 * </pre>
 *
 * <h3>5. BIF Return Type Fixes</h3>
 * For BIFs that should return the modified object instead of true/false,
 * add to {@code BIFReturnTypeFixSet}:
 *
 * <pre>
 * BIFReturnTypeFixSet.add( "bifname" );
 * </pre>
 *
 * @see ReplacingBoxVisitor
 * @see BoxNode
 *
 * @author BoxLang Team
 */
public class CFTranspilerVisitor extends ReplacingBoxVisitor {

	/**
	 * Static mappings for Built-In Function (BIF) name transformations.
	 * Maps CF function names to their BoxLang equivalents.
	 * All keys must be lowercase for consistent matching.
	 */
	private static Set<String>						BIFReturnTypeFixSet			= new HashSet<>();

	/**
	 * Maps CF BIF names to BoxLang BIF names.
	 * Used to rename function calls during transpilation.
	 */
	private static Map<String, String>				BIFMap						= new HashMap<>();

	/**
	 * Maps CF variable/identifier names to BoxLang equivalents.
	 * Used for renaming variables like cfcatch -> bxcatch.
	 */
	private static Map<String, String>				identifierMap				= new HashMap<>();

	/**
	 * Maps CF component names to BoxLang component names.
	 * Used for renaming component tags during transpilation.
	 */
	private static Map<String, String>				componentMap				= new HashMap<>();

	/**
	 * Maps component names to their attribute rename mappings.
	 * Outer key is component name, inner map is old->new attribute names.
	 */
	private static Map<String, Map<String, String>>	componentAttrMap			= new HashMap<>();

	/**
	 * Configuration keys for transpiler settings
	 */
	private static Key								transpilerKey				= Key.of( "transpiler" );
	private static Key								upperCaseKeysKey			= Key.of( "upperCaseKeys" );
	private static Key								forceOutputTrueKey			= Key.of( "forceOutputTrue" );
	private static Key								mergeDocsIntoAnnotationsKey	= Key.of( "mergeDocsIntoAnnotations" );
	private static Key								isLuceeKey					= Key.of( "isLucee" );
	private static Key								compatKey					= Key.of( "compat-cfml" );

	/**
	 * Runtime and service references
	 */
	private static BoxRuntime						runtime						= BoxRuntime.getInstance();
	private static ModuleService					moduleService				= runtime.getModuleService();

	/**
	 * Instance state variables
	 */
	private boolean									isClass						= false;
	private String									className					= "";
	private boolean									upperCaseKeys				= true;
	private boolean									forceOutputTrue				= true;
	private boolean									mergeDocsIntoAnnotations	= true;
	// If compat module is not installed, we assume this is a Lucee compat visitor
	// Users can toggle this by installing compat and setting the engine to "adobe"
	private boolean									isLuceeCompat				= true;

	private Set<BoxBinaryOperator>					binaryOpsHigherThanNot		= Set.of(
	    BoxBinaryOperator.Power,
	    BoxBinaryOperator.Star,
	    BoxBinaryOperator.Slash,
	    BoxBinaryOperator.Backslash,
	    BoxBinaryOperator.Mod,
	    BoxBinaryOperator.Plus,
	    BoxBinaryOperator.Minus,
	    BoxBinaryOperator.Contains,
	    BoxBinaryOperator.NotContains,
	    BoxBinaryOperator.BitwiseAnd,
	    BoxBinaryOperator.BitwiseOr,
	    BoxBinaryOperator.BitwiseXor,
	    BoxBinaryOperator.BitwiseSignedLeftShift,
	    BoxBinaryOperator.BitwiseSignedRightShift,
	    BoxBinaryOperator.BitwiseUnsignedRightShift
	);

	static {
		// ENSURE ALL KEYS ARE LOWERCASE FOR EASIER MATCHING
		BIFMap.put( "asc", "ascii" );
		BIFMap.put( "chr", "char" );
		BIFMap.put( "deserializejson", "JSONDeserialize" );
		BIFMap.put( "getapplicationsettings", "getApplicationMetadata" );
		BIFMap.put( "gettemplatepath", "getBaseTemplatePath" );
		BIFMap.put( "serializejson", "JSONSerialize" );
		BIFMap.put( "valuearray", "queryColumnData" );
		BIFMap.put( "objectSave", "objectSerialize" );
		BIFMap.put( "objectLoad", "objectDeserialize" );
		BIFMap.put( "querygetrow", "queryrowdata" );
		// valueList() and quotedValueList() are special cases below
		// queryColumnData().toList( delimiter )
		// queryColumnData().map().toList( delimiter )

		identifierMap.put( "cfthread", "bxthread" );
		identifierMap.put( "cfcatch", "bxcatch" );
		identifierMap.put( "cffile", "bxfile" );
		identifierMap.put( "cfftp", "bxftp" );
		identifierMap.put( "cfhttp", "bxhttp" );
		identifierMap.put( "cfquery", "bxquery" );
		identifierMap.put( "cfdocument", "bxdocument" );
		identifierMap.put( "cfstoredproc", "bxstoredproc" );
		identifierMap.put( "thistag", "thiscomponent" );

		/**
		 * These are components that have been renamed
		 */
		componentMap.put( "module", "component" );

		/*
		 * Outer string is name of component
		 * inner map is old attribute name to new attribute name
		 */
		componentAttrMap.put( "setting", Map.of( "enablecfoutputonly", "enableoutputonly" ) );
		componentAttrMap.put( "invoke", Map.of( "component", "class" ) );
		componentAttrMap.put( "procparam", Map.of( "cfsqltype", "sqltype" ) );
		componentAttrMap.put( "queryparam", Map.of( "cfsqltype", "sqltype" ) );
		componentAttrMap.put( "object", Map.of( "component", "className" ) );

		/*
		 * These are BIFs that return something useless like true, but would be much more useful to return the actual data structure.
		 */
		BIFReturnTypeFixSet.add( "arrayappend" );
		BIFReturnTypeFixSet.add( "arrayclear" );
		BIFReturnTypeFixSet.add( "arraydeleteat" );
		BIFReturnTypeFixSet.add( "arrayinsertat" );
		BIFReturnTypeFixSet.add( "arrayprepend" );
		BIFReturnTypeFixSet.add( "arrayresize" );
		BIFReturnTypeFixSet.add( "arrayset" );
		BIFReturnTypeFixSet.add( "arrayswap" );
		BIFReturnTypeFixSet.add( "structclear" );
		BIFReturnTypeFixSet.add( "structkeytranslate" );
		BIFReturnTypeFixSet.add( "structinsert" );
		BIFReturnTypeFixSet.add( "structdelete" );
		BIFReturnTypeFixSet.add( "structappend" );
		BIFReturnTypeFixSet.add( "querysetrow" );
		BIFReturnTypeFixSet.add( "querydeleterow" );
		BIFReturnTypeFixSet.add( "querysort" );
		BIFReturnTypeFixSet.add( "arraydelete" );

	}

	/**
	 * Constructor
	 */
	/**
	 * Constructor initializes the visitor with configuration settings from the BoxLang runtime.
	 * Reads transpiler-specific settings from the runtime configuration to customize
	 * the transpilation behavior.
	 *
	 * The constructor performs the following initialization:
	 * 1. Retrieves the transpiler configuration section from runtime settings
	 * 2. Sets up case sensitivity for generated keys
	 * 3. Configures output behavior for components
	 * 4. Determines documentation merge strategy
	 * 5. Checks for CFML compatibility mode
	 *
	 * Configuration options:
	 * - upperCaseKeys: Controls whether generated keys are uppercase (default: true)
	 * - forceOutputTrue: Forces components to have output=true (default: true)
	 * - mergeDocsIntoAnnotations: Merges documentation into annotations (default: true)
	 * - isLucee: Sets Lucee compatibility mode based on compat module presence
	 */
	public CFTranspilerVisitor() {
		// This may change when moving this visitor to the actual compat module
		this( moduleService.hasModule( compatKey ) ? StructCaster.cast( moduleService.getModuleSettings( compatKey ) ) : Struct.EMPTY );
	}

	/**
	 * Constructor with config
	 */
	public CFTranspilerVisitor( IStruct settings ) {
		if ( settings.containsKey( transpilerKey ) ) {
			settings = StructCaster.cast( settings.get( transpilerKey ) );
			if ( settings.containsKey( upperCaseKeysKey ) ) {
				upperCaseKeys = BooleanCaster.cast( settings.get( upperCaseKeysKey ) );
			}
			if ( settings.containsKey( forceOutputTrueKey ) ) {
				forceOutputTrue = BooleanCaster.cast( settings.get( forceOutputTrueKey ) );
			}
			if ( settings.containsKey( mergeDocsIntoAnnotationsKey ) ) {
				mergeDocsIntoAnnotations = BooleanCaster.cast( settings.get( mergeDocsIntoAnnotationsKey ) );
			}
			if ( settings.containsKey( isLuceeKey ) ) {
				isLuceeCompat = BooleanCaster.cast( settings.get( isLuceeKey ) );
			}
		}
	}

	/**
	 * Main entry point for AST transformation. Transforms a BoxLang AST into a CFML-compatible
	 * format by applying visitor pattern transformations to all nodes in the tree.
	 *
	 * This method orchestrates the entire transpilation process:
	 * 1. Resets internal state for the new transformation
	 * 2. Determines if the root node represents a class or script
	 * 3. Applies recursive transformations to all child nodes
	 * 4. Returns the transformed AST ready for code generation
	 *
	 * @param node The root AST node to transform (typically BoxScript or BoxClass)
	 *
	 * @return The transformed AST node with CFML-compatible structure
	 */
	@Override
	public BoxNode visit( BoxClass node ) {
		var annotations = node.getAnnotations();
		this.isClass = true;

		// We don't store the class name in the AST since it's based on the file name, so try and see the filename we compiled.
		if ( node.getPosition() != null && node.getPosition().getSource() != null && node.getPosition().getSource() instanceof SourceFile sf ) {
			File sourceFile = sf.getFile();
			className = sourceFile.getName().replaceFirst( "[.][^.]+$", "" );
		}

		mergeDocsIntoAnnotations( annotations, node.getDocumentation() );

		// Disable Accessors by default in CFML, unless this is a persistent ORM entity, then leave as-is because accessors needs to be enabled anyway
		if ( annotations.stream().noneMatch( a -> a.getKey().getValue().equalsIgnoreCase( "accessors" ) )
		    && annotations.stream().noneMatch( a -> a.getKey().getValue().equalsIgnoreCase( "persistent" ) ) ) {
			// @output true
			annotations.add(
			    new BoxAnnotation(
			        new BoxFQN( "accessors", null, null ),
			        new BoxBooleanLiteral( false, null, null ),
			        null,
			        null )
			);
		}

		enableOutput( annotations );
		return super.visit( node );
	}

	/**
	 * Transforms User Defined Function (UDF) declarations for CFML compatibility.
	 *
	 * Key transformations applied:
	 * - Merges documentation comments into function annotations
	 * - Enables output=true for functions (configurable)
	 * - Renames CFML-specific lifecycle methods (onCFCRequest -> onClassRequest)
	 * - Applies return type fixes for certain BIFs
	 *
	 * Special handling for lifecycle methods:
	 * - onCFCRequest becomes onClassRequest for BoxLang compatibility
	 *
	 * @param node The BoxFunctionDeclaration node to transform
	 *
	 * @return The transformed BoxFunctionDeclaration node
	 */
	@Override
	public BoxNode visit( BoxFunctionDeclaration node ) {
		mergeDocsIntoAnnotations( node.getAnnotations(), node.getDocumentation() );
		// Don't touch UDFs in a class, otherwise they won't inherit from the class's output annotation.
		if ( isClass ) {
			enableOutput( node.getAnnotations() );
			if ( node.getName().equalsIgnoreCase( "onCFCRequest" ) && className.equalsIgnoreCase( "application" ) ) {
				node.setName( "onClassRequest" );
			}
		}
		return super.visit( node );
	}

	/**
	 * Transpile Box Class properties
	 * - Merge documentation into annotations
	 */
	@Override
	public BoxNode visit( BoxProperty node ) {
		mergeDocsIntoAnnotations( node.getAnnotations(), node.getDocumentation() );
		return super.visit( node );
	}

	/**
	 * Transforms BoxIdentifier nodes by renaming CF-specific variables to BoxLang equivalents.
	 *
	 * This method handles the renaming of reserved CFML variables to their BoxLang counterparts:
	 * - cfcatch -> bxcatch
	 * - cfthread -> bxthread
	 * - cffile -> bxfile
	 * - etc.
	 *
	 * @param node The BoxIdentifier node to transform
	 *
	 * @return The transformed BoxIdentifier node with renamed variables
	 */
	@Override
	public BoxNode visit( BoxIdentifier node ) {
		renameTopLevelVars( node );
		return super.visit( node );
	}

	/**
	 * Transforms BoxArrayAccess nodes by renaming CF-specific variables in array access patterns.
	 *
	 * This method specifically handles string literal array access where CF variables are accessed:
	 * - variables["cfcatch"] becomes variables["bxcatch"]
	 * - variables["cfthread"] becomes variables["bxthread"]
	 * - etc.
	 *
	 * This ensures that code like `variables.cfcatch` or `variables["cfcatch"]` gets properly
	 * renamed to use BoxLang variable names.
	 *
	 * @param node The BoxArrayAccess node to transform
	 *
	 * @return The transformed BoxArrayAccess node with renamed variable references
	 */
	@Override
	public BoxNode visit( BoxArrayAccess node ) {
		if ( node.getAccess() instanceof BoxStringLiteral str ) {
			String name = str.getValue().toLowerCase();
			if ( identifierMap.containsKey( name ) ) {
				str.setValue( identifierMap.get( name ) );
			}
		}
		return super.visit( node );
	}

	/**
	 * Transforms BoxDotAccess nodes for CFML compatibility.
	 *
	 * Key transformations applied:
	 * - Renames specific properties (columnNames -> columnArray)
	 * - Converts dot access keys to uppercase (configurable via upperCaseKeys setting)
	 *
	 * Examples:
	 * - obj.columnNames becomes obj.columnArray
	 * - obj.myProperty becomes obj.MYPROPERTY (if upperCaseKeys is true)
	 *
	 * This maintains CFML's case-insensitive behavior while ensuring
	 * consistent key casing for compatibility.
	 *
	 * @param node The BoxDotAccess node to transform
	 *
	 * @return The transformed BoxDotAccess node
	 */
	@Override
	public BoxNode visit( BoxDotAccess node ) {
		// Make sure we upper case the dot access keys like in CFML
		upperCaseDotAceessKeys( node );
		return super.visit( node );
	}

	/**
	 * change foo.bar to foo.BAR
	 */
	private void upperCaseDotAceessKeys( BoxDotAccess node ) {
		if ( !upperCaseKeys )
			return;

		BoxExpression access = node.getAccess();
		if ( access instanceof BoxIdentifier id ) {
			id.setName( id.getName().toUpperCase() );
		}
	}

	/**
	 * Transforms BoxStructLiteral nodes by converting keys to uppercase for CFML compatibility.
	 *
	 * In CFML, struct keys are typically case-insensitive but displayed in uppercase.
	 * This transformation ensures consistent key casing:
	 * - { foo : 'bar' } becomes { FOO : 'bar' }
	 * - { myKey : 'value' } becomes { MYKEY : 'value' }
	 *
	 * The transformation is controlled by the upperCaseKeys configuration setting.
	 *
	 * @param node The BoxStructLiteral node to transform
	 *
	 * @return The transformed BoxStructLiteral node with uppercase keys
	 */
	public BoxNode visit( BoxStructLiteral node ) {
		upperCaseStructLiteralKeys( node );
		return super.visit( node );
	}

	/**
	 * change { foo : 'bar' } to { FOO : 'bar' }
	 */
	private void upperCaseStructLiteralKeys( BoxStructLiteral node ) {
		if ( !upperCaseKeys )
			return;

		// Only apply this logic to odd-numbered values
		for ( int i = 0; i < node.getValues().size(); i += 2 ) {
			BoxExpression key = node.getValues().get( i );
			if ( key instanceof BoxIdentifier id ) {
				id.setName( id.getName().toUpperCase() );
			} else if ( key instanceof BoxScope s ) {
				s.setName( s.getName().toUpperCase() );
			}
		}
	}

	/**
	 * Transforms BoxFunctionInvocation nodes by applying BIF (Built-In Function) mappings and
	 * special transformations for CFML compatibility.
	 *
	 * Key transformations applied:
	 * 1. BIF Name Mapping: Renames functions using the BIFMap (e.g., chr() -> char())
	 * 2. QueryExecute Parameter Transformation: Converts CFML query parameters to BoxLang format
	 * - Renames cfsqltype -> sqltype in parameter structs
	 * - Removes "cf_sql_" prefix from sqltype values
	 * 3. Special Function Handling: Applies custom transpilation logic for specific functions
	 *
	 * Examples:
	 * - chr(65) becomes char(65)
	 * - structKeyExists(obj, "key") gets special handling
	 * - queryExecute() parameters get normalized for BoxLang
	 *
	 * @param node The BoxFunctionInvocation node to transform
	 *
	 * @return The transformed BoxFunctionInvocation node or alternative representation
	 */
	@Override
	public BoxNode visit( BoxFunctionInvocation node ) {
		String name = node.getName().toLowerCase();
		if ( BIFMap.containsKey( name ) ) {
			node.setName( BIFMap.get( name ) );
		}
		// look for "params" named arg, or 2nd positional arg, and if it's a struct literal, any of the values which are also a struct literal,
		// rename any keys from cfsqltype to sqltype and remove "cf_sql_" from the values of any sqltype
		if ( name.equals( "queryexecute" ) && node.getArguments().size() >= 2 ) {
			BoxExpression params = null;
			// Check for positional args
			if ( !node.isNamedArgs() ) {
				params = node.getArguments().get( 1 ).getValue();
			} else {
				params = node.getArguments().stream()
				    .filter( a -> ( a.getName().getAsSimpleValue().toString().equalsIgnoreCase( "params" ) ) )
				    .findFirst()
				    .map( a -> a.getValue() )
				    .orElse( null );
			}
			// If we found named or positional struct params
			if ( params != null ) {
				// Named params could just be { myParam : "myValue" }
				// But we only care if it's using a nested struct for the value as in: { myParam : { value : "myValue" } }
				if ( params instanceof BoxStructLiteral structParamsValues ) {
					// Rename cfsqltype to sqltype
					List<BoxExpression> paramsValues = structParamsValues.getValues();
					// For each even index, which is a value
					for ( int k = 1; k < paramsValues.size(); k += 2 ) {
						BoxExpression paramData = paramsValues.get( k );
						// If the value is a struct literal, we can rename the keys
						if ( paramData instanceof BoxStructLiteral paramDataStruct ) {
							modifySQLParamStruct( paramDataStruct );
						}
					}
				} else if ( params instanceof BoxArrayLiteral arrayParamsValues ) {
					// now do the same thing but for a BoxArrayLiteral of params, where each item in the array is potentially a struct
					arrayParamsValues.getValues().stream()
					    .filter( v -> v instanceof BoxStructLiteral )
					    .map( v -> ( BoxStructLiteral ) v )
					    .forEach( this::modifySQLParamStruct );
				}
			}

		}
		// This is now done with runtime checks in the actual structKeyExist() BIF triggered by the compat module
		/*
		 * if ( name.equalsIgnoreCase( "structKeyExists" ) && node.getArguments().size() == 2 ) {
		 * return transpileStructKeyExists( node );
		 * }
		 */

		// Look for valueList( myQry.columnName ) or valueList( myQry[ "columnName" ] )
		if ( name.equals( "valuelist" ) && node.getArguments().size() > 0 && node.getArguments().get( 0 ).getValue() instanceof BoxAccess ) {
			return transpileValueList( node );
		}
		// Look for quotedValueList( myQry.columnName ) or valueList( myQry[ "columnName" ] )
		if ( name.equals( "quotedvaluelist" ) && node.getArguments().size() > 0 && node.getArguments().get( 0 ).getValue() instanceof BoxAccess ) {
			return transpileQuotedValueList( node );
		}
		// look for BIFs whose return type has changed
		if ( BIFReturnTypeFixSet.contains( name ) && returnValueIsUsed( node ) ) {
			return transpileBIFReturnType( node, name );
		}
		// look for listAppend() so we can default includeEmptyFields to true
		if ( name.equals( "listappend" ) ) {
			return transpileListAppend( node );
		}
		// look for rewritten variable names passed to isDefined()
		if ( name.equals( "isdefined" ) && node.getArguments().size() > 0 && node.getArguments().get( 0 ).getValue() instanceof BoxStringLiteral bsl ) {
			identifierMap.entrySet().stream().forEach( e -> {
				bsl.setValue( replaceIdentifiersInString( bsl.getValue(), e.getKey(), e.getValue() ) );
			} );
		}

		// swap "once" for "one"
		// Only handling position args for now. We can add named arg support later if needed.
		if ( ( name.equals( "replacenocase" ) || name.equals( "replace" ) || name.equals( "rereplace" ) || name.equals( "rereplacenocase" ) )
		    && node.getArguments().size() > 3
		    && node.getArguments().get( 0 ).getName() == null ) {
			if ( node.getArguments().get( 3 ).getValue() instanceof BoxStringLiteral bsl ) {
				String val = bsl.getValue().toLowerCase();
				if ( val.equals( "once" ) ) {
					bsl.setValue( "one" );
				}
			}
		}

		return super.visit( node );
	}

	private BoxNode transpileListAppend( BoxFunctionInvocation node ) {
		var args = node.getArguments();
		if ( args.isEmpty() ) {
			return super.visit( node );
		}
		// Check if named args
		if ( args.get( 0 ).getName() != null ) {
			// named args
			boolean hasIncludeEmptyFields = args.stream().anyMatch( a -> a.getName().getAsSimpleValue().toString().equalsIgnoreCase( "includeEmptyFields" ) );
			if ( !hasIncludeEmptyFields ) {
				args.add(
				    new BoxArgument(
				        new BoxStringLiteral( "includeEmptyFields", null, null ),
				        new BoxBooleanLiteral( true, null, null ),
				        null,
				        null
				    )
				);
				node.setArguments( args );
			}
		} else {
			// positional args
			if ( args.size() < 4 ) {
				if ( args.size() == 2 ) {
					// add delimiter as 3rd positional arg
					args.add(
					    new BoxArgument(
					        null,
					        new BoxStringLiteral( ListUtil.DEFAULT_DELIMITER, null, null ),
					        null,
					        null
					    )
					);
				}
				// add includeEmptyFields as 4th positional arg
				args.add(
				    new BoxArgument(
				        null,
				        new BoxBooleanLiteral( true, null, null ),
				        null,
				        null
				    )
				);
				// Always re-set args and don't just modify the list so the AST model can be updated
				node.setArguments( args );
			}
		}
		return super.visit( node );
	}

	/**
	 * Replaces occurrences of oldValue with newValue in the input string,
	 * matching whole words only, case-insensitively.
	 * This is designed for variable names as it will only match
	 * whole words to avoid partial replacements. non-alphanumeric characters are considered word boundaries.
	 * 
	 * @param input    The input string
	 * @param oldValue The value to be replaced
	 * @param newValue The replacement value
	 * 
	 * @return The modified string with replacements made
	 */
	private String replaceIdentifiersInString( String input, String oldValue, String newValue ) {
		// Quick smoke test to avoid regex
		if ( !input.toLowerCase().contains( oldValue.toLowerCase() ) ) {
			return input;
		}
		String pattern = "(?i)\\b" + Pattern.quote( oldValue ) + "\\b";
		return RegexBuilder.of(
		    input,
		    pattern,
		    true
		).replaceAllAndGet( newValue );
	}

	/**
	 * Takes a struct representation of a query param and modifies any keys
	 * from cfsqltype to sqltype and removes the "cf_sql_" prefix from the values
	 *
	 * @param paramDataStruct The struct literal representing the query param data
	 */
	private void modifySQLParamStruct( BoxStructLiteral paramDataStruct ) {
		List<BoxExpression> paramDataValues = paramDataStruct.getValues();
		// For each odd index, which is the name of the key
		for ( int i = 0; i < paramDataValues.size(); i += 2 ) {
			BoxExpression	key		= paramDataValues.get( i );
			boolean			isType	= false;
			if ( key instanceof BoxIdentifier id && id.getName().equalsIgnoreCase( "cfsqltype" ) ) {
				id.setName( "sqltype" );
				isType = true;
			} else if ( key instanceof BoxStringLiteral str && str.getValue().equalsIgnoreCase( "cfsqltype" ) ) {
				str.setValue( "sqltype" );
				isType = true;
			}
			if ( isType && i + 1 < paramDataValues.size() ) {
				// Now look for the value, which is the next item in the list
				BoxExpression value = paramDataValues.get( i + 1 );
				if ( value instanceof BoxStringLiteral valStr ) {
					valStr.setValue( valStr.getValue().replace( "cf_sql_", "" ) );
				}
			}
		}
	}

	private BoxNode transpileBIFReturnType( BoxFunctionInvocation node, String name ) {
		var					args			= node.getArguments();
		List<BoxStatement>	bodyStatements	= new ArrayList<>();
		bodyStatements.add(
		    // Call the actual BIF
		    new BoxExpressionStatement(
		        new BoxFunctionInvocation(
		            node.getName(),
		            generateBIFArgs( args ),
		            null,
		            null
		        ),
		        null,
		        null
		    )
		);

		// arrayDelete() and structDelete() have special logic and don't just blindly return true
		if ( name.equals( "arraydelete" ) ) {
			// local.__len = arrayLen( arg1 )
			bodyStatements.addFirst(
			    new BoxExpressionStatement(
			        new BoxAssignment(
			            new BoxDotAccess( new BoxIdentifier( "local", null, null ), false, new BoxIdentifier( "__len", null, null ), null, null ),
			            BoxAssignmentOperator.Equal,
			            new BoxFunctionInvocation( "arrayLen", generateArrayLenArgs( args ), null, null ),
			            List.of(),
			            null,
			            null
			        ),
			        null,
			        null
			    )
			);
			// return arrayLen( arg1 ) > local.__len
			bodyStatements.add(
			    new BoxReturn(
			        new BoxComparisonOperation(
			            new BoxFunctionInvocation( "arrayLen", generateArrayLenArgs( args ), null, null ),
			            BoxComparisonOperator.LessThan,
			            new BoxDotAccess( new BoxIdentifier( "local", null, null ), false, new BoxIdentifier( "__len", null, null ), null, null ),
			            null,
			            null
			        ),
			        null,
			        null
			    )
			);
		} else if ( name.equals( "structdelete" ) ) {
			// Return true if the key existed
			// local.__existed = structKeyExists( arg1, arg2 )
			bodyStatements.addFirst(
			    new BoxExpressionStatement(
			        new BoxAssignment(
			            new BoxDotAccess( new BoxIdentifier( "local", null, null ), false, new BoxIdentifier( "__existed", null, null ), null, null ),
			            BoxAssignmentOperator.Equal,
			            new BoxFunctionInvocation( "structKeyExists", generateBIFArgs( args ), null, null ),
			            List.of(),
			            null,
			            null
			        ),
			        null,
			        null
			    )
			);
			BoxExpression indicateNotExistsExpresssion = null;
			if ( args.get( 0 ).getName() == null && args.size() > 2 ) {
				indicateNotExistsExpresssion = new BoxIdentifier( "arg3", null, null );
			} else if ( args.get( 0 ).getName() != null ) {
				// look for arg with name indicateNotExists
				for ( BoxArgument arg : args ) {
					String argName = ( ( BoxStringLiteral ) arg.getName() ).getValue();
					if ( argName.equalsIgnoreCase( "indicateNotExisting" ) ) {
						indicateNotExistsExpresssion = new BoxIdentifier( argName, null, null );
						break;
					}
				}
			}

			// return indicateNotExist ? local.__existed : true
			if ( indicateNotExistsExpresssion != null ) {
				bodyStatements.add(
				    new BoxReturn(
				        new BoxTernaryOperation(
				            indicateNotExistsExpresssion,
				            new BoxDotAccess(
				                new BoxIdentifier( "local", null, null ),
				                false,
				                new BoxIdentifier( "__existed", null, null ),
				                null,
				                null
				            ),
				            new BoxBooleanLiteral(
				                true,
				                null,
				                null
				            ),
				            null,
				            null
				        ),
				        null,
				        null
				    )
				);
			}
		}
		// default behavior: Return the "dummy" value of true
		bodyStatements.add(
		    new BoxReturn(
		        new BoxBooleanLiteral(
		            true,
		            null,
		            null
		        ),
		        null,
		        null
		    )
		);

		var closure = new BoxClosure(
		    // arg1, arg2, etc for as many args to the original BIF, or the actual arg names if using named args
		    generateIIFEArgs( args ),
		    // annotations
		    List.of(),
		    // body
		    new BoxStatementBlock(
		        bodyStatements,
		        null,
		        null
		    ),
		    null,
		    null
		);

		// wrap up the closure as an IIFE
		return new BoxExpressionInvocation(
		    new BoxParenthesis( closure, null, null ),
		    args,
		    null,
		    null
		).addComment( new BoxSingleLineComment( "Transpiler workaround for BIF return type", null, null ) );
	}

	private List<BoxArgument> generateBIFArgs( List<BoxArgument> args ) {
		// positional
		if ( args.size() == 0 || args.get( 0 ).getName() == null ) {

			return args.stream().map( a -> new BoxArgument(
			    new BoxIdentifier(
			        "arg" + ( args.indexOf( a ) + 1 ),
			        null,
			        null ),
			    null,
			    null )
			).collect( Collectors.toList() );
		} else {
			// named
			return args.stream().map( a -> new BoxArgument(
			    a.getName(),
			    new BoxIdentifier(
			        ( ( BoxStringLiteral ) a.getName() ).getValue(),
			        null,
			        null ),
			    null,
			    null )
			).collect( Collectors.toList() );
		}
	}

	private List<BoxArgument> generateArrayLenArgs( List<BoxArgument> args ) {
		// positional
		if ( args.size() == 0 || args.get( 0 ).getName() == null ) {

			return List.of( new BoxArgument(
			    new BoxIdentifier(
			        "arg1",
			        null,
			        null ),
			    null,
			    null ) );
		} else {
			// named
			return List.of( new BoxArgument(
			    new BoxIdentifier(
			        "array",
			        null,
			        null ),
			    null,
			    null ) );
		}
	}

	private List<BoxArgumentDeclaration> generateIIFEArgs( List<BoxArgument> args ) {
		// positional
		if ( args.size() == 0 || args.get( 0 ).getName() == null ) {
			return args.stream().map( a -> new BoxArgumentDeclaration(
			    false,
			    null,
			    "arg" + ( args.indexOf( a ) + 1 ),
			    null,
			    List.of(),
			    List.of(),
			    null,
			    null
			) ).collect( Collectors.toList() );
		} else {
			// named
			return args.stream().map( a -> new BoxArgumentDeclaration(
			    false,
			    null,
			    ( ( BoxStringLiteral ) a.getName() ).getValue(),
			    null,
			    List.of(),
			    List.of(),
			    null,
			    null
			) ).collect( Collectors.toList() );
		}

	}

	/**
	 * Detect if the returned value of an expression appears to be used at all
	 */
	private boolean returnValueIsUsed( BoxExpression node ) {
		/**
		 * This should cover
		 * result = arrayAppend()
		 * arrayAppend().yesNoFormat()
		 * someFunc( arrayAppend() )
		 * arrayAppend() & 'value' (and any other operator)
		 */
		if ( node.getParent() instanceof BoxExpression ) {
			return true;
		}
		/**
		 * This covers
		 * return arrayAppend()
		 */
		if ( node.getParent() instanceof BoxReturn ) {
			return true;
		}

		/**
		 * This covers
		 * if( arrayAppend() ) {}
		 */
		if ( node.getParent() instanceof BoxIfElse ife && ife.getCondition() == node ) {
			return true;
		}

		/**
		 * This covers
		 * while( arrayAppend() ) {}
		 */
		if ( node.getParent() instanceof BoxWhile w && w.getCondition() == node ) {
			return true;
		}

		/**
		 * This covers
		 * do{} while( arrayAppend() )
		 */
		if ( node.getParent() instanceof BoxDo d && d.getCondition() == node ) {
			return true;
		}

		/**
		 * This covers
		 * switch( arrayAppend() ) {}
		 */
		if ( node.getParent() instanceof BoxSwitch s && s.getCondition() == node ) {
			return true;
		}

		/**
		 * This covers
		 * for( ; arrayAppend() ; ) {}
		 */
		if ( node.getParent() instanceof BoxForIndex f && f.getCondition() == node ) {
			return true;
		}

		// Add any more missed scenarios here

		/**
		 * False for just top level statements in a script or function body (parent will be BoxStatementExpression)
		 * arrayAppend()
		 */
		return false;
	}

	// quotedValueList( delimiter ) -> queryColumnData().map().toList( delimiter )
	private BoxNode transpileQuotedValueList( BoxFunctionInvocation node ) {
		BoxAccess			queryCol		= ( BoxAccess ) node.getArguments().get( 0 ).getValue();
		List<BoxArgument>	toListArguments	= new ArrayList<>();
		// If there was a delimiter, pass it on to the toList() call
		if ( node.getArguments().size() > 1 ) {
			toListArguments.add( node.getArguments().get( 1 ) );
		}

		// queryColumnData( qry, col ).map()
		BoxMethodInvocation	mapExpr			= new BoxMethodInvocation(
		    new BoxIdentifier( "map", null, null ),
		    new BoxFunctionInvocation(
		        "queryColumnData",
		        List.of(
		            // Query reference
		            new BoxArgument( queryCol.getContext(), null, null ),
		            // Column name. If it was qry.col extra the text from the idenfitifer and make a string.
		            // if it was qry[ col ] when use col as an expression
		            new BoxArgument( queryCol instanceof BoxDotAccess ? new BoxStringLiteral( ( ( BoxIdentifier ) queryCol.getAccess() ).getName(), null, null )
		                : queryCol.getAccess(), null, null
		            )
		        ),
		        null,
		        null
		    ),
		    List.of(
		        new BoxArgument(
		            // (arr) -> '"' & arr & '"'
		            new BoxClosure(
		                List.of(
		                    new BoxArgumentDeclaration( true, "any", "arr", null, List.of(), List.of(), null, null )
		                ),
		                List.of(),
		                new BoxExpressionStatement(
		                    new BoxStringConcat( List.of(
		                        new BoxStringLiteral( "\"", null, null ),
		                        new BoxIdentifier( "arr", null, null ),
		                        new BoxStringLiteral( "\"", null, null )
		                    ),
		                        null,
		                        null ),
		                    null,
		                    null ),
		                null,
		                null ),
		            null,
		            null )
		    ),
		    null,
		    null
		);

		BoxMethodInvocation	newInvocation	= new BoxMethodInvocation(
		    new BoxIdentifier( "toList", null, null ),
		    mapExpr,
		    toListArguments,
		    null,
		    null
		);
		return super.visit( newInvocation );

	}

	// valueList( delimiter ) -> queryColumnData().toList( delimiter )
	private BoxNode transpileValueList( BoxFunctionInvocation node ) {
		BoxAccess			queryCol		= ( BoxAccess ) node.getArguments().get( 0 ).getValue();
		List<BoxArgument>	toListArguments	= new ArrayList<>();
		// If there was a delimiter, pass it on to the toList() call
		if ( node.getArguments().size() > 1 ) {
			toListArguments.add( node.getArguments().get( 1 ) );
		}

		BoxMethodInvocation newInvocation = new BoxMethodInvocation(
		    new BoxIdentifier( "toList", null, null ),
		    new BoxFunctionInvocation(
		        "queryColumnData",
		        List.of(
		            // Query reference
		            new BoxArgument( queryCol.getContext(), null, null ),
		            // Column name. If it was qry.col extra the text from the idenfitifer and make a string.
		            // if it was qry[ col ] when use col as an expression
		            new BoxArgument( queryCol instanceof BoxDotAccess ? new BoxStringLiteral( ( ( BoxIdentifier ) queryCol.getAccess() ).getName(), null, null )
		                : queryCol.getAccess(), null, null
		            )
		        ),
		        null,
		        null
		    ),
		    toListArguments,
		    false,
		    true,
		    null,
		    null
		);
		return super.visit( newInvocation );

	}

	/**
	 * Helper method to transpile structKeyExists() function calls to BoxLang-compatible null checks.
	 *
	 * Transforms CFML structKeyExists(struct, key) calls into BoxLang !isNull(struct[key]) expressions.
	 * This provides equivalent functionality while using BoxLang's null-checking semantics.
	 *
	 * Transformation example:
	 * - structKeyExists(myStruct, "key") becomes !isNull(myStruct["key"])
	 * - structKeyExists(obj, varname) becomes !isNull(obj[varname])
	 *
	 * @param node The BoxFunctionInvocation node representing structKeyExists call
	 *
	 * @return A BoxUnaryOperation node representing the !isNull() equivalent
	 */
	private BoxNode transpileStructKeyExists( BoxFunctionInvocation node ) {
		BoxUnaryOperation newNode = new BoxUnaryOperation(
		    new BoxFunctionInvocation(
		        "isNull",
		        List.of(
		            new BoxArgument(
		                new BoxArrayAccess(
		                    node.getArguments().get( 0 ).getValue(),
		                    true,
		                    node.getArguments().get( 1 ).getValue(),
		                    null,
		                    null ),
		                null,
		                null
		            )
		        ),
		        null,
		        null
		    ),
		    BoxUnaryOperator.Not,
		    null,
		    null
		);
		return super.visit( newNode );
	}

	/**
	 * Transforms BoxComponent nodes for CFML tag compatibility.
	 *
	 * Key transformations applied:
	 * 1. Component Name Mapping: Renames components using componentMap (e.g., module -> component)
	 * 2. CF Identifier Updates: Updates CF-prefixed identifiers in component attributes
	 * - Handles "result" attribute string literals containing CF variable references
	 * 3. Attribute Transformations: Applies component-specific attribute mappings
	 *
	 * Examples:
	 * - <cfmodule> becomes <component>
	 * - result="cfthread" becomes result="bxthread" in string literals
	 *
	 * Special handling for lifecycle components and their result attributes ensures
	 * proper variable reference updates throughout the transformation.
	 *
	 * @param node The BoxComponent node to transform
	 *
	 * @return The transformed BoxComponent node
	 */
	@Override
	public BoxNode visit( BoxComponent node ) {
		String componentName = node.getName().toLowerCase();
		if ( identifierMap.containsKey( "cf" + componentName ) ) {
			String strToReplace = "cf" + componentName;
			// Look for a "result" attribute, and if it's a string literal, replace any identifers inside with the new name
			node.getAttributes().stream().filter( a -> a.getKey().getValue().equalsIgnoreCase( "result" ) && a.getValue() instanceof BoxStringLiteral )
			    .forEach( a -> {
				    BoxStringLiteral str = ( BoxStringLiteral ) a.getValue();
				    str.setValue( replaceIdentifiersInString( str.getValue(), strToReplace, identifierMap.get( strToReplace ) ) );
			    } );

		}

		if ( componentMap.containsKey( componentName ) ) {
			node.setName( componentMap.get( componentName ) );
		}

		if ( componentAttrMap.containsKey( componentName ) ) {
			var					attrs	= node.getAttributes();
			Map<String, String>	attrMap	= componentAttrMap.get( componentName );
			for ( BoxAnnotation attr : attrs ) {
				String key = attr.getKey().getValue().toLowerCase();
				if ( attrMap.containsKey( key ) ) {
					attr.getKey().setValue( attrMap.get( key ) );
				}
			}
			attrs.stream().forEach( attr -> {
				if ( attr.getKey().getValue().equalsIgnoreCase( "attributeCollection" ) ) {
					node.addComment( new BoxSingleLineComment( "Transpiler workaround for runtime transpilation of attributeCollection", null, null ) );

					List<BoxExpression> keyList = attrMap.keySet().stream()
					    .flatMap( k -> Stream.of( new BoxStringLiteral( k, null, null ), new BoxStringLiteral( attrMap.get( k ), null, null ) ) )
					    .collect( Collectors.toList() );

					attr.setValue(
					    // MOVE THIS TO THE COMPAT MODULE WHEN THE TRANSPILER IS MOVED THERE
					    new BoxFunctionInvocation(
					        "transpileCollectionKeySwap",
					        List.of(
					            new BoxArgument(
					                attr.getValue(),
					                null,
					                null
					            ),
					            new BoxArgument(
					                new BoxStructLiteral(
					                    BoxStructType.Unordered,
					                    keyList,
					                    null,
					                    null
					                ),
					                null,
					                null
					            )
					        ),
					        null,
					        null
					    )
					);
				}
			} );
		}
		// fix SQL types to remove cf_sql_ from values (we transpile the attribute names generically above)
		if ( componentName.equals( "queryparam" ) || componentName.equals( "procparam" ) ) {
			node.getAttributes().stream()
			    .filter( a -> a.getKey().getValue().equalsIgnoreCase( "sqltype" ) && a.getValue() instanceof BoxStringLiteral )
			    .forEach( a -> {
				    BoxStringLiteral bsl		= ( BoxStringLiteral ) a.getValue();
				    String			newValue	= bsl.getValue().replace( "cf_sql_", "" );
				    bsl.setValue( newValue );
			    } );
		}
		// Ignore invalid values for cfquery dbtype. If it's a string literal, and not query or hql, then literally delete the attribute entirely
		if ( componentName.equals( "query" ) ) {
			node.getAttributes()
			    .stream()
			    .filter( a -> a.getKey().getValue().equalsIgnoreCase( "dbtype" ) && a.getValue() instanceof BoxStringLiteral )
			    .filter( a -> {
				    String value = ( ( BoxStringLiteral ) a.getValue() ).getValue();
				    return !value.equalsIgnoreCase( "query" ) && !value.equalsIgnoreCase( "hql" );
			    } )
			    .toList()
			    .forEach( a -> node.getAttributes().remove( a ) );
		}
		return super.visit( node );
	}

	/**
	 * Rewrite !foo eq bar
	 * as !(foo eq bar)
	 * These operators should be higher precedence than the not operator
	 * ^
	 * *, /
	 * \
	 * MOD
	 * +, -
	 * CONTAINS
	 * DOES NOT CONTAIN
	 */
	public BoxNode visit( BoxBinaryOperation node ) {
		BoxExpression left = node.getLeft();
		// If this is a binary operations and the left operator is a unary not
		if ( binaryOpsHigherThanNot.contains( node.getOperator() ) && left instanceof BoxUnaryOperation buo && buo.getOperator() == BoxUnaryOperator.Not ) {
			// Rip off the not and warp the binary in parents
			node.setLeft( buo.getExpr() );
			BoxExpression parenNode = new BoxParenthesis( node, node.getPosition(), node.getSourceText() );
			// Then re-apply the not to the parens
			return visit( new BoxUnaryOperation( parenNode, BoxUnaryOperator.Not, node.getPosition(), node.getSourceText() ) );
		}
		return super.visit( node );
	}

	/**
	 * Transforms BoxComparisonOperation nodes to fix operator precedence issues with the NOT operator.
	 *
	 * In CFML, comparison operators have higher precedence than the NOT operator, which differs
	 * from some other languages. This method ensures proper precedence by restructuring
	 * expressions where NOT appears on the left side of a comparison.
	 *
	 * Transformations applied:
	 * - !foo eq bar becomes !(foo eq bar)
	 * - !obj.prop > 5 becomes !(obj.prop > 5)
	 *
	 * This affects all comparison operators: EQ, NEQ, LT, LTE, GT, GTE, ==, !=, >, >=, <, <=
	 *
	 * The transformation:
	 * 1. Detects NOT operator on the left side of comparison
	 * 2. Removes the NOT from the left operand
	 * 3. Wraps the entire comparison in parentheses
	 * 4. Applies the NOT to the parenthesized expression
	 *
	 * @param node The BoxComparisonOperation node to transform
	 *
	 * @return The transformed node with corrected operator precedence
	 */
	public BoxNode visit( BoxComparisonOperation node ) {
		BoxExpression left = node.getLeft();
		// If this is a binary operations and the left operator is a unary not
		if ( left instanceof BoxUnaryOperation buo && buo.getOperator() == BoxUnaryOperator.Not ) {
			// Rip off the not and wrap the comparison in parents
			node.setLeft( buo.getExpr() );
			BoxExpression parenNode = new BoxParenthesis( node, node.getPosition(), node.getSourceText() );
			// Then re-apply the not to the parens
			return visit( new BoxUnaryOperation( parenNode, BoxUnaryOperator.Not, node.getPosition(), node.getSourceText() ) );
		}
		return super.visit( node );
	}

	/**
	 * Rewrite !foo eq bar
	 * as !(foo eq bar)
	 * These operators should be higher precedence than the not operator
	 */
	public BoxNode visit( BoxStringConcat node ) {
		List<BoxExpression> values = node.getValues();
		// If this is a concat with 2 expressions. I'm going to ignore foo & bar & baz & bum... as it feels like an edge case and is more annoying
		if ( values.size() == 2 && values.get( 0 ) instanceof BoxUnaryOperation buo && buo.getOperator() == BoxUnaryOperator.Not ) {
			// Rip off the not and wrap the comparison in parents
			values.set( 0, buo.getExpr() );
			// values is passed by reference, but this updates the internal children tracking
			node.setValues( values );
			BoxExpression parenNode = new BoxParenthesis( node, node.getPosition(), node.getSourceText() );
			// Then re-apply the not to the parens
			return visit( new BoxUnaryOperation( parenNode, BoxUnaryOperator.Not, node.getPosition(), node.getSourceText() ) );
		}
		return super.visit( node );
	}

	/**
	 * CF reads documentation comment lines such as
	 *
	 * @foo bar
	 *      as an actual "annotation" for classes and functions and properties.
	 *      We'll need to merge these in manually as BL keeps them separate.
	 *      /**
	 *      Helper method to merge documentation annotations into the formal annotation list.
	 *
	 *      This method bridges CFML's documentation comments with BoxLang's annotation system
	 *      by converting documentation annotations into formal annotations that can be
	 *      processed by the runtime.
	 *
	 *      Key behaviors:
	 *      - Only merges if mergeDocsIntoAnnotations configuration is enabled
	 *      - Skips "hint" annotations (handled separately)
	 *      - Avoids overriding existing formal annotations
	 *      - Trims whitespace from string values and converts empty strings to null
	 *
	 *      Examples:
	 *      - @param name "User name" becomes a formal annotation
	 *      - @return "Generated user ID" becomes a return annotation
	 *      - @Deprecated "Use newMethod instead" becomes a deprecated annotation
	 *
	 * @param annotations   The existing formal annotations list to merge into
	 * @param documentation The documentation annotations to merge from
	 */
	private void mergeDocsIntoAnnotations( List<BoxAnnotation> annotations, List<BoxDocumentationAnnotation> documentation ) {
		if ( !mergeDocsIntoAnnotations )
			return;

		Set<String> existingAnnotations = annotations.stream().map( BoxAnnotation::getKey ).map( BoxFQN::getValue ).map( k -> k.toLowerCase() )
		    .collect( Collectors.toSet() );
		for ( BoxDocumentationAnnotation doc : documentation ) {
			// Don't override existing annotations, and don't copy hint
			if ( !doc.getKey().getValue().equalsIgnoreCase( "hint" ) && !existingAnnotations.contains( doc.getKey().getValue().toLowerCase() ) ) {
				BoxExpression value = doc.getValue();
				if ( value instanceof BoxStringLiteral bsl ) {
					bsl.setValue( bsl.getValue().trim() );
					if ( bsl.getValue().isEmpty() ) {
						value = null;
					}
				}
				annotations.add(
				    new BoxAnnotation(
				        new BoxFQN( doc.getKey().getValue(), null, null ),
				        value,
				        null,
				        null
				    )
				);
			}
		}
	}

	/**
	 * Remove empty output nodes from script (because in BoxLang, classes are only script, so the original CF may have been tags)
	 */
	@Override
	public BoxNode visit( BoxBufferOutput node ) {
		if ( isClass ) {
			BoxExpression expr = node.getExpression();
			// only contains white space
			if ( expr instanceof BoxStringLiteral str && str.getValue().isBlank() ) {
				if ( node.getFirstAncestorOfType( BoxTemplateIsland.class ) == null
				    // This is prolly not comprehensive. Maybe there's a better approach, but let's try to detect if we're in a componenet that's actually outputting something
				    // The main problem here is that any whitespace COULD POTENTIALLY be significant depending on what the code is doing.
				    // An alternative approach is to not remove the nodes here, but skip them in the actual BoxPrettyPrintVisitor which is the original for this change
				    // But even then, we still have the problem, it just moves to another class!
				    // Another actual approach is to look for sibling buffer output nodes and if any of them are not empty, then don't remove this one
				    // That's still not great as there could be cousin nodes a littler further away. In reality, the best way is prolly to get all the descendants of a UDF
				    // which are a buffer outpuot, and if all of them are whitespace, then ignore them all, but if there is any real actual output, then preserve them all. ) {
				    && node.getFirstAncestorOfType( BoxComponent.class, n -> n.getName().equalsIgnoreCase( "query" )
				        || n.getName().equalsIgnoreCase( "document" )
				        || n.getName().equalsIgnoreCase( "savecontent" ) ) == null ) {
					return null;
				}
			}
		}
		return super.visit( node );
	}

	/**
	 * Replace new java() with createObject( "java", "java.lang.String" )
	 * Replace new component() with createObject( "component", "path.to.component" )
	 */
	@Override
	public BoxNode visit( BoxNew node ) {
		if ( !node.getArguments().isEmpty() && node.getPrefix() == null && node.getExpression() instanceof BoxFQN fqn ) {
			String name = fqn.getValue().toLowerCase();
			// TODO: When we add more features to createObject(), add them here as well. corba, com, Webservice, .NET, dotnet,
			if ( name.equals( "java" ) || name.equals( "component" ) ) {
				List<BoxArgument> args = new ArrayList<>();
				args.add( new BoxArgument( new BoxStringLiteral( name, fqn.getPosition(), fqn.getSourceText() ), fqn.getPosition(), fqn.getSourceText() ) );
				args.addAll( node.getArguments() );
				BoxFunctionInvocation newExpr = new BoxFunctionInvocation(
				    "createObject",
				    args,
				    node.getPosition(),
				    node.getSourceText()
				);
				return super.visit( newExpr );
			}
		}
		return super.visit( node );
	}

	/**
	 * Add the statement
	 * variables.addOverride( 'bxcatch', e )
	 * to the top of the try catch block
	 * This is to ensure that the catch variable is always available in the variables scope
	 */
	@Override
	public BoxNode visit( BoxTryCatch node ) {
		// Tag catch blocks don't allow you to set the variable anyway
		// Also, only do this if we're in Lucee compat.
		if ( !isLuceeCompat || !isInScript( node ) ) {
			return super.visit( node );
		}

		if ( node.getCatchBody() != null && node.getException() != null ) {
			var body = node.getCatchBody();
			body.addFirst(
			    ( BoxStatement ) new BoxExpressionStatement(
			        new BoxMethodInvocation(
			            new BoxIdentifier( "addOverride", null, null ),
			            new BoxScope( "variables", null, null ),
			            List.of(
			                new BoxArgument(
			                    new BoxStringLiteral( "bxcatch", null, null ),
			                    null,
			                    null
			                ),
			                new BoxArgument(
			                    new BoxIdentifier( node.getException().getName(), null, null ),
			                    null,
			                    null
			                )
			            ),
			            null,
			            null
			        ),
			        null,
			        null
			    ).addComment(
			        new BoxSingleLineComment(
			            "Ensure the catch variable is available in the variables scope as 'bxcatch' (Lucee compat)",
			            null,
			            null
			        )
			    )
			);
			node.setCatchBody( body );
		}
		return super.visit( node );
	}

	/**
	 * Helper method to add or ensure the output annotation is set to true for CFML compatibility.
	 *
	 * In CFML, components and functions typically have output enabled by default,
	 * whereas BoxLang may have different defaults. This method ensures consistent
	 * behavior by adding output=true annotations when they don't exist.
	 *
	 * Behavior:
	 * - Only operates if forceOutputTrue configuration is enabled
	 * - Checks if an "output" annotation already exists
	 * - Adds output=true annotation if none exists
	 * - Preserves existing output annotations (doesn't override)
	 *
	 * This maintains CFML's output behavior while allowing explicit override
	 * when developers specify their own output annotations.
	 *
	 * @param annotations The annotations list to potentially modify
	 */
	private void enableOutput( List<BoxAnnotation> annotations ) {
		if ( !forceOutputTrue )
			return;

		if ( annotations.stream().noneMatch( a -> a.getKey().getValue().equalsIgnoreCase( "output" ) ) ) {
			// @output true
			annotations.add(
			    new BoxAnnotation(
			        new BoxFQN( "output", null, null ),
			        new BoxBooleanLiteral( true, null, null ),
			        null,
			        null )
			);
		}
	}

	/**
	 * Helper method to rename top-level CF variables to BoxLang equivalents.
	 *
	 * This method handles the systematic renaming of CFML reserved variables
	 * to their BoxLang counterparts, ensuring compatibility while avoiding
	 * naming conflicts with BoxLang's own reserved words.
	 *
	 * Variable mappings applied:
	 * - cfcatch -> bxcatch (exception handling variable)
	 * - cfthread -> bxthread (threading variable)
	 * - cffile -> bxfile (file operation variable)
	 * - cfftp -> bxftp (FTP operation variable)
	 * - cfhttp -> bxhttp (HTTP operation variable)
	 * - cfquery -> bxquery (query operation variable)
	 * - thisTag -> thisComponent (component scope reference)
	 * - etc.
	 *
	 * The method uses case-insensitive matching but preserves the original
	 * casing pattern in the replacement to maintain code readability.
	 * Special logic prevents renaming when the identifier represents a function
	 * argument with the same name.
	 *
	 * @param id The BoxIdentifier node to potentially rename
	 */
	private void renameTopLevelVars( BoxIdentifier id ) {
		String name = id.getName().toLowerCase();
		if ( identifierMap.containsKey( name ) && !isInFunctionWithArgNamed( id, name ) ) {
			id.setName( identifierMap.get( name ) );
		}
	}

	// Check if a node is inside of a function with an argument of the given name
	private boolean isInFunctionWithArgNamed( BoxNode node, String name ) {
		return node.getFirstAncestorOfType(
		    BoxFunctionDeclaration.class,
		    funcDec -> funcDec.getArgs().stream().anyMatch( a -> a.getName().equalsIgnoreCase( name ) )
		) != null;
	}

	/**
	 * Determine if a node is inside a script or template
	 * TODO: Does this deserve to exist on BoxNode?
	 *
	 * @param node The node to check
	 *
	 * @return true if the node is inside a script or template, false otherwise
	 */
	@SuppressWarnings( "unchecked" )
	private boolean isInScript( BoxNode node ) {
		return Optional.ofNullable( node.getFirstNodeOfTypes( BoxScript.class, BoxTemplate.class, BoxTemplateIsland.class, BoxScriptIsland.class ) )
		    .map( n -> n instanceof BoxScript || n instanceof BoxScriptIsland )
		    .orElse( false );
	}
}
