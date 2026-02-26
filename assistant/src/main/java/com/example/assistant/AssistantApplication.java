package com.example.assistant;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class AssistantApplication {

	public static void main(String[] args) {
		SpringApplication.run(AssistantApplication.class, args);
	}

	@Bean
	ApplicationRunner seedCustomers(CustomerRepository customerRepository, VectorStore vectorStore) {
		return args -> {
			if (customerRepository.count() == 0) {
				customerRepository.saveAll(List.of(
						new Customer(null, "FreshMart Downtown", "New York", "Supermarket"),
						new Customer(null, "Harbor Grocers", "Boston", "Supermarket"),
						new Customer(null, "Sunrise Supermarket", "Miami", "Supermarket"),
						new Customer(null, "Oak Bistro", "Chicago", "Restaurant"),
						new Customer(null, "Cedar Grill", "Austin", "Restaurant"),
						new Customer(null, "Seaside Diner", "San Diego", "Restaurant"),
						new Customer(null, "Grandview Hotel", "Seattle", "Hotel"),
						new Customer(null, "Riverside Inn", "Denver", "Hotel"),
						new Customer(null, "Northside Fuel", "Portland", "Gas Station"),
						new Customer(null, "Pinecrest Gas", "Raleigh", "Gas Station")
				));
			}

			List<Customer> customers = new ArrayList<>();
			customerRepository.findAll().forEach(customers::add);

			List<Document> documents = customers.stream()
					.map(customer -> new Document(
							"customer-" + customer.id(),
							"Customer " + customer.name() + " is a " + customer.type() + " type of customer in " + customer.location() + ".",
							Map.of(
									"customer-id", customer.id(),
									"customer-name", customer.name(),
									"customer-location", customer.location(),
									"customer-type", customer.type()
							)
					))
					.toList();

			vectorStore.add(documents);
		};
	}

	@Bean
	VectorStore vectorStore(EmbeddingModel embeddingModel) {
		return SimpleVectorStore.builder(embeddingModel).build();
	}

}

interface CustomerRepository extends ListCrudRepository<Customer, Integer> {
	List<Customer> findByType(String type);
}

record Customer(@Id Integer id, String name, String location, String type) {
}

record Product(@Id Integer id, String name, String category, double price) {
}

record Order(@Id Integer id, Customer customer, Date orderDate, List<OrderItem> orderItems) {
}

record OrderItem(@Id Integer id, Order order, Product product, int quantity) {
}


@RestController
class AssistantController {

    private final ChatClient chatClient;

    private final Map<String, PromptChatMemoryAdvisor> memory = new ConcurrentHashMap<>();

	AssistantController(ChatClient.Builder builder, CustomerTools customerTools, VectorStore vectorStore) {
		var qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore).build();
		this.chatClient = builder
            .defaultSystem("""
                You are an Sales and Marketing Agent. 
				You have access to the commercial database and can answer questions.
				First use the tools at your disposal to find the relevant information, if not then refer to the advisors.
				If there is no information, then return a polite response indicating that you don't have the information, and avoid making up an answer.
                """)
			.defaultTools(customerTools)
			//.defaultAdvisors(qaAdvisor)
            .build();
	}

	@PostMapping(value = "/{user}/assistant", consumes = "text/plain")
    String assistant(@PathVariable String user, @RequestBody String query) {
		
		var advisor = PromptChatMemoryAdvisor
                .builder(MessageWindowChatMemory.builder().chatMemoryRepository(new InMemoryChatMemoryRepository()).build())
                .build();
        var advisorForUser = this.memory.computeIfAbsent(user, k -> advisor);

        return this.chatClient
                .prompt()
                .user(query)
                .advisors(advisorForUser)
                .call()
                .content();
	}

}

@Component
class CustomerTools {

	private final CustomerRepository repository;

	CustomerTools(CustomerRepository repository) {
		this.repository = repository;
	}

	@Tool(description = "List customers, optionally filtered by type. Leave type blank to list all customers.")
	List<Customer> listCustomers(String type) {
		if (type == null || type.isBlank()) {
			return repository.findAll();
		}
		return repository.findByType(type);
	}	

	@Tool(description = "List customer types")
	List<String> listCustomerTypes() {
		return repository.findAll().stream()
				.map(Customer::type)
				.distinct()
				.toList();
	}
}