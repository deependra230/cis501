package cis501.submission;

import cis501.*;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import static cis501.Bypass.MX;
import static org.junit.Assert.assertEquals;

public class InorderPipelineSampleTest {

    private static IInorderPipeline sim;

    private static Insn makeInsn(int dst, int src1, int src2, MemoryOp mop) {
        if (MemoryOp.Store == mop) assert -1 == dst : dst;
        return new Insn(dst, src1, src2,
                1, 4,
                null, 0, null,
                mop, 1, 1,
                "synthetic");
    }

    @Before
    public void setup() {
        sim = new InorderPipeline(0/*no add'l memory latency*/, Bypass.FULL_BYPASS);
    }

    @Test
    public void test1Uop() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, null));
        sim.run(new InsnIterator(insns));
        assertEquals(1, sim.getInsns());
        // 12345678
        // fdxmw|
        assertEquals(6, sim.getCycles());
    }

    @Test
    public void testManyUops() {
        List<Insn> insns = new LinkedList<>();
        final int COUNT = 10;
        for (int i = 0; i < COUNT; i++) {
            insns.add(makeInsn(3, 1, 2, null));
        }
        sim.run(new InsnIterator(insns));
        assertEquals(COUNT, sim.getInsns());
        assertEquals(5 + COUNT, sim.getCycles());
    }

    @Test
    public void test1MemUop() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        sim.run(new InsnIterator(insns));
        assertEquals(1, sim.getInsns());
        // 123456789abcdef
        // fdxmw|
        assertEquals(6, sim.getCycles());
    }

    @Test
    public void testManyMemUops() {
        List<Insn> insns = new LinkedList<>();
        final int COUNT = 10;
        for (int i = 0; i < COUNT; i++) {
            insns.add(makeInsn(-1, 1, 2, MemoryOp.Store)); // no load-use dependencies
        }
        sim.run(new InsnIterator(insns));
        assertEquals(COUNT, sim.getInsns());
        assertEquals(5 + COUNT, sim.getCycles());
    }

    @Test
    public void testLoadUse1() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        insns.add(makeInsn(5, 3, 4, null)); // load to src reg 1
        sim.run(new InsnIterator(insns));
        assertEquals(2, sim.getInsns());
        // 123456789abcdef
        // fdxmw  |
        //  fd.xmw|
        assertEquals(6 + 2, sim.getCycles());
    }

    @Test
    public void testLoadUse2() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        insns.add(makeInsn(5, 4, 3, null)); // load to src reg 2
        sim.run(new InsnIterator(insns));
        assertEquals(2, sim.getInsns());
        // 123456789abcdef
        // fdxmw  |
        //  fd.xmw|
        assertEquals(6 + 2, sim.getCycles());
    }

    @Test
    public void testLoadUseStoreAddress() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        insns.add(makeInsn(-1, 4, 3, MemoryOp.Store)); // load to src reg 2 (store address), so we stall
        sim.run(new InsnIterator(insns));
        assertEquals(2, sim.getInsns());
        // 123456789abc
        // fdxmw  |
        //  fd.xmw|
        final long expected = 6 + 2;
        assertEquals(expected, sim.getCycles());
    }

    @Test
    public void testLoadUseStoreValue() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        insns.add(makeInsn(-1, 3, 4, MemoryOp.Store)); // load to src reg 1 (store value), so no stall
        sim.run(new InsnIterator(insns));
        assertEquals(2, sim.getInsns());
        // 123456789abcdef
        // fdxmw |
        //  fdxmw|
        final long expected = 6 + 1;
        assertEquals(expected, sim.getCycles());
    }

    @Test
    public void testMultipleProducerMX() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, null));
        insns.add(makeInsn(3, 4, 5, null));
        insns.add(makeInsn(7, 6, 3, null));

        sim = new InorderPipeline(0/*no add'l memory latency*/, EnumSet.of(MX));

        sim.run(new InsnIterator(insns));
        assertEquals(3, sim.getInsns());
        // 123456789a
        // fdxmw  |
        //  fdxmw |
        //   fdxmw|
        assertEquals(8, sim.getCycles());
    }

    @Test
    public void testMultipleProducerLoadMX() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        insns.add(makeInsn(3, 4, 5, null));
        insns.add(makeInsn(7, 6, 3, null));

        sim = new InorderPipeline(0/*no add'l memory latency*/, EnumSet.of(MX));

        sim.run(new InsnIterator(insns));
        assertEquals(3, sim.getInsns());
        // 123456789a
        // fdxmw  |
        //  fdxmw |
        //   fdxmw|
        assertEquals(8, sim.getCycles());
    }

}
