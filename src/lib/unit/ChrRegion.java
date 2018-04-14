package lib.unit;

public class ChrRegion {
    public String Chr;
    public int Begin;
    public int Terminal;

    public ChrRegion(String[] s) {
        Chr = s[0];
        Begin = Integer.parseInt(s[1]);
        Terminal = Integer.parseInt(s[2]);
    }

    public boolean IsOverlap(ChrRegion reg) {
        return this.Chr.equals(reg.Chr) && (this.Terminal + reg.Terminal - this.Begin - reg.Begin) >= (Math.max(this.Terminal, reg.Terminal) - Math.min(this.Begin, reg.Begin));
    }

    public boolean IsBelong(ChrRegion reg) {
        return this.Chr.equals(reg.Chr) && (this.Begin >= reg.Begin && this.Terminal <= reg.Terminal);
    }

    public boolean IsContain(ChrRegion reg) {
        return this.Chr.equals(reg.Chr) && (this.Begin <= reg.Begin && this.Terminal >= reg.Terminal);
    }
}
