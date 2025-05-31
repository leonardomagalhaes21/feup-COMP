package pt.up.fe.comp2025.backend.builders;

import org.specs.comp.ollir.OperationType;
import org.specs.comp.ollir.tree.TreeNode;
import org.specs.comp.ollir.inst.UnaryOpInstruction;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

public class UnaryOpInstructionBuilder {
    private static final String NL = "\n";

    private final UnaryOpInstruction unaryOp;
    private final FunctionClassMap<TreeNode, String> generators;
    private final StringBuilder codeBuilder;
    private final LabelGenerator labelGenerator;

    public UnaryOpInstructionBuilder(UnaryOpInstruction unaryOp,
            FunctionClassMap<TreeNode, String> generators,
            LabelGenerator labelGenerator) {
        this.unaryOp = unaryOp;
        this.generators = generators;
        this.codeBuilder = new StringBuilder();
        this.labelGenerator = labelGenerator;
    }

    public UnaryOpInstructionBuilder withOperandLoading() {
        // Load the single operand onto the stack
        codeBuilder.append(generators.apply(unaryOp.getOperands().get(0)));
        return this;
    }

    public UnaryOpInstructionBuilder withOperationGeneration() {
        OperationType opType = unaryOp.getOperation().getOpType();

        switch (opType) {
            case NOTB -> generateBooleanNot();
            default -> throw new NotImplementedException(opType);
        }

        return this;
    }

    private void generateBooleanNot() {
        // Generate boolean NOT operation using conditional jumps
        String ifLabel = labelGenerator.getNextLabel();
        String endLabel = labelGenerator.getNextLabel();

        // If operand is 0 (false), jump to ifLabel to set result to 1 (true)
        codeBuilder.append("ifeq ").append(ifLabel).append(NL);

        // Operand was non-zero (true), so result should be 0 (false)
        codeBuilder.append("iconst_0").append(NL);
        codeBuilder.append("goto ").append(endLabel).append(NL);

        // Operand was zero (false), so result should be 1 (true)
        codeBuilder.append(ifLabel).append(":").append(NL);
        codeBuilder.append("iconst_1").append(NL);

        // End of operation
        codeBuilder.append(endLabel).append(":").append(NL);
    }

    public String buildUnaryOpInstruction() {
        return codeBuilder.toString();
    }

    // Interface for label generation to maintain compatibility
    public interface LabelGenerator {
        String getNextLabel();
    }
}
