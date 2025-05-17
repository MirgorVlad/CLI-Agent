package org.mirgor.console_agent;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Scanner;

@SpringBootApplication
public class CliAgentApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(CliAgentApplication.class, args);
    }

    @Override
    public void run(String... args) {
        help();
        mainLoop();
    }

    private static void mainLoop() {
        while (true) {
            System.out.print("\nInput: ");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine().trim();

            if (input.equals("/exit")) {
                System.exit(0);
            } else if (input.equals("/help")) {
                help();
            } else if (input.startsWith("#") && !input.startsWith("#file")) {
                System.out.println("Added to context");
            } else if (input.startsWith("/model")) {
                System.out.println("Model selection");
            } else if (input.startsWith("/clear")) {
                System.out.println("Clear context");
            } else if (!input.isBlank()) {
                System.out.println("Response from LLM");
            }
        }
    }

    private static void help() {
        System.out.println("How can I help you?\n " +
                "\t* Enter text to prompt LLM\n" +
                "\t* Enter '#text' to add message to context\n" +
                "\t* Enter '/model' to select model\n" +
                "\t* Enter '/clear' to clear context\n " +
                "\t* Enter '/exit' to exit\n" +
                "\t* Enter '/help' to show help'\n" +
                "\t** Use #file 'FileName1,FileName2' to add files to context in prompt\n"
        );
    }

}
