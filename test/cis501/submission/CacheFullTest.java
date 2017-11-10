package cis501.submission;

import cis501.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(Enclosed.class)
public class CacheFullTest {

    private static final int ACCESS_LAT = 0;
    private static final int CLEAN_MISS_LAT = 2;
    private static final int DIRTY_MISS_LAT = 3;

    private static Insn makeBr(long pc, Direction dir, int isize, long targetPC) {
        return new Insn(-1, 0, 0, pc, isize, dir, targetPC, null, null, 0, 0, "fake-branch");
    }


    // pipeline integration tests

    private static Insn makeInt(int src1, int src2, int dst, long pc, int isize) {
        return new Insn(dst, src1, src2, pc, isize, null, 0, null, null, 0, 0, "fake-intop");
    }

    private static Insn makeMem(int src1, int src2, int dst, long pc, int isize, MemoryOp mop, long dataAddr) {
        return new Insn(dst, src1, src2, pc, isize, null, 0, null, mop, dataAddr, 1, "fake-mem");
    }

    @RunWith(Parameterized.class)
    public static class CacheTests {

        @Rule
        public final Timeout globalTimeout = Timeout.seconds(2);
        private final int INDEX_BITS;
        private final int WAYS;
        private final int BLOCK_BITS;
        private final String MSG;
        private final ICache cache;

        public CacheTests(int indexBits, int ways, int blockBits) {
            INDEX_BITS = indexBits;
            WAYS = ways;
            BLOCK_BITS = blockBits;

            MSG = "[index bits = " + INDEX_BITS +
                    ", assoc = " + WAYS +
                    ", block offset bits = " + BLOCK_BITS + "]";

            cache = new cis501.submission.Cache(INDEX_BITS, WAYS, BLOCK_BITS,
                    ACCESS_LAT, CLEAN_MISS_LAT, DIRTY_MISS_LAT);
        }

        /** The {index bits,ways,block offset bits} to test. */
        @Parameterized.Parameters
        public static Collection params() {
            return new CtorParams(2, 1, 2).p(2, 2, 2).p(3, 4, 1).p(0, 4, 1)/*fully-associative*/;
        }


        @Test
        public void testInitialState() {
            final long addr = 0xFF << (INDEX_BITS + BLOCK_BITS);
            int lat = cache.access(true, addr);
            assertEquals(MSG, CLEAN_MISS_LAT, lat);
        }


        @Test
        public void testRemainderIndexing() {
            final long addr = -1;
            int lat = cache.access(true, addr);
            assertEquals(MSG, CLEAN_MISS_LAT, lat);
        }


        @Test
        public void testBlockOffset() {
            final long firstByteInBlock = 0xFF << (INDEX_BITS + BLOCK_BITS);
            int lat = cache.access(true, firstByteInBlock);
            assertEquals(MSG, CLEAN_MISS_LAT, lat);
            final long lastByteInBlock = firstByteInBlock + (1 << BLOCK_BITS) - 1;
            lat = cache.access(true, lastByteInBlock);
            assertEquals(MSG, ACCESS_LAT, lat);
        }


        @Test
        public void testLRU() {
            final long a = 0xFF << (INDEX_BITS + BLOCK_BITS);
            final long waySize = (1 << INDEX_BITS) * (1 << BLOCK_BITS);

            int lat = cache.access(true, a);
            assertEquals(MSG, CLEAN_MISS_LAT, lat);

            for (int w = 1; w < WAYS * 2; w++) {
                // a hits
                lat = cache.access(true, a);
                assertEquals(MSG, ACCESS_LAT, lat);

                // conflicting access
                lat = cache.access(true, a + (w * waySize));
                assertEquals(MSG, CLEAN_MISS_LAT, lat);
            }
        }


        @Test
        public void testFullSetLoads() {
            final long a = 0xFF << (INDEX_BITS + BLOCK_BITS);
            final long waySize = (1 << INDEX_BITS) * (1 << BLOCK_BITS);
            for (int w = 0; w < WAYS; w++) {
                int lat = cache.access(true, a + (w * waySize));
                assertEquals(MSG, CLEAN_MISS_LAT, lat);
            }

            // a should still be in the cache
            int lat = cache.access(true, a);
            assertEquals(MSG, ACCESS_LAT, lat);
        }


