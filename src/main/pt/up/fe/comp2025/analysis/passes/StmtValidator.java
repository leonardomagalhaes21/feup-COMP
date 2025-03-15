package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

public class StmtValidator extends AnalysisVisitor {
    @Override
    public void buildVisitor() {

        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.BLOCK_STMT, this::visitBlockStmt);
        addVisit(Kind.EXPR_STMT, this::visitExprStmt);
        addVisit(Kind.ARRAY_ASSIGN_STMT, this::visitArrayAssignStmt);
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);
        var left = assignStmt.getChildren().get(0);
        var right = assignStmt.getChildren().get(1);

        var leftType = typeUtils.getExprType(left);
        var rightType = typeUtils.getExprType(right);

        if (!typeUtils.isAssignable(leftType, rightType)) {
            var message = "Cannot assign a value of type '" + rightType.getName() + "' to a variable of type '" + leftType.getName() + "'.";
            addReport(Report.newError(Stage.SEMANTIC, assignStmt.getLine(), assignStmt.getColumn(), message, null));
            return null;
        }

        if (leftType.isArray() && rightType.isArray()) {
            if (Kind.ARRAY_EXPR.check(right)) {
                for (JmmNode element : right.getChildren()) {
                    Type elementType = typeUtils.getExprType(element);
                    Type expectedType = new Type(leftType.getName(), false);

                    if (!typeUtils.isAssignable(expectedType, elementType)) {
                        String errorMessage = "Expected array elements of type '" + expectedType.getName() + "', but found type '" + elementType.getName() + "'.";
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


    private Void visitBlockStmt(JmmNode blockStmt, SymbolTable table) {
        // Implement validation logic for block statements
        return null;
    }

    private Void visitExprStmt(JmmNode exprStmt, SymbolTable table) {
        // Implement validation logic for expression statements
        return null;
    }

    private Void visitArrayAssignStmt(JmmNode arrayAssignStmt, SymbolTable table) {
        // Implement validation logic for array assignment statements
        return null;
    }

}