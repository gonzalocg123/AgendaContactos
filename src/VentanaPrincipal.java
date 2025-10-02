import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.List;

/**
 * Ventana principal de la aplicación.
 * Muestra la lista de contactos y permite agregarlos, editarlos, eliminarlos, exportarlos e importarlos.
 */
public class VentanaPrincipal {
    /** Tabla para mostrar los contactos */
    public JTable table1;
    /** Etiquetas para mostrar detalles del contacto seleccionado */
    private JLabel imagenLabel;
    private JLabel nombreLabel;
    private JLabel telefonoLabel;
    private JLabel correoLabel;
    private JLabel webLabel;
    /** Botones de interacción */
    private JButton agregarContactoButton;
    private JPanel ventana;
    private JButton actualizarTablaButton;
    private JButton importarContactosButton;
    private JButton exportarContactosButton;
    private JButton editarContactoButton;
    private JButton eliminarContactoButton;

    /**
     * Constructor que inicializa la ventana principal.
     */
    public VentanaPrincipal() {
        JFrame frame = new JFrame("Agenda de Contactos");
        frame.setContentPane(ventana);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 450);
        frame.setLocationRelativeTo(null);

        // Configurar acciones de los botones
        agregarContactoButton.addActionListener(e -> new VentanaAgregarContacto(this));
        actualizarTablaButton.addActionListener(e -> cargarContactos());
        importarContactosButton.addActionListener(e -> importarContactosDesdeJSON());
        exportarContactosButton.addActionListener(e -> exportarContactosAJSON());
        editarContactoButton.addActionListener(e -> editarContactoSeleccionado());
        eliminarContactoButton.addActionListener(e -> eliminarContactoSeleccionado());

        cargarContactos();