        @Test
        public void testConflictMissLoads() {
            final long a = 0xFF << (INDEX_BITS + BLOCK_BITS);
            final long waySize = (1 << INDEX_BITS) * (1 << BLOCK_BITS);
            for (int w = 0; w < WAYS + 1; w++) {
                int lat = cache.access(true, a + (w * waySize));
                assertEquals(MSG, CLEAN_MISS_LAT, lat);
            }

            // a should have gotten evicted
            int lat = cache.access(true, a);
            assertEquals(MSG, CLEAN_MISS_LAT, lat);
        }


        @Test
        public void testConflictMissStores() {
            final long a = 0xFF << (INDEX_BITS + BLOCK_BITS);
            final long waySize = (1 << INDEX_BITS) * (1 << BLOCK_BITS);
            for (int w = 0; w < WAYS; w++) {
                int lat = cache.access(false, a + (w * waySize));
                assertEquals(MSG, CLEAN_MISS_LAT, lat);
            }

            // this kicks a out
            int lat = cache.access(false, a + ((WAYS + 1) * waySize));
            assertEquals(MSG, DIRTY_MISS_LAT, lat);

            // a should have gotten evicted
            lat = cache.access(false, a);
            assertEquals(MSG, DIRTY_MISS_LAT, lat);
        }


        @Test
        public void testConflictMissLoadThenStore() {
            final long a = 0xFF << (INDEX_BITS + BLOCK_BITS);
            final long waySize = (1 << INDEX_BITS) * (1 << BLOCK_BITS);
            int lat;

            // read all the lines in the set
            for (int w = 0; w < WAYS; w++) {
                lat = cache.access(true, a + (w * waySize));
                assertEquals(MSG, CLEAN_MISS_LAT, lat);
            }

            // now write to all the lines in the set
            for (int w = 0; w < WAYS; w++) {
                lat = cache.access(false, a + (w * waySize));
                assertEquals(MSG, ACCESS_LAT, lat);
            }

            // this kicks a out
            lat = cache.access(false, a + ((WAYS + 1) * waySize));
            assertEquals(MSG, DIRTY_MISS_LAT, lat);

            // a should have gotten evicted
            lat = cache.access(false, a);
            assertEquals(MSG, DIRTY_MISS_LAT, lat);
        }


        @Test
        public void testCapacityLoads() {
            final long cacheSize = (1 << INDEX_BITS) * (1 << BLOCK_BITS) * WAYS;
            final long a = 0xFF << (INDEX_BITS + BLOCK_BITS);

            for (long i = 0; i < cacheSize; i += (1 << BLOCK_BITS)) {
                int lat = cache.access(true, a + i);
                assertEquals(MSG, CLEAN_MISS_LAT, lat);
            }

            // a should still be in the cache
            int lat = cache.access(true, a);
            assertEquals(MSG, ACCESS_LAT, lat);
        }


        @Test
        public void testCapacityMissLoads() {
            final long cacheSize = (1 << INDEX_BITS) * (1 << BLOCK_BITS) * WAYS;
            final long blockSize = (1 << BLOCK_BITS);
            final long a = 0xFF << (INDEX_BITS + BLOCK_BITS);

            for (long i = 0; i < cacheSize + blockSize; i += blockSize) {
                int lat = cache.access(true, a + i);
                assertEquals(MSG, CLEAN_MISS_LAT, lat);
            }

            // a should have gotten evicted
            int lat = cache.access(true, a);
            assertEquals(MSG, CLEAN_MISS_LAT, lat);
        }


        @Test
        public void testCapacityMissStores() {
            final long cacheSize = (1 << INDEX_BITS) * (1 << BLOCK_BITS) * WAYS;
            final long blockSize = (1 << BLOCK_BITS);
            final long a = 0xFF << (INDEX_BITS + BLOCK_BITS);

            for (long i = 0; i < cacheSize; i += blockSize) {
                int lat = cache.access(false, a + i);
                assertEquals(MSG, CLEAN_MISS_LAT, lat);
            }

            // this should evict a
            int lat = cache.access(false, a + cacheSize);
            assertEquals(MSG, DIRTY_MISS_LAT, lat);

            // a should have gotten evicted
            lat = cache.access(false, a);
            assertEquals(MSG, DIRTY_MISS_LAT, lat);
        }

    }

