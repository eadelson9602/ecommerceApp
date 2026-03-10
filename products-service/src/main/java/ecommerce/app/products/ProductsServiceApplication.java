package ecommerce.app.products;

import ecommerce.app.products.config.EnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProductsServiceApplication {

	public static void main(String[] args) {
		EnvLoader.load();
		SpringApplication.run(ProductsServiceApplication.class, args);
	}
}
