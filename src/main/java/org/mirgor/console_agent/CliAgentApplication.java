package org.mirgor.console_agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.mirgor.console_agent.service.LlmWebClient;
import org.mirgor.console_agent.service.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Scanner;

@SpringBootApplication
public class CliAgentApplication implements CommandLineRunner {

    private Model model = Model.GPT_4_1;

    @Autowired
    private LlmWebClient llmWebClient;

    public static void main(String[] args) {
        SpringApplication.run(CliAgentApplication.class, args);
    }

    @Override
    public void run(String... args) throws JsonProcessingException {
        help();
        mainLoop();
    }

    private void mainLoop() throws JsonProcessingException {
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
            } else if (input.startsWith("/models")) {
                modelSelection(scanner);
            } else if (input.startsWith("/model")) {
                System.out.println(model);
            } else if (input.startsWith("/clear")) {
                System.out.println("Clear context");
            } else if (!input.isBlank()) {
                String modelResponse = llmWebClient.getModelResponse(model, input);
                System.out.println(modelResponse);
            }
        }
    }

    private static void help() {
        System.out.println("How can I help you?\n " +
                "\t* Enter text to prompt LLM\n" +
                "\t* Enter '#text' to add message to context\n" +
                "\t* Enter '/models' to select model\n" +
                "\t* Enter '/model' to get current model\n" +
                "\t* Enter '/clear' to clear context\n " +
                "\t* Enter '/exit' to exit\n" +
                "\t* Enter '/help' to show help'\n" +
                "\t** Use #file 'FileName1,FileName2' to add files to context in prompt\n"
        );
    }

    private void modelSelection(Scanner scanner) {
        int i = 1;
        System.out.println("Models:");
        for (Model model : Model.values()) {
            System.out.printf("\t%s. %s\n", i++, model.toString());
        }
        String modelNumber = scanner.nextLine().trim();
        try {
            int number = Integer.parseInt(modelNumber);
            model = Model.values()[number-1];
        } catch (NumberFormatException e) {
            System.out.println("Invalid number: " + modelNumber);
        }
    }

}
