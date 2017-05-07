package ru.croc.java2017.shell;

import java.io.*;

import java.nio.file.*;

import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.regex.Pattern;

public class Shell {
    private Path currentPath;

    private static final String LS_FORMAT = "%c%c%c%c %15d %s %s";

    public class ShellIOException extends IOException {
        public static final String MSG_NOT_DIRECTORY = "\"%s\" is not a directory";
        public static final String MSG_NOT_FILE      = "\"%s\" is not a file";
        public static final String MSG_NOT_EXIST     = "\"%s\" doesn't exist";
        public static final String MSG_ALREADY_EXIST = "\"%s\" has already exist";
        public static final String MSG_UNABLE_DELETE = "\"%s\" can't be deleted";
        public static final String MSG_UNABLE_READ   = "\"%s\" can't be read";

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

        public ShellIllegalUsage(String command) {
            super(String.format(MSG_ILLEGAL_USAGE, command));
        }
    }

    public class ShellMissingArgumentException extends ShellRuntimeException {
        private static final String MSG_MISSED_ARGUMENT = "Illegal usage of command \"%s\". Use \"%s\"";

        public ShellMissingArgumentException(String command, String missedArgument) {
            super(String.format(MSG_MISSED_ARGUMENT, command, missedArgument));
        }
    }

    public Shell() {
        currentPath = Paths.get("").toAbsolutePath();
    }

    private Path getAbsolutePath(String path) throws ShellIOException {
        try {
            return currentPath.resolve(path).normalize();
        } catch (InvalidPathException err) {
            throw new ShellIOException(err);
        }
    }

    private void moveDirectory(String path) throws ShellIOException {
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
    }

    private void makeDirectory(String path) throws ShellIOException {
        Path newPath = getAbsolutePath(path);

        try {
            Files.createDirectory(newPath);
        } catch (FileAlreadyExistsException err) {
            throw new ShellIOException(ShellIOException.MSG_ALREADY_EXIST, path);
        } catch (IOException err) {
            while (!Files.exists(newPath.getParent())) {
                newPath = newPath.getParent();
            }
            throw new ShellIOException(ShellIOException.MSG_NOT_EXIST, newPath.toString());
        }
    }

    private void listDirectory(String path) throws ShellIOException {
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
    }

    private void remove(String path, boolean recursive) throws ShellIOException, ShellMissingArgumentException {
        Path newPath = getAbsolutePath(path);

        try {
            if (!Files.isDirectory(newPath)) {
                Files.delete(newPath);
            } else {
                throw new ShellMissingArgumentException("rm", "-r");
            }
        } catch (NoSuchFileException err) {
            throw new ShellIOException(ShellIOException.MSG_NOT_EXIST, path);
        } catch (DirectoryNotEmptyException err) {
            if (recursive) {
                try {
                    Files.walk(newPath).sorted(Comparator.reverseOrder()).forEach((Path p) -> {
                        try {
                            Files.delete(p);
                        } catch (IOException er) {
                            System.out.println(String.format(ShellIOException.MSG_UNABLE_DELETE, p));
                        }
                    });
                } catch (IOException err_) {
                    throw new ShellIOException(ShellIOException.MSG_UNABLE_DELETE, newPath.toString());
                }
            } else {
                throw new ShellMissingArgumentException("rm", "-r");
            }
        } catch (IOException err) {
            throw new ShellIOException(ShellIOException.MSG_UNABLE_DELETE, newPath.toString());
        }
    }

    private void showFile(String path, int numberOfLines) throws ShellIOException {
        Path newPath = getAbsolutePath(path);

        if (Files.isRegularFile(newPath)) {
            try {
                Files.lines(newPath).limit(numberOfLines).forEach(System.out::println);
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
    }

    private void processMoveDirectory(String[] args) throws ShellIOException, ShellIllegalUsage {
        if (args.length == 2) {
            moveDirectory(args[1]);
        } else {
            throw new ShellIllegalUsage("cd");
        }
    }

    private void processMakeDirectory(String[] args) throws ShellIOException, ShellIllegalUsage {
        if (args.length == 2) {
            makeDirectory(args[1]);
        } else {
            throw new ShellIllegalUsage("mkdir");
        }
    }

    private void processlistDirectory(String[] args) throws ShellIOException, ShellIllegalUsage {
        switch (args.length) {
            case 1:
                listDirectory(currentPath.toString());
                break;
            case 2:
                listDirectory(args[1]);
                break;
            default:
                throw new ShellIllegalUsage("ls");
        }
    }

    private void processRemove(String[] args) throws ShellIOException, ShellIllegalUsage {
        if (args.length == 3) {
            boolean recursive = false;
            String path = null;

            for (int i = 1; i < args.length; i++) {
                if (args[i].equals("-r")) {
                    recursive = true;
                } else if (path == null) {
                    path = args[i];
                } else {
                    throw new ShellIllegalUsage("rm");
                }
            }

            remove(path, recursive);
        } else if (args.length == 2) {
            remove(args[1], false);
        } else {
            throw new ShellIllegalUsage("rm");
        }
    }

    private void processShowFile(String[] args) throws ShellIOException, ShellIllegalUsage {
        if (args.length == 4) {
            int numberOfLines = -1;
            String path = null;

            for (int i = 1; i < args.length; i++) {
                if (args[i].equals("-n") && i < args.length - 1) {
                    try {
                        numberOfLines = Integer.valueOf(args[++i]);
                    } catch (NumberFormatException err) {
                        throw new ShellIllegalUsage("head");
                    }
                } else if (path == null) {
                    path = args[i];
                } else {
                    throw new ShellIllegalUsage("head");
                }
            }

            if (numberOfLines < 0) {
                throw new ShellIllegalUsage("head");
            }

            showFile(path, numberOfLines);
        } else {
            throw new ShellIllegalUsage("head");
        }
    }

    private void processCommand(String command) {
        String[] args = command.split(" ");

        try {
            switch (args[0]) {
                case "cd":
                    processMoveDirectory(args);
                    break;
                case "mkdir":
                    processMakeDirectory(args);
                    break;
                case "ls":
                    processlistDirectory(args);
                    break;
                case "rm":
                    processRemove(args);
                    break;
                case "head":
                    processShowFile(args);
                    break;
                default:
                    System.out.println(String.format("Command \"%s\" is not found", args[0]));
            }
        } catch (ShellIOException | ShellRuntimeException err) {
            System.out.println(err.getMessage());
        }
    }

    public void processInputStream(InputStream input, boolean printCommands) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        Pattern spaces = Pattern.compile("\\s+");

        String command;
        while (true) {
            System.out.print(currentPath);
            System.out.print("$: ");

            if ((command = reader.readLine()) == null) {
                break;
            }

            command = spaces.matcher(command).replaceAll(" ").trim();
            if (command.length() > 0) {
                if (printCommands) {
                    System.out.println(command);
                }

                processCommand(command);
            } else if (printCommands) {
                System.out.print('\n');
            }
        }

        reader.close();
    }

    public static void main(String[] args) {
        try {
            Shell shell = new Shell();
            switch (args.length) {
                case 0:
                    shell.processInputStream(System.in, false);
                    break;
                case 1:
                    shell.processInputStream(new FileInputStream(args[0]), true);
                    break;
                default:
                    System.out.println("java -jar shell.jar [file_name]");
            }
        } catch (IOException err) {
            System.out.println(err.getMessage());
        }
    }
}
