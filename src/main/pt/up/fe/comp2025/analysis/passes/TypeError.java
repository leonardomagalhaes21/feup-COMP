package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeName;
import pt.up.fe.comp2025.ast.TypeUtils;

/**
 * This visitor validates types used in the program, checking for:
 * - Void used as field type
 * - Multiple return statements in the same scope
 * - Statements after return statements
 * - Type compatibility in expressions and assignments
 * - Boolean conditions in control structures
 */
public class TypeError extends AnalysisVisitor {

    private JmmNode currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.RETURN_STMT, this::visitReturnStmt);
        addVisit(Kind.BLOCK_STMT, this::visitBlockStmt);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit(Kind.IF_STMT, this::visitIfStmt);
        addVisit(Kind.WHILE_STMT, this::visitWhileStmt);
    }

    private Void visitVarDecl(JmmNode varDecl, SymbolTable table) {
        // Check if this is a field declaration (parent is class)
        if (Kind.CLASS_DECL.check(varDecl.getParent())) {
            JmmNode typeNode = varDecl.getChildren().getFirst();
            String typeName = typeNode.get("name");

            // Check if field type is void
            if (typeName.equals("void")) {
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        varDecl.getLine(),
                        varDecl.getColumn(),
                        "Field cannot have void type",
                        null
                ));
            }
        }
        return null;
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method;

        String methodName = method.get("name");
        Type returnType = table.getReturnType(methodName);

        // Check for multiple returns in the same code path
        boolean hasMultipleReturns = method.getDescendants(Kind.RETURN_STMT).size() > 1;

        if (hasMultipleReturns) {
            JmmNode lastReturnStmt = null;
            for (JmmNode returnStmt : method.getDescendants(Kind.RETURN_STMT)) {
                if (lastReturnStmt != null) {
                    // Check if returns are in different branches (if/else)
                    if (!areInDifferentBranches(lastReturnStmt, returnStmt)) {
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                returnStmt.getLine(),
                                returnStmt.getColumn(),
                                "Multiple return statements in the same code path",
                                null
                        ));
                        break;
                    }
                }
                lastReturnStmt = returnStmt;
            }
        }

        // Skip void methods for return path check
        if (returnType == null || returnType.getName().equals(TypeName.VOID.getName())) {
            return null;
        }

        // Check if the method has a complete return path
        boolean hasCompleteReturnPath = false;

        // Find the method body (statements are typically children of method node)
        for (JmmNode child : method.getChildren()) {
            if (hasCompleteReturnPath(child)) {
                hasCompleteReturnPath = true;
                break;
            }
        }

        if (!hasCompleteReturnPath) {
            var message = "Missing return statement. Method '" + methodName +
                    "' must return a value of type '" + returnType.getName() + "' in all execution paths.";
            addReport(Report.newError(Stage.SEMANTIC, method.getLine(), method.getColumn(), message, null));
        }

        return null;
    }

    private boolean areInDifferentBranches(JmmNode node1, JmmNode node2) {
        // Find first common ancestor that is an if statement
        JmmNode current1 = node1;
        while (current1 != null) {
            JmmNode current2 = node2;
            while (current2 != null) {
                if (current1 == current2) {
                    // Found common ancestor, check if it's an IF statement
                    return Kind.IF_STMT.check(current1);
                }
                current2 = current2.getParent();
            }
            current1 = current1.getParent();
        }
        return false;
    }

    private boolean hasCompleteReturnPath(JmmNode node) {
        // Direct return statement
        if (Kind.RETURN_STMT.check(node)) {
            return true;
        }

        // If statement with both branches having returns
        if (Kind.IF_STMT.check(node)) {
            if (node.getNumChildren() >= 3) {
                JmmNode thenBranch = node.getChildren().get(1);
                JmmNode elseBranch = node.getChildren().get(2);

                return hasCompleteReturnPath(thenBranch) && hasCompleteReturnPath(elseBranch);
            }
            return false;
        }

        // Block statement
        if (Kind.BLOCK_STMT.check(node)) {
            if (node.getChildren().isEmpty()) {
                return false;
            }

            // Check if any child is a return statement
            for (JmmNode child : node.getChildren()) {
                if (hasCompleteReturnPath(child)) {
                    return true;
                }
            }
        }

        // For other node types, check all children
        for (JmmNode child : node.getChildren()) {
            if (hasCompleteReturnPath(child)) {
                return true;
            }
        }

        return false;
    }

    private Void visitReturnStmt(JmmNode returnStmt, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);

        // Get the method this return is in
        var methodNode = returnStmt.getAncestor(Kind.METHOD_DECL)
                .orElseThrow(() -> new RuntimeException("Return statement not in method"));
        String methodName = methodNode.get("name");

        // Get method return type
        Type methodReturnType = table.getReturnType(methodName);
        boolean methodReturnsVoid = methodReturnType.getName().equals("void");

        // If there's a return expression in a void method
        if (methodReturnsVoid && returnStmt.getNumChildren() > 0) {
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    returnStmt.getLine(),
                    returnStmt.getColumn(),
                    "Void methods cannot return a value",
                    null
            ));
            return null;
        }

        // If the return statement has an expression, check type compatibility
        if (!returnStmt.getChildren().isEmpty()) {
            JmmNode returnExpr = returnStmt.getChildren().getFirst();
            Type exprType = typeUtils.getExprType(returnExpr);

            if (!typeUtils.isAssignable(methodReturnType, exprType)) {
                var message = "Incompatible return type: method '" + methodName + "' returns '" +
                        methodReturnType.getName() + (methodReturnType.isArray() ? "[]" : "") +
                        "' but found '" + exprType.getName() + (exprType.isArray() ? "[]" : "") + "'.";
                addReport(Report.newError(Stage.SEMANTIC, returnStmt.getLine(), returnStmt.getColumn(), message, null));
            }
        } else if (!methodReturnType.getName().equals("void")) {
            // If no expression but method doesn't return void
            var message = "Missing return value: method '" + methodName + "' must return '" +
                    methodReturnType.getName() + (methodReturnType.isArray() ? "[]" : "") + "'.";
            addReport(Report.newError(Stage.SEMANTIC, returnStmt.getLine(), returnStmt.getColumn(), message, null));
        }

        return null;
    }

    private Void visitBlockStmt(JmmNode blockStmt, SymbolTable table) {
        boolean foundReturn = false;

        for (JmmNode child : blockStmt.getChildren()) {
            if (foundReturn) {
                // Found a statement after return
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        child.getLine(),
                        child.getColumn(),
                        "Unreachable code: statement after return",
                        null
                ));
            }

            if (Kind.RETURN_STMT.check(child)) {
                foundReturn = true;
            }
        }

        return null;
    }

    private Void visitBinaryExpr(JmmNode binaryExpr, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);
        if (binaryExpr.getNumChildren() != 2) {
            var message = "Binary expression must have exactly two children, but found " + binaryExpr.getNumChildren() + ".";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    binaryExpr.getLine(),
                    binaryExpr.getColumn(),
                    message,
                    null)
            );
            return null;
        }

        JmmNode leftExpr = binaryExpr.getChildren().getFirst();
        JmmNode rightExpr = binaryExpr.getChildren().get(1);

        var leftType = typeUtils.getExprType(leftExpr);
        var rightType = typeUtils.getExprType(rightExpr);

        if (!typeUtils.isAssignable(leftType, rightType)) {
            var message = "Binary expression must have compatible types, but found '" + leftType.getName() + "' and '" + rightType.getName() + "'.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    binaryExpr.getLine(),
                    binaryExpr.getColumn(),
                    message,
                    null)
            );
            return null;
        }

        return null;
    }

    private Void visitIfStmt(JmmNode ifStmt, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);

        // Validate if statement has correct number of children (condition + then block + optional else block)
        if (ifStmt.getNumChildren() < 2 || ifStmt.getNumChildren() > 3) {
            var message = "If statement must have 2 or 3 children (condition, then block, optional else block).";
            addReport(Report.newError(Stage.SEMANTIC, ifStmt.getLine(), ifStmt.getColumn(), message, null));
            return null;
        }

        // Check if condition is boolean
        JmmNode condition = ifStmt.getChildren().getFirst();
        Type conditionType = typeUtils.getExprType(condition);

        if (!TypeUtils.isBoolean(conditionType)) {
            var message = "If condition must be of type 'boolean', but found '" + conditionType.getName() +
                    (conditionType.isArray() ? "[]" : "") + "'.";
            addReport(Report.newError(Stage.SEMANTIC, condition.getLine(), condition.getColumn(), message, null));
        }

        return null;
    }

    private Void visitWhileStmt(JmmNode whileStmt, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);

        // Validate while statement has correct number of children (condition + body)
        if (whileStmt.getNumChildren() != 2) {
            var message = "While statement must have exactly 2 children (condition and body).";
            addReport(Report.newError(Stage.SEMANTIC, whileStmt.getLine(), whileStmt.getColumn(), message, null));
            return null;
        }

        // Check if condition is boolean
        JmmNode condition = whileStmt.getChildren().getFirst();
        Type conditionType = typeUtils.getExprType(condition);

        if (!TypeUtils.isBoolean(conditionType)) {
            var message = "While condition must be of type 'boolean', but found '" + conditionType.getName() +
                    (conditionType.isArray() ? "[]" : "") + "'.";
            addReport(Report.newError(Stage.SEMANTIC, condition.getLine(), condition.getColumn(), message, null));
        }

        return null;
    }
}