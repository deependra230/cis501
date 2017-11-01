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
import cis501.TestUtils;

public class CachePipelineIntegrationTest {
	
	 private static final int ACCESS_LAT = 0;
	    private static final int CLEAN_MISS_LAT = 2;
	    private static final int DIRTY_MISS_LAT = 3;

	    private static final int INDEX_BITS = 3;
	    private static final int WAYS = 4;
	    private static final int BLOCK_BITS = 2;
	    private static final int BLOCK_SIZE = 1 << BLOCK_BITS; // 4B, 1 ARM insn per block

	    private ICache cache;
	    private IInorderPipeline pipe;

	    private static Insn makeInt(int sr1, int sr2, int dr, long pc, int isize) {
	        return new Insn(dr, sr1, sr2,
	                pc, isize,
	                null, 0, null,
	                null, 0, 0,
	                "fake-alu");
	    }

	    private static Insn makeBr(long pc, Direction dir, int isize, long targetPC) {
	        return new Insn(-1, 0, 0,
	                pc, isize,
	                dir, targetPC, null,
	                null, 0, 0,
	                "fake-branch");
	    }

	    // cache tests

	    private static Insn makeMem(int src1, int src2, int dst, long pc, int isize, MemoryOp mop, long dataAddr) {
	        return new Insn(dst, src1, src2,
	                pc, isize,
	                null, 0, null,
	                mop, dataAddr, 1,
	                "fake-mem");
	    }

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
	    public void testImiss() {
	        List<Insn> insns = new LinkedList<>();
	        insns.add(makeInt(1, 2, 3, 0xAA, 4));
	        pipe.run(new InsnIterator(insns));

	        assertEquals(1, pipe.getInsns());
	        // 123456789a
	        // f..dxmw|
	        assertEquals(6 + CLEAN_MISS_LAT, pipe.getCycles());
	    }

	    @Test
	    public void test2Imiss() {
	        List<Insn> insns = new LinkedList<>();
	        insns.add(makeInt(1, 2, 3, 0x0, 4));
	        insns.add(makeInt(1, 2, 3, BLOCK_SIZE, 4));
	        pipe.run(new InsnIterator(insns));

	        assertEquals(2, pipe.getInsns());
	        // 123456789abcd
	        // f..dxmw   |
	        //    f..dxmw|
	        assertEquals(7 + (2 * CLEAN_MISS_LAT), pipe.getCycles());
	    }

	    @Test
	    public void testImissIhit() {
	        List<Insn> insns = new LinkedList<>();
	        insns.add(makeInt(1, 2, 3, 0x0, 2));
	        insns.add(makeInt(1, 2, 3, 0x2, 2));
	        pipe.run(new InsnIterator(insns));

	        assertEquals(2, pipe.getInsns());
	        // 123456789abcd
	        // f..dxmw |
	        //    fdxmw|
	        assertEquals(7 + CLEAN_MISS_LAT, pipe.getCycles());
	    }

	    @Test
	    public void testManyImiss() {
	        List<Insn> insns = new LinkedList<>();
	        final int numInsns = 10;
	        for (int i = 0; i < numInsns; i++) {
	            insns.add(makeInt(1, 2, 3, i * BLOCK_SIZE, 4));
	        }
	        pipe.run(new InsnIterator(insns));

	        assertEquals(numInsns, pipe.getInsns());
	        // 123456789abcdef
	        // f..dxmw      |
	        //    f..dxmw   |
	        //       f..dxmw|
	        assertEquals(5 + (numInsns * (CLEAN_MISS_LAT + 1)), pipe.getCycles());
	    }

	    @Test
	    public void testImissDmiss() {
	        List<Insn> insns = new LinkedList<>();
	        insns.add(makeMem(1, 2, 3, 0x0, 4, MemoryOp.Load, 0xB));
	        pipe.run(new InsnIterator(insns));

	        assertEquals(1, pipe.getInsns());
	        // 123456789a
	        // f..dxm..w|
	        assertEquals(6 + (2 * CLEAN_MISS_LAT), pipe.getCycles());
	    }

	    @Test
	    public void testImissDmissIhitDhit() {
	        List<Insn> insns = new LinkedList<>();
	        insns.add(makeMem(1, 2, 3, 0x0, 1, MemoryOp.Load, 0x42));
	        insns.add(makeMem(1, 2, 3, 0x2, 1, MemoryOp.Load, 0x42));
	        pipe.run(new InsnIterator(insns));

	        assertEquals(2, pipe.getInsns());
	        // 123456789abcd
	        // f..dxm..w |
	        //    fdx  mw|
	        assertEquals(7 + (2 * CLEAN_MISS_LAT), pipe.getCycles());
	    }

