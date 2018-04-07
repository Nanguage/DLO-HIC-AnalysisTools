package lib.unit;

import org.jetbrains.annotations.NotNull;

public class StringArrays implements Comparable {
    private String[] item;
    private int length;

    public StringArrays(String[] i) {
        item = i;
        length = item.length;
    }

    public StringArrays(int l) {
        length = l;
        item = new String[length];
    }

    @Override
    public int compareTo(@NotNull Object o) {
        StringArrays b = (StringArrays) o;
        int result = 0;
        int i = 0;
        while (result == 0 && i < item.length && i < b.getItem().length) {
            result = item[i].compareTo(b.getItem()[i]);
            i++;
        }
        if (result == 0) {
            result = item.length - b.getItem().length;
        }
        return result;
    }

    public String[] getItem() {
        return item;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void set(int index, String s) {
        this.item[index] = s;
    }
}
