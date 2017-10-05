package cis501;

import java.util.StringTokenizer;

/** Class representing a single micro-op. */
public class Insn {

    /** Destination register. Value will be in range [-1,15]. */
    public final short dstReg;

    /**
     * Input 1 for ALU ops, or address for loads, value-to-be-stored for stores. Value will be in
     * range [-1,15].
     */
    public final short srcReg1;

    /** Input 2 for ALU ops, or address for stores. Value will be in range [-1,15]. */
    public final short srcReg2;

    public final long pc;
    public final short insnSizeBytes;

    public final BranchType branchType;
    public final Direction branchDirection;
    public final long branchTarget;
    public final CondCodes condCode;

    public final MemoryOp mem;
    public final long memAddress;
    public final short memAccessBytes;

    public final String asm;

    /** Parse an insn from a line in the trace file */
    public Insn(String line) {
        StringTokenizer st = new StringTokenizer(line, "\t");
        // asm string may contain commas and be split into multiple tokens
        assert st.countTokens() == 13;

        try {
            this.pc = Long.parseLong(st.nextToken(), 16);
            this.insnSizeBytes = Short.parseShort(st.nextToken());
            this.branchType = BranchTypeOfString(st.nextToken());
            this.branchDirection = BranchOfChar(st.nextToken().charAt(0));
            this.branchTarget = Long.parseLong(st.nextToken(), 16);
            this.mem = MemOfChar(st.nextToken().charAt(0));
            this.memAddress = Long.parseLong(st.nextToken(), 16);
            this.memAccessBytes = Short.parseShort(st.nextToken());
            this.condCode = CondCodeOfChar(st.nextToken().charAt(0));
            this.dstReg = parseReg(st.nextToken());
            this.srcReg1 = parseReg(st.nextToken());
            this.srcReg2 = parseReg(st.nextToken());
            this.asm = st.nextToken();
        } catch (Exception e) {
            System.out.println("Error parsing insn: " + line);
            throw e;
        }
    }

    /** Create an insn directly. Used for testing. */
    public Insn(int dr, int sr1, int sr2,
                long pc, int isize,
                Direction dir, long branchTarget, CondCodes cc,
                MemoryOp mop, long memAddr, int msize,
                String asm) {
        this.dstReg = (short) dr;
        this.srcReg1 = (short) sr1;
        this.srcReg2 = (short) sr2;
        this.pc = pc;
        this.insnSizeBytes = (short) isize;
        this.branchType = (null == dir) ? null : BranchType.ConditionalDirect;
        this.branchDirection = dir;
        this.branchTarget = branchTarget;
        this.condCode = cc;
        this.mem = mop;
        this.memAddress = memAddr;
        this.memAccessBytes = (short) msize;
        this.asm = asm;
    }

    private static short parseReg(String r) {
        switch (r) {
            case "  _":
                return -1;
            case " r0":
                return 0;
            case " r1":
                return 1;
            case " r2":
                return 2;
            case " r3":
                return 3;
            case " r4":
                return 4;
            case " r5":
                return 5;
            case " r6":
                return 6;
            case " r7":
                return 7;
            case " r8":
                return 8;
            case " r9":
                return 9; // sb
            case "r10":
                return 10; // sl
            case "r11":
                return 11; // fp
            case "r12":
                return 12; // ip
            case " sp":
                return 13;
            case " lr":
                return 14;
            case " pc":
                return 15;
            default:
                throw new IllegalArgumentException("Invalid reg: " + r);
        }
    }

    private static String stringOfReg(short reg) {
        switch (reg) {
            case -1:
                return "  _";
            case 0:
                return " r0";
            case 1:
                return " r1";
            case 2:
                return " r2";
            case 3:
                return " r3";
            case 4:
                return " r4";
            case 5:
                return " r5";
            case 6:
                return " r6";
            case 7:
                return " r7";
            case 8:
                return " r8";
            case 9:
                return " r9";
            case 10:
                return "r10";
            case 11:
                return "r11";
            case 12:
                return "r12";
            case 13:
                return " sp";
            case 14:
                return " lr";
            case 15:
                return " pc";
            default:
                throw new IllegalArgumentException("Invalid register: " + reg);
        }
    }

