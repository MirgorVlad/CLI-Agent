package org.mirgor.console_agent;

import org.mirgor.console_agent.service.LlmService;
import org.mirgor.console_agent.service.Profile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.Scanner;

@SpringBootApplication
public class CliAgentApplication implements CommandLineRunner {

    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";
    private static final String RED = "\u001B[31m";
    private static final String PROMPT = "➜ ";

    @Autowired
    private LlmService llmService;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CliAgentApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }

    @Override
    public void run(String... args) throws IOException {
        printLogo();
        System.out.println(GREEN + "Working directory: " + CYAN + System.getProperty("user.dir") + RESET);
        help();
        mainLoop();
    }

    private void mainLoop() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            try {
                Profile currentProfile = llmService.getCurrentProfile();
                System.out.print("\n" + BOLD + BLUE + PROMPT + "[" + currentProfile + "] " + RESET);

                String input = scanner.nextLine().trim();

                if (input.equals("/exit")) {
                    System.out.println(YELLOW + "Goodbye!" + RESET);
                    System.exit(0);
                } else if (input.equals("/help")) {
                    help();
                } else if (input.startsWith("/profiles")) {
                    profileSelection(scanner);
                } else if (input.startsWith("/context")) {
                    printContext();
                } else if (input.startsWith("/clear")) {
                    llmService.clearContext();
                    System.out.println(GREEN + "✓ Context cleared" + RESET);
                } else if (!input.isBlank()) {
                    validateContextWindow();
                    System.out.println(CYAN + "Processing your request..." + RESET);
                    String modelResponse = llmService.sendUserPrompt(input);
                    System.out.println(YELLOW + "▶ " + RESET + modelResponse);
                }
            } catch (Exception e) {
                System.out.println(RED + "Error: " + e.getMessage() + RESET);
            }
        }
    }

    private void printContext() {
        long modelContextWindowSize = llmService.getCurrentProfile().getModel().getContextWindowSize();
        int contextWindow = llmService.countContextTokens();

        System.out.println(BOLD + "\nChat Context:" + RESET);
        llmService.getChatContext().forEach(message ->
                System.out.printf(BOLD + BLUE + "  %s: " + RESET + "%s\n",
                        message.role(), message.content())
        );

        double usagePercentage = (double) contextWindow / modelContextWindowSize * 100;
        String usageColor = usagePercentage > 80 ? RED : (usagePercentage > 60 ? YELLOW : GREEN);

        System.out.printf("\n" + BOLD + "Context usage: " + usageColor + "%d/%d tokens (%.1f%%)" + RESET + "\n",
                contextWindow, modelContextWindowSize, usagePercentage);
    }

    private static void help() {
        System.out.println(BOLD + "\n📚 Available Commands:" + RESET);
        System.out.println(CYAN + "  • " + RESET + "Enter text to prompt LLM");
        System.out.println(CYAN + "  • " + RESET + BOLD + "/profiles" + RESET + " - Select model profile");
        System.out.println(CYAN + "  • " + RESET + BOLD + "/clear" + RESET + "    - Clear context");
        System.out.println(CYAN + "  • " + RESET + BOLD + "/context" + RESET + "  - Show chat context");
        System.out.println(CYAN + "  • " + RESET + BOLD + "/exit" + RESET + "     - Exit application");
        System.out.println(CYAN + "  • " + RESET + BOLD + "/help" + RESET + "     - Show this help");
        System.out.println(YELLOW + "\n💡 Tip: " + RESET + "Use " + BOLD + "#file FileName" + RESET + " to add files to context\n");
    }

    private void profileSelection(Scanner scanner) throws IOException {
        System.out.println(BOLD + "\n🔧 Available Profiles:" + RESET);
        Profile[] profiles = Profile.values();

        for (int i = 0; i < profiles.length; i++) {
            Profile profile = profiles[i];
            boolean isSelected = profile == llmService.getCurrentProfile();
            String prefix = isSelected ? GREEN + "✓ " : "  ";
            System.out.printf(prefix + CYAN + "%d. " + RESET + "%s\n", i + 1, profile.toString());
        }

        System.out.print(BOLD + "\nEnter profile number (1-" + profiles.length + "): " + RESET);
        String profileNumber = scanner.nextLine().trim();

        try {
            int number = Integer.parseInt(profileNumber);
            if (number < 1 || number > profiles.length) {
                System.out.println(RED + "⚠ Invalid profile number. Please enter 1-" + profiles.length + RESET);
                return;
            }

            Profile profile = profiles[number - 1];
            llmService.setCurrentProfile(profile);
            System.out.println(GREEN + "✓ Profile set to: " + profile + RESET);
        } catch (NumberFormatException e) {
            System.out.println(RED + "⚠ Invalid input: " + profileNumber + RESET);
        }
    }

    private void validateContextWindow() {
        double contextSizeCapacityRate = llmService.getContextSizeCapacityRate();
        if (contextSizeCapacityRate > 0.9) {
            System.out.println(RED + BOLD + "⚠ WARNING: Context window capacity > 90%! Consider clearing context." + RESET);
        } else if (contextSizeCapacityRate > 0.7) {
            System.out.println(YELLOW + "⚠ Note: Context window capacity > 70%" + RESET);
        }
    }

    private void printLogo() {
        System.out.println(CYAN + BOLD +
                "\n" +
                "         ___  __    ____          __    ___  ____  _  _  ____\n" +
                "        / __)(  )  (_  _)  ___   /__\\  / __)( ___)( \\( )(_  _)\n" +
                "       ( (__  )(__  _)(_  (___) /(__)\\( (_-. )__)  )  (   )(  \n" +
                "        \\___)(____)(____)      (__)(__)\\___/(____)(_)\\_) (__)\n" +
                "\n" +
                RESET);
    }
}