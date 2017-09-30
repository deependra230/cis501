package cis501.submission;

import cis501.Direction;
import cis501.IDirectionPredictor;

public class DirPredTournament extends DirPredBimodal {
	int[] bht;
	private IDirectionPredictor predictorNT;
	private IDirectionPredictor predictorT;
	private int indexBits;
	final int STRONGLY_NOT_TAKEN = 0;
	final int WEAKLY_NOT_TAKEN = 1;
	final int WEAKLY_TAKEN = 2;
	final int STRONGLY_TAKEN = 3;

	public DirPredTournament(int chooserIndexBits, IDirectionPredictor predictorNT, IDirectionPredictor predictorT) {
		super(chooserIndexBits); // re-use DirPredBimodal as the chooser table
		this.predictorNT = predictorNT;
		this.predictorT = predictorT;
		bht = new int[2 ^ chooserIndexBits];
		indexBits = chooserIndexBits;
	}

	@Override
	public Direction predict(long pc) {
		int index = (int) (pc & (int) Math.pow((double) indexBits, 2.0) - 1);
		switch (bht[index]) {
		case STRONGLY_NOT_TAKEN:
		case WEAKLY_NOT_TAKEN:
			return predictorNT.predict(pc);

		case WEAKLY_TAKEN:
		case STRONGLY_TAKEN:
			return predictorT.predict(pc);

		default:
			return null;
		}

	}

	@Override
	public void train(long pc, Direction actual) {
		predictorNT.train(pc, actual);
		predictorT.train(pc, actual);

		Direction predictorNTDirection = predictorNT.predict(pc);
		Direction predictorTDirection = predictorT.predict(pc);
		int index = (int) (pc & (int) Math.pow((double) indexBits, 2.0) - 1);
		if (predictorNTDirection != predictorTDirection) {
			if (predictorNTDirection == actual) {
				if (bht[index] != STRONGLY_NOT_TAKEN) {
					bht[index]--;
				}
			} else if (predictorTDirection == actual) {
				if (bht[index] != STRONGLY_TAKEN) {
					bht[index]++;
				}

			}
		}

		// else do nothing

	}

}
