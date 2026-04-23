package dev.drperky.utils;

public class Logger {

    public static void Info(String... n) {
        PrintWithFormat("[INFO]", n);
    }

    public static void Warn(String... n) {
        PrintWithFormat("[WARN]", n);
    }

    public static void Error(String... n) {
        PrintWithFormat("[ERROR]", n);
    }

    private static void PrintWithFormat(String type, String... n) {
        System.out.print(type + " ");

        for (String _s : n) {
            System.out.print(_s);
        }
        System.out.println();
    }
}
