package cis501.submission;

import cis501.Direction;
import cis501.IDirectionPredictor;

public class DirPredAlwaysTaken implements IDirectionPredictor {

    @Override
    public Direction predict(long pc) {
        return null;
    }

    @Override
    public void train(long pc, Direction actual) {

    }

}
