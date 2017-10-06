package cis501.submission;

import cis501.Direction;
import cis501.IDirectionPredictor;

public class DirPredTournament extends DirPredBimodal {
	private IDirectionPredictor predictorNT;
	private IDirectionPredictor predictorT;

	public DirPredTournament(int chooserIndexBits, IDirectionPredictor predictorNT, IDirectionPredictor predictorT) {
		super(chooserIndexBits); // re-use DirPredBimodal as the chooser table
		this.predictorNT = predictorNT;
		this.predictorT = predictorT;
	}

	@Override
	public Direction predict(long pc) {
		switch (super.predict(pc)) {
			case NotTaken:
				return predictorNT.predict(pc);
			case Taken:
				return predictorT.predict(pc);
            default:
		        assert false;
		}
        throw new IllegalStateException("Should never reach here!");
	}

	@Override
	public void train(long pc, Direction actual) {
		predictorNT.train(pc, actual);
		predictorT.train(pc, actual);

		Direction NTDir = predictorNT.predict(pc);
		Direction TDir = predictorT.predict(pc);

		if (NTDir != TDir) {
		    if (actual == NTDir) {
		        super.train(pc, Direction.NotTaken);
            } else if (actual == TDir) {
		        super.train(pc, Direction.Taken);
            }
        }
		// else do nothing
	}

}
