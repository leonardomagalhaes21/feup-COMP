package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.List;
import java.util.stream.Collectors;

public class StmtValidator extends AnalysisVisitor {

    @Override
    public void buildVisitor() {
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.ARRAY_ASSIGN_STMT, this::visitArrayAssignStmt);
        addVisit(Kind.BLOCK_STMT, this::visitBlockStmt);

    }
    @Override
    public Void visit(JmmNode node, SymbolTable table) {
        if (node.getKind().equals("Method")) {
            checkStatementsAfterReturn(node);
        } else if (node.getKind().equals("BlockStmt") ||
                node.getKind().equals("IfStmt") ||
                node.getKind().equals("WhileStmt")) {
            checkBlockForStatementsAfterReturn(node);
        }
        return super.visit(node, table);
    }

    private void checkStatementsAfterReturn(JmmNode methodNode) {
        List<JmmNode> statements = methodNode.getChildren().stream()
                .filter(child -> !child.getKind().equals("Variable") &&
                        !child.getKind().equals("Parameters"))
                .toList();

        checkStatementList(statements);
    }

    private void checkBlockForStatementsAfterReturn(JmmNode blockNode) {
        List<JmmNode> statements = blockNode.getChildren();
        checkStatementList(statements);
    }

    private void checkStatementList(List<JmmNode> statements) {
        boolean foundReturn = false;
        for (JmmNode stmt : statements) {
            if (foundReturn && !stmt.getKind().equals("ElseStmt")) {
                addReport(newError(stmt, "Unreachable code: statement after return"));
            }

            if (stmt.getKind().equals("ReturnStmt")) {
                foundReturn = true;
            } else if (stmt.getKind().equals("BlockStmt")) {
                checkBlockForStatementsAfterReturn(stmt);
            } else if (stmt.getKind().equals("IfStmt")) {
                if (stmt.getNumChildren() >= 2) {
                    JmmNode thenBlock = stmt.getChildren().get(1);
                    checkBlockForStatementsAfterReturn(thenBlock);

                    if (stmt.getNumChildren() >= 3) {
                        JmmNode elseBlock = stmt.getChildren().get(2);
                        checkBlockForStatementsAfterReturn(elseBlock);
                    }
                }
            } else if (stmt.getKind().equals("WhileStmt")) {
                if (stmt.getNumChildren() >= 2) {
                    JmmNode whileBody = stmt.getChildren().get(1);
                    checkBlockForStatementsAfterReturn(whileBody);
                }
            }
        }
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);

        if (assignStmt.getNumChildren() != 2) {
            var message = "Assignment statement must have exactly two children.";
            addReport(Report.newError(Stage.SEMANTIC, assignStmt.getLine(), assignStmt.getColumn(), message, null));
            return null;
        }

        JmmNode left = assignStmt.getChildren().getFirst();
        JmmNode right = assignStmt.getChildren().get(1);

        Type leftType = typeUtils.getExprType(left);
        Type rightType = typeUtils.getExprType(right);

        if (!typeUtils.isAssignable(leftType, rightType)) {
            var message = "Cannot assign value of type '" + rightType.getName() +
                    (rightType.isArray() ? "[]" : "") + "' to variable of type '" +
                    leftType.getName() + (leftType.isArray() ? "[]" : "") + "'.";

            addReport(Report.newError(Stage.SEMANTIC, assignStmt.getLine(), assignStmt.getColumn(), message, null));
            return null;
        }

        if (leftType.isArray() && rightType.isArray()) {
            if (Kind.ARRAY_EXPR.check(right)) {
                for (JmmNode element : right.getChildren()) {
                    Type elementType = typeUtils.getExprType(element);
                    Type expectedType = new Type(leftType.getName(), false);

                    if (!typeUtils.isAssignable(expectedType, elementType)) {
                        String errorMessage = "Expected array elements of type '" + expectedType.getName() +
                                "', but found type '" + elementType.getName() + "'.";
                        addReport(Report.newError(Stage.SEMANTIC, assignStmt.getLine(), assignStmt.getColumn(), errorMessage, null));
                    }
                }
            }
        }

        if (Kind.NEW_EXPR.check(right) && !right.hasAttribute("classname")) {
            var message = "Node NewExpr does not contain attribute 'classname'.";
            addReport(Report.newError(Stage.SEMANTIC, right.getLine(), right.getColumn(), message, null));
            return null;
        }

        return null;
    }

    private Void visitArrayAssignStmt(JmmNode arrayAssignStmt, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);

        if (arrayAssignStmt.getNumChildren() != 3) {
            var message = "Array assignment statement must have exactly three children.";
            addReport(Report.newError(Stage.SEMANTIC, arrayAssignStmt.getLine(), arrayAssignStmt.getColumn(), message, null));
            return null;
        }

        JmmNode array = arrayAssignStmt.getChildren().getFirst();
        JmmNode index = arrayAssignStmt.getChildren().get(1);
        JmmNode value = arrayAssignStmt.getChildren().get(2);

        Type arrayType = typeUtils.getExprType(array);
        Type indexType = typeUtils.getExprType(index);
        Type valueType = typeUtils.getExprType(value);

        // Check if array is actually an array
        if (!arrayType.isArray()) {
            var message = "Cannot perform array access on non-array type '" + arrayType.getName() + "'.";
            addReport(Report.newError(Stage.SEMANTIC, arrayAssignStmt.getLine(), arrayAssignStmt.getColumn(), message, null));
            return null;
        }

        // Check if index is an integer
        if (!indexType.getName().equals("int") || indexType.isArray()) {
            var message = "Array index must be of type 'int', but found type '" +
                    indexType.getName() + (indexType.isArray() ? "[]" : "") + "'.";
            addReport(Report.newError(Stage.SEMANTIC, arrayAssignStmt.getLine(), arrayAssignStmt.getColumn(), message, null));
            return null;
        }

        // Check if value type is compatible with array element type
        Type elementType = new Type(arrayType.getName(), false);
        if (!typeUtils.isAssignable(elementType, valueType)) {
            var message = "Cannot assign value of type '" + valueType.getName() +
                    (valueType.isArray() ? "[]" : "") + "' to array element of type '" +
                    elementType.getName() + "'.";
            addReport(Report.newError(Stage.SEMANTIC, arrayAssignStmt.getLine(), arrayAssignStmt.getColumn(), message, null));
            return null;
        }

        return null;
    }

    private Void visitBlockStmt(JmmNode blockStmt, SymbolTable symbolTable) {
        List<JmmNode> statements = blockStmt.getChildren();

        boolean returnFound = false;

        for (int i = 0; i < statements.size(); i++) {
            JmmNode stmt = statements.get(i);

            if (returnFound) {
                // We found code after a return statement
                var message = "Unreachable code: statement after return.";
                addReport(Report.newError(Stage.SEMANTIC, stmt.getLine(), stmt.getColumn(), message, null));
            }

            // Check if this is a return statement
            if (Kind.RETURN_STMT.check(stmt)) {
                returnFound = true;
            }
        }

        return null;
    }
}