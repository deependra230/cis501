package cis501.submission;

import cis501.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.*;

import static cis501.Bypass.*;
import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class InorderPipelineFullTest {

    private static Insn makeInsn(int dst, int src1, int src2, MemoryOp mop) {
        return new Insn(dst, src1, src2,
                1, 4,
                null, 0, null,
                mop, 1, 1,
                "synthetic");
    }

    @RunWith(Parameterized.class)
    public static class FullBypassTests {

        @Rule
        public final Timeout globalTimeout = Timeout.seconds(2);

        private final int ADDL_MEM_LAT;
        private final String MSG;
        private final IInorderPipeline sim;

        public FullBypassTests(int additionalMemLat) {
            super();
            ADDL_MEM_LAT = additionalMemLat;
            MSG = "[Add'l memory latency = " + ADDL_MEM_LAT + "]";
            final Set<Bypass> fullbp = FULL_BYPASS;

            sim = new cis501.submission.InorderPipeline(additionalMemLat, fullbp);
        }

        /** The memory latencies to test. */
        @Parameterized.Parameters
        public static Collection memLatencies() {
            return new CtorParams(0).p(1).p(3);
        }

        @Test
        public void test1Insn() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            sim.run(new InsnIterator(insns));
            assertEquals(MSG, 1, sim.getInsns());
            // 123456
            // fdxmw|
            assertEquals(MSG, 6, sim.getCycles());
        }

        @Test
        public void testManyInsns() {
            List<Insn> insns = new LinkedList<>();
            final int COUNT = 10;
            for (int i = 0; i < COUNT; i++) {
                insns.add(makeInsn(3, 1, 2, null));
            }
            sim.run(new InsnIterator(insns));
            assertEquals(MSG, COUNT, sim.getInsns());
            assertEquals(MSG, 5 + COUNT, sim.getCycles());
        }

        @Test
        public void test1MemInsn() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            sim.run(new InsnIterator(insns));
            assertEquals(MSG, 1, sim.getInsns());
            // 123456789abcdef
            // fdxmmmmw|
            assertEquals(MSG, 6 + ADDL_MEM_LAT, sim.getCycles());
        }

        @Test
        public void testManyMemInsns() {
            List<Insn> insns = new LinkedList<>();
            final int COUNT = 10;
            for (int i = 0; i < COUNT; i++) {
                insns.add(makeInsn(3, 1, 2, MemoryOp.Store)); // no load-use dependencies
            }
            sim.run(new InsnIterator(insns));
            assertEquals(MSG, COUNT, sim.getInsns());
            // 123456789abcdefghi
            // fdxmmmmw        |
            //  fdx   mmmmw    |
            //   fd   x   mmmmw|
            assertEquals(MSG, 5 + COUNT + (COUNT * ADDL_MEM_LAT), sim.getCycles());
        }

        @Test
        public void testLoadUse1() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            insns.add(makeInsn(5, 3, 4, null)); // load to src reg 1
            sim.run(new InsnIterator(insns));
            assertEquals(MSG, 2, sim.getInsns());
            // 123456789abcdef
            // fdxmmmmw  |
            //  fd    xmw|
            assertEquals(MSG, 6 + ADDL_MEM_LAT + 2, sim.getCycles());
        }

        @Test
        public void testLoadUse2() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            insns.add(makeInsn(5, 4, 3, null)); // load to src reg 2
            sim.run(new InsnIterator(insns));
            assertEquals(MSG, 2, sim.getInsns());
            // 123456789abcdef
            // fdxmmmmw  |
            //  fd    xmw|
            assertEquals(MSG, 6 + ADDL_MEM_LAT + 2, sim.getCycles());
        }

        @Test
        public void testLoadUseStoreAddress() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            insns.add(makeInsn(5, 4, 3, MemoryOp.Store)); // load to src reg 2 (store address), so we stall
            sim.run(new InsnIterator(insns));
            assertEquals(MSG, 2, sim.getInsns());
            // 123456789abcdef
            // fdxmmmmw     |
            //  fd    xmmmmw|
            final long expected = 6 + (2 * ADDL_MEM_LAT) + 2;
            assertEquals(MSG, expected, sim.getCycles());
        }

        @Test
        public void testLoadUseStoreValue() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            insns.add(makeInsn(5, 3, 4, MemoryOp.Store)); // load to src reg 1 (store value), so no stall
            sim.run(new InsnIterator(insns));
            assertEquals(MSG, 2, sim.getInsns());
            // 123456789abcdef
            // fdxmmmmw    |
            // fdx    mmmmw|
            final long expected = 6 + (2 * ADDL_MEM_LAT) + 1;
            assertEquals(MSG, expected, sim.getCycles());
        }

        @Test
        public void testLoadUseStoreAddress2() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            insns.add(makeInsn(5, 4, 3, MemoryOp.Store)); // load to src reg 2 (store address), so we stall
            insns.add(makeInsn(8, 6, 7, null)); // independent insn
            sim.run(new InsnIterator(insns));
            assertEquals(MSG, 3, sim.getInsns());
            // 123456789abcdef
            // fdxmmmmw      |
            //  fd    xmmmmw |
            //   f    dx   mw|
            final long expected = 6 + (2 * ADDL_MEM_LAT) + 3;
            assertEquals(MSG, expected, sim.getCycles());
        }

        @Test
        public void testLoadUseStoreValue2() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            insns.add(makeInsn(5, 3, 4, MemoryOp.Store)); // load to src reg 1 (store value), so no stall
            insns.add(makeInsn(8, 6, 7, null)); // independent insn
            sim.run(new InsnIterator(insns));
            assertEquals(MSG, 3, sim.getInsns());
            // 123456789abcdef
            // fdxmmmmw     |
            //  fdx   mmmmw |
            //   fd   x   mw|
            final long expected = 6 + (2 * ADDL_MEM_LAT) + 2;
            assertEquals(MSG, expected, sim.getCycles());
        }
    }

    @RunWith(Parameterized.class)
    public static class MXWXTests {

        @Rule
        public final Timeout globalTimeout = Timeout.seconds(2);

        private final int ADDL_MEM_LAT;
        private final Set<Bypass> BYPASSES;
        private String MSG;
        private IInorderPipeline sim;

        public MXWXTests(int additionalMemLat, Set<Bypass> bypasses) {
            super();
            ADDL_MEM_LAT = additionalMemLat;
            MSG = "[Add'l memory latency = " + ADDL_MEM_LAT + "]";
            BYPASSES = bypasses;
        }

        /** The memory latencies to test. */
        @Parameterized.Parameters
        public static Collection memLatenciesBypasses() {
            // returns a Collection of Object[], each Object[] holds ctor arguments
            List<Object[]> args = new LinkedList<>();
            for (Set<Bypass> bp : Arrays.asList(NO_BYPASS, EnumSet.of(WM))) {
                for (Integer aml : Arrays.asList(0, 1, 3)) {
                    args.add(new Object[]{aml, bp});
                }
            }
            return args;
        }

        void init(EnumSet<Bypass> extrabp) {
            Set<Bypass> bp = EnumSet.copyOf(extrabp);
            bp.addAll(BYPASSES);
            sim = new cis501.submission.InorderPipeline(ADDL_MEM_LAT, bp);
            MSG += String.format("[bypasses = %s]", bp);
        }


        @Test
        public void testMXWX1() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(5, 3, 4, null));

            init(EnumSet.of(MX, WX));

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789a
            // fdxmw |
            //  fdxmw|
            assertEquals(MSG + TestUtils.i2s(insns), 7, sim.getCycles());
        }


        @Test
        public void testMXWX2() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(5, 4, 3, null));

            init(EnumSet.of(MX, WX));

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789a
            // fdxmw |
            //  fdxmw|
            assertEquals(MSG + TestUtils.i2s(insns), 7, sim.getCycles());
        }


        @Test
        public void testMX1() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(5, 3, 4, null));

            init(EnumSet.of(MX));

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789a
            // fdxmw |
            //  fdxmw|
            assertEquals(MSG + TestUtils.i2s(insns), 7, sim.getCycles());
        }


        @Test
        public void testMX2() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(5, 4, 3, null));

            init(EnumSet.of(MX));

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789a
            // fdxmw |
            //  fdxmw|
            assertEquals(MSG + TestUtils.i2s(insns), 7, sim.getCycles());
        }


        @Test
        public void testWX1() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(5, 3, 4, null));

            init(EnumSet.of(WX));

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789a
            // fdxmw  |
            //  fd.xmw|
            assertEquals(MSG + TestUtils.i2s(insns), 8, sim.getCycles());
        }


        @Test
        public void testWX2() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(5, 4, 3, null));

            init(EnumSet.of(WX));

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789a
            // fdxmw  |
            //  fd.xmw|
            assertEquals(MSG + TestUtils.i2s(insns), 8, sim.getCycles());
        }


        @Test
        public void testNeither1() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(5, 3, 4, null));

            init(NO_BYPASS);

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789a
            // fdxmw   |
            //  fd..xmw|
            assertEquals(MSG + TestUtils.i2s(insns), 9, sim.getCycles());
        }


        @Test
        public void testNeither2() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(5, 4, 3, null));

            init(NO_BYPASS);

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789a
            // fdxmw   |
            //  fd..xmw|
            assertEquals(MSG + TestUtils.i2s(insns), 9, sim.getCycles());
        }


        @Test
        public void testMultipleProducerMX() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(3, 4, 5, null));
            insns.add(makeInsn(7, 6, 3, null));

            init(EnumSet.of(MX));

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 3, sim.getInsns());
            // 123456789a
            // fdxmw  |
            //  fdxmw |
            //   fdxmw|
            assertEquals(MSG + TestUtils.i2s(insns), 8, sim.getCycles());
        }


        @Test
        public void testMultipleProducerMXWX() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(3, 4, 5, null));
            insns.add(makeInsn(7, 6, 3, null));

            init(EnumSet.of(MX, WX));

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 3, sim.getInsns());
            // 123456789a
            // fdxmw  |
            //  fdxmw |
            //   fdxmw|
            assertEquals(MSG + TestUtils.i2s(insns), 8, sim.getCycles());
        }


        @Test
        public void testMultipleProducerWX() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(3, 4, 5, null));
            insns.add(makeInsn(7, 6, 3, null));

            init(EnumSet.of(WX));

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 3, sim.getInsns());
            // 123456789a
            // fdxmw   |
            //  fdxmw  |
            //   fd.xmw|
            assertEquals(MSG + TestUtils.i2s(insns), 9, sim.getCycles());
        }


        @Test
        public void testMultipleProducerNoBypass() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(3, 4, 5, null));
            insns.add(makeInsn(7, 6, 3, null));

            init(NO_BYPASS);

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 3, sim.getInsns());
            // 123456789a
            // fdxmw    |
            //  fdxmw   |
            //   fd..xmw|
            assertEquals(MSG + TestUtils.i2s(insns), 10, sim.getCycles());
        }


        @Test
        public void testMultipleProducerLoadMX() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            insns.add(makeInsn(3, 4, 5, null));
            insns.add(makeInsn(7, 6, 3, null));

            init(EnumSet.of(MX));

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 3, sim.getInsns());
            // 123456789a
            // fdxmw  |
            //  fdxmw |
            //   fdxmw|
            assertEquals(MSG + TestUtils.i2s(insns), 8 + ADDL_MEM_LAT, sim.getCycles());
        }


        @Test
        public void testMultipleProducerLoadMXWX() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            insns.add(makeInsn(3, 4, 5, null));
            insns.add(makeInsn(7, 6, 3, null));

            init(EnumSet.of(MX, WX));

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 3, sim.getInsns());
            // 123456789a
            // fdxmw  |
            //  fdxmw |
            //   fdxmw|
            assertEquals(MSG + TestUtils.i2s(insns), 8 + ADDL_MEM_LAT, sim.getCycles());
        }


        @Test
        public void testMultipleProducerLoadWX() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            insns.add(makeInsn(3, 4, 5, null));
            insns.add(makeInsn(7, 6, 3, null));

            init(EnumSet.of(WX));

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 3, sim.getInsns());
            // 123456789a
            // fdxmw   |
            //  fdxmw  |
            //   fd.xmw|
            assertEquals(MSG + TestUtils.i2s(insns), 9 + ADDL_MEM_LAT, sim.getCycles());
        }


        @Test
        public void testMultipleProducerLoadNoBypass() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            insns.add(makeInsn(3, 4, 5, null));
            insns.add(makeInsn(7, 6, 3, null));

            init(NO_BYPASS);

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 3, sim.getInsns());
            // 123456789a
            // fdxmw    |
            //  fdxmw   |
            //   fd..xmw|
            assertEquals(MSG + TestUtils.i2s(insns), 10 + ADDL_MEM_LAT, sim.getCycles());
        }


        @Test
        public void testDoubleDependenceMX() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(4, 1, 2, null));
            insns.add(makeInsn(5, 3, 4, null));

            init(EnumSet.of(MX));

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 3, sim.getInsns());
            // 123456789a
            // fdxmw    |
            //  fdxmw   |
            //   fd..xmw|
            assertEquals(MSG + TestUtils.i2s(insns), 10, sim.getCycles());
        }


        @Test
        public void testDoubleDependenceMXWX() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(4, 1, 2, null));
            insns.add(makeInsn(5, 3, 4, null));

            init(EnumSet.of(MX, WX));

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 3, sim.getInsns());
            // 123456789a
            // fdxmw  |
            //  fdxmw |
            //   fdxmw|
            assertEquals(MSG + TestUtils.i2s(insns), 8, sim.getCycles());
        }


        @Test
        public void testDoubleDependenceWX() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(4, 1, 2, null));
            insns.add(makeInsn(5, 3, 4, null));

            init(EnumSet.of(WX));

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 3, sim.getInsns());
            // 123456789a
            // fdxmw   |
            //  fdxmw  |
            //   fd.xmw|
            assertEquals(MSG + TestUtils.i2s(insns), 9, sim.getCycles());
        }
    }

    @RunWith(Parameterized.class)
    public static class WXTests {

        @Rule
        public final Timeout globalTimeout = Timeout.seconds(2);

        private final int ADDL_MEM_LAT;
        private final Set<Bypass> BYPASSES;
        private String MSG;
        private IInorderPipeline sim;

        public WXTests(int additionalMemLat, Set<Bypass> bypasses) {
            super();
            ADDL_MEM_LAT = additionalMemLat;
            MSG = "[Add'l memory latency = " + ADDL_MEM_LAT + "]";
            BYPASSES = bypasses;
        }

        /** The memory latencies to test. */
        @Parameterized.Parameters
        public static Collection memLatenciesBypasses() {
            // returns a Collection of Object[], each Object[] holds ctor arguments
            List<Object[]> args = new LinkedList<>();
            for (EnumSet<Bypass> wm : Arrays.asList(NO_BYPASS, EnumSet.of(WM))) {
                for (EnumSet<Bypass> mx : Arrays.asList(NO_BYPASS, EnumSet.of(MX))) {
                    for (Object aml : new Object[]{0, 1, 3}) {
                        EnumSet<Bypass> bp = EnumSet.copyOf(wm);
                        bp.addAll(mx);
                        args.add(new Object[]{aml, bp});
                    }
                }
            }
            return args;
        }

        void init(EnumSet<Bypass> extrabp) {
            Set<Bypass> bp = EnumSet.copyOf(extrabp);
            bp.addAll(BYPASSES);
            sim = new cis501.submission.InorderPipeline(ADDL_MEM_LAT, bp);
            MSG += String.format("[bypasses = %s]", bp);
        }


        @Test
        public void testWX() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(10, 10, 10, null));
            insns.add(makeInsn(5, 3, 4, null));

            init(EnumSet.of(WX));

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 3, sim.getInsns());
            // 123456789a
            // fdxmw  |
            //  fdxmw |
            //   fdxmw|
            assertEquals(MSG + TestUtils.i2s(insns), 8, sim.getCycles());
        }


        @Test
        public void testNoWX() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(10, 10, 10, null));
            insns.add(makeInsn(5, 4, 3, null));

            init(NO_BYPASS);

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 3, sim.getInsns());
            // 123456789a
            // fdxmw   |
            //  fdxmw  |
            //   fd.xmw|
            assertEquals(MSG + TestUtils.i2s(insns), 9, sim.getCycles());
        }


        @Test
        public void testNoWXLoadUse1() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            insns.add(makeInsn(5, 3, 4, null));

            init(NO_BYPASS);

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789a
            // fdxmw   |
            //  fd..xmw|
            assertEquals(MSG + TestUtils.i2s(insns), 7 + 2 + ADDL_MEM_LAT, sim.getCycles());
        }


        @Test
        public void testNoWXLoadUse2() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            insns.add(makeInsn(5, 4, 3, null));

            init(NO_BYPASS);

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789a
            // fdxmw   |
            //  fd..xmw|
            assertEquals(MSG + TestUtils.i2s(insns), 7 + 2 + ADDL_MEM_LAT, sim.getCycles());
        }


        @Test
        public void testWXLoadUse1() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            insns.add(makeInsn(5, 3, 4, null));

            init(EnumSet.of(WX));

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789a
            // fdxmw  |
            //  fd.xmw|
            assertEquals(MSG + TestUtils.i2s(insns), 7 + 1 + ADDL_MEM_LAT, sim.getCycles());
        }


        @Test
        public void testWXLoadUse2() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            insns.add(makeInsn(5, 4, 3, null));

            init(EnumSet.of(WX));

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789a
            // fdxmw  |
            //  fd.xmw|
            assertEquals(MSG + TestUtils.i2s(insns), 7 + 1 + ADDL_MEM_LAT, sim.getCycles());
        }
    }

    @RunWith(Parameterized.class)
    public static class WMTests {

        @Rule
        public final Timeout globalTimeout = Timeout.seconds(2);

        private final int ADDL_MEM_LAT;
        private final Set<Bypass> BYPASSES;
        private String MSG;
        private IInorderPipeline sim;

        public WMTests(int additionalMemLat, Set<Bypass> bypasses) {
            super();
            ADDL_MEM_LAT = additionalMemLat;
            MSG = "[Add'l memory latency = " + ADDL_MEM_LAT + "]";
            BYPASSES = bypasses;
        }

        /** The memory latencies to test. */
        @Parameterized.Parameters
        public static Collection memLatenciesBypasses() {
            // returns a Collection of Object[], each Object[] holds ctor arguments
            List<Object[]> args = new LinkedList<>();
            for (EnumSet<Bypass> mx : Arrays.asList(NO_BYPASS, EnumSet.of(MX))) {
                for (Object aml : new Object[]{0, 1, 3}) {
                    args.add(new Object[]{aml, mx});
                }
            }
            return args;
        }

        void init(EnumSet<Bypass> extrabp) {
            Set<Bypass> bp = EnumSet.copyOf(extrabp);
            bp.addAll(BYPASSES);
            sim = new cis501.submission.InorderPipeline(ADDL_MEM_LAT, bp);
            MSG += String.format("[bypasses = %s]", bp);
        }


        @Test
        public void testWM() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            insns.add(makeInsn(-1, 3, 4, MemoryOp.Store)); // load-use to store-reg

            init(EnumSet.of(WM));

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789a
            // fdxmw |
            //  fdxmw|
            assertEquals(MSG + TestUtils.i2s(insns), 7 + (2 * ADDL_MEM_LAT), sim.getCycles());
        }


        @Test
        public void testWmWx() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            insns.add(makeInsn(-1, 3, 4, MemoryOp.Store)); // load-use to store-reg

            init(EnumSet.of(WM, WX));

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789a
            // fdxmw |
            //  fdxmw|
            assertEquals(MSG + TestUtils.i2s(insns), 7 + (2 * ADDL_MEM_LAT), sim.getCycles());
        }


        @Test
        public void testWX() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            insns.add(makeInsn(-1, 3, 4, MemoryOp.Store)); // load-use to store-reg

            init(EnumSet.of(WX));

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789a
            // fdxmw  |
            //  fd.xmw|

            // 123456789a
            // fdxmmw   |
            //  fd..xmmw|

            assertEquals(MSG + TestUtils.i2s(insns), 8 + (2 * ADDL_MEM_LAT), sim.getCycles());
        }


        @Test
        public void testNone() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            insns.add(makeInsn(-1, 3, 4, MemoryOp.Store)); // load-use to store-reg

            init(NO_BYPASS);

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789a
            // fdxmw   |
            //  fd..xmw|
            assertEquals(MSG + TestUtils.i2s(insns), 9 + (2 * ADDL_MEM_LAT), sim.getCycles());
        }
    }

    @RunWith(Parameterized.class)
    public static class WMorMXTests {

        @Rule
        public final Timeout globalTimeout = Timeout.seconds(2);

        private final int ADDL_MEM_LAT;
        private final String MSG;
        private final Set<Bypass> BYPASSES;
        private IInorderPipeline sim;

        public WMorMXTests(int additionalMemLat, Set<Bypass> bypasses) {
            super();
            ADDL_MEM_LAT = additionalMemLat;
            BYPASSES = bypasses;
            MSG = String.format("[Add'l memory latency = %d, bypasses = %s]", ADDL_MEM_LAT, BYPASSES);
            sim = new cis501.submission.InorderPipeline(ADDL_MEM_LAT, BYPASSES);
        }

        /** The memory latencies and bypasses to test. */
        @Parameterized.Parameters
        public static Collection memLatenciesBypasses() {
            // returns a Collection of Object[], each Object[] holds ctor arguments
            List<Object[]> args = new LinkedList<>();
            for (Set<Bypass> usefulBP : Arrays.asList(EnumSet.of(WM), EnumSet.of(MX), EnumSet.of(WM, MX))) {
                for (Set<Bypass> dontcareBP : Arrays.asList(NO_BYPASS, EnumSet.of(WX))) {
                    for (Integer aml : Arrays.asList(0, 1, 3)) {
                        Set<Bypass> bp = EnumSet.copyOf(usefulBP);
                        bp.addAll(dontcareBP);
                        args.add(new Object[]{aml, bp});
                    }
                }
            }
            return args;
        }

        @Test
        public void testALUtoStoreValue() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(-1, 3, 4, MemoryOp.Store)); // dep. to store value

            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789a
            // fdxmw |
            //  fdxmw|
            assertEquals(MSG + TestUtils.i2s(insns), 7 + ADDL_MEM_LAT, sim.getCycles());
        }

    }

}
