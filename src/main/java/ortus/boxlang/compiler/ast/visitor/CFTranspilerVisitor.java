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

/**
 * Pretty print BoxLang AST nodes
 */
public class CFTranspilerVisitor extends ReplacingBoxVisitor {

	private static Set<String>						BIFReturnTypeFixSet			= new HashSet<>();
	private static Map<String, String>				BIFMap						= new HashMap<>();
	private static Map<String, String>				identifierMap				= new HashMap<>();
	private static Map<String, String>				componentMap				= new HashMap<>();
	private static Map<String, Map<String, String>>	componentAttrMap			= new HashMap<>();
	private static Key								transpilerKey				= Key.of( "transpiler" );
	private static Key								upperCaseKeysKey			= Key.of( "upperCaseKeys" );
	private static Key								forceOutputTrueKey			= Key.of( "forceOutputTrue" );
	private static Key								mergeDocsIntoAnnotationsKey	= Key.of( "mergeDocsIntoAnnotations" );
	private static Key								isLuceeKey					= Key.of( "isLucee" );
	private static Key								compatKey					= Key.of( "compat-cfml" );
	private static BoxRuntime						runtime						= BoxRuntime.getInstance();
	private static ModuleService					moduleService				= runtime.getModuleService();
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
	 * Transpile Box Classes
	 * - Merge documentation into annotations
	 * - enable output
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
	 * Transpile UDF declarations
	 * - Merge documentation into annotations
	 * - enable output
	 * - rename onCFCRequest to onClassRequest
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
	 * Rename CF variables
	 */
	@Override
	public BoxNode visit( BoxIdentifier node ) {
		renameTopLevelVars( node );
		return super.visit( node );
	}

	/**
	 * Rename CF variables
	 * change variables[ "cfcatch" ] to variables[ "bxcatch" ]
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
	 * change foo.bar to foo.BAR
	 */
	@Override
	public BoxNode visit( BoxDotAccess node ) {
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
	 * Rename top level CF variables
	 * change { foo : 'bar' } to { FOO : 'bar' }
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
	 * Rename some common CF built-in functions like chr() to char()
	 *
	 * Replace
	 * structKeyExists( struct, key )
	 * with
	 * !isNull( struct[ key ] )
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
		if ( name.equalsIgnoreCase( "valueList" ) && node.getArguments().size() > 0 && node.getArguments().get( 0 ).getValue() instanceof BoxAccess ) {
			return transpileValueList( node );
		}
		// Look for quotedValueList( myQry.columnName ) or valueList( myQry[ "columnName" ] )
		if ( name.equalsIgnoreCase( "quotedValueList" ) && node.getArguments().size() > 0 && node.getArguments().get( 0 ).getValue() instanceof BoxAccess ) {
			return transpileQuotedValueList( node );
		}
		// look for BIFs whose return type has changed
		if ( BIFReturnTypeFixSet.contains( name ) && returnValueIsUsed( node ) ) {
			return transpileBIFReturnType( node, name );
		}
		return super.visit( node );
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
	 * Rename components and attributes
	 * Rename enablecfoutputonly attribute on cfsetting tag
	 */
	@Override
	public BoxNode visit( BoxComponent node ) {
		String componentName = node.getName().toLowerCase();
		if ( identifierMap.containsKey( "cf" + componentName ) ) {
			String strToReplace = "cf" + componentName;
			// Look for a "result" attribute, and if it's a string literal, replace any identifers inside with the new name
			node.getAttributes().stream().filter( a -> a.getKey().getValue().equalsIgnoreCase( "result" ) && a.getValue() instanceof BoxStringLiteral )
			    .forEach( a -> {
				    BoxStringLiteral str		= ( BoxStringLiteral ) a.getValue();
				    String			newValue	= str.getValue().replace( strToReplace, identifierMap.get( strToReplace ) );
				    str.setValue( newValue );
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
	 * Rewrite !foo eq bar
	 * as !(foo eq bar)
	 * These operators should be higher precedence than the not operator
	 * EQ, NEQ, LT, LTE, GT, GTE, ==, !=, >, >=, &lt;, &lt;=
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
	 *
	 * @param annotations   The annotations for the node
	 * @param documentation The documentation for the node
	 *
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
			if ( expr instanceof BoxStringLiteral str && str.getValue().trim().isEmpty() ) {
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
	 * Add output annotation and set to true if it doesn't exist
	 *
	 * @param annotations The annotations for the node
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
	 * Rename some common CF variables like
	 * cfcatch
	 * to
	 * bxcatch
	 *
	 * @param BoxIdentifier The identifier node
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
