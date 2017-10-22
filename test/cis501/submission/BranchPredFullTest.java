package cis501.submission;

import cis501.*;
import org.junit.Before;
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
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class BranchPredFullTest {

    private static final int BMISPRED_LAT = 2;

    private static Insn makeBr(long pc, Direction dir, long targetPC) {
        return new Insn(-1, -1, -1, pc, 4, dir, targetPC, null, null, 0, 0, "<branch>");
    }

    private static Insn makeBrSrcReg(long pc, Direction dir, long targetPC, int src1) {
        return new Insn(-1, src1, -1, pc, 4, dir, targetPC, null, null, 0, 0, "<branch>");
    }

    private static Insn makeOp(int dst, int src1, int src2, MemoryOp mop, long pc) {
        return new Insn(dst, src1, src2, pc, 4, null, 0, null, mop, 0, 1, "<op>");
    }

    @RunWith(Parameterized.class)
    public static class BTBTests {

        @Rule
        public final Timeout globalTimeout = Timeout.seconds(2);
        private final int INDEX_BITS;
        private final String BTB_MSG;
        private final IBranchTargetBuffer btb;

        public BTBTests(int idxBits) {
            INDEX_BITS = idxBits;
            BTB_MSG = "[index bits = " + INDEX_BITS + "]";
            btb = new cis501.submission.BranchTargetBuffer(idxBits);
        }

        /** The btb sizes (in index bits) to test. */
        @Parameterized.Parameters
        public static Collection indexBits() {
            return new CtorParams(2).p(4).p(8);
        }

        @Test
        public void testInitialState() {
            assertEquals(BTB_MSG, 0, btb.predict(0));
        }

        @Test
        public void testRemainderIndexing() {
            btb.train(-1, 42);
            assertEquals(BTB_MSG, 42, btb.predict(-1));
        }


        @Test
        public void testNewTarget() {
            btb.train(0, 42);
            assertEquals(BTB_MSG, 42, btb.predict(0));
        }


        @Test
        public void testAlias() {
            btb.train(0, 42);
            assertEquals(BTB_MSG, 42, btb.predict(0));
            long alias0 = (long) Math.pow(2, INDEX_BITS);
            btb.train(alias0, 100);
            assertEquals(BTB_MSG, 0, btb.predict(0)); // tag doesn't match
            assertEquals(BTB_MSG, 100, btb.predict(alias0)); // tag match
        }


        @Test
        public void testUnalias() {
            btb.train(0, 42);
            assertEquals(BTB_MSG, 42, btb.predict(0));

            // only one entry should be set to 42
            for (int i = 1; i < (long) Math.pow(2, INDEX_BITS); i++) {
                assertEquals(BTB_MSG, 0, btb.predict(i));
            }
        }
    }

    @RunWith(Parameterized.class)
    public static class BimodalTests {

        @Rule
        public final Timeout globalTimeout = Timeout.seconds(2);
        private final int INDEX_BITS;
        private final String IB_MSG;
        private final IDirectionPredictor bp;

        public BimodalTests(int idxBits) {
            INDEX_BITS = idxBits;
            IB_MSG = "[index bits = " + INDEX_BITS + "]";
            bp = new cis501.submission.DirPredBimodal(idxBits);
        }

        /** The bimodal predictor sizes (in index bits) to test. */
        @Parameterized.Parameters
        public static Collection indexBits() {
            return new CtorParams(2).p(4).p(8);
        }


        @Test
        public void testInitialState() {
            assertEquals(IB_MSG, Direction.NotTaken, bp.predict(0));
        }


        @Test
        public void testRemainderIndexing() {
            assertEquals(IB_MSG, Direction.NotTaken, bp.predict(-1));
        }


        @Test
        public void testTaken() {
            bp.train(0, Direction.Taken);
            assertEquals(IB_MSG, Direction.NotTaken, bp.predict(0));
            bp.train(0, Direction.Taken);
            assertEquals(IB_MSG, Direction.Taken, bp.predict(0));
        }


        @Test
        public void testTakenSaturation() {
            for (int i = 0; i < 10; i++) {
                bp.train(0, Direction.Taken);
            }
            bp.train(0, Direction.NotTaken);
            bp.train(0, Direction.NotTaken);
            assertEquals(IB_MSG, Direction.NotTaken, bp.predict(0));
        }


        @Test
        public void testNotTakenSaturation() {
            for (int i = 0; i < 10; i++) {
                bp.train(0, Direction.NotTaken);
            }
            bp.train(0, Direction.Taken);
            bp.train(0, Direction.Taken);
            assertEquals(IB_MSG, Direction.Taken, bp.predict(0));
        }


        @Test
        public void testAlias() {
            bp.train(0, Direction.Taken);
            assertEquals(IB_MSG, Direction.NotTaken, bp.predict(0));
            bp.train((long) Math.pow(2, INDEX_BITS), Direction.Taken);
            assertEquals(IB_MSG, Direction.Taken, bp.predict(0));
        }


        @Test
        public void testUnalias() {
            bp.train(0, Direction.Taken);
            bp.train(0, Direction.Taken);
            assertEquals(IB_MSG, Direction.Taken, bp.predict(0));

            // only one counter should be set to t
            for (int i = 1; i < (long) Math.pow(2, INDEX_BITS); i++) {
                assertEquals(IB_MSG, Direction.NotTaken, bp.predict(i));
            }
        }
    }

    @RunWith(Parameterized.class)
    public static class GshareNoHistoryTests {

        @Rule
        public final Timeout globalTimeout = Timeout.seconds(2);
        private final int INDEX_BITS;
        private final int HIST_BITS = 0;
        private final String GS_MSG;
        private final IDirectionPredictor bp;

        public GshareNoHistoryTests(int idxBits) {
            INDEX_BITS = idxBits;
            GS_MSG = "[index bits = " + INDEX_BITS + ", history bits = " + HIST_BITS + "]";
            bp = new cis501.submission.DirPredGshare(INDEX_BITS, HIST_BITS);
        }

        /** The gshare predictor sizes to test. */
        @Parameterized.Parameters
        public static Collection indexBits() {
            return new CtorParams(2).p(4).p(8);
        }


        @Test
        public void testInitialState() {
            assertEquals(GS_MSG, Direction.NotTaken, bp.predict(0));
        }


        @Test
        public void testRemainderIndexing() {
            assertEquals(GS_MSG, Direction.NotTaken, bp.predict(-1));
        }


        @Test
        public void testTaken() {
            bp.train(0, Direction.Taken);
            assertEquals(GS_MSG, Direction.NotTaken, bp.predict(0));
            bp.train(0, Direction.Taken);
            assertEquals(GS_MSG, Direction.Taken, bp.predict(0));
        }


        @Test
        public void testTakenSaturation() {
            for (int i = 0; i < 10; i++) {
                bp.train(0, Direction.Taken);
            }
            bp.train(0, Direction.NotTaken);
            bp.train(0, Direction.NotTaken);
            assertEquals(GS_MSG, Direction.NotTaken, bp.predict(0));
        }


        @Test
        public void testNotTakenSaturation() {
            for (int i = 0; i < 10; i++) {
                bp.train(0, Direction.NotTaken);
            }
            bp.train(0, Direction.Taken);
            bp.train(0, Direction.Taken);
            assertEquals(GS_MSG, Direction.Taken, bp.predict(0));
        }


        @Test
        public void testAlias() {
            bp.train(0, Direction.Taken);
            assertEquals(GS_MSG, Direction.NotTaken, bp.predict(0));
            bp.train((long) Math.pow(2, INDEX_BITS), Direction.Taken);
            assertEquals(GS_MSG, Direction.Taken, bp.predict(0));
        }


        @Test
        public void testUnalias() {
            bp.train(0, Direction.Taken);
            bp.train(0, Direction.Taken);
            assertEquals(GS_MSG, Direction.Taken, bp.predict(0));

            // only one counter should be set to t
            for (int i = 1; i < (int) Math.pow(2, INDEX_BITS); i++) {
                assertEquals(GS_MSG, Direction.NotTaken, bp.predict(i));
            }
        }

    }

    @RunWith(Parameterized.class)
    public static class GshareHistoryTests {

        @Rule
        public final Timeout globalTimeout = Timeout.seconds(2);

        private final int INDEX_BITS;
        private final int HIST_BITS;
        private final int HIST_BITS_MASK;
        private final String GS_MSG;

        private final IDirectionPredictor bp;
        private int history = 0;

        public GshareHistoryTests(int idxBits, int histBits) {
            INDEX_BITS = idxBits;
            HIST_BITS = histBits;
            HIST_BITS_MASK = (1 << histBits) - 1;
            GS_MSG = "[index bits = " + INDEX_BITS + ", history bits = " + HIST_BITS + "]";
            bp = new cis501.submission.DirPredGshare(INDEX_BITS, HIST_BITS);
        }

        /** The gshare predictor and history sizes to test. */
        @Parameterized.Parameters
        public static Collection indexHistoryBits() {
            return new CtorParams(2, 2).p(4, 2).p(4, 3).p(4, 4).p(8, 2).p(8, 3);
        }


        @Test
        public void testInitialState() {
            assertEquals(GS_MSG, Direction.NotTaken, bp.predict(0));
        }

        /**
         * Train the counter at index i with direction dir. Takes history into account to synthesize
         * an address that will map to i.
         */
        private void trainIndex(int i, Direction dir) {
            bp.train(i ^ history, dir);
            history <<= 1;
            if (Direction.Taken == dir) {
                history |= 1;
            }
            history &= HIST_BITS_MASK;
        }

        private Direction predictIndex(int i) {
            return bp.predict(i ^ history);
        }


        @Test
        public void testTaken() {
            trainIndex(0, Direction.Taken);
            trainIndex(0, Direction.Taken);
            assertEquals(GS_MSG, predictIndex(0), Direction.Taken);
        }


        @Test
        public void testTakenSaturation() {
            for (int i = 0; i < 10; i++) {
                trainIndex(0, Direction.Taken);
            }
            trainIndex(0, Direction.NotTaken);
            trainIndex(0, Direction.NotTaken);
            assertEquals(GS_MSG, Direction.NotTaken, predictIndex(0));
        }


        @Test
        public void testNotTakenSaturation() {
            for (int i = 0; i < 10; i++) {
                trainIndex(0, Direction.NotTaken);
            }
            trainIndex(0, Direction.Taken);
            trainIndex(0, Direction.Taken);
            assertEquals(GS_MSG, Direction.Taken, predictIndex(0));
        }


        @Test
        public void testAlias() {
            trainIndex(0, Direction.Taken);
            assertEquals(GS_MSG, Direction.NotTaken, predictIndex(0));
            trainIndex((int) Math.pow(2, INDEX_BITS), Direction.Taken);
            assertEquals(GS_MSG, Direction.Taken, predictIndex(0));
        }


        @Test
        public void testUnalias() {
            trainIndex(0, Direction.Taken);
            trainIndex(0, Direction.Taken);
            assertEquals(GS_MSG, Direction.Taken, predictIndex(0));

            // only one counter should be set to t
            for (int i = 1; i < (int) Math.pow(2, INDEX_BITS); i++) {
                assertEquals(GS_MSG, Direction.NotTaken, predictIndex(i));
            }
        }

    }

    public static class TournamentBimodalTests {

        private static IDirectionPredictor bmNT;
        private static IDirectionPredictor bmT;
        private static IDirectionPredictor tournament;
        @Rule
        public final Timeout globalTimeout = Timeout.seconds(2);
        private final String MSG = "[tournament, chooser bits = 2, T sub-predictor: bimodal index bits = 2, NT sub-predictor: bimodal index bits = 2]";

        @Before
        public void setup() {
            final int indexBits = 2;
            bmNT = new cis501.submission.DirPredBimodal(indexBits);
            bmT = new cis501.submission.DirPredBimodal(indexBits);
            tournament = new cis501.submission.DirPredTournament(indexBits, bmNT, bmT);
        }

        /** Ensure that both sub-predictors get trained. */

        @Test
        public void testTrainBoth() {
            tournament.train(0, Direction.Taken);
            tournament.train(0, Direction.Taken);

            assertEquals(MSG, Direction.Taken, tournament.predict(0));
            assertEquals(MSG, Direction.Taken, bmNT.predict(0));
            assertEquals(MSG, Direction.Taken, bmT.predict(0));
        }

        /** Ensure that chooser doesn't get trained when sub-predictors agree. */

        @Test
        public void testChooserUnchangedWhenSubpredictorsAgree() {
            // moves both bimodals to t, chooser is still at N
            tournament.train(0, Direction.Taken);
            tournament.train(0, Direction.Taken);

            bmNT.train(0, Direction.NotTaken); // moves bmNT to n
            assertEquals(MSG, Direction.NotTaken, tournament.predict(0)); // should use bmNT
        }

    }

    @RunWith(Parameterized.class)
    public static class TournamentAlwaysNeverTests {

        @Rule
        public final Timeout globalTimeout = Timeout.seconds(2);

        private final int CHOOSER_INDEX_BITS;
        private final String MSG;

        /**
         * selects NT when chooser is NT, and T when chooser is T. Turns the Tournament predictor
         * into a bimodal predictor.
         */
        private final IDirectionPredictor bp;

        public TournamentAlwaysNeverTests(int chooserIndexBits) {
            CHOOSER_INDEX_BITS = chooserIndexBits;
            MSG = "[chooser index bits = " + CHOOSER_INDEX_BITS + ", T sub-predictor: always, NT sub-predictor: never]";
            IDirectionPredictor never = new cis501.submission.DirPredNeverTaken();
            IDirectionPredictor always = new cis501.submission.DirPredAlwaysTaken();
            bp = new cis501.submission.DirPredTournament(CHOOSER_INDEX_BITS, never/*NT*/, always/*T*/);
        }

        /** The chooser table sizes to test. */
        @Parameterized.Parameters
        public static Collection indexBits() {
            return new CtorParams(2).p(4).p(8);
        }


        @Test
        public void testInitialState() {
            assertEquals(MSG, Direction.NotTaken, bp.predict(0));
        }


        @Test
        public void testRemainderIndexing() {
            assertEquals(MSG, Direction.NotTaken, bp.predict(-1));
            bp.train(-1, Direction.NotTaken);
        }


        @Test
        public void testTaken() {
            bp.train(0, Direction.Taken);
            assertEquals(MSG, Direction.NotTaken, bp.predict(0));
            bp.train(0, Direction.Taken);
            assertEquals(MSG, Direction.Taken, bp.predict(0));
        }


        @Test
        public void testTakenSaturation() {
            for (int i = 0; i < 10; i++) {
                bp.train(0, Direction.Taken);
            }
            bp.train(0, Direction.NotTaken);
            bp.train(0, Direction.NotTaken);
            assertEquals(MSG, Direction.NotTaken, bp.predict(0));
        }


        @Test
        public void testNotTakenSaturation() {
            for (int i = 0; i < 10; i++) {
                bp.train(0, Direction.NotTaken);
            }
            bp.train(0, Direction.Taken);
            bp.train(0, Direction.Taken);
            assertEquals(MSG, Direction.Taken, bp.predict(0));
        }


        @Test
        public void testAlias() {
            bp.train(0, Direction.Taken);
            assertEquals(MSG, Direction.NotTaken, bp.predict(0));
            bp.train((long) Math.pow(2, CHOOSER_INDEX_BITS), Direction.Taken);
            assertEquals(MSG, Direction.Taken, bp.predict(0));
        }


        @Test
        public void testUnalias() {
            bp.train(0, Direction.Taken);
            bp.train(0, Direction.Taken);
            assertEquals(MSG, Direction.Taken, bp.predict(0));

            // only one counter should be set to t
            for (int i = 1; i < (long) Math.pow(2, CHOOSER_INDEX_BITS); i++) {
                assertEquals(MSG, Direction.NotTaken, bp.predict(i));
            }
        }

    }

    @RunWith(Parameterized.class)
    public static class PipelineIntegrationTests {

        @Rule
        public final Timeout globalTimeout = Timeout.seconds(2);

        private final int ADDL_MEM_LAT;
        private final String MSG;

        private final IInorderPipeline pipe;
        /** Keep btb accessible to tests, if they need to fiddle with its state. */
        private final IBranchTargetBuffer btb;

        public PipelineIntegrationTests(int memLat) {
            ADDL_MEM_LAT = memLat;
            MSG = "[mem lat = " + ADDL_MEM_LAT + "][never btb index bits = 3]";

            IDirectionPredictor never = new cis501.submission.DirPredNeverTaken();
            btb = new cis501.submission.BranchTargetBuffer(3);
            pipe = new cis501.submission.InorderPipeline(ADDL_MEM_LAT, new BranchPredictor(never, btb));
        }

        /** The memory latencies to test. */
        @Parameterized.Parameters
        public static Collection memLat() {
            return new CtorParams(0).p(1).p(2);
        }


        @Test
        public void testCorrectPred() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeBr(0, Direction.NotTaken, 40));
            insns.add(makeBr(4, Direction.NotTaken, 40));
            pipe.run(new InsnIterator(insns));

            assertEquals(MSG + TestUtils.i2s(insns), 2, pipe.getInsns());
            // 123456789
            // fdxmw |
            //  fdxmw|
            assertEquals(MSG + TestUtils.i2s(insns), 7, pipe.getCycles());
        }


        @Test
        public void testMispredicted() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeBr(0, Direction.Taken, 40));  // mispredicted
            insns.add(makeBr(40, Direction.NotTaken, 60));
            pipe.run(new InsnIterator(insns));

            assertEquals(MSG + TestUtils.i2s(insns), 2, pipe.getInsns());
            // 123456789
            // fdxmw   |
            //  ..fdxmw|
            assertEquals(MSG + TestUtils.i2s(insns), 7 + BMISPRED_LAT, pipe.getCycles());
        }


        @Test
        public void test2Mispredicted() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeBr(0, Direction.Taken, 40));  // mispredicted
            insns.add(makeBr(40, Direction.Taken, 60));  // mispredicted
            insns.add(makeBr(60, Direction.NotTaken, 80));
            pipe.run(new InsnIterator(insns));

            assertEquals(MSG + TestUtils.i2s(insns), 3, pipe.getInsns());
            // 123456789abcd
            // fdxmw      |
            //  ..fdxmw   |
            //   ....fdxmw|
            assertEquals(MSG + TestUtils.i2s(insns), 8 + (BMISPRED_LAT * 2), pipe.getCycles());
        }


        @Test
        public void testMemOp() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeBr(0, Direction.Taken, 40));  // mispredicted
            insns.add(makeOp(3, 1, 2, MemoryOp.Load, 40));
            pipe.run(new InsnIterator(insns));

            assertEquals(MSG + TestUtils.i2s(insns), 2, pipe.getInsns());
            // 123456789abc
            // fdxmw      |
            //  ..fdxmmmmw|
            assertEquals(MSG + TestUtils.i2s(insns), 7 + BMISPRED_LAT + ADDL_MEM_LAT, pipe.getCycles());
        }


        @Test
        public void testBranchMem() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeBr(0, Direction.Taken, 40)); // mispredicted
            insns.add(makeOp(3, 1, 2, null, 40));
            insns.add(makeOp(6, 4, 5, MemoryOp.Load, 44));
            pipe.run(new InsnIterator(insns));

            assertEquals(MSG + TestUtils.i2s(insns), 3, pipe.getInsns());
            // 123456789abcd
            // fdxmw       |
            //  ..fdxmw    |
            //     fdxmmmmw|
            assertEquals(MSG + TestUtils.i2s(insns), 8 + BMISPRED_LAT + ADDL_MEM_LAT, pipe.getCycles());
        }


        @Test
        public void testMemBranch() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeOp(3, 1, 2, MemoryOp.Load, 40));
            insns.add(makeBr(44, Direction.Taken, 50)); // mispredicted
            insns.add(makeOp(6, 4, 5, null, 50));
            pipe.run(new InsnIterator(insns));

            assertEquals(MSG + TestUtils.i2s(insns), 3, pipe.getInsns());
            // 123456789abcd aml=0, mispred is exposed
            // fdxmw    |
            //  fdxmw   |
            //   ..fdxmw|
            // 123456789abcd aml=2, mispred hides in shadow of memlat
            // fdxmmmw  |
            //  fdx  mw |
            //   ..fdxmw|
            assertEquals(MSG + TestUtils.i2s(insns), 8 + Math.max(BMISPRED_LAT, ADDL_MEM_LAT), pipe.getCycles());
        }


        @Test
        public void testLoadUse() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeBr(0, Direction.Taken, 40));  // mispredicted
            insns.add(makeOp(3, 1, 2, MemoryOp.Load, 40));
            insns.add(makeOp(5, 3, 4, null, 44)); // load-use
            pipe.run(new InsnIterator(insns));

            assertEquals(MSG + TestUtils.i2s(insns), 3, pipe.getInsns());
            // 123456789abcdef
            // fdxmw        |
            //  ..fdxmmmmw  |
            //     fd    xmw|
            assertEquals(MSG + TestUtils.i2s(insns), 8 + BMISPRED_LAT + ADDL_MEM_LAT + 1/*lu*/, pipe.getCycles());
        }


        @Test
        public void testBranchWithLoadUse() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeOp(3, 1, 2, MemoryOp.Load, 0));
            insns.add(makeBrSrcReg(4, Direction.Taken, 20, 3)); // mispredicted, load-use
            insns.add(makeOp(6, 4, 5, null, 20));
            pipe.run(new InsnIterator(insns));

            assertEquals(MSG + TestUtils.i2s(insns), 3, pipe.getInsns());
            // 123456789abcdef
            // fdxmmw     |
            //  fd..xmw   |
            //       fdxmw|
            int lat = (6 + ADDL_MEM_LAT/*load*/) + (2/*load-use branch*/) + (3/*mispred insn*/);
            assertEquals(MSG + TestUtils.i2s(insns), lat, pipe.getCycles());
        }


        @Test
        public void testMemMispredict() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeOp(3, 1, 2, MemoryOp.Load, 0));
            insns.add(makeBr(4, Direction.Taken, 40));  // mispredicted
            insns.add(makeOp(7, 5, 6, null, 40));
            pipe.run(new InsnIterator(insns));
            assertEquals(MSG + TestUtils.i2s(insns), 3, pipe.getInsns());
            // 123456789abcdef
            // fdxmmmw  |   memlat=2
            //  fdx..mw |
            //   ..fdxmw|
            // fdxmw    |   memlat=0
            //  fdxmw   |
            //   ..fdxmw|
            assertTrue(ADDL_MEM_LAT <= 2); // NB: the expected cycles is only correct for mlat<=2
            assertEquals(MSG + TestUtils.i2s(insns), 10, pipe.getCycles());
        }
    }

    public static class PipelineIntegrationTestsBtb {

        @Rule
        public final Timeout globalTimeout = Timeout.seconds(2);

        private final IInorderPipeline pipe;
        /** Keep btb accessible to tests, if they need to fiddle with its state. */
        private final IBranchTargetBuffer btb;
        private final String MSG;

        public PipelineIntegrationTestsBtb() {
            MSG = "[ add'l mem latency = 0 ][ always, btb index bits = 3 ]";
            final IDirectionPredictor always;
            always = new cis501.submission.DirPredAlwaysTaken();
            btb = new cis501.submission.BranchTargetBuffer(3);
            pipe = new cis501.submission.InorderPipeline(0,
                    new BranchPredictor(always, btb));
        }


        @Test
        public void testTrainBtb() {
            List<Insn> insns = new LinkedList<>();
            insns.add(makeBr(8, Direction.Taken, 4)); // mispredicted b/c BTB is empty
            insns.add(makeOp(3, 1, 2, null, 4));
            insns.add(makeBr(8, Direction.Taken, 4)); // predicted correctly this time
            insns.add(makeOp(3, 1, 2, null, 4));
            pipe.run(new InsnIterator(insns));

            assertEquals(MSG, 4, pipe.getInsns());
            // 123456789ab
            // fdxmw     | mispred
            //  ..fdxmw  |
            //     fdxmw |
            //      fdxmw|
            assertEquals(MSG, 6 + 3 + BMISPRED_LAT, pipe.getCycles());
        }

    }

}
