package cis501.submission;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import cis501.BranchPredictor;
import cis501.Direction;
import cis501.IBranchTargetBuffer;
import cis501.ICache;
import cis501.IDirectionPredictor;
import cis501.IInorderPipeline;
import cis501.Insn;
import cis501.InsnIterator;
import cis501.MemoryOp;

public class CacheTest {
	 private static final int ACCESS_LAT = 0;
	    private static final int CLEAN_MISS_LAT = 2;
	    private static final int DIRTY_MISS_LAT = 3;

	    private static final int INDEX_BITS = 3;
	    private static final int WAYS = 4;
	    private static final int BLOCK_BITS = 2;
	    private static final int BLOCK_SIZE = 1 << BLOCK_BITS; // 4B, 1 ARM insn per block

	    private ICache cache;
	    private IInorderPipeline pipe;

	    /** Runs before each test...() method */
	    @Before
	    public void setup() {
	        cache = new Cache(INDEX_BITS, WAYS, BLOCK_BITS, ACCESS_LAT, CLEAN_MISS_LAT, DIRTY_MISS_LAT);

	        IBranchTargetBuffer btb = new BranchTargetBuffer(3/*index bits*/);
	        IDirectionPredictor never = new DirPredNeverTaken();

	        // pipeline uses never predictor for simplicity
	        pipe = new InorderPipeline(new BranchPredictor(never, btb),
	                new Cache(INDEX_BITS, WAYS, BLOCK_BITS, ACCESS_LAT, CLEAN_MISS_LAT, DIRTY_MISS_LAT),
	                new Cache(INDEX_BITS, WAYS, BLOCK_BITS, ACCESS_LAT, CLEAN_MISS_LAT, DIRTY_MISS_LAT));
	    }

	    @Test
	    public void testInitialState() {
	        final long addr = 0xFF << (INDEX_BITS + BLOCK_BITS);
	        int lat = cache.access(true, addr);
	        assertEquals(CLEAN_MISS_LAT, lat);
	    }

	    @Test
	    public void testRemainderIndexing() {
	        final long addr = -1;
	        int lat = cache.access(true, addr);
	        assertEquals(CLEAN_MISS_LAT, lat);
	    }

	    @Test
	    public void testBlockOffset() {
	        final long firstByteInBlock = 0xFF << (INDEX_BITS + BLOCK_BITS);
	        int lat = cache.access(true, firstByteInBlock);
	        assertEquals(CLEAN_MISS_LAT, lat);
	        final long lastByteInBlock = firstByteInBlock + (1 << BLOCK_BITS) - 1;
	        lat = cache.access(true, lastByteInBlock);
	        assertEquals(ACCESS_LAT, lat);
	    }

	    @Test
	    public void testLRU() {
	        final long a = 0xFF << (INDEX_BITS + BLOCK_BITS);
	        final long waySize = (1 << INDEX_BITS) * (1 << BLOCK_BITS);

	        int lat = cache.access(true, a);
	        assertEquals(CLEAN_MISS_LAT, lat);

	        for (int w = 1; w < WAYS * 2; w++) {
	            // a hits
	            lat = cache.access(true, a);
	            assertEquals(ACCESS_LAT, lat);

	            // conflicting access
	            lat = cache.access(true, a + (w * waySize));
	            assertEquals(CLEAN_MISS_LAT, lat);
	        }
	    }

	    // pipeline integration tests

	    @Test
	    public void testFullSetLoads() {
	        final long a = 0xFF << (INDEX_BITS + BLOCK_BITS);
	        final long waySize = (1 << INDEX_BITS) * (1 << BLOCK_BITS);
	        for (int w = 0; w < WAYS; w++) {
	            int lat = cache.access(true, a + (w * waySize));
	            assertEquals(CLEAN_MISS_LAT, lat);
	        }

	        // a should still be in the cache
	        int lat = cache.access(true, a);
	        assertEquals(ACCESS_LAT, lat);
	    }

	    @Test
	    public void testConflictMissLoads() {
	        final long a = 0xFF << (INDEX_BITS + BLOCK_BITS);
	        final long waySize = (1 << INDEX_BITS) * (1 << BLOCK_BITS);
	        for (int w = 0; w < WAYS + 1; w++) {
	            int lat = cache.access(true, a + (w * waySize));
	            assertEquals(CLEAN_MISS_LAT, lat);
	        }

	        // a should have gotten evicted
	        int lat = cache.access(true, a);
	        assertEquals(CLEAN_MISS_LAT, lat);
	    }
	    

	    //test fully associative
	    @Test
	    public void testFullyAssociativeLRUSingleByteEvenWays() {
	    	int indexBits = 0;
	    	int ways = 4;
	    	int blockbits =  0;
	    	Cache LocalCache = new Cache(indexBits, ways, blockbits, ACCESS_LAT, CLEAN_MISS_LAT, DIRTY_MISS_LAT);
	    	for (long address = 0; address < ways; address ++) {
	    		int lat = LocalCache.access(true, address);
	    		System.out.println("add: " + address + "latency: " + lat);
	    		assertEquals(CLEAN_MISS_LAT, lat);
	    	}
	    	//first address should still be there
	    	assertEquals(ACCESS_LAT, LocalCache.access(true,0));  	
	    }
	    
