package cis501.submission;

import cis501.*;
import org.junit.Before;
import org.junit.Test;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import static cis501.Bypass.MX;
import static org.junit.Assert.assertEquals;

public class InorderPipelineSampleTest {

    private InsnIterator uiter;
    private InsnIterator uiter_full;

    private static IInorderPipeline sim;
    private static InorderPipeline sim_FullBypass_MemLat1;

    private static List<Insn> wX_Dependency_Insns;
    private static List<Insn> mX_Dependency_StoreAddr_Insns;
    private static List<Insn> wM_Dependency_StoreVal_Insns;

    private static Insn makeInsn(int dst, int src1, int src2, MemoryOp mop) {
        if (MemoryOp.Store == mop) assert -1 == dst : dst;
        return new Insn(dst, src1, src2,
                1, 4,
                null, 0, null,
                mop, 1, 1,
                "synthetic");
    }

    @Before
    public void setup() {
        sim = new InorderPipeline(0/*no add'l memory latency*/, Bypass.FULL_BYPASS);
        sim_FullBypass_MemLat1 = new InorderPipeline(1/*no add'l memory latency*/, Bypass.FULL_BYPASS);

        wX_Dependency_Insns = new LinkedList<>();
        wX_Dependency_Insns.add(makeInsn(0, 3, -1, null));
        wX_Dependency_Insns.add(makeInsn(3, 13, -1, MemoryOp.Load));
        wX_Dependency_Insns.add(makeInsn(0, 14, 0, MemoryOp.Load));

        mX_Dependency_StoreAddr_Insns = new LinkedList<>();
        mX_Dependency_StoreAddr_Insns.add(makeInsn(1, 2, 3, null));
        mX_Dependency_StoreAddr_Insns.add(makeInsn(-1, 10, 1, MemoryOp.Store)); //store-addr is dependent

        wM_Dependency_StoreVal_Insns = new LinkedList<>();
        wM_Dependency_StoreVal_Insns.add(makeInsn(1, 2, 3, null));
        wM_Dependency_StoreVal_Insns.add(makeInsn(-1, 1, 10, MemoryOp.Store)); //store-val is dependent


        String traceFilePath = "./test/resources/cis501/submission/streamcluster-10M-v2.trace.gz";
        this.uiter = new InsnIterator(traceFilePath, 5000);
        this.uiter_full = new InsnIterator(traceFilePath, -1);
    }

