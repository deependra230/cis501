package cis501.submission;


import cis501.*;
import javafx.util.Pair;

import java.util.*;

enum Stage {
    FETCH(0), DECODE(1), EXECUTE(2), MEMORY(3), WRITEBACK(4);

    private static Stage[] vals = values();
    private final int index;

    Stage(int idx) {
        this.index = idx;
    }

    /** Returns the index of this stage within the pipeline */
    public int i() {
        return index;
    }

    /** Returns the next stage in the pipeline, e.g., next after Fetch is Decode */
    public Stage next() {
        return vals[(this.ordinal() +1) % vals.length];
    }
}

public class InorderPipeline implements IInorderPipeline {
    private static final int NO_REGISTER = -1;

    private final int additionalMemLatency;
    private final Set<Bypass> bypasses;
    private final BranchPredictor branchPredictor;

    private List<Insn> pipeline;
    private Insn lastFetchedInsn;

    private long numInsns;
    private long numCycles;

    /**
     * Create a new pipeline with the given additional memory latency.
     *git reset HEAD~
     * @param additionalMemLatency The number of extra cycles mem insns require in the M stage. If
     *                             0, mem insns require just 1 cycle in the M stage, like all other
     *                             insns. If x, mem insns require 1+x cycles in the M stage.
     * @param bypasses             Which bypasses should be modeled. For example, if this is an
     *                             empty set, then your pipeline should model no bypassing, using
     *                             stalling to resolve all data hazards.
     */
    public InorderPipeline(int additionalMemLatency, Set<Bypass> bypasses) {
        this.additionalMemLatency = additionalMemLatency;
        this.bypasses = bypasses;
        this.branchPredictor = null;

        this.pipeline = new ArrayList<>(Stage.values().length);
        initializePipeline(); // all stages set to null
        this.lastFetchedInsn = null;

        this.numInsns = 0;
        this.numCycles = 0;
    }

    /**
     * Create a new pipeline with the additional memory latency and branch predictor. The pipeline
     * should model full bypassing (MX, Wx, WM).
     *
     * @param additionalMemLatency see InorderPipeline(int, Set<Bypass>)
     * @param bp                   the branch predictor to use
     */
    public InorderPipeline(int additionalMemLatency, BranchPredictor bp) {
        this.additionalMemLatency = additionalMemLatency;
        this.bypasses = Bypass.FULL_BYPASS;
        this.branchPredictor = bp;

        this.pipeline = new ArrayList<>(Stage.values().length);
        initializePipeline(); // all stages set to null
        this.lastFetchedInsn = null;

        this.numInsns = 0;
        this.numCycles = 0;
    }

    /** Cache homework ctor */
    public InorderPipeline(BranchPredictor bp, ICache ic, ICache dc) {
    }

    @Override
    public String[] groupMembers() {
        return new String[]{"Deependra Singh", "Clinton Nyabuto"};
    }

    @Override
    public void run(InsnIterator ii) {
        int remainingMemLatency = 1 + additionalMemLatency;

        /* In order to simulate the branch-predictor, we keep track of the last-fetched-PC
        * and the PCs of the insns in F, D that would have been there if the branch-prediction
        * was happening in real-time. */
        List<Pair<Long, Long>> branchPredictorPCs = new ArrayList<>(2);
        branchPredictorPCs.add(null);
        branchPredictorPCs.add(null);

        while (ii.hasNext() || !isPipelineEmpty()) {
            remainingMemLatency = advanceInsns(remainingMemLatency, bypasses, branchPredictorPCs, ii);

            if (!hasInsn(Stage.FETCH) && ii.hasNext()) {
                pipeline.set(Stage.FETCH.i(), ii.next());
                numInsns += 1;

                //simulate branch predictor
                if (branchPredictor != null && lastFetchedInsn != null) {
                    // Note that we are only simulating the branch prediction, and hence we are putting the
                    // last-correctly-fetched (because the trace is dynamic) insn's fall-through-pc instead of the
                    // last-predicted-fetched insn's fall-through-pc.
                    long predictedPC = branchPredictor.predict(lastFetchedInsn.pc, lastFetchedInsn.fallthroughPC());
                    branchPredictorPCs.set(0, new Pair<>(lastFetchedInsn.pc, predictedPC));
                }

                lastFetchedInsn = pipeline.get(Stage.FETCH.i());
            }
            numCycles += 1;
        }
    }

