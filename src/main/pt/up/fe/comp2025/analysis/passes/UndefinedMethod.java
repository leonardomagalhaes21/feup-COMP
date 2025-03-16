package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;

public class UndefinedMethod extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        // Implement the visitor logic
        addVisit(Kind.FUNC_EXPR, this::visitFuncExpr);
    }

    private Void visitFuncExpr(JmmNode funcExpr, SymbolTable table) {
        var method = funcExpr.get("methodname");

        if (table.getMethods().contains(method) || method.equals("length")) {
            return null;
        }

        TypeUtils typeUtils = new TypeUtils(table);

        var caller = funcExpr.getChildren().getFirst();
        var callerType = typeUtils.getExprType(caller);

        var imports = table.getImports();
        var superClass = table.getSuper();

        if (superClass != null && imports.contains(superClass)) {
            return null;
        }

        if (imports.contains(callerType.getName())) {
            return null;
        }

        var message = String.format("Method '%s' does not exist.", method);
        addReport(Report.newError(
                Stage.SEMANTIC,
                funcExpr.getLine(),
                funcExpr.getColumn(),
                message,
                null)
        );

        return null;
    }
}