    @Test
    public void test1Uop() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, null));
        sim.run(insns);
        assertEquals(1, sim.getInsns());
        // 12345678
        // fdxmw|
        assertEquals(6, sim.getCycles());
    }

    @Test
    public void testManyUops() {
        List<Insn> insns = new LinkedList<>();
        final int COUNT = 10;
        for (int i = 0; i < COUNT; i++) {
            insns.add(makeInsn(3, 1, 2, null));
        }
        sim.run(insns);
        assertEquals(COUNT, sim.getInsns());
        assertEquals(5 + COUNT, sim.getCycles());
    }

    @Test
    public void test1MemUop() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        sim.run(insns);
        assertEquals(1, sim.getInsns());
        // 123456789abcdef
        // fdxmw|
        assertEquals(6, sim.getCycles());
    }

    @Test
    public void test1MemUopLatency1() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        sim_FullBypass_MemLat1.run(insns);
        assertEquals(1, sim_FullBypass_MemLat1.getInsns());
        // 123456789abcdef
        // fdxmw|
        assertEquals(7, sim_FullBypass_MemLat1.getCycles());
    }

    @Test
    public void testManyMemUops() {
        List<Insn> insns = new LinkedList<>();
        final int COUNT = 10;
        for (int i = 0; i < COUNT; i++) {
            insns.add(makeInsn(-1, 1, 2, MemoryOp.Store)); // no load-use dependencies
        }
        sim.run(insns);
        assertEquals(COUNT, sim.getInsns());
        assertEquals(5 + COUNT, sim.getCycles());
    }

    @Test
    public void testManyMemUopsLatency1() {
        List<Insn> insns = new LinkedList<>();
        final int COUNT = 10;
        for (int i = 0; i < COUNT; i++) {
            insns.add(makeInsn(3, 1, 2, MemoryOp.Store)); // no load-use dependencies
        }
        sim_FullBypass_MemLat1.run(insns);
        assertEquals(COUNT, sim_FullBypass_MemLat1.getInsns());
        assertEquals(5 + 2*COUNT, sim_FullBypass_MemLat1.getCycles());
    }

    @Test
    public void testLoadUse1() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        insns.add(makeInsn(5, 3, 4, null)); // load to src reg 1
        sim.run(insns);
        assertEquals(2, sim.getInsns());
        // 123456789abcdef
        // fdxmw  |
        //  fd.xmw|
        assertEquals(6 + 2, sim.getCycles());
    }

    @Test
    public void testLoadUse2() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        insns.add(makeInsn(5, 4, 3, null)); // load to src reg 2
        sim.run(insns);
        assertEquals(2, sim.getInsns());
        // 123456789abcdef
        // fdxmw  |
        //  fd.xmw|
        assertEquals(6 + 2, sim.getCycles());
    }

    @Test
    public void testLoadUseStoreAddress() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        insns.add(makeInsn(-1, 4, 3, MemoryOp.Store)); // load to src reg 2 (store address), so we stall
        sim.run(insns);
        assertEquals(2, sim.getInsns());
        // 123456789abc
        // fdxmw  |
        //  fd.xmw|
        final long expected = 6 + 2;
        assertEquals(expected, sim.getCycles());
    }

    @Test
    public void testLoadUseStoreValue() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        insns.add(makeInsn(-1, 3, 4, MemoryOp.Store)); // load to src reg 1 (store value), so no stall
        sim.run(insns);
        assertEquals(2, sim.getInsns());
        // 123456789abcdef
        // fdxmw |
        //  fdxmw|
        final long expected = 6 + 1;
        assertEquals(expected, sim.getCycles());
    }

    @Test
    public void testMultipleProducerMX() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, null));
        insns.add(makeInsn(3, 4, 5, null));
        insns.add(makeInsn(7, 6, 3, null));

        sim = new InorderPipeline(0/*no add'l memory latency*/, EnumSet.of(MX));

        sim.run(insns);
        assertEquals(3, sim.getInsns());
        // 123456789a
        // fdxmw  |
        //  fdxmw |
        //   fdxmw|
        assertEquals(8, sim.getCycles());
    }

    @Test
    public void testMultipleProducerLoadMX() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        insns.add(makeInsn(3, 4, 5, null));
        insns.add(makeInsn(7, 6, 3, null));

        sim = new InorderPipeline(0/*no add'l memory latency*/, EnumSet.of(MX));

        sim.run(insns);
        assertEquals(3, sim.getInsns());
        // 123456789a
        // fdxmw  |
        //  fdxmw |
        //   fdxmw|
        assertEquals(8, sim.getCycles());
    }

    @Test
    public void test_NoBypass_LoadStoreValueDependency() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        insns.add(makeInsn(5, 3, 4, MemoryOp.Store)); // load to store value

        InorderPipeline sim_NoBypass_MemLat0 = new InorderPipeline(0, Bypass.NO_BYPASS);
        sim_NoBypass_MemLat0.run(insns);
        assertEquals(2, sim_NoBypass_MemLat0.getInsns());
        // 123456789abcdef
        // fdxmw  |
        //  fd..xmw|
        assertEquals(9, sim_NoBypass_MemLat0.getCycles());
    }

    @Test
    public void test_WXBypass_LoadStoreValueDependency() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        insns.add(makeInsn(5, 3, 4, MemoryOp.Store)); // load to store value

        InorderPipeline sim_WXBypass_MemLat0 = new InorderPipeline(0, EnumSet.of(Bypass.WX));
        sim_WXBypass_MemLat0.run(insns);
        assertEquals(2, sim_WXBypass_MemLat0.getInsns());
        // 123456789abcdef
        // fdxmw  |
        //  fd.xmw|
        assertEquals(8, sim_WXBypass_MemLat0.getCycles());
    }

    @Test
    public void test_WMBypass_LoadStoreValueDependency() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        insns.add(makeInsn(5, 3, 4, MemoryOp.Store)); // load to store value

        InorderPipeline sim_WMBypass_MemLat0 = new InorderPipeline(0, EnumSet.of(Bypass.WM));
        sim_WMBypass_MemLat0.run(insns);
        assertEquals(2, sim_WMBypass_MemLat0.getInsns());
        // 123456789abcdef
        // fdxmw|
        // fdxmw|
        assertEquals(7, sim_WMBypass_MemLat0.getCycles());
    }

    @Test
    public void test_NoBypass_MemLat2_LoadStoreValueDependencyWithInsn() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        insns.add(makeInsn(10, 11, 12, null));
        insns.add(makeInsn(5, 3, 4, MemoryOp.Store)); // load to store value

        InorderPipeline sim_NoBypass_MemLat2 = new InorderPipeline(2, Bypass.NO_BYPASS);
        sim_NoBypass_MemLat2.run(insns);
        assertEquals(3, sim_NoBypass_MemLat2.getInsns());
        // 123456789abcdef
        // fdxm..w     |
        //  fdx..mw
        //   fd...xm..w|
        assertEquals(13, sim_NoBypass_MemLat2.getCycles());
    }

    @Test
    public void test_NoBypass_MemLat2_LoadStoreValueDependencyWithBubble() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        insns.add(null);
        insns.add(makeInsn(5, 3, 4, MemoryOp.Store)); // load to store value

        InorderPipeline sim_NoBypass_MemLat2 = new InorderPipeline(2, Bypass.NO_BYPASS);
        sim_NoBypass_MemLat2.run(insns);
        assertEquals(3, sim_NoBypass_MemLat2.getInsns());
        // 123456789abcdef
        // fdxm..w     |
        //   fd...xm..w|
        assertEquals(13, sim_NoBypass_MemLat2.getCycles());
    }

    @Test
    public void test_WMBypass_MemLat2_LoadStoreValueDependencyWithBubble() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        insns.add(null);
        insns.add(makeInsn(5, 3, 4, MemoryOp.Store)); // load to store value

        InorderPipeline sim_WMBypass_MemLat2 = new InorderPipeline(2, EnumSet.of(Bypass.WM));
        sim_WMBypass_MemLat2.run(insns);
        assertEquals(3, sim_WMBypass_MemLat2.getInsns());
        // 123456789abcdef
        // fdxm..w   |
        //   fdx.m..w|
        assertEquals(11, sim_WMBypass_MemLat2.getCycles());
    }

    @Test
    public void test_WMBypass_MemLat2_LoadStoreAddrDependencyWithBubble() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        insns.add(null);
        insns.add(makeInsn(5, 4, 3, MemoryOp.Store)); // load to store addr

        InorderPipeline sim_WMBypass_MemLat2 = new InorderPipeline(2, EnumSet.of(Bypass.WM));
        sim_WMBypass_MemLat2.run(insns);
        assertEquals(3, sim_WMBypass_MemLat2.getInsns());
        // 123456789abcdef
        // fdxm..w     |
        //   fd...xm..w|
        assertEquals(13, sim_WMBypass_MemLat2.getCycles());
    }

    @Test
    public void test_WXBypass_MemLat2_LoadStoreAddrDependencyWithBubble() {
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(3, 1, 2, MemoryOp.Load));
        insns.add(null);
        insns.add(makeInsn(5, 4, 3, MemoryOp.Store)); // load to store addr

        InorderPipeline sim_WXBypass_MemLat2 = new InorderPipeline(2, EnumSet.of(Bypass.WX));
        sim_WXBypass_MemLat2.run(insns);
        assertEquals(3, sim_WXBypass_MemLat2.getInsns());
        // 123456789abcdef
        // fdxm..w    |
        //   fd..xm..w|
        assertEquals(12, sim_WXBypass_MemLat2.getCycles());
    }

    @Test
    public void test_NoBypass_MemLat0_WXDependency() {
        InorderPipeline sim_NoBypass_MemLat0 = new InorderPipeline(0, Bypass.NO_BYPASS);
        /*
        * ands r0, r3             F D X M W
        * ldr r3, [sp, #0x2c]       F D X M W
        * ldr.w r0, [lr, r0, lsl #2]  F D . X M W
        * */
        sim_NoBypass_MemLat0.run(wX_Dependency_Insns);
        assertEquals(3, sim_NoBypass_MemLat0.getInsns());
        assertEquals(9, sim_NoBypass_MemLat0.getCycles());
    }

    @Test
    public void test_WXBypass_MemLat0_WXDependency() {
        InorderPipeline sim_WXBypass_MemLat0 = new InorderPipeline(0, EnumSet.of(Bypass.WX));
        /*
        * ands r0, r3             F D X M W
        * ldr r3, [sp, #0x2c]       F D X M W
        * ldr.w r0, [lr, r0, lsl #2]  F D X M W
        * */
        sim_WXBypass_MemLat0.run(wX_Dependency_Insns);
        assertEquals(3, sim_WXBypass_MemLat0.getInsns());
        assertEquals(8, sim_WXBypass_MemLat0.getCycles());
    }

    @Test
    public void test_MXWMBypass_MemLat0_WXDependency() {
        InorderPipeline sim_MXWMBypass_MemLat0 = new InorderPipeline(0, EnumSet.of(Bypass.MX, Bypass.WM));
        /*
        * ands r0, r3             F D X M W
        * ldr r3, [sp, #0x2c]       F D X M W
        * ldr.w r0, [lr, r0, lsl #2]  F D . X M W
        * */
        sim_MXWMBypass_MemLat0.run(wX_Dependency_Insns);
        assertEquals(3, sim_MXWMBypass_MemLat0.getInsns());
        assertEquals(9, sim_MXWMBypass_MemLat0.getCycles());
    }

    @Test
    public void test_NoBypass_MemLat2_WXDependency() {
        InorderPipeline sim_NoBypass_MemLat2 = new InorderPipeline(2, Bypass.NO_BYPASS);
        /*
        * ands r0, r3             F D X M W
        * ldr r3, [sp, #0x2c]       F D X M . . W
        * ldr.w r0, [lr, r0, lsl #2]  F D . X . M . . W
        * */
        sim_NoBypass_MemLat2.run(wX_Dependency_Insns);
        assertEquals(3, sim_NoBypass_MemLat2.getInsns());
        assertEquals(12, sim_NoBypass_MemLat2.getCycles());
    }

    @Test
    public void test_FullBypass_MemLat2_WXDependency() {
        InorderPipeline sim_FullBypass_MemLat2 = new InorderPipeline(2, Bypass.FULL_BYPASS);
        /*
        * ands r0, r3             F D X M W
        * ldr r3, [sp, #0x2c]       F D X M . . W
        * ldr.w r0, [lr, r0, lsl #2]  F D X . . M . . W
        * */
        sim_FullBypass_MemLat2.run(wX_Dependency_Insns);
        assertEquals(3, sim_FullBypass_MemLat2.getInsns());
        assertEquals(12, sim_FullBypass_MemLat2.getCycles());
    }

    @Test
    public void test_NoBypass_MemLat0_MXDependency_StoreAddr() {
        InorderPipeline sim_NoBypass_MemLat0 = new InorderPipeline(0, Bypass.NO_BYPASS);
        /*
        * add r1<-r2,r3     F D X M W
        * str val,[r1]        F D . . X M W  (store-address is dependent)
        * */
        sim_NoBypass_MemLat0.run(mX_Dependency_StoreAddr_Insns);
        assertEquals(2, sim_NoBypass_MemLat0.getInsns());
        assertEquals(9, sim_NoBypass_MemLat0.getCycles());
    }

    @Test
    public void test_WXBypass_MemLat0_MXDependency_StoreAddr() {
        InorderPipeline sim_WXBypass_MemLat0 = new InorderPipeline(0, EnumSet.of(Bypass.WX));
        /*
        * add r1<-r2,r3     F D X M W
        * str val,[r1]        F D . X M W  (store-address is dependent)
        * */
        sim_WXBypass_MemLat0.run(mX_Dependency_StoreAddr_Insns);
        assertEquals(2, sim_WXBypass_MemLat0.getInsns());
        assertEquals(8, sim_WXBypass_MemLat0.getCycles());
    }

    @Test
    public void test_MXBypass_MemLat0_MXDependency_StoreAddr() {
        InorderPipeline sim_MXBypass_MemLat0 = new InorderPipeline(0, EnumSet.of(Bypass.MX));
        /*
        * add r1<-r2,r3     F D X M W
        * str val,[r1]        F D X M W  (store-address is dependent)
        * */
        sim_MXBypass_MemLat0.run(mX_Dependency_StoreAddr_Insns);
        assertEquals(2, sim_MXBypass_MemLat0.getInsns());
        assertEquals(7, sim_MXBypass_MemLat0.getCycles());
    }

    @Test
    public void test_WMBypass_MemLat0_MXDependency_StoreAddr() {
        InorderPipeline sim_WMBypass_MemLat0 = new InorderPipeline(0, EnumSet.of(Bypass.WM));
        /*
        * add r1<-r2,r3     F D X M W
        * str val,[r1]        F D . . X M W  (store-address is dependent)
        * */
        sim_WMBypass_MemLat0.run(mX_Dependency_StoreAddr_Insns);
        assertEquals(2, sim_WMBypass_MemLat0.getInsns());
        assertEquals(9, sim_WMBypass_MemLat0.getCycles());
    }

    @Test
    public void test_MXBypass_MemLat0_MXDependency_StoreVal() {
        InorderPipeline sim_MXBypass_MemLat0 = new InorderPipeline(0, EnumSet.of(Bypass.MX));
        /*
        * add r1<-r2,r3     F D X M W
        * str addr,[r1]       F D X M W  (store-val is dependent)
        * */
        sim_MXBypass_MemLat0.run(wM_Dependency_StoreVal_Insns);
        assertEquals(2, sim_MXBypass_MemLat0.getInsns());
        assertEquals(7, sim_MXBypass_MemLat0.getCycles());
    }

    @Test
    public void test_WMBypass_MemLat0_MXDependency_StoreVal() {
        InorderPipeline sim_WMBypass_MemLat0 = new InorderPipeline(0, EnumSet.of(Bypass.WM));
        /*
        * add r1<-r2,r3     F D X M W
        * str addr,[r1]       F D X M W  (store-val is dependent)
        * */
        sim_WMBypass_MemLat0.run(wM_Dependency_StoreVal_Insns);
        assertEquals(2, sim_WMBypass_MemLat0.getInsns());
        assertEquals(7, sim_WMBypass_MemLat0.getCycles());
    }

    @Test
    public void test_WXBypass_MemLat0_MXDependency_StoreVal() {
        InorderPipeline sim_WXBypass_MemLat0 = new InorderPipeline(0, EnumSet.of(Bypass.WX));
        /*
        * add r1<-r2,r3     F D X M W
        * str addr,[r1]       F D . X M W  (store-val is dependent)
        * */
        sim_WXBypass_MemLat0.run(wM_Dependency_StoreVal_Insns);
        assertEquals(2, sim_WXBypass_MemLat0.getInsns());
        assertEquals(8, sim_WXBypass_MemLat0.getCycles());
    }

    @Test
    public void loadUseStr_no_WM_Bypass_noMX_noLatency() {
        IInorderPipeline simFile = new InorderPipeline(0, Bypass.NO_BYPASS);
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(1, 2, 4, MemoryOp.Load));
        insns.add(makeInsn(-1, 1, 4, MemoryOp.Store));
        simFile.run(insns);
        assertEquals(9, simFile.getCycles());
        assertEquals(2, simFile.getInsns());
        /*
          1:  F  D  X  M  W
          2:      F   D  X   .  .  M   W

         */
    }

    @Test
    public void loadUseStr_no_WM_Bypass_WX_noLatency() {
        IInorderPipeline simFile = new InorderPipeline(0, EnumSet.of(Bypass.WX));
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(1, 2, 4, MemoryOp.Load));
        insns.add(makeInsn(-1, 1, 4, MemoryOp.Store));
        simFile.run(insns);
        assertEquals(8, simFile.getCycles());
        assertEquals(2, simFile.getInsns());
        /*
          1:  F  D  X  M  W
          2:      F   D  X   .   M   W

         */
    }

    @Test
    public void loadUseStr_noWM_noMX_Bypass_WX_Latency2() {
        IInorderPipeline simFile = new InorderPipeline(2, EnumSet.of(Bypass.WX));
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(1, 2, 4, MemoryOp.Load));
        insns.add(makeInsn(-1, 1, 4, MemoryOp.Store));
        simFile.run(insns);
        assertEquals(12, simFile.getCycles());
        assertEquals(2, simFile.getInsns());
        /*
          1:  F  D  X  M   .   .   W
          2:     F  D  .   .   .   X	M	.	.	W

         */
    }

    @Test
    public void loadUseStr_noWM_Bypass_noWX_Latency2() {
        IInorderPipeline simFile = new InorderPipeline(2, EnumSet.of(Bypass.MX));
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(1, 2, 4, MemoryOp.Load));
        insns.add(makeInsn(-1, 1, 4, MemoryOp.Store));
        simFile.run(insns);
        assertEquals(13, simFile.getCycles());
        assertEquals(2, simFile.getInsns());
        /*
          1:  F  D  X  M   .   .   W
          2:     F  D  .   X   .   .   .	M	.	.	W

         */
    }

    @Test
    public void test_StoreUseLoadtest_no_Bypass() {
        IInorderPipeline simFile = new InorderPipeline(0, Bypass.NO_BYPASS);
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(0, 1, 0, MemoryOp.Load));
        insns.add(makeInsn(-1, 1, 0, MemoryOp.Store));
        simFile.run(insns);
        assertEquals(9, simFile.getCycles());
        assertEquals(2, simFile.getInsns());
        /*
          1:  F  D  X  M   W
          2:     F  D  .   .   X	 M	 W
         */
    }

    @Test
    public void test_StoreUseLoadtest_MXBypass() {
        IInorderPipeline simFile = new InorderPipeline(0, EnumSet.of(Bypass.MX));
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(0, 1, 0, MemoryOp.Load));
        insns.add(makeInsn(-1, 1, 0, MemoryOp.Store));
        simFile.run(insns);
        assertEquals(9, simFile.getCycles());
        assertEquals(2, simFile.getInsns());
        /*
          1:  F  D  X  M   W
          2:     F  D  .   .   X	 M	 W
         */
    }

    @Test
    public void test_StoreUseLoadtest_WXBypass() {
        IInorderPipeline simFile = new InorderPipeline(0, EnumSet.of(Bypass.WX));
        List<Insn> insns = new LinkedList<>();
        insns.add(makeInsn(0, 1, 0, MemoryOp.Load));
        insns.add(makeInsn(-1, 1, 0, MemoryOp.Store));
        simFile.run(insns);
        assertEquals(8, simFile.getCycles());
        assertEquals(2, simFile.getInsns());
        /*
          1:  F  D  X  M   W
          2:     F  D  .   X	 M	 W
         */
    }

    @Test
    public void testFullBypassMemLat0() {
        IInorderPipeline simFile = new InorderPipeline(0, Bypass.FULL_BYPASS);
        simFile.run(uiter);
        assertEquals(5000, simFile.getInsns());
        assertEquals(5430, simFile.getCycles());
    }

    @Test
    public void testFullBypassMemLat1() {
        IInorderPipeline simFile = new InorderPipeline(1, Bypass.FULL_BYPASS);
        simFile.run(uiter);
        assertEquals(5000, simFile.getInsns());
        assertEquals(6870, simFile.getCycles());
    }

    @Test
    public void testFullBypassMemLat2() {
        IInorderPipeline simFile = new InorderPipeline(2, Bypass.FULL_BYPASS);
        simFile.run(uiter);
        assertEquals(5000, simFile.getInsns());
        assertEquals(8310, simFile.getCycles());
    }

    @Test
    public void testMXBypassMemLat0() {
        IInorderPipeline simFile = new InorderPipeline(0, EnumSet.of(Bypass.MX));
        simFile.run(uiter);
        assertEquals(5000, simFile.getInsns());
        assertEquals(6152, simFile.getCycles());
    }

    @Test
    public void testMXBypassMemLat1() {
        IInorderPipeline simFile = new InorderPipeline(1, EnumSet.of(Bypass.MX));
        simFile.run(uiter);
        assertEquals(5000, simFile.getInsns());
        assertEquals(7498, simFile.getCycles());
    }

    @Test
    public void testMXBypassMemLat2() {
        IInorderPipeline simFile = new InorderPipeline(2, EnumSet.of(Bypass.MX));
        simFile.run(uiter);
        assertEquals(5000, simFile.getInsns());
        assertEquals(8938, simFile.getCycles());
    }
}
