import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LibraryBookTracker {

    public static void main(String[] args) {

        File catalogFile = null;

        int validRecords = 0;
        int searchResults = 0;
        int booksAdded = 0;
        int errorsEncountered = 0;

        try {

            if (args.length < 2) {
                throw new InsufficientArgumentsException(
                        "Fewer than two command-line arguments provided.");
            }

            String filePath = args[0];
            String input = args[1];

            if (!filePath.endsWith(".txt")) {
                throw new InvalidFileNameException(
                        "First argument must be a .txt file.");
            }

            catalogFile = new File(filePath);

            if (!catalogFile.exists()) {
                catalogFile.createNewFile();
            }

            if (input.contains(":")) {

                String[] parts = input.split(":");
                if (parts.length != 4)
                    throw new MalformedBookEntryException(
                            "Book entry must contain exactly 4 fields.");

                String title = parts[0].trim();
                String author = parts[1].trim();
                String ISBN = parts[2].trim();
                String copiesStr = parts[3].trim();

                if (title.isEmpty() || author.isEmpty())
                    throw new MalformedBookEntryException(
                            "Title and Author cannot be empty.");

                if (!ISBN.matches("\\d{13}"))
                    throw new InvalidISBNException(
                            "ISBN must be exactly 13 digits.");

                int copies;
                try {
                    copies = Integer.parseInt(copiesStr);
                    if (copies <= 0)
                        throw new MalformedBookEntryException(
                                "Copies must be positive.");
                } catch (NumberFormatException e) {
                    throw new MalformedBookEntryException(
                            "Copies must be numeric.");
                }

                List<String> books = readAllBooks(catalogFile);
                validRecords = books.size();

                books.add(title + ":" + author + ":" + ISBN + ":" + copies);

                Collections.sort(books, (b1, b2) -> {
                    String t1 = b1.split(":")[0].toLowerCase();
                    String t2 = b2.split(":")[0].toLowerCase();
                    return t1.compareTo(t2);
                });

                writeAllBooks(catalogFile, books);
                booksAdded = 1;

                System.out.println("\nBook Added Successfully:\n");
                printHeader();
                printBook(title, author, ISBN, String.valueOf(copies));
            }

            else {

                List<String> books = readAllBooks(catalogFile);
                validRecords = books.size();
                boolean found = false;

                if (input.matches("\\d{13}")) {

                    int count = 0;
                    String[] matchedBook = null;

                    for (String line : books) {
                        String[] parts = validateBook(line);

                        if (parts[2].equals(input)) {
                            count++;
                            matchedBook = parts;
                        }
                    }

                    if (count > 1)
                        throw new DuplicateISBNException(
                                "Multiple books found with same ISBN.");

                    if (count == 1) {
                        printHeader();
                        printBook(matchedBook[0], matchedBook[1],
                                matchedBook[2], matchedBook[3]);
                        found = true;
                        searchResults = 1;
                    }
                }

                else {

                    printHeader();

                    for (String line : books) {
                        String[] parts = validateBook(line);

                        if (parts[0].toLowerCase()
                                .contains(input.toLowerCase())) {

                            printBook(parts[0], parts[1],
                                    parts[2], parts[3]);
                            found = true;
                            searchResults++;
                        }
                    }
                }

                if (!found)
                    System.out.println("No matching book found.");
            }

        }

        catch (InsufficientArgumentsException e) {
            errorsEncountered++;
            System.out.println("Argument Error: " + e.getMessage());
            logErrorSafe(args, catalogFile, e);
        }

        catch (InvalidFileNameException e) {
            errorsEncountered++;
            System.out.println("File Name Error: " + e.getMessage());
            logErrorSafe(args, catalogFile, e);
        }

        catch (InvalidISBNException e) {
            errorsEncountered++;
            System.out.println("ISBN Error: " + e.getMessage());
            logErrorSafe(args, catalogFile, e);
        }

        catch (DuplicateISBNException e) {
            errorsEncountered++;
            System.out.println("Duplicate ISBN Error: " + e.getMessage());
            logErrorSafe(args, catalogFile, e);
        }

        catch (MalformedBookEntryException e) {
            errorsEncountered++;
            System.out.println("Format Error: " + e.getMessage());
            logErrorSafe(args, catalogFile, e);
        }

        catch (FileNotFoundException e) {
            errorsEncountered++;
            System.out.println("File Not Found: " + e.getMessage());
            logErrorSafe(args, catalogFile, e);
        }

        catch (IOException e) {
            errorsEncountered++;
            System.out.println("I/O Error occurred.");
            logErrorSafe(args, catalogFile, e);
        }

        catch (Exception e) {
            errorsEncountered++;
            System.out.println("Unexpected Error: " + e.getMessage());
            logErrorSafe(args, catalogFile, e);
        }

        finally {

            System.out.println();
            System.out.println("Number of valid records processed from the catalog file: " + validRecords);
            System.out.println("Number of search results: " + searchResults);
            System.out.println("Number of books added: " + booksAdded);
            System.out.println("Number of errors encountered: " + errorsEncountered);
            System.out.println("Thank you for using the Library Book Tracker.");
        }
    }

    private static void logErrorSafe(String[] args, File catalogFile, Exception e) {
        if (catalogFile != null && args.length >= 2) {
            logError(catalogFile, args[1], e);
        }
    }

    private static void logError(File catalogFile, String offendingText, Exception e) {
        try {
            File errorFile = new File(catalogFile.getParent(), "errors.log");
            FileWriter writer = new FileWriter(errorFile, true);

            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            writer.write("[" + timestamp + "] ");
            writer.write("INVALID: \"" + offendingText + "\" ");
            writer.write(e.getClass().getSimpleName() + ": " + e.getMessage());
            writer.write(System.lineSeparator());

            writer.close();

        } catch (IOException ignored) {}
    }

    private static List<String> readAllBooks(File file) throws IOException {
        List<String> books = new ArrayList<>();
        Scanner scanner = new Scanner(file);

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();

            if (!line.isEmpty()) {
                try {
                    validateBook(line);
                    books.add(line);
                } catch (Exception e) {
                    logError(file, line, e);
                }
            }
        }

        scanner.close();
        return books;
    }

    private static void writeAllBooks(File file, List<String> books)
            throws IOException {

        FileWriter writer = new FileWriter(file, false);

        for (String book : books) {
            writer.write(book + "\n");
        }

        writer.close();
    }

    private static String[] validateBook(String line)
            throws MalformedBookEntryException, InvalidISBNException {

        String[] parts = line.split(":");

        if (parts.length != 4)
            throw new MalformedBookEntryException("Invalid book format.");

        if (!parts[2].matches("\\d{13}"))
            throw new InvalidISBNException("Stored ISBN invalid.");

        return parts;
    }

    private static void printHeader() {
        System.out.printf("%-25s %-20s %-15s %-6s%n",
                "Title", "Author", "ISBN", "Copies");
        System.out.println("-------------------------------------------------------------------");
    }

    private static void printBook(String title, String author,
                                  String ISBN, String copies) {

        System.out.printf("%-25s %-20s %-15s %-6s%n",
                title, author, ISBN, copies);
    }
}