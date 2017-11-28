package cis501.submission;

import cis501.*;

import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

enum FrontendStage {
    FETCH(0), DECODE(1), RENAME(2), DISPATCH(3);

    private static FrontendStage[] vals = values();
    private final int index;

    FrontendStage(int idx) {
        this.index = idx;
    }

    /** Returns the index of this stage within the pipeline */
    public int i() {
        return index;
    }

    /** Returns the next stage in the pipeline, e.g., next after Fetch is Decode */
    public FrontendStage next() {
        return vals[(this.ordinal() +1) % vals.length];
    }
}

enum BackendStage {
    REGISTERREAD(0), EXECUTE(1), MEMORY(2), WRITEBACK(3);

    private static BackendStage[] vals = values();
    private final int index;

    BackendStage(int idx) {
        this.index = idx;
    }

    /** Returns the index of this stage within the pipeline */
    public int i() {
        return index;
    }

    /** Returns the next stage in the pipeline, e.g., next after Fetch is Decode */
    public BackendStage next() {
        return vals[(this.ordinal() +1) % vals.length];
    }
}

public class OOOPipeline implements IOOOPipeline {

    /**
     * The number of architectural registers in ARMv7. There are 16 general-purpose registers, +1
     * for condition codes.
     */
    public static final int NUM_ARCH_REGS = 17;

    /** Treat condition codes as architectural register "16". */
    public static final short COND_CODE_ARCH_REG = 16;
    private static final short NO_REGISTER = -1;

    private LinkedList<PhysReg> freeList;
    private List<PhysReg> mapTable;
    private Map<PhysReg, Boolean> readyTable;

    private int robSize;
    private int issueQSize;
    private BlockingDeque<OOOInsn> rob;
    private BlockingDeque<OOOInsn> issueQueue;

    private List<OOOInsn> frontendPipeline;
    private List<OOOInsn> backendPipeline;

    private ICache insnCache;
    private ICache dataCache;

    private long numInsns = 0;
    private long numCycles = 0;

    // Auxiliary tracking variables
    private OOOInsn freeFromROB = null;
    private OOOInsn freeFromIQ = null;

    private int icMemLatency = 0;
    private int dcMemLatency = 0;



    public OOOPipeline(int pregs, int robSize, int issueQSize, ICache ic, ICache dc) {
        assert pregs >= NUM_ARCH_REGS + 2;
        assert robSize > 0;
        assert issueQSize > 0;

        // arch regs 0-16 will be mapped to phys reg 0-16, respectively, at the beginning
        initializeMapTable();

        // 0-16 will not be in the free-list in the beginning
        initializeFreeList(pregs);

        // all physical insns will eb ready at the beginning
        initializeReadyTable(pregs);

        this.robSize = robSize;
        this.issueQSize = issueQSize;
        rob = new LinkedBlockingDeque(robSize);
        issueQueue = new LinkedBlockingDeque(issueQSize);

        // all stages are set to null
        initializePipelines();

        //handle caches later
        insnCache = ic;
        dataCache = dc;
    }

    @Override
    public String[] groupMembers() {
        return new String[]{"Clinton Nyabuto", "Deependra Singh"};
    }

    @Override
    public void run(InsnIterator uiter) {
        while(uiter.hasNext() || !isPipelineEmpty()) {
            /*
            * 1. Commit from ROB if the head is done
            * 2. Advance the backend out-of-order pipeline
            * 3. Handle Issue logic
            * 4. Advance the frontend inorder pipeline
            * 5. Fetch the next insn
            *
            * Order of the functions is not exactly as listed above. Because we want to assert that the relevant
            * hardware structures such as freed physical register, rob entry, issueq entry, etc. becomes available
            * only in the next cycle to be used by other insn
            * */

            handleCommit();
            advanceBackendPipeline();
            handleIssue();
            advanceFrontendPipeline(uiter);

            freeHardware();

            numCycles += 1;
        }
        numCycles += 1;
    }

    @Override
    public long getInsns() {
        return this.numInsns;
    }

    @Override
    public long getCycles() {
        return this.numCycles;
    }

    private void handleCommit() {
         OOOInsn insn = rob.peek();
         if (insn != null && insn.isReadyToCommit()) {
             freeFromROB = insn;
         } else {
             freeFromROB = null;
         }
    }

    private void advanceBackendPipeline() {
        handleWriteback();
        handleMemory();
        handleExecute();
        handleRegisterRead();
    }

    private void handleIssue() {
        OOOInsn insn = findOldestReadyInsn();
        if (insn != null && !hasInsn(BackendStage.REGISTERREAD)) {
            freeFromIQ = insn;
        } else {
            freeFromIQ = null;
        }
    }

