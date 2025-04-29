import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public class ONPSynchronizacja {

    private static final String sciezkaDoPliku = "rownania.txt";

    public static final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    public static final Lock readLock = rwl.readLock();
    public static final Lock writeLock = rwl.writeLock();

    private static final ExecutorService executorCzytajacy = Executors.newCachedThreadPool();
    private static final ExecutorService executorObliczajacy = Executors.newCachedThreadPool();

    public static void main(String[] args) {

        List<Future<String>> przyszleRownania = new ArrayList<>();

        int liczbaLinii = policzLiczbeLiniiWPliku();

        for (int i = 0; i < liczbaLinii; i++) {
            Future<String> przyszleRownanie = executorCzytajacy.submit(new Czytanie(i, sciezkaDoPliku));
            przyszleRownania.add(przyszleRownanie);
        }

        for (Future<String> przyszleRownanie : przyszleRownania) {
            try {
                String rownanie = przyszleRownanie.get();
                if (rownanie != null) {
                    FutureTask<Void> zadanieObliczajace = new FutureTask<>(new Obliczanie(rownanie, sciezkaDoPliku)) {
                        @Override
                        protected void done() {
                            try {
                                get();
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }
                        }
                    };
                    executorObliczajacy.submit(zadanieObliczajace);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        executorCzytajacy.shutdown();
        executorObliczajacy.shutdown();
    }

    private static int policzLiczbeLiniiWPliku() {
        readLock.lock();
        try (BufferedReader czytnik = new BufferedReader(new FileReader(sciezkaDoPliku))) {
            int licznik = 0;
            while (czytnik.readLine() != null) {
                licznik++;
            }
            return licznik;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        } finally {
            readLock.unlock();
        }
    }
}
