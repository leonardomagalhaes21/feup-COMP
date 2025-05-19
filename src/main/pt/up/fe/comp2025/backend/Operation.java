package pt.up.fe.comp2025.backend;

public class Operation {
    private final OperationType opType;
    private final String operationType;

    public Operation(OperationType opType, String operationType) {
        this.opType = opType;
        this.operationType = operationType;
    }

    public OperationType getOpType() {
        return opType;
    }

    public String getOperationType() {
        return operationType;
    }
}