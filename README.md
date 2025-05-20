# CLI-Agent
![image](https://github.com/user-attachments/assets/0df74f64-ccfe-4d28-ae13-8742c53ca837)

## Overview
CliAgentApplication is a CLI-based intelligent agent designed to assist with coding tasks, automation, and application management. It leverages language models for natural language processing and provides modular services for extensibility.

## Building the Project

1. Clone the repository:
    ```
    git clone <repository_url>
    cd CLI-Agent
    ```

2. Build the application with Maven:
    ```
    ./mvnw clean package
    ```

3. The executable JAR will be generated in the `target/` directory.

## Setting Environment Variables

Before running the application, set the following environment variables:

- `OPEN_AI_API_KEY` : Your API key for the OpenAI language model API.
- `CLAUDE_API_KEY` : Your API key for the Anthropic Claude language models API.

You can set them in your shell or add to your `/etc/environment`:
```sh
export OPEN_AI_API_KEY=your_key_here
export CLAUDE_API_KEY=your_key_here
```

## Running & Making Executable from Ubuntu Console

1. To run after building:
    ```
    java -jar target/console_agent-1.0.0.jar "$@" --logging.level.root=OFF
    ```

2. To make it executable:
    - Create a Shell Script Launcher  
      Create a file named cli-agent (no file extension) with the following content:

      ```
       #!/bin/bash
       JAVA_CMD=java
       JAR_PATH=/path/to/cli-agent.jar
       exec $JAVA_CMD -jar "$JAR_PATH" "$@" --logging.level.root=OFF
         ```
    - Save this script, give it execute permissions using:  
       ```
       chmod +x cli-agent
        ```
    - Place the Launcher in Your PATH  
      Move the cli-agent script to a directory in your PATH (for example, /usr/local/bin):
         ```
         sudo mv cli-agent /usr/local/bin/
         ```
    - Now you can run the app from anywhere with:
      ```
      cli-agent
      ```
