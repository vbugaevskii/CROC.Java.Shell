package ru.croc.java2017.shell;

import java.io.*;

import java.nio.file.*;

import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import java.util.regex.Pattern;

public class Shell {
    private Path currentPath;

    private static final String LS_FORMAT = "%c%c%c%c %15d %s %s";

    private static final String MSG_NOT_DIRECTORY = "\"%s\" is not a directory";
    private static final String MSG_NOT_FILE      = "\"%s\" is not a file";
    private static final String MSG_NOT_EXIST     = "\"%s\" doesn't exist";
    private static final String MSG_ALREADY_EXIST = "\"%s\" has already exist";
    private static final String MSG_UNABLE_DELETE = "\"%s\" can't be deleted";
    private static final String MSG_ILLEGAL_USAGE = "Illegal usage of command";

    public Shell() {
        currentPath = Paths.get("").toAbsolutePath();
    }

    private Path getAbsolutePath(String path) throws InvalidPathException {
        return currentPath.resolve(path).normalize();
    }

    private void moveDirectory(String path) {
        Path newPath = getAbsolutePath(path);

        if (Files.exists(newPath)) {
            if (Files.isDirectory(newPath)) {
                currentPath = newPath;
            } else {
                System.out.println(String.format(MSG_NOT_DIRECTORY, path));
            }
        } else {
            System.out.println(String.format(MSG_NOT_EXIST, path));
        }
    }

    private void makeDirectory(String path) {
        Path newPath = getAbsolutePath(path);

        try {
            Files.createDirectory(newPath);
        } catch (FileAlreadyExistsException err) {
            System.out.println(String.format(MSG_ALREADY_EXIST, path));
        } catch (IOException err) {
            while (!Files.exists(newPath.getParent())) {
                newPath = newPath.getParent();
            }
            System.out.println(String.format(MSG_NOT_EXIST, newPath));
        }
    }

    private void listDirectory(String path) {
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
            System.out.println(String.format(MSG_NOT_DIRECTORY, path));
        } catch (IOException err) {
            System.out.println(String.format(MSG_NOT_EXIST, path));
        }
    }

    private void remove(String path, boolean recursive) {
        Path newPath = getAbsolutePath(path);

        try {
            Files.delete(newPath);
        } catch (NoSuchFileException err) {
            System.out.println(String.format(MSG_NOT_EXIST, path));
        } catch (DirectoryNotEmptyException err) {
            if (recursive) {
                try {
                    Files.walk(newPath).sorted(Comparator.reverseOrder()).forEach((Path p) -> {
                        try {
                            Files.delete(p);
                        } catch (IOException er) {
                            System.out.println(String.format(MSG_UNABLE_DELETE, p));
                        }
                    });
                } catch (IOException err_) {
                    System.out.println(String.format(MSG_UNABLE_DELETE, newPath));
                }
            } else {
                System.out.println("Use \"-r\" modificator");
            }
        } catch (IOException err) {
            System.out.println(String.format(MSG_UNABLE_DELETE, newPath));
        }
    }

    private void showFile(String path, int numberOfLines) {
        Path newPath = getAbsolutePath(path);

        if (Files.isRegularFile(newPath)) {
            try {
                Files.lines(newPath).limit(numberOfLines).forEach(System.out::println);
            } catch (IOException err) {
                System.out.print("Didn't manage to open the file");
            }
        } else {
            if (!Files.exists(newPath)) {
                System.out.println(String.format(MSG_NOT_EXIST, path));
            } else {
                System.out.println(String.format(MSG_NOT_FILE, path));
            }
        }
    }

    private void processMoveDirectory(String[] args) {
        if (args.length == 2) {
            moveDirectory(args[1]);
        } else {
            System.out.println(MSG_ILLEGAL_USAGE);
        }
    }

    private void processMakeDirectory(String[] args) {
        if (args.length == 2) {
            makeDirectory(args[1]);
        } else {
            System.out.println(MSG_ILLEGAL_USAGE);
        }
    }

    private void processlistDirectory(String[] args) {
        switch (args.length) {
            case 1:
                listDirectory(currentPath.toString());
                break;
            case 2:
                listDirectory(args[1]);
                break;
            default:
                System.out.println(MSG_ILLEGAL_USAGE);
        }
    }

    private void processRemove(String[] args) {
        if (args.length == 3) {
            boolean recursive = false;
            String path = null;

            for (int i = 1; i < args.length; i++) {
                if (args[i].equals("-r")) {
                    recursive = true;
                } else if (path == null) {
                    path = args[i];
                } else {
                    System.out.println(MSG_ILLEGAL_USAGE);
                }
            }

            remove(path, recursive);
        } else if (args.length == 2) {
            remove(args[1], false);
        } else {
            System.out.println(MSG_ILLEGAL_USAGE);
        }
    }

    private void processShowFile(String[] args) {
        if (args.length == 4) {
            int numberOfLines = -1;
            String path = null;

            for (int i = 1; i < args.length; i++) {
                if (args[i].equals("-n") && i < args.length - 1) {
                    numberOfLines = Integer.valueOf(args[++i]);
                } else if (path == null) {
                    path = args[i];
                } else {
                    System.out.println(MSG_ILLEGAL_USAGE);
                }
            }

            if (numberOfLines < 0) {
                System.out.println(MSG_ILLEGAL_USAGE);
            }

            showFile(path, numberOfLines);
        } else {
            System.out.println(MSG_ILLEGAL_USAGE);
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
        } catch (InvalidPathException err) {
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

    public static void main(String[] args) throws IOException {
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
