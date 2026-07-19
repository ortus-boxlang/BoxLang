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
package ortus.boxlang.compiler.ast.visitor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.expression.BoxArgument;
import ortus.boxlang.compiler.ast.expression.BoxAssignment;
import ortus.boxlang.compiler.ast.expression.BoxAssignmentModifier;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxDotAccess;
import ortus.boxlang.compiler.ast.expression.BoxFunctionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxLambda;
import ortus.boxlang.compiler.ast.expression.BoxScope;
import ortus.boxlang.compiler.ast.statement.BoxAccessModifier;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;

/**
 * AST visitor that obfuscates BoxLang source code in-place before it is re-serialized
 * by the pretty-printer. It performs the following transformations:
 *
 * <ul>
 * <li>Renames {@code var}-declared local variables within each function/closure/lambda scope</li>
 * <li>Optionally renames private / script-level function declarations and their call sites</li>
 * <li>Optionally renames function argument names</li>
 * </ul>
 *
 * Comment stripping is handled up-front via the static {@link #stripComments(BoxNode)} helper,
 * which should be called on the root node before it is passed to this visitor.
 *
 * <h2>Scope safety</h2>
 * Only variables that are demonstrably local (introduced with a {@code var} declaration) are
 * renamed. References through the {@code local.} scope (and {@code arguments.} for renamed
 * arguments) are kept consistent with the renamed declaration. Struct keys, member access on
 * the right-hand side of a dot, named argument keys and BoxLang scope keywords are never renamed.
 *
 * <h2>Name generation</h2>
 * A single monotonic counter feeds the local-variable name generator, producing names in the
 * sequence {@code _a, _b, …, _z, _aa, _ab, …}. Because the counter never resets, names are unique
 * across the whole file, which prevents collisions when an inner closure captures an outer local.
 */
public class ObfuscationVisitor extends VoidBoxVisitor {

	/**
	 * BoxLang built-in scope identifiers that must never be renamed.
	 */
	private static final Set<String>			SCOPE_NAMES			= Set.of(
	    "variables", "local", "request", "session", "application",
	    "arguments", "url", "form", "this", "static", "server", "cgi",
	    "thread", "cookie", "client", "cluster", "super" );

	/** Whether to rename {@code var}-declared local variables. */
	private final boolean						renameVars;

	/** Whether to rename private / script-level function declarations and call sites. */
	private final boolean						renameFunctions;

	/** Whether to rename function argument names. */
	private final boolean						renameArgs;

	/** Stack of per-scope rename maps (top of stack = inner-most scope). */
	private final Deque<Map<String, String>>	scopeStack			= new ArrayDeque<>();

	/** File-level map of original function name (lowercase) → obfuscated name. */
	private final Map<String, String>			functionRenameMap	= new LinkedHashMap<>();

	/** Monotonic counter feeding the local-variable name generator. */
	private int									varCounter			= 0;

	/** Monotonic counter feeding the function name generator. */
	private int									funcCounter			= 0;

	/**
	 * Constructs an ObfuscationVisitor.
	 *
	 * @param renameVars      rename {@code var}-declared local variables
	 * @param renameFunctions rename private / script-level functions and their call sites
	 * @param renameArgs      rename function argument names
	 */
	public ObfuscationVisitor( boolean renameVars, boolean renameFunctions, boolean renameArgs ) {
		this.renameVars			= renameVars;
		this.renameFunctions	= renameFunctions;
		this.renameArgs			= renameArgs;
	}

	/**
	 * Recursively strips all comments from the AST by clearing every node's comment list.
	 * Call this once on the root node before passing the root to this visitor.
	 *
	 * @param node the root node to strip comments from
	 */
	public static void stripComments( BoxNode node ) {
		node.getComments().clear();
		for ( BoxNode child : new ArrayList<>( node.getChildren() ) ) {
			stripComments( child );
		}
	}

	/**
	 * Performs a first pass to collect all renameable (private / script-level) function names
	 * in the file so their call sites can be rewritten during the main traversal. Must be called
	 * before {@code root.accept(this)} when {@code renameFunctions} is enabled.
	 *
	 * @param root the root AST node
	 */
	public void collectFunctionNames( BoxNode root ) {
		if ( !this.renameFunctions ) {
			return;
		}
		root.accept( new VoidBoxVisitor() {

			@Override
			public void visit( BoxFunctionDeclaration func ) {
				if ( isRenameableFunction( func ) ) {
					String lower = func.getName().toLowerCase();
					if ( !functionRenameMap.containsKey( lower ) ) {
						functionRenameMap.put( lower, nextFunctionName() );
					}
				}
				super.visit( func );
			}
		} );
	}

