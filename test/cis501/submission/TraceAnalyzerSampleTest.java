package cis501.submission;

import cis501.ITraceAnalyzer;
import cis501.InsnIterator;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TraceAnalyzerSampleTest {

    private static ITraceAnalyzer subm = new TraceAnalyzer();

    @BeforeClass
    public static void setUpClass() throws Exception {
        // Run the trace before any tests are run. Then, test the results of the run.
        String traceFilePath = "./test/resources/cis501/submission/test-trace-analyzer.trace.gz";
        InsnIterator uiter = new InsnIterator(traceFilePath, -1);
        subm.run(uiter);
    }

    /** Simple do-nothing test to verify that the test suite is being run. */
    @Test
    public void testNop() {
        assertTrue(true);
    }

    /** The trace's actual average insn size, so you can check your implementation. */
    @Test
    public void testAvgInsnSize() {
        assertEquals(2.60, subm.avgInsnSize(), 0.01);
    }

    @Test
    public void testInsnBandwidthIncreaseWithoutThumb() {
        assertEquals(1.54, subm.insnBandwidthIncreaseWithoutThumb(), 0.01);
    }

    @Test
    public void testFractionOfDirectBranchOffsetsLteNBits() {
        assertEquals(0, subm.fractionOfDirectBranchOffsetsLteNBits(1), 0.01);
        assertEquals(0, subm.fractionOfDirectBranchOffsetsLteNBits(2), 0.01);
        assertEquals(1, subm.fractionOfDirectBranchOffsetsLteNBits(32), 0.01);

        assertEquals(1.0, subm.fractionOfDirectBranchOffsetsLteNBits(5), 0.01);
    }

    @Test
    public void testMostCommonInsnCategory() {
        assertEquals("other", subm.mostCommonInsnCategory());
    }

    // add more tests here!

}
