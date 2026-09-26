package com.urbancompany.clone.config;

import com.urbancompany.clone.model.Service;
import com.urbancompany.clone.model.ServiceProvider;
import com.urbancompany.clone.model.User;
import com.urbancompany.clone.model.Location;
import com.urbancompany.clone.repository.ServiceRepository;
import com.urbancompany.clone.repository.ServiceProviderRepository;
import com.urbancompany.clone.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    @Profile("dev")
    public CommandLineRunner loadData(
            ServiceRepository serviceRepository,
            ServiceProviderRepository providerRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            if (serviceRepository.count() > 0) return;  // already seeded

            List<Service> services = Arrays.asList(
                    new Service(null, "Carpenter", "Furniture repair, installation, and custom woodwork", 499.0, null, "https://images.unsplash.com/photo-1558618666-fcd25c85f82e?w=600&h=400&fit=crop", true),
                    new Service(null, "Electrician", "Electrical wiring, switch/socket repair, light fixture installation", 399.0, null, "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?w=600&h=400&fit=crop", true),
                    new Service(null, "Plumber", "Pipe repair, leak fixing, bathroom fitting, and water line services", 349.0, null, "https://images.unsplash.com/photo-1585704032915-c3400ca199e7?w=600&h=400&fit=crop", true),
                    new Service(null, "Massage Therapist", "Professional massage therapy for relaxation and wellness", 799.0, null, "https://images.unsplash.com/photo-1544161515-4ab6ce6db874?w=600&h=400&fit=crop", true),
                    new Service(null, "House Cleaning", "Deep cleaning, vacuuming, dusting, and mopping services", 499.0, null, "https://images.unsplash.com/photo-1581578731548-c64695cc6952?w=600&h=400&fit=crop", true),
                    new Service(null, "AC Repair", "Air conditioner servicing, installation, and repair", 699.0, null, "https://images.unsplash.com/photo-1631545806609-3c480b4c2986?w=600&h=400&fit=crop", true),
                    new Service(null, "Appliance Repair", "Washing machine, refrigerator, microwave oven repair", 599.0, null, "https://images.unsplash.com/photo-1590794056226-79ef3a8147e1?w=600&h=400&fit=crop", true),
                    new Service(null, "Painter", "Interior and exterior painting, wall texture, and finishing", 449.0, null, "https://images.unsplash.com/photo-1589939705384-5185137a7f0f?w=600&h=400&fit=crop", true)
            );
            serviceRepository.saveAll(services);

            List<User> users = Arrays.asList(
                    new User(null, "Rajesh Kumar", "rajesh@example.com", "9876543210", passwordEncoder.encode("password"), "CUSTOMER", new Location(28.6139, 77.2090, "New Delhi")),
                    new User(null, "Priya Sharma", "priya@example.com", "9876543211", passwordEncoder.encode("password"), "CUSTOMER", new Location(28.6140, 77.2091, "New Delhi"))
            );
            userRepository.saveAll(users);

            String providerPass = passwordEncoder.encode("provider123");
            List<ServiceProvider> providers = Arrays.asList(
                    new ServiceProvider(null, "Amit Verma", "amit@example.com", "9876501234", providerPass, "PROVIDER",
                            new Location(28.6150, 77.2080, "Connaught Place, Delhi"), 4.8, 120, true, true,
                            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&h=200&fit=crop&crop=face", Arrays.asList(1L, 8L),
                            Arrays.asList("Certified Carpenter", "Furniture Making"), 10,
                            "Experienced carpenter with 10+ years in furniture making and repair."),

                    new ServiceProvider(null, "Sneha Gupta", "sneha@example.com", "9876501235", providerPass, "PROVIDER",
                            new Location(28.6145, 77.2095, "Karol Bagh, Delhi"), 4.7, 95, true, true,
                            "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&h=200&fit=crop&crop=face", Arrays.asList(2L),
                            Arrays.asList("Licensed Electrician", "LED Specialist"), 7,
                            "Licensed electrician specializing in LED lighting and residential wiring."),

                    new ServiceProvider(null, "Rameshwar Das", "rameshwar@example.com", "9876501236", providerPass, "PROVIDER",
                            new Location(28.6100, 77.2100, "Lajpat Nagar, Delhi"), 4.6, 80, true, true,
                            "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200&h=200&fit=crop&crop=face", Arrays.asList(3L),
                            Arrays.asList("Certified Plumber"), 8,
                            "Plumber with expertise in bathroom fitting and water line repair."),

                    new ServiceProvider(null, "Pooja Srivastava", "pooja@example.com", "9876501237", providerPass, "PROVIDER",
                            new Location(28.6200, 77.2050, "Defence Colony, Delhi"), 4.9, 150, true, true,
                            "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=200&h=200&fit=crop&crop=face", Arrays.asList(4L),
                            Arrays.asList("Certified Massage Therapist", "Yoga Instructor"), 6,
                            "Certified massage therapist offering therapeutic and relaxation massages."),

                    new ServiceProvider(null, "Manoj Tiwari", "manoj@example.com", "9876501238", providerPass, "PROVIDER",
                            new Location(28.6050, 77.2150, "Saket, Delhi"), 4.3, 65, true, false,
                            "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=200&h=200&fit=crop&crop=face", Arrays.asList(5L),
                            Arrays.asList("Professional Cleaner"), 5,
                            "Professional cleaner with experience in residential and commercial cleaning."),

                    new ServiceProvider(null, "Deepak Chauhan", "deepak@example.com", "9876501239", providerPass, "PROVIDER",
                            new Location(28.6000, 77.2100, "Mehrauli, Delhi"), 4.5, 70, true, true,
                            "https://images.unsplash.com/photo-1560250097-0b93528c311a?w=200&h=200&fit=crop&crop=face", Arrays.asList(6L),
                            Arrays.asList("AC Certified Technician"), 9,
                            "AC technician certified with 9 years of experience in all major brands."),

                    new ServiceProvider(null, "Kavita Mishra", "kavita@example.com", "9876501240", providerPass, "PROVIDER",
                            new Location(28.6180, 77.2050, "Hauz Khas, Delhi"), 4.2, 55, true, true,
                            "https://images.unsplash.com/photo-1580489944761-15a19d654956?w=200&h=200&fit=crop&crop=face", Arrays.asList(7L),
                            Arrays.asList("Appliance Repair Specialist"), 6,
                            "Specialist in appliance repair with quick turnaround time."),

                    new ServiceProvider(null, "Anil Kumar", "anil@example.com", "9876501241", providerPass, "PROVIDER",
                            new Location(28.6070, 77.2200, "Greater Kailash, Delhi"), 4.4, 90, true, true,
                            "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=200&h=200&fit=crop&crop=face", Arrays.asList(8L),
                            Arrays.asList("Licensed Painter", "Wall Texture Expert"), 12,
                            "Licensed painter with 12 years of experience in interior and exterior painting.")
            );
            providerRepository.saveAll(providers);
        };
    }
}
