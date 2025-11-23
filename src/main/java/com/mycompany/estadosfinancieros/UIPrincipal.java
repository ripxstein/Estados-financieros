package com.mycompany.estadosfinancieros;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.filechooser.FileNameExtensionFilter; // IMPORTANTE: Nuevo import
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class UIPrincipal extends JFrame {
    private CatalogoCuentas catalogo;
    
    // COLORES INSTITUCIONALES IPN
    // Guinda aproximado (Pantone 222 C)
    private final Color COLOR_GUINDA = new Color(109, 13, 30); 
    private final Color COLOR_BLANCO = Color.WHITE;
    private final Color COLOR_NEGRO = Color.BLACK;

    // SELECTORES EN CASCADA
    private JComboBox<String> cmbTipoReporte; 
    private JComboBox<String> cmbCategoria;   
    private JComboBox<String> cmbGrupo;       
    private JComboBox<CuentaContable> cmbCuenta; 
    
    private JTextField txtMonto;
    private DefaultTableModel modeloTabla;
    private JTable tabla;
    private JLabel lblStatus;
    
    private boolean isUpdating = false;

    public UIPrincipal() {
        catalogo = new CatalogoCuentas();
        initUI();
        verificarCatalogo();
        inicializarLogicaCascada();
    }

    private void initUI() {
        setTitle("Sistema Contable ARP - Finanzas IPN");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750); 
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        
        getContentPane().setBackground(COLOR_GUINDA);

        // --- 0. PANEL ENCABEZADO ---
        JPanel pnlEncabezado = new JPanel(new BorderLayout());
        pnlEncabezado.setBackground(COLOR_GUINDA);
        pnlEncabezado.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        // Cargar Logo IPN (Fijo en la interfaz)
        JLabel lblLogo = new JLabel();
        try {
            ImageIcon icon = new ImageIcon("logo_ipn.png"); 
            Image img = icon.getImage().getScaledInstance(65, 65, Image.SCALE_SMOOTH);
            lblLogo.setIcon(new ImageIcon(img));
        } catch (Exception e) {
            lblLogo.setText("[LOGO IPN]");
            lblLogo.setForeground(COLOR_BLANCO);
        }
        
        JLabel lblTituloPrincipal = new JLabel("SISTEMA DE ESTADOS FINANCIEROS", SwingConstants.CENTER);
        lblTituloPrincipal.setFont(new Font("Arial", Font.BOLD, 24));
        lblTituloPrincipal.setForeground(COLOR_BLANCO);

        pnlEncabezado.add(lblLogo, BorderLayout.WEST);
        pnlEncabezado.add(lblTituloPrincipal, BorderLayout.CENTER);
        
        JPanel dummy = new JPanel(); 
        dummy.setPreferredSize(new Dimension(60, 60));
        dummy.setOpaque(false);
        pnlEncabezado.add(dummy, BorderLayout.EAST);

        // --- 1. PANEL SUPERIOR (CAPTURA) ---
        JPanel pnlEntrada = new JPanel(new GridBagLayout());
        pnlEntrada.setBackground(COLOR_GUINDA); 
        pnlEntrada.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_BLANCO), 
                "Captura de Movimientos", 
                0, 0, 
                new Font("Arial", Font.BOLD, 12), 
                COLOR_BLANCO));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // FILA 0
        gbc.gridx = 0; gbc.gridy = 0;
        pnlEntrada.add(crearLabel("1. Reporte:"), gbc);
        
        cmbTipoReporte = new JComboBox<>(new String[]{"Balance General", "Estado de Resultados"});
        gbc.gridx = 1; gbc.weightx = 0.5;
        pnlEntrada.add(cmbTipoReporte, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        pnlEntrada.add(crearLabel("2. Categoría:"), gbc);
        
        cmbCategoria = new JComboBox<>();
        gbc.gridx = 3; gbc.weightx = 0.5;
        pnlEntrada.add(cmbCategoria, gbc);

        // FILA 1
        gbc.gridx = 0; gbc.gridy = 1;
        pnlEntrada.add(crearLabel("3. Grupo:"), gbc);
        
        cmbGrupo = new JComboBox<>();
        gbc.gridx = 1; gbc.weightx = 0.5;
        pnlEntrada.add(cmbGrupo, gbc);

        gbc.gridx = 2; 
        pnlEntrada.add(crearLabel("4. Cuenta:"), gbc);
        
        cmbCuenta = new JComboBox<>();
        cmbCuenta.setMaximumRowCount(15);
        gbc.gridx = 3; gbc.weightx = 0.5;
        pnlEntrada.add(cmbCuenta, gbc);

        // FILA 2
        gbc.gridx = 0; gbc.gridy = 2;
        pnlEntrada.add(crearLabel("Monto ($):"), gbc);

        txtMonto = new JTextField();
        gbc.gridx = 1; 
        pnlEntrada.add(txtMonto, gbc);
        
        gbc.gridx = 2; 
        gbc.gridwidth = 2; 
        
        JButton btnAgregar = new JButton("AGREGAR DATOS");
        estilizarBoton(btnAgregar); 
        
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
        
        tabla.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) eliminarFilasSeleccionadas();
            }
        });
        modeloTabla.addTableModelListener(e -> {
            if (e.getColumn() == 4 && e.getFirstRow() >= 0) actualizarMontoDesdeTabla(e.getFirstRow());
        });

        JScrollPane scrollTabla = new JScrollPane(tabla);
        scrollTabla.setBorder(BorderFactory.createLineBorder(COLOR_GUINDA, 2));

        // --- BOTONES INFERIORES ---
        JPanel pnlBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        pnlBotones.setBackground(COLOR_GUINDA); 

        JButton btnGenerar = new JButton("GENERAR PDF");
        JButton btnLimpiar = new JButton("Limpiar Todo");
        
        estilizarBoton(btnGenerar);
        estilizarBoton(btnLimpiar);
        
        btnGenerar.addActionListener(e -> generarReporteActual());
        btnLimpiar.addActionListener(e -> limpiarTodo());

        pnlBotones.add(btnGenerar);
        pnlBotones.add(btnLimpiar);

        lblStatus = new JLabel("Bienvenido al Sistema Contable");
        lblStatus.setForeground(COLOR_BLANCO); 
        lblStatus.setFont(new Font("Arial", Font.ITALIC, 12));
        lblStatus.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JPanel pnlInferior = new JPanel(new BorderLayout());
        pnlInferior.setBackground(COLOR_GUINDA);
        pnlInferior.add(pnlBotones, BorderLayout.CENTER);
        pnlInferior.add(lblStatus, BorderLayout.SOUTH);

        // Listeners
        txtMonto.addActionListener(e -> agregarOActualizarCuenta());
        cmbTipoReporte.addActionListener(e -> { if(!isUpdating) cambiarModoReporte(); });
        cmbCategoria.addActionListener(e -> { if(!isUpdating) cambiarCategoria(); });
        cmbGrupo.addActionListener(e -> { if(!isUpdating) cambiarGrupo(); });

        // Armado final
        add(pnlEncabezado, BorderLayout.NORTH);
        
        JPanel pnlCentro = new JPanel(new BorderLayout(0, 10));
        pnlCentro.setBackground(COLOR_GUINDA);
        pnlCentro.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        pnlCentro.add(pnlEntrada, BorderLayout.NORTH);
        pnlCentro.add(scrollTabla, BorderLayout.CENTER);
        
        add(pnlCentro, BorderLayout.CENTER);
        add(pnlInferior, BorderLayout.SOUTH);
    }

    private JLabel crearLabel(String texto) {
        JLabel l = new JLabel(texto);
        l.setForeground(COLOR_BLANCO); 
        l.setFont(new Font("Arial", Font.BOLD, 12));
        return l;
    }
    
    private void estilizarBoton(JButton btn) {
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setBackground(COLOR_BLANCO); 
        btn.setForeground(COLOR_NEGRO);  
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY, 1),
            BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
    }

    // ========================================================================
    // LÓGICA DE NEGOCIO
    // ========================================================================

    private void inicializarLogicaCascada() {
        cambiarModoReporte(); 
    }

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
            if ("Ventas y Compras".equals(cat)) {
                cmbGrupo.addItem("Ventas");
                cmbGrupo.addItem("Compras");
            } else if ("Gastos de Operación".equals(cat)) {
                cmbGrupo.addItem("Gastos de venta");
                cmbGrupo.addItem("Gastos de administración");
                cmbGrupo.addItem("Productos financieros");
                cmbGrupo.addItem("Gastos financieros");
            } else if ("Otros".equals(cat)) {
                cmbGrupo.addItem("Otros gastos");
                cmbGrupo.addItem("Otros productos");
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
                    boolean tipoCorrecto = reporte.equals("Balance General") 
                                           ? c.esParaBalance() 
                                           : c.esParaResultados();
                    boolean grupoCorrecto = c.getGrupo() != null && 
                                            c.getGrupo().trim().equalsIgnoreCase(grupoSel.trim());
                    return tipoCorrecto && grupoCorrecto;
                })
                .collect(Collectors.toList());

            for (CuentaContable c : filtradas) {
                cmbCuenta.addItem(c);
            }

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
            mostrarError("Número inválido en monto.");
        }
    }

    private int buscarFilaPorCodigo(String codigo) {
        for (int i = 0; i < modeloTabla.getRowCount(); i++) {
            if (modeloTabla.getValueAt(i, 0).toString().equalsIgnoreCase(codigo)) return i;
        }
        return -1;
    }

    // --- MÉTODO PRINCIPAL MODIFICADO PARA SELECCIONAR LOGO ---
    private void generarReporteActual() {
        String modo = (String) cmbTipoReporte.getSelectedItem();
        
        List<CuentaContable> cuentas = modo.equals("Balance General")
            ? catalogo.getCuentasParaBalance()
            : catalogo.getCuentasParaResultados();

        if (cuentas.isEmpty()) { 
            mostrarError("No hay datos capturados para generar el " + modo); 
            return; 
        }

        // 1. Datos básicos
        String empresa = JOptionPane.showInputDialog(this, "Nombre de la empresa:");
        if (empresa == null || empresa.isBlank()) return;

        String tipoFormato = "Reporte"; 
        if (modo.equals("Balance General")) {
            String[] formatos = {"Cuenta (Horizontal)", "Reporte (Vertical)"};
            String formatoSel = (String) JOptionPane.showInputDialog(this, "Seleccione el Formato:", "Opciones de Impresión", 
                    JOptionPane.QUESTION_MESSAGE, null, formatos, formatos[0]);
            
            if (formatoSel == null) return;
            tipoFormato = formatoSel.contains("Cuenta") ? "Cuenta" : "Reporte";
        }

        String periodo = JOptionPane.showInputDialog(this, "Fecha / Periodo:");
        String quienElaboro = JOptionPane.showInputDialog(this, "Nombre de quien Elaboró:");
        if (quienElaboro == null) quienElaboro = " ";
        String quienAutorizo = JOptionPane.showInputDialog(this, "Nombre de quien Autorizó:");
        if (quienAutorizo == null) quienAutorizo = " ";

        // 2. PREGUNTAR POR EL LOGO
        String rutaLogo = null;
        int respuestaLogo = JOptionPane.showConfirmDialog(this, "¿Desea agregar un logo de la empresa al reporte?", "Logo Empresa", JOptionPane.YES_NO_OPTION);
        if (respuestaLogo == JOptionPane.YES_OPTION) {
            JFileChooser fileChooserLogo = new JFileChooser();
            fileChooserLogo.setDialogTitle("Seleccione el Logo de la Empresa");
            fileChooserLogo.setFileFilter(new FileNameExtensionFilter("Imágenes (JPG, PNG)", "jpg", "png", "jpeg"));
            
            if (fileChooserLogo.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                rutaLogo = fileChooserLogo.getSelectedFile().getAbsolutePath();
            }
        }

        // 3. Guardar archivo PDF
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(modo.replace(" ", "_") + ".pdf"));
        
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String ruta = fc.getSelectedFile().getAbsolutePath();
                if(!ruta.toLowerCase().endsWith(".pdf")) ruta += ".pdf";
                
                // --- SE PASA 'rutaLogo' AL GENERADOR ---
                GeneradorPDF.generarPDF(empresa, modo, periodo, cuentas, tipoFormato, ruta, quienElaboro, quienAutorizo, rutaLogo);
                
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
             if (JOptionPane.showConfirmDialog(this, "¿Borrar seleccionados?", "Confirma", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                for (int i = filas.length - 1; i >= 0; i--) {
                    String cod = modeloTabla.getValueAt(filas[i], 0).toString();
                    catalogo.buscarPorCodigo(cod).ifPresent(c -> c.setSaldo(0));
                    modeloTabla.removeRow(filas[i]);
                }
             }
        }
    }
    
    private void limpiarTodo() {
        if (JOptionPane.showConfirmDialog(this, "¿Seguro de limpiar todo?", "Confirma", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            catalogo.reiniciarSaldos();
            modeloTabla.setRowCount(0);
            txtMonto.setText("");
        }
    }

    private void verificarCatalogo() {
        if (!catalogo.isCargadoCorrectamente()) {
            JOptionPane.showMessageDialog(this,
                "Error al cargar el catálogo:\n" + catalogo.getMensajeError() +
                "\n\nAsegúrese de que 'catalogo_cuentas.xlsx' esté en el directorio.",
                "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            System.out.println("Catálogo cargado correctamente: " + catalogo.getCuentas().size() + " cuentas.");
        }
    }

    private void mostrarError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> new UIPrincipal().setVisible(true));
    }
}