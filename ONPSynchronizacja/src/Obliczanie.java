import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.Stack;
import java.util.StringTokenizer;

public class Obliczanie implements Callable<Void> {
    private final String rownanie;
    private final String sciezkaDoPliku; // dodajemy ścieżkę pliku jako pole

    public Obliczanie(String rownanie, String sciezkaDoPliku) {
        this.rownanie = rownanie;
        this.sciezkaDoPliku = sciezkaDoPliku;
    }

    @Override
    public Void call() {
        if (rownanie == null || rownanie.isEmpty()) {
            return null;
        }

        double wynik = obliczWyrazenie(rownanie.replace("=", "").trim());

        ONPSynchronizacja.writeLock.lock();
        try {
            List<String> linie = Files.readAllLines(Paths.get(sciezkaDoPliku));
            for (int i = 0; i < linie.size(); i++) {
                if (linie.get(i).trim().equals(rownanie.trim())) {
                    linie.set(i, rownanie + " " + wynik);
                }
            }
            Files.write(Paths.get(sciezkaDoPliku), linie);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ONPSynchronizacja.writeLock.unlock();
        }
        return null;
    }

    private double obliczWyrazenie(String wyrazenie) {
        List<String> onp = przeksztalcNaONP(wyrazenie);
        return obliczONP(onp);
    }

    private List<String> przeksztalcNaONP(String wyrazenie) {
        List<String> wynik = new ArrayList<>();
        Stack<String> stos = new Stack<>();
        StringTokenizer dzielnik = new StringTokenizer(wyrazenie, "()+-*/^", true);

        while (dzielnik.hasMoreTokens()) {
            String element = dzielnik.nextToken().trim();
            if (element.isEmpty()) continue;

            if (czyLiczba(element)) {
                wynik.add(element);
            } else if (element.equals("(")) {
                stos.push(element);
            } else if (element.equals(")")) {
                while (!stos.isEmpty() && !stos.peek().equals("(")) {
                    wynik.add(stos.pop());
                }
                stos.pop();
            } else {
                while (!stos.isEmpty() && priorytet(stos.peek()) >= priorytet(element)) {
                    wynik.add(stos.pop());
                }
                stos.push(element);
            }
        }

        while (!stos.isEmpty()) {
            wynik.add(stos.pop());
        }

        return wynik;
    }

    private double obliczONP(List<String> onp) {
        Stack<Double> stos = new Stack<>();

        for (String element : onp) {
            if (czyLiczba(element)) {
                stos.push(Double.parseDouble(element));
            } else {
                double b = stos.pop();
                double a = stos.pop();
                switch (element) {
                    case "+" -> stos.push(a + b);
                    case "-" -> stos.push(a - b);
                    case "*" -> stos.push(a * b);
                    case "/" -> stos.push(a / b);
                    case "^" -> stos.push(Math.pow(a, b));
                    default -> throw new IllegalArgumentException("Nieznany operator: " + element);
                }
            }
        }
        return stos.pop();
    }

    private boolean czyLiczba(String element) {
        try {
            Double.parseDouble(element);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private int priorytet(String operator) {
        return switch (operator) {
            case "+", "-" -> 1;
            case "*", "/" -> 2;
            case "^" -> 3;
            default -> 0;
        };
    }
}
