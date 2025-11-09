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
import java.util.List;

public class UIPrincipal extends JFrame {
    private CatalogoCuentas catalogo;
    private JComboBox<CuentaContable> cmbCuenta;
    private JTextField txtMonto;
    private DefaultTableModel modeloTabla;

    private JTable tabla;
    
    public UIPrincipal() {
        catalogo = new CatalogoCuentas();
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
}