    @Override
    public long getInsns() {
        return numInsns;
    }

    @Override
    public long getCycles() {
        return numCycles;
    }

    // If the pipeline has no instructions
    private boolean isPipelineEmpty() {
        boolean isEmpty = true;
        for (Insn insn: pipeline) {
            if (insn != null) {
                isEmpty = false;
                break;
            }
        }
        return isEmpty;
    }

    private int advanceInsns(int remainingMemLatency, Set<Bypass> bypasses, List<Pair<Long, Long>> branchPredictorPCs,
                             InsnIterator ii) {
        // Insn in the W stage will always flow
        pipeline.set(Stage.WRITEBACK.i(), null);

        /*
        * Check if the memory instruction can advance. If yes, then do further calculations;
        * if no, fill
        */
        if (hasInsn(Stage.MEMORY) &&
                (pipeline.get(Stage.MEMORY.i()).mem != null) &&
                remainingMemLatency > 1) {
            remainingMemLatency = remainingMemLatency - 1;

            // If there is an insn in X, we handle the branch prediction accordingly.
            if (hasInsn(Stage.EXECUTE)) {
                branchPredictionTrainingAndFlush(branchPredictorPCs, ii);
            }

            // Filling up the pipeline before the M stage if some stages are empty
            if (!hasInsn(Stage.EXECUTE) && hasInsn(Stage.DECODE)) {
                int dstReg = pipeline.get(Stage.MEMORY.i()).dstReg;

                int src1 = pipeline.get(Stage.DECODE.i()).srcReg1;
                int src2 = pipeline.get(Stage.DECODE.i()).srcReg2;

                if ((dstReg == NO_REGISTER) || ((dstReg != src1)&&(dstReg != src2))) {
                    moveInsnToNextStage(Stage.DECODE);
                    updateBranchPredictionPCs(branchPredictorPCs, Stage.DECODE);
                    moveInsnToNextStage(Stage.FETCH);
                    updateBranchPredictionPCs(branchPredictorPCs, Stage.FETCH);

                } else if ((pipeline.get(Stage.DECODE.i()).mem == MemoryOp.Store)
                        && (dstReg == src1)
                        && (bypasses.contains(Bypass.WM))
                        ) {
                    moveInsnToNextStage(Stage.DECODE);
                    updateBranchPredictionPCs(branchPredictorPCs, Stage.DECODE);
                    moveInsnToNextStage(Stage.FETCH);
                    updateBranchPredictionPCs(branchPredictorPCs, Stage.FETCH);
                }

            }

            if (!hasInsn(Stage.DECODE) && hasInsn(Stage.FETCH)) {
                moveInsnToNextStage(Stage.FETCH);
                updateBranchPredictionPCs(branchPredictorPCs, Stage.FETCH);
            }
            return remainingMemLatency;
        }

        boolean isStallRequired;
        /*
        * Bypass disabled order is very important. Since other bypass-disables have dependencies on WX,
        * it should be the first.
        * We do not update this case after the pipelining homework: will not be tested again.
        * */
        if (!bypasses.contains(Bypass.WX)) {
            isStallRequired = handleWXBypassDisabled();
            if (isStallRequired) {
                remainingMemLatency = 1 + this.additionalMemLatency;
                moveInsnToNextStage(Stage.MEMORY);
                moveInsnToNextStage(Stage.EXECUTE);
                return remainingMemLatency;
            }
        }

        /* Further bypasses do not need info from the memory stage insn, hence advancing it.*/
        if (hasInsn(Stage.MEMORY)) {
            remainingMemLatency = 1 + this.additionalMemLatency;
            moveInsnToNextStage(Stage.MEMORY);
        }

        /*Stalls despite all bypasses*/
        isStallRequired = handleNoBypassDisabled();
        if (isStallRequired) {
            //reaching here necessitates that stage X has an insn so no need to check before invoking misprediction check.
            branchPredictionTrainingAndFlush(branchPredictorPCs, ii);
            moveInsnToNextStage(Stage.EXECUTE);
            return remainingMemLatency;
        }

        /*MX Bypass disabled. - we do not update this case after the pipelining homework: will not be tested again.*/
        if (!bypasses.contains(Bypass.MX)) {
            isStallRequired = handleMXBypassDisabled();
            if (isStallRequired) {
                moveInsnToNextStage(Stage.EXECUTE);
                return remainingMemLatency;
            }
        }

        /* WM bypass disabled. Peculiar case of A->MSV has already been handled in MX: read the code of MX
        * for more details.
        *  We do not update this case after the pipelining homework: will not be tested again.
        *  */
        if (!bypasses.contains(Bypass.WM)) {
            isStallRequired = handleWMBypassDisabled();
            if (isStallRequired) {
                moveInsnToNextStage(Stage.EXECUTE);
                return remainingMemLatency;
            }
        }

        //All Stalls handled, now advancing rest of the insns:
        if (hasInsn(Stage.EXECUTE)) {
            branchPredictionTrainingAndFlush(branchPredictorPCs, ii);
            moveInsnToNextStage(Stage.EXECUTE);
        }

        if (hasInsn(Stage.DECODE)) {
            moveInsnToNextStage(Stage.DECODE);
            updateBranchPredictionPCs(branchPredictorPCs, Stage.DECODE);
        }

        if (hasInsn(Stage.FETCH)) {
            moveInsnToNextStage(Stage.FETCH);
            updateBranchPredictionPCs(branchPredictorPCs, Stage.FETCH);
        }
        return remainingMemLatency;
    }

