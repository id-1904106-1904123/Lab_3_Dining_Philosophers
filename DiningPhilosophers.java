import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class DiningPhilosophers {

    private static final int NUM_TABLES = 5;
    private static final int PHILOSOPHERS_PER_TABLE = 5;
    private static final int FORKS_PER_TABLE = 5;

    private static Lock[][] forks = new ReentrantLock[NUM_TABLES + 1][FORKS_PER_TABLE]; // 6th table for deadlock moves
    private static Lock sixthTableLock = new ReentrantLock(); // Lock to manage movement to the 6th table
    private static int philosophersAtSixthTable = 0; // Count of philosophers at the 6th table
    private static Lock deadlockLock = new ReentrantLock(); // Deadlock detection lock
    private static volatile boolean deadlockReached = false; // Flag to indicate when deadlock is reached

    private static final Random random = new Random();

    public static void main(String[] args) {
        // Initialize locks for each fork on all tables
        for (int i = 0; i < NUM_TABLES + 1; i++) {
            for (int j = 0; j < FORKS_PER_TABLE; j++) {
                forks[i][j] = new ReentrantLock();
            }
        }

        // Create and start philosopher threads for each table
        Thread[] philosopherThreads = new Thread[NUM_TABLES * PHILOSOPHERS_PER_TABLE];
        for (int table = 0; table < NUM_TABLES; table++) {
            for (int i = 0; i < PHILOSOPHERS_PER_TABLE; i++) {
                int philosopherId = table * PHILOSOPHERS_PER_TABLE + i;
                philosopherThreads[philosopherId] = new Thread(new Philosopher(philosopherId, table));
                philosopherThreads[philosopherId].start();
            }
        }

        // Wait for all threads to finish
        for (Thread philosopherThread : philosopherThreads) {
            try {
                philosopherThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    static class Philosopher implements Runnable {

        private int id;
        private int table;

        public Philosopher(int id, int table) {
            this.id = id;
            this.table = table;
        }

        @Override
        public void run() {
            try {
                while (!deadlockReached) {  // Stop if deadlock is reached
                    think();
                    if (deadlockReached) break;
                    pickUpForks();
                    if (deadlockReached) break;
                    eat();
                    if (deadlockReached) break;
                    putDownForks();
                    if (deadlockReached) break;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void think() throws InterruptedException {
            if (deadlockReached) return;
            int thinkingTime = random.nextInt(10); // Random thinking time between 0 and 10 seconds
            System.out.println("Philosopher " + (char) ('A' + id) + " is thinking for " + thinkingTime + " seconds.");
            TimeUnit.SECONDS.sleep(thinkingTime);
        }

        private void pickUpForks() throws InterruptedException {
            if (deadlockReached) return;
            Lock leftFork = forks[table][id % FORKS_PER_TABLE];
            Lock rightFork = forks[table][(id + 1) % FORKS_PER_TABLE];

            // Try to pick up left fork
            leftFork.lock();
            System.out.println("Philosopher " + (char) ('A' + id) + " picked up left fork.");

            // Simulate delay before trying to pick up right fork
            TimeUnit.SECONDS.sleep(4);

            // Try to pick up right fork, detect deadlock and move to sixth table if deadlocked
            if (!rightFork.tryLock()) {
                System.out.println("Philosopher " + (char) ('A' + id) + " couldn't pick up right fork, moving to the sixth table.");
                moveToSixthTable();
                return;
            }

            System.out.println("Philosopher " + (char) ('A' + id) + " picked up right fork.");
        }

        private void eat() throws InterruptedException {
            if (deadlockReached) return;
            int eatingTime = random.nextInt(5); // Random eating time between 0 and 5 seconds
            System.out.println("Philosopher " + (char) ('A' + id) + " is eating for " + eatingTime + " seconds.");
            TimeUnit.SECONDS.sleep(eatingTime);
        }

        private void putDownForks() {
            Lock leftFork = forks[table][id % FORKS_PER_TABLE];
            Lock rightFork = forks[table][(id + 1) % FORKS_PER_TABLE];

            // Put down right fork
            if (rightFork.tryLock()) {
                rightFork.unlock();
                System.out.println("Philosopher " + (char) ('A' + id) + " put down right fork.");
            }

            // Put down left fork
            leftFork.unlock();
            System.out.println("Philosopher " + (char) ('A' + id) + " put down left fork.");
        }

        private void moveToSixthTable() throws InterruptedException {
            sixthTableLock.lock();
            try {
                if (philosophersAtSixthTable < PHILOSOPHERS_PER_TABLE && !deadlockReached) {
                    philosophersAtSixthTable++;
                    System.out.println("Philosopher " + (char) ('A' + id) + " moved to the sixth table.");
                    if (philosophersAtSixthTable == PHILOSOPHERS_PER_TABLE) {
                        System.out.println("Sixth table has entered deadlock. Last philosopher to move: " + (char) ('A' + id));
                        deadlockReached = true;  // Set the flag when deadlock is reached
                    }
                    return;
                }
            } finally {
                sixthTableLock.unlock();
            }
        }
    }
}
