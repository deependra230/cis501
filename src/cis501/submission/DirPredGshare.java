package cis501.submission;

import cis501.Direction;

public class DirPredGshare extends DirPredBimodal {

    public DirPredGshare(int indexBits, int historyBits) {
        super(indexBits);
    }

    @Override
    public Direction predict(long pc) {
        return null;
    }

    @Override
    public void train(long pc, Direction actual) {

    }
}
