package pt.up.fe.comp2025.backend.builders;

import org.specs.comp.ollir.OperationType;
import org.specs.comp.ollir.inst.BinaryOpInstruction;
import org.specs.comp.ollir.inst.CondBranchInstruction;
import org.specs.comp.ollir.inst.Instruction;
import org.specs.comp.ollir.tree.TreeNode;

import pt.up.fe.specs.util.classmap.FunctionClassMap;

public class CondBranchInstructionBuilder {
    private static final String NL = "\n";

    private final CondBranchInstruction condBranch;
    private final FunctionClassMap<TreeNode, String> generators;
    private final StringBuilder codeBuilder;
    private final String targetLabel;

    public CondBranchInstructionBuilder(CondBranchInstruction condBranch,
            FunctionClassMap<TreeNode, String> generators) {
        this.condBranch = condBranch;
        this.generators = generators;
        this.codeBuilder = new StringBuilder();
        this.targetLabel = condBranch.getLabel();
    }

    public CondBranchInstructionBuilder withConditionAnalysis() {
        Instruction condition = condBranch.getCondition();

        if (condition instanceof BinaryOpInstruction) {
            return withBinaryCondition((BinaryOpInstruction) condition);
        } else {
            return withGenericCondition(condition);
        }
    }

    private CondBranchInstructionBuilder withBinaryCondition(BinaryOpInstruction binOp) {
        OperationType opType = binOp.getOperation().getOpType();

        // Load both operands for comparison
        codeBuilder.append(generators.apply(binOp.getLeftOperand()));
        codeBuilder.append(generators.apply(binOp.getRightOperand()));

        // Generate appropriate comparison instruction
        String comparisonInstruction = getComparisonInstruction(opType);
        codeBuilder.append(comparisonInstruction).append(targetLabel).append(NL);

        return this;
    }

    private CondBranchInstructionBuilder withGenericCondition(Instruction condition) {
        // For non-binary conditions, evaluate condition and check if non-zero
        codeBuilder.append(generators.apply(condition));
        codeBuilder.append("ifne ").append(targetLabel).append(NL);

        return this;
    }

    private String getComparisonInstruction(OperationType opType) {
        return switch (opType) {
            case LTH -> "if_icmplt ";
            case LTE -> "if_icmple ";
            case GTH -> "if_icmpgt ";
            case GTE -> "if_icmpge ";
            case EQ -> "if_icmpeq ";
            case NEQ -> "if_icmpne ";
            default -> {
                // For non-comparison operations, fall back to generic condition handling
                codeBuilder.setLength(0); // Clear current buffer
                yield "ifne ";
            }
        };
    }

    public String buildCondBranchInstruction() {
        return codeBuilder.toString();
    }
}
