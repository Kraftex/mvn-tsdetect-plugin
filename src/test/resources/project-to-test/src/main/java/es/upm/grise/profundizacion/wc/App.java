package es.upm.grise.profundizacion.wc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class App {

    public static void main(String[] args) {

        // Without parameters, we print the function usage
        if (args.length == 0) {
            System.out.println("Usage: wc [-clw file]");
            return; // System.exit() would be preferable, but it breaks the tests
        }

        // If more than 2 parameters are given, we stop without further information
        if (args.length != 2) {
            System.out.println("Wrong arguments!");
            return;
        }

        // Check the commands later (see switch below)
        // At this point, we open the text file
        BufferedReader br = null;
        String fileName = args[1];

        try {
            br = new BufferedReader(new FileReader(fileName));
        } catch (Exception e) {
            System.out.println("Cannot find file: " + fileName);
            return;
        }

        // And count the characters, words and lines. It is easier
        // to count everything than rewind the file several times,
        // which is not an straightforward operation
        Counter counter = null;
        try {
            counter = new Counter(br);
        } catch (IOException e) {
            System.out.println("Error reading file: " + fileName);
            return;
        }

        // Verify that the commands start with -. It is not necessary
        // because the first parameter is a command always, but...
        String commands = args[0];
        if (commands.charAt(0) != '-') {
            System.out.println("The commands do not start with -");
            return;
        }

        // Check the commands and return values. In case of wrong commands
        // the program exits with error
        String result = "";
        commands = commands.substring(1);
        for (char c : commands.toCharArray()) {
            switch (c) {
                case 'c':
                    result += "\t" + Integer.toString(counter.getNumberCharacters());
                    break;
                case 'l':
                    result += "\t" + Integer.toString(counter.getNumberLines());
                    break;
                case 'w':
                    result += "\t" + Integer.toString(counter.getNumberWords());
                    break;
                default:
                    System.out.println("Unrecognized command: " + c);
                    return;
            }
        }

        // Print the results
        // The counts are returned in the specified order
        System.out.println(result + "\t" + fileName);
    }
}
