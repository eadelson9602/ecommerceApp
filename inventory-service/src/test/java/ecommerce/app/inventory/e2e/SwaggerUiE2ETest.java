package ecommerce.app.inventory.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas E2E con Selenium + HtmlUnit: Swagger UI y actuator (Inventory Service).
 * No requiere navegador instalado (HtmlUnit es un driver en JVM).
 */
@Tag("e2e")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SwaggerUiE2ETest {

	@LocalServerPort
	private int port;

	private WebDriver driver;

	@AfterEach
	void tearDown() {
		if (driver != null) {
			driver.quit();
		}
	}

	private WebDriver createDriver(boolean enableJs) {
		HtmlUnitDriver unit = new HtmlUnitDriver(enableJs);
		unit.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
		driver = unit;
		return driver;
	}

	@Test
	void swaggerUi_loadsAndShowsInventoryApi() {
		WebDriver d = createDriver(false);
		d.get("http://localhost:" + port + "/swagger-ui.html");
		new WebDriverWait(d, Duration.ofSeconds(5))
				.until(webDriver -> webDriver.findElement(By.tagName("body")).isDisplayed());
		assertThat(d.getTitle()).contains("Swagger");
		assertThat(d.getPageSource()).containsIgnoringCase("swagger");
	}

	@Test
	void actuatorHealth_returnsUp() {
		WebDriver d = createDriver(false);
		d.get("http://localhost:" + port + "/actuator/health");
		assertThat(d.getPageSource()).contains("UP");
	}
}
