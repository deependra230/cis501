package cis501.submission;

import cis501.ICache;
import cis501.ITraceAnalyzer;
import cis501.InsnIterator;

import java.io.IOException;

public class TraceRunner {

    public static void main(String[] args) throws IOException {
        final int insnLimit;

        switch (args.length) {
            case 1:
                insnLimit = -1; // by default, run on entire trace
                break;
            case 2: // use user-provided limit
                insnLimit = Integer.parseInt(args[1]);
                break;
            default:
                System.err.println("Usage: path/to/trace-file [insn-limit]");
                return;
        }
		InsnIterator uiter = new InsnIterator(args[0], insnLimit);
        ITraceAnalyzer ta = new TraceAnalyzer();
        ta.run(uiter);        
       
        System.out.println("Avg insn size is: " + ta.avgInsnSize());
        System.out.println("Insn bw increase sans thumb: " + ta.insnBandwidthIncreaseWithoutThumb());
        System.out.println("Most common insn category: " + ta.mostCommonInsnCategory());
        
        for (int b = 1; b <= 32; b++) {
            System.out.format("Direct branch offsets encodable in %d bits: %.2f%n",
                    b, ta.fractionOfDirectBranchOffsetsLteNBits(b));
        }
        OOOStats(args[0], insnLimit);
    }
    
    public static void OOOStats(String filename, int insnLimit) {
        
    		for (int ROBSize = 10; ROBSize < 25; ROBSize += 5) {
	    		System.out.println("ROBSize, " + ROBSize);
	    		System.out.println("IQSize, IPC");
	    		
	    		for (int IQSize = 2; IQSize <11; IQSize++ ) {
	    			InsnIterator uiter = new InsnIterator(filename, insnLimit);
	    			int PRegisters = 50;
	    	        int indexBits = 7; //(32KB/(64B * 4ways))
	    	        int ways = 4;
	    	        	int blockOffsetBits = 6;
	    	        final int accessLatency = 0;
	    	        	final int cleanMissLatency = 2;
	    	        final 	int dirtyMissLatency = 3;
	    	        ICache ic = new Cache(indexBits, ways,  blockOffsetBits,
	    	                accessLatency, cleanMissLatency,  dirtyMissLatency);
	    	        ICache dc = new Cache(indexBits, ways,  blockOffsetBits,
	    	                accessLatency, cleanMissLatency,  dirtyMissLatency);
	    	        OOOPipeline sim = new OOOPipeline(PRegisters, ROBSize, IQSize, ic, dc);
	    	        sim.run(uiter);
	    	        double IPC = (double)sim.getInsns()/ (double)sim.getCycles();
	    	        //System.out.println("insns " + sim.getInsns());
	    	        //System.out.println("Cycles " + sim.getCycles());
	    	        System.out.println( IQSize + "," + IPC);
	    		}
        }
    }

}
