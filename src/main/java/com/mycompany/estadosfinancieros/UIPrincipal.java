package com.mycompany.estadosfinancieros;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UIPrincipal extends JFrame {
    private CatalogoCuentas catalogo;
    
    // SELECTORES EN CASCADA
    private JComboBox<String> cmbTipoReporte; // Balance vs Resultados
    private JComboBox<String> cmbCategoria;   // Activo, Pasivo, Capital
    private JComboBox<String> cmbGrupo;       // Circulante, Fijo, Diferido...
    private JComboBox<CuentaContable> cmbCuenta; // La cuenta final (Caja, etc.)
    
    private JTextField txtMonto;
    private DefaultTableModel modeloTabla;
    private JTable tabla;
    private JLabel lblStatus;
    
    // BANDERA PARA EVITAR EVENTOS MIENTRAS LLENAMOS COMBOS
    private boolean isUpdating = false;

    public UIPrincipal() {
        catalogo = new CatalogoCuentas();
        //catalogo.setCuentas(this.cargarCuentasPrueba());
        initUI();
        verificarCatalogo();
        inicializarLogicaCascada();
    }

    private void initUI() {
        setTitle("Sistema Contable - Finanzas");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // --- PANEL SUPERIOR (CAPTURA) ---
        JPanel pnlEntrada = new JPanel(new GridBagLayout());
        pnlEntrada.setBorder(BorderFactory.createTitledBorder("Captura de Movimientos"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // FILA 0: TIPO DE REPORTE
        gbc.gridx = 0; gbc.gridy = 0;
        pnlEntrada.add(new JLabel("1. Reporte:"), gbc);
        
        cmbTipoReporte = new JComboBox<>(new String[]{"Balance General", "Estado de Resultados"});
        gbc.gridx = 1; gbc.weightx = 0.5;
        pnlEntrada.add(cmbTipoReporte, gbc);

        // FILA 0 (Lado derecho): CATEGORIA (Activo/Pasivo...)
        gbc.gridx = 2; gbc.weightx = 0;
        pnlEntrada.add(new JLabel("2. Categoría:"), gbc);
        
        cmbCategoria = new JComboBox<>();
        gbc.gridx = 3; gbc.weightx = 0.5;
        pnlEntrada.add(cmbCategoria, gbc);

        // FILA 1: GRUPO (Circulante/Fijo...)
        gbc.gridx = 0; gbc.gridy = 1;
        pnlEntrada.add(new JLabel("3. Grupo:"), gbc);
        
        cmbGrupo = new JComboBox<>();
        gbc.gridx = 1; gbc.weightx = 0.5;
        pnlEntrada.add(cmbGrupo, gbc);

        // FILA 1 (Lado derecho): CUENTA ESPECÍFICA
        gbc.gridx = 2; 
        pnlEntrada.add(new JLabel("4. Cuenta:"), gbc);
        
        cmbCuenta = new JComboBox<>();
        cmbCuenta.setMaximumRowCount(15); // Para que no tape la pantalla si hay muchas
        gbc.gridx = 3; gbc.weightx = 0.5;
        pnlEntrada.add(cmbCuenta, gbc);

        // FILA 2: MONTO Y BOTÓN
        gbc.gridx = 0; gbc.gridy = 2;
        pnlEntrada.add(new JLabel("Monto ($):"), gbc);

        txtMonto = new JTextField();
        gbc.gridx = 1; 
        pnlEntrada.add(txtMonto, gbc);
        
        gbc.gridx = 4; gbc.weightx = 0;
        JButton btnAgregar = new JButton("AGREGAR DATOS");
        
        // 1. COLORES VISIBLES
        btnAgregar.setFont(new Font("Arial", Font.BOLD, 12));
        btnAgregar.setBackground(Color.ORANGE); // Fondo Naranja
        btnAgregar.setForeground(Color.BLACK);  // Texto Negro
        btnAgregar.setOpaque(true);
        btnAgregar.setBorderPainted(false);

        // 2. POSICIÓN "CENTRADA" (Como antes)
        // Esto hace que el botón ocupe las dos columnas de la derecha en la fila de abajo
        gbc.gridx = 2; 
        gbc.gridwidth = 2; // Abarca 2 espacios para verse centrado/ancho
        gbc.fill = GridBagConstraints.HORIZONTAL; // Se estira a lo ancho
        
        btnAgregar.addActionListener(e -> agregarOActualizarCuenta());
        pnlEntrada.add(btnAgregar, gbc);

        // --- TABLA ---
        String[] columnas = {"Código", "Nombre", "Tipo", "Grupo", "Monto"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override public boolean isCellEditable(int row, int column) { return column == 4; }
        };
        tabla = new JTable(modeloTabla);
        tabla.setRowHeight(22);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(70);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(250);
        
        // Eventos Tabla
        tabla.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) eliminarFilasSeleccionadas();
            }
        });
        modeloTabla.addTableModelListener(e -> {
            if (e.getColumn() == 4 && e.getFirstRow() >= 0) actualizarMontoDesdeTabla(e.getFirstRow());
        });

        JScrollPane scrollTabla = new JScrollPane(tabla);

        // --- BOTONES INFERIORES ---
        JPanel pnlBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton btnGenerar = new JButton("GENERAR PDF");
        JButton btnLimpiar = new JButton("Limpiar Todo");
        
        btnGenerar.setFont(new Font("Arial", Font.BOLD, 14));
        btnGenerar.addActionListener(e -> generarReporteActual());
        btnLimpiar.addActionListener(e -> limpiarTodo());

        pnlBotones.add(btnGenerar);
        pnlBotones.add(btnLimpiar);

        lblStatus = new JLabel("Bienvenido al Sistema Contable");
        lblStatus.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        JPanel pnlInferior = new JPanel(new BorderLayout());
        pnlInferior.add(pnlBotones, BorderLayout.CENTER);
        pnlInferior.add(lblStatus, BorderLayout.SOUTH);

        // Listeners de Interfaz
        btnAgregar.addActionListener(e -> agregarOActualizarCuenta());
        txtMonto.addActionListener(e -> agregarOActualizarCuenta());

        // Listeners de Combos (Lógica Cascada)
        cmbTipoReporte.addActionListener(e -> { if(!isUpdating) cambiarModoReporte(); });
        cmbCategoria.addActionListener(e -> { if(!isUpdating) cambiarCategoria(); });
        cmbGrupo.addActionListener(e -> { if(!isUpdating) cambiarGrupo(); });

        add(pnlEntrada, BorderLayout.NORTH);
        add(scrollTabla, BorderLayout.CENTER);
        add(pnlInferior, BorderLayout.SOUTH);
    }

    // ========================================================================
    // LÓGICA DE COMBOS EN CASCADA
    // ========================================================================

    private void inicializarLogicaCascada() {
        cambiarModoReporte(); // Inicia el flujo
    }

