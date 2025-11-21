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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class GeneradorPDF {

    public static void generarPDF(String empresa, String titulo, String periodo,
                                  List<CuentaContable> cuentas, String tipoFormato,
                                  String nombreArchivo) {
        try {
            Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(doc, new FileOutputStream(nombreArchivo));
            doc.open();

            // === FUENTES ===
            Font empresaFont   = new Font(Font.HELVETICA, 14, Font.BOLD);
            Font tituloFont    = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font subtituloFont = new Font(Font.HELVETICA, 12, Font.ITALIC);
            Font seccionFont   = new Font(Font.HELVETICA, 14, Font.BOLD);
            Font textoFont     = new Font(Font.HELVETICA, 12);

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

            // === CONTENIDO (BALANCE O RESULTADOS) ===
            if (titulo.toLowerCase().contains("balance")) {
                generarSeccionBalance(doc, cuentas, tipoFormato, seccionFont, textoFont);
            } else {
                generarSeccionResultados(doc, cuentas, tipoFormato, seccionFont, textoFont);
            }

            // === FIRMAS AL FINAL ===
            agregarFirmas(doc, textoFont);

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

        // Separar por tipo
        List<CuentaContable> activos  = new ArrayList<>();
        List<CuentaContable> pasivos  = new ArrayList<>();
        List<CuentaContable> capital  = new ArrayList<>();

        for (CuentaContable c : cuentas) {
            if (c.getTipo().equalsIgnoreCase("Activo")) {
                activos.add(c);
            } else if (c.getTipo().equalsIgnoreCase("Pasivo")) {
                pasivos.add(c);
            } else if (c.getTipo().equalsIgnoreCase("Capital")) {
                capital.add(c);
            }
        }

        double totalActivo = 0.0;
        double totalPasivo = 0.0;
        double totalCapital = 0.0;

        for (CuentaContable c : activos) totalActivo += c.getSaldo();
        for (CuentaContable c : pasivos) totalPasivo += c.getSaldo();
        for (CuentaContable c : capital) totalCapital += c.getSaldo();

        if (tipoFormato.equalsIgnoreCase("Cuenta")) {
            // ======= FORMA DE CUENTA (T) CON DOS COLUMNAS =======
            PdfPTable tabla = new PdfPTable(2);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{1, 1});

            // Columna izquierda: ACTIVO
            PdfPCell celdaIzq = new PdfPCell();
            celdaIzq.setBorder(Rectangle.NO_BORDER);

            Paragraph pIzqTitulo = new Paragraph("ACTIVO\n\n", seccionFont);
            celdaIzq.addElement(pIzqTitulo);

            for (CuentaContable c : activos) {
                Paragraph p = new Paragraph(
                        String.format("%s  %s  $%.2f",
                                c.getCodigo(), c.getNombre(), c.getSaldo()), textoFont);
                celdaIzq.addElement(p);
            }
            celdaIzq.addElement(new Paragraph(
                    String.format("\nTotal Activo: $%.2f", totalActivo), textoFont));

            // Columna derecha: PASIVO Y CAPITAL
            PdfPCell celdaDer = new PdfPCell();
            celdaDer.setBorder(Rectangle.NO_BORDER);

            Paragraph pDerTitulo = new Paragraph("PASIVO Y CAPITAL CONTABLE\n\n", seccionFont);
            celdaDer.addElement(pDerTitulo);

            // PASIVO
            if (!pasivos.isEmpty()) {
                celdaDer.addElement(new Paragraph("PASIVO\n", seccionFont));
                for (CuentaContable c : pasivos) {
                    Paragraph p = new Paragraph(
                            String.format("%s  %s  $%.2f",
                                    c.getCodigo(), c.getNombre(), c.getSaldo()), textoFont);
                    celdaDer.addElement(p);
                }
                celdaDer.addElement(new Paragraph(
                        String.format("Total Pasivo: $%.2f\n", totalPasivo), textoFont));
            }

            // CAPITAL
            if (!capital.isEmpty()) {
                celdaDer.addElement(new Paragraph("CAPITAL CONTABLE\n", seccionFont));
                for (CuentaContable c : capital) {
                    Paragraph p = new Paragraph(
                            String.format("%s  %s  $%.2f",
                                    c.getCodigo(), c.getNombre(), c.getSaldo()), textoFont);
                    celdaDer.addElement(p);
                }
                celdaDer.addElement(new Paragraph(
                        String.format("Total Capital: $%.2f\n", totalCapital), textoFont));
            }

            double pasivoMasCapital = totalPasivo + totalCapital;
            celdaDer.addElement(new Paragraph(
                    String.format("Total Pasivo + Capital: $%.2f", pasivoMasCapital), textoFont));

            tabla.addCell(celdaIzq);
            tabla.addCell(celdaDer);

            doc.add(tabla);

            // Comprobación al final
            double diferencia = totalActivo - pasivoMasCapital;
            Paragraph finalMsg = new Paragraph(
                    String.format("\nComprobación: Activo (%.2f) = Pasivo + Capital (%.2f)\nDiferencia: %.2f",
                            totalActivo, pasivoMasCapital, diferencia),
                    textoFont);
            finalMsg.setAlignment(Element.ALIGN_RIGHT);
            doc.add(finalMsg);

        } else {
            // ======= FORMA DE REPORTE (SECCIONES UNA DEBAJO DE OTRA) =======
            // ACTIVO
            doc.add(new Paragraph("\nACTIVO", seccionFont));
            PdfPTable tAct = new PdfPTable(3);
            tAct.setWidthPercentage(100);
            tAct.setWidths(new float[]{2, 5, 2});
            tAct.addCell(new Phrase("Código", textoFont));
            tAct.addCell(new Phrase("Nombre", textoFont));
            tAct.addCell(new Phrase("Saldo", textoFont));
            for (CuentaContable c : activos) {
                tAct.addCell(c.getCodigo());
                tAct.addCell(c.getNombre());
                tAct.addCell(String.format("$%.2f", c.getSaldo()));
            }
            doc.add(tAct);
            doc.add(new Paragraph(String.format("Total Activo: $%.2f\n", totalActivo), textoFont));

            // PASIVO
            doc.add(new Paragraph("\nPASIVO", seccionFont));
            PdfPTable tPas = new PdfPTable(3);
            tPas.setWidthPercentage(100);
            tPas.setWidths(new float[]{2, 5, 2});
            tPas.addCell(new Phrase("Código", textoFont));
            tPas.addCell(new Phrase("Nombre", textoFont));
            tPas.addCell(new Phrase("Saldo", textoFont));
            for (CuentaContable c : pasivos) {
                tPas.addCell(c.getCodigo());
                tPas.addCell(c.getNombre());
                tPas.addCell(String.format("$%.2f", c.getSaldo()));
            }
            doc.add(tPas);
            doc.add(new Paragraph(String.format("Total Pasivo: $%.2f\n", totalPasivo), textoFont));

            // CAPITAL
            doc.add(new Paragraph("\nCAPITAL CONTABLE", seccionFont));
            PdfPTable tCap = new PdfPTable(3);
            tCap.setWidthPercentage(100);
            tCap.setWidths(new float[]{2, 5, 2});
            tCap.addCell(new Phrase("Código", textoFont));
            tCap.addCell(new Phrase("Nombre", textoFont));
            tCap.addCell(new Phrase("Saldo", textoFont));
            for (CuentaContable c : capital) {
                tCap.addCell(c.getCodigo());
                tCap.addCell(c.getNombre());
                tCap.addCell(String.format("$%.2f", c.getSaldo()));
            }
            doc.add(tCap);
            doc.add(new Paragraph(String.format("Total Capital: $%.2f\n", totalCapital), textoFont));

            double pasivoMasCapital = totalPasivo + totalCapital;
            double diferencia = totalActivo - pasivoMasCapital;

            Paragraph finalMsg = new Paragraph(
                    String.format("\nComprobación: Activo (%.2f) = Pasivo + Capital (%.2f)\nDiferencia: %.2f",
                            totalActivo, pasivoMasCapital, diferencia),
                    textoFont);
            finalMsg.setAlignment(Element.ALIGN_RIGHT);
            doc.add(finalMsg);
        }
    }

    // ==============================
    //      ESTADO DE RESULTADOS
    // ==============================
private static void generarSeccionResultados(Document doc, List<CuentaContable> cuentas,
                                             String tipoFormato, Font seccionFont, Font textoFont)
        throws DocumentException {

    // ==========================
    //   GRUPOS DEFINIDOS POR EL EXCEL
    // ==========================
    String[] gruposOficiales = {
            "Ventas",
            "Ventas – Deducciones",
            "Costo de ventas",
            "Gastos de venta",
            "Gastos de administración",
            "Gastos financieros",
            "Productos financieros",
            "Otros gastos",
            "Otros productos"
    };

    Map<String, List<CuentaContable>> grupos = new LinkedHashMap<>();
    for (String g : gruposOficiales)
        grupos.put(g, new ArrayList<>());

    for (CuentaContable c : cuentas) {
        if (grupos.containsKey(c.getGrupo()))
            grupos.get(c.getGrupo()).add(c);
    }

    List<CuentaContable> ventas            = grupos.get("Ventas");
    List<CuentaContable> deducciones      = grupos.get("Ventas – Deducciones");
    List<CuentaContable> costo            = grupos.get("Costo de ventas");
    List<CuentaContable> gastosVenta      = grupos.get("Gastos de venta");
    List<CuentaContable> gastosAdmin      = grupos.get("Gastos de administración");
    List<CuentaContable> gastosFin        = grupos.get("Gastos financieros");
    List<CuentaContable> productosFin     = grupos.get("Productos financieros");
    List<CuentaContable> otrosGastos      = grupos.get("Otros gastos");
    List<CuentaContable> otrosProductos   = grupos.get("Otros productos");

    Font bold12 = new Font(Font.HELVETICA, 12, Font.BOLD);
    Font bold14 = new Font(Font.HELVETICA, 14, Font.BOLD);
    Font bold16 = new Font(Font.HELVETICA, 16, Font.BOLD);

    // ==========================
    //   VENTAS NETAS
    // ==========================
    double totalVentas       = ventas.stream().mapToDouble(CuentaContable::getSaldo).sum();
    double totalDeducciones  = deducciones.stream().mapToDouble(CuentaContable::getSaldo).sum();
    double ventasNetas       = totalVentas - totalDeducciones;

    // ==========================
    //   COSTO DE VENTAS
    // ==========================
    double compras = 0, gastosCompra = 0, devCompra = 0, descCompra = 0, invInicial = 0, invFinal = 0;

    for (CuentaContable c : costo) {
        String n = c.getNombre().toLowerCase();

        if (n.contains("compras") && !n.contains("devoluciones") && !n.contains("descuentos"))
            compras += c.getSaldo();

        else if (n.contains("gastos de compra"))
            gastosCompra += c.getSaldo();

        else if (n.contains("devoluciones sobre compras"))
            devCompra += c.getSaldo();

        else if (n.contains("descuentos sobre compras"))
            descCompra += c.getSaldo();

        else if (n.contains("inventario inicial"))
            invInicial += c.getSaldo();

        else if (n.contains("inventario final"))
            invFinal += c.getSaldo();
    }

    double comprasTotales   = compras + gastosCompra;
    double comprasNetas     = comprasTotales - devCompra - descCompra;
    double totalMercancia   = invInicial + comprasNetas;
    double costoVentasFinal = totalMercancia - invFinal;

    // ==========================
    //   UTILIDAD BRUTA
    // ==========================
    double utilidadBruta = ventasNetas - costoVentasFinal;

    // ==========================
    //   GASTOS DE OPERACIÓN
    // ==========================
    double totalGastosVenta = gastosVenta.stream().mapToDouble(CuentaContable::getSaldo).sum();
    double totalGastosAdmin = gastosAdmin.stream().mapToDouble(CuentaContable::getSaldo).sum();

    double gastosOperacion = totalGastosVenta + totalGastosAdmin;

    // ==========================
    //   ÁREA FINANCIERA
    // ==========================
    double totalFinancieroProd = productosFin.stream().mapToDouble(CuentaContable::getSaldo).sum();
    double totalFinancieroGto  = gastosFin.stream().mapToDouble(CuentaContable::getSaldo).sum();

    double totalFinanciero = totalFinancieroProd - totalFinancieroGto;

    // ==========================
    //   UTILIDAD DE OPERACIÓN
    // ==========================
    double utilidadOperacion =
            utilidadBruta - (gastosOperacion - totalFinanciero);

    // ==========================
    //   OTROS INGRESOS Y GASTOS
    // ==========================
    double totalOtrosGastos    = otrosGastos.stream().mapToDouble(CuentaContable::getSaldo).sum();
    double totalOtrosProductos = otrosProductos.stream().mapToDouble(CuentaContable::getSaldo).sum();

    
    
   // ==========================
    //   PÉRDIDA ENTRE OTROS
    // ==========================
    double perdidaEntreOtros = totalOtrosGastos - totalOtrosProductos;

    // ==========================
    //   UTILIDAD ANTES DE ISR Y PTU
    // ==========================
    double utilidadAntesISRPTU = utilidadOperacion - perdidaEntreOtros;

    // ==========================
    //   IMPUESTOS (PORCENTAJE FIJO)
    // ==========================
    double ISR = utilidadAntesISRPTU * 0.33;  // 30%
    double PTU = utilidadAntesISRPTU * 0.10;  // 10%

    // ==========================
    //   UTILIDAD NETA
    // ==========================
    double utilidadNeta = utilidadAntesISRPTU - ISR - PTU;


    // ============================================================
    //             FORMATO TIPO REPORTE (LISTA VERTICAL)
    // ============================================================
    if (tipoFormato.equalsIgnoreCase("Reporte")) {

        doc.add(new Paragraph("\nVENTAS", seccionFont));
        for (CuentaContable c : ventas)
            doc.add(new Paragraph(c.getNombre() + " ............ $" + c.getSaldo(), textoFont));
        for (CuentaContable c : deducciones)
            doc.add(new Paragraph(c.getNombre() + " ............ $" + c.getSaldo(), textoFont));

        doc.add(new Paragraph("Ventas netas ............ $" + ventasNetas, bold12));


        // COSTO DE VENTAS
        doc.add(new Paragraph("\nCOSTO DE VENTAS", seccionFont));
        doc.add(new Paragraph("Compras ............ $" + compras, textoFont));
        doc.add(new Paragraph("Gastos de compra ............ $" + gastosCompra, textoFont));
        doc.add(new Paragraph("Compras totales ............ $" + comprasTotales, textoFont));
        doc.add(new Paragraph("Devoluciones s/compras ............ $" + devCompra, textoFont));
        doc.add(new Paragraph("Descuentos s/compras ............ $" + descCompra, textoFont));
        doc.add(new Paragraph("Compras netas ............ $" + comprasNetas, textoFont));
        doc.add(new Paragraph("Inventario inicial ............ $" + invInicial, textoFont));
        doc.add(new Paragraph("Total mercancía ............ $" + totalMercancia, textoFont));
        doc.add(new Paragraph("Inventario final ............ $" + invFinal, textoFont));
        doc.add(new Paragraph("Costo de ventas ............ $" + costoVentasFinal, bold12));

        doc.add(new Paragraph("Utilidad bruta ............ $" + utilidadBruta, bold12));


        // GASTOS DE OPERACIÓN
        doc.add(new Paragraph("\nGASTOS DE OPERACIÓN", seccionFont));

        doc.add(new Paragraph("Gastos de venta", textoFont));
        for (CuentaContable c : gastosVenta)
            doc.add(new Paragraph(c.getNombre() + " ............ $" + c.getSaldo(), textoFont));
        doc.add(new Paragraph("Total gastos de venta ............ $" + totalGastosVenta, textoFont));

        doc.add(new Paragraph("\nGastos de administración", textoFont));
        for (CuentaContable c : gastosAdmin)
            doc.add(new Paragraph(c.getNombre() + " ............ $" + c.getSaldo(), textoFont));
        doc.add(new Paragraph("Total gastos de administración ............ $" + totalGastosAdmin, textoFont));

        doc.add(new Paragraph("Total gastos de operación ............ $" + gastosOperacion, bold12));


        // ÁREA FINANCIERA
        doc.add(new Paragraph("\nPRODUCTOS FINANCIEROS", seccionFont));
        for (CuentaContable c : productosFin)
            doc.add(new Paragraph(c.getNombre() + " ............ $" + c.getSaldo(), textoFont));
        doc.add(new Paragraph("Total productos financieros ............ $" + totalFinancieroProd, textoFont));

        doc.add(new Paragraph("\nGASTOS FINANCIEROS", seccionFont));
        for (CuentaContable c : gastosFin)
            doc.add(new Paragraph(c.getNombre() + " ............ $" + c.getSaldo(), textoFont));
        doc.add(new Paragraph("Total gastos financieros ............ $" + totalFinancieroGto, textoFont));

        doc.add(new Paragraph("Total financiero ............ $" + totalFinanciero, bold12));


        // UTILIDAD DE OPERACIÓN
        doc.add(new Paragraph("\nUtilidad de operación ............ $" + utilidadOperacion, bold14));


        // OTROS GASTOS
        doc.add(new Paragraph("\nOTROS GASTOS", seccionFont));
        for (CuentaContable c : otrosGastos)
            doc.add(new Paragraph(c.getNombre() + " ............ $" + c.getSaldo(), textoFont));
        doc.add(new Paragraph("Total otros gastos ............ $" + totalOtrosGastos, textoFont));

        // OTROS PRODUCTOS
        doc.add(new Paragraph("\nOTROS PRODUCTOS", seccionFont));
        for (CuentaContable c : otrosProductos)
            doc.add(new Paragraph(c.getNombre() + " ............ $" + c.getSaldo(), textoFont));
        doc.add(new Paragraph("Total otros productos ............ $" + totalOtrosProductos, textoFont));


        doc.add(new Paragraph("Pérdida entre otros ............ $" + perdidaEntreOtros, bold12));

        doc.add(new Paragraph("Utilidad antes de ISR y PTU ............ $" + utilidadAntesISRPTU, bold14));

        // IMPUESTOS
        doc.add(new Paragraph("ISR (33%) ............ $" + ISR, textoFont));
        doc.add(new Paragraph("PTU (10%) ............ $" + PTU, textoFont));

        doc.add(new Paragraph("\nUTILIDAD NETA ............ $" + utilidadNeta, bold16));
        return;
    }


    // ============================================================
    //                  FORMATO TIPO CUENTA (2 COLUMNAS)
    // ============================================================

    PdfPTable tabla = new PdfPTable(2);
    tabla.setWidthPercentage(100);
    tabla.setWidths(new float[]{1, 1});

    PdfPCell izq = new PdfPCell();
    izq.setBorder(Rectangle.NO_BORDER);
    PdfPCell der = new PdfPCell();
    der.setBorder(Rectangle.NO_BORDER);

    // ----------- COLUMNA IZQUIERDA: DEBE -----------
    
     for (CuentaContable c : deducciones)
        izq.addElement(new Paragraph(c.getCodigo() + " " + c.getNombre() + "  $" + c.getSaldo(), textoFont));
    izq.addElement(new Paragraph("Compras: $" + compras, textoFont));
    izq.addElement(new Paragraph("Gastos de compra: $" + gastosCompra, textoFont));
    izq.addElement(new Paragraph("Compras totales: $" + comprasTotales, textoFont));
    
    izq.addElement(new Paragraph("Compras netas: $" + comprasNetas, textoFont));
    izq.addElement(new Paragraph("Inventario inicial: $" + invInicial, textoFont));
    izq.addElement(new Paragraph("Total mercancía: $" + totalMercancia, textoFont));
    
    izq.addElement(new Paragraph("Costo de ventas: $" + costoVentasFinal, bold12));

    izq.addElement(new Paragraph("Utilidad bruta: $" + utilidadBruta, bold12));
    
     // GASTOS DE OPERACIÓN
    izq.addElement(new Paragraph("\nGASTOS DE OPERACIÓN", seccionFont));

    izq.addElement(new Paragraph("Gastos de venta:", textoFont));
    for (CuentaContable c : gastosVenta)
        izq.addElement(new Paragraph(c.getCodigo() + " " + c.getNombre() + " $" + c.getSaldo(), textoFont));
    izq.addElement(new Paragraph("Total gastos de venta: $" + totalGastosVenta, textoFont));

    izq.addElement(new Paragraph("\nGastos de administración:", textoFont));
    for (CuentaContable c : gastosAdmin)
        izq.addElement(new Paragraph(c.getCodigo() + " " + c.getNombre() + " $" + c.getSaldo(), textoFont));
    izq.addElement(new Paragraph("Total gastos de administración: $" + totalGastosAdmin, textoFont));

    izq.addElement(new Paragraph("Total gastos de operación: $" + gastosOperacion, bold12));
    
   izq.addElement(new Paragraph("\nGASTOS FINANCIEROS", seccionFont));
    for (CuentaContable c : gastosFin)
        izq.addElement(new Paragraph(c.getCodigo() + " " + c.getNombre() + " $" + c.getSaldo(), textoFont));
    izq.addElement(new Paragraph("Total gastos financieros: $" + totalFinancieroGto, textoFont));

    // OTROS GASTOS
    izq.addElement(new Paragraph("\nOTROS GASTOS", seccionFont));
    for (CuentaContable c : otrosGastos)
        izq.addElement(new Paragraph(c.getCodigo() + " " + c.getNombre() + " $" + c.getSaldo(), textoFont));
    izq.addElement(new Paragraph("Total otros gastos: $" + totalOtrosGastos, textoFont));
    
    izq.addElement(new Paragraph("ISR (33%): $" + ISR, textoFont));
    izq.addElement(new Paragraph("PTU (10%): $" + PTU, textoFont));

    // ----------- COLUMNA DERECHA: HABER -----------
    
     der.addElement(new Paragraph("VENTAS", seccionFont));
    for (CuentaContable c : ventas)
        der.addElement(new Paragraph(c.getCodigo() + " " + c.getNombre() + "  $" + c.getSaldo(), textoFont));
   
    der.addElement(new Paragraph("Ventas netas: $" + ventasNetas, bold12));
    
    der.addElement(new Paragraph("Devoluciones sobre compras: $" + devCompra, textoFont));
    der.addElement(new Paragraph("Descuentos sobre compras: $" + descCompra, textoFont));
    
    der.addElement(new Paragraph("Inventario final: $" + invFinal, textoFont));

    
    
    
    
    
   


   


    // ÁREA FINANCIERA
    der.addElement(new Paragraph("\nPRODUCTOS FINANCIEROS", seccionFont));
    for (CuentaContable c : productosFin)
        der.addElement(new Paragraph(c.getCodigo() + " " + c.getNombre() + " $" + c.getSaldo(), textoFont));
    der.addElement(new Paragraph("Total productos financieros: $" + totalFinancieroProd, textoFont));

    

    der.addElement(new Paragraph("Total financiero: $" + totalFinanciero, bold12));


    // UTILIDAD DE OPERACIÓN
    der.addElement(new Paragraph("\nUtilidad de operación: $" + utilidadOperacion, bold14));

    

    // OTROS PRODUCTOS
    der.addElement(new Paragraph("\nOTROS PRODUCTOS", seccionFont));
    for (CuentaContable c : otrosProductos)
        der.addElement(new Paragraph(c.getCodigo() + " " + c.getNombre() + " $" + c.getSaldo(), textoFont));
    der.addElement(new Paragraph("Total otros productos: $" + totalOtrosProductos, textoFont));

   der.addElement(new Paragraph("Pérdida entre otros: $" + perdidaEntreOtros, bold12));

    der.addElement(new Paragraph("Utilidad antes ISR y PTU: $" + utilidadAntesISRPTU, bold14));

    

    der.addElement(new Paragraph("UTILIDAD NETA: $" + utilidadNeta, bold16));

    tabla.addCell(izq);
    tabla.addCell(der);

    doc.add(tabla);
}

    // ==============================
    //      FIRMAS AL FINAL
    // ==============================
    private static void agregarFirmas(Document doc, Font textoFont) throws DocumentException {
        doc.add(new Paragraph("\n\n\n"));

        PdfPTable tablaFirmas = new PdfPTable(2);
        tablaFirmas.setWidthPercentage(100);
        tablaFirmas.setWidths(new float[]{1, 1});

        PdfPCell celdaIzq = new PdfPCell();
        celdaIzq.setBorder(Rectangle.NO_BORDER);
        celdaIzq.setHorizontalAlignment(Element.ALIGN_CENTER);
        celdaIzq.addElement(new Paragraph("_____________________________", textoFont));
        celdaIzq.addElement(new Paragraph("Autorizado por", textoFont));

        PdfPCell celdaDer = new PdfPCell();
        celdaDer.setBorder(Rectangle.NO_BORDER);
        celdaDer.setHorizontalAlignment(Element.ALIGN_CENTER);
        celdaDer.addElement(new Paragraph("_____________________________", textoFont));
        celdaDer.addElement(new Paragraph("Elaborado por", textoFont));

        tablaFirmas.addCell(celdaIzq);
        tablaFirmas.addCell(celdaDer);

        doc.add(tablaFirmas);
    }
    
    
    
}
