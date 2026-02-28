package com.example;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String input;

        System.out.println("Type something and press Enter (type 'exit' to quit):");

        while (true) {
            input = scanner.nextLine();

            // Check for specific input
            if (input.equalsIgnoreCase("7")) {

            } else if (input.equalsIgnoreCase("8")) {

            } else if (input.equalsIgnoreCase("9")) {

            } else if (input.equalsIgnoreCase("0")) {
                System.out.println("Exiting...");
                break;
            } else {
                System.out.println("You entered: " + input);
            }
        }

        scanner.close();
    }
}