    private void advanceFrontendPipeline(InsnIterator uiter) {
        handleDispatch();
        handleRegisterRename();
        handleDecode();
        handleFetch(uiter);
    }

    private void freeHardware() {
        // free from ROB
        if (freeFromROB != null) {
            freePhysicalRegs(freeFromROB);
            rob.remove();
        }

        // free from IQ
        if (freeFromIQ != null) {
            backendPipeline.set(BackendStage.REGISTERREAD.i(), freeFromIQ);
            if (freeFromIQ.getMemoryOp() != MemoryOp.Load) {
                // non-load insns mark their destination regs ready upon issue
                wakeupReadyTable(freeFromIQ);
            }
            issueQueue.remove(freeFromIQ);
        }
    }

    private void fetchNextInsn(InsnIterator uiter) {
        if (!uiter.hasNext() || hasInsn(FrontendStage.FETCH) || (rob.size() == robSize)) {
            return;
        }

        OOOInsn insn = new OOOInsn(uiter.next(), (int) numInsns + 1);
        frontendPipeline.set(FrontendStage.FETCH.i(), insn);
        icMemLatency = calculateInsnCacheMemLatency(insn);
        boolean success = rob.offer(insn);
        if (!success) {
            throw new IllegalStateException("Should never reach here");
        }

        this.numInsns += 1;
    }

    private void handleFetch(InsnIterator uiter) {
        fetchNextInsn(uiter);
        if (hasInsn(FrontendStage.FETCH) && icMemLatency >= 1) {
            icMemLatency -= 1;
        }
        if (hasInsn(FrontendStage.FETCH) && (icMemLatency == 0) && !hasInsn(FrontendStage.DECODE)) {
            frontendPipeline.set(FrontendStage.DECODE.i(), frontendPipeline.get(FrontendStage.FETCH.i()));
            frontendPipeline.set(FrontendStage.FETCH.i(), null);
        }
    }

    private void handleDecode() {
        if (hasInsn(FrontendStage.DECODE) && !hasInsn(FrontendStage.RENAME)) {
            frontendPipeline.set(FrontendStage.RENAME.i(), frontendPipeline.get(FrontendStage.DECODE.i()));
            frontendPipeline.set(FrontendStage.DECODE.i(), null);
        }
    }

    private void handleRegisterRename() {
        OOOInsn insn = frontendPipeline.get(FrontendStage.RENAME.i());
        if (insn == null || insn.getNumOutRegs() > freeList.size() || hasInsn(FrontendStage.DISPATCH)) {
            return;
        }
        insn.renameInpRegs(mapTable);
        insn.recordOldOutRegs(mapTable);
        insn.allocateOutRegs(mapTable, freeList);

        frontendPipeline.set(FrontendStage.DISPATCH.i(), insn);
        frontendPipeline.set(FrontendStage.RENAME.i(), null);
    }

    private void handleDispatch() {
        OOOInsn insn = frontendPipeline.get(FrontendStage.DISPATCH.i());
        if (insn == null) {
            return;
        }
        if (issueQueue.offer(insn)) {
            clearReadyTable(insn);
            frontendPipeline.set(FrontendStage.DISPATCH.i(), null);
        }
    }

    private void handleRegisterRead() {
        if (hasInsn(BackendStage.REGISTERREAD) && !hasInsn(BackendStage.EXECUTE)) {
            backendPipeline.set(BackendStage.EXECUTE.i(), backendPipeline.get(BackendStage.REGISTERREAD.i()));
            backendPipeline.set(BackendStage.REGISTERREAD.i(), null);
        }
    }

    private void handleExecute() {
        if (hasInsn(BackendStage.EXECUTE) && !hasInsn(BackendStage.MEMORY)) {
            OOOInsn insn = backendPipeline.get(BackendStage.EXECUTE.i());
            backendPipeline.set(BackendStage.MEMORY.i(), insn);
            dcMemLatency = calculateDataCacheMemLatency(insn);
            backendPipeline.set(BackendStage.EXECUTE.i(), null);
        }
    }

    private void handleMemory() {
        if (hasInsn(BackendStage.MEMORY) && dcMemLatency >= 1) {
            dcMemLatency -= 1;
        }
        if (hasInsn(BackendStage.MEMORY) && (dcMemLatency == 0) && !hasInsn(BackendStage.WRITEBACK)) {
            backendPipeline.set(BackendStage.WRITEBACK.i(), backendPipeline.get(BackendStage.MEMORY.i()));
            backendPipeline.set(BackendStage.MEMORY.i(), null);
        }
    }

    private void handleWriteback() {
        if (hasInsn(BackendStage.WRITEBACK)) {
            OOOInsn insn = backendPipeline.get(BackendStage.WRITEBACK.i());
            if (insn.getMemoryOp() == MemoryOp.Load) {
                // load insn mark its destination regs ready in WB stage
                wakeupReadyTable(insn);
            }
            insn.setReadyToCommit(true);
            backendPipeline.set(BackendStage.WRITEBACK.i(), null);
        }
    }

