package cis501;

public interface ICache {

    /**
     * @param load true if the access is a load, false if the access is a store
     * @param address the memory address to access
     * @return the latency (in cycles) of this access
     */
    public int access(boolean load, long address);

}
