package cis501.submission;

import cis501.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CacheTraceTest {

    // TODO: replace the path of trace file here
    private static final String TRACE_FILE = "./test/resources/cis501/submission/streamcluster-10M-v2.trace.gz";

    @Test
    public void test_DirectMappedCache_BimodalPredictor_5KTrace() {
        IDirectionPredictor bimodal = new DirPredBimodal(10);
        IBranchTargetBuffer btb = new BranchTargetBuffer(10);
        BranchPredictor bp = new BranchPredictor(bimodal, btb);
        ICache insnCache = new Cache(10, 1, 2, 0, 2, 3);
        ICache dataCache = new Cache(10, 1, 2, 0, 2, 3);
        InorderPipeline sim = new InorderPipeline(bp, insnCache, dataCache);
        InsnIterator uiter = new InsnIterator(TRACE_FILE, 5000);
        sim.run(uiter);
        assertEquals(5000, sim.getInsns());
        assertEquals(7628, sim.getCycles());
    }

    @Test
    public void test_2WaysCache_BimodalPredictor_5KTrace() {
        IDirectionPredictor bimodal = new DirPredBimodal(10);
        IBranchTargetBuffer btb = new BranchTargetBuffer(10);
        BranchPredictor bp = new BranchPredictor(bimodal, btb);
        ICache insnCache = new Cache(10, 2, 2, 0, 2, 3);
        ICache dataCache = new Cache(10, 2, 2, 0, 2, 3);
        InorderPipeline sim = new InorderPipeline(bp, insnCache, dataCache);
        InsnIterator uiter = new InsnIterator(TRACE_FILE, 5000);
        sim.run(uiter);
        assertEquals(5000, sim.getInsns());
        assertEquals(6900, sim.getCycles());
    }

    @Test
    public void test_DirectMappedCache_NeverTakenPredictor_5KTrace() {
        IDirectionPredictor never = new DirPredNeverTaken();
        IBranchTargetBuffer btb = new BranchTargetBuffer(10);
        BranchPredictor bp = new BranchPredictor(never, btb);
        ICache insnCache = new Cache(10, 1, 2, 0, 2, 3);
        ICache dataCache = new Cache(10, 1, 2, 0, 2, 3);
        InorderPipeline sim = new InorderPipeline(bp, insnCache, dataCache);
        InsnIterator uiter = new InsnIterator(TRACE_FILE, 5000);
        sim.run(uiter);
        assertEquals(5000, sim.getInsns());
        assertEquals(8132, sim.getCycles());
    }

    @Test
    public void test_2WaysCache_NeverTakenPredictor_5KTrace() {
        IDirectionPredictor never = new DirPredNeverTaken();
        IBranchTargetBuffer btb = new BranchTargetBuffer(10);
        BranchPredictor bp = new BranchPredictor(never, btb);
        ICache insnCache = new Cache(10, 2, 2, 0, 2, 3);
        ICache dataCache = new Cache(10, 2, 2, 0, 2, 3);
        InorderPipeline sim = new InorderPipeline(bp, insnCache, dataCache);
        InsnIterator uiter = new InsnIterator(TRACE_FILE, 5000);
        sim.run(uiter);
        assertEquals(5000, sim.getInsns());
        assertEquals(7404, sim.getCycles());
    }

    /* Using Branch Predictor trace tests: total 3*/
    @Test
    public void test_BimodalPredictor_5KTrace() {
        IDirectionPredictor bimodal = new DirPredBimodal(5);
        IBranchTargetBuffer btb = new BranchTargetBuffer(5);
        BranchPredictor bp = new BranchPredictor(bimodal, btb);
        ICache dataCache = new Cache(10, 1, 2, 1, 1, 1);
        InorderPipeline sim = new InorderPipeline(bp, null, dataCache);
        InsnIterator uiter = new InsnIterator(TRACE_FILE, 5000);
        sim.run(uiter);
        assertEquals(5000, sim.getInsns());
        assertEquals(7227, sim.getCycles());
    }

    @Test
    public void test_GSharePredictor_5KTrace() {
        IDirectionPredictor gshare = new DirPredGshare(5, 5);
        IBranchTargetBuffer btb = new BranchTargetBuffer(5);
        BranchPredictor bp = new BranchPredictor(gshare, btb);
        ICache dataCache = new Cache(10, 1, 2, 1, 1, 1);
        InorderPipeline sim = new InorderPipeline(bp, null, dataCache);
        InsnIterator uiter = new InsnIterator(TRACE_FILE, 5000);
        sim.run(uiter);
        assertEquals(5000, sim.getInsns());
        assertEquals(7247, sim.getCycles());
    }

    @Test
    public void test_NeverTakenPredictor_5KTrace() {
        IDirectionPredictor neverTaken = new DirPredNeverTaken();
        IBranchTargetBuffer btb = new BranchTargetBuffer(5);
        BranchPredictor bp = new BranchPredictor(neverTaken, btb);
        ICache dataCache = new Cache(10, 1, 2, 1, 1, 1);
        InorderPipeline sim = new InorderPipeline(bp, null, dataCache);
        InsnIterator uiter = new InsnIterator(TRACE_FILE, 5000);
        sim.run(uiter);
        assertEquals(5000, sim.getInsns());
        assertEquals(7562, sim.getCycles());
    }


//    @Test
//    public void test_FullTrace() {
//        IDirectionPredictor gshare = new DirPredGshare(18, 18);
//        IBranchTargetBuffer btb = new BranchTargetBuffer(18);
//        BranchPredictor bp = new BranchPredictor(gshare, btb);
//        double[][] ipc = new double[5][10];
//        int cacheSize, blockSize, ways;
//        int blockOffsetBits, indexBits;
//        blockSize = 32; blockOffsetBits = 5;
//        for (int i = 0; i <= 4; i++) {
//            ways = 1 << i;
//            System.out.println("Ways is: " + ways);
//            for (int j = 9; j <= 18; j++) {
//                cacheSize = 1 << j;
//                System.out.println("cache size is: " + cacheSize);
//                indexBits = j - i - blockOffsetBits;
//                System.out.println("cache size is: " + (1 << (indexBits))*ways*32);
//                System.out.println("indexBits are: " + indexBits);
//                ICache insnCache = new Cache(indexBits, ways, blockOffsetBits, 0, 2, 3);
//                ICache dataCache = new Cache(indexBits, ways, blockOffsetBits, 0, 2, 3);
//                InorderPipeline sim = new InorderPipeline(bp, insnCache, dataCache);
//                InsnIterator uiter = new InsnIterator(TRACE_FILE, -1);
//                sim.run(uiter);
//                ipc[i][j-9] = 1.0 * sim.getInsns() / sim.getCycles();
//            }
//        }
//        System.out.println("Reached the end!");
//        // Print the ipc values
//        for (int i = 0; i <= 4; i++) {
//            System.out.println("associativity: " + (1 << i));
//            for (int j = 0; j < 10; j++) {
//                System.out.println(ipc[i][j]);
//            }
//        }
//    }

    /*
    * Q: Your graph should show that all the caches have basically the same performance at large cache sizes.
    *       Why does this convergence occur?
    * A: For the same block size and cache capacity, the number of unique indexes will reduce with increasing associativity,
    *       but at the same time, we can accommodate more cache entries for the same index.  In other words, increased
    *       associativity can capture the entropy in higher order bits of the address, and helps reduce conflict misses
    *       for a reasonable sized program (that exhibits temporal locality).
    *
    *       But as we increase the cache size, our lower associative caches will have enough indexes to capture the entropy
    *       in higher order bits, and therefore will obviate the issue of conflict misses implicitly by indexing them to
    *       different cache rows.  Therefore we get the same performance for different associative caches for large cache sizes.
    *
    *       Also note that the size of the program also influences the convergence calculations.
    *       If the memory adddresses that the program is trying to access have entropy in the highest order bits,
    *       then the convergence will be quite slow, and we would need large cache size to achieve that.
    *
    *       Also note that we do not take into account the fact that as we increase the cache size, in reality, the latency
    *       will increase and that will also affect our calculations.  Same is the case with associativity: higher associativity
    *       implies additional tag-matching, etc.
    * */

}