// En UIPrincipal.java

    private void cambiarModoReporte() {
        isUpdating = true;
        String reporte = (String) cmbTipoReporte.getSelectedItem();
        cmbCategoria.removeAllItems();
        
        modeloTabla.setRowCount(0); 
        lblStatus.setText("Modo cambiado a: " + reporte);

        if ("Balance General".equals(reporte)) {
            cmbCategoria.addItem("Activo");
            cmbCategoria.addItem("Pasivo");
            cmbCategoria.addItem("Capital Contable");
        } else {
            // --- NUEVAS CATEGORÍAS DEL ESTADO DE RESULTADOS ---
            cmbCategoria.addItem("Ventas y Compras");
            cmbCategoria.addItem("Gastos de Operación");
            cmbCategoria.addItem("Otros"); 
        }
        isUpdating = false;
        cambiarCategoria();
    }

    private void cambiarCategoria() {
        isUpdating = true;
        String reporte = (String) cmbTipoReporte.getSelectedItem();
        String cat = (String) cmbCategoria.getSelectedItem();
        cmbGrupo.removeAllItems();

        if (cat == null) { isUpdating = false; return; }

        if ("Balance General".equals(reporte)) {
            // ... (Lógica del Balance se queda igual) ...
            if ("Activo".equals(cat)) {
                cmbGrupo.addItem("Activo Circulante");
                cmbGrupo.addItem("Activo Fijo");
                cmbGrupo.addItem("Cargos Diferidos");
            } else if ("Pasivo".equals(cat)) {
                cmbGrupo.addItem("Pasivo Circulante"); 
                cmbGrupo.addItem("Pasivo Fijo");      
                cmbGrupo.addItem("Créditos Diferidos");
            } else if ("Capital Contable".equals(cat)) {
                cmbGrupo.addItem("Capital Contable");
            }
        } else {
            // --- LÓGICA EXACTA DE TU LISTA PARA ESTADO DE RESULTADOS ---
            
            if ("Ventas y Compras".equals(cat)) {
                cmbGrupo.addItem("Ventas");   // 1.1
                cmbGrupo.addItem("Compras");  // 1.2
                
            } else if ("Gastos de Operación".equals(cat)) {
                cmbGrupo.addItem("Gastos de venta");           // 2.1
                cmbGrupo.addItem("Gastos de administración");  // 2.2
                cmbGrupo.addItem("Productos financieros");     // 2.3
                cmbGrupo.addItem("Gastos financieros");        // 2.4
                
            } else if ("Otros".equals(cat)) {
                cmbGrupo.addItem("Otros gastos");    // 3.1
                cmbGrupo.addItem("Otros productos"); // 3.2
            }
        }
        isUpdating = false;
        cambiarGrupo(); 
    }

    private void cambiarGrupo() {
        isUpdating = true;
        String grupoSel = (String) cmbGrupo.getSelectedItem();
        String reporte = (String) cmbTipoReporte.getSelectedItem();
        
        cmbCuenta.removeAllItems();

        if (grupoSel != null) {
            
            List<CuentaContable> filtradas = catalogo.getCuentas().stream()
                .filter(c -> {
                    // 1. Filtro: ¿Es para Balance o Resultados?
                    boolean tipoCorrecto = reporte.equals("Balance General") 
                                           ? c.esParaBalance() 
                                           : c.esParaResultados();

                    // 2. Filtro: ¿Coincide el Grupo?
                    // CORRECCIÓN AQUÍ: Es .getGrupo() en español
                    boolean grupoCorrecto = c.getGrupo() != null && 
                                            c.getGrupo().trim().equalsIgnoreCase(grupoSel.trim());
                    
                    return tipoCorrecto && grupoCorrecto;
                })
                .collect(Collectors.toList());

            // Llenar el combo
            for (CuentaContable c : filtradas) {
                cmbCuenta.addItem(c);
            }

            // Feedback visual
            if (filtradas.isEmpty()) {
                lblStatus.setText("⚠️ 0 cuentas. En Excel busca en Grupo: '" + grupoSel + "'");
            } else {
                lblStatus.setText("Cuentas encontradas: " + filtradas.size());
            }
        }
        isUpdating = false;
    }
    
    private void agregarOActualizarCuenta() {
        try {
            CuentaContable sel = (CuentaContable) cmbCuenta.getSelectedItem();
            if (sel == null) { mostrarError("Seleccione una cuenta válida."); return; }

            String textoMonto = txtMonto.getText().trim().replace(",", "");
            //if (textoMonto.isEmpty()) { mostrarError("Ingrese un monto."); return; }

            double monto = Double.parseDouble(textoMonto);
            if (monto < 0) { mostrarError("El monto no puede ser negativo."); return; }

            sel.setSaldo(monto);
            
            int fila = buscarFilaPorCodigo(sel.getCodigo());
            if (fila != -1) {
                modeloTabla.setValueAt(monto, fila, 4);
                lblStatus.setText("Actualizado: " + sel.getNombre());
            } else {
                modeloTabla.addRow(new Object[]{
                    sel.getCodigo(), sel.getNombre(), sel.getTipo(), sel.getGrupo(), monto
                });
                lblStatus.setText("Agregado: " + sel.getNombre());
            }
            txtMonto.setText("");
            txtMonto.requestFocus();
        } catch (NumberFormatException ex) {
            //mostrarError("Número inválido.");
        }
    }

    private int buscarFilaPorCodigo(String codigo) {
        for (int i = 0; i < modeloTabla.getRowCount(); i++) {
            if (modeloTabla.getValueAt(i, 0).toString().equalsIgnoreCase(codigo)) return i;
        }
        return -1;
    }

    private void generarReporteActual() {
        String modo = (String) cmbTipoReporte.getSelectedItem();
        
        // Obtener cuentas (según el filtro seleccionado)
        List<CuentaContable> cuentas = modo.equals("Balance General")
            ? catalogo.getCuentasParaBalance()
            : catalogo.getCuentasParaResultados();

        // Validar que existan datos
        if (cuentas.isEmpty()) { 
            mostrarError("No hay datos capturados para generar el " + modo); 
            return; 
        }

        // Pedir datos de cabecera
        String empresa = JOptionPane.showInputDialog(this, "Nombre de la empresa:");
        if (empresa == null || empresa.isBlank()) return;

        // --- LÓGICA DE FORMATO CONDICIONAL ---
        String tipoFormato = "Reporte"; // Valor por defecto (Vertical)

        // Solo preguntamos el formato si es Balance General
        if (modo.equals("Balance General")) {
            String[] formatos = {"Cuenta (Horizontal)", "Reporte (Vertical)"};
            String formatoSel = (String) JOptionPane.showInputDialog(this, "Seleccione el Formato:", "Opciones de Impresión", 
                    JOptionPane.QUESTION_MESSAGE, null, formatos, formatos[0]);
            
            if (formatoSel == null) return; // Si cancela, no hacemos nada
            tipoFormato = formatoSel.contains("Cuenta") ? "Cuenta" : "Reporte";
        }
        // -------------------------------------

        String periodo = JOptionPane.showInputDialog(this, "Fecha / Periodo:");
        
        String quienElaboro = JOptionPane.showInputDialog(this, "Nombre de quien Elaboró:");
        if (quienElaboro == null) quienElaboro = " ";
        
        String quienAutorizo = JOptionPane.showInputDialog(this, "Nombre de quien Autorizó:");
        if (quienAutorizo == null) quienAutorizo = " ";

        // Guardar archivo
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(modo.replace(" ", "_") + ".pdf"));
        
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String ruta = fc.getSelectedFile().getAbsolutePath();
                if(!ruta.toLowerCase().endsWith(".pdf")) ruta += ".pdf";
                
                // Generar PDF
                GeneradorPDF.generarPDF(empresa, modo, periodo, cuentas, tipoFormato, ruta, quienElaboro, quienAutorizo);
                
                JOptionPane.showMessageDialog(this, "¡PDF Generado con éxito!");
                try { Desktop.getDesktop().open(new File(ruta)); } catch(Exception e){}
            } catch (Exception ex) {
                ex.printStackTrace();
                mostrarError("Error al generar el PDF: " + ex.getMessage());
            }
        }
    }
    
    private void actualizarMontoDesdeTabla(int row) {
        try {
            String codigo = modeloTabla.getValueAt(row, 0).toString();
            double val = Double.parseDouble(modeloTabla.getValueAt(row, 4).toString());
            catalogo.buscarPorCodigo(codigo).ifPresent(c -> c.setSaldo(val));
        } catch (Exception e) {}
    }

    private void eliminarFilasSeleccionadas() {
        int[] filas = tabla.getSelectedRows();
        if (filas.length > 0) {
             if (JOptionPane.showConfirmDialog(this, "¿Borrar?", "Confirma", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                for (int i = filas.length - 1; i >= 0; i--) {
                    String cod = modeloTabla.getValueAt(filas[i], 0).toString();
                    catalogo.buscarPorCodigo(cod).ifPresent(c -> c.setSaldo(0));
                    modeloTabla.removeRow(filas[i]);
                }
             }
        }
    }
    
    private void limpiarTodo() {
        catalogo.reiniciarSaldos();
        modeloTabla.setRowCount(0);
        txtMonto.setText("");
    }

private void verificarCatalogo() {
        if (!catalogo.isCargadoCorrectamente()) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar el catálogo:\n" + catalogo.getMensajeError() +
                "\n\nAsegúrese de que 'catalogo_cuentas.xlsx' esté en el directorio.",
                "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            // --- CÓDIGO DE DIAGNÓSTICO (NUEVO) ---
            System.out.println("========================================");
            System.out.println("🔍 DIAGNÓSTICO DE CARGA DE EXCEL");
            System.out.println("Total de cuentas leídas: " + catalogo.getCuentas().size());
            for (CuentaContable c : catalogo.getCuentas()) {
                System.out.println("Cuenta: [" + c.getNombre() + "] " +
                                   "| Tipo: [" + c.getTipo() + "] " + 
                                   "| Grupo: [" + c.getGrupo() + "]");
            }
            System.out.println("========================================");
            // -------------------------------------
        }
    }

    private void mostrarError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
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