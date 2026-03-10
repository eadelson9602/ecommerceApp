package ecommerce.app.inventory;

import ecommerce.app.inventory.config.EnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class InventoryServiceApplication {

	public static void main(String[] args) {
		EnvLoader.load();
		SpringApplication.run(InventoryServiceApplication.class, args);
	}
}
