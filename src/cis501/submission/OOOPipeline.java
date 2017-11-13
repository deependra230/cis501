package cis501.submission;

import cis501.ICache;
import cis501.IOOOPipeline;
import cis501.InsnIterator;

public class OOOPipeline implements IOOOPipeline {

    /**
     * The number of architectural registers in ARMv7. There are 16 general-purpose registers, +1
     * for condition codes.
     */
    public static final int NUM_ARCH_REGS = 17;

    /** Treat condition codes as architectural register "16". */
    public static final short COND_CODE_ARCH_REG = 16;

    public OOOPipeline(int pregs, int robSize, int issueQSize, ICache ic, ICache dc) {
        assert pregs >= NUM_ARCH_REGS + 2;
        assert robSize > 0;
        assert issueQSize > 0;
    }

    @Override
    public String[] groupMembers() {
        return new String[]{"your", "names"};
    }

    @Override
    public void run(InsnIterator uiter) {

    }

    @Override
    public long getInsns() {
        return 0;
    }

    @Override
    public long getCycles() {
        return 0;
    }

}
