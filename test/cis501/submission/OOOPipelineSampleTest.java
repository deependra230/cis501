package cis501.submission;

import cis501.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import java.util.LinkedList;
import java.util.List;

import static cis501.submission.OOOPipeline.NUM_ARCH_REGS;
import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class OOOPipelineSampleTest {

    private static int currentPC = 0;

    private static Insn makeInsn(int dst, int src1, int src2, MemoryOp mop) {
        if (MemoryOp.Store == mop) assert -1 == dst : dst;
        currentPC += 4;
        return new Insn(dst, src1, src2,
                currentPC, 4,
                null, 0, null,
                mop, 1, 1,
                "synthetic");
    }

    private static Insn makeCCInsn(int dst, int src1, int src2, CondCodes cc) {
        currentPC += 4;
        return new Insn(dst, src1, src2,
                currentPC, 4,
                null, 0, cc,
                null, 1, 1,
                "synthetic cc");
    }

    private static Insn makeMemInsn(int dst, int src1, int src2, MemoryOp mop, long maddr) {
        if (MemoryOp.Store == mop) assert -1 == dst : dst;
        currentPC += 4;
        return new Insn(dst, src1, src2,
                currentPC, 4,
                null, 0, null,
                mop, maddr, 1,
                "synthetic mem");
    }

    private static Insn makeWideMemInsn(int dst, int src1, int src2, MemoryOp mop, long maddr, int msize) {
        if (MemoryOp.Store == mop) assert -1 == dst : dst;
        currentPC += 4;
        return new Insn(dst, src1, src2,
                currentPC, 4,
                null, 0, null,
                mop, maddr, msize,
                "synthetic wide mem");
    }

    private static Insn makePCInsn(int dst, int src1, int src2, long pc) {
        return new Insn(dst, src1, src2,
                pc, 4,
                null, 0, null,
                null, 1, 1,
                "synthetic");
    }

    public static class BasicTests {

        @Rule
        public final Timeout globalTimeout = Timeout.seconds(2);

        private IOOOPipeline sim;

        @Before
        public void setup() {
            sim = new OOOPipeline(NUM_ARCH_REGS + 20, 10, 5, null, null);
        }

        @Test
        public void test1Insn() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 1, sim.getInsns());
            // 123456789ab
            // fdrsigxmwc|
            assertEquals(TestUtils.i2s(insns), 11, sim.getCycles());
        }

        @Test
        public void test2Insns() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(6, 5, 4, null));
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789abc
            // fdrsigxmwc |
            //  fdrsigxmwc|
            assertEquals(TestUtils.i2s(insns), 12, sim.getCycles());
        }

        @Test
        public void testManyInsns() {
            List<Insn> insns = new LinkedList<>();
            final int COUNT = 8;
            for (int i = 0; i < COUNT; i++) {
                insns.add(makeInsn(3, 1, 2, null));
            }
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), COUNT, sim.getInsns());
            assertEquals(TestUtils.i2s(insns), 10 + COUNT, sim.getCycles());
        }

        @Test
        public void testRegDep1() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(2, 1, 1, null));
            insns.add(makeInsn(3, 2, 1, null));
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789abc
            // fdrsigxmwc |
            //  fdrsigxmwc|
            assertEquals(TestUtils.i2s(insns), 12, sim.getCycles());
        }

        @Test
        public void testRegDep2() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(2, 1, 1, null));
            insns.add(makeInsn(3, 1, 2, null));
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789abc
            // fdrsigxmwc |
            //  fdrsigxmwc|
            assertEquals(TestUtils.i2s(insns), 12, sim.getCycles());
        }

        @Test
        public void test1MemInsn() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 1, sim.getInsns());
            // 123456789abcdef
            // fdrsigxmwc|
            assertEquals(TestUtils.i2s(insns), 11, sim.getCycles());
        }

        @Test
        public void testManyMemInsns() {
            List<Insn> insns = new LinkedList<>();
            final int COUNT = 8;
            for (int i = 0; i < COUNT; i++) {
                insns.add(makeInsn(-1, 1, 2, MemoryOp.Store)); // no load-use dependencies
            }
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), COUNT, sim.getInsns());
            // 123456789abcdefghi
            // fdrsigxmwc  |
            //  fdrsigxmwc |
            //   fdrsigxmwc|
            assertEquals(TestUtils.i2s(insns), 10 + COUNT, sim.getCycles());
        }

        @Test
        public void testLoadUse1() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            insns.add(makeInsn(5, 3, 4, null)); // load to src reg 1
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789abcdef
            // fdrsigxmwc    |
            //  fdrs...igxmwc|
            assertEquals(TestUtils.i2s(insns), 15, sim.getCycles());
        }

        @Test
        public void testLoadUse2() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            insns.add(makeInsn(5, 4, 3, null)); // load to src reg 2
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789abcdef
            // fdrsigxmwc    |
            //  fdrs...igxmwc|
            assertEquals(TestUtils.i2s(insns), 15, sim.getCycles());
        }

        @Test
        public void testLoadUseStoreAddress() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            insns.add(makeInsn(-1, 4, 3, MemoryOp.Store)); // load to src reg 2 (store address), so we stall
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789abcdef
            // fdrsigxmwc    |
            //  fdrs...igxmwc|
            assertEquals(TestUtils.i2s(insns), 15, sim.getCycles());
        }

        @Test
        public void testLoadUseStoreValue() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            insns.add(makeInsn(-1, 3, 4, MemoryOp.Store)); // load to src reg 1 (store value), but *still stall*
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789abcdef
            // fdrsigxmwc    |
            //  fdrs...igxmwc|
            assertEquals(TestUtils.i2s(insns), 15, sim.getCycles());
        }

        @Test
        public void testLoadUseStoreAddress2() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            insns.add(makeInsn(-1, 4, 3, MemoryOp.Store)); // load to src reg 2 (store address), so we stall
            insns.add(makeInsn(8, 6, 7, null)); // independent insn
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 3, sim.getInsns());
            // 123456789abcdefg
            // fdrsigxmwc     |
            //  fdrs...igxmwc |
            //   fdrsigxmw   c|
            assertEquals(TestUtils.i2s(insns), 16, sim.getCycles());
        }

        @Test
        public void testLoadUseStoreValue2() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            insns.add(makeInsn(-1, 3, 4, MemoryOp.Store)); // load to src reg 1 (store value), but *still stall*
            insns.add(makeInsn(8, 6, 7, null)); // independent insn
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 3, sim.getInsns());
            // 123456789abcdefg
            // fdrsigxmwc     |
            //  fdrs...igxmwc |
            //   fdrsigxmw   c|
            assertEquals(TestUtils.i2s(insns), 16, sim.getCycles());
        }

        @Test
        public void testOOO() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(6, 5, 4, null));
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789abc
            // fdrsigxmwc |
            //  fdrsigxmwc|
            assertEquals(TestUtils.i2s(insns), 12, sim.getCycles());
        }
    }

    public static class RenameTests {

        @Rule
        public final Timeout globalTimeout = Timeout.seconds(2);
        private final int PREGS = NUM_ARCH_REGS + 2;
        private String MSG;
        private IOOOPipeline sim;

        @Before
        public void setup() {
            MSG = "[pregs = " + PREGS + "]";
            sim = new OOOPipeline(PREGS, 20, 5, null, null);
        }

        @Test
        public void testEnoughPregs() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(4, 1, 2, null));
            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789abc
            // fdrsigxmwc |
            //  fdrsigxmwc|
            assertEquals(MSG + TestUtils.i2s(insns), 12, sim.getCycles());
        }

        @Test
        public void testOutOfPregs() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(4, 1, 2, null));
            insns.add(makeInsn(5, 1, 2, null));
            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 3, sim.getInsns());
            // 123456789abcdefhijk
            // fdrsigxmwc        |
            //  fdrsigxmwc       |
            //   fdr......sigxmwc|
            assertEquals(MSG + TestUtils.i2s(insns), 12 + 7, sim.getCycles());
        }

        @Test
        public void testStoreNoAllocatePreg() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(-1, 1, 2, MemoryOp.Store)); // doesn't allocate a new preg
            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 3, sim.getInsns());
            // 123456789abc
            // fdrsigxmwc |
            //  fdrsigxmwc|
            assertEquals(MSG + TestUtils.i2s(insns), 13, sim.getCycles());
        }

        @Test
        public void testAllocateForCondCodes1() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeCCInsn(3, 1, 2, CondCodes.WriteCC));
            insns.add(makeCCInsn(4, 1, 2, null));
            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789abcdefhijk
            // fdrsigxmwc        |
            //  fdr.......sigxmwc|
            assertEquals(MSG + TestUtils.i2s(insns), 19, sim.getCycles());
        }

        @Test
        public void testAllocateForCondCodes2() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeCCInsn(3, 1, 2, CondCodes.ReadWriteCC));
            insns.add(makeCCInsn(4, 1, 2, null));
            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789abcdefhijk
            // fdrsigxmwc        |
            //  fdr.......sigxmwc|
            assertEquals(MSG + TestUtils.i2s(insns), 19, sim.getCycles());
        }

        @Test
        public void testAllocateForCondCodes3() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeCCInsn(3, 1, 2, null));
            insns.add(makeCCInsn(4, 1, 2, CondCodes.WriteCC));
            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789abcdefhijk
            // fdrsigxmwc        |
            //  fdr.......sigxmwc|
            assertEquals(MSG + TestUtils.i2s(insns), 19, sim.getCycles());
        }

        @Test
        public void testAllocateForCondCodes4() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeCCInsn(3, 1, 2, null));
            insns.add(makeCCInsn(4, 1, 2, CondCodes.ReadWriteCC));
            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789abcdefhijk
            // fdrsigxmwc        |
            //  fdr.......sigxmwc|
            assertEquals(MSG + TestUtils.i2s(insns), 19, sim.getCycles());
        }
    }
    
  

    public static class OneEntryROBTests {

        @Rule
        public final Timeout globalTimeout = Timeout.seconds(2);
        private final int PREGS = NUM_ARCH_REGS + 20;
        private final int ISSUE_Q = 5;
        private final int ROB_SIZE = 1;
        private String MSG;
        private IOOOPipeline sim;

        @Before
        public void setup() {
            MSG = "[ROB size = " + ROB_SIZE + "]";
            sim = new OOOPipeline(PREGS, ROB_SIZE, ISSUE_Q, null, null);
        }

        @Test
        public void test2Insns() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(4, 1, 2, null));
            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 2, sim.getInsns());
            // 12345678901234567890123
            // fdrsigxmwc          |
            //           fdrsigxmwc|
            assertEquals(MSG + TestUtils.i2s(insns), (2 * 10) + 1, sim.getCycles());
        }

        @Test
        public void test3Insns() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, null));
            insns.add(makeInsn(4, 1, 2, null));
            insns.add(makeInsn(5, 1, 2, null));
            sim.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 3, sim.getInsns());
            // 12345678901234567890123
            // fdrsigxmwc          |
            //           fdrsigxmwc|
            // etc
            assertEquals(MSG + TestUtils.i2s(insns), (3 * 10) + 1, sim.getCycles());
        }

    }

    public static class ICacheTests {

        @Rule
        public final Timeout globalTimeout = Timeout.seconds(2);
        private IOOOPipeline sim;

        @Before
        public void setup() {
            final int PREGS = NUM_ARCH_REGS + 20;
            final int ISSUE_Q = 10;
            final int ROB_SIZE = 10;
            final int INDEX_BITS = 1;
            final int WAYS = 1;
            final int OFFSET_BITS = 3; // 8B == two 4B insns
            final int ACCESS_LAT = 0;
            final int CLEAN_MISS_LAT = 2;
            final int DIRTY_MISS_LAT = 2;

            ICache ic = new Cache(INDEX_BITS, WAYS, OFFSET_BITS, ACCESS_LAT, CLEAN_MISS_LAT, DIRTY_MISS_LAT);
            sim = new OOOPipeline(PREGS, ROB_SIZE, ISSUE_Q, ic, null);
        }

        @Test
        public void testImiss() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makePCInsn(3, 1, 2, 0x0)); // I$ miss
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 1, sim.getInsns());
            // 12345678901234
            // f..drsigxmwc|
            assertEquals(TestUtils.i2s(insns), 13, sim.getCycles());
        }

        @Test
        public void testImissIhit() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makePCInsn(3, 1, 2, 0x0)); // I$ miss
            insns.add(makePCInsn(4, 1, 2, 0x4)); // I$ hit
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 2, sim.getInsns());
            // 123456789012345
            // f..drsigxmwc |
            //    fdrsigxmwc|
            assertEquals(TestUtils.i2s(insns), 14, sim.getCycles());
        }

        @Test
        public void testImissImiss() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makePCInsn(3, 1, 2, 0x0)); // I$ miss
            insns.add(makePCInsn(4, 1, 2, 0x48)); // I$ miss
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 2, sim.getInsns());
            // 1234567890123456
            // f..drsigxmwc   |
            //    f..drsigxmwc|
            assertEquals(TestUtils.i2s(insns), 16, sim.getCycles());
        }

    }

    public static class DCacheTests {

        @Rule
        public final Timeout globalTimeout = Timeout.seconds(2);
        private IOOOPipeline sim;

        @Before
        public void setup() {
            final int PREGS = NUM_ARCH_REGS + 20;
            final int ISSUE_Q = 5;
            final int ROB_SIZE = 10;
            final int INDEX_BITS = 1;
            final int WAYS = 1;
            final int OFFSET_BITS = 2; // 4B blocks
            final int ACCESS_LAT = 0;
            final int CLEAN_MISS_LAT = 2;
            final int DIRTY_MISS_LAT = 2;

            ICache dc = new Cache(INDEX_BITS, WAYS, OFFSET_BITS, ACCESS_LAT, CLEAN_MISS_LAT, DIRTY_MISS_LAT);
            sim = new OOOPipeline(PREGS, ROB_SIZE, ISSUE_Q, null, dc);
        }

        @Test
        public void testLoadMiss() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load)); // D$ miss
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 1, sim.getInsns());
            // 1234567890123456789
            // fdrsigxm..wc|
            assertEquals(TestUtils.i2s(insns), 13, sim.getCycles());
        }

        @Test
        public void testLoadMissMiss() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeMemInsn(3, 1, 2, MemoryOp.Load, 0x0)); // D$ miss
            insns.add(makeMemInsn(3, 1, 2, MemoryOp.Load, 0x48)); // D$ miss
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 2, sim.getInsns());
            // 1234567890123456789
            // fdrsigxm..wc   |
            //  fdrsigx  m..wc|
            assertEquals(TestUtils.i2s(insns), 16, sim.getCycles());
        }

        @Test
        public void testLoadMissStoreMiss() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeMemInsn(3, 1, 2, MemoryOp.Load, 0x0)); // D$ miss
            insns.add(makeMemInsn(-1, 1, 2, MemoryOp.Store, 0x48)); // D$ miss
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 2, sim.getInsns());
            // 1234567890123456789
            // fdrsigxm..wc   |
            //  fdrsigx  m..wc|
            assertEquals(TestUtils.i2s(insns), 16, sim.getCycles());
        }

        @Test
        public void testLoadMissHit() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeMemInsn(3, 1, 2, MemoryOp.Load, 0x0)); // D$ miss
            insns.add(makeMemInsn(3, 1, 2, MemoryOp.Load, 0x0)); // D$ hit
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 2, sim.getInsns());
            // 1234567890123456789
            // fdrsigxm..wc |
            //  fdrsigx  mwc|
            assertEquals(TestUtils.i2s(insns), 14, sim.getCycles());
        }

        @Test
        public void testLoadMissStoreHit() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeMemInsn(3, 1, 2, MemoryOp.Load, 0x0)); // D$ miss
            insns.add(makeMemInsn(-1, 1, 2, MemoryOp.Store, 0x0)); // D$ hit
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 2, sim.getInsns());
            // 1234567890123456789
            // fdrsigxm..wc |
            //  fdrsigx  mwc|
            assertEquals(TestUtils.i2s(insns), 14, sim.getCycles());
        }

        @Test
        public void testStoreMissLoadHit() {
            // check that stores access cache during M
            List<Insn> insns = new LinkedList<>();
            insns.add(makeMemInsn(-1, 1, 2, MemoryOp.Store, 0x0)); // D$ miss
            insns.add(makeMemInsn(3, 1, 2, MemoryOp.Load, 0x0)); // D$ hit
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 2, sim.getInsns());
            // 1234567890123456789
            // fdrsigxm..wc |
            //  fdrsigx  mwc|
            assertEquals(TestUtils.i2s(insns), 14, sim.getCycles());
        }

        @Test
        public void testLoadUse() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
            insns.add(makeInsn(4, 1, 3, null)); // dependent on load
            insns.add(makeInsn(4, 1, 2, null));
            insns.add(makeInsn(4, 1, 2, null));
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 4, sim.getInsns());
            // 1234567890123456789
            // fdrsigxm..wc      |
            //  fdrs     igxmwc  |
            //   fdrsigxmw     c |
            //    fdrsigxmw     c|
            assertEquals(TestUtils.i2s(insns), 19, sim.getCycles());
        }

        @Test
        public void testLoadUseMissHit() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load)); // $ miss
            insns.add(makeInsn(4, 1, 2, MemoryOp.Load)); // $ hit
            insns.add(makeInsn(5, 3, 4, null)); // dependent on both loads
            insns.add(makeInsn(5, 1, 2, null));
            insns.add(makeInsn(5, 1, 2, null));
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 5, sim.getInsns());
            // 12345678901234567890
            // fdrsigxm..wc       |
            //  fdrsigx  mwc      |
            //   fdrs     igxmwc  |
            //    fdrsig xmw    c |
            //     fdrsi gxmw    c|
            assertEquals(TestUtils.i2s(insns), 20, sim.getCycles());
        }

        @Test
        public void testLoadUseMissMiss() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load)); // $ miss
            insns.add(makeMemInsn(4, 1, 2, MemoryOp.Load, 0x16)); // $ miss
            insns.add(makeInsn(5, 3, 4, null)); // dependent on both loads
            insns.add(makeInsn(5, 1, 2, null));
            insns.add(makeInsn(5, 1, 2, null));
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 5, sim.getInsns());
            // 12345678901234567890123
            // fdrsigxm..wc         |
            //  fdrsigx  m..wc      |
            //  fdrs        igxmwc  |
            //   fdrsig x   mw    c |
            //    fdrsig x   mw    c|
            assertEquals(TestUtils.i2s(insns), 22, sim.getCycles());
        }

        @Test
        public void testNoWAW() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load)); // $ miss
            insns.add(makeInsn(3, 1, 2, null)); // produces r3
            insns.add(makeInsn(4, 1, 3, null)); // consumes r3 from ALU op, not from load
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 3, sim.getInsns());
            // 123456789012345
            // fdrsigxm..wc  |
            //  fdrsigx  mwc |
            //   fdrsig  xmwc|
            assertEquals(TestUtils.i2s(insns), 15, sim.getCycles());
        }

        @Test
        public void testNoWAR() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load)); // $ miss
            insns.add(makeInsn(4, 1, 3, null));
            insns.add(makeInsn(3, 1, 2, null));
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 3, sim.getInsns());
            // 123456789012345678
            // fdrsigxm..wc     |
            //  fdrs     igxmwc |
            //   fdrsigx mw    c|
            assertEquals(TestUtils.i2s(insns), 18, sim.getCycles());
        }

        @Test
        public void testLoadNoAliasAddr() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeMemInsn(3, 1, 2, MemoryOp.Load, 0x0)); // D$ miss
            insns.add(makeMemInsn(-1, 1, 3, MemoryOp.Store, 0x1)); // D$ hit, load-use
            insns.add(makeMemInsn(4, 1, 2, MemoryOp.Load, 0x2)); // D$ hit, no aliasing
            insns.add(makeInsn(5, 1, 4, null)); // load-use
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 4, sim.getInsns());
            // 1234567890123456789
            // fdrsigxm..wc      |
            //  fdrs     igxmwc  |
            //   fdrsigx mw    c |
            //    fdrs    igxmw c|
            assertEquals(TestUtils.i2s(insns), 19, sim.getCycles());
        }

        @Test
        public void testLoadNoAliasAge() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeMemInsn(3, 1, 2, MemoryOp.Load, 0x0)); // D$ miss
            insns.add(makeMemInsn(4, 1, 2, MemoryOp.Load, 0x2)); // D$ hit, no aliasing
            insns.add(makeMemInsn(-1, 1, 3, MemoryOp.Store, 0x1)); // D$ hit, load-use
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 3, sim.getInsns());
            // 1234567890123456789
            // fdrsigxm..wc    |
            //  fdrsigx  mwc   |
            //   fdrs    igxmwc|
            assertEquals(TestUtils.i2s(insns), 17, sim.getCycles());
        }

        @Test
        public void testLoadAliasAddr() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeMemInsn(3, 1, 2, MemoryOp.Load, 0x0)); // D$ miss
            insns.add(makeMemInsn(-1, 1, 3, MemoryOp.Store, 0x1)); // D$ hit, load-use
            insns.add(makeMemInsn(4, 1, 2, MemoryOp.Load, 0x1)); // D$ hit, aliasing
            insns.add(makeInsn(5, 1, 4, null)); // load-use
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 4, sim.getInsns());
            // 12345678901234567890123
            // fdrsigxm..wc         |
            //  fdrs     igxmwc     |
            //   fdrs     igxmwc    |
            //    fdrs        igxmwc|
            assertEquals(TestUtils.i2s(insns), 22, sim.getCycles());
        }

        @Test
        public void testLoadAliasOverlapping1() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeMemInsn(3, 1, 2, MemoryOp.Load, 0x0)); // D$ miss
            insns.add(makeWideMemInsn(-1, 1, 3, MemoryOp.Store, 0x2, 2)); // D$ hit, load-use
            insns.add(makeMemInsn(4, 1, 2, MemoryOp.Load, 0x3)); // D$ hit, aliasing
            insns.add(makeInsn(5, 1, 4, null)); // load-use
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 4, sim.getInsns());
            // 12345678901234567890123
            // fdrsigxm..wc         |
            //  fdrs     igxmwc     |
            //   fdrs     igxmwc    |
            //    fdrs        igxmwc|
            assertEquals(TestUtils.i2s(insns), 22, sim.getCycles());
        }

        @Test
        public void testLoadAliasOverlapping2() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeMemInsn(3, 1, 2, MemoryOp.Load, 0x0)); // D$ miss
            insns.add(makeMemInsn(-1, 1, 3, MemoryOp.Store, 0x3)); // D$ hit, load-use
            insns.add(makeWideMemInsn(4, 1, 2, MemoryOp.Load, 0x2, 2)); // D$ hit, aliasing
            insns.add(makeInsn(5, 1, 4, null)); // load-use
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 4, sim.getInsns());
            // 12345678901234567890123
            // fdrsigxm..wc         |
            //  fdrs     igxmwc     |
            //   fdrs     igxmwc    |
            //    fdrs        igxmwc|
            assertEquals(TestUtils.i2s(insns), 22, sim.getCycles());
        }

    }

    public static class OneEntryIssueQTests {

        @Rule
        public final Timeout globalTimeout = Timeout.seconds(2);
        private IOOOPipeline sim;

        @Before
        public void setup() {
            final int PREGS = NUM_ARCH_REGS + 20;
            final int ISSUE_Q = 1;
            final int ROB_SIZE = 10;
            final int INDEX_BITS = 1;
            final int WAYS = 1;
            final int OFFSET_BITS = 2;
            final int ACCESS_LAT = 0;
            final int CLEAN_MISS_LAT = 5;
            final int DIRTY_MISS_LAT = 5;

            ICache dc = new Cache(INDEX_BITS, WAYS, OFFSET_BITS, ACCESS_LAT, CLEAN_MISS_LAT, DIRTY_MISS_LAT);
            sim = new OOOPipeline(PREGS, ROB_SIZE, ISSUE_Q, null, dc);
        }

        @Test
        public void testFullIssueQ1() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInsn(3, 1, 2, MemoryOp.Load)); // D$ miss
            insns.add(makeInsn(4, 1, 2, null));
            insns.add(makeInsn(5, 1, 2, null));
            insns.add(makeInsn(6, 1, 2, null));
            insns.add(makeInsn(7, 1, 2, null));
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 5, sim.getInsns());
            // 123456789012345678901
            // fdrsigxm.....wc     |
            //  fdr sigx    mwc    |
            //   fd r sig   xmwc   |
            //    f d r si  gxmwc  |
            //      f d r   sigxmwc|
            assertEquals(TestUtils.i2s(insns), 21, sim.getCycles());
        }

        @Test
        public void testFullIssueQ2() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeMemInsn(3, 1, 2, MemoryOp.Load, 0x1)); // D$ miss
            insns.add(makeMemInsn(4, 1, 3, MemoryOp.Load, 0x16)); // D$ miss and load-use
            insns.add(makeInsn(5, 1, 2, null));
            insns.add(makeInsn(6, 1, 2, null));
            insns.add(makeInsn(7, 1, 2, null));
            sim.run(new InsnIterator(insns));
            assertEquals(TestUtils.i2s(insns), 5, sim.getInsns());
            // 1234567890123456789012345678
            // fdrsigxm.....wc            |
            //  fdr s       igxm.....wc   |
            //   fd r        sigx    mwc  |
            //    f d        r sig   xmwc |
            //      f        d r si  gxmwc|
            assertEquals(TestUtils.i2s(insns), 28, sim.getCycles());
        }
    }
    
    //test load dependence making sure that issures in cycle K
    //test instruction cache which would cause stalls in the fetch stage
    //check memory address of load with second byte not issued yet
    //check 
    
    //WAW 
    //WAR
    // register dependencies checks
    
    //i$miss: I$ miss d$miss: D$ miss nopregs: no physical registers available causing stalls in Rename iqfull:

}
