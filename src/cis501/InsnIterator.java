package cis501;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.zip.GZIPInputStream;

public class InsnIterator implements Iterator<Insn>, Iterable<Insn> {

    private final BufferedReader reader;
    private final int LIMIT;
    private final List<Insn> list;
    private final Stack<Insn> pbBuffer = new Stack<>();
    private int insnsProcessed = 0;

    /**
     * @param filename The path to the compressed trace file
     * @param limit    Stop after processing this many insns. If -1, process the entire trace.
     */
    public InsnIterator(String filename, int limit) {
        if (-1 == limit) {
            LIMIT = Integer.MAX_VALUE; // no limit
        } else {
            LIMIT = limit;
        }
        list = null;

        InputStreamReader isr = null;
        try {
            isr = new InputStreamReader(new GZIPInputStream(new FileInputStream(filename)), "US-ASCII");
        } catch (IOException e) {
            e.printStackTrace();
        }
        reader = new BufferedReader(isr);
    }

    public InsnIterator(List<Insn> l) {
        reader = null;
        LIMIT = l.size();
        list = l;
    }

    public boolean hasNext() {
        if (Thread.interrupted()) {
            throw new IllegalStateException("Interrupted!");
        }
        try {
            return !pbBuffer.isEmpty() ||
                    (insnsProcessed < LIMIT &&
                           ((null != reader && reader.ready()) ||
                            (null != list && !list.isEmpty())));
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Insn next() {
        if (Thread.interrupted()) {
            throw new IllegalStateException("Interrupted!");
        }
        try {
            insnsProcessed++;
            if (!pbBuffer.isEmpty()) {
                return pbBuffer.pop();
            }
            if (null != list) {
                return list.remove(0);
            }
            String ln = reader.readLine();
            return new Insn(ln);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Push the given insn i back into the iterator. If the following method call is next(), then
     * next() will return i. Insns that are put back will be returned via next() in LIFO order.
     */
    public void putBack(Insn i) {
        pbBuffer.push(i);
        insnsProcessed--;
    }

    public Iterator<Insn> iterator() {
        return this;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

}
