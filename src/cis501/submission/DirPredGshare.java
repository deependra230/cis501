package cis501.submission;

import cis501.Direction;

public class DirPredGshare extends DirPredBimodal {
	private int numHistoryBits;
	private long branchHistoryRegister;
	private long branchHistoryRegisterMask;


	public DirPredGshare(int indexBits, int historyBits) {
		super(indexBits);

		this.numHistoryBits = historyBits;
		this.branchHistoryRegister = 0L;
		this.branchHistoryRegisterMask = (long) Math.pow(2, historyBits) - 1;
	}

	@Override
	public Direction predict(long pc) {
		// index into bht
		return super.predict(pc ^ branchHistoryRegister);
	}

	@Override
	public void train(long pc, Direction actual) {
        super.train(pc ^ branchHistoryRegister, actual);

        // update register
		branchHistoryRegister = branchHistoryRegister << 1;
		// update register LSB and BHT
		if (actual == Direction.Taken) {
			branchHistoryRegister = branchHistoryRegister | 1;
		}

		branchHistoryRegister = branchHistoryRegister & branchHistoryRegisterMask;
	}
}
