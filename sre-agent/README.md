# sre-agent

## Overview
The `sre-agent` project is a Spring Boot application designed to provide AI functionalities through a RESTful API. It serves as a framework for integrating AI models and processing data efficiently.

## Project Structure
```
sre-agent
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── example
│   │   │           └── sreagent
│   │   │               ├── SreAgentApplication.java
│   │   │               ├── controller
│   │   │               │   └── AiController.java
│   │   │               ├── service
│   │   │               │   └── AiService.java
│   │   │               └── config
│   │   │                   └── AiConfig.java
│   │   └── resources
│   │       ├── application.properties
│   │       └── prompts
│   │           └── system-prompt.txt
│   └── test
│       └── java
│           └── com
│               └── example
│                   └── sreagent
│                       └── SreAgentApplicationTests.java
├── pom.xml
└── README.md
```

## Setup Instructions
1. **Clone the Repository**
   ```bash
   git clone <repository-url>
   cd sre-agent
   ```

2. **Build the Project**
   Ensure you have Maven installed, then run:
   ```bash
   mvn clean install
   ```

3. **Run the Application**
   You can start the application using:
   ```bash
   mvn spring-boot:run
   ```

## Usage
Once the application is running, you can access the API endpoints defined in the `AiController` class. The application is designed to handle various AI-related requests.

## Configuration
The application properties can be configured in the `src/main/resources/application.properties` file. This includes settings for server ports, database connections, and other configurations.

## Testing
Unit tests are provided in the `src/test/java/com/example/sreagent/SreAgentApplicationTests.java` file. You can run the tests using:
```bash
mvn test
```

## Contributing
Contributions are welcome! Please submit a pull request or open an issue for any enhancements or bug fixes.

## License
This project is licensed under the MIT License. See the LICENSE file for more details.