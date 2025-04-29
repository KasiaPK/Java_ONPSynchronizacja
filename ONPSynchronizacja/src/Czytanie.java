import java.io.*;
import java.util.concurrent.Callable;

public class Czytanie implements Callable<String> {
    private final int numerLinii;
    private final String sciezkaDoPliku; // dodajemy ścieżkę pliku jako pole

    public Czytanie(int numerLinii, String sciezkaDoPliku) {
        this.numerLinii = numerLinii;
        this.sciezkaDoPliku = sciezkaDoPliku;
    }

    @Override
    public String call() {
        ONPSynchronizacja.readLock.lock();
        try (BufferedReader czytnik = new BufferedReader(new FileReader(sciezkaDoPliku))) {
            int aktualnaLinia = 0;
            String wczytana;
            while ((wczytana = czytnik.readLine()) != null) {
                if (aktualnaLinia == numerLinii) {
                    return wczytana;
                }
                aktualnaLinia++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ONPSynchronizacja.readLock.unlock();
        }
        return null;
    }
}
