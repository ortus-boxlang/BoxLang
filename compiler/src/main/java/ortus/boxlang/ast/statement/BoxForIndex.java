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
package ortus.boxlang.ast.statement;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.Position;

import java.util.Collections;
import java.util.List;

/**
 * AST Node representing a for statement like:
 * <code>for(variable;expression;expression) body</code>
 */
public class BoxForIndex extends BoxStatement {

    private final BoxExpr            variable;
    private final BoxExpr            initial;
    private final BoxExpr            condition;
    private final BoxExpr            step;
    private final List<BoxStatement> body;

    /**
     *
     * @param variable
     * @param condition
     * @param body
     * @param position
     * @param sourceText
     */
    public BoxForIndex( BoxExpr variable, BoxExpr initial, BoxExpr condition, BoxExpr step, List<BoxStatement> body, Position position, String sourceText ) {
        super( position, sourceText );
        this.variable = variable;
        this.variable.setParent( this );
        this.initial = initial;
        this.initial.setParent( this );
        this.condition = condition;
        this.condition.setParent( this );
        this.step = step;
        this.step.setParent( this );

        this.body = Collections.unmodifiableList( body );
        this.body.forEach( arg -> arg.setParent( this ) );
    }

    public BoxExpr getVariable() {
        return variable;
    }

    public BoxExpr getCondition() {
        return condition;
    }

    public BoxExpr getStep() {
        return step;
    }

    public BoxExpr getInitial() {
        return initial;
    }

    public List<BoxStatement> getBody() {
        return body;
    }
}