	// -------------------------------------------------------------------------
	// Visit overrides
	// -------------------------------------------------------------------------

	@Override
	public void visit( BoxFunctionDeclaration func ) {
		if ( this.renameFunctions && isRenameableFunction( func ) ) {
			String newName = this.functionRenameMap.get( func.getName().toLowerCase() );
			if ( newName != null ) {
				func.setName( newName );
			}
		}

		// Drop inline documentation annotations (@foo doc tags) as part of obfuscation
		if ( func.getDocumentation() != null ) {
			func.getDocumentation().clear();
		}

		enterScope( func.getArgs(), func.getBody() );
		super.visit( func );
		this.scopeStack.pop();
	}

	@Override
	public void visit( BoxClosure closure ) {
		enterScope( closure.getArgs(), wrapBody( closure.getBody() ) );
		super.visit( closure );
		this.scopeStack.pop();
	}

	@Override
	public void visit( BoxLambda lambda ) {
		enterScope( lambda.getArgs(), wrapBody( lambda.getBody() ) );
		super.visit( lambda );
		this.scopeStack.pop();
	}

	@Override
	public void visit( BoxIdentifier id ) {
		renameIfInScope( id );
	}

	@Override
	public void visit( BoxFunctionInvocation invocation ) {
		if ( this.renameFunctions ) {
			String newName = this.functionRenameMap.get( invocation.getName().toLowerCase() );
			if ( newName != null ) {
				invocation.setName( newName );
			}
		}
		super.visit( invocation );
	}

	@Override
	public void visit( BoxDotAccess access ) {
		// The right-hand side of a dot is a member/struct key, not a variable reference,
		// so by default we only descend into the object being accessed. The exception is
		// scope-qualified access to a renamed local (e.g. local.x / arguments.foo), where the
		// key must stay in sync with the renamed declaration.
		String scopeName = scopeNameOf( access.getContext() );
		if ( scopeName != null
		    && ( scopeName.equals( "local" ) || scopeName.equals( "arguments" ) )
		    && access.getAccess() instanceof BoxIdentifier keyId ) {
			renameIfInScope( keyId );
			return;
		}
		access.getContext().accept( this );
	}

