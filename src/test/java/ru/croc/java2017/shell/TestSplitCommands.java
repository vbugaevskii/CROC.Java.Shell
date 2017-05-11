package ru.croc.java2017.shell;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestSplitCommands {
    private String input;
    private String[] output;

    @Test
    public void testEasy() {
        input  = "head -n   10  \t test.txt";
        output = new String[] {"head", "-n", "10", "test.txt"};
        assertArrayEquals(output, Shell.splitCommands(input));

        input  = "    cd    ../../test    \n\n\n";
        output = new String[] {"cd", "../../test"};
        assertArrayEquals(output, Shell.splitCommands(input));
    }

    @Test
    public void testQuotes() {
        String tongueTwister = "    She\tsells   seashells   by the seashore   ";

        input  = String.format("echo \"%s\" file.txt", tongueTwister);
        output = new String[] {"echo", "file.txt", String.format("\"%s\"", tongueTwister)};
        assertArrayEquals(output, Shell.splitCommands(input));

        input  = String.format("echo '%s' file.txt", tongueTwister);
        output = new String[] {"echo", "file.txt", String.format("'%s'", tongueTwister)};
        assertArrayEquals(output, Shell.splitCommands(input));
    }

    @Test
    public void testQuotesMixed() {
        input = "\"Words like violence\n'Break\tthe   silence'\"";
        output = new String[] {input};
        assertArrayEquals(output, Shell.splitCommands(input));

        input = "'Words like violence\n\"Break\tthe   silence\"'";
        output = new String[] {input};
        assertArrayEquals(output, Shell.splitCommands(input));
    }

    @Test
    public void testQuotesMixedFailed() {
        input = "echo \"string1 'string2'";
        output = input.split(" ");
        assertArrayEquals(output, Shell.splitCommands(input));

        input = "echo 'string1 \"string2\"";
        output = input.split(" ");
        assertArrayEquals(output, Shell.splitCommands(input));

        input  = "echo 'string2' \"string1";
        output = new String[] {"echo", "\"string1", "'string2'"};
        assertArrayEquals(output, Shell.splitCommands(input));
    }
}