    private OOOInsn findOldestReadyInsn() {
        for (OOOInsn insn: issueQueue) {
            if (isInsnReadyForIssue(insn)) {
                return insn;
            }
        }
        return null;
    }

    private boolean isInsnReadyForIssue(OOOInsn insn) {
        // register dependencies
        List<PhysReg> inpRegs = insn.getInpRegs();
        for (PhysReg physReg: inpRegs) {
            if (!readyTable.get(physReg)) {
                return false;
            }
        }

        boolean aliased = false;
        // load store aliasing
        if (insn.getMemoryOp() == MemoryOp.Load) {
            aliased = isLoadStoreAliasing(insn);
        }
        return !aliased;
    }

    private boolean isLoadStoreAliasing(OOOInsn loadInsn) {
        int loadBday = loadInsn.getBday();
        long l1 = loadInsn.getMemAddress();
        long l2 = l1 + loadInsn.getMemAccessBytes() - 1; // inclusive
        for (OOOInsn insn: issueQueue) {
            if ((insn.getBday() >= loadBday) || (insn.getMemoryOp() != MemoryOp.Store)) {
                continue;
            }
            long s1 = insn.getMemAddress();
            long s2 = s1 + insn.getMemAccessBytes() - 1; // inclusive
            if (((s1 >= l1) && (s1 <= l2)) || ((s2 >= l1) && (s2 <= l2))) {
                return true;
            }
        }
        return false;
    }

    private void clearReadyTable(OOOInsn insn) {
        // mark dest regs not ready
        List<PhysReg> destRegs = insn.getDestRegs();
        for (PhysReg physReg: destRegs) {
            readyTable.put(physReg, Boolean.FALSE);
        }
    }

    private void wakeupReadyTable(OOOInsn insn) {
        // mark destination registers ready
        List<PhysReg> destRegs = insn.getDestRegs();
        for (PhysReg physReg: destRegs) {
            readyTable.put(physReg, Boolean.TRUE);
        }
    }

    private boolean hasInsn(BackendStage stage) {
        if (backendPipeline.get(stage.i()) == null) {
            return false;
        }
        return true;
    }

    private boolean hasInsn(FrontendStage stage) {
        if (frontendPipeline.get(stage.i()) == null) {
            return false;
        }
        return true;
    }

    private void freePhysicalRegs(OOOInsn insn) {
        freeList.addAll(insn.getOldOuts());
    }

    private boolean isPipelineEmpty() {
        if (rob.size() == 0) {
            return true;
        }
        return false;
    }

    // Initially, arch regs 0-16 will be mapped to phys regs 0-16
    private void initializeMapTable() {
        mapTable = new ArrayList<>();
        for (int i = 0; i < NUM_ARCH_REGS; i++) {
            mapTable.add(new PhysReg(i));
        }
    }

    // Initially, all the physical regs that are not mapped to arch regs,
    // i.e. everything except 0-16 will be on the free list.
    private void initializeFreeList(int numPregs) {
        freeList = new LinkedList<>();
        for (int i = NUM_ARCH_REGS; i < numPregs; i++) {
            freeList.add(new PhysReg(i));
        }
    }

    // All physical regs are ready at the beginning
    private void initializeReadyTable(int numPregs) {
        readyTable = new HashMap<>();
        for (int i = 0; i < numPregs; i++) {
            readyTable.put(new PhysReg(i), Boolean.TRUE);
        }
    }

    // initialize both front-end and back-end pipelines
    private void initializePipelines() {
        frontendPipeline = new ArrayList<>();
        backendPipeline = new ArrayList<>();
        for (FrontendStage stage: FrontendStage.values()) {
            frontendPipeline.add(null);
        }
        for (BackendStage stage: BackendStage.values()) {
            backendPipeline.add(null);
        }
    }

    private int calculateInsnCacheMemLatency(OOOInsn insn) {
        // insn will never be null
        if (this.insnCache == null) {
            return 1;
        } else {
            // will always be only load
            return 1 + insnCache.access(true, insn.getPC());
        }
    }

    private int calculateDataCacheMemLatency(OOOInsn insn) {
        // insn will never be null
        if (this.dataCache == null) {
            return 1;
        }

        MemoryOp mem = insn.getMemoryOp();
        if (mem == MemoryOp.Load) {
            return 1 + dataCache.access(true, insn.getMemAddress());
        } else if (mem == MemoryOp.Store) {
            return 1 + dataCache.access(false, insn.getMemAddress());
        } else {
            return 1;
        }
    }

}
