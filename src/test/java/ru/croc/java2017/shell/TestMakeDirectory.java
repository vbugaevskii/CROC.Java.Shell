package ru.croc.java2017.shell;

import org.junit.Test;

import static org.junit.Assert.*;

import ru.croc.java2017.shell.Shell.*;

import java.nio.file.Files;
import java.nio.file.Path;

public class TestMakeDirectory extends TestCreatorFolder {
    @Test
    public void commonTest() throws ShellIOException{
        Path path = shell.makeDirectory("dir");
        assertTrue(Files.exists(path) && Files.isDirectory(path));
    }

    @Test(expected = ShellIOException.class)
    public void throwsNotInCurrentDirectory() throws ShellIOException {
        shell.makeDirectory("dir/path");
    }

    @Test(expected = ShellIOException.class)
    public void throwsAlreadyExists() throws ShellIOException {
        shell.makeDirectory("dir");
        shell.makeDirectory("dir");
    }
}