package cis501;


public interface IInorderPipeline {

    /** @return the names of the group members for this assignment. */
    public String[] groupMembers();

    /**
     * Run the pipeline simulation over the given sequence of insns. The simulation should stop as
     * soon as the last insn exits the pipeline.
     */
    public void run(InsnIterator iiter);

    /** @return the number of insns executed so far */
    public long getInsns();

    /**
     * @return the number of cycles this pipeline has executed, i.e., the number of cycles it takes
     * for the last insn to leave the pipeline.
     */
    public long getCycles();

}