	    @Test
	    public void testFullyAssociativeLRUSingleByteOddWays() {
	    	int indexBits = 0;
	    	int ways = 3;
	    	int blockbits =  0;
	    	Cache LocalCache = new Cache(indexBits, ways, blockbits, ACCESS_LAT, CLEAN_MISS_LAT, DIRTY_MISS_LAT);
	    	for (long address = 0; address < ways; address ++) {
	    		int lat = LocalCache.access(true, address);
	    		System.out.println("add: " + address + "latency: " + lat);
	    		assertEquals(CLEAN_MISS_LAT, lat);
	    	}
	    	//first address should still be there
	    	assertEquals(ACCESS_LAT, LocalCache.access(true,0));  	
	    }
	    
	    @Test
	    public void testLRUAlgoTimeStamp() {
	    	int indexBits = 0;
	    	int ways = 4;
	    	int blockbits =  0;
	    	Cache LocalCache = new Cache(indexBits, ways, blockbits, ACCESS_LAT, CLEAN_MISS_LAT, DIRTY_MISS_LAT);
	    	
	    	//fill up all the ways in the index
	    	for (long address = 0; address < ways; address ++) {
	    		int lat = LocalCache.access(true, address);
	    		assertEquals(CLEAN_MISS_LAT, lat);
	    	}
	    	//make sure they are filled
	    	for (long address = 0; address < ways; address ++) {
	    		int lat = LocalCache.access(true, address);
	    		assertEquals(ACCESS_LAT, lat);
	    	}
	    	
	    	// 1. access one [0] and [3] multiple times (so they both have max value)
	    	// 2. access [2], [1], [0] (now [3] has to be LRU)  
	    	// 3. replace element in set
	    	// 4. confirm [3] is LRU by checking that [0],[1] and [2] still in the ways and [4] is not
	    	for (int i = 0; i < 100; i++) {
	    		assertEquals(ACCESS_LAT, LocalCache.access(true, 0));
	    		assertEquals(ACCESS_LAT, LocalCache.access(true, 3));	
	    	}
	    	for (int i = 0; i < 3; i++) {
	    		assertEquals(ACCESS_LAT, LocalCache.access(true, i));
	    	}
	    	assertEquals(CLEAN_MISS_LAT,LocalCache.access(true,7));
	    	
	    	for (int i = 0; i < 3; i++) {
	    		assertEquals(ACCESS_LAT, LocalCache.access(true, i));
	    	}
	    	assertEquals(ACCESS_LAT, LocalCache.access(true, 7));
	      	
	    }
	    
	    //store tests
	    @Test
	    public void testLoadDirtyBit() {
	    	assertEquals(CLEAN_MISS_LAT,cache.access(false, 0));
	    	assertEquals(ACCESS_LAT,cache.access(true, 0));
	    	
	    }
	    
	    @Test
	    public void testStoreWithDirtyBitSetSameAdress() {
	    	assertEquals(CLEAN_MISS_LAT,cache.access(false, 0));
	    	assertEquals(ACCESS_LAT,cache.access(false, 0));
	    	assertEquals(ACCESS_LAT,cache.access(true, 0));
	    	
	    }
	    
	    @Test
	    public void testStoreWithDirtyBitSetReplacement() {
	    	final long a = 0xFF << (INDEX_BITS + BLOCK_BITS);
	    	int waySize =  (1 << INDEX_BITS) * (1 << BLOCK_BITS);
	    	assertEquals(CLEAN_MISS_LAT,cache.access(false, a));
	    	  for (int w = 1; w < WAYS; w++) {
		            int lat = cache.access(true, a + (w * waySize));
		            assertEquals(CLEAN_MISS_LAT, lat);
		        }
	    	assertEquals(DIRTY_MISS_LAT,cache.access(false, a + WAYS * waySize));
	    	assertEquals(CLEAN_MISS_LAT,cache.access(false, a+ BLOCK_SIZE));
	    }
	    
	    @Test
	    public void testLoadWithDirtyBitSetReplacement() {
	    	final long a = 0xFF << (INDEX_BITS + BLOCK_BITS);
	    	int waySize =  (1 << INDEX_BITS) * (1 << BLOCK_BITS);
	    	assertEquals(CLEAN_MISS_LAT,cache.access(false, a));
	    	  for (int w = 1; w < WAYS; w++) {
		            int lat = cache.access(false, a + (w * waySize));
		            assertEquals(CLEAN_MISS_LAT, lat);
		        }
	    	assertEquals(DIRTY_MISS_LAT,cache.access(true, a + WAYS * waySize));
	    	assertEquals(CLEAN_MISS_LAT,cache.access(false, a + BLOCK_SIZE));
	    }
	    
	   
}
