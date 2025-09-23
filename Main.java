package AgendaContactos;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


/**
 * Clase principal del programa. Se encarga de crear la base de datos (si no existe)
 * y lanzar la ventana principal.
 */
public class Main {
    // URL de conexión a la base de datos SQLite
    private static final String URL = "jdbc:sqlite:src/TareaFinal1/contactos.db";
    // Ruta del archivo de base de datos
    private static final String DB = "src/TareaFinal1/contactos.db";
    // Ruta del archivo SQL con la estructura de la base de datos
    private static final String SQL_ARCHIVO = "src/TareaFinal1/contactos.sql";

    /**
     * Método principal que inicia la aplicación.
     * @param args Argumentos de línea de comandos (no se utilizan).
     */
    public static void main(String[] args) {
        crearBaseDeDatosSQLite();
        SwingUtilities.invokeLater(VentanaPrincipal::new);
    }

    /**
     * Crea la base de datos a partir del archivo SQL si no existe previamente.
     */
    private static void crearBaseDeDatosSQLite() {
        Path db = Paths.get(DB);

        if (Files.exists(db)) {
            System.out.println("La base de datos ya existe. No se creará nuevamente.");
        } else {
            try (Connection conn = DriverManager.getConnection(URL);
                 BufferedReader reader = new BufferedReader(new FileReader(SQL_ARCHIVO))) {

                StringBuilder sqlBuilder = new StringBuilder();
                String line;

                // Leer el archivo SQL línea por línea y construir la consulta
                while ((line = reader.readLine()) != null) {
                    sqlBuilder.append(line).append("\n");
                }

                // Ejecutar la consulta SQL para crear la estructura de la BD
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(sqlBuilder.toString());
                    System.out.println("Base de datos creada desde el fichero SQL.");
                }

            } catch (IOException | SQLException e) {
                System.out.println("Error al crear la base de datos: " + e.getMessage());
            }
        }
    }
}

