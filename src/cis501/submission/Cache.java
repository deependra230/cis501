package cis501.submission;

import cis501.ICache;

import java.util.List;

public class Cache implements ICache {
    private int indexBits;
    private int ways;
    private int blockOffsetBits;
    private int tagBits;

    private final int accessLatency;
    private final int cleanMissLatency;
    private final int dirtyMissLatency;

    int[][] tagArray;
    boolean[][] validBits;
    boolean[][] dirtyBits;
    int[][] LRUStates;

    public Cache(int indexBits, int ways, int blockOffsetBits,
                 final int accessLatency, final int cleanMissLatency, final int dirtyMissLatency) {
        assert indexBits >= 0;
        assert ways > 0;
        assert blockOffsetBits >= 0;
        assert indexBits + blockOffsetBits < 32;
        assert accessLatency >= 0;
        assert cleanMissLatency >= 0;
        assert dirtyMissLatency >= 0;

        this.indexBits = indexBits;
        this.ways = ways;
        this.blockOffsetBits = blockOffsetBits;
        this.tagBits = 32 - indexBits - blockOffsetBits;

        this.accessLatency = accessLatency;
        this.cleanMissLatency = cleanMissLatency;
        this.dirtyMissLatency = dirtyMissLatency;

        this.tagArray = new int[1 << indexBits][ways];
    
        
        this.validBits = new boolean[1 << indexBits][ways];
        this.dirtyBits = new boolean[1 << indexBits][ways];
        this.LRUStates = new int[1 << indexBits][ways];
    }

    @Override
    public int access(boolean load, long address) {
    
        int addrIndex = extractIndexBits(address);
        int addrTag = extractTagBits(address);
        int addrWay = whichWayInCache(addrIndex, addrTag);
        if (addrWay != -1) {
            /*
            * We have a cache hit (tag matched and validBit = 1)
            * 1. update LRU state
            * 2. if load: nothing else to do
            *       if store: update the dirty bit to 1
            * */
            updateLRUState(addrIndex, addrWay);
            if (!load) {
                dirtyBits[addrIndex][addrWay] = true;
            }
            return accessLatency;
        } else {
            /*
            * We have a cache miss
            * 1. find the eviction candidate, record whether it is clean or dirty --- accordingly return the latency
            * 2. update the LRU_State
            * 3. if load: bring in the new block, i.e. update the tag value, set the valid bit to 1, set the dirty bit to 0
            *       if store: bring in the new block, i.e. update the tag value, set the valid bit to 1, set the dirty bit to 1
            * */
            int evictionCandidateWay = getMinElementIndexArray(LRUStates[addrIndex]);
            
            boolean dirtyBitValue = dirtyBits[addrIndex][evictionCandidateWay];
            boolean validBitValue = validBits[addrIndex][evictionCandidateWay];
            updateLRUState(addrIndex, evictionCandidateWay);
            tagArray[addrIndex][evictionCandidateWay] = addrTag;
            validBits[addrIndex][evictionCandidateWay] = true;
            if (load) {
                dirtyBits[addrIndex][evictionCandidateWay] = false;
            } else {
                dirtyBits[addrIndex][evictionCandidateWay] = true;
            }

            if (dirtyBitValue && validBitValue) {
                return dirtyMissLatency;
            }
            return cleanMissLatency;
        }
        
    }

    private int extractIndexBits(long address) {
        address = address >> blockOffsetBits;
        return (int) (address & ((1L << indexBits) - 1));
    }

    private int extractTagBits(long address) {
        address = address >> (indexBits + blockOffsetBits);
        return (int) (address & ((1L << tagBits) - 1));
    }

    /*
    * if the block containing the address is present in the cache, and has validBit set to 1
    * */
    private int whichWayInCache(int index, int tag) {
        for (int way = 0; way < ways; way++) {
            if ((tagArray[index][way] == tag) && (validBits[index][way])) {
                return way;
            }
        }
        return -1;
    }

    private void updateLRUState(int index, int way) {
        int maxLastTime = getMaxElementArray(LRUStates[index]);
        LRUStates[index][way] = maxLastTime + 1;
    }

    private int getMaxElementArray(int[] values) {
        int max = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] > max) {
                max = values[i];
            }
        }
        return max;
    }

    private int getMinElementIndexArray(int[] values) {
        int min = values[0];
        int minIndex = 0;
        for (int i = 1; i < values.length; i++) {
            if (values[i] < min) {
                min = values[i];
                minIndex = i;
            }
        }
        return minIndex;
    }
    
    private void printLRUBlock() {
   	 for (int i = 0; i < tagArray[0].length; i++) {
         if (i > 0) {
            System.out.print(", ");
         }
         System.out.print(tagArray[0][i]);
      }
	 System.out.println(" ");
    }
}
