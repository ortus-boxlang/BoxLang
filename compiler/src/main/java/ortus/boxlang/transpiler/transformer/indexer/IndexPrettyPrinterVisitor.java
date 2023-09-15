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
package ortus.boxlang.transpiler.transformer.indexer;

import com.github.javaparser.Position;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.printer.DefaultPrettyPrinterVisitor;
import com.github.javaparser.printer.configuration.PrinterConfiguration;
import ortus.boxlang.ast.BoxNode;

import static ortus.boxlang.transpiler.transformer.indexer.BoxNodeKey.BOX_NODE_DATA_KEY;

/**
 * Visitor to walk the tree and resolve the cross-reference
 */
public class IndexPrettyPrinterVisitor extends DefaultPrettyPrinterVisitor {

    public IndexPrettyPrinterVisitor( PrinterConfiguration configuration ) {
        super( configuration );
    }

    /**
     * Detects if a Node has an BOX_NODE_DATA_KEY with a BoxNode
     * 
     * @param node a Java Parser Node
     * 
     * @return the associated BoxNode (if any) or null
     */
    private Object hasBoxNodeKey( Node node ) {

        if ( !node.getDataKeys().isEmpty() ) {
            for ( Object key : node.getDataKeys() ) {
                if ( key.equals( BOX_NODE_DATA_KEY ) ) {
                    return node.getData( BOX_NODE_DATA_KEY );
                }

            }
        }
        return null;
    }

    /**
     * Visits an ExpressionStmt Node
     * 
     * @param node a Java Parser ExpressionStmt
     * @param arg  void
     */
    @Override
    public void visit( ExpressionStmt node, Void arg ) {
        BoxNode boxNode = ( BoxNode ) hasBoxNodeKey( node );
        if ( boxNode != null ) {
            Position start = printer.getCursor();
            super.visit( node, arg );
            Position end = printer.getCursor();
        } else {
            super.visit( node, arg );
        }
    }

    /**
     * Visits an IfStmt Node
     * 
     * @param node a Java Parser IfStmt
     * @param arg  void
     */
    @Override
    public void visit( IfStmt node, Void arg ) {
        BoxNode boxNode = ( BoxNode ) hasBoxNodeKey( node );
        if ( boxNode != null ) {
            Position start = printer.getCursor();
            super.visit( node, arg );
            Position end = printer.getCursor();
        } else {
            super.visit( node, arg );
        }
    }

    /**
     * Visits an DoStmt Node
     * 
     * @param node a Java Parser DoStmt
     * @param arg  void
     */
    @Override
    public void visit( DoStmt node, Void arg ) {
        BoxNode boxNode = ( BoxNode ) hasBoxNodeKey( node );
        if ( boxNode != null ) {
            Position start = printer.getCursor();
            super.visit( node, arg );
            Position end = printer.getCursor();
        } else {
            super.visit( node, arg );
        }
    }

}
