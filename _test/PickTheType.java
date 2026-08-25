// Practice #2 - What type for each?
public class PickTheType {
    public static void main(String[] args) {
        // 1. Number of siblings → int (counting, whole number)
        int siblings = 2;
        
        // 2. Your first name → String (text/words)
        String firstName = "Alex";
        
        // 3. Are you hungry? → boolean (yes/no, true/false)
        boolean isHungry = true;
        
        // 4. Your favorite letter → char (single letter)
        char favoriteLetter = 'A';
        
        // 5. Your height in inches → double (might be decimal like 65.5)
        // (Can also be int if your height is a whole number)
        double heightInches = 65.5;
        
        // 6. Days in a year → final int (never changes, whole number)
        final int DAYS_IN_YEAR = 365;
        
        System.out.println("Siblings: " + siblings);
        System.out.println("Name: " + firstName);
        System.out.println("Hungry: " + isHungry);
        System.out.println("Favorite letter: " + favoriteLetter);
        System.out.println("Height: " + heightInches + " inches");
        System.out.println("Days per year: " + DAYS_IN_YEAR);
    }
}

// class method to run the java.
PickTheType.main(null);