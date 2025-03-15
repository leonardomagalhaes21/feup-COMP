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
        if (!funcExpr.hasAttribute("name")) {
            var message = "Node FuncExpr does not contain attribute 'name'.";
            addReport(Report.newError(Stage.SEMANTIC, funcExpr.getLine(), funcExpr.getColumn(), message, null));
            return null;
        }

        var methodName = funcExpr.get("name");

        // Check if the method is declared
        if (!table.getMethods().contains(methodName)) {
            var message = "Method '" + methodName + "' is not declared.";
            addReport(Report.newError(Stage.SEMANTIC, funcExpr.getLine(), funcExpr.getColumn(), message, null));
            return null;
        }

        var args = funcExpr.getChildren();
        var params = table.getParameters(methodName);

        if (args.size() != params.size()) {
            var message = "Incompatible number of arguments for method '" + methodName + "'. Expected " + params.size() + " but found " + args.size() + ".";
            addReport(Report.newError(Stage.SEMANTIC, funcExpr.getLine(), funcExpr.getColumn(), message, null));
            return null;
        }

        for (int i = 0; i < args.size(); i++) {
            var argType = new TypeUtils(table).getExprType(args.get(i));
            var paramType = params.get(i).getType();

            if (!argType.equals(paramType)) {
                var message = "Incompatible argument type for parameter " + (i + 1) + " of method '" + methodName + "'. Expected '" + paramType.getName() + "' but found '" + argType.getName() + "'.";
                addReport(Report.newError(Stage.SEMANTIC, funcExpr.getLine(), funcExpr.getColumn(), message, null));
                return null;
            }
        }

        return null;
    }

    private Void visitMemberExpr(JmmNode memberExpr, SymbolTable table) {
        TypeUtils typeUtils = new TypeUtils(table);
        JmmNode objectExpr = memberExpr.getChildren().getFirst();
        String memberName = memberExpr.get("member");

        // Obter o tipo da expressão do objeto
        var objectType = typeUtils.getExprType(objectExpr);

        // Verificar se o membro existe na classe ou suas superclasses
        if (!table.getMethods().contains(memberName) && !table.getFields().stream().anyMatch(field -> field.getName().equals(memberName))) {
            var message = "Member '" + memberName + "' does not exist in class '" + objectType.getName() + "'.";
            addReport(Report.newError(Stage.SEMANTIC, memberExpr.getLine(), memberExpr.getColumn(), message, null));
            return null;
        }

        return null;
    }

    private Void visitNewExpr(JmmNode newExpr, SymbolTable table) {
        if (!newExpr.hasAttribute("class")) {
            var message = "Node NewExpr does not contain attribute 'class'.";
            addReport(Report.newError(Stage.SEMANTIC, newExpr.getLine(), newExpr.getColumn(), message, null));
            return null;
        }

        var className = newExpr.get("class");

        // Check if the class is imported
        if (!table.getImports().contains(className)) {
            var message = "Class '" + className + "' is not imported.";
            addReport(Report.newError(Stage.SEMANTIC, newExpr.getLine(), newExpr.getColumn(), message, null));
            return null;
        }

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
        TypeUtils typeUtils = new TypeUtils(table);
        JmmNode expr = unaryExpr.getChildren().getFirst();

        var exprType = typeUtils.getExprType(expr);

        // Verificar se o tipo da expressão é compatível com a operação unária
        if (!exprType.getName().equals(TypeName.INT.getName()) && !exprType.getName().equals(TypeName.BOOLEAN.getName())) {
            var message = "Unary expression must be of type 'int' or 'boolean', but found '" + exprType.getName() + "'.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    unaryExpr.getLine(),
                    unaryExpr.getColumn(),
                    message,
                    null)
            );
            return null;
        }

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


