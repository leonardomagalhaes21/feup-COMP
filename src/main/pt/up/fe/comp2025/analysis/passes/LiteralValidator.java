package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

public class LiteralValidator extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.BOOLEAN_LITERAL, this::visitBooleanLiteral);
        addVisit(Kind.INTEGER_LITERAL, this::visitIntegerLiteral);
    }

    private Void visitBooleanLiteral(JmmNode booleanLiteral, SymbolTable table) {
        // Implement validation logic for boolean literals
        return null;
    }

    private Void visitIntegerLiteral(JmmNode integerLiteral, SymbolTable table) {
        // Implement validation logic for integer literals
        return null;
    }
}