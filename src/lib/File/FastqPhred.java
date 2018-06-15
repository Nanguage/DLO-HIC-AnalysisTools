package lib.File;

import lib.unit.CustomFile;

import java.io.IOException;

public class FastqPhred {
    public static void main(String[] args) throws IOException {
        for (String file : args) {
            System.out.println(new CustomFile(file).getName() + "\t" + new CustomFile(file).FastqPhred());
        }
    }
}
