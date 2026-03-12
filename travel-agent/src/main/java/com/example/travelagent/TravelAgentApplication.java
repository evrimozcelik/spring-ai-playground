package com.example.travelagent;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.ai.chat.client.ChatClient;
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
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class TravelAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(TravelAgentApplication.class, args);
	}

	@Bean
	@SuppressWarnings("null")
	ApplicationRunner seedDestinations(DestinationRepository destinationRepository, VectorStore vectorStore) {
		return args -> {
			if (destinationRepository.count() == 0) {
				destinationRepository.saveAll(List.of(
						new Destination(null, "Paris", "France", "The City of Light, famous for the Eiffel Tower, Louvre Museum, and romantic atmosphere.", "City"),
						new Destination(null, "Bali", "Indonesia", "Tropical paradise with beautiful beaches, temples, and rice terraces.", "Beach"),
						new Destination(null, "Tokyo", "Japan", "Modern metropolis blending tradition and technology, known for sushi, temples, and shopping.", "City"),
						new Destination(null, "Maldives", "Maldives", "Stunning overwater bungalows, crystal-clear waters, and world-class diving.", "Beach"),
						new Destination(null, "Swiss Alps", "Switzerland", "Majestic mountains perfect for skiing, hiking, and breathtaking scenery.", "Mountain"),
						new Destination(null, "New York", "USA", "The Big Apple with iconic landmarks like Times Square, Central Park, and the Statue of Liberty.", "City"),
						new Destination(null, "Santorini", "Greece", "White-washed buildings with blue domes overlooking the Aegean Sea.", "Island"),
						new Destination(null, "Amazon Rainforest", "Brazil", "The world's largest rainforest with incredible biodiversity and adventure.", "Adventure"),
						new Destination(null, "Dubai", "UAE", "Luxury shopping, ultramodern architecture, and desert safaris.", "City"),
						new Destination(null, "Iceland", "Iceland", "Land of fire and ice with geysers, waterfalls, and the Northern Lights.", "Nature")
				));
			}

			List<Destination> destinations = new ArrayList<>();
			destinationRepository.findAll().forEach(destinations::add);

			List<Document> documents = destinations.stream()
					.map(destination -> new Document(
							"destination-" + destination.id(),
							destination.name() + " in " + destination.country() + " is a " + destination.category() + " destination. " + destination.description(),
							Map.of(
									"destination-id", destination.id(),
									"destination-name", destination.name(),
									"destination-country", destination.country(),
									"destination-category", destination.category()
							)
					))
					.toList();

			vectorStore.add(documents);
		};
	}

	@Bean
	@SuppressWarnings("null")
	VectorStore vectorStore(EmbeddingModel embeddingModel) {
		return SimpleVectorStore.builder(embeddingModel).build();
	}

}

interface DestinationRepository extends ListCrudRepository<Destination, Integer> {
	List<Destination> findByCategory(String category);
	List<Destination> findByCountry(String country);
}

record Destination(@Id Integer id, String name, String country, String description, String category) {
}

@RestController
class TravelAgentController {

	private final ChatClient chatClient;

	private final Map<String, PromptChatMemoryAdvisor> memory = new ConcurrentHashMap<>();

    private final TravelCoordinator travelCoordinator;

	TravelAgentController(ChatClient.Builder builder, TravelTools travelTools, VectorStore vectorStore, TravelCoordinator travelCoordinator) {
        this.travelCoordinator = travelCoordinator;

		this.chatClient = builder
				.defaultSystem("""
						You are a Travel Agent AI Assistant. 
						You have access to a database of travel destinations and can help users plan their trips.
						Use the tools at your disposal to find relevant destinations based on user preferences.
						Provide helpful recommendations and travel advice.
						If you don't have specific information, provide general travel guidance without making up facts.
						""")
				.defaultTools(travelTools)
				.build();
	}

