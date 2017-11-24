package cis501.submission;

import cis501.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OOOPipelineTraceTest {

    // TODO: replace the path of trace file here
    private static final String TRACE_FILE = "./test/resources/cis501/submission/streamcluster-10M-v2.trace.gz";

    @Test
    public void test_NoCache_40PRegs_5KTrace() {
        OOOPipeline sim = new OOOPipeline(40, 10, 5, null, null);
        InsnIterator uiter = new InsnIterator(TRACE_FILE, 5000);
        sim.run(uiter);
        assertEquals(5000, sim.getInsns());
        assertEquals(5982, sim.getCycles());
    }

    @Test
    public void test_NoCache_19PRegs_5KTrace() {
        OOOPipeline sim = new OOOPipeline(19, 10, 5, null, null);
        InsnIterator uiter = new InsnIterator(TRACE_FILE, 5000);
        sim.run(uiter);
        assertEquals(5000, sim.getInsns());
        assertEquals(15924, sim.getCycles());
    }

    @Test
    public void test_WithCache_40PRegs_5KTrace() {
        ICache insnCache = new Cache(10, 1, 4, 0, 2, 3);
        ICache dataCache = new Cache(10, 1, 4, 0, 2, 3);
        OOOPipeline sim = new OOOPipeline(40, 10, 5, insnCache, dataCache);
        InsnIterator uiter = new InsnIterator(TRACE_FILE, 5000);
        sim.run(uiter);
        assertEquals(5000, sim.getInsns());
        assertEquals(6387, sim.getCycles());
    }

    @Test
    public void test_WithCache_19PRegs_5KTrace() {
        ICache insnCache = new Cache(10, 1, 4, 0, 2, 3);
        ICache dataCache = new Cache(10, 1, 4, 0, 2, 3);
        OOOPipeline sim = new OOOPipeline(19, 10, 5, insnCache, dataCache);
        InsnIterator uiter = new InsnIterator(TRACE_FILE, 5000);
        sim.run(uiter);
        assertEquals(5000, sim.getInsns());
        assertEquals(16084, sim.getCycles());
    }

    //Full Trace Results
//    @Test
//    public void test_FUllTrace() {
//        int[] robSizeList = new int[] {10, 15, 20};
//        int[] iqSizeList = new int[] {2, 5, 10};
//
//        for (int i = 0; i <= 2; i++) {
//            for (int j = 0; j <= 2; j++) {
//                ICache insnCache = new Cache(7, 4, 6, 0, 2, 3);
//                ICache dataCache = new Cache(7, 4, 6, 0, 2, 3);
//                OOOPipeline sim = new OOOPipeline(50, robSizeList[i], iqSizeList[j], insnCache, dataCache);
//                InsnIterator uiter = new InsnIterator(TRACE_FILE, -1);
//                sim.run(uiter);
//                System.out.println("Config: (a) robSize: " + robSizeList[i] + ", (b) iqSize: "+ iqSizeList[j] + "IPC: " + 1.0 * sim.getInsns()/sim.getCycles());
//            }
//        }
//    }

    /*
    * Q: The performance benefits of increasing the number of ROB/Issue Queue entries provide
    *       rapidly diminishing returns. Why might this be?
    * A: When ROB entries are not enough to accommodate enough insns to fill up the pipeline, we
    *       lose out on performance since some of the stages in our pipeline are empty while they
    *       can be utilized if not for the limited ROB size. Once ROB size is large enough to
    *       accommodate all the insns that can be present in the pipeline (along with IQ), we are at
    *       peak performance, and increasing ROB size does not help anymore.
    *
    *       Similarly, when issue queue entries are not enough to pool insns that are independent
    *       and can use the backend out-of-order structure of our pipeline, we lose out on performance.
    *       Once we have enough entries in IQ, we don't gain much by adding more.
    * */

}
