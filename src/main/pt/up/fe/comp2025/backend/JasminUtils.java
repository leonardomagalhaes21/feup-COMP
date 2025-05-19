// src/main/pt/up/fe/comp2025/backend/JasminUtils.java
package pt.up.fe.comp2025.backend;

import com.sun.jdi.VoidType;
import org.specs.comp.ollir.*;
import org.specs.comp.ollir.type.*;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import org.specs.comp.ollir.type.BuiltinType;
import org.specs.comp.ollir.type.BuiltinKind;
import org.specs.comp.ollir.type.ClassType;
import org.specs.comp.ollir.type.ArrayType;
import org.specs.comp.ollir.type.Type;

import java.util.HashMap;
import java.util.Map;

public class JasminUtils {

    private final OllirResult ollirResult;
    private final ClassUnit ollirClass;
    private final Map<String, String> jasminTypes;

    public JasminUtils(OllirResult ollirResult) {
        this.ollirResult = ollirResult;
        this.ollirClass = ollirResult.getOllirClass();
        this.jasminTypes = initJasminTypes();
    }

    private Map<String, String> initJasminTypes() {
        Map<String, String> types = new HashMap<>();
        types.put("void", "V");
        types.put("int", "I");
        types.put("boolean", "Z");
        types.put("String", "Ljava/lang/String;");
        return types;
    }



    public String getJasminType(Type type) {
        if (type instanceof ArrayType arrayType) {
            // Array: [ + element type
            return "[" + getJasminType(arrayType.getElementType());
        }
        if (type instanceof ClassType classType) {
            String name = classType.getName();
            if (name.equals("String")) {
                return "Ljava/lang/String;";
            }
            return "L" + name.replace('.', '/') + ";";
        }
        if (type instanceof BuiltinType builtinType) {
            BuiltinKind kind = builtinType.getKind();
            return switch (kind) {
                case INT32 -> "I";
                case BOOLEAN -> "Z";
                case VOID -> "V";
                case STRING -> "Ljava/lang/String;";

                default -> throw new IllegalArgumentException("Unknown or unsupported builtin type: " + kind);
            };
        }
        throw new IllegalArgumentException("Unknown or unsupported type: " + type);
    }


    public String getModifier(AccessModifier modifier) {
        switch (modifier) {
            case PUBLIC:
                return "public ";
            case PRIVATE:
                return "private ";
            case PROTECTED:
                return "protected ";
            default:
                return "";
        }
    }
}