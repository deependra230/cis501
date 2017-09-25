package cis501;

/** A branch predictor, instantiated with a direction predictor and a branch target buffer. */
public class BranchPredictor {

    public final IDirectionPredictor dp;
    public final IBranchTargetBuffer btb;

    public BranchPredictor(IDirectionPredictor dirp, IBranchTargetBuffer btbuf) {
        this.dp = dirp;
        this.btb = btbuf;
    }

    /**
     * @param pc            the PC of an insn i (which may or may not be a branch) on which we make
     *                      our prediction. For ARM, this is the actual PC of the insn; the caller
     *                      does not need to discard the LSB.
     * @param fallthroughPC the fallthrough PC of insn i, computed from i's pc and size
     * @return the predicted next PC
     */
    public long predict(long pc, long fallthroughPC) {
        assert 0 == (pc & 0x1) : "lsb of PC should always be 0 for ARM";
        final long predictorIndex = pc >> 1;
        long btbTarget = btb.predict(predictorIndex);
        // if it's not a (taken) branch, just predict fallthroughPC
        if (0 == btbTarget) return fallthroughPC;

        Direction d = dp.predict(predictorIndex);
        switch (d) {
            case Taken:
                return btbTarget;
            case NotTaken:
                return fallthroughPC;
            default:
                assert false;
        }
        throw new IllegalStateException("Should never reach here!");
    }

    /**
     * Update the predictor state based on the actual behavior of the branch.
     *
     * @param pc           The program counter of the branch to train. For ARM, this is the actual
     *                     PC of the insn; the caller does not need to discard the LSB.
     * @param actualNextPC the true next PC following the branch
     * @param actualDir    the actual direction of the branch
     */
    public void train(long pc, long actualNextPC, Direction actualDir) {
        assert 0 == (pc & 0x1) : "lsb of PC should always be 0 for ARM";
        final long predictorIndex = pc >> 1;
        // only record taken branches in the BTB
        if (Direction.Taken == actualDir) {
            btb.train(predictorIndex, actualNextPC);
        }
        // train direction predictor no matter the direction
        dp.train(predictorIndex, actualDir);
    }

    @Override
    public String toString() {
        return dp.toString() + " " + btb.toString();
    }

    /*
    public void reset() {
        dp.reset();
        btb.reset();
    }
    */

}
