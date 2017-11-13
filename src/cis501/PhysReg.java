package cis501;

/** A class representing a physical register */
public class PhysReg extends Reg {
    public PhysReg(int r) {
        super(r);
    }

    @Override
    public String toString() {
        return "p" + String.valueOf(reg);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof PhysReg) {
            return this.reg == ((PhysReg)o).reg;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.reg;
    }
}
