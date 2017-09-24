package cis501.submission;

import cis501.BranchType;
import cis501.ITraceAnalyzer;
import cis501.Insn;
import cis501.MemoryOp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TraceAnalyzer implements ITraceAnalyzer {

    private static final int DIRECT_BRANCH_OFFSET_BITS_VALUES = 32;

    private static final int THUMB_INSN_BYTE_SIZE = 2;
    private static final int ARM_INSN_BYTE_SIZE = 4;

    private static final int INSN_CATEGORY_TYPES = 5;
    private static final String UNCONDITIONAL_BRANCH_INSN_TYPE_STRING = "unconditionalbranch";
    private static final String CONDITIONAL_BRANCH_INSN_TYPE_STRING = "conditionalbranch";
    private static final String OTHER_INSN_TYPE_STRING = "other";

    private int totalInsnsCount;
    private int thumbInsnsCount;
    private int ARMInsnsCount;
    private String mostCommonInsnCategory;
    private int[] directBranchOffsetBitsCounts;

    public TraceAnalyzer() {
        this.totalInsnsCount = 0;
        this.thumbInsnsCount = 0;
        this.ARMInsnsCount = 0;
        this.directBranchOffsetBitsCounts = new int[DIRECT_BRANCH_OFFSET_BITS_VALUES];
    }

    @Override
    public String author() {
        return "Deependra Singh";
    }

    @Override
    public void run(Iterable<Insn> iiter) {
        Map<String, Integer> insnCategoryCountsMap = new HashMap<>(INSN_CATEGORY_TYPES);
        initializeInsnCategoryCountsMap(insnCategoryCountsMap);

        for (Insn insn : iiter) {
            totalInsnsCount++;

            // Increment the count of THUMB and ARM instructions accordingly
            if (insn.insnSizeBytes == THUMB_INSN_BYTE_SIZE) {
                thumbInsnsCount += 1;
            } else if (insn.insnSizeBytes == ARM_INSN_BYTE_SIZE) {
                ARMInsnsCount += 1;
            }

            // Increment the count for each instruction category accordingly
            String category = OTHER_INSN_TYPE_STRING;
            if (insn.mem != null) {
                category = insn.mem.toString().toLowerCase();
            } else if (insn.branchType != null) {
                category = getBranchTypeCategory(insn.branchType);
            }
            insnCategoryCountsMap.replace(category, insnCategoryCountsMap.get(category) + 1);


            // Increment the count of branch-direct instructions for each possible offset size
            if ((insn.branchType == BranchType.UnconditionalDirect) || (insn.branchType == BranchType.ConditionalDirect)) {
                int bitsForBranchOffset = calculateBitsForBranchOffset(insn.pc, insn.branchTarget);
                directBranchOffsetBitsCounts[bitsForBranchOffset - 1] += 1;
            }
        }

        // Find the most frequent instruction category and store it in the class variable
        mostCommonInsnCategory = calculateMostCommonInsnCategory(insnCategoryCountsMap);

        // Update the partial sum in the directBranchOffsetBitsCounts array in order to get the fraction
        calculateAndUpdatePartialSums(directBranchOffsetBitsCounts);
    }

    @Override
    public double avgInsnSize() {
        int sizeOfThumbInsns = thumbInsnsCount * THUMB_INSN_BYTE_SIZE;
        int sizeOfARMInsns = ARMInsnsCount * ARM_INSN_BYTE_SIZE;
        return (double) (sizeOfThumbInsns + sizeOfARMInsns) / totalInsnsCount;
    }

    @Override
    public double insnBandwidthIncreaseWithoutThumb() {
        int sizeOfThumbInsns = thumbInsnsCount * THUMB_INSN_BYTE_SIZE;
        int sizeOfARMInsns = ARMInsnsCount * ARM_INSN_BYTE_SIZE;
        return (double) ((2 * sizeOfThumbInsns) + sizeOfARMInsns) / (sizeOfThumbInsns + sizeOfARMInsns);
    }

    @Override
    public String mostCommonInsnCategory() {
        return mostCommonInsnCategory;
    }

    @Override
    public double fractionOfDirectBranchOffsetsLteNBits(int bits) {
        return (double) directBranchOffsetBitsCounts[bits - 1]/directBranchOffsetBitsCounts[DIRECT_BRANCH_OFFSET_BITS_VALUES - 1];
    }

    public int getNumberOfInsns() {
        return totalInsnsCount;
    }

    //Initializes the insnCategoryCountsMap with 0
    private void initializeInsnCategoryCountsMap(Map<String, Integer> insnCategoryCountsMap) {
        for (MemoryOp memoryOp : MemoryOp.values()) {
            insnCategoryCountsMap.put(memoryOp.toString().toLowerCase(), 0);
        }
        insnCategoryCountsMap.put(CONDITIONAL_BRANCH_INSN_TYPE_STRING, 0);
        insnCategoryCountsMap.put(UNCONDITIONAL_BRANCH_INSN_TYPE_STRING, 0);
        insnCategoryCountsMap.put(OTHER_INSN_TYPE_STRING, 0);
    }
    // Converts BranchType to appropriate string for branchTypeCategoryCounts
    private String getBranchTypeCategory(BranchType branchType) {
        if (branchType == null) {
            return null;
        }

        if ((branchType == BranchType.ConditionalDirect) || (branchType == BranchType.ConditionalIndirect)) {
            return CONDITIONAL_BRANCH_INSN_TYPE_STRING;
        }

        return UNCONDITIONAL_BRANCH_INSN_TYPE_STRING;
    }

    // Iterates over the <categoryType, count> map in order to retrieve the category with largest number of instructions
    private String calculateMostCommonInsnCategory(Map<String, Integer> insnCategoryCountsMap) {
        Iterator<Map.Entry<String, Integer>> insnCategoryCountsIterator = insnCategoryCountsMap.entrySet().iterator();
        Map.Entry<String, Integer> currentCategoryCountEntry = insnCategoryCountsIterator.next();
        Map.Entry<String, Integer> maxCategoryCountEntry = currentCategoryCountEntry;
        while (insnCategoryCountsIterator.hasNext()) {
            currentCategoryCountEntry = insnCategoryCountsIterator.next();
            if (currentCategoryCountEntry.getValue() > maxCategoryCountEntry.getValue()) {
                maxCategoryCountEntry = currentCategoryCountEntry;
            }
        }
        return maxCategoryCountEntry.getKey();
    }

    // Computes the bits needed to encode the branch offset according to the formula: 2 + floor( log2(abs(PC - BranchTarget)))
    private int calculateBitsForBranchOffset(long pc, long branchTarget){
        return (int) ( 2 + Math.floor( Math.log( Math.abs(pc - branchTarget) ) / Math.log(2) ));
    }

    /*
    Computes the partial sums for the sequence defined by the input array.
    For e.g., [1, 2, 3] would result in [1, 3, 6]
     */
    private void calculateAndUpdatePartialSums(int[] directBranchOffsetBitsCounts){
        for(int bits = 1; bits < DIRECT_BRANCH_OFFSET_BITS_VALUES; bits++) {
            directBranchOffsetBitsCounts[bits] += directBranchOffsetBitsCounts[bits-1];
        }
    }
}
