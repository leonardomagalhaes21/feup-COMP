package pt.up.fe.comp2025.analysis.passes;

import  pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;

public class TypeError extends AnalysisVisitor {

    private JmmNode currentClass;
    private JmmNode currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.CLASS_DECL, this::visitClassDecl);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.RETURN_STMT, this::visitReturnStmt);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.ARRAY_ACCESS_EXPR, this::visitArrayAccessExpr);
        addVisit(Kind.ARRAY_EXPR, this::visitArrayExpr);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
    }
    /*
            BINARY_EXPR,
            VAR_REF_EXPR,
            LENGTH_EXPR,
            PAREN_EXPR,
            UNARY_EXPR,
            FUNC_EXPR,
            MEMBER_EXPR,
            BOOLEAN_LITERAL,
            ARRAY_EXPR,
            NEW_EXPR,
            THIS_EXPR,
            NEW_ARRAY_EXPR
     */

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method;
        return null;
    }

    private Void visitClassDecl(JmmNode classDecl, SymbolTable table) {
        currentClass = classDecl;
        return null;
    }

    private Void visitReturnStmt(JmmNode returnStmt, SymbolTable table) {
        var methodReturnType = table.getReturnType(currentMethod.get("name"));

        if(methodReturnType.getName().equals(TypeUtils.TypeName.VOID.getName())) {
            if(returnStmt.getNumChildren() > 0) {
                var message = "Cannot return a value from a method that returns 'void'.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        returnStmt.getLine(),
                        returnStmt.getColumn(),
                        message,
                        null));
            }
        }

        return null;
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);
        var left = assignStmt.getChildren().get(0);
        var right = assignStmt.getChildren().get(1);

        var leftType = typeUtils.getExprType(left);
        var rightType = typeUtils.getExprType(right);

        if (!TypeUtils.isAssignable(leftType, rightType)) {
            var message = "Cannot assign a value of type '" + rightType.getName() + "' to a variable of type '" + leftType.getName() + "'.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    assignStmt.getLine(),
                    assignStmt.getColumn(),
                    message,
                    null)
            );
            return null;
        }

        if (leftType.isArray() && rightType.isArray()) {
            //TODO
        }
        return null;

    }


    private Void visitArrayAccessExpr(JmmNode arrayAccessExpr, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);

        /*
        if (arrayAccessExpr.getNumChildren() != 2) {
            var message = "Array access must have exactly two children, but found " + arrayAccessExpr.getNumChildren() + ".";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    arrayAccessExpr.getLine(),
                    arrayAccessExpr.getColumn(),
                    message,
                    null)
            );
            return null;
        }
        */
        JmmNode arrayExpr = arrayAccessExpr.getChildren().getFirst();
        JmmNode indexExpr = arrayAccessExpr.getChildren().get(1);

        var arrayType = typeUtils.getExprType(arrayExpr);
        var indexType = typeUtils.getExprType(indexExpr);

        if (!arrayType.isArray()) {
            var message = "Array access must be performed on an array, but found '" + arrayType.getName() + "'.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    arrayAccessExpr.getLine(),
                    arrayAccessExpr.getColumn(),
                    message,
                    null)
            );
            return null;
        }

        if (!indexType.getName().equals(TypeUtils.TypeName.INT.getName())) {
            var message = "Array index must be of type 'int', but found '" + indexType.getName() + "'.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    arrayAccessExpr.getLine(),
                    arrayAccessExpr.getColumn(),
                    message,
                    null)
            );
            return null;
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

        if (!TypeUtils.isAssignable(leftType, rightType)) {
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

    private Void visitArrayExpr(JmmNode arrayExpr, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);
        var arrayType = typeUtils.getExprType(arrayExpr);
        //os arrays tem de ter os valores todos do mesmo tip
        for (var child : arrayExpr.getChildren()) {
            var childType = typeUtils.getExprType(child);
            if (!TypeUtils.isAssignable(arrayType, childType)) {
                var message = "Array elements must have the same type, but found '" + arrayType.getName() + "' and '" + childType.getName() + "'.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        arrayExpr.getLine(),
                        arrayExpr.getColumn(),
                        message,
                        null)
                );
                return null;
            }
        }
        return null;
    }
}
