package lib.unit;

public class Matrix<T extends Number> {
    private T[][] Matrix;
    private int[] Size = new int[2];
    private int Resolution;

    public Matrix(T[][] matrix) {
        Matrix = matrix;
        Size[0] = Matrix.length;
        Size[1] = Matrix[0].length;
    }

    public Matrix(int Row, int Col, T Value) {
        Size = new int[]{Row, Col};
        MatrixInit(Value);
    }

    public int[] getSize() {
        return Size;
    }

    private void MatrixInit(T Value) {
        for (int i = 0; i < Size[0]; i++) {
            for (int j = 0; j < Size[1]; j++) {
                Matrix[i][j] = Value;
            }
        }
    }

    public T[][] SubMatrix(int[] IRegion, int[] JRegion) {
        T[][] submatrix = (T[][]) new Number[IRegion[1] - IRegion[0] + 1][JRegion[1] - JRegion[0] + 1];
        for (int i = 0; i < submatrix.length; i++) {
            System.arraycopy(Matrix[IRegion[0] + i], JRegion[0], submatrix[i], 0, submatrix[i].length);
        }
        return submatrix;
    }

    public void put(int i, int j, T value) {
        Matrix[i][j] = value;
    }

    public T get(int i, int j) {
        return Matrix[i][j];
    }

    public void setResolution(int resolution) {
        Resolution = resolution;
    }

    public int getResolution() {
        return Resolution;
    }
}
