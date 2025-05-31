package pt.up.fe.comp2025.backend.builders;

import org.specs.comp.ollir.inst.AssignInstruction;
import org.specs.comp.ollir.ArrayOperand;
import org.specs.comp.ollir.inst.BinaryOpInstruction;
import org.specs.comp.ollir.Element;
import org.specs.comp.ollir.inst.Instruction;
import org.specs.comp.ollir.LiteralElement;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.Operand;
import org.specs.comp.ollir.OperationType;
import org.specs.comp.ollir.type.ArrayType;
import org.specs.comp.ollir.type.Type;
import pt.up.fe.comp2025.backend.JasminUtils;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import org.specs.comp.ollir.tree.TreeNode;

/**
 * Builder for generating Jasmin code for assignment instructions.
 */
public class AssignInstructionBuilder {
    private static final String NL = "\n";

    private final AssignInstruction instruction;
    private final Method currentMethod;
    private final JasminUtils types;
    private final FunctionClassMap<TreeNode, String> generators;
    private final StringBuilder codeBuffer;

    public AssignInstructionBuilder(AssignInstruction instruction, Method currentMethod,
            JasminUtils types, FunctionClassMap<TreeNode, String> generators) {
        this.instruction = instruction;
        this.currentMethod = currentMethod;
        this.types = types;
        this.generators = generators;
        this.codeBuffer = new StringBuilder();
    }

    public AssignInstructionBuilder withAssignmentGeneration() {
        Element lhs = instruction.getDest();
        Instruction rhsInst = instruction.getRhs();

        if (lhs instanceof ArrayOperand) {
            generateArrayAssignment((ArrayOperand) lhs, rhsInst);
        } else if (canOptimizeIncrement(lhs, rhsInst)) {
            generateIncrementOptimization(lhs, rhsInst);
        } else {
            generateStandardAssignment(lhs, rhsInst);
        }

        return this;
    }

    public String buildAssignInstruction() {
        return codeBuffer.toString();
    }

    private void generateArrayAssignment(ArrayOperand arrayOp, Instruction rhsInst) {
        loadArrayReference(arrayOp);
        loadArrayIndex(arrayOp);
        loadRightHandSide(rhsInst);
        generateArrayStore(arrayOp);
    }

    private void generateStandardAssignment(Element lhs, Instruction rhsInst) {
        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException("Not supported LHS type: " + lhs.getClass());
        }

        loadRightHandSide(rhsInst);
        storeToVariable((Operand) lhs);
    }

    private boolean canOptimizeIncrement(Element lhs, Instruction rhsInst) {
        return lhs instanceof Operand &&
                rhsInst instanceof BinaryOpInstruction &&
                ((BinaryOpInstruction) rhsInst).getOperation().getOpType() == OperationType.ADD;
    }

    private void generateIncrementOptimization(Element lhs, Instruction rhsInst) {
        BinaryOpInstruction binOp = (BinaryOpInstruction) rhsInst;
        Element left = binOp.getLeftOperand();
        Element right = binOp.getRightOperand();

        if (tryOptimizeIncrement((Operand) lhs, left, right) ||
                tryOptimizeIncrement((Operand) lhs, right, left)) {
            return; // Optimization successful
        }

        // Fall back to standard assignment
        generateStandardAssignment(lhs, rhsInst);
    }

    private boolean tryOptimizeIncrement(Operand lhs, Element variable, Element literal) {
        if (variable instanceof Operand && literal instanceof LiteralElement) {
            Operand varOp = (Operand) variable;
            LiteralElement lit = (LiteralElement) literal;

            if (varOp.getName().equals(lhs.getName())) {
                return generateIincIfPossible(varOp, lit);
            }
        }
        return false;
    }

    private boolean generateIincIfPossible(Operand variable, LiteralElement literal) {
        try {
            int increment = Integer.parseInt(literal.getLiteral());
            if (increment >= -128 && increment <= 127) {
                var reg = currentMethod.getVarTable().get(variable.getName()).getVirtualReg();
                codeBuffer.append("iinc ").append(reg).append(" ").append(increment).append(NL);
                return true;
            }
        } catch (NumberFormatException e) {
            // Not a valid integer, cannot optimize
        }
        return false;
    }

    private void loadArrayReference(ArrayOperand arrayOp) {
        String name = arrayOp.getName();
        var descriptor = currentMethod.getVarTable().get(name);

        if (descriptor == null) {
            codeBuffer.append("aload_1").append(NL);
        } else {
            int reg = descriptor.getVirtualReg();
            codeBuffer.append(types.getLoadInstruction(arrayOp.getType(), reg, true)).append(NL);
        }
    }

    private void loadArrayIndex(ArrayOperand arrayOp) {
        codeBuffer.append(generators.apply(arrayOp.getIndexOperands().get(0)));
    }

    private void loadRightHandSide(Instruction rhsInst) {
        codeBuffer.append(generators.apply(rhsInst));
    }

    private void generateArrayStore(ArrayOperand arrayOp) {
        Type type = arrayOp.getType();
        if (type instanceof ArrayType arrayType) {
            type = arrayType.getElementType();
        }
        codeBuffer.append(types.getArrayStorePrefix(type)).append(NL);
    }

    private void storeToVariable(Operand operand) {
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();
        Type type = operand.getType();
        String storeInst = types.getStoreInstruction(type, reg);
        codeBuffer.append(storeInst).append(NL);
    }
}
