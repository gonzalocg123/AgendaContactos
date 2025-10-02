import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.*;

/**
 * Ventana encargada de agregar o editar contactos en la base de datos.
 */
public class VentanaAgregarContacto {
    /** Panel principal de la ventana */
    private JPanel ventana;
    /** Campos de texto para los datos del contacto */
    private JTextField textFieldNombre;
    private JTextField textFieldTelefono;
    private JTextField textFieldCorreo;
    private JTextField textFieldWeb;
    /** Botones de la interfaz */
    private JButton seleccionarImagenButton;
    private JButton guardarButton;
    private JButton cancelarButton;
    /** Labels para los textos descriptivos */
    private JLabel nombreLabel;
    private JLabel telefonoLabel;
    private JLabel correoLabel;
    private JLabel webLabel;
    private JLabel tituloLabel;

    /** Ruta de la imagen seleccionada para el contacto */
    private String rutaImagen = "";
    private JFrame frame;

    /**
     * Constructor para agregar un nuevo contacto.
     * @param principal Instancia de VentanaPrincipal para recargar los datos tras guardar.
     */
    public VentanaAgregarContacto(VentanaPrincipal principal) {
        inicializarVentana("Añadir nuevo contacto");
        configurarSeleccionImagen();
        configurarBotonGuardar(principal);
        configurarBotonCancelar();
    }

    /**
     * Constructor para editar un contacto existente.
     * @param nombreOriginal Nombre original del contacto.
     * @param telefonoOriginal Teléfono original del contacto.
     * @param principal Instancia de VentanaPrincipal para recargar los datos tras actualizar.
     */
    public VentanaAgregarContacto(String nombreOriginal, String telefonoOriginal, VentanaPrincipal principal) {
        inicializarVentana("Editar contacto");
        cargarDatosContacto(nombreOriginal, telefonoOriginal);
        configurarSeleccionImagen();
        configurarBotonActualizar(nombreOriginal, telefonoOriginal, principal);
        configurarBotonCancelar();
    }

    /**
     * Inicializa la ventana con título y tamaño.
     */
    private void inicializarVentana(String titulo) {
        frame = new JFrame(titulo);
        frame.setContentPane(ventana);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * Configura el botón para seleccionar una imagen desde el sistema de archivos.
     */
    private void configurarSeleccionImagen() {
        seleccionarImagenButton.addActionListener((ActionEvent e) -> {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File archivo = chooser.getSelectedFile();

                // Reemplazar espacios en el nombre
                String nombreArchivo = archivo.getName().replaceAll("\\s+", "_");

                // Carpeta destino dentro del proyecto
                String rutaBase = System.getProperty("user.dir");
                File carpetaDestino = new File(rutaBase, "/imagenes");

                if (!carpetaDestino.exists()) {
                    carpetaDestino.mkdirs();
                }

                File destino = new File(carpetaDestino, nombreArchivo);

                try {
                    Files.copy(archivo.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    rutaImagen = destino.getAbsolutePath(); // ruta completa guardada
                    JOptionPane.showMessageDialog(null, "Imagen copiada correctamente en la carpeta 'imagenes'.");
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Error al copiar la imagen.");
                }
            }
        });
    }

    /**
     * Configura el botón de guardar para insertar un nuevo contacto en la BD.
     */
    private void configurarBotonGuardar(VentanaPrincipal principal) {
        guardarButton.addActionListener(e -> {
            String nombre = textFieldNombre.getText().trim();
            String telefono = textFieldTelefono.getText().trim();
            String correo = textFieldCorreo.getText().trim();
            String web = textFieldWeb.getText().trim();

            if (nombre.isEmpty() || telefono.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Nombre y Teléfono son obligatorios.");
                return;
            }

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:src/contactos.db")) {
                PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO CONTACTOS (Nombre, Telefono, WebPersonal, Correo, Imagen) VALUES (?, ?, ?, ?, ?)"
                );
                stmt.setString(1, nombre);
                stmt.setString(2, telefono);
                stmt.setString(3, web);
                stmt.setString(4, correo);
                stmt.setString(5, rutaImagen);
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(null, "Contacto guardado exitosamente.");
                principal.cargarContactos();
                frame.dispose();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Error al guardar el contacto:\n" + ex.getMessage());
            }
        });
    }

    /**
     * Configura el botón de guardar para actualizar un contacto existente.
     */
    private void configurarBotonActualizar(String nombreOriginal, String telefonoOriginal, VentanaPrincipal principal) {
        guardarButton.setText("Actualizar Contacto");
        guardarButton.addActionListener(e -> {
            String nombre = textFieldNombre.getText().trim();
            String telefono = textFieldTelefono.getText().trim();
            String correo = textFieldCorreo.getText().trim();
            String web = textFieldWeb.getText().trim();

            if (nombre.isEmpty() || telefono.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Nombre y Teléfono son obligatorios.");
                return;
            }

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:src/contactos.db")) {
                PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE CONTACTOS SET Nombre = ?, Telefono = ?, WebPersonal = ?, Correo = ?, Imagen = ? WHERE Nombre = ? AND Telefono = ?"
                );
                stmt.setString(1, nombre);
                stmt.setString(2, telefono);
                stmt.setString(3, web);
                stmt.setString(4, correo);
                stmt.setString(5, rutaImagen);
                stmt.setString(6, nombreOriginal);
                stmt.setString(7, telefonoOriginal);
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(null, "Contacto actualizado.");
                principal.cargarContactos();
                frame.dispose();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(null, "Error al actualizar contacto:\n" + ex.getMessage());
            }
        });
    }

    /**
     * Carga los datos de un contacto existente en los campos de texto para editarlo.
     */
    private void cargarDatosContacto(String nombreOriginal, String telefonoOriginal) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:src/contactos.db")) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM CONTACTOS WHERE Nombre = ? AND Telefono = ?"
            );
            stmt.setString(1, nombreOriginal);
            stmt.setString(2, telefonoOriginal);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                textFieldNombre.setText(rs.getString("Nombre"));
                textFieldTelefono.setText(rs.getString("Telefono"));
                textFieldCorreo.setText(rs.getString("Correo"));
                textFieldWeb.setText(rs.getString("WebPersonal"));
                rutaImagen = rs.getString("Imagen");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "Error al cargar contacto: " + ex.getMessage());
        }
    }

    /**
     * Configura el botón cancelar para cerrar la ventana.
     */
    private void configurarBotonCancelar() {
        cancelarButton.addActionListener(e -> frame.dispose());
    }
}

