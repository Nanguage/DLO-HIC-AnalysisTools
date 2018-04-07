import java.io.IOException;

import lib.tool.Statistic;

public class CalculatorLineNumber {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java -cp DLO-HIC-AnalysisTools CalculatorLineNumber <File1 [File2] ...>");
            System.exit(0);
        }
        for (int i = 0; i < args.length; i++) {
            int finalI = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        long linenumber = Statistic.CalculatorLineNumber(args[finalI]);
                        synchronized (Thread.class) {
                            System.out.println(args[finalI] + " line number is:\t" + linenumber);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
}
