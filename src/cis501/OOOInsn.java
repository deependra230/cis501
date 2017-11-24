package cis501;

import java.util.*;

public class OOOInsn {
    public static final short COND_CODE_ARCH_REG = 16;
    private static final short NO_REGISTER = -1;

    private Insn insn;

    private PhysReg inp1;
    private PhysReg inp2;
    private PhysReg inp3;
    private PhysReg oldOut1;
    private PhysReg oldOut2;
    private PhysReg out1;
    private PhysReg out2;
    private boolean readyToCommit;
    private int bday;

    public OOOInsn(Insn insn, int bday) {
        this.insn = insn;

        this.readyToCommit = false;
        this.bday = bday;
    }

    public int getBday() {
        return bday;
    }

    public MemoryOp getMemoryOp() {
        return this.insn.mem;
    }

    public long getMemAddress() {
        return this.insn.memAddress;
    }

    public short getMemAccessBytes() {
        return this.insn.memAccessBytes;
    }

    public boolean isReadyToCommit() {
        return this.readyToCommit;
    }

    public void setReadyToCommit(boolean readyToCommit) {
        this.readyToCommit = readyToCommit;
    }

    public List<PhysReg> getOldOuts() {
        List<PhysReg> oldOuts = new ArrayList<>();
        if (this.oldOut1 != null) {
            oldOuts.add(oldOut1);
        }
        if (this.oldOut2 != null) {
            oldOuts.add(oldOut2);
        }
        return oldOuts;
    }

    public List<PhysReg> getDestRegs() {
        List<PhysReg> outs = new ArrayList<>();
        if (this.out1 != null) {
            outs.add(out1);
        }
        if (this.out2 != null) {
            outs.add(out2);
        }
        return outs;
    }

    public List<PhysReg> getInpRegs() {
        List<PhysReg> inps = new ArrayList<>();
        if (this.inp1 != null) {
            inps.add(inp1);
        }
        if (this.inp2 != null) {
            inps.add(inp2);
        }
        if (this.inp3 != null) {
            inps.add(inp3);
        }
        return inps;
    }

    public void renameInpRegs(List<PhysReg> mapTable) {
        if (insn.srcReg1 != NO_REGISTER) {
            inp1 = mapTable.get(insn.srcReg1);
        }
        if (insn.srcReg2 != NO_REGISTER) {
            inp2 = mapTable.get(insn.srcReg2);
        }
        if ((insn.condCode == CondCodes.ReadCC) || (insn.condCode == CondCodes.ReadWriteCC)) {
            inp3 = mapTable.get(COND_CODE_ARCH_REG);
        }
    }

    public int getNumOutRegs() {
        int numOutRegs = 0;
        if (insn.dstReg != NO_REGISTER) {
            numOutRegs += 1;
        }
        if ((insn.condCode == CondCodes.WriteCC) || (insn.condCode == CondCodes.ReadWriteCC)) {
            numOutRegs += 1;
        }
        return numOutRegs;
    }

    public void recordOldOutRegs(List<PhysReg> mapTable) {
        if (insn.dstReg != NO_REGISTER) {
            oldOut1 = mapTable.get(insn.dstReg);
        }
        if ((insn.condCode == CondCodes.WriteCC) || (insn.condCode == CondCodes.ReadWriteCC)) {
            oldOut2 = mapTable.get(COND_CODE_ARCH_REG);
        }
    }

    public void allocateOutRegs(List<PhysReg> mapTable, LinkedList<PhysReg> freeList) {
        if (insn.dstReg != NO_REGISTER) {
            out1 = freeList.pop();
            mapTable.set(insn.dstReg, out1);
        }
        if ((insn.condCode == CondCodes.WriteCC) || (insn.condCode == CondCodes.ReadWriteCC)) {
            out2 = freeList.pop();
            mapTable.set(COND_CODE_ARCH_REG, out2);
        }
    }

    public long getPC() {
        return insn.pc;
    }
}
