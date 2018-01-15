import java.util.ArrayList;
import java.util.Date;

/**
 * <p>quickly sort ArrayList</p>
 * <p>the first row is index of list, so please print from the second row</p>
 *
 * @author hjiang
 * @version 1.0
 */
class SortListStr extends ArrayList<char[][]> {


    public boolean add(String[] strings) {
        char[][] newchars = new char[strings.length + 1][];
        newchars[0] = String.valueOf(size()).toCharArray();
        for (int i = 0; i < strings.length; i++) {
            newchars[i + 1] = strings[i].toCharArray();
        }
        return super.add(newchars);
    }

    public boolean add(String Index, String[] strings) {
        char[][] newchars = new char[strings.length + 1][];
        newchars[0] = String.valueOf(Index).toCharArray();
        for (int i = 0; i < strings.length; i++) {
            newchars[i + 1] = strings[i].toCharArray();
        }
        return super.add(newchars);
    }

    /**
     * @param start The region of start index
     * @param end   The region of end index
     * @param Model Ordering mode
     * @param Level Thread layer number
     * @throws IndexOutOfBoundsException
     */
    public void QuickSort(int start, int end, String Model, int Level) throws IndexOutOfBoundsException {
        //判断是否需要进一步排序
        if (start >= end) {
            return;
        }
        char[][] temp;
        temp = get((start + end) / 2);
        set((start + end) / 2, get(start));
        set(start, temp);
        char[][] base = get(start);//选取第一个元素作为基准
        int i = start;
        int j = end;
        int index;
        int compare_result;
        boolean flag = true;
        //---------------------------------------------------------------------------
        if (Model.matches(".*r.*")) {
            //倒序排序
            while (i < j) {
                if (flag) {
                    //查找比基准大的元素并放到基准的左边
                    while (i < j) {
                        index = 1;
                        compare_result = 0;
                        while (index < base.length && (compare_result = CommonMethod.CompareTo(base[index], get(j)[index])) == 0) {
                            index++;
                        }
                        if (compare_result >= 0) {
                            j--;
                        } else {
                            flag = false;
                            break;
                        }
                    }
                } else {
                    //查找比基准小的元素并放到基准的右边
                    while (i < j) {
                        index = 1;
                        compare_result = 0;
                        while (index < base.length && (compare_result = CommonMethod.CompareTo(get(i)[index], base[index])) == 0) {
                            index++;
                        }
                        if (compare_result >= 0) {
                            i++;
                        } else {
                            flag = true;
                            break;
                        }
                    }
                }
                //交换
                temp = get(j);
                set(j, get(i));
                set(i, temp);
            }
        } else {
            //顺序排序
            while (i < j) {
                if (flag) {
                    //查找比基准小的元素并放在左边
                    while (i < j) {
                        index = 1;
                        compare_result = 0;
                        while (index < base.length && (compare_result = CommonMethod.CompareTo(base[index], get(j)[index])) == 0) {
                            index++;
                        }
                        if (compare_result <= 0) {
                            j--;
                        } else {
                            flag = false;
                            break;
                        }
                    }
                } else {
                    //查找比基准大的元素并放在右边
                    while (i < j) {
                        index = 1;
                        compare_result = 0;
                        while (index < base.length && (compare_result = CommonMethod.CompareTo(get(i)[index], base[index])) == 0) {
                            index++;
                        }
                        if (compare_result <= 0) {
                            i++;
                        } else {
                            flag = true;
                            break;
                        }
                    }
                }
                //交换
                temp = get(j);
                set(j, get(i));
                set(i, temp);
            }
        }
        if (Level > 0) {
            Thread[] Process = new Thread[2];
            if (start < i - 1) {
                int finalI = i;
                Process[0] = new Thread(new Runnable() {
                    @Override
                    public void run() {
//                    System.out.println(new Date() + "\t" + Thread.currentThread().getName() + "\tstart");
                        SortListStr.this.QuickSort(start, finalI - 1, Model, Level - 1);
//                    System.out.println(new Date() + "\t" + Thread.currentThread().getName() + "\tend");
                    }
                });
                Process[0].start();
            }
            if (j + 1 < end) {
                int finalJ = j;
                Process[1] = new Thread(new Runnable() {
                    @Override
                    public void run() {
//                    System.out.println(new Date() + "\t" + Thread.currentThread().getName() + "\tstart");
                        SortListStr.this.QuickSort(finalJ + 1, end, Model, Level - 1);
//                    System.out.println(new Date() + "\t" + Thread.currentThread().getName() + "\tend");
                    }
                });
                Process[1].start();
            }
            if (start < i - 1) {
                try {
                    Process[0].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (j + 1 < end) {
                try {
                    Process[1].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (start < i - 1) {
                QuickSort(start, i - 1, Model, Level);
            }
            if (end > j + 1) {
                QuickSort(j + 1, end, Model, Level);
            }
        }

    }
}