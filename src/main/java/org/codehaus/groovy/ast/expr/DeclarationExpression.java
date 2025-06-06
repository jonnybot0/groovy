/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.codehaus.groovy.ast.expr;

import org.codehaus.groovy.GroovyBugError;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GroovyCodeVisitor;
import org.codehaus.groovy.syntax.Token;

import static org.apache.groovy.ast.tools.ClassNodeUtils.formatTypeName;

/**
 * Represents one or more local variables. Typically it is a single local variable
 * declared by name with an expression like "def foo" or with type "String foo". However,
 * the multiple assignment feature allows you to create two or more variables using
 * an expression like: <code>def (x, y) = [1, 2]</code>.
 * <p>
 * You can access the left hand side of a declaration using the
 * "<code>Expression getLeftExpression()</code>" method. In which case you might then
 * use <code>instanceof</code> and casting to perform operations specific to a
 * single local variable (<code>VariableExpression</code>) or for the multiple
 * assignment case (<code>TupleExpression</code>).
 * <p>
 * Alternatively, if <code>isMultipleAssignmentDeclaration()</code> is <code>false</code>
 * you can use the method "<code>VariableExpression getVariableExpression()</code>" method.
 * Similarly, if <code>isMultipleAssignmentDeclaration()</code> is <code>true</code>
 * you can use the method "<code>TupleExpression getTupleExpression()</code>" method.
 * Calling either of these expression getters when the "isMultipleAssignment" condition
 * is not appropriate is unsafe and will result in a <code>ClassCastException</code>.
 */
public class DeclarationExpression extends BinaryExpression {

    /**
     * Creates a declaration like "def v" or "int w = 0".
     *
     * @param left
     *      the left hand side of a variable declaration
     * @param operation
     *      the operation, assumed to be assignment operator
     * @param right
     *      the right hand side of a declaration; {@link EmptyExpression} for no initial value
     */
    public DeclarationExpression(final VariableExpression left, final Token operation, final Expression right) {
        this((Expression) left, operation, right);
    }

    /**
     * Creates a declaration like "def v" or "int w = 0" or "def (x, y) = [1, 2]".
     *
     * @param left
     *      the left hand side of a declaration -- either a {@link VariableExpression} or
     *      a {@link TupleExpression} with at least one element
     * @param operation
     *       the operation, assumed to be assignment operator
     * @param right
     *       the right hand side of a declaration
     */
    public DeclarationExpression(final Expression left, final Token operation, final Expression right) {
        super(left, Token.newSymbol("=", operation.getStartLine(), operation.getStartColumn()), right);
        check(left);
    }

    private static void check(final Expression left) {
        if (left instanceof VariableExpression) {
            // all good
        } else if (left instanceof TupleExpression) {
            TupleExpression tuple = (TupleExpression) left;
            if (tuple.getExpressions().isEmpty())
                throw new GroovyBugError("one element required for left side");
        } else {
            throw new GroovyBugError("illegal left expression for declaration: " + left);
        }
    }

    @Override
    public void visit(GroovyCodeVisitor visitor) {
        visitor.visitDeclarationExpression(this);
    }

    /**
     * This method returns the left hand side of the declaration cast to the VariableExpression type.
     * This is an unsafe method to call. In a multiple assignment statement, the left hand side will
     * be a TupleExpression and a ClassCastException will occur. If you invoke this method then
     * be sure to invoke isMultipleAssignmentDeclaration() first to check that it is safe to do so.
     * If that method returns true then this method is safe to call.
     *
     * @return left hand side of normal variable declarations
     * @throws ClassCastException if the left hand side is not a VariableExpression (and is probably a multiple assignment statement).
     */
    public VariableExpression getVariableExpression() {
        Expression leftExpression = this.getLeftExpression();

        return leftExpression instanceof VariableExpression
                    ? (VariableExpression) leftExpression
                    : null;
    }

    /**
     * This method returns the left hand side of the declaration cast to the TupleExpression type.
     * This is an unsafe method to call. In a single assignment statement, the left hand side will
     * be a VariableExpression and a ClassCastException will occur. If you invoke this method then
     * be sure to invoke isMultipleAssignmentDeclaration() first to check that it is safe to do so.
     * If that method returns true then this method is safe to call.
     * @return
     *      left hand side of multiple assignment declarations
     * @throws ClassCastException
     *      if the left hand side is not a TupleExpression (and is probably a VariableExpression).
     *
     */
    public TupleExpression getTupleExpression() {
        Expression leftExpression = this.getLeftExpression();

        return leftExpression instanceof TupleExpression
                    ? (TupleExpression) leftExpression
                    : null;
    }

    @Override
    public ClassNode getType() {
        return (isMultipleAssignmentDeclaration() ? getTupleExpression() : getVariableExpression()).getType();
    }

    @Override
    public String getText() {
        StringBuilder text = new StringBuilder();

        if (!isMultipleAssignmentDeclaration()) {
            VariableExpression v = getVariableExpression();
            if (v.isDynamicTyped()) {
                text.append("def");
            } else {
                text.append(formatTypeName(v.getOriginType()));
            }
            text.append(' ').append(v.getText());
        } else {
            TupleExpression t = getTupleExpression();
            text.append("def (");
            for (Expression e : t.getExpressions()) {
                if (e instanceof VariableExpression) {
                    VariableExpression v = (VariableExpression) e;
                    if (!v.isDynamicTyped()) {
                        text.append(formatTypeName(v.getOriginType())).append(' ');
                    }
                }
                text.append(e.getText()).append(", ");
            }
            text.setLength(text.length() - 2);
            text.append(')');
        }

        if (getRightExpression() instanceof EmptyExpression) {
            text.append(';');
        } else {
            text.append(' ').append(getOperation().getText());
            text.append(' ').append(getRightExpression().getText());
        }

        return text.toString();
    }

    /**
     * This method sets the leftExpression for this BinaryExpression. The parameter must be
     * either a VariableExpression or a TupleExpression with one or more elements.
     * @param leftExpression
     *      either a VariableExpression or a TupleExpression with one or more elements.
     */
    @Override
    public void setLeftExpression(Expression leftExpression) {
        check(leftExpression);
        super.setLeftExpression(leftExpression);
    }

    @Override
    public void setRightExpression(Expression rightExpression) {
        super.setRightExpression(rightExpression);
    }

    @Override
    public Expression transformExpression(ExpressionTransformer transformer) {
        Expression ret = new DeclarationExpression(transformer.transform(getLeftExpression()),
                getOperation(), transformer.transform(getRightExpression()));
        ret.setSourcePosition(this);
        ret.addAnnotations(getAnnotations());
        ret.setDeclaringClass(getDeclaringClass());
        ret.copyNodeMetaData(this);
        return ret;
    }

    /**
     * This method tells you if this declaration is a multiple assignment declaration, which
     * has the form "def (x, y) = ..." in Groovy. If this method returns true, then the left
     * hand side is an ArgumentListExpression. Do not call "getVariableExpression()" on this
     * object if this method returns true, instead use "getLeftExpression()".
     * @return
     *      true if this declaration is a multiple assignment declaration, which means the
     *      left hand side is an ArgumentListExpression.
     */
    public boolean isMultipleAssignmentDeclaration() {
        return getLeftExpression() instanceof TupleExpression;
    }
}
