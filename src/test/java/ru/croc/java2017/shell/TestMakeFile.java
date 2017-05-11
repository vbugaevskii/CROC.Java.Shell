package ru.croc.java2017.shell;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

import ru.croc.java2017.shell.Shell.*;

public class TestMakeFile extends TestCreatorFolder {
    @Test(expected = ShellIOException.class)
    public void commonTest() throws ShellIOException {
        Path path = shell.makeFile("cheburek.txt");
        assertTrue(Files.exists(path) && Files.isRegularFile(path));

        shell.makeDirectory("dir");
        path = shell.makeFile("dir/cheburek.txt");
        assertTrue(Files.exists(path) && Files.isRegularFile(path));

        shell.makeFile("dir");
    }

    @Test(expected = ShellIOException.class)
    public void throwsDirectoryNotExits() throws ShellIOException {
        shell.makeFile("dir/cheburek.txt");
    }

    @Test(expected = ShellIOException.class)
    public void throwsAlreadyExists() throws ShellIOException {
        shell.makeFile("cheburek.txt");
        shell.makeFile("cheburek.txt");
    }
}