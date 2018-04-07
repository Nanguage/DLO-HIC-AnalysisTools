package lib.unit;

import org.jetbrains.annotations.NotNull;

public class IntegerArrays implements Comparable {
    private int[] item;
    private int length;

    public IntegerArrays(int[] i) {
        item = i;
        length = item.length;
    }

    public IntegerArrays(int l) {
        length = l;
        item = new int[length];
    }

    @Override
    public int compareTo(@NotNull Object o) {
        IntegerArrays b = (IntegerArrays) o;
        int result = 0;
        int i = 0;
        while (result == 0 && i < item.length && i < b.getItem().length) {
            result = item[i] - b.getItem()[i];
            i++;
        }
        if (result == 0) {
            result = item.length - b.getItem().length;
        }
        return result;
    }

    public int[] getItem() {
        return item;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getLength() {
        return length;
    }

    public void set(int index, int i) {
        item[index] = i;
    }

}