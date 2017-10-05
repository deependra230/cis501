package cis501.submission;


import cis501.*;

import java.util.Set;

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

    /**
     * Create a new pipeline with the given additional memory latency.
     *
     * @param additionalMemLatency The number of extra cycles mem insns require in the M stage. If
     *                             0, mem insns require just 1 cycle in the M stage, like all other
     *                             insns. If x, mem insns require 1+x cycles in the M stage.
     * @param bypasses             Which bypasses should be modeled. For example, if this is an
     *                             empty set, then your pipeline should model no bypassing, using
     *                             stalling to resolve all data hazards.
     */
    public InorderPipeline(int additionalMemLatency, Set<Bypass> bypasses) {

    }

    /**
     * Create a new pipeline with the additional memory latency and branch predictor. The pipeline
     * should model full bypassing (MX, Wx, WM).
     *
     * @param additionalMemLatency see InorderPipeline(int, Set<Bypass>)
     * @param bp                   the branch predictor to use
     */
    public InorderPipeline(int additionalMemLatency, BranchPredictor bp) {
    }

    @Override
    public String[] groupMembers() {
        return new String[]{"your", "names"};
    }

    @Override
    public void run(InsnIterator ii) {

    }

    @Override
    public long getInsns() {
        return 0;
    }

    @Override
    public long getCycles() {
        return 0;
    }
}
