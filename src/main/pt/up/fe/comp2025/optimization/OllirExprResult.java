package pt.up.fe.comp2025.optimization;

/**
 * Classe que representa o resultado de uma expressão convertida para OLLIR.
 * Contém o código da expressão e seu código de computação (instruções necessárias).
 */
public class OllirExprResult {
    public static final OllirExprResult EMPTY = new OllirExprResult("");
    
    private final String code;
    private final String computation;
    
    /**
     * Cria um resultado com código sem computação.
     */
    public OllirExprResult(String code) {
        this.code = code;
        this.computation = "";
    }
    
    /**
     * Cria um resultado com código e computação.
     */
    public OllirExprResult(String code, String computation) {
        this.code = code;
        this.computation = computation;
    }
    
    /**
     * Cria um resultado com uma StringBuilder de computação.
     */
    public OllirExprResult(String code, StringBuilder computation) {
        this.code = code;
        this.computation = computation.toString();
    }
    
    /**
     * Obtém o código da expressão.
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Obtém o código de computação.
     */
    public String getComputation() {
        return computation;
    }
    
    /**
     * Verifica se o resultado está vazio.
     */
    public boolean isEmpty() {
        return code.isEmpty() && computation.isEmpty();
    }
    
    /**
     * Cria um novo resultado acrescentando computação.
     */
    public OllirExprResult withAppendedComputation(String additionalComputation) {
        return new OllirExprResult(code, computation + additionalComputation);
    }
    
    /**
     * Cria um novo resultado com código atualizado.
     */
    public OllirExprResult withCode(String newCode) {
        return new OllirExprResult(newCode, computation);
    }
    
    @Override
    public String toString() {
        return "OllirExprResult{" +
                "code='" + code + '\'' +
                ", computation='" + computation + '\'' +
                '}';
    }
}