    private static CondCodes CondCodeOfChar(char c) {
        switch (c) {
            case 'R':
                return CondCodes.ReadCC;
            case 'W':
                return CondCodes.WriteCC;
            case 'B':
                return CondCodes.ReadWriteCC;
            case '_':
                return null; // ignore condition codes
            default:
                throw new IllegalArgumentException("Invalid cond code type: " + c);
        }
    }

    private static Direction BranchOfChar(char c) {
        switch (c) {
            case 'T':
                return Direction.Taken;
            case 'N':
                return Direction.NotTaken;
            case '_':
                return null;
            default:
                throw new IllegalArgumentException("Invalid branch direction: " + c);
        }
    }

    private static BranchType BranchTypeOfString(String s) {
        switch (s) {
            case "CD": return BranchType.ConditionalDirect;
            case "CI": return BranchType.ConditionalIndirect;
            case "UD": return BranchType.UnconditionalDirect;
            case "UI": return BranchType.UnconditionalIndirect;
            case " _": return null;
            default:
                throw new IllegalArgumentException("Invalid branch type: " + s);
        }
    }

    private static MemoryOp MemOfChar(char c) {
        switch (c) {
            case 'L':
                return MemoryOp.Load;
            case 'S':
                return MemoryOp.Store;
            case '_':
                return null;
            default:
                throw new IllegalArgumentException("Invalid mem op: " + c);
        }
    }

    public long fallthroughPC() {
        return this.pc + this.insnSizeBytes;
    }

    private char condCodeChar() {
        if (null == this.condCode) return '_';
        switch (this.condCode) {
            case ReadCC:
                return 'R';
            case WriteCC:
                return 'W';
            case ReadWriteCC:
                return 'B';
            default:
                throw new IllegalArgumentException("Invalid condcode field: " + this.toString());
        }
    }

    private String branchTypeString() {
        if (null == this.branchType) return " _";
        switch (this.branchType) {
            case ConditionalDirect:
                return "CD";
            case ConditionalIndirect:
                return "CI";
            case UnconditionalDirect:
                return "UD";
            case UnconditionalIndirect:
                return "UI";
            default:
                throw new IllegalArgumentException("Invalid branch type: " + this.toString());
        }
    }

    private char branchDirectionChar() {
        if (null == this.branchDirection) return '_';
        switch (this.branchDirection) {
            case NotTaken:
                return 'N';
            case Taken:
                return 'T';
            default:
                throw new IllegalArgumentException("Invalid branch direction: " + this.toString());
        }
    }

    private char memChar() {
        if (null == this.mem) return '_';
        switch (this.mem) {
            case Load:
                return 'L';
            case Store:
                return 'S';
            default:
                throw new IllegalArgumentException("Invalid mem field: " + this.toString());
        }
    }

    public String toTraceLine() {
        return String.format("%08x\t%d\t%s\t%c\t%08x\t%c\t%08x\t%d\t%c\t%s\t%s\t%s\t%s%n",
                this.pc, this.insnSizeBytes,
                branchTypeString(), branchDirectionChar(), this.branchTarget,
                memChar(), this.memAddress, this.memAccessBytes,
                condCodeChar(),
                stringOfReg(this.dstReg), stringOfReg(this.srcReg1), stringOfReg(this.srcReg2),
                this.asm);
    }

    @Override
    public String toString() {
        return String.format("dst:%d src1:%d src2:%d pc:%x isize:%d btype:%s bdir:%s btarg:%x cc:%s mem:%s maddr:%x msize:%d %s",
                dstReg, srcReg1, srcReg2,
                pc, insnSizeBytes,
                branchType, branchDirection, branchTarget, condCode,
                mem, memAddress, memAccessBytes,
                asm);
    }

}
