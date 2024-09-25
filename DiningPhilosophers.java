import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DiningPhilosophers {

    private static final int NUM_TABLES = 5;
    private static final int NUM_PHILOSOPHERS_PER_TABLE = 5;
    private static final int SIXTH_TABLE_INDEX = 5;
    private static final int THINK_TIME_RANGE = 10;  // 0 to 10 seconds
    private static final int EAT_TIME_RANGE = 5;     // 0 to 5 seconds
    private static final int WAIT_TIME = 4;          // 4 seconds wait time between forks

    // Fork class representing a shared fork with a Lock
    static class Fork {
        private final Lock lock = new ReentrantLock();

        public void pickUp() {
            lock.lock();
        }

        public void putDown() {
            lock.unlock();
        }
    }

    // Philosopher class representing each philosopher's behavior
    static class Philosopher extends Thread {
        private final String name;
        private final Fork leftFork;
        private final Fork rightFork;
        private boolean isHungry;
        private final Table table;

        public Philosopher(String name, Fork leftFork, Fork rightFork, Table table) {
            this.name = name;
            this.leftFork = leftFork;
            this.rightFork = rightFork;
            this.table = table;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    think();
                    if (pickUpForks()) {
                        eat();
                        putDownForks();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        private void think() throws InterruptedException {
            int thinkTime = new Random().nextInt(THINK_TIME_RANGE + 1);
            System.out.println(name + " is thinking for " + thinkTime + " seconds.");
            Thread.sleep(thinkTime * 1000);
        }

        private boolean pickUpForks() throws InterruptedException {
            isHungry = true;
            System.out.println(name + " is hungry and trying to pick up left fork.");
            leftFork.pickUp();
            System.out.println(name + " picked up the left fork.");

            Thread.sleep(WAIT_TIME * 1000); // Wait 4 seconds before trying to pick up right fork

            System.out.println(name + " is trying to pick up right fork.");
            rightFork.pickUp();
            System.out.println(name + " picked up the right fork.");
            isHungry = false;
            return true;
        }

        private void eat() throws InterruptedException {
            int eatTime = new Random().nextInt(EAT_TIME_RANGE + 1);
            System.out.println(name + " is eating for " + eatTime + " seconds.");
            Thread.sleep(eatTime * 1000);
        }

        private void putDownForks() {
            System.out.println(name + " is putting down forks.");
            rightFork.putDown();
            leftFork.putDown();
        }

        public boolean isHungry() {
            return isHungry;
        }

        public String getPhilosopherName() {
            return name;
        }
    }

    // Table class managing philosophers and forks
    static class Table {
        private final int tableId;
        private final List<Philosopher> philosophers = new ArrayList<>();
        private final Fork[] forks = new Fork[NUM_PHILOSOPHERS_PER_TABLE];

        public Table(int tableId) {
            this.tableId = tableId;
            for (int i = 0; i < NUM_PHILOSOPHERS_PER_TABLE; i++) {
                forks[i] = new Fork();
            }
        }

        public void addPhilosopher(Philosopher philosopher) {
            philosophers.add(philosopher);
        }

        public void startPhilosophers() {
            for (Philosopher philosopher : philosophers) {
                philosopher.start();
            }
        }

        public boolean isDeadlocked() {
            // Deadlock occurs when all philosophers at the table are hungry
            for (Philosopher philosopher : philosophers) {
                if (!philosopher.isHungry()) {
                    return false;
                }
            }
            return true;
        }

        public void removePhilosopher(Philosopher philosopher) {
            philosophers.remove(philosopher);
        }

        public boolean hasVacantSeat() {
            return philosophers.size() < NUM_PHILOSOPHERS_PER_TABLE;
        }

        public List<Philosopher> getPhilosophers() {
            return philosophers;
        }
    }

    // Simulation class for running the entire simulation
    static class Simulation {
        private final Table[] tables = new Table[NUM_TABLES + 1];
        private long startTime;
        private Philosopher lastPhilosopherMoved = null;

        public Simulation() {
            // Create tables and assign philosophers to them
            for (int i = 0; i <= NUM_TABLES; i++) {
                tables[i] = new Table(i);
            }

            // Initialize philosophers at the first 5 tables
            int philosopherIndex = 0;
            for (int i = 0; i < NUM_TABLES; i++) {
                for (int j = 0; j < NUM_PHILOSOPHERS_PER_TABLE; j++) {
                    String name = Character.toString((char) ('A' + philosopherIndex));
                    Philosopher philosopher = new Philosopher(name, tables[i].forks[j], tables[i].forks[(j + 1) % NUM_PHILOSOPHERS_PER_TABLE], tables[i]);
                    tables[i].addPhilosopher(philosopher);
                    philosopherIndex++;
                }
            }
        }

        public void runSimulation() throws InterruptedException {
            startTime = System.currentTimeMillis();
            for (int i = 0; i < NUM_TABLES; i++) {
                tables[i].startPhilosophers();
            }
        
            // Periodically check for deadlock and handle movement of philosophers to the sixth table
            while (true) {
                Thread.sleep(2000); // Check every 2 seconds
        
                for (int i = 0; i < NUM_TABLES; i++) {
                    if (tables[i].isDeadlocked()) {
                        handleDeadlock(i);
                    }
                }
        
                if (tables[SIXTH_TABLE_INDEX].isDeadlocked()) {
                    long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
                    System.out.println("Sixth table deadlocked after " + elapsedTime + " seconds.");
                    
                    // Null check for lastPhilosopherMoved
                    if (lastPhilosopherMoved != null) {
                        System.out.println("Last philosopher to move: " + lastPhilosopherMoved.getPhilosopherName());
                    } else {
                        System.out.println("No philosopher has moved to the sixth table yet.");
                    }
                    return;
                }
            }
        }
        
        private void handleDeadlock(int tableIndex) {
            Table table = tables[tableIndex];
            if (!table.getPhilosophers().isEmpty()) {
                Philosopher philosopher = table.getPhilosophers().get(new Random().nextInt(table.getPhilosophers().size()));
                System.out.println("Deadlock at table " + tableIndex + "! Moving " + philosopher.getPhilosopherName() + " to the sixth table.");

                table.removePhilosopher(philosopher);

                // Move the philosopher to the sixth table if there's a vacant seat
                if (tables[SIXTH_TABLE_INDEX].hasVacantSeat()) {
                    tables[SIXTH_TABLE_INDEX].addPhilosopher(philosopher);
                    lastPhilosopherMoved = philosopher;
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Simulation simulation = new Simulation();
        simulation.runSimulation();
    }
}
