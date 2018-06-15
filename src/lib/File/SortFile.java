package lib.File;

import java.io.*;

import lib.unit.*;


public class SortFile {

    public static void main(String[] args) throws IOException {
        new CustomFile(args[0]).SortFile(new int[1], "n", "", new File(args[0] + ".sort"));
    }
}
