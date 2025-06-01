package pt.up.fe.comp2025.backend.builders;

import org.specs.comp.ollir.inst.BinaryOpInstruction;
import org.specs.comp.ollir.Element;
import org.specs.comp.ollir.LiteralElement;
import org.specs.comp.ollir.OperationType;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import org.specs.comp.ollir.tree.TreeNode;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Builder for generating Jasmin code for binary operations.
 */
public class BinaryOpInstructionBuilder {
    private static final String NL = "\n";
    private static final Set<OperationType> COMPARATORS = Set.of(
            OperationType.LTE, OperationType.LTH, OperationType.GTH,
            OperationType.GTE, OperationType.EQ, OperationType.NEQ);

    private final BinaryOpInstruction instruction;
    private final FunctionClassMap<TreeNode, String> generators;
    private final Supplier<String> labelGenerator;
    private final StringBuilder codeBuffer;
    private final OperationType operationType;

    public BinaryOpInstructionBuilder(BinaryOpInstruction instruction,
            FunctionClassMap<TreeNode, String> generators,
            Supplier<String> labelGenerator) {
        this.instruction = instruction;
        this.generators = generators;
        this.labelGenerator = labelGenerator;
        this.codeBuffer = new StringBuilder();
        this.operationType = instruction.getOperation().getOpType();
    }

    public BinaryOpInstructionBuilder withOperandAnalysis() {
        if (COMPARATORS.contains(operationType)) {
            if (hasZeroOperand()) {
                generateOptimizedComparison();
            } else {
                generateStandardComparison();
            }
        } else {
            generateArithmeticOperation();
        }
        return this;
    }

    public String buildBinaryOperation() {
        return codeBuffer.toString();
    }

    private boolean hasZeroOperand() {
        return isZeroLiteral(instruction.getLeftOperand()) || isZeroLiteral(instruction.getRightOperand());
    }

    private boolean isZeroLiteral(Element operand) {
        return operand instanceof LiteralElement &&
                ((LiteralElement) operand).getLiteral().equals("0");
    }

    private void generateOptimizedComparison() {
        boolean leftIsZero = isZeroLiteral(instruction.getLeftOperand());

        if (leftIsZero) {
            generateZeroLeftComparison();
        } else {
            generateZeroRightComparison();
        }
    }

    private void generateZeroLeftComparison() {
        // Load only the right operand
        codeBuffer.append(generators.apply(instruction.getRightOperand()));

        // 0 < a, 0 <= a, 0 > a, 0 >= a, 0 == a, 0 != a
        String ifInstruction = getZeroLeftComparisonInstruction();
        generateComparisonBranching(ifInstruction);
    }

    private String getZeroLeftComparisonInstruction() {
        return switch (operationType) {
            case LTH -> "ifgt "; // 0 < a  <=> a > 0
            case LTE -> "ifge "; // 0 <= a <=> a >= 0
            case GTH -> "iflt "; // 0 > a  <=> a < 0
            case GTE -> "ifle "; // 0 >= a <=> a <= 0
            case EQ -> "ifeq ";
            case NEQ -> "ifne ";
            default -> throw new NotImplementedException(operationType);
        };
    }

    private void generateZeroRightComparison() {
        // Load only the left operand
        codeBuffer.append(generators.apply(instruction.getLeftOperand()));

        // For a < 0, a <= 0, a > 0, a >= 0, a == 0, a != 0
        String ifInstruction = getZeroRightComparisonInstruction();
        generateComparisonBranching(ifInstruction);
    }

    private String getZeroRightComparisonInstruction() {
        return switch (operationType) {
            case LTH -> "iflt ";
            case LTE -> "ifle ";
            case GTH -> "ifgt ";
            case GTE -> "ifge ";
            case EQ -> "ifeq ";
            case NEQ -> "ifne ";
            default -> throw new NotImplementedException(operationType);
        };
    }

    private void generateStandardComparison() {
        // Load both operands
        codeBuffer.append(generators.apply(instruction.getLeftOperand()));
        codeBuffer.append(generators.apply(instruction.getRightOperand()));

        String ifInstruction = getTwoOperandComparisonInstruction();
        generateComparisonBranching(ifInstruction);
    }

    private void generateArithmeticOperation() {
        codeBuffer.append(generators.apply(instruction.getLeftOperand()));
        codeBuffer.append(generators.apply(instruction.getRightOperand()));

        String arithmeticOp = getArithmeticInstruction();
        codeBuffer.append(arithmeticOp).append(NL);
    }

    private void generateComparisonBranching(String ifInstruction) {
        String trueLabel = labelGenerator.get();
        String endLabel = labelGenerator.get();

        codeBuffer.append(ifInstruction).append(trueLabel).append(NL);
        codeBuffer.append("iconst_0").append(NL)
                .append("goto ").append(endLabel).append(NL);
        codeBuffer.append(trueLabel).append(":").append(NL)
                .append("iconst_1").append(NL);
        codeBuffer.append(endLabel).append(":").append(NL);
    }

    private String getReversedComparisonInstruction() {
        return switch (operationType) {
            case LTH -> "ifgt ";
            case LTE -> "ifge ";
            case GTH -> "iflt ";
            case GTE -> "ifle ";
            case EQ -> "ifeq ";
            case NEQ -> "ifne ";
            default -> throw new NotImplementedException(operationType);
        };
    }

    private String getStandardComparisonInstruction() {
        return switch (operationType) {
            case LTH -> "iflt ";
            case LTE -> "ifle ";
            case GTH -> "ifgt ";
            case GTE -> "ifge ";
            case EQ -> "ifeq ";
            case NEQ -> "ifne ";
            default -> throw new NotImplementedException(operationType);
        };
    }

    private String getTwoOperandComparisonInstruction() {
        return switch (operationType) {
            case LTH -> "if_icmplt ";
            case LTE -> "if_icmple ";
            case GTH -> "if_icmpgt ";
            case GTE -> "if_icmpge ";
            case EQ -> "if_icmpeq ";
            case NEQ -> "if_icmpne ";
            default -> throw new NotImplementedException(operationType);
        };
    }

    private String getArithmeticInstruction() {
        return switch (operationType) {
            case ADD -> "iadd";
            case MUL -> "imul";
            case SUB -> "isub";
            case DIV -> "idiv";
            case XOR -> "ixor";
            case AND, ANDB, NOTB -> "iand";
            case OR, ORB -> "ior";
            default -> throw new NotImplementedException(operationType);
        };
    }
}
