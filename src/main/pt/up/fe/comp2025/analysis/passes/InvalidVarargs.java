package pt.up.fe.comp2025.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2025.analysis.AnalysisVisitor;
import pt.up.fe.comp2025.ast.Kind;

import java.util.HashSet;
import java.util.Set;

/**
 * A visitor that checks for incorrect usage of varargs.
 */
public class InvalidVarargs extends AnalysisVisitor {

    /**
     * Creates a new instance of the {@link InvalidVarargs} class.
     */
    @Override
    public void buildVisitor() {
        addVisit(Kind.CLASS_DECL, this::visitClassDecl);
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.IMPORT_DECL, this::visitImportDeclaration);
    }
    private final Set<String> seenImports = new HashSet<>();

    private Void visitImportDeclaration(JmmNode importNode, SymbolTable table) {
        String importName = importNode.get("ID");

        if (seenImports.contains(importName)) {
            var message = "Duplicate import detected: '" + importName + "'.";
            addReport(Report.newError(Stage.SEMANTIC, importNode.getLine(), importNode.getColumn(), message, null));
        } else {
            seenImports.add(importName);
        }

        return null;
    }

    /**
     * Visit a class declaration node and check if any field has a varargs type.
     * If so, add an error report.
     *
     * @param classDecl The class declaration
     * @param table     The symbol table
     */
    private Void visitClassDecl(JmmNode classDecl, SymbolTable table) {
        var fields = classDecl.getChildren(Kind.VAR_DECL);

        for (var field : fields) {
            var fieldType = field.getChildren(Kind.TYPE).getFirst();

            if (Boolean.parseBoolean(fieldType.get("isVarargs"))) {
                var message = "Field type cannot be varargs.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        classDecl.getLine(),
                        classDecl.getColumn(),
                        message,
                        null)
                );
            }
        }

        return null;
    }

    /**
     * Visit a method declaration node and check if any local variable or the return type is varargs.
     * If so, add an error report.
     *
     * @param methodDecl The method declaration
     * @param table      The symbol table
     */
    private Void visitMethodDecl(JmmNode methodDecl, SymbolTable table) {
        var localVariables = methodDecl.getChildren(Kind.VAR_DECL);
        var returnType = methodDecl.getChildren(Kind.TYPE).getFirst();

        // Check local variables for varargs
        for (var localVariable : localVariables) {
            var localVariableType = localVariable.getChildren(Kind.TYPE).getFirst();

            if (Boolean.parseBoolean(localVariableType.get("isVarargs"))) {
                var message = "Local variable type cannot be varargs.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        methodDecl.getLine(),
                        methodDecl.getColumn(),
                        message,
                        null)
                );
            }
        }

        // Check return type for varargs
        if (Boolean.parseBoolean(returnType.get("isVarargs"))) {
            var message = "Method return type cannot be varargs.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    returnType.getLine(),
                    returnType.getColumn(),
                    message,
                    null)
            );
        }

        // Get parameters and check for duplicates
        var parameters = methodDecl.getChild(1).getChildren(Kind.PARAM);
        Set<String> paramNames = new HashSet<>();

        // Check for duplicate parameters
        for (var parameter : parameters) {
            String paramName = parameter.get("name");
            if (!paramNames.add(paramName)) {
                var message = "Duplicate parameter name '" + paramName + "'.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        parameter.getLine(),
                        parameter.getColumn(),
                        message,
                        null)
                );
            }
        }

        // Check varargs - only one allowed and must be last
        boolean hasVarargs = false;
        boolean isLast = true;

        JmmNode parameter = null;
        for (var i = 0; i < parameters.size(); i++) {
            parameter = parameters.get(i);
            var parameterType = parameter.getChildren(Kind.TYPE).getFirst();

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
}