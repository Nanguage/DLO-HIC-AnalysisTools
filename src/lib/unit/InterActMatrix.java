package lib.unit;

public class InterActMatrix {
    private double[][] Matrix;
    public double Area;
    public double Count;
    private int[] Size = new int[2];
    public int Resolution;

    public InterActMatrix(int[][] matrix) {
        Matrix = new double[matrix.length][];
        for (int i = 0; i < matrix.length; i++) {
            Matrix[i] = IntegerArrays.toDouble(matrix[i]);
        }
        Size[0] = Matrix.length;
        Size[1] = Matrix[0].length;
        for (int i = 0; i < Size[0]; i++) {
            for (int j = 0; j < Size[1]; j++) {
                Count += Matrix[i][j];
            }
        }
        Area = Size[0] * Size[1];
    }

    public InterActMatrix(double[][] matrix) {
        Matrix = matrix;
        Size[0] = Matrix.length;
        Size[1] = Matrix[0].length;
        for (int i = 0; i < Size[0]; i++) {
            for (int j = 0; j < Size[1]; j++) {
                Count += Matrix[i][j];
            }
        }
        Area = Size[0] * Size[1];
    }

    public int[] getSize() {
        return Size;
    }

    public double[][] getMatrix() {
        return Matrix;
    }
}
