package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.specs.util.SpecsCheck;

/**
 * Checks if the type of the expression in a return statement is compatible with the method return type.
 *
 * @author JBispo
 */
public class UndeclaredVariable extends AnalysisVisitor {

    private String currentMethod;
    private boolean isCurrentStatic;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.VAR_REF_EXPR, this::visitVarRefExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        isCurrentStatic = method.getOptional("isStatic").map(Boolean::parseBoolean).orElse(false);
        return null;
    }

    private Void visitVarRefExpr(JmmNode varRefExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        // Check if exists a parameter or variable declaration with the same name as the variable reference
        var varRefName = varRefExpr.get("name");

        // Var is a parameter, return
        if (table.getParameters(currentMethod).stream()
                .anyMatch(param -> param.getName().equals(varRefName))) {
            return null;
        }

        // Var is a declared variable, return
        if (table.getLocalVariables(currentMethod).stream()
                .anyMatch(varDecl -> varDecl.getName().equals(varRefName))) {
            return null;
        }

        // Var is a field, check if it is static
        var fieldOpt = table.getFields().stream()
                .filter(field -> field.getName().equals(varRefName))
                .findFirst();
        if (fieldOpt.isPresent()) {
            if (isCurrentStatic) {
                var message = "Cannot access instance field " + varRefName + " in a static context.";
                addReport(Report.newError(Stage.SEMANTIC, varRefExpr.getLine(), varRefExpr.getColumn(), message, null));
            }
            return null;
        }

        // Var is an imported class, return
        if (table.getImports().stream()
                .anyMatch(importName -> {
                    // Check full import name or just the class name
                    String className = importName.contains(".") ?
                            importName.substring(importName.lastIndexOf(".") + 1) : importName;
                    return importName.equals(varRefName) || className.equals(varRefName);
                })) {
            return null;
        }

        // Create error report
        var message = String.format("Variable '%s' does not exist.", varRefName);
        addReport(Report.newError(
                Stage.SEMANTIC,
                varRefExpr.getLine(),
                varRefExpr.getColumn(),
                message,
                null)
        );
        return null;
    }
}
