package cis501.submission;

import cis501.IBranchTargetBuffer;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;


public class BranchTargetBuffer implements IBranchTargetBuffer {
    private final int numIndexBits;
    private List<Long> bTBTags;
    private List<Long> bTBTargets;

    public BranchTargetBuffer(int indexBits) {
        this.numIndexBits = indexBits;
        this.bTBTags = new ArrayList<Long>((int) Math.pow(2, numIndexBits));
        this.bTBTargets = new ArrayList<Long>((int) Math.pow(2, numIndexBits));
        initializeBTB();
    }

    @Override
    public long predict(long pc) {
       int index = getPCIndex(pc);
       if (bTBTags.get(index) == pc) {
           return bTBTargets.get(index);
       } else {
           return 0L;
       }
    }

    @Override
    public void train(long pc, long actual) {
        int index = getPCIndex(pc);
        bTBTags.set(index, pc);
        bTBTargets.set(index, actual);
    }

    private int getPCIndex(long pc) {
        long rightShiftedPC = pc >> numIndexBits;
        rightShiftedPC = rightShiftedPC << numIndexBits;
        return (int) (pc^rightShiftedPC);
    }

    private void initializeBTB() {
        int size = (int) Math.pow(2, numIndexBits);
        for (int i = 0; i < size; i++) {
            this.bTBTags.add(0L);
            this.bTBTargets.add(0L);
        }
    }
}