    private boolean hasInsn(Stage stage) {
        if (pipeline.get(stage.i()) == null) {
            return false;
        }
        return true;
    }

    private void moveInsnToNextStage(Stage currentStage) {
        pipeline.set(currentStage.next().i(), pipeline.get(currentStage.i()));
        pipeline.set(currentStage.i(), null);
    }

    private void updateBranchPredictionPCs(List<Pair<Long, Long>> branchPredictorPCs, Stage stage){
        if (branchPredictor == null) {
            return;
        }

        if (stage == Stage.DECODE) {
            branchPredictorPCs.set(1, null);
        } else {
            branchPredictorPCs.set(1, branchPredictorPCs.get(0));
            branchPredictorPCs.set(0, null);
        }
    }

    private void initializePipeline() {
        for (Stage stage: Stage.values()) {
            pipeline.add(null);
        }
    }

    /*
    * Even If all the bypasses are enabled, we have to stall for certain load-use dependencies : namely,
    * load followed by a use except when the use is a store and dependency is in the form of store's value
    * (ML->MSV handled in WM bypass.)
    *
    * We need to detect it at D->X stage. If if find one, we need to stall.
    * Dependencies: 1. ML -> A; ML -> ML; ML -> MSA.
    *
    * Notes: (i) it assumes that WX bypass is enabled,
    * thus bypassing the output of the load insn directly to X in the next cycle.
    * (ii) All A-> dependencies can be handled via other bypasses.
    */
    private boolean handleNoBypassDisabled() {

        if (!hasInsn(Stage.EXECUTE) ||
                !hasInsn(Stage.DECODE) ||
                pipeline.get(Stage.EXECUTE.i()).mem != MemoryOp.Load ||
                pipeline.get(Stage.EXECUTE.i()).dstReg == NO_REGISTER) {
            return false;
        }

        boolean stallRequired = false;

        int dstReg = pipeline.get(Stage.EXECUTE.i()).dstReg;
        if (pipeline.get(Stage.DECODE.i()).mem == MemoryOp.Load) {
            if ((pipeline.get(Stage.DECODE.i()).srcReg1 == dstReg) || (pipeline.get(Stage.DECODE.i()).srcReg2 == dstReg)) {
                // 1 cycle stall required - WX will be used in the next cycle
                stallRequired = true;
            }
        } else if (pipeline.get(Stage.DECODE.i()).mem == MemoryOp.Store) {
            if (pipeline.get(Stage.DECODE.i()).srcReg2 == dstReg) {
                // 1 cycle stall required - WX will be used in the next cycle
                stallRequired = true;
            }
        } else {
            if ((pipeline.get(Stage.DECODE.i()).srcReg1 == dstReg) || (pipeline.get(Stage.DECODE.i()).srcReg2 == dstReg)) {
                // 1 cycle stall required - WX will be used in the next cycle
                stallRequired = true;
            }
        }
        return stallRequired;
    }

