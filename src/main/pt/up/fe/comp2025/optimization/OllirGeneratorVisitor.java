package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.ast.Kind;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static pt.up.fe.comp2025.ast.Kind.*;

/**
 * Main OLLIR code generator visitor that traverses the JMM AST.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<String, String> {
    private final SymbolTable symbolTable;
    private final TypeUtils typeUtils;
    private final OptUtils optUtils;
    private final OllirExprGeneratorVisitor exprGenerator;
    private String currentMethod;
    private int tempCounter = 0;
    private int labelCounter = 0;

    public OllirGeneratorVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.typeUtils = new TypeUtils(symbolTable);
        this.optUtils = new OptUtils(typeUtils);
        this.exprGenerator = new OllirExprGeneratorVisitor(symbolTable);
    }

    @Override
    protected void buildVisitor() {
        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(VAR_DECL, this::visitVarDecl);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(BLOCK_STMT, this::visitBlock);
        addVisit(FIELD_DECL, this::visitFieldDecl);
        addVisit(EXPR_STMT, this::visitExprStmt);
        addVisit(ASSIGN_STMT, this::visitAssign);
        addVisit(ARRAY_ASSIGN_STMT, this::visitArrayAssign);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(IF_STMT, this::visitIf);
        addVisit(WHILE_STMT, this::visitWhile);
        addVisit(IMPORT_DECL, this::visitImport);
        setDefaultVisit(this::defaultVisit);
    }

    private String defaultVisit(JmmNode node, String indent) {
        return "";
    }

    private String visitProgram(JmmNode node, String indent) {
        StringBuilder code = new StringBuilder();

        // Visit all children
        for (JmmNode child : node.getChildren()) {
            code.append(visit(child, indent));
        }

        return code.toString();
    }

    private String visitClass(JmmNode node, String indent) {
        StringBuilder code = new StringBuilder();

        // Class declaration
        String className = symbolTable.getClassName();
        String superClass = symbolTable.getSuper();

        code.append(indent).append(".class public ").append(className).append("\n");

        if (superClass != null && !superClass.isEmpty() && !superClass.equals("java.lang.Object")) {
            code.append(indent).append(".extends ").append(superClass).append("\n");
        }

        code.append(indent).append("{\n");

        // Fields
        for (Symbol field : symbolTable.getFields()) {
            code.append(indent).append("  .field private ").append(field.getName())
                    .append(optUtils.toOllirType(field.getType())).append(";\n");
        }

        // Constructor
        code.append(indent).append("  .construct ").append(className).append("().V {\n");
        code.append(indent).append("    invokespecial(this, \"<init>\").V;\n");
        code.append(indent).append("  }\n\n");

        // Methods (including the "main" method)
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals(METHOD_DECL.getNodeName())) {
                code.append(visit(child, indent + "  "));
            }
        }

        code.append(indent).append("}\n");

        return code.toString();
    }

    private String visitVarDecl(JmmNode node, String indent) {
        // Variable declarations don't generate OLLIR code directly
        return "";
    }

    private String visitFieldDecl(JmmNode node, String indent) {
        // Fields are handled in visitClass
        return "";
    }

    private String visitReturn(JmmNode node, String indent) {
        StringBuilder code = new StringBuilder();

        if (node.getNumChildren() > 0) {
            JmmNode expr = node.getChild(0);
            OllirExprResult exprResult = exprGenerator.visit(expr);

            code.append(exprResult.getComputation());

            Type returnType = typeUtils.getExprType(expr);
            code.append(indent).append("ret").append(optUtils.toOllirType(returnType))
                    .append(" ").append(exprResult.getCode()).append(";\n");
        } else {
            code.append(indent).append("ret.V;\n");
        }

        return code.toString();
    }

    private String visitAssign(JmmNode node, String indent) {
        StringBuilder code = new StringBuilder();

        JmmNode lhs = node.getChild(0);
        JmmNode rhs = node.getChild(1);

        // Generate code for right-hand side
        OllirExprResult rhsResult = exprGenerator.visit(rhs);
        code.append(rhsResult.getComputation());

        // Handle different types of assignments
        if (lhs.getKind().equals(ARRAY_ACCESS_EXPR.getNodeName())) {
            // Array assignment
            JmmNode arrayNode = lhs.getChild(0);
            JmmNode indexNode = lhs.getChild(1);

            OllirExprResult arrayResult = exprGenerator.visit(arrayNode);
            OllirExprResult indexResult = exprGenerator.visit(indexNode);

            code.append(arrayResult.getComputation());
            code.append(indexResult.getComputation());

            // Get element type (arrays in JMM are int[] only)
            String elemType = ".i32";

            code.append(indent).append(arrayResult.getCode())
                    .append("[").append(indexResult.getCode()).append("]")
                    .append(elemType).append(" :=").append(elemType).append(" ")
                    .append(rhsResult.getCode()).append(";\n");
        }
        else if (lhs.getKind().equals(VAR_REF_EXPR.getNodeName())) {
            // Simple variable assignment
            String varName = lhs.get("name");
            Type varType = typeUtils.getExprType(lhs);
            String ollirType = optUtils.toOllirType(varType);

            // Check if it's a field
            boolean isField = false;
            for (Symbol field : symbolTable.getFields()) {
                if (field.getName().equals(varName)) {
                    isField = true;
                    break;
                }
            }

            if (isField) {
                // Field assignment
                code.append(indent).append("putfield(this, \"").append(varName).append("\", ")
                        .append(rhsResult.getCode()).append(").V;\n");
            } else {
                // Local variable assignment
                code.append(indent).append(varName).append(ollirType)
                        .append(" :=").append(ollirType).append(" ")
                        .append(rhsResult.getCode()).append(";\n");
            }
        }

        return code.toString();
    }

    private String visitArrayAssign(JmmNode node, String indent) {
        StringBuilder code = new StringBuilder();

        JmmNode arrayNode = node.getChild(0);
        JmmNode indexNode = node.getChild(1);
        JmmNode valueNode = node.getChild(2);

        // Generate code for array, index and value
        OllirExprResult arrayResult = exprGenerator.visit(arrayNode);
        OllirExprResult indexResult = exprGenerator.visit(indexNode);
        OllirExprResult valueResult = exprGenerator.visit(valueNode);

        code.append(arrayResult.getComputation());
        code.append(indexResult.getComputation());
        code.append(valueResult.getComputation());

        // Array element type (always int in JMM)
        String elemType = ".i32";

        code.append(indent).append(arrayResult.getCode())
                .append("[").append(indexResult.getCode()).append("]")
                .append(elemType).append(" :=").append(elemType).append(" ")
                .append(valueResult.getCode()).append(";\n");

        return code.toString();
    }

    private String visitExprStmt(JmmNode node, String indent) {
        if (node.getNumChildren() > 0) {
            OllirExprResult exprResult = exprGenerator.visit(node.getChild(0));
            return exprResult.getComputation();
        }
        return "";
    }

    private String visitIf(JmmNode node, String indent) {
        StringBuilder code = new StringBuilder();

        // Generate unique labels for if-then-else
        String thenLabel = "if_then_" + labelCounter;
        String elseLabel = "if_else_" + labelCounter;
        String endLabel = "if_end_" + labelCounter;
        labelCounter++;

        // Condition
        OllirExprResult condResult = exprGenerator.visit(node.getChild(0));
        code.append(condResult.getComputation());

        // Branch instruction
        code.append(indent).append("if (").append(condResult.getCode())
                .append(") goto ").append(thenLabel).append(";\n");
        code.append(indent).append("goto ").append(elseLabel).append(";\n");

        // Then block
        code.append(indent).append(thenLabel).append(":\n");
        code.append(visit(node.getChild(1), indent + "  "));
        code.append(indent).append("goto ").append(endLabel).append(";\n");

        // Else block
        code.append(indent).append(elseLabel).append(":\n");
        if (node.getNumChildren() > 2) {
            code.append(visit(node.getChild(2), indent + "  "));
        }

        // End label
        code.append(indent).append(endLabel).append(":\n");

        return code.toString();
    }

    private String visitWhile(JmmNode node, String indent) {
        StringBuilder code = new StringBuilder();

        // Generate unique labels for while
        String condLabel = "while_cond_" + labelCounter;
        String bodyLabel = "while_body_" + labelCounter;
        String endLabel = "while_end_" + labelCounter;
        labelCounter++;

        // Condition label
        code.append(indent).append(condLabel).append(":\n");

        // Condition
        OllirExprResult condResult = exprGenerator.visit(node.getChild(0));
        code.append(condResult.getComputation());

        // Branch instruction
        code.append(indent).append("if (").append(condResult.getCode())
                .append(") goto ").append(bodyLabel).append(";\n");
        code.append(indent).append("goto ").append(endLabel).append(";\n");

        // Body
        code.append(indent).append(bodyLabel).append(":\n");
        code.append(visit(node.getChild(1), indent + "  "));
        code.append(indent).append("goto ").append(condLabel).append(";\n");

        // End label
        code.append(indent).append(endLabel).append(":\n");

        return code.toString();
    }

    private String visitBlock(JmmNode node, String indent) {
        StringBuilder code = new StringBuilder();

        for (JmmNode child : node.getChildren()) {
            code.append(visit(child, indent));
        }

        return code.toString();
    }

    private String visitMethodDecl(JmmNode node, String indent) {
        StringBuilder code = new StringBuilder();

        // Set current method
        currentMethod = node.get("name");

        // Method header
        Type returnType = symbolTable.getReturnType(currentMethod);
        String ollirReturnType = optUtils.toOllirType(returnType);

        code.append(indent).append(".method public ").append(currentMethod).append("(");

        // Parameters
        List<Symbol> parameters = symbolTable.getParameters(currentMethod);
        if (!parameters.isEmpty()) {
            List<String> paramList = parameters.stream()
                    .map(param -> param.getName() + optUtils.toOllirType(param.getType()))
                    .collect(Collectors.toList());
            code.append(String.join(", ", paramList));
        }

        code.append(")").append(ollirReturnType).append(" {\n");

        // Method body - visit all statements
        for (JmmNode child : node.getChildren()) {
            if (Kind.check(child, BLOCK_STMT) ||
                    Kind.check(child, STATEMENTS.toArray(new Kind[0]))) {
                code.append(visit(child, indent + "  "));
            }
        }

        // Method end
        code.append(indent).append("}\n\n");

        return code.toString();
    }

    private String visitImport(JmmNode node, String indent) {
        // Imports don't generate OLLIR code
        return "";
    }
}