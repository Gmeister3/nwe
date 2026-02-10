import java.io.*;
import java.util.*;
import java.util.function.Predicate;

/**
 * Main class demonstrating the Persistent Student Management System.
 * This program showcases:
 * - Collections (ArrayList)
 * - Lambda Expressions and Functional Interfaces
 * - Character-Based I/O (FileReader/FileWriter)
 * - Object Serialization
 * - Method References
 */
public class Main {
    private static final String INPUT_FILE = "input";
    private static final String OUTPUT_FILE = "output.txt";
    private static final String SERIALIZED_FILE = "students.ser";
    private static final int RANDOM_SEED = 123;
    
    public static void main(String[] args) {
        System.out.println("=== Persistent Student Management System ===\n");
        
        // Step 1: Read student names from input file using Character Streams
        List<String> studentNames = readStudentNamesFromFile(INPUT_FILE);
        System.out.println("Student names read from file:");
        studentNames.forEach(System.out::println); // Method Reference
        System.out.println();
        
        // Step 2: Create Student objects with random GPAs and store in ArrayList
        List<Student> students = createStudentList(studentNames);
        System.out.println("Initial Student List:");
        students.forEach(System.out::println); // Method Reference
        System.out.println();
        
        // Step 3: Sort students by GPA using Lambda Expression
        students.sort((s1, s2) -> Double.compare(s2.getGpa(), s1.getGpa()));
        System.out.println("Students sorted by GPA (descending):");
        students.forEach(System.out::println); // Method Reference
        System.out.println();
        
        // Step 4: Filter students using Predicate and Lambda Expression
        Predicate<Student> highAchiever = student -> student.getGpa() >= 3.5;
        System.out.println("High Achievers (GPA >= 3.5):");
        students.stream()
                .filter(highAchiever)
                .forEach(System.out::println); // Method Reference
        System.out.println();
        
        // Step 5: Write filtered results to output file using FileWriter
        writeFilteredStudentsToFile(students, highAchiever, OUTPUT_FILE);
        System.out.println("High achievers written to " + OUTPUT_FILE + "\n");
        
        // Step 6: Demonstrate Object Serialization
        serializeStudents(students, SERIALIZED_FILE);
        System.out.println("Students serialized to " + SERIALIZED_FILE);
        System.out.println("Note: systemId (transient) will not be serialized\n");
        
        // Step 7: Demonstrate Object Deserialization
        List<Student> deserializedStudents = deserializeStudents(SERIALIZED_FILE);
        System.out.println("Students deserialized from " + SERIALIZED_FILE + ":");
        deserializedStudents.forEach(System.out::println); // Method Reference
        System.out.println("\nNotice: systemId is 0 after deserialization (transient field not saved)");
        
        System.out.println("\n=== Demonstration Complete ===");
    }
    
    /**
     * Reads student names from a text file using Character-Based I/O (FileReader).
     * Demonstrates sequential access with Character Streams.
     */
    private static List<String> readStudentNamesFromFile(String filename) {
        List<String> names = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    names.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        
        return names;
    }
    
    /**
     * Creates a list of Student objects with random GPAs.
     */
    private static List<Student> createStudentList(List<String> names) {
        List<Student> students = new ArrayList<>();
        Random random = new Random(RANDOM_SEED); // Fixed seed for reproducibility
        
        for (String name : names) {
            double gpa = 2.5 + random.nextDouble() * 1.5; // GPA between 2.5 and 4.0
            students.add(new Student(name, gpa));
        }
        
        return students;
    }
    
    /**
     * Writes filtered students to a file using Character-Based I/O (FileWriter).
     * Demonstrates data transformation using Predicate.
     */
    private static void writeFilteredStudentsToFile(List<Student> students, 
                                                     Predicate<Student> filter,
                                                     String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("High Achieving Students (GPA >= 3.5)\n");
            writer.write("=====================================\n\n");
            
            for (Student student : students) {
                if (filter.test(student)) {
                    writer.write(String.format("%s - GPA: %.2f\n", 
                                              student.getName(), student.getGpa()));
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
        }
    }
    
    /**
     * Serializes the list of students to a file using ObjectOutputStream.
     * Demonstrates Byte-Based I/O for object persistence.
     */
    private static void serializeStudents(List<Student> students, String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(filename))) {
            oos.writeObject(students);
        } catch (IOException e) {
            System.err.println("Error serializing students: " + e.getMessage());
        }
    }
    
    /**
     * Deserializes the list of students from a file using ObjectInputStream.
     * Demonstrates reading persisted object state.
     */
    @SuppressWarnings("unchecked")
    private static List<Student> deserializeStudents(String filename) {
        List<Student> students = new ArrayList<>();
        
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(filename))) {
            students = (List<Student>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error deserializing students: " + e.getMessage());
        }
        
        return students;
    }
}