	@Override
	public void visit( BoxArgument arg ) {
		// For named arguments (foo(name=value)) the name is a struct key, never a variable
		// reference, so only the value is traversed.
		if ( arg.getValue() != null ) {
			arg.getValue().accept( this );
		}
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	/**
	 * Pushes a new scope onto the stack, populating it with renamed argument names (when
	 * {@code renameArgs} is enabled) and {@code var}-declared locals discovered in the body
	 * (when {@code renameVars} is enabled).
	 *
	 * @param args the argument declarations for this scope (may be null)
	 * @param body the statement list forming this scope's body (may be null)
	 */
	private void enterScope( List<BoxArgumentDeclaration> args, List<BoxStatement> body ) {
		Map<String, String> scope = new LinkedHashMap<>();

		if ( this.renameArgs && args != null ) {
			for ( BoxArgumentDeclaration arg : args ) {
				String lower = arg.getName().toLowerCase();
				if ( !SCOPE_NAMES.contains( lower ) && !scope.containsKey( lower ) ) {
					String newName = nextVarName();
					scope.put( lower, newName );
					arg.setName( newName );
				}
			}
		}

		if ( this.renameVars && body != null ) {
			collectVarDeclarations( body, scope );
		}

		this.scopeStack.push( scope );
	}

	/**
	 * Renames the identifier in-place if its (case-insensitive) name is registered in any scope
	 * currently on the stack. BoxLang scope keywords are never renamed.
	 *
	 * @param id the identifier to potentially rename
	 */
	private void renameIfInScope( BoxIdentifier id ) {
		String lower = id.getName().toLowerCase();
		if ( SCOPE_NAMES.contains( lower ) ) {
			return;
		}
		for ( Map<String, String> scope : this.scopeStack ) {
			String newName = scope.get( lower );
			if ( newName != null ) {
				id.setName( newName );
				return;
			}
		}
	}

	/**
	 * Returns the scope keyword represented by an expression, or {@code null} if it is not a
	 * scope reference. Handles both {@link BoxScope} nodes and bare {@link BoxIdentifier}s whose
	 * name matches a scope keyword.
	 *
	 * @param expr the expression to inspect
	 *
	 * @return the lowercase scope name, or {@code null}
	 */
	private String scopeNameOf( BoxNode expr ) {
		if ( expr instanceof BoxScope scope ) {
			return scope.getName().toLowerCase();
		}
		if ( expr instanceof BoxIdentifier id && SCOPE_NAMES.contains( id.getName().toLowerCase() ) ) {
			return id.getName().toLowerCase();
		}
		return null;
	}

	/**
	 * Wraps a single body statement in a list, or returns null when the body is null.
	 *
	 * @param body the single-statement body of a closure or lambda
	 *
	 * @return a singleton list wrapping the body, or null
	 */
	private List<BoxStatement> wrapBody( BoxStatement body ) {
		if ( body == null ) {
			return null;
		}
		List<BoxStatement> list = new ArrayList<>( 1 );
		list.add( body );
		return list;
	}

	/**
	 * Returns true if the function declaration may have its name obfuscated. Only private
	 * functions and unmodified script-level functions are renamed; public/remote/package
	 * functions form the callable API and are left untouched.
	 *
	 * @param func the function declaration to test
	 *
	 * @return true when the function name may be safely renamed
	 */
	private boolean isRenameableFunction( BoxFunctionDeclaration func ) {
		BoxAccessModifier mod = func.getAccessModifier();
		return mod == null || mod == BoxAccessModifier.Private;
	}

	/**
	 * Scans a scope's body statements for {@code var}-declared identifiers, registering each
	 * unique one in {@code scope} with a freshly generated obfuscated name. Descent stops at
	 * nested function/closure/lambda bodies, which own their own scopes.
	 *
	 * @param statements the statement list to scan
	 * @param scope      the scope map to populate
	 */
	private void collectVarDeclarations( List<BoxStatement> statements, Map<String, String> scope ) {
		if ( statements == null ) {
			return;
		}
		VoidBoxVisitor collector = new VoidBoxVisitor() {

			@Override
			public void visit( BoxAssignment node ) {
				if ( node.getModifiers() != null
				    && node.getModifiers().contains( BoxAssignmentModifier.VAR )
				    && node.getLeft() instanceof BoxIdentifier id ) {
					String lower = id.getName().toLowerCase();
					if ( !scope.containsKey( lower ) && !SCOPE_NAMES.contains( lower ) ) {
						scope.put( lower, nextVarName() );
					}
				}
				super.visit( node );
			}

			@Override
			public void visit( BoxFunctionDeclaration nested ) {
				// Nested functions own their own scope; do not descend
			}

			@Override
			public void visit( BoxClosure nested ) {
				// Nested closures own their own scope; do not descend
			}

			@Override
			public void visit( BoxLambda nested ) {
				// Nested lambdas own their own scope; do not descend
			}
		};
		statements.forEach( stmt -> stmt.accept( collector ) );
	}

	/**
	 * Generates the next obfuscated local-variable name.
	 *
	 * @return a short obfuscated variable name (e.g. {@code _a})
	 */
	private String nextVarName() {
		return "_" + toBase26( this.varCounter++ );
	}

	/**
	 * Generates the next obfuscated function name.
	 *
	 * @return a short obfuscated function name (e.g. {@code _fa})
	 */
	private String nextFunctionName() {
		return "_f" + toBase26( this.funcCounter++ );
	}

	/**
	 * Converts a non-negative integer to a lowercase base-26 alphabetic string:
	 * 0 → "a", 25 → "z", 26 → "aa", 27 → "ab", etc.
	 *
	 * @param n the non-negative integer to convert
	 *
	 * @return the base-26 string representation
	 */
	private static String toBase26( int n ) {
		StringBuilder sb = new StringBuilder();
		do {
			sb.insert( 0, ( char ) ( 'a' + ( n % 26 ) ) );
			n = ( n / 26 ) - 1;
		} while ( n >= 0 );
		return sb.toString();
	}
}
