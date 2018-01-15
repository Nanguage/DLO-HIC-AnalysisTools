import java.util.ArrayList;
import java.util.Date;

public class SortListInt extends ArrayList<int[]> {

    public boolean add(int[] ints) {
        int[] new_ints = new int[ints.length + 1];
        new_ints[0] = size();
        System.arraycopy(ints, 0, new_ints, 1, ints.length);
        return super.add(new_ints);
    }

    public void add(int index, int[] ints) {
        int[] new_ints = new int[ints.length + 1];
        new_ints[0] = index;
        System.arraycopy(ints, 0, new_ints, 1, ints.length);
        super.add(new_ints);
    }

    /**
     * @param start The region of start index
     * @param end   The region of end index
     * @param Model Ordering mode
     * @param Level Thread layer number
     * @throws IndexOutOfBoundsException
     */
    public void QuickSort(int start, int end, String Model, int Level) throws IndexOutOfBoundsException {
        //判断是否要继续排序
        if (start >= end) {
            return;
        }
        int[] temp;
        temp = get((start + end) / 2);
        set((start + end) / 2, get(start));
        set(start, temp);
        int[] base = get(start);//提取第一个元素作为基准
        int i = start;
        int j = end;
        int index;
        int compare_result;
        boolean flag = true;
        if (Model.matches(".*r.*")) {
            //倒序排序
            while (i < j) {
                if (flag) {
                    //查找比基准大的元素放在基准左边
                    while (i < j) {
                        index = 1;
                        compare_result = 0;
                        while (index < base.length && (compare_result = Integer.compare(base[index], get(j)[index])) == 0) {
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
                    //查找比基准小的元素放在基准右边
                    while (i < j) {
                        index = 1;
                        compare_result = 0;
                        while (index < base.length && (compare_result = Integer.compare(get(i)[index], base[index])) == 0) {
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
            while (i < j) {
                if (flag) {
                    //查找比基准大的元素放在基准右边
                    while (i < j) {
                        index = 1;
                        compare_result = 0;
                        while (index < base.length && (compare_result = Integer.compare(base[index], get(j)[index])) == 0) {
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
                    //查找比基准小的元素放在基准左边
                    while (i < j) {
                        index = 1;
                        compare_result = 0;
                        while (index < base.length && (compare_result = Integer.compare(get(i)[index], base[index])) == 0) {
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
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + "\tstart");
                        QuickSort(start, finalI - 1, Model, Level - 1);
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + "\tend");
                    }
                });
                Process[0].start();
            }
            if (end > j + 1) {
                int finalJ = j;
                Process[1] = new Thread(new Runnable() {
                    @Override
                    public void run() {
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + "\tstart");
                        QuickSort(finalJ + 1, end, Model, Level - 1);
//                        System.out.println(new Date() + "\t" + Thread.currentThread().getName() + "\tend");
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
            if (end > j + 1) {
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
