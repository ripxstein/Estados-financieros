/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.estadosfinancieros;

/**
 *
 * @author ripxs
 */
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class UIPrincipal extends JFrame {
    private CatalogoCuentas catalogo;
    private JComboBox<CuentaContable> cmbCuenta;
    private JTextField txtMonto;
    private DefaultTableModel modeloTabla;

    private JTable tabla;
    
    public UIPrincipal() {
        catalogo = new CatalogoCuentas();
        //catalogo.setCuentas(this.cargarCuentasPrueba());
        initUI();
    }

    private void initUI() {
        setTitle("Sistema Contable - Estados Financieros");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // PANEL SUPERIOR
        JPanel pnlEntrada = new JPanel(new GridLayout(2, 3, 5, 5));
        pnlEntrada.setBorder(BorderFactory.createTitledBorder("Seleccionar Cuenta Contable"));

        cmbCuenta = new JComboBox<>(catalogo.getCuentas().toArray(new CuentaContable[0]));
        txtMonto = new JTextField();
        JButton btnAgregar = new JButton("Agregar al reporte");

        pnlEntrada.add(new JLabel("Cuenta contable:"));
        pnlEntrada.add(cmbCuenta);
        //pnlEntrada.add(new JLabel("Monto:"));
        pnlEntrada.add(txtMonto);
        pnlEntrada.add(new JLabel(""));
        pnlEntrada.add(btnAgregar);

        // TABLA
        modeloTabla = new DefaultTableModel(new Object[]{"Código", "Nombre", "Tipo", "Monto"}, 0);
        tabla = new JTable(modeloTabla);
        JScrollPane scrollTabla = new JScrollPane(tabla);
        
         // Permitir eliminar filas con tecla Supr
        tabla.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    eliminarFilasSeleccionadas();
                }
            }
        });

        // BOTONES
        JPanel pnlBotones = new JPanel();
        JButton btnBalance = new JButton("Balance General");
        JButton btnResultados = new JButton("Estado de Resultados");
        pnlBotones.add(btnBalance);
        pnlBotones.add(btnResultados);

        // EVENTOS
        btnAgregar.addActionListener((ActionEvent e) -> agregarOActualizarCuenta());
        btnBalance.addActionListener(e -> generarReporte("Balance General"));
        btnResultados.addActionListener(e -> generarReporte("Estado de Resultados"));

        add(pnlEntrada, BorderLayout.NORTH);
        add(scrollTabla, BorderLayout.CENTER);
        add(pnlBotones, BorderLayout.SOUTH);
    }

      /**
     * Agrega una cuenta a la tabla, pero si ya existe el mismo código, actualiza su monto.
     */
    private void agregarOActualizarCuenta() {
        try {
            CuentaContable seleccionada = (CuentaContable) cmbCuenta.getSelectedItem();
            double monto = Double.parseDouble(txtMonto.getText());

            if (monto < 0) {
                JOptionPane.showMessageDialog(this, "El monto no puede ser negativo.");
                return;
            }

            seleccionada.setSaldo(monto);

            // Verificar si la cuenta ya existe en la tabla
            int filaExistente = buscarFilaPorCodigo(seleccionada.getCodigo());
            if (filaExistente != -1) {
                // Si existe, actualiza el monto
                modeloTabla.setValueAt(monto, filaExistente, 3);
                JOptionPane.showMessageDialog(this, "Monto actualizado para la cuenta " + seleccionada.getCodigo());
            } else {
                // Si no existe, agregar nueva fila
                modeloTabla.addRow(new Object[]{
                        seleccionada.getCodigo(),
                        seleccionada.getNombre(),
                        seleccionada.getTipo(),
                        monto
                });
            }

            txtMonto.setText("");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Monto inválido. Debe ser un número.");
        }
    }

    /**
     * Busca una fila en la tabla según el código de cuenta.
     * @return índice de la fila o -1 si no existe
     */
    private int buscarFilaPorCodigo(String codigo) {
        for (int i = 0; i < modeloTabla.getRowCount(); i++) {
            String codigoTabla = modeloTabla.getValueAt(i, 0).toString();
            if (codigoTabla.equalsIgnoreCase(codigo)) {
                return i;
            }
        }
        return -1;
    }

    private void generarReporte(String tipoEstado) {
        String empresa = JOptionPane.showInputDialog(this, "Ingrese el nombre de la empresa:");
        if (empresa == null || empresa.isBlank()) return;

        String[] opciones = {"Cuenta", "Reporte"};
        String tipoFormato = (String) JOptionPane.showInputDialog(this,
                "Selecciona el formato:", "Formato de salida",
                JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
        if (tipoFormato == null) return;

        String periodo;
        if (tipoEstado.equals("Balance General")) {
            periodo = JOptionPane.showInputDialog(this, "Ingrese la fecha del balance (ej. 31/12/2025):");
        } else {
            periodo = JOptionPane.showInputDialog(this, "Ingrese el periodo (ej. 01/01/2025 - 31/12/2025):");
        }

        List<CuentaContable> cuentasFiltradas = catalogo.getCuentas().stream()
                .filter(c -> c.getSaldo() > 0 && (
                        tipoEstado.equals("Balance General")
                                ? c.getTipo().matches("(?i)Activo|Pasivo|Capital")
                                : c.getTipo().matches("(?i)Ingreso|Gasto")))
                .toList();

        if (cuentasFiltradas.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay cuentas con montos registrados.");
            return;
        }

        String nombreArchivo = tipoEstado.replace(" ", "_") + ".pdf";
        GeneradorPDF.generarPDF(empresa, tipoEstado, periodo, cuentasFiltradas, tipoFormato, nombreArchivo);
        JOptionPane.showMessageDialog(this, "PDF generado: " + nombreArchivo);
        try {
    java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
    java.io.File archivoPDF = new java.io.File(nombreArchivo);
    if (archivoPDF.exists()) {
        desktop.open(archivoPDF);
    } else {
        JOptionPane.showMessageDialog(this, "No se encontró el archivo PDF para abrirlo.");
    }
} catch (Exception ex) {
    JOptionPane.showMessageDialog(this, "No se pudo abrir el PDF: " + ex.getMessage());
    ex.printStackTrace();
}
    }
    
    private void eliminarFilasSeleccionadas() {
        int[] filas = tabla.getSelectedRows();
        if (filas.length == 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una o más filas para eliminar.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Deseas eliminar las filas seleccionadas?",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            // Eliminar del modelo visual y también del catálogo
            for (int i = filas.length - 1; i >= 0; i--) {
                String codigo = modeloTabla.getValueAt(filas[i], 0).toString();
                modeloTabla.removeRow(filas[i]);

                // Buscar en el catálogo y poner saldo en 0
                catalogo.getCuentas().stream()
                        .filter(c -> c.getCodigo().equals(codigo))
                        .findFirst()
                        .ifPresent(c -> c.setSaldo(0));
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UIPrincipal().setVisible(true));
    }
    
    public static List<CuentaContable> cargarCuentasPrueba() {

    List<CuentaContable> cuentas = new ArrayList<>();

    cuentas.add(new CuentaContable("4002", "Ventas Totales", "Ingreso", "Ventas", 1950000));
    cuentas.add(new CuentaContable("4101", "Devoluciones sobre ventas", "Gasto", "Ventas – Deducciones", 30000));
    cuentas.add(new CuentaContable("4102", "Descuentos sobre ventas", "Gasto", "Ventas – Deducciones", 20000));

    cuentas.add(new CuentaContable("5001", "Inventario inicial", "Gasto", "Costo de ventas", 1250000));
    cuentas.add(new CuentaContable("5003", "Compras", "Gasto", "Costo de ventas", 800000));
    cuentas.add(new CuentaContable("5102", "Gastos de compra", "Gasto", "Costo de ventas", 20000));
    cuentas.add(new CuentaContable("5004", "Devoluciones sobre compras", "Gasto", "Costo de ventas", 60000));
    cuentas.add(new CuentaContable("5101", "Descuentos sobre compras", "Gasto", "Costo de ventas", 10000));
    cuentas.add(new CuentaContable("5002", "Inventario final", "Gasto", "Costo de ventas", 600000));

    cuentas.add(new CuentaContable("6001", "Renta del almacén", "Gasto", "Gastos de venta", 17000));
    cuentas.add(new CuentaContable("6003", "Propaganda y publicidad", "Gasto", "Gastos de venta", 9000));
    cuentas.add(new CuentaContable("6011", "Sueldos de agentes y dependientes", "Gasto", "Gastos de venta", 32000));
    cuentas.add(new CuentaContable("6004", "Comisiones de agentes", "Gasto", "Gastos de venta", 16000));
    cuentas.add(new CuentaContable("8005", "Consumo de luz de almacén", "Gasto", "Gastos de venta", 1000));

    cuentas.add(new CuentaContable("6006", "Renta de oficinas", "Gasto", "Gastos de administración", 12000));
    cuentas.add(new CuentaContable("6007", "Sueldos del personal de oficinas", "Gasto", "Gastos de administración", 43000));
    cuentas.add(new CuentaContable("6008", "Papelería y útiles", "Gasto", "Gastos de administración", 3000));
    cuentas.add(new CuentaContable("6009", "Consumo de luz de oficinas", "Gasto", "Gastos de administración", 2000));

    cuentas.add(new CuentaContable("7005", "Intereses cobrados", "Ingreso", "Productos financieros", 7000));

    cuentas.add(new CuentaContable("8006", "Pérdida", "Gasto", "Gastos financieros", 5000));
    cuentas.add(new CuentaContable("8007", "Pérdida en venta de acciones", "Gasto", "Otros gastos", 6000));
    cuentas.add(new CuentaContable("8003", "Pérdida en venta de mobiliario", "Gasto", "Otros gastos", 20000));

    cuentas.add(new CuentaContable("8004", "Comisiones cobradas", "Ingreso", "Otros productos", 2000));
    cuentas.add(new CuentaContable("8008", "Dividendos cobrados", "Ingreso", "Otros productos", 4000));

    cuentas.add(new CuentaContable("7004", "Intereses pagados", "Gasto", "Gastos financieros", 5000));
    cuentas.add(new CuentaContable("7002", "Pérdida en cambios", "Gasto", "Productos financieros", 5000));

    return cuentas;
}
    
    
    
}

