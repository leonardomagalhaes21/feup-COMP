package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.ast.TypeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Visitor para gerar código OLLIR a partir de expressões JMM.
 */
public class OllirExprGeneratorVisitor extends AJmmVisitor<Void, OllirExprResult> {
    private static final String END_STMT = ";\n";
    
    private final SymbolTable symbolTable;
    private final TypeUtils typeUtils;
    private final OptUtils optUtils;
    
    public OllirExprGeneratorVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
        this.typeUtils = new TypeUtils(symbolTable);
        this.optUtils = new OptUtils(typeUtils);
    }
    
    @Override
    protected void buildVisitor() {
        addVisit("BinaryOp", this::visitBinaryOp);
        addVisit("UnaryOp", this::visitUnaryOp);
        addVisit("ParenthesisOp", this::visitParenthesisOp);
        addVisit("ArraySubscript", this::visitArraySubscript);
        addVisit("ArrayLength", this::visitArrayLength);
        addVisit("MethodCall", this::visitMethodCall);
        addVisit("NewObject", this::visitNewObject);
        addVisit("NewIntArray", this::visitNewIntArray);
        addVisit("Identifier", this::visitIdentifier);
        addVisit("IntLiteral", this::visitIntLiteral);
        addVisit("BooleanLiteral", this::visitBooleanLiteral);
        addVisit("This", this::visitThis);
        
        setDefaultVisit(this::defaultVisit);
    }
    
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {
        return OllirExprResult.EMPTY;
    }
    
    private OllirExprResult visitBinaryOp(JmmNode node, Void unused) {
        // Tratamento especial para o operador '&&'
        if (node.get("op").equals("&&")) {
            return visitShortCircuitAnd(node);
        }
        
        StringBuilder computation = new StringBuilder();
        
        // Visita os operandos
        OllirExprResult left = visit(node.getChild(0));
        OllirExprResult right = visit(node.getChild(1));
        
        // Determina o tipo do resultado
        Type resultType = typeUtils.getExprType(node);
        String ollirType = optUtils.toOllirType(resultType);
        
        // Adiciona código de computação dos operandos
        computation.append(left.getComputation());
        computation.append(right.getComputation());
        
        // Gera temporário para o resultado
        String tempVar = OptUtils.getTemp();
        
        // Mapeia o operador Java para o operador OLLIR
        String ollirOp = mapOperator(node.get("op"));
        
        // Gera a atribuição
        computation.append(tempVar).append(ollirType)
                .append(" :=").append(ollirType).append(" ")
                .append(left.getCode()).append(" ")
                .append(ollirOp).append(ollirType).append(" ")
                .append(right.getCode())
                .append(END_STMT);
        
        return new OllirExprResult(tempVar + ollirType, computation);
    }
    
    private String mapOperator(String javaOperator) {
        return switch (javaOperator) {
            case "+" -> "+";
            case "-" -> "-";
            case "*" -> "*";
            case "/" -> "/";
            case "<" -> "<";
            case ">" -> ">";
            case "<=" -> "<=";
            case ">=" -> ">=";
            case "==" -> "==";
            case "!=" -> "!=";
            case "&&" -> "&&";
            case "||" -> "||";
            default -> throw new IllegalArgumentException("Operador não suportado: " + javaOperator);
        };
    }
    
    private OllirExprResult visitShortCircuitAnd(JmmNode node) {
        StringBuilder computation = new StringBuilder();
        
        // Visita o operando esquerdo
        OllirExprResult left = visit(node.getChild(0));
        computation.append(left.getComputation());
        
        // Cria rótulos para o circuito lógico
        String falseLabel = OptUtils.getLabel("AND_FALSE");
        String endLabel = OptUtils.getLabel("AND_END");
        
        // Cria temporário para o resultado
        String resultTemp = OptUtils.getTemp();
        String resultType = ".bool";
        
        // Se o operando esquerdo for falso, pula para o rótulo falso
        computation.append("if (!.bool ")
                .append(left.getCode())
                .append(") goto ")
                .append(falseLabel)
                .append(END_STMT);
        
        // Avalia o operando direito apenas se o esquerdo for verdadeiro
        OllirExprResult right = visit(node.getChild(1));
        computation.append(right.getComputation());
        
        // Atribui o valor do operando direito ao resultado
        computation.append(resultTemp).append(resultType)
                .append(" :=").append(resultType).append(" ")
                .append(right.getCode())
                .append(END_STMT);
        
        // Pula para o fim
        computation.append("goto ").append(endLabel).append(END_STMT);
        
        // Rótulo falso: atribui falso ao resultado
        computation.append(falseLabel).append(":\n");
        computation.append(resultTemp).append(resultType)
                .append(" :=").append(resultType).append(" 0")
                .append(resultType)
                .append(END_STMT);
        
        // Rótulo fim
        computation.append(endLabel).append(":\n");
        
        return new OllirExprResult(resultTemp + resultType, computation);
    }
    
    private OllirExprResult visitUnaryOp(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        
        // Visita o operando
        OllirExprResult operand = visit(node.getChild(0));
        computation.append(operand.getComputation());
        
        // Determina o tipo do resultado
        Type resultType = typeUtils.getExprType(node);
        String ollirType = optUtils.toOllirType(resultType);
        
        // Gera temporário para o resultado
        String tempVar = OptUtils.getTemp();
        
        // Gera a operação unária
        computation.append(tempVar).append(ollirType)
                .append(" :=").append(ollirType).append(" ")
                .append(node.get("op")).append(ollirType).append(" ")
                .append(operand.getCode())
                .append(END_STMT);
        
        return new OllirExprResult(tempVar + ollirType, computation);
    }
    
    private OllirExprResult visitParenthesisOp(JmmNode node, Void unused) {
        // Parênteses são transparentes em OLLIR, apenas visite a expressão interna
        return visit(node.getChild(0));
    }
    
    private OllirExprResult visitArraySubscript(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        
        // Visita o array e o índice
        OllirExprResult array = visit(node.getChild(0));
        OllirExprResult index = visit(node.getChild(1));
        
        computation.append(array.getComputation());
        computation.append(index.getComputation());
        
        // Determina o tipo do elemento do array
        String elementType = ".i32"; // Arrays em JMM são sempre de inteiros
        
        // Gera temporário para o resultado
        String tempVar = OptUtils.getTemp();
        
        // Gera o acesso ao array
        computation.append(tempVar).append(elementType)
                .append(" :=").append(elementType).append(" ")
                .append(array.getCode())
                .append("[").append(index.getCode()).append("]")
                .append(elementType)
                .append(END_STMT);
        
        return new OllirExprResult(tempVar + elementType, computation);
    }
    
    private OllirExprResult visitArrayLength(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        
        // Visita o array
        OllirExprResult array = visit(node.getChild(0));
        computation.append(array.getComputation());
        
        // O resultado é sempre um inteiro
        String resultType = ".i32";
        
        // Gera temporário para o resultado
        String tempVar = OptUtils.getTemp();
        
        // Gera a chamada de comprimento do array
        computation.append(tempVar).append(resultType)
                .append(" :=").append(resultType).append(" ")
                .append("arraylength(").append(array.getCode()).append(")")
                .append(resultType)
                .append(END_STMT);
        
        return new OllirExprResult(tempVar + resultType, computation);
    }
    
    private OllirExprResult visitMethodCall(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        
        // Visita o objeto que recebe a chamada
        OllirExprResult object = visit(node.getChild(0));
        computation.append(object.getComputation());
        
        // Obtém o nome do método
        String methodName = node.get("method");
        
        // Processa os argumentos
        List<OllirExprResult> args = new ArrayList<>();
        for (int i = 1; i < node.getNumChildren(); i++) {
            OllirExprResult arg = visit(node.getChild(i));
            computation.append(arg.getComputation());
            args.add(arg);
        }
        
        // Determina o tipo de retorno
        String returnType = determineMethodReturnType(node);
        
        // Gera temporário para o resultado se o método não retornar void
        String tempVar = OptUtils.getTemp();
        String resultCode = tempVar + returnType;
        
        // Determina o tipo de invocação (virtual, static, etc)
        String invocationType = determineInvocationType(node);
        
        // Constrói a invocação do método
        if (!returnType.equals(".V")) {
            computation.append(tempVar).append(returnType)
                    .append(" :=").append(returnType).append(" ");
        }
        
        computation.append(invocationType).append("(")
                .append(object.getCode()).append(", \"")
                .append(methodName).append("\"");
        
        // Adiciona os argumentos
        for (OllirExprResult arg : args) {
            computation.append(", ").append(arg.getCode());
        }
        
        computation.append(")").append(returnType).append(END_STMT);
        
        return new OllirExprResult(resultCode, computation);
    }
    
    private String determineMethodReturnType(JmmNode node) {
        // Tenta encontrar o tipo de retorno do método na tabela de símbolos
        String methodName = node.get("method");
        
        if (symbolTable.getMethods().contains(methodName)) {
            Type returnType = symbolTable.getReturnType(methodName);
            return optUtils.toOllirType(returnType);
        }
        
        // Se não for um método da classe atual, infere a partir do contexto
        JmmNode parent = node.getParent();
        if (parent != null) {
            if (parent.getKind().equals("BinaryOp")) {
                // Em operadores binários, o tipo pode ser inferido pelo operador
                String op = parent.get("op");
                if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/")) {
                    return ".i32";
                } else {
                    return ".bool";
                }
            } else if (parent.getKind().equals("AssignStatement")) {
                // Em atribuições, o tipo é o tipo da variável à esquerda
                JmmNode target = parent.getChild(0);
                Type targetType = typeUtils.getExprType(target);
                return optUtils.toOllirType(targetType);
            }
        }
        
        // Se não conseguir determinar, assume void
        return ".V";
    }
    
    private String determineInvocationType(JmmNode node) {
        JmmNode object = node.getChild(0);
        
        // Caso 1: Chamada em objeto 'this'
        if (object.getKind().equals("This")) {
            return "invokevirtual";
        }
        
        // Caso 2: Chamada estática (em uma classe importada)
        String objectName = "";
        if (object.getKind().equals("Identifier")) {
            objectName = object.get("name");
        }
        
        if (symbolTable.getImports().contains(objectName)) {
            return "invokestatic";
        }
        
        // Caso 3: Chamada virtual para outros objetos
        return "invokevirtual";
    }
    
    private OllirExprResult visitNewObject(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        
        // Obtém o nome da classe
        String className = node.get("className");
        String objectType = "." + className;
        
        // Gera temporário para o novo objeto
        String tempVar = OptUtils.getTemp();
        
        // Gera a criação do objeto
        computation.append(tempVar).append(objectType)
                .append(" :=").append(objectType).append(" ")
                .append("new(").append(className).append(")")
                .append(objectType)
                .append(END_STMT);
        
        // Chama o construtor
        computation.append("invokespecial(")
                .append(tempVar).append(objectType)
                .append(", \"<init>\").V")
                .append(END_STMT);
        
        return new OllirExprResult(tempVar + objectType, computation);
    }
    
    private OllirExprResult visitNewIntArray(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        
        // Visita a expressão de tamanho
        OllirExprResult size = visit(node.getChild(0));
        computation.append(size.getComputation());
        
        // Define o tipo do array
        String arrayType = ".array.i32";
        
        // Gera temporário para o novo array
        String tempVar = OptUtils.getTemp();
        
        // Gera a criação do array
        computation.append(tempVar).append(arrayType)
                .append(" :=").append(arrayType).append(" ")
                .append("new(array, ").append(size.getCode()).append(")")
                .append(arrayType)
                .append(END_STMT);
        
        return new OllirExprResult(tempVar + arrayType, computation);
    }
    
    private OllirExprResult visitIdentifier(JmmNode node, Void unused) {
        String varName = node.get("name");
        
        // Verifica se é um campo da classe
        for (Symbol field : symbolTable.getFields()) {
            if (field.getName().equals(varName)) {
                return handleFieldAccess(field);
            }
        }
        
        // Verifica se é um parâmetro ou variável local
        Type varType = typeUtils.getExprType(node);
        String ollirType = optUtils.toOllirType(varType);
        
        return new OllirExprResult(varName + ollirType);
    }
    
    private OllirExprResult handleFieldAccess(Symbol field) {
        StringBuilder computation = new StringBuilder();
        
        String fieldType = optUtils.getOllirType(field);
        String tempVar = OptUtils.getTemp();
        
        // Gera acesso ao campo
        computation.append(tempVar).append(fieldType)
                .append(" :=").append(fieldType).append(" ")
                .append("getfield(this, \"").append(field.getName()).append("\")")
                .append(fieldType)
                .append(END_STMT);
        
        return new OllirExprResult(tempVar + fieldType, computation);
    }
    
    private OllirExprResult visitIntLiteral(JmmNode node, Void unused) {
        String value = node.get("value");
        return new OllirExprResult(value + ".i32");
    }
    
    private OllirExprResult visitBooleanLiteral(JmmNode node, Void unused) {
        boolean value = Boolean.parseBoolean(node.get("value"));
        return new OllirExprResult((value ? "1" : "0") + ".bool");
    }
    
    private OllirExprResult visitThis(JmmNode node, Void unused) {
        String className = symbolTable.getClassName();
        return new OllirExprResult("this." + className);
    }
}
