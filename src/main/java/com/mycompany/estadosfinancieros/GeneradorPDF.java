/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.estadosfinancieros;

/**
 *
 * @author ripxs
 */
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class GeneradorPDF {

    public static void generarPDF(String empresa, String titulo, String periodo,
                                  List<CuentaContable> cuentas, String tipoFormato,
                                  String nombreArchivo) {
        try {
            Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(doc, new FileOutputStream(nombreArchivo));
            doc.open();

            // === FUENTES ===
            Font empresaFont = new Font(Font.HELVETICA, 14, Font.BOLD);
            Font tituloFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font subtituloFont = new Font(Font.HELVETICA, 12, Font.ITALIC);
            Font seccionFont = new Font(Font.HELVETICA, 14, Font.BOLD);
            Font textoFont = new Font(Font.HELVETICA, 12);

            // === ENCABEZADO ===
            Paragraph pEmpresa = new Paragraph(empresa.toUpperCase() + "\n", empresaFont);
            pEmpresa.setAlignment(Element.ALIGN_CENTER);
            doc.add(pEmpresa);

            Paragraph encabezado = new Paragraph(titulo + "\n", tituloFont);
            encabezado.setAlignment(Element.ALIGN_CENTER);
            doc.add(encabezado);

            Paragraph sub = new Paragraph(periodo + "\n\n", subtituloFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            doc.add(sub);

            // === SECCIONES ===
            if (titulo.contains("Balance")) {
                generarSeccionBalance(doc, cuentas, tipoFormato, seccionFont, textoFont);
            } else {
                generarSeccionResultados(doc, cuentas, tipoFormato, seccionFont, textoFont);
            }

            doc.close();
            System.out.println("PDF generado correctamente: " + nombreArchivo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==============================
    //        BALANCE GENERAL
    // ==============================
    private static void generarSeccionBalance(Document doc, List<CuentaContable> cuentas,
                                              String tipoFormato, Font seccionFont, Font textoFont)
            throws DocumentException {

        Map<String, Double> totales = new LinkedHashMap<>();
        totales.put("Activo", 0.0);
        totales.put("Pasivo", 0.0);
        totales.put("Capital", 0.0);

        for (String tipo : totales.keySet()) {
            doc.add(new Paragraph("\n" + tipo.toUpperCase(), seccionFont));
            double total = 0.0;

            if (tipoFormato.equalsIgnoreCase("Cuenta")) {
                for (CuentaContable c : cuentas) {
                    if (c.getTipo().equalsIgnoreCase(tipo)) {
                        doc.add(new Paragraph(
                                String.format("%s  %s  $%.2f",
                                        c.getCodigo(), c.getNombre(), c.getSaldo()), textoFont));
                        total += c.getSaldo();
                    }
                }
            } else {
                PdfPTable tabla = new PdfPTable(3);
                tabla.setWidths(new float[]{2, 5, 2});
                tabla.setWidthPercentage(100);
                tabla.addCell(new Phrase("Código", textoFont));
                tabla.addCell(new Phrase("Nombre", textoFont));
                tabla.addCell(new Phrase("Saldo", textoFont));

                for (CuentaContable c : cuentas) {
                    if (c.getTipo().equalsIgnoreCase(tipo)) {
                        tabla.addCell(c.getCodigo());
                        tabla.addCell(c.getNombre());
                        tabla.addCell(String.format("$%.2f", c.getSaldo()));
                        total += c.getSaldo();
                    }
                }
                doc.add(tabla);
            }

            doc.add(new Paragraph(String.format("Total %s: $%.2f\n", tipo, total), textoFont));
            totales.put(tipo, total);
        }

        // === COMPROBACIÓN ===
        double activo = totales.get("Activo");
        double pasivoMasCapital = totales.get("Pasivo") + totales.get("Capital");
        double diferencia = activo - pasivoMasCapital;

        Paragraph finalMsg = new Paragraph(
                String.format("\nComprobación: Activo (%.2f) = Pasivo + Capital (%.2f)\nDiferencia: %.2f",
                        activo, pasivoMasCapital, diferencia),
                textoFont);
        finalMsg.setAlignment(Element.ALIGN_RIGHT);
        doc.add(finalMsg);
    }

    // ==============================
    //      ESTADO DE RESULTADOS
    // ==============================
    private static void generarSeccionResultados(Document doc, List<CuentaContable> cuentas,
                                                 String tipoFormato, Font seccionFont, Font textoFont)
            throws DocumentException {

        double totalIngresos = 0.0;
        double totalGastos = 0.0;

        // --- INGRESOS ---
        doc.add(new Paragraph("\nINGRESOS", seccionFont));
        if (tipoFormato.equalsIgnoreCase("Cuenta")) {
            for (CuentaContable c : cuentas) {
                if (c.getTipo().equalsIgnoreCase("Ingreso")) {
                    doc.add(new Paragraph(
                            String.format("%s  %s  $%.2f",
                                    c.getCodigo(), c.getNombre(), c.getSaldo()), textoFont));
                    totalIngresos += c.getSaldo();
                }
            }
        } else {
            PdfPTable tabla = new PdfPTable(3);
            tabla.setWidths(new float[]{2, 5, 2});
            tabla.setWidthPercentage(100);
            tabla.addCell(new Phrase("Código", textoFont));
            tabla.addCell(new Phrase("Nombre", textoFont));
            tabla.addCell(new Phrase("Saldo", textoFont));

            for (CuentaContable c : cuentas) {
                if (c.getTipo().equalsIgnoreCase("Ingreso")) {
                    tabla.addCell(c.getCodigo());
                    tabla.addCell(c.getNombre());
                    tabla.addCell(String.format("$%.2f", c.getSaldo()));
                    totalIngresos += c.getSaldo();
                }
            }
            doc.add(tabla);
        }

        doc.add(new Paragraph(String.format("Total Ingresos: $%.2f\n", totalIngresos), textoFont));

        // --- GASTOS ---
        doc.add(new Paragraph("\nGASTOS", seccionFont));
        if (tipoFormato.equalsIgnoreCase("Cuenta")) {
            for (CuentaContable c : cuentas) {
                if (c.getTipo().equalsIgnoreCase("Gasto")) {
                    doc.add(new Paragraph(
                            String.format("%s  %s  $%.2f",
                                    c.getCodigo(), c.getNombre(), c.getSaldo()), textoFont));
                    totalGastos += c.getSaldo();
                }
            }
        } else {
            PdfPTable tabla = new PdfPTable(3);
            tabla.setWidths(new float[]{2, 5, 2});
            tabla.setWidthPercentage(100);
            tabla.addCell(new Phrase("Código", textoFont));
            tabla.addCell(new Phrase("Nombre", textoFont));
            tabla.addCell(new Phrase("Saldo", textoFont));

            for (CuentaContable c : cuentas) {
                if (c.getTipo().equalsIgnoreCase("Gasto")) {
                    tabla.addCell(c.getCodigo());
                    tabla.addCell(c.getNombre());
                    tabla.addCell(String.format("$%.2f", c.getSaldo()));
                    totalGastos += c.getSaldo();
                }
            }
            doc.add(tabla);
        }

        doc.add(new Paragraph(String.format("Total Gastos: $%.2f\n", totalGastos), textoFont));

        // --- UTILIDAD NETA ---
        double utilidad = totalIngresos - totalGastos;
        Paragraph utilidadParrafo = new Paragraph(
                String.format("\nUTILIDAD NETA: $%.2f", utilidad),
                new Font(Font.HELVETICA, 14, Font.BOLD));
        utilidadParrafo.setAlignment(Element.ALIGN_RIGHT);
        doc.add(utilidadParrafo);
    }
}