	@PostMapping(value = "/{user}/travel", consumes = "text/plain")
	@SuppressWarnings("null")
	String travel(@PathVariable String user, @RequestBody String query) {

         /*
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
                 */

        return travelCoordinator.handleRequestLLM(query);
	}

}


@Service
class TravelCoordinator {

    private final SearchAgent searchAgent;
    private final BookingAgent bookingAgent;
	private final AgentRouterLLM agentRouterLLM;

    public TravelCoordinator(SearchAgent searchAgent, BookingAgent bookingAgent, AgentRouterLLM agentRouterLLM) {
        this.searchAgent = searchAgent;
        this.bookingAgent = bookingAgent;
        this.agentRouterLLM = agentRouterLLM;
    }

    public String handleRequest(String request) {

        if(request.contains("search") || request.contains("find")) {
            return searchAgent.search(request);
        }

        if(request.contains("book") || request.contains("reserve")) {
            return bookingAgent.book(request);
        }

        return searchAgent.search(request);
    }

	public String handleRequestLLM(String request) {
		
		// Use LLM to route the request
		AgentRoute route = agentRouterLLM.route(request);

		switch (route.agent()) {
			case "search":
				return searchAgent.search(request);
			case "booking":
				return bookingAgent.book(request);
			default:
				return "Sorry, I couldn't determine how to handle your request.";
		}
	}
}

record AgentRoute(
    String agent,
    String reason
) {}

@Service
class AgentRouterLLM {

    private final ChatClient chatClient;

    public AgentRouterLLM(ChatClient.Builder builder) {

        this.chatClient = builder
            .defaultSystem("""
                You are a routing agent for a travel system.

                Decide which agent should handle the request.

                Available agents:
                - search : searching flights or hotels
                - booking : booking reservations

                Only return JSON.
            """)
            .build();
    }

    public AgentRoute route(String userRequest) {

        return chatClient.prompt()
                .user(userRequest)
                .call()
                .entity(AgentRoute.class);
    }
}


@Service
class SearchAgent {

    private final ChatClient chatClient;

    public SearchAgent(ChatClient.Builder builder, TravelTools tools) {
        this.chatClient = builder
                .defaultSystem("""
                    You are a travel search assistant.
                    Your job is to find flights and hotels.
					You have access to the travel destinations database and can answer questions.
					First use the tools to find the relevant information.
                    Do not perform bookings.
                """)
                .defaultTools(tools)
                .build();
    }

    public String search(String request) {
        return chatClient.prompt()
                .user(request)
                .call()
                .content();
    }
}


@Service
class BookingAgent {

    private final ChatClient chatClient;

    public BookingAgent(ChatClient.Builder builder, TravelTools tools) {
        this.chatClient = builder
                .defaultSystem("""
                    You are a travel booking assistant.
                    Your job is to finalize reservations.
					You have access to the travel destinations database and can answer questions.
					First use the tools to find the relevant information.
                """)
                .defaultTools(tools)
                .build();
    }

    public String book(String request) {
        return chatClient.prompt()
                .user(request)
                .call()
                .content();
    }
}



@Component
class TravelTools {

	private final DestinationRepository destinationRepository;

	TravelTools(DestinationRepository destinationRepository) {
		this.destinationRepository = destinationRepository;
	}

	@Tool(description = "Find flight and travel destinations by category (e.g., City, Beach, Mountain, Island, Adventure, Nature)")
	public List<Destination> findDestinationsByCategory(String category) {
		return destinationRepository.findByCategory(category);
	}

	@Tool(description = "Find flight and travel destinations by country name")
	public List<Destination> findDestinationsByCountry(String country) {
		return destinationRepository.findByCountry(country);
	}

	@Tool(description = "Get all available flight and travel destinations")
	public List<Destination> getAllDestinations() {
		List<Destination> destinations = new ArrayList<>();
		destinationRepository.findAll().forEach(destinations::add);
		return destinations;
	}
}
