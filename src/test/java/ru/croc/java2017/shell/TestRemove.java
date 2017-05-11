package ru.croc.java2017.shell;

import org.junit.Test;

import ru.croc.java2017.shell.Shell.*;

public class TestRemove extends TestCreatorFolder {
    @Test
    public void commonTest() throws ShellIOException {
        shell.makeDirectory("dir");
        shell.makeFile("dir/cheburek.txt");
        shell.remove("dir", true);

        shell.makeDirectory("empty");
        shell.remove("empty", true);

        shell.makeFile("empty.txt");
        shell.remove("empty.txt", false);
    }

    @Test (expected = ShellIOException.class)
    public void throwsNotExist() throws ShellIOException {
        shell.remove("dir", true);
    }

    @Test (expected = ShellMissingArgumentException.class)
    public void throwsMissedFlag() throws ShellIOException, ShellMissingArgumentException {
        shell.makeDirectory("dir");
        shell.makeFile("dir/cheburek.txt");
        shell.remove("dir", false);
    }
}