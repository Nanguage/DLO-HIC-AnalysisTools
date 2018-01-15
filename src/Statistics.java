import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Statistics {
    public static long RangeCount(String Bedpe, float Min, float Max, int Threads) throws IOException, InterruptedException {
        BufferedReader bedpe = new BufferedReader(new FileReader(Bedpe));
        final long[] Count = {0};
        String line;
        String[] str;
        line = bedpe.readLine();
        str = line.split("\\s+");
        try {
            if (Math.abs(Integer.parseInt(str[1]) - Integer.parseInt(str[3])) <= Max && Math.abs(Integer.parseInt(str[1]) - Integer.parseInt(str[3])) >= Min) {
                Count[0]++;
            }
            Thread[] Process = new Thread[Threads];
            for (int i = 0; i < Threads; i++) {
                Process[i] = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String line;
                        String[] str;
                        try {
                            while ((line = bedpe.readLine()) != null) {
                                str = line.split("\\s+");
                                if (Math.abs(Integer.parseInt(str[1]) - Integer.parseInt(str[3])) <= Max && Math.abs(Integer.parseInt(str[1]) - Integer.parseInt(str[3])) >= Min) {
                                    synchronized (Thread.class) {
                                        Count[0]++;
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                });
                Process[i].start();
            }
            for (int i = 0; i < Threads; i++) {
                Process[i].join();
            }
        } catch (NumberFormatException e) {
            if (Math.abs(Integer.parseInt(str[1]) - Integer.parseInt(str[4])) <= Max && Math.abs(Integer.parseInt(str[1]) - Integer.parseInt(str[4])) >= Min) {
                Count[0]++;
            }
            Thread[] Process = new Thread[Threads];
            for (int i = 0; i < Threads; i++) {
                Process[i] = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String line;
                        String[] str;
                        try {
                            while ((line = bedpe.readLine()) != null) {
                                str = line.split("\\s+");
                                if (Math.abs(Integer.parseInt(str[1]) - Integer.parseInt(str[4])) <= Max && Math.abs(Integer.parseInt(str[1]) - Integer.parseInt(str[4])) >= Min) {
                                    synchronized (Thread.class) {
                                        Count[0]++;
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                Process[i].start();
            }
            for (int i = 0; i < Threads; i++) {
                Process[i].join();
            }
        }
        bedpe.close();
        return Count[0];
    }
}