	    @Test
	    public void testBranchMispredImiss() {
	        List<Insn> insns = new LinkedList<>();
	        insns.add(makeBr(0x2, Direction.Taken, 2, 0x42));
	        insns.add(makeInt(1, 2, 3, 0x42, 2));
	        pipe.run(new InsnIterator(insns));

	        assertEquals(2, pipe.getInsns());
	        // 123456789abcd
	        // f..dxmw     |
	        //      f..dxmw|
	        assertEquals(7 + (2 * CLEAN_MISS_LAT) + 2/*br mispred*/, pipe.getCycles());
	    }
	    
	    @Test
	    public void testImissIhitDmissStoreDirtymiss() {
	        List<Insn> insns = new LinkedList<>();
	        insns.add(makeMem(1, 2, 3, 0x0, 4, MemoryOp.Store, 0xB));
	        insns.add(makeMem(1, 2, 3, 0x1, 4, MemoryOp.Load, 0xB +(1 << INDEX_BITS) * (1 << BLOCK_BITS) ));
	        pipe.run(new InsnIterator(insns));

	        assertEquals(2, pipe.getInsns());
	        // f..dxm..w|
	        //	  fdxm...w|
	        assertEquals(6 + (2*CLEAN_MISS_LAT + DIRTY_MISS_LAT), pipe.getCycles());
	    }
	    
	    @Test
	    public void testImissImissDmissStoreDirtymiss() {
	        List<Insn> insns = new LinkedList<>();
	        insns.add(makeMem(1, 2, 3, 0x0, 4, MemoryOp.Store, 0xB));
	        insns.add(makeMem(1, 2, 3, 0x0 + (1 << INDEX_BITS) * (1 << BLOCK_BITS), 4, MemoryOp.Load, 0xB +(1 << INDEX_BITS) * (1 << BLOCK_BITS) ));
	        pipe.run(new InsnIterator(insns));

	        assertEquals(2, pipe.getInsns());
	        // f..dxm..w|
	        //	  f..dxm...w|
	        assertEquals(6 + (3*CLEAN_MISS_LAT + DIRTY_MISS_LAT), pipe.getCycles());
	    }
	    
	    @Test
	    public void testImissImissDmissStoreDirty() {
	        List<Insn> insns = new LinkedList<>();
	        insns.add(makeMem(1, 2, 3, 0x0, 4, MemoryOp.Store, 0xB));
	        insns.add(makeMem(1, 2, 3, 0x0 + (1 << INDEX_BITS) * (1 << BLOCK_BITS), 4, MemoryOp.Load, 0xB));
	        pipe.run(new InsnIterator(insns));

	        assertEquals(2, pipe.getInsns());
	        // f..dxm..w|
	        //	  f..dxm...w|
	        assertEquals(7 + (3*CLEAN_MISS_LAT + ACCESS_LAT), pipe.getCycles());
	    }
	    
	    //test Load-use
	   
	       @Test
	        public void testLoadUseIMissIhitDMiss() {
	            List<Insn> insns = new LinkedList<>();
	            insns.add(makeMem(3, 1, 2, 0x0,4, MemoryOp.Load, 0x0));
	            insns.add(makeInt(1, 2, 3, 0x1, 4));
	            pipe.run(new InsnIterator(insns));
		        assertEquals(2, pipe.getInsns());
	            // f..dxm..w     |
	            //    fd...mw  |    
	            assertEquals(7 + 1/*load use penalty*/ + 2*CLEAN_MISS_LAT, pipe.getCycles());
	        }
	       
	       @Test
	        public void testLoadUseIMissIMISSDMiss() {
	            List<Insn> insns = new LinkedList<>();
	            insns.add(makeMem(3, 1, 2, 0x0,4, MemoryOp.Load, 0x0));
	            insns.add(makeInt(1, 2, 3, 0x42, 4));
	            pipe.run(new InsnIterator(insns));
		        assertEquals(2, pipe.getInsns());
	            // f..dxm..w    |
	            //    f..d.xmw  |    
	            assertEquals(7 + 3*CLEAN_MISS_LAT, pipe.getCycles());
	        }
	       

          
}
