package cis501.submission;

import cis501.Direction;
import cis501.IDirectionPredictor;

import java.util.ArrayList;
import java.util.List;

enum State {
    STRONGLY_NOT_TAKEN,
    NOT_TAKEN,
    TAKEN,
    STRONGLY_TAKEN;

    public Direction getDirection() {
        switch (this) {
            case STRONGLY_NOT_TAKEN:
                return Direction.NotTaken;
            case NOT_TAKEN:
                return Direction.NotTaken;
            case TAKEN:
                return Direction.Taken;
            case STRONGLY_TAKEN:
                return Direction.Taken;
            default:
                assert false;
        }
        throw new IllegalStateException("Should never reach here!");
    }

    public State updatedState(Direction actual) {
        if (actual == Direction.Taken) {
            return increaseCounter();
        } else if (actual == Direction.NotTaken) {
            return decreaseCounter();
        } else {
            return this;
        }
    }

    private State increaseCounter() {
        if (this.ordinal() == 3) {
            return this;
        } else {
            return State.values()[this.ordinal() + 1];
        }
    }

    private State decreaseCounter() {
        if (this.ordinal() == 0) {
            return this;
        } else {
            return State.values()[this.ordinal() - 1];
        }
    }
}


public class DirPredBimodal implements IDirectionPredictor {
    private int numIndexBits;
    private List<State> branchHistoryTable;

    public DirPredBimodal(int indexBits) {
        numIndexBits = indexBits;
        branchHistoryTable = new ArrayList<State>((int) Math.pow(2, numIndexBits));
        initializeBHT();
    }

    @Override
    public Direction predict(long pc) {
        int index = getPCIndex(pc);
        return branchHistoryTable.get(index).getDirection();
    }

    @Override
    public void train(long pc, Direction actual) {
        int index = getPCIndex(pc);
        State currentState = branchHistoryTable.get(index);
        branchHistoryTable.set(index, currentState.updatedState(actual));
    }

    private int getPCIndex(long pc) {
        long rightShiftedPC = pc >> numIndexBits;
        rightShiftedPC = rightShiftedPC << numIndexBits;
        return (int) (pc^rightShiftedPC);
    }

    private void initializeBHT() {
        int size = (int) Math.pow(2, numIndexBits);
        for (int i = 0; i < size; i++) {
            branchHistoryTable.add(State.STRONGLY_NOT_TAKEN);
        }
    }

}
