package cis501.submission;

import cis501.IBranchTargetBuffer;

public class BranchTargetBuffer implements IBranchTargetBuffer {

    public BranchTargetBuffer(int indexBits) {

    }

    @Override
    public long predict(long pc) {
        return 0;
    }

    @Override
    public void train(long pc, long actual) {

    }
}
