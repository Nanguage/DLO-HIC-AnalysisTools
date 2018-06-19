package lib.unit;

public class Matrix<T extends Number> {
    public T[][] Matrix;
    public double Area;
    public double Count;
    private int[] Size = new int[2];
    public int Resolution;

    public Matrix(T[][] matrix) {
        Matrix = matrix;
        Size[0] = Matrix.length;
        Size[1] = Matrix[0].length;
        for (int i = 0; i < Size[0]; i++) {
            for (int j = 0; j < Size[1]; j++) {
                Count += Matrix[i][j].doubleValue();
            }
        }
        Area = Size[0] * Size[1];
    }

    public Matrix(int Row, int Col, T Value) {
        Size = new int[]{Row, Col};
        MatrixInit(Value);
    }

    public int[] getSize() {
        return Size;
    }

    public T[][] getMatrix() {
        return Matrix;
    }

    private void MatrixInit(T Value) {
        for (int i = 0; i < Size[0]; i++) {
            for (int j = 0; j < Size[1]; j++) {
                Matrix[i][j] = Value;
            }
        }
    }

    public void setResolution(int resolution) {
        Resolution = resolution;
    }
}