    /*
    * M->X dependency has to be detected at D->X. If we find one, we need to stall:
    * Dependencies: 1. A -> A; A-> ML; A-> MSA;
    *               2. A-> MSV is a special case: even if MX is disabled, it can be passed via WM;
    *                  but if both are disabled, we need to stall.
    *               3. ML-> is not possible to be rectified by this.
    * Notes: (i) it assumes that WX bypass is enabled,
    * thus bypassing the output of the A insn directly to X in the next cycle.
    */
    private boolean handleMXBypassDisabled() {
        if (!hasInsn(Stage.EXECUTE) ||
                !hasInsn(Stage.DECODE) ||
                pipeline.get(Stage.EXECUTE.i()).mem != null ||
                pipeline.get(Stage.EXECUTE.i()).dstReg == NO_REGISTER) {
            return false;
        }

        boolean stallRequired = false;

        int dstReg = pipeline.get(Stage.EXECUTE.i()).dstReg;
        if (pipeline.get(Stage.DECODE.i()).mem == MemoryOp.Load) {
            if ((pipeline.get(Stage.DECODE.i()).srcReg1 == dstReg) || (pipeline.get(Stage.DECODE.i()).srcReg2 == dstReg)) {
                // 1 cycle stall required - WX will be used in the next cycle
                stallRequired = true;
            }
        } else if (pipeline.get(Stage.DECODE.i()).mem == MemoryOp.Store) {
            if (pipeline.get(Stage.DECODE.i()).srcReg2 == dstReg) {
                // 1 cycle stall required - WX will be used in the next cycle
                stallRequired = true;
            }
            if (pipeline.get(Stage.DECODE.i()).srcReg1 == dstReg) {
                if (!bypasses.contains(Bypass.WM)) {
                    // Very good and corner case
                    stallRequired = true;
                }
            }
        } else {
            if ((pipeline.get(Stage.DECODE.i()).srcReg1 == dstReg) || (pipeline.get(Stage.DECODE.i()).srcReg2 == dstReg)) {
                // 1 cycle stall required - WX will be used in the next cycle
                stallRequired = true;
            }
        }
        return stallRequired;
    }

    /*
    * W->X dependency has to be detected at M->D. If there is a dependency, we need to stall.
    * Dependencies: 1. ML -> A; ML -> ML; ML -> MSA/MSV.
    *               2. A -> A; A -> ML; A-> MSA/MSV.
    *
    * Caveat (multiple producer dependency): if the destination of W is overwritten by the insn in M,
    * then X has a dependency on M and not W, and therefore we can ignore whether the WX Bypass is enabled or not
    * (i.e., we will not need a stall due to the WX bypass being disabled). Note that at the time of
    * detection of W->X (i.e. at M->D), the intermediate insn will be sandwiched in X stage.
    * */
    private boolean handleWXBypassDisabled() {
        if (!hasInsn(Stage.MEMORY) ||
                !hasInsn(Stage.DECODE) ||
                pipeline.get(Stage.MEMORY.i()).dstReg == NO_REGISTER) {
            return false;
        }

        boolean stallRequired = false;

        int dstReg = pipeline.get(Stage.MEMORY.i()).dstReg;

        // code for the caveat described above
        if (hasInsn(Stage.EXECUTE) && pipeline.get(Stage.EXECUTE.i()).dstReg == dstReg) {
            return false;
        }

        if (pipeline.get(Stage.DECODE.i()).mem == MemoryOp.Load) {
            if ((pipeline.get(Stage.DECODE.i()).srcReg1 == dstReg) || (pipeline.get(Stage.DECODE.i()).srcReg2 == dstReg)) {
                // 1 cycle stall required
                stallRequired = true;
            }
        } else if (pipeline.get(Stage.DECODE.i()).mem == MemoryOp.Store) {
            if ((pipeline.get(Stage.DECODE.i()).srcReg1 == dstReg) || (pipeline.get(Stage.DECODE.i()).srcReg2 == dstReg)) {
                // 1 cycle stall required
                stallRequired = true;
            }
        } else {
            if ((pipeline.get(Stage.DECODE.i()).srcReg1 == dstReg) || (pipeline.get(Stage.DECODE.i()).srcReg2 == dstReg)) {
                // 1 cycle stall required
                stallRequired = true;
            }
        }
        return stallRequired;
    }

