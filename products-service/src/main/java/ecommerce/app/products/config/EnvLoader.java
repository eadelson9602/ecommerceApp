package ecommerce.app.products.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Carga el archivo .env desde la raíz del proyecto (directorio actual o padres)
 * y define las variables como system properties para que Spring Boot las use.
 * Las variables ya definidas en el sistema no se sobrescriben.
 */
public final class EnvLoader {

	private static final int MAX_PARENT_LEVELS = 5;

	public static void load() {
		File dir = findEnvDirectory();
		if (dir == null) {
			return;
		}
		Dotenv dotenv = Dotenv.configure()
				.directory(dir.getAbsolutePath())
				.ignoreIfMissing()
				.load();
		for (DotenvEntry e : dotenv.entries()) {
			String key = e.getKey();
			if (key == null || key.isBlank()) {
				continue;
			}
			if (System.getProperty(key) == null) {
				System.setProperty(key, e.getValue() != null ? e.getValue() : "");
			}
		}
	}

	private static File findEnvDirectory() {
		Path current = Paths.get(System.getProperty("user.dir", ".")).normalize().toAbsolutePath();
		for (int i = 0; i <= MAX_PARENT_LEVELS; i++) {
			File envFile = current.resolve(".env").toFile();
			if (envFile.canRead()) {
				return current.toFile();
			}
			Path parent = current.getParent();
			if (parent == null) {
				break;
			}
			current = parent;
		}
		return null;
	}

	private EnvLoader() {}
}
