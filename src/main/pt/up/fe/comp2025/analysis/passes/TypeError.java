package pt.up.fe.comp2025.analysis.passes;

import  pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.comp2025.ast.TypeName;


public class TypeError extends AnalysisVisitor {

    private JmmNode currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.RETURN_STMT, this::visitReturnStmt);
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
    }


    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method;
        return null;
    }

    private Void visitReturnStmt(JmmNode returnStmt, SymbolTable table) {
        var methodReturnType = table.getReturnType(currentMethod.get("name"));

        if (methodReturnType.getName().equals(TypeName.VOID.getName())) {
            if (returnStmt.getNumChildren() > 0) {
                var message = "Cannot return a value from a method that returns 'void'.";
                addReport(Report.newError(Stage.SEMANTIC, returnStmt.getLine(), returnStmt.getColumn(), message, null));
            }
        } else {
            var returnType = new TypeUtils(table).getExprType(returnStmt.getChildren().get(0));
            if (!methodReturnType.equals(returnType)) {
                var message = "Incompatible return type. Expected '" + methodReturnType.getName() + "' but found '" + returnType.getName() + "'.";
                addReport(Report.newError(Stage.SEMANTIC, returnStmt.getLine(), returnStmt.getColumn(), message, null));
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

}
