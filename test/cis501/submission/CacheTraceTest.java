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
    public void test_DirectMappedCache_NeverTaken5KTrace() {
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
    public void test_2WaysCache_NeverTaken5KTrace() {
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


//    @Test
//    public void testFullTraceBimodal() {
//        IDirectionPredictor bimodal;
//        IBranchTargetBuffer btb;
//        InorderPipeline sim_bimodal;
//        InsnIterator uiter;
//
//        for (int indexBits = 4; indexBits <= 18; indexBits++) {
//            bimodal = new DirPredBimodal(indexBits);
//            btb = new BranchTargetBuffer(indexBits);
//            sim_bimodal = new InorderPipeline(1, new BranchPredictor(bimodal, btb));
//            uiter = new InsnIterator(TRACE_FILE, -1);
//            sim_bimodal.run(uiter);
//            //System.out.print(sim_bimodal.getCycles()  + "\t");
//            System.out.println(1.0 * sim_bimodal.getInsns() / sim_bimodal.getCycles());
//        }
//    }
//

}