    @RunWith(Parameterized.class)
    public static class PipelineIntegrationTests {

        @Rule
        public final Timeout globalTimeout = Timeout.seconds(2);

        private final String MSG;

        private final int INDEX_BITS;
        private final int WAYS;
        private final int BLOCK_BITS;
        private final int BLOCK_SIZE;
        private final int WAY_SIZE;
        private final IInorderPipeline pipe;
        private final ICache ic, dc;

        public PipelineIntegrationTests(int indexBits, int ways, int blockBits) {
            INDEX_BITS = indexBits;
            WAYS = ways;
            BLOCK_BITS = blockBits;
            BLOCK_SIZE = 1 << blockBits;
            WAY_SIZE = 1 << (INDEX_BITS + BLOCK_BITS);

            assert blockBits > 0;
            assert indexBits > 0;
            assert ways > 0;

            MSG = "[index bits = " + INDEX_BITS +
                    ", assoc = " + WAYS +
                    ", block offset bits = " + BLOCK_BITS + "]";

            ic = new cis501.submission.Cache(INDEX_BITS, WAYS, BLOCK_BITS,
                    ACCESS_LAT, CLEAN_MISS_LAT, DIRTY_MISS_LAT);
            dc = new cis501.submission.Cache(INDEX_BITS, WAYS, BLOCK_BITS,
                    ACCESS_LAT, CLEAN_MISS_LAT, DIRTY_MISS_LAT);
            IDirectionPredictor never = new cis501.submission.DirPredNeverTaken();
            IBranchTargetBuffer btb = new cis501.submission.BranchTargetBuffer(3);
            pipe = new cis501.submission.InorderPipeline(new BranchPredictor(never, btb), ic, dc);
        }

        /** The {index bits,ways,block offset bits} to test. */
        @Parameterized.Parameters
        public static Collection params() {
            return new CtorParams(2, 1, 2).p(2, 2, 2).p(3, 4, 2);
        }


        @Test
        public void testImiss() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInt(1, 2, 3, 0xAA, 2));
            pipe.run(new InsnIterator(insns));

