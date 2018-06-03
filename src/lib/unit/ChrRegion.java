package lib.unit;

import org.jetbrains.annotations.NotNull;

public class ChrRegion implements Comparable {
    public Chromosome Chr;
    public int Begin;
    public int Terminal;
    public int Length;

    public ChrRegion(String[] s) {
        Chr = new Chromosome(s[0]);
        Begin = Integer.parseInt(s[1]);
        Terminal = Integer.parseInt(s[2]);
        Length = Terminal - Begin;
    }

    public ChrRegion(Chromosome s, int left, int right) {
        Chr = s;
        Begin = left;
        Terminal = right;
        Length = Terminal - Begin;
    }

    public boolean IsOverlap(ChrRegion reg) {
        return this.Chr.Name.equals(reg.Chr.Name) && (this.Terminal + reg.Terminal - this.Begin - reg.Begin) >= (Math.max(this.Terminal, reg.Terminal) - Math.min(this.Begin, reg.Begin));
    }

    public boolean IsBelong(ChrRegion reg) {
        return this.Chr.Name.equals(reg.Chr.Name) && (this.Begin >= reg.Begin && this.Terminal <= reg.Terminal);
    }

    public boolean IsContain(ChrRegion reg) {
        return this.Chr.Name.equals(reg.Chr.Name) && (this.Begin <= reg.Begin && this.Terminal >= reg.Terminal);
    }

    @Override
    public int compareTo(@NotNull Object o) {
        ChrRegion b = (ChrRegion) o;
        if (this.Chr.equals(b.Chr)) {
            return this.Begin == b.Begin ? this.Terminal - b.Terminal : this.Begin - b.Begin;
        } else {
            return this.Chr.compareTo(b.Chr);
        }
    }

    public String toString() {
        return Chr.Name + "\t" + Begin + "\t" + Terminal;
    }
}
