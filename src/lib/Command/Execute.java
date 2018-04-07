package lib.Command;

import java.io.*;
import java.util.Date;

public class Execute {
    public Execute(String CommandStr, String... args) throws IOException {
        Process P;
        int ExitValue;
        try {
            System.out.println(new Date() + "\t" + CommandStr);
            P = Runtime.getRuntime().exec(CommandStr);

            Thread OutThread = new Thread(new Runnable() {
                String line;

                @Override
                public void run() {
                    try {
                        BufferedReader bufferedReaderIn = new BufferedReader(new InputStreamReader(P.getInputStream()));
                        if (args.length >= 1) {
                            String OutFile = args[0];
                            BufferedWriter bufferedwriter_out = new BufferedWriter(new FileWriter(OutFile));
                            while ((line = bufferedReaderIn.readLine()) != null) {
                                bufferedwriter_out.write(line + "\n");
                            }
                            bufferedReaderIn.close();
                            bufferedwriter_out.close();
                        } else {
                            while ((line = bufferedReaderIn.readLine()) != null) {
                            }
                            bufferedReaderIn.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            Thread ErrThread = new Thread(new Runnable() {
                String line;

                @Override
                public void run() {
                    try {
                        BufferedReader bufferedReaderIn = new BufferedReader(new InputStreamReader(P.getErrorStream()));
                        if (args.length >= 2) {
                            String LogFile = args[1];
                            BufferedWriter bufferedwriter_err = new BufferedWriter(new FileWriter(LogFile));
                            while ((line = bufferedReaderIn.readLine()) != null) {
                                bufferedwriter_err.write(line + "\n");
                            }
                            bufferedReaderIn.close();
                            bufferedwriter_err.close();
                        } else {
                            while ((line = bufferedReaderIn.readLine()) != null) {
                            }
                            bufferedReaderIn.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            try {
                OutThread.start();
                ErrThread.start();
                OutThread.join();
                ErrThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ExitValue = P.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
