package ru.croc.java2017.shell;

import org.junit.Test;

import static org.junit.Assert.*;

import ru.croc.java2017.shell.Shell.*;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;

public class TestWriteFile extends TestCreatorFolder {
    @Test (expected = ShellIOException.class)
    public void throwsFileNotExist() throws ShellIOException {
        shell.makeDirectory("dir");
        shell.writeTextToFile("dir/cheburek.txt", "");
    }

    @Test (expected = ShellIOException.class)
    public void throwsNotFile() throws ShellIOException {
        shell.makeDirectory("dir");
        shell.writeTextToFile("dir", "");
    }

    @Test
    public void commonTest() throws IOException {
        shell.makeFile("cheburek.txt");

        String message =
                "Every breaking wave on the shore,\n" +
                "Tells the next one there'll be one more.\n" +
                "Every gambler knows that to lose,\n" +
                "It's what you're really there for.\n" +
                "Summer I was fearless.\n" +
                "Now I speak into an answer phone.\n" +
                "Like every falling leaf on the breeze,\n" +
                "Winter wouldn't leave it alone, alone.\n" +
                "\n\n\n";

        Path path = shell.writeTextToFile("cheburek.txt", message);

        StringBuilder stringBuilder = new StringBuilder();
        List<String> linesRead = Files.readAllLines(path);
        for (String s: linesRead) {
            stringBuilder.append(s);
            stringBuilder.append('\n');
        }

        assertEquals(message, stringBuilder.toString());
    }
}