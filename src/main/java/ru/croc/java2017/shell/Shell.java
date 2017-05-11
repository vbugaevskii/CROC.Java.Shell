package ru.croc.java2017.shell;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;

import java.util.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Shell extends ShellKeyListener {
    public class ShellIOException extends IOException {
        public static final String MSG_NOT_DIRECTORY = "\"%s\" is not a directory";
        public static final String MSG_NOT_FILE      = "\"%s\" is not a file";
        public static final String MSG_NOT_EXIST     = "\"%s\" doesn't exist";
        public static final String MSG_ALREADY_EXIST = "\"%s\" has already exist";
        public static final String MSG_UNABLE_DELETE = "\"%s\" can't be deleted";
        public static final String MSG_UNABLE_READ   = "\"%s\" can't be read";

        public static final String MSG_NOT_CURRENT_DIR = "\"%s\" is not in current directory";

        public ShellIOException(Exception cause) {
            super(cause);
        }

        public ShellIOException(String messageFormat, String path) {
            super(String.format(messageFormat, path));
        }
    }

    public class ShellRuntimeException extends RuntimeException {
        ShellRuntimeException(String message) {
            super(message);
        }
    }

    public class ShellIllegalUsage extends ShellRuntimeException {
        private static final String MSG_ILLEGAL_USAGE = "Illegal usage of command \"%s\"";

        public ShellIllegalUsage(ShellCommands command) {
            super(String.format(MSG_ILLEGAL_USAGE, command));
        }
    }

    public class ShellMissingArgumentException extends ShellRuntimeException {
        private static final String MSG_MISSED_ARGUMENT = "Illegal usage of command \"%s\". Use \"%s\"";

        public ShellMissingArgumentException(ShellCommands command, String missedArgument) {
            super(String.format(MSG_MISSED_ARGUMENT, command, missedArgument));
        }
    }

    public enum ShellCommands {
        MOVE_DIRECTORY ("cd"),
        MAKE_DIRECTORY ("mkdir"),
        LIST_DIRECTORY ("ls"),
        REMOVE         ("rm"),
        SHOW_FILE      ("head"),
        MAKE_FILE      ("mkfile"),
        WRITE_FILE     ("echo"),
        NULL_COMMAND   (null);

        private final String command;

        private ShellCommands(String command) {
            this.command = command;
        }

        public static ShellCommands getEnumCommand(String command) {
            switch (command) {
                case "cd":
                    return MOVE_DIRECTORY;
                case "mkdir":
                    return MAKE_DIRECTORY;
                case "ls":
                    return LIST_DIRECTORY;
                case "rm":
                    return REMOVE;
                case "head":
                    return SHOW_FILE;
                case "mkfile":
                    return MAKE_FILE;
                case "echo":
                    return WRITE_FILE;
                default:
                    return NULL_COMMAND;
            }
        }

        @Override
        public String toString() {
            return command;
        }
    }

    private Path currentPath;

    public Shell() {
        currentPath = Paths.get("").toAbsolutePath();
    }

    public Path getAbsolutePath(String path) throws ShellIOException {
        try {
            return currentPath.resolve(path).normalize();
        } catch (InvalidPathException err) {
            throw new ShellIOException(err);
        }
    }

    public Path moveDirectory(String path) throws ShellIOException {
        Path newPath = getAbsolutePath(path);

        if (Files.exists(newPath)) {
            if (Files.isDirectory(newPath)) {
                currentPath = newPath;
            } else {
                throw new ShellIOException(ShellIOException.MSG_NOT_DIRECTORY, path);
            }
        } else {
            throw new ShellIOException(ShellIOException.MSG_NOT_EXIST, path);
        }

        return newPath;
    }

    public Path makeDirectory(String path) throws ShellIOException {
        Path newPath = getAbsolutePath(path);

        if (!newPath.getParent().equals(currentPath)) {
            throw new ShellIOException(ShellIOException.MSG_NOT_CURRENT_DIR, path);
        }

        try {
            Files.createDirectory(newPath);
        } catch (FileAlreadyExistsException err) {
            throw new ShellIOException(ShellIOException.MSG_ALREADY_EXIST, path);
        } catch (IOException err) {
            if (!Files.exists(newPath.getParent())) {
                while (!Files.exists(newPath.getParent())) {
                    newPath = newPath.getParent();
                }
                throw new ShellIOException(ShellIOException.MSG_NOT_EXIST, newPath.toString());
            } else {
                throw new ShellIOException(err);
            }
        }

        return newPath;
    }

    public Path makeFile(String path) throws ShellIOException {
        Path newPath = getAbsolutePath(path);

        if (!newPath.getParent().equals(currentPath)) {
            throw new ShellIOException(ShellIOException.MSG_NOT_CURRENT_DIR, path);
        }

        try {
            if (!Files.exists(newPath)) {
                Files.createFile(newPath);
            } else {
                throw new ShellIOException(ShellIOException.MSG_ALREADY_EXIST, path);
            }
        } catch (FileAlreadyExistsException err) {
            throw new ShellIOException(ShellIOException.MSG_ALREADY_EXIST, path);
        } catch (IOException err) {
            if (!Files.exists(newPath.getParent())) {
                throw new ShellIOException(ShellIOException.MSG_NOT_EXIST, newPath.toString());
            } else {
                throw new ShellIOException(err);
            }
        }

        return newPath;
    }

    private static final String LS_FORMAT = "%c%c%c%c %15d %s %s";

    public Path listDirectory(String path) throws ShellIOException {
        Path newPath = getAbsolutePath(path);

        try {
            Files.list(newPath).forEach((Path p) -> {
                boolean isDirectory  = Files.isDirectory(p);
                boolean isReadable   = Files.isReadable(p);
                boolean isWritable   = Files.isWritable(p);
                boolean isExecutable = Files.isExecutable(p);

                FileTime lastModified;
                long fileSize;
                try {
                    lastModified = Files.getLastModifiedTime(p);
                    fileSize = Files.size(p);
                } catch (IOException err) {
                    lastModified = FileTime.fromMillis(0);
                    fileSize = 0;
                }

                System.out.println(String.format(
                        LS_FORMAT,
                        isDirectory  ? 'd' : '-',
                        isReadable   ? 'r' : '-',
                        isWritable   ? 'w' : '-',
                        isExecutable ? 'x' : '-',
                        fileSize,
                        lastModified,
                        p.getFileName()
                ));
            });
        } catch (NotDirectoryException err) {
            throw new ShellIOException(ShellIOException.MSG_NOT_DIRECTORY, path);
        } catch (IOException err) {
            throw new ShellIOException(ShellIOException.MSG_NOT_EXIST, path);
        }

        return newPath;
    }

    public Path remove(String path, boolean recursive) throws ShellIOException, ShellMissingArgumentException {
        Path newPath = getAbsolutePath(path);

        try {
            if (!Files.isDirectory(newPath)) {
                Files.delete(newPath);
            } else if (recursive) {
                Files.walk(newPath).sorted(Comparator.reverseOrder()).forEach((Path p) -> {
                    try {
                        Files.delete(p);
                    } catch (IOException er) {
                        System.out.println(String.format(ShellIOException.MSG_UNABLE_DELETE, p));
                    }
                });
            } else {
                throw new ShellMissingArgumentException(ShellCommands.REMOVE, "-r");
            }
        } catch (NoSuchFileException err) {
            throw new ShellIOException(ShellIOException.MSG_NOT_EXIST, path);
        } catch (IOException err) {
            throw new ShellIOException(ShellIOException.MSG_UNABLE_DELETE, newPath.toString());
        }
        return newPath;
    }

    public Path showFile(String path, int numberOfLines) throws ShellIOException {
        Path newPath = getAbsolutePath(path);

        if (Files.isRegularFile(newPath)) {
            try {
                Stream<String> stream = Files.lines(newPath);
                if (numberOfLines > 0) {
                    stream = stream.limit(numberOfLines);
                }
                stream.forEach(System.out::println);
            } catch (IOException err) {
                throw new ShellIOException(ShellIOException.MSG_UNABLE_READ, path);
            }
        } else {
            if (!Files.exists(newPath)) {
                throw new ShellIOException(ShellIOException.MSG_NOT_EXIST, path);
            } else {
                throw new ShellIOException(ShellIOException.MSG_NOT_FILE, path);
            }
        }

        return newPath;
    }

    public Path writeTextToFile(String path, String text) throws ShellIOException {
        Path newPath = getAbsolutePath(path);

        if (!Files.exists(newPath)) {
            throw new ShellIOException(ShellIOException.MSG_NOT_EXIST, path);
        }

        if (Files.isRegularFile(newPath)) {
            try {
                try (BufferedWriter writer = Files.newBufferedWriter(newPath, StandardOpenOption.APPEND)) {
                    writer.write(text);
                }
            } catch (IOException err) {
                throw new ShellIOException(err);
            }
        } else {
            throw new ShellIOException(ShellIOException.MSG_NOT_FILE, path);
        }

        return newPath;
    }

    private void processMoveDirectory(String[] args) throws ShellIOException, ShellIllegalUsage {
        String path = null;

        for (int i = 1; i < args.length; i++) {
            if (path == null) {
                path = args[i];
            } else {
                throw new ShellIllegalUsage(ShellCommands.MOVE_DIRECTORY);
            }
        }

        if (path == null) {
            throw new ShellIllegalUsage(ShellCommands.MOVE_DIRECTORY);
        }

        moveDirectory(path);
    }

    private void processMakeDirectory(String[] args) throws ShellIOException, ShellIllegalUsage {
        String path = null;

        for (int i = 1; i < args.length; i++) {
            if (path == null) {
                path = args[i];
            } else {
                throw new ShellIllegalUsage(ShellCommands.MAKE_DIRECTORY);
            }
        }

        if (path == null) {
            throw new ShellIllegalUsage(ShellCommands.MAKE_DIRECTORY);
        }

        makeDirectory(path);
    }

    private void processMakeFile(String[] args) throws ShellIOException, ShellIllegalUsage {
        String path = null;

        for (int i = 1; i < args.length; i++) {
            if (path == null) {
                path = args[i];
            } else {
                throw new ShellIllegalUsage(ShellCommands.MAKE_FILE);
            }
        }

        if (path == null) {
            throw new ShellIllegalUsage(ShellCommands.MAKE_FILE);
        }

        makeFile(path);
    }

    private void processlistDirectory(String[] args) throws ShellIOException, ShellIllegalUsage {
        String path = null;

        for (int i = 1; i < args.length; i++) {
            if (path == null) {
                path = args[i];
            } else {
                throw new ShellIllegalUsage(ShellCommands.LIST_DIRECTORY);
            }
        }

        if (path == null) {
            path = currentPath.toString();
        }

        listDirectory(path);
    }

    private void processRemove(String[] args) throws ShellIOException,
            ShellIllegalUsage, ShellMissingArgumentException {
        boolean recursive = false;
        String path = null;

        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("-r")) {
                recursive = true;
            } else if (path == null) {
                path = args[i];
            } else {
                throw new ShellIllegalUsage(ShellCommands.REMOVE);
            }
        }

        if (path == null) {
            throw new ShellIllegalUsage(ShellCommands.REMOVE);
        }

        remove(path, recursive);
    }

    private void processShowFile(String[] args) throws ShellIOException,
            ShellIllegalUsage, ShellMissingArgumentException {
        int numberOfLines = -1;
        String path = null;

        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("-n") && i < args.length - 1) {
                try {
                    numberOfLines = Integer.valueOf(args[++i]);
                } catch (NumberFormatException err) {
                    throw new ShellIllegalUsage(ShellCommands.SHOW_FILE);
                }
            } else if (path == null) {
                path = args[i];
            } else {
                throw new ShellIllegalUsage(ShellCommands.SHOW_FILE);
            }
        }

        if (path == null) {
            throw new ShellIllegalUsage(ShellCommands.SHOW_FILE);
        }

        showFile(path, numberOfLines);
    }

    private void processWriteToFile(String[] args) throws ShellIOException, ShellIllegalUsage {
        String path = null, message;

        for (int i = 1; i < args.length; i++) {
            if ((args[i].startsWith("\"") && args[i].endsWith("\""))
                    || (args[i].startsWith("'") && args[i].endsWith("'"))) {
                if (path != null) {
                    message = args[i].substring(1, args[i].length() - 1);
                    message = StringEscapeUtils.unescapeJava(message);
                    writeTextToFile(path, message);
                } else {
                    throw new ShellIllegalUsage(ShellCommands.WRITE_FILE);
                }
            } else if (path == null) {
                path = args[i];
            }
        }
    }

    private void processCommand(String command) {
        addProcessedCommand(command);
        String[] args = splitCommands(command);

        try {
            switch (ShellCommands.getEnumCommand(args[0])) {
                case MOVE_DIRECTORY:
                    processMoveDirectory(args);
                    break;
                case MAKE_DIRECTORY:
                    processMakeDirectory(args);
                    break;
                case LIST_DIRECTORY:
                    processlistDirectory(args);
                    break;
                case REMOVE:
                    processRemove(args);
                    break;
                case SHOW_FILE:
                    processShowFile(args);
                    break;
                case MAKE_FILE:
                    processMakeFile(args);
                    break;
                case WRITE_FILE:
                    processWriteToFile(args);
                    break;
                default:
                    System.out.println(String.format("Command \"%s\" is not found", args[0]));
            }
        } catch (ShellIOException | ShellRuntimeException err) {
            System.out.println(err.getMessage());
        }
    }

    public static String[] splitCommands(String command) {
        Pattern spaces = Pattern.compile("\\s+");
        Pattern quotesDouble = Pattern.compile("\"((.|\\n)*?)\"");
        Pattern quotesSingle = Pattern.compile("'((.|\\n)*?)'");

        Matcher matcher;
        List<String> argumentList = new LinkedList<>();

        while (command.contains("\"") || command.contains("\'")) {
            int indexQuotesDouble = command.indexOf("\"");
            int indexQuotesSingle = command.indexOf("\'");

            if (indexQuotesDouble < 0) {
                matcher = quotesSingle.matcher(command);
            } else if (indexQuotesSingle < 0) {
                matcher = quotesDouble.matcher(command);
            } else if (indexQuotesDouble < indexQuotesSingle) {
                matcher = quotesDouble.matcher(command);
            } else if (indexQuotesDouble > indexQuotesSingle) {
                matcher = quotesSingle.matcher(command);
            } else {
                break;
            }

            if (matcher.find()) {
                argumentList.add(matcher.group());
            } else {
                break;
            }

            command = matcher.replaceFirst("");
        }

        List<String> commandsList;

        if (command.length() > 0) {
            commandsList = Arrays.asList(
                    spaces.matcher(command).replaceAll(" ").trim().split(" "));
        } else {
            commandsList = new ArrayList<>();
        }

        String[] output = new String[commandsList.size() + argumentList.size()];
        System.arraycopy(commandsList.toArray(), 0, output, 0, commandsList.size());
        System.arraycopy(argumentList.toArray(), 0, output, commandsList.size(), argumentList.size());
        return output;
    }

    private String introMessage() {
        return currentPath + "$: ";
    }

    private boolean mutex = false;

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if (mutex) {
            return;
        }

        super.nativeKeyPressed(e);

        switch (e.getKeyCode()) {
            case NativeKeyEvent.VC_UP:
            case NativeKeyEvent.VC_DOWN:
                System.out.flush();
                System.out.print("\r");
                System.out.print(introMessage());

                String command = getCurrentCommand();
                if (command != null) {
                    System.out.print(command);
                }
        }
    }

    public void processInputStream(InputStream input, boolean printCommands) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            String command;

            while (true) {
                System.out.print(introMessage());

                if ((command = reader.readLine()) == null) {
                    break;
                } else {
                    command = command.trim();
                }

                command = getCurrentCommand() != null ? getCurrentCommand() : command;

                if (command.length() > 0) {
                    if (printCommands) {
                        System.out.println(command);
                    }

                    mutex = true;
                    processCommand(command);
                    mutex = false;
                } else {
                    if (printCommands) {
                        System.out.print('\n');
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws NativeHookException {
        try {
            Shell shell = new Shell();
            switch (args.length) {
                case 0:
                    GlobalScreen.registerNativeHook();
                    GlobalScreen.addNativeKeyListener(shell);
                    shell.processInputStream(System.in, false);
                    GlobalScreen.removeNativeKeyListener(shell);
                    GlobalScreen.unregisterNativeHook();
                    break;
                case 1:
                    shell.processInputStream(new FileInputStream(args[0]), true);
                    break;
                default:
                    System.out.println("java -jar shell.jar [file_name]");
            }
        } catch (IOException | NativeHookException err) {
            System.out.println(err.getMessage());
        } finally {
            if (GlobalScreen.isNativeHookRegistered()) {
                GlobalScreen.unregisterNativeHook();
            }
        }
    }
}
