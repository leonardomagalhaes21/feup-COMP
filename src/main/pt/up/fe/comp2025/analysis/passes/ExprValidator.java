package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeName;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

public class ExprValidator extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.FUNC_EXPR, this::visitFuncExpr);
        addVisit(Kind.MEMBER_EXPR, this::visitMemberExpr);
        addVisit(Kind.ARRAY_ACCESS_EXPR, this::visitArrayAccessExpr);
        addVisit(Kind.ARRAY_EXPR, this::visitArrayExpr);
        addVisit(Kind.NEW_EXPR, this::visitNewExpr);
        addVisit(Kind.NEW_ARRAY_EXPR, this::visitNewArrayExpr);
        addVisit(Kind.PAREN_EXPR, this::visitParenExpr);
        addVisit(Kind.UNARY_EXPR, this::visitUnaryExpr);
        addVisit(Kind.THIS_EXPR, this::visitThisExpr);
        addVisit(Kind.EXPR, this::visitExpr);
    }
    private Void visitArrayExpr(JmmNode arrayExpr, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);
        var arrayType = typeUtils.getExprType(arrayExpr);
        for (var child : arrayExpr.getChildren()) {
            var childType = typeUtils.getExprType(child);
            if (!typeUtils.isAssignable(arrayType, childType)) {
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
    private Void visitArrayAccessExpr(JmmNode arrayAccessExpr, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);
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

        if (!indexType.getName().equals(TypeName.INT.getName())) {
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

    private Void visitFuncExpr(JmmNode funcExpr, SymbolTable table) {
        return null;
    }

    private Void visitMemberExpr(JmmNode memberExpr, SymbolTable table) {
        return null;
    }

    private Void visitNewExpr(JmmNode newExpr, SymbolTable table){
        return null;
    }

    private Void visitNewArrayExpr(JmmNode newArrayExpr, SymbolTable table){
        return null;
    }

    private Void visitParenExpr(JmmNode parenExpr, SymbolTable table) {
        // Implement validation logic for parenthesis expressions
        return null;
    }

    private Void visitUnaryExpr(JmmNode unaryExpr, SymbolTable table) {
        // Implement validation logic for unary expressions
        return null;
    }

    private Void visitThisExpr(JmmNode thisExpr, SymbolTable table) {
        // Implement validation logic for 'this' expressions
        return null;
    }
    private Void visitExpr(JmmNode expr, SymbolTable table) {
        return null;
    }
}