package lib.File;

import java.io.*;
import java.util.Date;

public class Append {
    public Append(String File1, String File2) throws IOException {
        System.out.println(new Date() + "\tAppend " + File1 + " to " + File2);
        String line;
        BufferedReader read = new BufferedReader(new FileReader(File1));
        BufferedWriter write = new BufferedWriter(new FileWriter(File2, true));
        while ((line = read.readLine()) != null) {
            write.write(line + "\n");
        }
        read.close();
        write.close();
        System.out.println(new Date() + "\tEnd append " + File1 + " to " + File2);
    }
}
