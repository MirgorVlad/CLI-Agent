package org.mirgor.console_agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.mirgor.console_agent.service.LlmService;
import org.mirgor.console_agent.service.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.Scanner;

@SpringBootApplication
public class CliAgentApplication implements CommandLineRunner {

    @Autowired
    private LlmService llmService;

    public static void main(String[] args) {
        SpringApplication.run(CliAgentApplication.class, args);
    }

    @Override
    public void run(String... args) throws IOException {
        System.out.println(System.getProperty("user.dir"));
        help();
        mainLoop();
    }

    private void mainLoop() throws IOException {
        while (true) {
            try {
                System.out.print("\nInput: ");
                Scanner scanner = new Scanner(System.in);
                String input = scanner.nextLine().trim();

                if (input.equals("/exit")) {
                    System.exit(0);
                } else if (input.equals("/help")) {
                    help();
                } else if (input.startsWith("#") && !input.startsWith("#file")) {
                    llmService.addDeveloperContext(input);
                } else if (input.startsWith("/models")) {
                    modelSelection(scanner);
                } else if (input.startsWith("/model")) {
                    System.out.println(llmService.getCurrentModel());
                } else if (input.startsWith("/context")) {
                    printContext();
                } else if (input.startsWith("/clear")) {
                    llmService.clearContext();
                    System.out.println("Context is cleared");
                } else if (!input.isBlank()) {
                    validateContextWindow();
                    String modelResponse = llmService.sendUserPrompt(input);
                    System.out.println(modelResponse);
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void printContext() {
        long modelContextWindowSize = llmService.getCurrentModel().getContextWindowSize();
        int contextWindow = llmService.countContextTokens();

        llmService.getChatContext()
                .forEach(message -> System.out.printf("\u001B[1m  - %s:\u001B[0m %s\n", message.role(), message.content()));

        System.out.printf("\u001B[1m %s/%s \u001B[0m \n", contextWindow, modelContextWindowSize);
    }

    private static void help() {
        System.out.println("How can I help you?\n " +
                "\t* Enter text to prompt LLM\n" +
                "\t* Enter '#text' to add message to context\n" +
                "\t* Enter '/models' to select model\n" +
                "\t* Enter '/model' to get current model\n" +
                "\t* Enter '/clear' to clear context\n " +
                "\t* Enter '/context' to see chat context\n " +
                "\t* Enter '/exit' to exit\n" +
                "\t* Enter '/help' to show help'\n" +
                "\t** Use #file FileName to add files to context\n"
        );
    }

    private void modelSelection(Scanner scanner) throws IOException {
        int i = 1;
        System.out.println("Models:");
        for (Model model : Model.values()) {
            System.out.printf("\t%s. %s\n", i++, model.toString());
        }
        String modelNumber = scanner.nextLine().trim();
        try {
            int number = Integer.parseInt(modelNumber);
            Model model = Model.values()[number - 1];
            llmService.setCurrentModel(model);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number: " + modelNumber);
        }
    }

    private void validateContextWindow() {
        double contextSizeCapacityRate = llmService.getContextSizeCapacityRate();
        if (contextSizeCapacityRate > 0.9) {
            System.out.println("\u001B[1m CONTEXT WINDOW CAPACITY > 90% !!! \u001B[0m");
        }
    }

}
