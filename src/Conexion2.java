// Si tus otros archivos del proyecto tienen una línea "package ...;" hasta arriba,
// pon aquí la misma línea (en NetBeans se agrega sola si creas la clase con New > Java Class).

import java.awt.Image;
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class Conexion2 {

    Connection con;

    // Se conecta a la base de datos. Devuelve 1 si se pudo conectar y 0 si no.
    public int conectar(String servidor, String baseDatos, String usuario, String clave) {
        try {
            String direccion = "jdbc:mysql://" + servidor + ":3306/" + baseDatos + "?serverTimezone=UTC";
            con = DriverManager.getConnection(direccion, usuario, clave);
            return 1;
        } catch (SQLException ex) {
            System.out.println("Error al conectar: " + ex.getMessage());
            return 0;
        }
    }

    // Ejecuta un SELECT y muestra el resultado en una JTable
    public void entablar(String consulta, JTable tabla) {
        try {
            Statement sentencia = con.createStatement();
            ResultSet resultado = sentencia.executeQuery(consulta);
            ResultSetMetaData metadatos = resultado.getMetaData();
            int totalColumnas = metadatos.getColumnCount();

            // Modelo de la tabla que no permite editar las celdas
            DefaultTableModel modelo = new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int fila, int columna) {
                    return false;
                }
            };

            // Nombres de las columnas
            for (int columna = 1; columna <= totalColumnas; columna++) {
                modelo.addColumn(metadatos.getColumnLabel(columna));
            }

            // Filas
            while (resultado.next()) {
                Object[] fila = new Object[totalColumnas];
                for (int columna = 1; columna <= totalColumnas; columna++) {
                    fila[columna - 1] = resultado.getObject(columna);
                }
                modelo.addRow(fila);
            }

            tabla.setModel(modelo);
            resultado.close();
            sentencia.close();
        } catch (SQLException ex) {
            System.out.println("Error al llenar la tabla: " + ex.getMessage());
        }
    }

    // Cambia el tamaño de una imagen (se usa en la etiqueta de la foto)
    public static Image escalarImagen(Image imagen, int ancho, int alto) {
        return imagen.getScaledInstance(ancho, alto, Image.SCALE_SMOOTH);
    }

    // Llama a cualquier procedimiento almacenado.
    // Devuelve un arreglo: posición 0 = estado (1 o 0), posición 1 = mensaje
    public Object[] ejecutarCRUD(String nombreProcedimiento, Object[] valores) {

        // Se suman 2 porque el procedimiento tiene 2 parámetros de salida (estado y mensaje)
        int totalParametros = valores.length + 2;
        String marcadores = String.join(", ", Collections.nCopies(totalParametros, "?"));
        String sql = "{CALL " + nombreProcedimiento + "(" + marcadores + ")}";

        try {
            CallableStatement llamada = con.prepareCall(sql);

            // 1. Asignar los parámetros de entrada según su tipo en Java
            for (int i = 0; i < valores.length; i++) {
                int indice = i + 1;
                Object valor = valores[i];

                if (valor == null) {
                    llamada.setNull(indice, Types.NULL);
                } else if (valor instanceof byte[]) {
                    llamada.setBytes(indice, (byte[]) valor);
                } else if (valor instanceof InputStream) {
                    llamada.setBinaryStream(indice, (InputStream) valor);
                } else if (valor instanceof LocalDate) {
                    llamada.setDate(indice, java.sql.Date.valueOf((LocalDate) valor));
                } else if (valor instanceof LocalDateTime) {
                    llamada.setTimestamp(indice, Timestamp.valueOf((LocalDateTime) valor));
                } else {
                    llamada.setObject(indice, valor);
                }
            }

            // 2. Registrar los 2 parámetros de salida (los últimos)
            int posicionEstado = valores.length + 1;
            int posicionMensaje = valores.length + 2;
            llamada.registerOutParameter(posicionEstado, Types.INTEGER);
            llamada.registerOutParameter(posicionMensaje, Types.VARCHAR);

            // 3. Ejecutar y leer los resultados
            llamada.execute();
            Object[] resultado = new Object[2];
            resultado[0] = llamada.getInt(posicionEstado);
            resultado[1] = llamada.getString(posicionMensaje);

            llamada.close();
            return resultado;

        } catch (SQLException ex) {
            return new Object[]{0, "Error de conexión o del procedimiento: " + ex.getMessage()};
        }
    }

}
