package lib.unit;

public class Filter {
    private int[][] FilterMatrix;
    private int Size;
    private boolean Type;

    public Filter(boolean b, int i) {
        Type = b;
        Size = i;
        Init();
    }

    private void Init() {
        FilterMatrix = new int[Size][Size];
        for (int i = 0; i < Size; i++) {
            for (int j = 0; j < Size; j++) {
                if (j < Size / 2)
                    FilterMatrix[i][j] = -1;
                else
                    FilterMatrix[i][j] = 1;
            }
        }
        if (Size % 2 == 1) {
            for (int i = 0; i < Size; i++) {
                FilterMatrix[i][Size / 2] = 0;
            }
        }
        if (!Type) {
            int temp;
            for (int i = 0; i < Size; i++) {
                for (int j = i + 1; j < Size; j++) {
                    temp = FilterMatrix[i][j];
                    FilterMatrix[i][j] = FilterMatrix[j][i];
                    FilterMatrix[j][i] = temp;
                }
            }
        }
    }

    public int[][] getFilterMatrix() {
        return FilterMatrix;
    }

    public int getSize() {
        return Size;
    }
}
