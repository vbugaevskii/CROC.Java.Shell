package ru.croc.java2017.shell;

import org.junit.After;
import org.junit.Before;

public class TestCreatorFolder {
    protected Shell shell;
    private static final String PATH_INIT = "./tmp";

    @Before
    public void setUp() throws Exception {
        shell = new Shell();
        shell.makeDirectory(PATH_INIT);
        shell.moveDirectory(PATH_INIT);
    }

    @After
    public void tearDown() throws Exception {
        shell = new Shell();
        shell.remove(PATH_INIT, true);
    }
}
