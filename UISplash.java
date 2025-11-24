package com.mycompany.estadosfinancieros;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class UISplash extends JFrame {

    private boolean iniciar = false;

    public UISplash() {
        initComponents();
    }

    public boolean usuarioQuiereIniciar() {
        return iniciar;
    }

    private void initComponents() {

        // Cargar imagen
        ImageIcon iconoOriginal = new ImageIcon(getClass().getResource("/logo.png"));
        Image imagenEscalada = iconoOriginal.getImage().getScaledInstance(320, 180, Image.SCALE_SMOOTH);
        ImageIcon iconoEscalado = new ImageIcon(imagenEscalada);

        JLabel lblLogo = new JLabel(iconoEscalado, SwingConstants.CENTER);

        JLabel lblTitulo = new JLabel("Bienvenid@ a Sistema Contable ARP", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 22));

        JLabel lblAbajo = new JLabel("Presione ENTER para iniciar", SwingConstants.CENTER);
        lblAbajo.setFont(new Font("Segoe UI", Font.PLAIN, 16));

        // entrada de teclado
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    iniciar = true;
                    dispose(); // cerrar splash
                }
            }
        });

        setFocusable(true);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 350);
        setLayout(new BorderLayout());

        add(lblLogo, BorderLayout.NORTH);
        add(lblTitulo, BorderLayout.CENTER);
        add(lblAbajo, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
    }
}
