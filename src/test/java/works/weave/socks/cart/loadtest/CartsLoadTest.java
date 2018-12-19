package works.weave.socks.cart.loadtest;

import com.neotys.testing.framework.BaseNeoLoadDesign;
import com.neotys.testing.framework.NeoLoadTest;

public class CartsLoadTest extends NeoLoadTest {
    @Override
    protected BaseNeoLoadDesign design() {
        return new TestingDesign();
    }

    @Override
    protected String projectName() {
        return "Carts_NeoLoad";
    }

    @Override
    public void createComplexPopulation() {

    }

    @Override
    public void createComplexScenario() {

    }

    @Override
    public void execute() {

        createSimpleConstantLoadScenario("Cart_Load","AddItemToCart",600,49,10);
        createSimpleConstantIterationScenario("DynatraceSanityCheck","BasicCheckTesting",15,1,0);
        createSanityCheckScenario();
    }
}
