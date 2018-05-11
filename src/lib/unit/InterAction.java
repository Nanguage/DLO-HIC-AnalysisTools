package lib.unit;

public class InterAction {
    private ChrRegion Left;
    private ChrRegion Right;
    public String Name;
    public int Count;
    public int LeftFragment;
    public int RightFragment;

    public InterAction(ChrRegion reg1, ChrRegion reg2) {
        Left = reg1;
        Right = reg2;
    }

    public InterAction(String[] s) {
        Left = new ChrRegion(new String[]{s[0], s[1], s[2]});
        Right = new ChrRegion(new String[]{s[3], s[4], s[5]});
    }

    public boolean IsOverlap(InterAction action) {
        return Left.IsOverlap(action.Left) && Right.IsOverlap(action.Right);
    }

    public boolean IsBelong(InterAction action) {
        return Left.IsBelong(action.Left) && Right.IsBelong(action.Right);
    }

    public ChrRegion getLeft() {
        return Left;
    }

    public ChrRegion getRight() {
        return Right;
    }

    public String toString() {
        return Left.toString() + "\t" + Right.toString() + "\t" + Count + "\t" + LeftFragment + "\t" + RightFragment;
    }
}