            assertEquals(MSG + TestUtils.i2s(insns), 1, pipe.getInsns());
            // 123456789a
            // f..dxmw|
            assertEquals(MSG + TestUtils.i2s(insns), 6 + CLEAN_MISS_LAT, pipe.getCycles());
        }


        @Test
        public void test2Imiss() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInt(1, 2, 3, 0, BLOCK_SIZE)); // fallthru is a different cache line
            insns.add(makeInt(1, 2, 3, BLOCK_SIZE, 1 << (BLOCK_BITS + 1)));
            pipe.run(new InsnIterator(insns));

            assertEquals(MSG + TestUtils.i2s(insns), 2, pipe.getInsns());
            // 123456789abcd
            // f..dxmw   |
            //    f..dxmw|
            assertEquals(MSG + TestUtils.i2s(insns), 7 + (2 * CLEAN_MISS_LAT), pipe.getCycles());
        }


        @Test
        public void testImissIhit() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeInt(1, 2, 3, 0x0, 2));
            insns.add(makeInt(1, 2, 3, 0x2, 2));
            pipe.run(new InsnIterator(insns));

            assertEquals(MSG + TestUtils.i2s(insns), 2, pipe.getInsns());
            // 123456789abcd
            // f..dxmw |
            //    fdxmw|
            assertEquals(MSG + TestUtils.i2s(insns), 7 + CLEAN_MISS_LAT, pipe.getCycles());
        }


        @Test
        public void testManyImiss() {
            List<Insn> insns = new LinkedList<>();
            final int numInsns = 10;
            for (int i = 0; i < numInsns; i++) {
                insns.add(makeInt(1, 2, 3, i * BLOCK_SIZE, BLOCK_SIZE));
            }
            pipe.run(new InsnIterator(insns));

            assertEquals(MSG + TestUtils.i2s(insns), numInsns, pipe.getInsns());
            // 123456789abcdef
            // f..dxmw      |
            //    f..dxmw   |
            //       f..dxmw|
            assertEquals(MSG + TestUtils.i2s(insns), 5 + (numInsns * (CLEAN_MISS_LAT + 1)), pipe.getCycles());
        }


        @Test
        public void testImissDmiss() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeMem(1, 2, 3, 0x0, 2, MemoryOp.Load, 0xB));
            pipe.run(new InsnIterator(insns));

            assertEquals(MSG + TestUtils.i2s(insns), 1, pipe.getInsns());
            // 123456789a
            // f..dxm..w|
            assertEquals(MSG + TestUtils.i2s(insns), 6 + (2 * CLEAN_MISS_LAT), pipe.getCycles());
        }


        @Test
        public void testImissDmissIhitDhit() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeMem(1, 2, 3, 0x0, 2, MemoryOp.Load, 0x42));
            insns.add(makeMem(1, 2, 3, 0x2, 2, MemoryOp.Load, 0x42));
            pipe.run(new InsnIterator(insns));

            assertEquals(MSG + TestUtils.i2s(insns), 2, pipe.getInsns());
            // 123456789abcd
            // f..dxm..w |
            //    fdx  mw|
            assertEquals(MSG + TestUtils.i2s(insns), 7 + (2 * CLEAN_MISS_LAT), pipe.getCycles());
        }


        @Test
        public void testImissDmissIhitDmiss() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeMem(1, 2, 3, 0x0, 2, MemoryOp.Load, 0x42));
            insns.add(makeMem(1, 2, 3, 0x2, 2, MemoryOp.Load, 0x42 + BLOCK_SIZE));
            pipe.run(new InsnIterator(insns));

            assertEquals(MSG + TestUtils.i2s(insns), 2, pipe.getInsns());
            // 123456789abcd
            // f..dxm..w   |
            //    fdx  m..w|
            assertEquals(MSG + TestUtils.i2s(insns), 7 + (3 * CLEAN_MISS_LAT), pipe.getCycles());
        }


        @Test
        public void testImissDmissImissDhit() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeMem(1, 2, 3, 0x0, BLOCK_SIZE, MemoryOp.Load, 0x42));
            insns.add(makeMem(1, 2, 3, BLOCK_SIZE, BLOCK_SIZE, MemoryOp.Load, 0x42));
            pipe.run(new InsnIterator(insns));

            assertEquals(MSG + TestUtils.i2s(insns), 2, pipe.getInsns());
            // 123456789abcd
            // f..dxm..w |
            //    f..dxmw|
            assertEquals(MSG + TestUtils.i2s(insns), 7 + (2 * CLEAN_MISS_LAT), pipe.getCycles());
        }


        @Test
        public void testLoadUseSrc1() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeMem(1, 2, 3, 0x0, 2, MemoryOp.Load, 0x42));
            insns.add(makeInt(3, 4, 5, 0x2, 2));
            pipe.run(new InsnIterator(insns));

            assertEquals(MSG + TestUtils.i2s(insns), 2, pipe.getInsns());
            // 123456789abcd
            // f..dxm..w  |
            //    fd   xmw|
            assertEquals(MSG + TestUtils.i2s(insns), 7 + (2 * CLEAN_MISS_LAT) + 1/*lu*/, pipe.getCycles());
        }


        @Test
        public void testLoadUseSrc2() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeMem(1, 2, 3, 0x0, 2, MemoryOp.Load, 0x42));
            insns.add(makeInt(4, 3, 5, 0x2, 2));
            pipe.run(new InsnIterator(insns));

            assertEquals(MSG + TestUtils.i2s(insns), 2, pipe.getInsns());
            // 123456789abcd
            // f..dxm..w  |
            //    fd   xmw|
            assertEquals(MSG + TestUtils.i2s(insns), 7 + (2 * CLEAN_MISS_LAT) + 1/*lu*/, pipe.getCycles());
        }


        @Test
        public void testLoadUseStoreAddr() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeMem(1, 2, 3, 0x0, 2, MemoryOp.Load, 0x42));
            insns.add(makeMem(4, 3, 5, 0x2, 2, MemoryOp.Store, 0x42));
            pipe.run(new InsnIterator(insns));

            assertEquals(MSG + TestUtils.i2s(insns), 2, pipe.getInsns());
            // 123456789abcd
            // f..dxm..w  |
            //    fd   xmw|
            assertEquals(MSG + TestUtils.i2s(insns), 7 + (2 * CLEAN_MISS_LAT) + 1/*lu*/, pipe.getCycles());
        }


        @Test
        public void testLoadUseStoreVal() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeMem(1, 2, 3, 0x0, 2, MemoryOp.Load, 0x42));
            insns.add(makeMem(3, 4, 5, 0x2, 2, MemoryOp.Store, 0x42));

            pipe.run(new InsnIterator(insns));

            assertEquals(MSG + TestUtils.i2s(insns), 2, pipe.getInsns());
            // 0123456789abcd
            // f..dxm..w |
            //    fd  xmw|
            assertEquals(MSG + TestUtils.i2s(insns), 7 + (2 * CLEAN_MISS_LAT), pipe.getCycles());
        }


        @Test
        public void testEvictDirty() {
            List<Insn> insns = new LinkedList<>();

            // loop: first access is i$/d$ clean miss, middle accesses are d$ clean misses, last access is a d$ dirty miss
            for (int i = 0; i <= WAYS; i++) {
                final long dataAddr = WAY_SIZE * i;
                insns.add(makeMem(1, 2, 3, 0x0, 0/*self-loop*/, MemoryOp.Store, dataAddr));
            }
            insns.add(makeMem(1, 2, 3, 0x0, 2, MemoryOp.Store, 0x0)); // d$ dirty miss

            pipe.run(new InsnIterator(insns));

            assertEquals(MSG + TestUtils.i2s(insns), 2 + WAYS, pipe.getInsns());
            // 123456789abcdefghi
            // f..dxm..w        |
            //    fd  xm...w    |
            //     fd  x   m...w|
            // 1 => 18
            // 2 => 21
            assertEquals(MSG + TestUtils.i2s(insns), 10 + ((WAYS - 1) * (CLEAN_MISS_LAT + 1)) + (2 * (DIRTY_MISS_LAT + 1)), pipe.getCycles());
        }


        @Test
        public void testEvictLoadDirty() {
            List<Insn> insns = new LinkedList<>();

            insns.add(makeMem(1, 2, 3, 0x0, 0/*self-loop*/, MemoryOp.Load, 0x0)); // i$ & d$ clean misses
            // loop: first access is i$/d$ hit, middle accesses are d$ clean misses, last access is a d$ dirty miss
            for (int i = 0; i <= WAYS; i++) {
                final long dataAddr = WAY_SIZE * i;
                insns.add(makeMem(1, 2, 3, 0x0, 0/*self-loop*/, MemoryOp.Store, dataAddr));
            }
            insns.add(makeMem(1, 2, 3, 0x0, 2, MemoryOp.Store, 0x0)); // d$ dirty miss

            pipe.run(new InsnIterator(insns));

            assertEquals(MSG + TestUtils.i2s(insns), 3 + WAYS, pipe.getInsns());
            // 123456789abcdefghij
            // f..dxm..w         |
            //    fdx  mw        |
            //     fd  xm...w    |
            //      f  dx   m...w|
            // 1 => 19
            assertEquals(MSG + TestUtils.i2s(insns), 11 + ((WAYS - 1) * (CLEAN_MISS_LAT + 1)) + (2 * (DIRTY_MISS_LAT + 1)), pipe.getCycles());
        }


        @Test
        public void testBranchMispredImiss() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeBr(0x2, Direction.Taken, 2, 0x42));
            insns.add(makeInt(1, 2, 3, 0x42, 2));
            pipe.run(new InsnIterator(insns));

            assertEquals(MSG + TestUtils.i2s(insns), 2, pipe.getInsns());
            // 123456789abcd
            // f..dxmw     |
            //      f..dxmw|
            assertEquals(MSG + TestUtils.i2s(insns), 7 + (2 * CLEAN_MISS_LAT) + 2/*br mispred*/, pipe.getCycles());
        }

    }

}
