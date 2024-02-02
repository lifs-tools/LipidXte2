package de.mpicbg.ms.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Spawn execution process
 */
public class Exec
{
	static public void run(String... commands)
    {
        Process process = null;
        try {
            process = new ProcessBuilder(commands).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        InputStream is = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;

//        System.out.printf("Output of running %s is:", Arrays.toString(args));

        try {
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