        // Cargar detalles cuando se selecciona una fila en la tabla
        table1.getSelectionModel().addListSelectionListener((ListSelectionEvent e) -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = table1.getSelectedRow();
                if (selectedRow != -1) {
                    String nombre = table1.getValueAt(selectedRow, 0).toString();
                    String telefono = table1.getValueAt(selectedRow, 1).toString();
                    cargarDetallesContacto(nombre, telefono);
                }
            }
        });

        frame.setVisible(true);
    }

    /**
     * Carga los contactos de la base de datos y los muestra en la tabla.
     */
    protected void cargarContactos() {
        DefaultTableModel model = new DefaultTableModel(new String[]{"Nombre", "Teléfono", "Correo"}, 0);
        model.addRow(new Object[]{"Nombre", "Telefono", "Correo"}); // Fila de encabezado

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:src/contactos.db")) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT Nombre, Telefono, Correo FROM CONTACTOS");
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("Nombre"),
                        rs.getString("Telefono"),
                        rs.getString("Correo")
                });
            }
            table1.setModel(model);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error cargando contactos: " + e.getMessage());
        }
    }

    /**
     * Importa contactos desde un archivo JSON y los agrega a la base de datos si no existen.
     */
    private void importarContactosDesdeJSON() {
        JFileChooser fileChooser = new JFileChooser();
        int seleccion = fileChooser.showOpenDialog(null);

        if (seleccion == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            try (Reader reader = new FileReader(file)) {
                Gson gson = new Gson();
                Type listType = new TypeToken<List<Map<String, String>>>() {}.getType();
                List<Map<String, String>> contactos = gson.fromJson(reader, listType);

                try (Connection conn = DriverManager.getConnection("jdbc:sqlite:src/contactos.db")) {
                    for (Map<String, String> contacto : contactos) {
                        String nombre = contacto.getOrDefault("Nombre", "");
                        String telefono = contacto.getOrDefault("Telefono", "");

                        // Evita insertar duplicados
                        PreparedStatement check = conn.prepareStatement(
                                "SELECT COUNT(*) FROM CONTACTOS WHERE Nombre = ? AND Telefono = ?"
                        );
                        check.setString(1, nombre);
                        check.setString(2, telefono);
                        ResultSet rs = check.executeQuery();
                        if (rs.next() && rs.getInt(1) == 0) {
                            PreparedStatement stmt = conn.prepareStatement(
                                    "INSERT INTO CONTACTOS (Nombre, Telefono, Correo, WebPersonal, Imagen) VALUES (?, ?, ?, ?, ?)"
                            );
                            stmt.setString(1, nombre);
                            stmt.setString(2, telefono);
                            stmt.setString(3, contacto.getOrDefault("Correo", ""));
                            stmt.setString(4, contacto.getOrDefault("WebPersonal", ""));
                            stmt.setString(5, contacto.getOrDefault("Imagen", ""));
                            stmt.executeUpdate();
                        }
                    }
                    JOptionPane.showMessageDialog(null, "Importación completada. Solo se añadieron contactos nuevos.");
                    cargarContactos();
                }
                catch (SQLException ex) {
                    JOptionPane.showMessageDialog(null, "Error al guardar contactos: " + ex.getMessage());
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Error leyendo el archivo: " + ex.getMessage());
            }
        }
    }

    /**
     * Exporta todos los contactos de la base de datos a un archivo JSON.
     */
    private void exportarContactosAJSON() {
        JFileChooser fileChooser = new JFileChooser();
        int seleccion = fileChooser.showSaveDialog(null);

        if (seleccion == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:src/contactos.db")) {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM CONTACTOS");

                List<Map<String, String>> contactos = new ArrayList<>();
                while (rs.next()) {
                    Map<String, String> contacto = new LinkedHashMap<>();
                    contacto.put("Nombre", rs.getString("Nombre"));
                    contacto.put("Telefono", rs.getString("Telefono"));
                    contacto.put("Correo", rs.getString("Correo"));
                    contacto.put("WebPersonal", rs.getString("WebPersonal"));
                    contacto.put("Imagen", rs.getString("Imagen"));
                    contactos.add(contacto);
                }

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                try (Writer writer = new FileWriter(file)) {
                    gson.toJson(contactos, writer);
                }

                JOptionPane.showMessageDialog(null, "Contactos exportados correctamente.");

            } catch (SQLException | IOException ex) {
                JOptionPane.showMessageDialog(null, "Error exportando contactos: " + ex.getMessage());
            }
        }
    }

    /**
     * Carga los detalles de un contacto seleccionado en la interfaz.
     */
    private void cargarDetallesContacto(String nombre, String telefono) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:src/contactos.db")) {
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM CONTACTOS WHERE Nombre = ? AND Telefono = ?"
            );
            stmt.setString(1, nombre);
            stmt.setString(2, telefono);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                nombreLabel.setText("Nombre: " + rs.getString("Nombre"));
                telefonoLabel.setText("Teléfono: " + rs.getString("Telefono"));
                correoLabel.setText("Correo: " + rs.getString("Correo"));
                webLabel.setText("Web personal: " + rs.getString("WebPersonal"));

                String rutaImagen = rs.getString("Imagen");
                if (rutaImagen != null && !rutaImagen.isEmpty()) {
                    ImageIcon icon = new ImageIcon(rutaImagen);
                    imagenLabel.setIcon(new ImageIcon(icon.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH)));
                } else {
                    imagenLabel.setIcon(null);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error mostrando detalles: " + e.getMessage());
        }
    }

    /**
     * Abre la ventana de edición para el contacto seleccionado.
     */
    private void editarContactoSeleccionado() {
        int selectedRow = table1.getSelectedRow();
        if (selectedRow != -1 && selectedRow != 0) {
            String nombre = table1.getValueAt(selectedRow, 0).toString();
            String telefono = table1.getValueAt(selectedRow, 1).toString();
            new VentanaAgregarContacto(nombre, telefono, this);
        } else {
            JOptionPane.showMessageDialog(null, "Selecciona un contacto para editar.");
        }
    }

    /**
     * Elimina el contacto seleccionado de la base de datos.
     */
    private void eliminarContactoSeleccionado() {
        int selectedRow = table1.getSelectedRow();
        if (selectedRow != -1 && selectedRow != 0) {
            String nombre = table1.getValueAt(selectedRow, 0).toString();
            String telefono = table1.getValueAt(selectedRow, 1).toString();

            int confirm = JOptionPane.showConfirmDialog(null, "¿Estás seguro de eliminar este contacto?", "Confirmar eliminación", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try (Connection conn = DriverManager.getConnection("jdbc:sqlite:src/contactos.db")) {
                    PreparedStatement stmt = conn.prepareStatement("DELETE FROM CONTACTOS WHERE Nombre = ? AND Telefono = ?");
                    stmt.setString(1, nombre);
                    stmt.setString(2, telefono);
                    stmt.executeUpdate();
                    JOptionPane.showMessageDialog(null, "Contacto eliminado.");
                    cargarContactos();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(null, "Error eliminando contacto: " + ex.getMessage());
                }
            }
        } else {
            JOptionPane.showMessageDialog(null, "Selecciona un contacto válido.");
        }
    }
}
