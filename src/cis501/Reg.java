package cis501;

/** Common functionality for both architectural and physical registers */
public abstract class Reg {

    final protected int reg;

    public Reg(int r) {
        assert r >= 0 : r;
        reg = r;
    }

    public int get() {
        return reg;
    }

}
