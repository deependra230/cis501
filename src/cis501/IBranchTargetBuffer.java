package cis501;

public interface IBranchTargetBuffer {

    /**
     * @param pc     the program counter of the insn needing prediction
     * @return the predicted target of the insn, returns 0 if there's no entry in the BTB
     */
    public long predict(long pc);

    /**
     * @param pc     the program counter of the branch to train
     * @param actual the true taken target of the branch
     */
    public void train(long pc, long actual);

}
