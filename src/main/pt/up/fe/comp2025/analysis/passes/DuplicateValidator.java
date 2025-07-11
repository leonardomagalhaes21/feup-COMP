package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;

import java.util.HashSet;
import java.util.Set;

public class DuplicateValidator extends AnalysisVisitor {

    private final Set<String> seenImports = new HashSet<>();

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.CLASS_DECL, this::visitClassDecl);
        addVisit(Kind.IMPORT_DECL, this::visitImportDecl);
    }

    private Void visitClassDecl(JmmNode classDecl, SymbolTable table) {
        Set<String> fieldNames = new HashSet<>();
        Set<String> methodNames = new HashSet<>();

        for (var field : table.getFields()) {
            if (!fieldNames.add(field.getName())) {
                var message = "Duplicate field '" + field.getName() + "' in class.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        classDecl.getLine(),
                        classDecl.getColumn(),
                        message,
                        null
                ));
            }
        }

        for (var method : table.getMethods()) {
            if (!methodNames.add(method)) {
                var message = "Duplicate method '" + method + "' in class.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        classDecl.getLine(),
                        classDecl.getColumn(),
                        message,
                        null
                ));
            }
        }

        return null;
    }

    private Void visitMethodDecl(JmmNode methodDecl, SymbolTable table) {
        Set<String> variableNames = new HashSet<>();

        // Check parameters
        var methodName = methodDecl.get("name");
        for (var param : table.getParameters(methodName)) {
            if (!variableNames.add(param.getName())) {
                var message = "Duplicate parameter '" + param.getName() + "' in method '" + methodName + "'.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        methodDecl.getLine(),
                        methodDecl.getColumn(),
                        message,
                        null
                ));
            }
        }

        // Check local variables
        for (var localVar : table.getLocalVariables(methodName)) {
            if (!variableNames.add(localVar.getName())) {
                var message = "Duplicate local variable '" + localVar.getName() + "' in method '" + methodName + "'.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        methodDecl.getLine(),
                        methodDecl.getColumn(),
                        message,
                        null
                ));
            }
        }

        // Check varargs
        var parameters = methodDecl.getChild(1).getChildren(Kind.PARAM);
        Set<String> paramNames = new HashSet<>();
        boolean hasVarargs = false;
        boolean isLast = true;

        JmmNode parameter = null;
        for (var i = 0; i < parameters.size(); i++) {
            parameter = parameters.get(i);
            var parameterType = parameter.getChildren(Kind.TYPE).getFirst();

            if (!paramNames.add(parameter.get("name"))) {
                var message = "Duplicate parameter name '" + parameter.get("name") + "'.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        parameter.getLine(),
                        parameter.getColumn(),
                        message,
                        null)
                );
            }

            if (Boolean.parseBoolean(parameterType.get("isVarargs"))) {
                if (hasVarargs) {
                    var message = "Only one varargs parameter is allowed.";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            parameter.getLine(),
                            parameter.getColumn(),
                            message,
                            null)
                    );
                    return null;
                }

                if (i != parameters.size() - 1) isLast = false;

                hasVarargs = true;
            }
        }

        if (hasVarargs && !isLast) {
            var message = "Varargs parameter must be the last parameter.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    parameter.getLine(),
                    parameter.getColumn(),
                    message,
                    null)
            );
        }

        return null;
    }

    private Void visitImportDecl(JmmNode importDecl, SymbolTable table) {
        String importName = importDecl.get("ID");

        if (seenImports.contains(importName)) {
            var message = "Duplicate import detected: '" + importName + "'.";
            addReport(Report.newError(Stage.SEMANTIC, importDecl.getLine(), importDecl.getColumn(), message, null));
        } else {
            seenImports.add(importName);
        }

        return null;
    }
}