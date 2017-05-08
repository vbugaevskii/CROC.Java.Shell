package ru.croc.java2017.shell;

import org.jnativehook.GlobalScreen;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

import java.util.List;
import java.util.LinkedList;

import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class ShellKeyListener implements NativeKeyListener {
    static {
        LogManager.getLogManager().reset();
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
    }

    private String currentCommand = null;
    private List<String> processedCommands = new LinkedList<>();
    private int processedCommandsIndex;

    public String getCurrentCommand() {
        return currentCommand;
    }

    protected void addProcessedCommand(String command) {
        processedCommands.add(command);
        processedCommandsIndex = processedCommands.size();
    }

    public void nativeKeyPressed(NativeKeyEvent e) {
        int keyCode = e.getKeyCode();

        switch (keyCode) {
            case NativeKeyEvent.VC_UP:
                if (processedCommandsIndex > 0) {
                    processedCommandsIndex -= 1;
                }
                break;
            case NativeKeyEvent.VC_DOWN:
                if (processedCommandsIndex < processedCommands.size()) {
                    processedCommandsIndex += 1;
                }
                break;
        }

        if (processedCommandsIndex < processedCommands.size()) {
            currentCommand = processedCommands.get(processedCommandsIndex);
        } else {
            currentCommand = null;
        }
    }

    public void nativeKeyReleased(NativeKeyEvent e) {

    }

    public void nativeKeyTyped(NativeKeyEvent e) {

    }
}