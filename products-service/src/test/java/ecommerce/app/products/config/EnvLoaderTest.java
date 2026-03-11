package ecommerce.app.products.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests para EnvLoader (paquete config, cobertura 0% -> cubierto).
 */
class EnvLoaderTest {

	private static final String TEST_PROP = "ENV_LOADER_TEST_PROPERTY_COVERAGE";

	@AfterEach
	void tearDown() {
		System.clearProperty(TEST_PROP);
	}

	@Test
	void load_doesNotThrow() {
		assertThatCode(EnvLoader::load).doesNotThrowAnyException();
	}

	@Test
	void load_doesNotOverwriteExistingSystemProperty() {
		System.setProperty(TEST_PROP, "existing-value");
		EnvLoader.load();
		assertEquals("existing-value", System.getProperty(TEST_PROP));
	}

	@Test
	void load_withTempDirContainingEnv_setsPropertyFromEnv() throws Exception {
		Path tempDir = Files.createTempDirectory("envloader-test");
		Path envFile = tempDir.resolve(".env");
		Files.writeString(envFile, TEST_PROP + "=from-env-file\n");
		String originalUserDir = System.getProperty("user.dir");
		try {
			System.setProperty("user.dir", tempDir.toAbsolutePath().toString());
			EnvLoader.load();
			assertEquals("from-env-file", System.getProperty(TEST_PROP));
		} finally {
			System.setProperty("user.dir", originalUserDir);
			Files.deleteIfExists(envFile);
			Files.deleteIfExists(tempDir);
			System.clearProperty(TEST_PROP);
		}
	}
}
