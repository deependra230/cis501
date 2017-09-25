package cis501.submission;

import cis501.*;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BranchPredSampleTest {

    // TODO: replace the path of trace file here
    private static final String TRACE_FILE = "path/to/go-10M.trace.gz";

    private IBranchTargetBuffer btb;
    private IDirectionPredictor bimodal;
    private IDirectionPredictor gshare;
    private IDirectionPredictor tournament;
    private IInorderPipeline pipe;

    private static Insn makeBr(long pc, Direction dir, /*long fallthruPC*/ long targetPC) {
        return new Insn(1, 2, 3, pc, 4, dir, targetPC, null, null, 0, 0, "<synthetic>");
    }

    // BTB tests

    @Before
    public void setUp() throws Exception {
        // Runs before each test...() method
        btb = new BranchTargetBuffer(3/*index bits*/);
        bimodal = new DirPredBimodal(3/*index bits*/);
        gshare = new DirPredGshare(3/*index bits*/, 1/*history bits*/);

        // create a tournament predictor that behaves like bimodal
        IDirectionPredictor always = new DirPredAlwaysTaken();
        IDirectionPredictor never = new DirPredNeverTaken();
        tournament = new DirPredTournament(3/*index bits*/, never, always);

        // pipeline uses never predictor
        pipe = new InorderPipeline(0, new BranchPredictor(never, btb));
    }

    @Test
    public void testBtbInitialState() {
        assertEquals(0, btb.predict(0));
    }

    /**
     * In Java, % is remainder, not modulo. See http://stackoverflow.com/questions/5385024/mod-in-java-produces-negative-numbers
     * for more details.
     */
    @Test
    public void testRemainderIndexing() {
        assertEquals(0, btb.predict(-1));
    }

    @Test
    public void testBtbNewTarget() {
        btb.train(0, 42);
        assertEquals(42, btb.predict(0));
    }

    // Bimodal tests

    @Test
    public void testBtbAlias() {
        btb.train(0, 42);
        assertEquals(42, btb.predict(0));
        long alias0 = (long) Math.pow(2, 3);
        btb.train(alias0, 100);
        assertEquals(0, btb.predict(0)); // tag doesn't match
        assertEquals(100, btb.predict(alias0)); // tag matches
    }

    @Test
    public void testBimodalInitialState() {
        assertEquals(Direction.NotTaken, bimodal.predict(0));
    }

    @Test
    public void testRemainderIndexing2() {
        assertEquals(Direction.NotTaken, bimodal.predict(-1));
    }

    @Test
    public void testBimodalTaken() {
        bimodal.train(0, Direction.Taken);
        assertEquals(Direction.NotTaken, bimodal.predict(0));
        bimodal.train(0, Direction.Taken);
        assertEquals(Direction.Taken, bimodal.predict(0));
    }

    // Gshare tests

    @Test
    public void testBimodalTakenSaturation() {
        for (int i = 0; i < 10; i++) {
            bimodal.train(0, Direction.Taken);
        }
        bimodal.train(0, Direction.NotTaken);
        bimodal.train(0, Direction.NotTaken);
        assertEquals(Direction.NotTaken, bimodal.predict(0));
    }

    @Test
    public void testGshareInitialState() {
        assertEquals(Direction.NotTaken, gshare.predict(0));
    }

    // Tournament predictor tests

    @Test
    public void testGshareTaken() {
        // initially, history is 0
        gshare.train(0, Direction.Taken); // 0 ^ 0 == 0
        // history is 1
        assertEquals(Direction.NotTaken, gshare.predict(1)); // 1 ^ 1 == 0
        gshare.train(1, Direction.Taken); // 1 ^ 1 == 0
        // history is 1
        assertEquals(Direction.Taken, gshare.predict(1)); // 1 ^ 1 == 0
    }

    @Test
    public void testTournInitialState() {
        assertEquals(Direction.NotTaken, tournament.predict(0));
    }

    @Test
    public void testTournTaken() {
        tournament.train(0, Direction.Taken);
        assertEquals(Direction.NotTaken, tournament.predict(0));
        tournament.train(0, Direction.Taken);
        assertEquals(Direction.Taken, tournament.predict(0));
    }

    // Pipeline tests

    @Test
    public void testTournTakenSaturation() {
        for (int i = 0; i < 10; i++) {
            tournament.train(0, Direction.Taken);
        }
        tournament.train(0, Direction.NotTaken);
        tournament.train(0, Direction.NotTaken);
        assertEquals(Direction.NotTaken, tournament.predict(0));
    }

    @Test
    public void testPipeCorrectPred() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeBr(0, Direction.NotTaken, 40));
        insns.add(makeBr(4, Direction.NotTaken, 40));
        pipe.run(insns);

        assertEquals(2, pipe.getInsns());
        // 123456789
        // fdxmw |
        //  fdxmw|
        assertEquals(7, pipe.getCycles());
    }

    @Test
    public void testPipeMispredicted() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeBr(0, Direction.Taken, 40));  // mispredicted
        insns.add(makeBr(40, Direction.NotTaken, 60));
        pipe.run(insns);

        assertEquals(2, pipe.getInsns());
        // 123456789
        // fdxmw   |
        //  ..fdxmw|
        assertEquals(7 + 2, pipe.getCycles());
    }

    @Test
    public void testPipe2Mispredicted() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeBr(0, Direction.Taken, 40));  // mispredicted
        insns.add(makeBr(40, Direction.Taken, 60));  // mispredicted
        insns.add(makeBr(60, Direction.NotTaken, 80));
        pipe.run(insns);

        assertEquals(3, pipe.getInsns());
        // 123456789abcd
        // fdxmw      |
        //  ..fdxmw   |
        //     ..fdxmw|
        assertEquals(8 + (2 * 2), pipe.getCycles());
    }

    // Trace tests: actual IPCs for streamcluster-10M-v1.trace.gz with the always/never-taken
    // predictors and zero additional memory latency.

    @Test
    public void testAlwaysTakenTrace() {
        final IDirectionPredictor always = new DirPredAlwaysTaken();
        final IBranchTargetBuffer bigBtb = new BranchTargetBuffer(10);
        InsnIterator uiter = new InsnIterator(TRACE_FILE, -1);
        IInorderPipeline pl = new InorderPipeline(0, new BranchPredictor(always, bigBtb));
        pl.run(uiter);
        assertEquals(0.96, pl.getInsns() / (double) pl.getCycles(), 0.01);
    }

    @Test
    public void testNeverTakenTrace() {
        final IDirectionPredictor never = new DirPredNeverTaken();
        final IBranchTargetBuffer bigBtb = new BranchTargetBuffer(10);
        InsnIterator uiter = new InsnIterator(TRACE_FILE, -1);
        IInorderPipeline pl = new InorderPipeline(0, new BranchPredictor(never, bigBtb));
        pl.run(uiter);
        assertEquals(0.81, pl.getInsns() / (double) pl.getCycles(), 0.01);
    }

    // add more tests here!
}
