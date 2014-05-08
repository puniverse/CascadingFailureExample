package co.paralleluniverse.examples.comsatjetty;

public class NewClass1 {
    public static void main(String[] args) {
        try {
            throw new RuntimeException("e1");
        } catch(RuntimeException ex) {
            throw new RuntimeException("e2");
        } finally {
            throw new RuntimeException("e3");
        }
    }
}
