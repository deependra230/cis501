package cis501.submission;

import cis501.ICache;

public class Cache implements ICache {

    public Cache(int indexBits, int ways, int blockOffsetBits,
                 final int accessLatency, final int cleanMissLatency, final int dirtyMissLatency) {
        assert indexBits >= 0;
        assert ways > 0;
        assert blockOffsetBits >= 0;
        assert indexBits + blockOffsetBits < 32;
        assert accessLatency >= 0;
        assert cleanMissLatency >= 0;
        assert dirtyMissLatency >= 0;
    }

    @Override
    public int access(boolean load, long address) {
        return 0;
    }

}
