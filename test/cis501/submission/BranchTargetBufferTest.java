package cis501.submission;

import cis501.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BranchTargetBufferTest {

    private IBranchTargetBuffer btb;
    private static Insn makeBr(long pc, Direction dir, /*long fallthruPC*/ long targetPC) {
        return new Insn(1, 2, 3, pc, 4, dir, targetPC, null, null, 0, 0, "<synthetic>");
    }

    // BTB tests

    @Before
    public void setUp() throws Exception {
        // Runs before each test...() method
        btb = new BranchTargetBuffer(3/*index bits*/);
    }

    @Test
    public void testBtbNewTarget() {
        btb.train(0, 42);
        assertEquals(42, btb.predict(0));
    }

    // Bimodal tests

    @Test
    public void testBtbAlias() {
        btb.train(0, 42);
        assertEquals(42, btb.predict(0));
        long alias0 = (long) Math.pow(2, 3);
        btb.train(alias0, 100);
        assertEquals(0, btb.predict(0)); // tag doesn't match
        assertEquals(100, btb.predict(alias0)); // tag matches
    }

    @Test
    public void testBtbNoTarget() {
        assertEquals(0, btb.predict(6));
    }

    


}
