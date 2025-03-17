package pt.up.fe.comp.cp1;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;


public class ExtraTests {

    //adicional tests
    @Test
    public void missingReturnPath() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/MissingReturnPath.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void booleanOperations() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/BooleanOperations.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void voidReturnValue() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/VoidReturnValue.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void nestedObjectAccess() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/NestedObjectAccess.jmm"));
        TestUtils.noErrors(result);
    }


    @Test
    public void stringConcatenation() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/StringConcatenation.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void methodOverloading() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/MethodOverloading.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void typeNarrowing() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/TypeNarrowing.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void varDeclCantBeVarargs() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/VarDeclCantBeVarargs.jmm"));
        TestUtils.mustFail(result);
    }

    @Test
    public void objectExtendsTest() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/ObjectExtendsTest.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void fieldDeclCantBeVarargs() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp1/extratests/FieldDeclCantBeVarargs.jmm"));
        TestUtils.mustFail(result);
    }
}
