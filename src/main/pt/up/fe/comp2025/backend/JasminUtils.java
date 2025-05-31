package pt.up.fe.comp2025.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.type.*;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.HashMap;
import java.util.Map;

public class JasminUtils {

    private final OllirResult ollirResult;

    public JasminUtils(OllirResult ollirResult) {
        this.ollirResult = ollirResult;
    }

    /**
     * Gets the Jasmin type representation of an OLLIR type.
     */
    public String getJasminType(Type type) {
        if (type == null) {
            return "V"; // void
        }

        if (type instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) type;
            return "[" + getJasminType(arrayType.getElementType());
        }

        if (type instanceof ClassType) {
            ClassType classType = (ClassType) type;

            if (classType.getKind() == ClassKind.OBJECTREF) {
                return "L" + classType.getName().replace(".", "/") + ";";
            }

            // This case should not happen, but just in case
            return "Ljava/lang/Object;";
        }

        if (type instanceof BuiltinType) {
            BuiltinType primitiveType = (BuiltinType) type;

            return switch (primitiveType.getKind()) {
                case INT32 -> "I";
                case BOOLEAN -> "Z";
                case STRING -> "Ljava/lang/String;";
                case VOID -> "V";
                default -> throw new IllegalArgumentException("Unknown primitive type: " + primitiveType);
            };
        }

        throw new IllegalArgumentException("Unknown type: " + type);
    }

    /**
     * Gets the prefix used for array load instructions based on type.
     */
    public String getArrayLoadPrefix(Type type) {
        if (type instanceof BuiltinType) {
            BuiltinType primitiveType = (BuiltinType) type;

            return switch (primitiveType.getKind()) {
                case INT32 -> "iaload";
                case BOOLEAN -> "baload";
                default -> "aaload";
            };
        }

        return "aaload";
    }

    /**
     * Gets the prefix used for array store instructions based on type.
     */
    public String getArrayStorePrefix(Type type) {
        if (type instanceof BuiltinType) {
            BuiltinType primitiveType = (BuiltinType) type;

            return switch (primitiveType.getKind()) {
                case INT32 -> "iastore";
                case BOOLEAN -> "bastore";
                default -> "aastore";
            };
        }

        return "aastore";
    }

    /**
     * Returns the appropriate load instruction based on the type and register.
     */
    public String getLoadInstruction(Type type, int register, boolean isArray) {
        if (isArray)
            return  "aload_" + register;
        String prefix;


        if (type instanceof BuiltinType) {
            BuiltinType primitiveType = (BuiltinType) type;

            prefix = switch (primitiveType.getKind()) {
                case INT32, BOOLEAN -> "i";
                default -> "a";
            };
        } else {
            prefix = "a"; // For arrays and objects
        }

        // Use specialized instructions for registers 0-3
        if (register >= 0 && register <= 3) {
            return prefix + "load_" + register;
        }

        return prefix + "load " + register;
    }

    /**
     * Returns the appropriate store instruction based on the type and register.
     */
    public String getStoreInstruction(Type type, int register) {
        String prefix;

        if (type instanceof BuiltinType) {
            BuiltinType primitiveType = (BuiltinType) type;

            prefix = switch (primitiveType.getKind()) {
                case INT32, BOOLEAN -> "i";
                default -> "a";
            };
        } else {
            prefix = "a"; // For arrays and objects
        }

        // Use specialized instructions for registers 0-3
        if (register >= 0 && register <= 3) {
            return prefix + "store_" + register;
        }

        return prefix + "store " + register;
    }

    /**
     * Returns the appropriate return instruction based on the type.
     */
    public String getReturnInstruction(Type type) {
        if (type == null || (type instanceof BuiltinType && ((BuiltinType) type).getKind() == BuiltinKind.VOID)) {
            return "return";
        }

        if (type instanceof BuiltinType) {
            BuiltinType primitiveType = (BuiltinType) type;

            return switch (primitiveType.getKind()) {
                case INT32, BOOLEAN -> "ireturn";
                default -> "areturn";
            };
        }

        return "areturn";
    }

    /**
     * Gets the type prefix for arithmetic operations.
     */
    public String getTypePrefix(Type type) {
        if (type instanceof BuiltinType) {
            BuiltinType primitiveType = (BuiltinType) type;

            return switch (primitiveType.getKind()) {
                case INT32, BOOLEAN -> "i";
                default -> "";
            };
        }

        return "";
    }

    /**
     * Gets the modifier representation for access modifiers.
     */
    public String getModifier(AccessModifier modifier) {
        return switch (modifier) {
            case DEFAULT -> "";
            case PUBLIC -> "public ";
            case PRIVATE -> "private ";
            case PROTECTED -> "protected ";
        };
    }

}