package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;

public class VarargsValidator extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
    }

    private Void visitVarDecl(JmmNode varDecl, SymbolTable table) {
        // Check variable type
        var varType = varDecl.getChildren().getFirst();
        if (varType.hasAttribute("isVarargs") && Boolean.parseBoolean(varType.get("isVarargs"))) {
            var message = "Local variables cannot be varargs.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    varType.getLine(),
                    varType.getColumn(),
                    message,
                    null)
            );
        }

        return null;
    }

    private Void visitMethodDecl(JmmNode methodDecl, SymbolTable table) {
        // Check return type
        var returnType = methodDecl.getChildren().getFirst();
        if (returnType.hasAttribute("isVarargs") && Boolean.parseBoolean(returnType.get("isVarargs"))) {
            var message = "Return type cannot be varargs.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    returnType.getLine(),
                    returnType.getColumn(),
                    message,
                    null)
            );
        }

        return null;
    }
}