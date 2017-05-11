package ru.croc.java2017.shell;

import org.junit.Test;

import ru.croc.java2017.shell.Shell.*;

public class TestMoveDirectory extends TestCreatorFolder {
    @Test
    public void commonTest() throws ShellIOException {
        shell.makeDirectory("dir");
        shell.makeDirectory("dir/path");
        shell.moveDirectory("dir");
        shell.moveDirectory("../");
        shell.moveDirectory("dir/path");
        shell.moveDirectory("../");
        shell.moveDirectory("path");
        shell.moveDirectory("../../dir");
    }

    @Test(expected = ShellIOException.class)
    public void throwsNotDirectory() throws ShellIOException {
        shell.makeFile("cheburek.txt");
        shell.moveDirectory("cheburek.txt");
    }

    @Test(expected = ShellIOException.class)
    public void throwsNotExist() throws ShellIOException {
        shell.makeDirectory("dir");
        shell.moveDirectory("dir/path");
    }
}