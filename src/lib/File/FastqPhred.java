package lib.File;

import java.io.IOException;

public class FastqPhred {
    public static void main(String[] args) throws IOException {
        for (String File : args) {
            System.out.println(File.replaceAll(".*/", "") + "\t" + FileTool.FastqPhred(File));
        }
    }
}
