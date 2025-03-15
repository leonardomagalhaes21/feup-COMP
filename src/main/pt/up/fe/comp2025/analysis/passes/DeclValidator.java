package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

public class DeclValidator extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.FIELD_DECL, this::visitFieldDecl);
        addVisit(Kind.IMPORT_DECL, this::visitImportDecl);
        addVisit(Kind.VAR_DECL, this::visitVarDecl);
        addVisit(Kind.PARAM, this::visitParam);
    }

    private Void visitFieldDecl(JmmNode fieldDecl, SymbolTable table) {
        // Implement validation logic for field declarations
        return null;
    }

    private Void visitImportDecl(JmmNode importDecl, SymbolTable table) {
        // Implement validation logic for import declarations
        return null;
    }

    private Void visitVarDecl(JmmNode varDecl, SymbolTable table) {
        // Implement validation logic for variable declarations
        return null;
    }

    private Void visitParam(JmmNode param, SymbolTable table) {
        // Implement validation logic for parameters
        return null;
    }
}