    /*
    * W->M dependency has to detected at D->X. If there is one, we need to stall.
    * Dependencies: 1. ML -> MSV; ML !> MSA(MSA needs it in X); ML !> ML(ML needs it in X); ML !> A(A needs it in X).
    *               2. Similarly, this insn could not have helped anything other than A->MSV;
    *               3. A -> MSV: MX should have been able to handle it; so make sure that for this case,
    *                       in the code for MX, we put a stall if both MX and WM are disabled.
    *
    * Notes: (i) it assumes that WX bypass is enabled,
    * thus bypassing the output of the load insn directly to the X-stage in the next cycle.
    * */
    private boolean handleWMBypassDisabled() {
        if (!hasInsn(Stage.EXECUTE) ||
                !hasInsn(Stage.DECODE) ||
                pipeline.get(Stage.EXECUTE.i()).mem != MemoryOp.Load ||
                pipeline.get(Stage.EXECUTE.i()).dstReg == NO_REGISTER) {
            return false;
        }

        boolean stallRequired = false;

        int dstReg = pipeline.get(Stage.EXECUTE.i()).dstReg;
        if (pipeline.get(Stage.DECODE.i()).mem == MemoryOp.Store) {
            if (pipeline.get(Stage.DECODE.i()).srcReg1 == dstReg) {
                // 1 cycle stall required
                stallRequired = true;
            }
        }
        return stallRequired;
    }

    /*
    * If X has a branch insn, then train the predictor
    * If D has an incorrect insn according the the resolution of insn in X, then invoke flush.
    * */
    private void branchPredictionTrainingAndFlush(List<Pair<Long, Long>> branchPredictorPCs, InsnIterator ii) {
        if (branchPredictor == null) {
            return;
        }

        /* We did not fetch anything in the last-to-last cycle(based on the prediction I mean  ---we could have the good
        * insn in fetch after flush, but then it makes no sense to do the branch prediction analysis)
        *
        * Or, we fetched the correct predicted insn in the last-to-last cycle and we checked already.
        * */
        if (branchPredictorPCs.get(1) == null) {
            return;
        }

        Insn xInsn = pipeline.get(Stage.EXECUTE.i());
        long predictedAddress = branchPredictorPCs.get(1).getValue();
        long actualAddress = pipeline.get(Stage.DECODE.i()).pc;

        if (xInsn.branchType != null) {
            branchPredictor.train(xInsn.pc, xInsn.branchTarget, xInsn.branchDirection);
        }

        if (predictedAddress != actualAddress) {
            // flush if incorrect prediction
            branchMisPredictionFlush(branchPredictorPCs, ii);
        } else {
            /* if correct, set decode-predicted-pc to null to indicate that it has been checked
            *   and no need to check in next cycle and therefore end up training again!
            *   Think F, D, X (has a br insn), M (has a mem insn) are full and M insn is stuck there because of mem
            *   latency and D has the correct insn (and therefore entire pipeline until M is stuck ),
            *   and you end up training every cycle during this process.  That would be incorrect! Train only once
            *   per branch.
            *   */
            branchPredictorPCs.set(1, null);
        }
    }

    /*
    * If Fetch and Decode have insns then clearly they are not correct, so push them back on the iterator
    * and decrement the count of insns processed.
    * */
    private boolean branchMisPredictionFlush(List<Pair<Long, Long>> branchPredictorPCs, InsnIterator ii) {
        boolean flushed = false;
        if (hasInsn(Stage.FETCH)) {
            ii.putBack(pipeline.get(Stage.FETCH.i()));
            this.numInsns -= 1;
            pipeline.set(Stage.FETCH.i(), null);
            flushed = true;
            branchPredictorPCs.set(0, null);
        }
        if (hasInsn(Stage.DECODE)) {
            ii.putBack(pipeline.get(Stage.DECODE.i()));
            this.numInsns -= 1;
            pipeline.set(Stage.DECODE.i(), null);
            flushed = true;
            branchPredictorPCs.set(1, null);
        }
        if (flushed) {
            lastFetchedInsn = null;
        }
        return flushed;
    }
}
