package cis501.submission;

import cis501.*;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BranchPredTraceTest {

    // TODO: replace the path of trace file here
    private static final String TRACE_FILE = "./test/resources/cis501/submission/streamcluster-10M-v2.trace.gz";

    // Trace tests: actual IPCs for streamcluster-10M-v1.trace.gz
    @Test
    public void testBimodal5KTrace() {
        IDirectionPredictor bimodal = new DirPredBimodal(5);
        IBranchTargetBuffer btb = new BranchTargetBuffer(5);
        InorderPipeline sim_bimodal = new InorderPipeline(1, new BranchPredictor(bimodal, btb));
        InsnIterator uiter = new InsnIterator(TRACE_FILE, 5000);
        sim_bimodal.run(uiter);
        assertEquals(5000, sim_bimodal.getInsns());
        assertEquals(7227, sim_bimodal.getCycles());
    }

    @Test
    public void testGShare5KTrace() {
        IDirectionPredictor gshare = new DirPredGshare(5, 5);
        IBranchTargetBuffer btb = new BranchTargetBuffer(5);
        InorderPipeline sim_gshare = new InorderPipeline(1, new BranchPredictor(gshare, btb));
        InsnIterator uiter = new InsnIterator(TRACE_FILE, 5000);
        sim_gshare.run(uiter);
        assertEquals(7247, sim_gshare.getCycles());
        assertEquals(5000, sim_gshare.getInsns());
    }

    @Test
    public void testNeverTaken5KTrace() {
        IDirectionPredictor never = new DirPredNeverTaken();
        IBranchTargetBuffer btb = new BranchTargetBuffer(5);
        InorderPipeline sim_never = new InorderPipeline(1, new BranchPredictor(never, btb));
        InsnIterator uiter = new InsnIterator(TRACE_FILE, 5000);
        sim_never.run(uiter);
        assertEquals(5000, sim_never.getInsns());
        assertEquals(7562, sim_never.getCycles());
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
//
//    @Test
//    public void testGShareFullTrace() {
//        IDirectionPredictor gshare;
//        IBranchTargetBuffer btb;
//        InorderPipeline sim_gshare;
//        InsnIterator uiter;
//
//        for (int indexBits = 4; indexBits <= 18; indexBits++) {
//            gshare = new DirPredGshare(indexBits, indexBits);
//            btb = new BranchTargetBuffer(indexBits);
//            sim_gshare = new InorderPipeline(1, new BranchPredictor(gshare, btb));
//            uiter = new InsnIterator(TRACE_FILE, -1);
//            sim_gshare.run(uiter);
//            //System.out.print(sim_gshare.getCycles()  + "\t");
//            System.out.println(1.0 * sim_gshare.getInsns() / sim_gshare.getCycles());
//        }
//    }
//
//
//    @Test
//    public void testTournamentFullTrace() {
//        IDirectionPredictor bimodal;
//        IDirectionPredictor gshare;
//        IBranchTargetBuffer btb;
//        IDirectionPredictor tournament;
//        InorderPipeline sim_tournament;
//        InsnIterator uiter;
//
//        for (int indexBits = 4; indexBits <= 18; indexBits++) {
//            bimodal = new DirPredBimodal(indexBits - 2);
//            gshare = new DirPredGshare(indexBits - 1, indexBits - 1);
//            btb = new BranchTargetBuffer(indexBits);
//            tournament = new DirPredTournament(indexBits - 2, gshare, bimodal);
//            sim_tournament = new InorderPipeline(1, new BranchPredictor(tournament, btb));
//            uiter = new InsnIterator(TRACE_FILE, -1);
//            sim_tournament.run(uiter);
//            //System.out.print(sim_tournament.getCycles()  + "\t");
//            System.out.println(1.0*sim_tournament.getInsns()/sim_tournament.getCycles());
//        }
//    }

    /*
    * Q: Why might a gshare predictor outperform bimodal?
    * A: gshare predictor captures not only an individual branch's most recent history but also
    *       the global history of all branches.  Thus, if the branches in a program exhibit high global
    *       correlation, gshare might outperform bimodal.
    *
    *
    *
    * Q: Why might a bimodal predictor outperform gshare?
    * A: It is not always the case that branches exhibit global correlation.  Thus, if the branches in
    *       a program have only local correlation and not global correlation, bimodal predictor might
    *       outperfom gshare.
    *
    *       Also, if the branch history table size is same for the both predictors, then gshare might
    *       have more aliasing issues than the bimodal one if the program has large number of branches.
    * */

}
