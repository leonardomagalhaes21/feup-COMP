package pt.up.fe.comp2025.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2025.ast.TypeUtils;

/**
 * Utilitários para geração de código OLLIR
 */
public class OptUtils {
    private static int tempCounter = 0;
    private static int labelCounter = 0;
    private final TypeUtils typeUtils;
    
    public OptUtils(TypeUtils typeUtils) {
        this.typeUtils = typeUtils;
    }
    
    /**
     * Gera um nome temporário único
     */
    public static String getTemp() {
        return "t" + (tempCounter++);
    }

    /**
     * Gera um nome temporário único com um prefixo específico
     */
    public static String getTemp(String prefix) {
        return prefix + (tempCounter++);
    }
    
    /**
     * Gera um rótulo único
     */
    public static String getLabel(String prefix) {
        return prefix + (labelCounter++);
    }

    // Add to OptUtils class if missing
    public String toOllirType(Type type) {
        if (type == null) {
            return ".V"; // Void for null type
        }

        StringBuilder ollirType = new StringBuilder();

        // Handle array types
        if (type.isArray()) {
            ollirType.append(".array");
        }

        // Map type names to OLLIR types
        switch (type.getName()) {
            case "int":
                ollirType.append(".i32");
                break;
            case "boolean":
                ollirType.append(".bool");
                break;
            case "void":
                ollirType.append(".V");
                break;
            case "String":
                ollirType.append(".String");
                break;
            default:
                // For class types
                ollirType.append(".").append(type.getName());
                break;
        }

        return ollirType.toString();
    }
    
    /**
     * Converte um nó de tipo JMM para seu equivalente OLLIR
     */
    public String toOllirType(JmmNode typeNode) {
        if (typeNode.hasAttribute("name")) {
            String typeName = typeNode.get("name");
            boolean isArray = typeNode.hasAttribute("isArray") && typeNode.get("isArray").equals("true");
            
            StringBuilder ollirType = new StringBuilder();
            
            if (isArray) {
                ollirType.append(".array");
            }
            
            ollirType.append(getBaseOllirType(typeName));
            
            return ollirType.toString();
        }
        
        return ".V"; // Default para void se não houver tipo
    }
    
    /**
     * Obtém o tipo OLLIR base para um tipo Java
     */
    private String getBaseOllirType(String typeName) {
        return switch (typeName) {
            case "int" -> ".i32";
            case "boolean" -> ".bool";
            case "String" -> ".String";
            case "void" -> ".V";
            default -> "." + typeName; // Tipos de classe personalizada
        };
    }
    
    /**
     * Obtém o tipo OLLIR para um símbolo da tabela de símbolos
     */
    public String getOllirType(Symbol symbol) {
        return toOllirType(symbol.getType());
    }
    
    /**
     * Verifica se uma string não está vazia ou só com espaços
     */
    public static boolean notEmptyWS(String str) {
        return str != null && !str.trim().isEmpty();
    }
    
    /**
     * Retorna o tipo OLLIR de um parâmetro
     */
    public String getParameterOllirType(Type type) {
        return toOllirType(type);
    }
    
    /**
     * Converte um literal booleano para seu equivalente OLLIR (0.bool ou 1.bool)
     */
    public String booleanLiteralToOllir(String boolValue) {
        return Boolean.parseBoolean(boolValue) ? "1.bool" : "0.bool";
    }
    
    /**
     * Converte um literal inteiro para seu equivalente OLLIR
     */
    public String intLiteralToOllir(String intValue) {
        return intValue + ".i32";
    }
    
    /**
     * Obtém o nome do tipo para os operandos
     */
    public String getOperandType(JmmNode node) {
        Type nodeType = typeUtils.getExprType(node);
        return toOllirType(nodeType);
    }
}
