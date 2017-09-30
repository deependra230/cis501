package cis501.submission;

import cis501.Direction;

public class DirPredGshare extends DirPredBimodal {
	private int indexBits;
	private int[] bht;
	private long histReg;
	private long histRegMask;
	final int STRONGLY_NOT_TAKEN = 0;
	final int WEAKLY_NOT_TAKEN = 1;
	final int WEAKLY_TAKEN = 2;
	final int STRONGLY_TAKEN = 3;

	public DirPredGshare(int indexBits, int historyBits) {
		super(indexBits);
		bht = new int[(int) Math.pow((double) indexBits, 2.0)];
		histRegMask = (long) Math.pow((double) historyBits, 2.0) - 1;
		this.indexBits = indexBits;
		histReg = 0;

	}

	@Override
	public Direction predict(long pc) {
		// index into bht
		long newpc = pc ^ histReg;
		int index = (int) (newpc & (int) Math.pow((double) indexBits, 2.0) - 1); // masking
																					// to
																					// get
		switch (bht[index]) {
		case STRONGLY_NOT_TAKEN:
		case WEAKLY_NOT_TAKEN:
			return Direction.NotTaken;
		case WEAKLY_TAKEN:
		case STRONGLY_TAKEN:
			return Direction.Taken;
		default:
			return null;
		}

	}

	@Override
	public void train(long pc, Direction actual) {
		int index = (int) ((pc ^ histReg) & (int) Math.pow((double) indexBits, 2.0) - 1);
		// update register
		histReg = histReg << 1;
		histReg = histReg & histRegMask;
		// update register LSB and BHT
		switch (actual) {
		case Taken:
			histReg = histReg | 1;
			if (bht[index] != STRONGLY_TAKEN) {
				bht[index]++;
			}
			break;
		case NotTaken:
			// then 0 already at LSB
			if (bht[index] != STRONGLY_NOT_TAKEN) {
				bht[index]--;
			}

		}
	}
}
