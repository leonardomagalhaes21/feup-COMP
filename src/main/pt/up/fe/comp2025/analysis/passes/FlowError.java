package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;
import pt.up.fe.comp2025.ast.TypeName;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

public class FlowError extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.IF_STMT, this::ifStmt);
        addVisit(Kind.WHILE_STMT, this::whileStmt);
    }

    private Void ifStmt(JmmNode ifStmt, SymbolTable table) {
        JmmNode condition = ifStmt.getChildren().get(0);
        JmmNode thenBranch = ifStmt.getChildren().get(1);
        JmmNode elseBranch = ifStmt.getChildren().size() > 2 ? ifStmt.getChildren().get(2) : null;

        TypeUtils typeUtils = new TypeUtils(table);
        var conditionType = typeUtils.getExprType(condition);

        if (!conditionType.getName().equals(TypeName.BOOLEAN.getName())) {
            var message = "Condition of 'if' statement must be of type 'boolean', but found '" + conditionType.getName() + "'.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    ifStmt.getLine(),
                    ifStmt.getColumn(),
                    message,
                    null)
            );
        }

        // Additional validation for thenBranch and elseBranch
        if (!isValidStatement(thenBranch, table)) {
            var message = "Invalid statement in 'then' branch of 'if' statement.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    thenBranch.getLine(),
                    thenBranch.getColumn(),
                    message,
                    null)
            );
        }

        if (elseBranch != null && !isValidStatement(elseBranch, table)) {
            var message = "Invalid statement in 'else' branch of 'if' statement.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    elseBranch.getLine(),
                    elseBranch.getColumn(),
                    message,
                    null)
            );
        }

        return null;
    }

    private Void whileStmt(JmmNode whileStmt, SymbolTable table) {
        JmmNode condition = whileStmt.getChildren().get(0);
        JmmNode body = whileStmt.getChildren().get(1);

        TypeUtils typeUtils = new TypeUtils(table);

        var conditionType = typeUtils.getExprType(condition);

        if (!conditionType.getName().equals(TypeName.BOOLEAN.getName())) {
            var message = "Condition of 'while' statement must be of type 'boolean', but found '" + conditionType.getName() + "'.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    whileStmt.getLine(),
                    whileStmt.getColumn(),
                    message,
                    null)
            );
        }

        // Additional validation for body
        if (!isValidStatement(body, table)) {
            var message = "Invalid statement in body of 'while' statement.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    body.getLine(),
                    body.getColumn(),
                    message,
                    null)
            );
        }

        return null;
    }

    private boolean isValidStatement(JmmNode node, SymbolTable table) {
        // Implement logic to check if the node represents a valid statement
        // This can include checking for valid expressions, valid variable declarations, etc.

        return false;
    }
}