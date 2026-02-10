import java.io.Serializable;

/**
 * Represents a Student with name, GPA, and an internal system ID.
 * The class is Serializable to support persistence.
 */
public class Student implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String name;
    private double gpa;
    
    // Internal system ID - marked as transient to prevent serialization
    private transient int systemId;
    
    // Static counter for generating system IDs
    private static final int INITIAL_ID = 1000;
    private static int idCounter = INITIAL_ID;
    
    /**
     * Constructs a Student with the given name and GPA.
     * Automatically assigns a unique system ID.
     */
    public Student(String name, double gpa) {
        this.name = name;
        this.gpa = gpa;
        this.systemId = idCounter++;
    }
    
    // Getters
    public String getName() {
        return name;
    }
    
    public double getGpa() {
        return gpa;
    }
    
    public int getSystemId() {
        return systemId;
    }
    
    // Setters
    public void setName(String name) {
        this.name = name;
    }
    
    public void setGpa(double gpa) {
        this.gpa = gpa;
    }
    
    @Override
    public String toString() {
        return String.format("Student{name='%s', gpa=%.2f, systemId=%d}", 
                           name, gpa, systemId);
    }
}
