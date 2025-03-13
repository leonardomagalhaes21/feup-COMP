package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

public class LengthExprValidator extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.LENGTH_EXPR, this::visitLengthExpr);
    }

    private Void visitLengthExpr(JmmNode lengthExpr, SymbolTable table) {
        // Implement validation logic for length expressions
        return null;
    }
}