package com.mycompany.estadosfinancieros;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;

public class GeneradorPDF {

    // Formateadores
    private static final DecimalFormat CON_MONEDA = new DecimalFormat("$#,##0.00;(-)$#,##0.00");
    private static final DecimalFormat SIN_MONEDA = new DecimalFormat("#,##0.00;(-)#,##0.00");

    private static final Font F_TITULO = new Font(Font.HELVETICA, 12, Font.BOLD);
    private static final Font F_SUBTITULO = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font F_NORMAL = new Font(Font.HELVETICA, 9);
    private static final Font F_NEGRITA = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font F_ITALICA = new Font(Font.HELVETICA, 9, Font.ITALIC);

    private static final Color GRIS_SECCION = new Color(180, 180, 180);
    private static final Color GRIS_GRUPO = new Color(215, 215, 215);
    private static final Color VERDE_TITULO = new Color(50, 205, 50);
    private static final Color VERDE_CUENTA = new Color(173, 255, 47);
    private static final Color VERDE_GASTOS = new Color(225, 255, 180);

    private static int contadorIndice = 1;

    public static void generarPDF(String empresa, String titulo, String periodo,
            List<CuentaContable> cuentas, String tipoFormato, String nombreArchivo,
            String elaboro, String autorizo) throws Exception {

        boolean esFormatoCuenta = tipoFormato != null && tipoFormato.equalsIgnoreCase("Cuenta");
        Rectangle tamanoPagina = esFormatoCuenta ? PageSize.A4.rotate() : PageSize.A4;

        Document doc = new Document(tamanoPagina, 30, 30, 40, 40);
        PdfWriter.getInstance(doc, new FileOutputStream(nombreArchivo));
        doc.open();

        if (titulo.toLowerCase().contains("balance")) {
            agregarEncabezadoBalance(doc, empresa, titulo, periodo);
            if (esFormatoCuenta) {
                generarBalanceCuenta(doc, cuentas);
            } else {
                generarBalanceReporte(doc, cuentas);
            }
        } else {
            generarEstadoResultados(doc, empresa, periodo, cuentas);
        }

        agregarFirmas(doc, elaboro, autorizo);
        doc.close();
    }

    private static void agregarEncabezadoBalance(Document doc, String empresa,
            String titulo, String periodo) throws DocumentException {
        Paragraph pEmpresa = new Paragraph(empresa, new Font(Font.HELVETICA, 14, Font.BOLD));
        pEmpresa.setAlignment(Element.ALIGN_CENTER);
        doc.add(pEmpresa);

        String linea2 = titulo;
        if (periodo != null && !periodo.isEmpty()) {
            linea2 += ", " + periodo;
        }
        Paragraph pLinea2 = new Paragraph(linea2, new Font(Font.HELVETICA, 11, Font.BOLD));
        pLinea2.setAlignment(Element.ALIGN_CENTER);
        pLinea2.setSpacingAfter(10);
        doc.add(pLinea2);
    }

    // ========================================================================
    //  ESTADO DE RESULTADOS
    // ========================================================================
    private static void generarEstadoResultados(Document doc, String empresa, String periodo, List<CuentaContable> cuentas) throws DocumentException {
        PdfPTable tabla = new PdfPTable(5);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{40, 15, 15, 15, 15});

        // Encabezado
        PdfPCell cEmpresa = new PdfPCell(new Phrase(empresa, F_TITULO));
        cEmpresa.setColspan(5);
        cEmpresa.setHorizontalAlignment(Element.ALIGN_CENTER);
        cEmpresa.setBackgroundColor(VERDE_TITULO);
        cEmpresa.setBorder(Rectangle.BOX);
        tabla.addCell(cEmpresa);

        String textoPeriodo = "Estado de resultado, " + (periodo != null ? periodo : "");
        PdfPCell cPeriodo = new PdfPCell(new Phrase(textoPeriodo, F_SUBTITULO));
        cPeriodo.setColspan(5);
        cPeriodo.setHorizontalAlignment(Element.ALIGN_CENTER);
        cPeriodo.setBorder(Rectangle.BOX);
        cPeriodo.setBorderWidthBottom(2.5f);
        tabla.addCell(cPeriodo);

        agregarCelda(tabla, "", F_NORMAL, false, false);
        for (int i = 1; i <= 4; i++) {
            PdfPCell c = new PdfPCell(new Phrase(String.valueOf(i), F_NORMAL));
            c.setHorizontalAlignment(Element.ALIGN_RIGHT);
            c.setBorder(Rectangle.BOX);
            tabla.addCell(c);
        }

        // --- 1. VENTAS ---
        double ventasTotales = obtenerSaldo(cuentas, "Ventas totales");
        double devVentas = obtenerSaldo(cuentas, "Devoluciones sobre ventas");
        double descVentas = obtenerSaldo(cuentas, "Descuentos sobre ventas");
        double totalDeduccionesVentas = devVentas + descVentas;
        double ventasNetas = ventasTotales - totalDeduccionesVentas;

        agregarFilaER(tabla, "Ventas totales", ventasTotales, 3, VERDE_CUENTA, false, true);

        if (devVentas > 0 && descVentas > 0) {
            agregarFilaER(tabla, "Devoluciones sobre ventas", devVentas, 2, VERDE_CUENTA, false, true);
            agregarFilaDobleER(tabla, "Descuentos sobre ventas", descVentas, 2, true, false,
                    totalDeduccionesVentas, 3, true, false, VERDE_CUENTA);
        } else if (devVentas > 0) {
            agregarFilaER(tabla, "Devoluciones sobre ventas", devVentas, 3, VERDE_CUENTA, true, true);
        } else if (descVentas > 0) {
            agregarFilaER(tabla, "Descuentos sobre ventas", descVentas, 3, VERDE_CUENTA, true, true);
        }

        agregarFilaER_Resultado(tabla, "Ventas netas", ventasNetas, 4, false, false, true);

        // --- 2. COSTOS ---
        double invInicial = obtenerSaldo(cuentas, "Inventario inicial");
        double compras = obtenerSaldo(cuentas, "Compras");
        if (compras == 0) {
            compras = obtenerSaldo(cuentas, "Compras");
        }
        double gastosCompra = obtenerSaldo(cuentas, "Gastos de compra");
        double devCompras = obtenerSaldo(cuentas, "Devoluciones sobre compras");
        double descCompras = obtenerSaldo(cuentas, "Descuentos sobre compras");

        double comprasTotales = compras + gastosCompra;
        double totalDeduccionesCompras = devCompras + descCompras;
        double comprasNetas = comprasTotales - totalDeduccionesCompras;
        double sumaMercancias = invInicial + comprasNetas;
        double invFinal = obtenerSaldo(cuentas, "Inventario final");
        double costoVentas = sumaMercancias - invFinal;

        agregarFilaER(tabla, "Inventario inicial", invInicial, 3, VERDE_CUENTA, false, true);

        if (gastosCompra > 0) {
            agregarFilaER(tabla, "Compras", compras, 1, VERDE_CUENTA, false, true);
            agregarFilaER(tabla, "Gastos de compra", gastosCompra, 1, VERDE_CUENTA, true, false);
            agregarFilaER(tabla, "Compras totales", comprasTotales, 2, Color.WHITE, false, true);
        } else {
            if (totalDeduccionesCompras > 0) {
                agregarFilaER(tabla, "Compras totales", compras, 2, Color.WHITE, false, true);
            }
        }

        if (totalDeduccionesCompras > 0) {
            if (devCompras > 0 && descCompras > 0) {
                agregarFilaER(tabla, "Devoluciones sobre compras", devCompras, 1, VERDE_CUENTA, false, true);
                agregarFilaDobleER(tabla, "Descuentos sobre compras", descCompras, 1, true, false,
                        totalDeduccionesCompras, 2, true, false, VERDE_CUENTA);
            } else if (devCompras > 0) {
                agregarFilaER(tabla, "Devoluciones sobre compras", devCompras, 2, VERDE_CUENTA, true, true);
            } else if (descCompras > 0) {
                agregarFilaER(tabla, "Descuentos sobre compras", descCompras, 2, VERDE_CUENTA, true, true);
            }
            agregarFilaER(tabla, "Compras netas", comprasNetas, 3, Color.WHITE, true, false);
        } else {
            if (gastosCompra > 0) {
                agregarFilaER(tabla, "Compras netas", comprasNetas, 3, Color.WHITE, true, false);
            } else {
                agregarFilaER(tabla, "Compras netas", compras, 3, Color.WHITE, true, true);
            }
        }

        agregarFilaER(tabla, "Suma o total de mercancías", sumaMercancias, 3, Color.WHITE, false, true);
        agregarFilaER(tabla, "Inventario final", invFinal, 3, VERDE_CUENTA, true, false);

        agregarFilaER_Resultado(tabla, "Costo de lo vendido", costoVentas, 4, false, true, true);

        double utilidadBruta = ventasNetas - costoVentas;
        String lblUtilidadBruta = utilidadBruta >= 0 ? "Utilidad bruta" : "Pérdida bruta";
        agregarFilaER_Resultado(tabla, lblUtilidadBruta, utilidadBruta, 4, false, false, true);

        // --- 3. GASTOS OPERACIÓN ---
        agregarFilaTituloSimple(tabla, "Gastos de operación", true);

        double tGastosVenta = obtenerSumaGrupo(cuentas, "Gastos de venta");
        double tGastosAdmin = obtenerSumaGrupo(cuentas, "Gastos de administración");
        double tGastosOp = tGastosVenta + tGastosAdmin;

        boolean ventaPrimero = tGastosVenta >= tGastosAdmin;
        String g1 = ventaPrimero ? "Gastos de venta" : "Gastos de administración";
        String g2 = ventaPrimero ? "Gastos de administración" : "Gastos de venta";

        agregarFilaTituloSimpleCursiva(tabla, "   " + g1);
        procesarGrupoDinamico(tabla, cuentas, g1, 1, 2, VERDE_GASTOS, 0, 0, false, true);

        agregarFilaTituloSimpleCursiva(tabla, "   " + g2);
        procesarListaER_ConExtra(tabla, cuentas, g2, 1, VERDE_GASTOS,
                tGastosOp, 3, true, false, false, true, false);

        // --- 4. FINANCIEROS ---
        double tProdFin = obtenerSumaGrupo(cuentas, "Productos financieros");
        double tGastFin = obtenerSumaGrupo(cuentas, "Gastos financieros");
        double resFin = tProdFin - tGastFin;
        double netoGastosYFinancieros = tGastosOp - resFin;

        boolean prodPrimero = tProdFin >= tGastFin;
        String f1 = prodPrimero ? "Productos financieros" : "Gastos financieros";
        String f2 = prodPrimero ? "Gastos financieros" : "Productos financieros";

        if (obtenerSumaGrupo(cuentas, f1) > 0) {
            agregarFilaTituloSimpleCursiva(tabla, "   " + f1);
            procesarGrupoDinamico(tabla, cuentas, f1, 1, 2, VERDE_GASTOS, 0, 0, false, false);
        }

        if (obtenerSumaGrupo(cuentas, f2) > 0) {
            agregarFilaTituloSimpleCursiva(tabla, "   " + f2);
            if (tProdFin == 0) {
                procesarListaER_ConExtra(tabla, cuentas, f2, 2, VERDE_GASTOS,
                        netoGastosYFinancieros, 4, true, true, false, true, true);
            } else {
                procesarListaER_FinancierosComplejo(tabla, cuentas, f2, 1, VERDE_GASTOS,
                        Math.abs(resFin), 3, netoGastosYFinancieros, 4, true);
            }
        }

        double utilidadOperacion = utilidadBruta - netoGastosYFinancieros;
        String lblUtilidadOp = utilidadOperacion >= 0 ? "Utilidad de operación" : "Pérdida de operación";
        agregarFilaER_ResultadoCentrado(tabla, lblUtilidadOp, utilidadOperacion, 4, true, false, true);

        // --- 5. OTROS ---
        double tOtrosGastos = obtenerSumaGrupo(cuentas, "Otros gastos");
        double tOtrosProd = obtenerSumaGrupo(cuentas, "Otros productos");

        double otrosNeto = tOtrosProd - tOtrosGastos;

        boolean prodOtrosPrimero = tOtrosProd >= tOtrosGastos;
        String o1 = prodOtrosPrimero ? "Otros productos" : "Otros gastos";
        String o2 = prodOtrosPrimero ? "Otros gastos" : "Otros productos";

        if (obtenerSumaGrupo(cuentas, o1) > 0) {
            agregarFilaTituloSimpleCursiva(tabla, "   " + o1);
            procesarGrupoDinamico(tabla, cuentas, o1, 2, 3, VERDE_GASTOS, 0, 0, false, true);
        }
        if (obtenerSumaGrupo(cuentas, o2) > 0) {
            agregarFilaTituloSimpleCursiva(tabla, "   " + o2);
            procesarGrupoDinamico(tabla, cuentas, o2, 2, 3, VERDE_GASTOS, 0, 0, false, true);
        }

        if (otrosNeto != 0) {
            String lblOtros = otrosNeto < 0 ? "Pérdida entre otros gastos y productos" : "Utilidad entre otros gastos y productos";
            agregarFilaTextoCentradoCursivaMonto(tabla, lblOtros, otrosNeto, 4, Color.WHITE, true, false);
        }

        // LÓGICA ARITMÉTICA CORREGIDA
        double utilidadAntesImp;
        if (otrosNeto < 0) {
            // Pérdida Otros: Se suma algebraicamente (aumenta pérdida o reduce utilidad)
            utilidadAntesImp = utilidadOperacion + otrosNeto; // Corrección de variable
        } else {
            // Utilidad Otros: Se resta de la Op (según tu lógica específica)
            utilidadAntesImp = utilidadOperacion - otrosNeto;
        }

        if (utilidadAntesImp < 0) {
            PdfPCell cTxt = new PdfPCell(new Phrase("PÉRDIDA NETA DEL EJERCICIO", F_NORMAL));
            cTxt.setBorder(Rectangle.BOX);
            tabla.addCell(cTxt);
            agregarCeldaVaciaConBorde(tabla);
            agregarCeldaVaciaConBorde(tabla);
            agregarCeldaVaciaConBorde(tabla);

            PdfPCell cFinal = new PdfPCell(new Phrase(formatearMoneda(utilidadAntesImp, true), F_NEGRITA));
            cFinal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cFinal.setBorder(Rectangle.BOX);
            cFinal.setBorderWidthBottom(2.5f);
            tabla.addCell(cFinal);

        } else {
            agregarFilaER_Resultado(tabla, "Utilidad antes de ISR y PTU", utilidadAntesImp, 4, false, false, true);

            double isr = utilidadAntesImp * 0.33;
            double ptu = utilidadAntesImp * 0.10;
            double totalImpuestos = isr + ptu;

            agregarFilaER(tabla, "Impuestos sobre la renta ISR", isr, 3, Color.WHITE, false, true);
            agregarFilaDobleER(tabla, "Participación de los trabajadores en las utilidades", ptu, 3, true, false,
                    totalImpuestos, 4, true, false, Color.WHITE);

            double utNeta = utilidadAntesImp - totalImpuestos;

            PdfPCell cTxt = new PdfPCell(new Phrase("UTILIDAD NETA DEL EJERCICIO", F_NORMAL));
            cTxt.setBorder(Rectangle.BOX);
            tabla.addCell(cTxt);
            agregarCeldaVaciaConBorde(tabla);
            agregarCeldaVaciaConBorde(tabla);
            agregarCeldaVaciaConBorde(tabla);

            PdfPCell cFinal = new PdfPCell(new Phrase(formatearMoneda(utNeta, true), F_NEGRITA));
            cFinal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cFinal.setBorder(Rectangle.BOX);
            cFinal.setBorderWidthBottom(2.5f);
            tabla.addCell(cFinal);
        }

        doc.add(tabla);
    }

    // --- AUXILIARES ---
    private static String formatearMoneda(double valor, boolean llevaSigno) {
        boolean esNegativo = valor < 0;
        double absValor = Math.abs(valor);
        String formateado = llevaSigno ? CON_MONEDA.format(absValor) : SIN_MONEDA.format(absValor);
        if (esNegativo) {
            return "(-)" + formateado;
        }
        return formateado;
    }

    private static double procesarGrupoDinamico(PdfPTable tabla, List<CuentaContable> cuentas, String grupo,
            int colDetalle, int colSuma, Color colorFondo,
            double montoExtra, int colExtra, boolean imprimirExtra, boolean bordeSuma) {
        List<CuentaContable> filtradas = cuentas.stream()
                .filter(c -> c.getGrupo().equalsIgnoreCase(grupo))
                .collect(Collectors.toList());
        double suma = 0;

        if (filtradas.isEmpty()) {
            return 0.0;
        }

        if (filtradas.size() == 1) {
            CuentaContable c = filtradas.get(0);
            suma = c.getSaldo();
            if (imprimirExtra) {
                PdfPCell cTxt = new PdfPCell(new Phrase(" " + c.getNombre(), F_NORMAL));
                cTxt.setBorder(Rectangle.BOX);
                cTxt.setBackgroundColor(colorFondo);
                tabla.addCell(cTxt);
                for (int j = 1; j <= 4; j++) {
                    if (j == colSuma) {
                        agregarCeldaMontoER(tabla, suma, bordeSuma, true);
                    } else if (j == colExtra) {
                        agregarCeldaMontoER(tabla, montoExtra, true, true);
                    } else {
                        agregarCeldaVaciaConBorde(tabla);
                    }
                }
            } else {
                agregarFilaER(tabla, c.getNombre(), suma, colSuma, colorFondo, bordeSuma, true);
            }
        } else {
            for (int i = 0; i < filtradas.size(); i++) {
                CuentaContable c = filtradas.get(i);
                suma += c.getSaldo();
                boolean esUltimo = (i == filtradas.size() - 1);
                boolean esPrimero = (i == 0);

                if (esUltimo) {
                    PdfPCell cTxt = new PdfPCell(new Phrase(" " + c.getNombre(), F_NORMAL));
                    cTxt.setBorder(Rectangle.BOX);
                    cTxt.setBackgroundColor(colorFondo);
                    tabla.addCell(cTxt);
                    for (int j = 1; j <= 4; j++) {
                        if (j == colDetalle) {
                            agregarCeldaMontoER(tabla, c.getSaldo(), true, false);
                        } else if (j == colSuma) {
                            agregarCeldaMontoER(tabla, suma, bordeSuma, false);
                        } else if (imprimirExtra && j == colExtra) {
                            agregarCeldaMontoER(tabla, montoExtra, false, true);
                        } else {
                            agregarCeldaVaciaConBorde(tabla);
                        }
                    }
                } else {
                    agregarFilaER(tabla, c.getNombre(), c.getSaldo(), colDetalle, colorFondo, false, esPrimero);
                }
            }
        }
        return suma;
    }

    private static double procesarListaER_ConExtra(PdfPTable tabla, List<CuentaContable> cuentas, String grupo,
            int colMonto, Color colorFondo,
            double montoExtra, int colExtra, boolean imprimirExtra,
            boolean bordeSuma, boolean bordeExtra,
            boolean signoEnPrimerElemento, boolean signoEnTotalGrupo) {
        List<CuentaContable> filtradas = cuentas.stream()
                .filter(c -> c.getGrupo().equalsIgnoreCase(grupo))
                .collect(Collectors.toList());
        double suma = 0;

        for (int i = 0; i < filtradas.size(); i++) {
            CuentaContable c = filtradas.get(i);
            suma += c.getSaldo();
            boolean esUltimo = (i == filtradas.size() - 1);
            boolean esPrimero = (i == 0);
            boolean ponerSignoIndividual = (esPrimero && signoEnPrimerElemento);

            if (esUltimo) {
                PdfPCell cTxt = new PdfPCell(new Phrase(" " + c.getNombre(), F_NORMAL));
                cTxt.setBorder(Rectangle.BOX);
                cTxt.setBackgroundColor(colorFondo);
                tabla.addCell(cTxt);
                for (int j = 1; j <= 4; j++) {
                    if (j == colMonto) {
                        agregarCeldaMontoER(tabla, c.getSaldo(), true, ponerSignoIndividual);
                    } else if (j == colMonto + 1) {
                        agregarCeldaMontoER(tabla, suma, bordeSuma, signoEnTotalGrupo);
                    } else if (imprimirExtra && j == colExtra) {
                        agregarCeldaMontoER(tabla, montoExtra, bordeExtra, true);
                    } else {
                        agregarCeldaVaciaConBorde(tabla);
                    }
                }
            } else {
                agregarFilaER(tabla, c.getNombre(), c.getSaldo(), colMonto, colorFondo, false, ponerSignoIndividual);
            }
        }
        return suma;
    }

    private static void procesarListaER_FinancierosComplejo(PdfPTable tabla, List<CuentaContable> cuentas, String grupo,
            int colMonto, Color colorFondo,
            double montoCol3, int col3,
            double montoCol4, int col4,
            boolean signoEnPrimero) {
        List<CuentaContable> filtradas = cuentas.stream().filter(c -> c.getGrupo().equalsIgnoreCase(grupo)).collect(Collectors.toList());
        double suma = 0;

        if (filtradas.size() == 1) {
            CuentaContable c = filtradas.get(0);
            suma = c.getSaldo();
            PdfPCell cTxt = new PdfPCell(new Phrase(" " + c.getNombre(), F_NORMAL));
            cTxt.setBorder(Rectangle.BOX);
            cTxt.setBackgroundColor(colorFondo);
            tabla.addCell(cTxt);
            agregarCeldaVaciaConBorde(tabla); // Col 1 vacia
            agregarCeldaMontoER(tabla, suma, false, true); // Borde FALSE
            agregarCeldaMontoER(tabla, montoCol3, true, false);
            agregarCeldaMontoER(tabla, montoCol4, true, false);
        } else {
            for (int i = 0; i < filtradas.size(); i++) {
                CuentaContable c = filtradas.get(i);
                suma += c.getSaldo();
                boolean esUltimo = (i == filtradas.size() - 1);
                boolean esPrimero = (i == 0);
                if (esUltimo) {
                    PdfPCell cTxt = new PdfPCell(new Phrase(" " + c.getNombre(), F_NORMAL));
                    cTxt.setBorder(Rectangle.BOX);
                    cTxt.setBackgroundColor(colorFondo);
                    tabla.addCell(cTxt);
                    agregarCeldaMontoER(tabla, c.getSaldo(), true, false); // Col 1
                    agregarCeldaMontoER(tabla, suma, false, false); // Borde FALSE
                    agregarCeldaMontoER(tabla, montoCol3, true, false); // Col 3
                    agregarCeldaMontoER(tabla, montoCol4, true, false); // Col 4
                } else {
                    agregarFilaER(tabla, c.getNombre(), c.getSaldo(), colMonto, colorFondo, false, (esPrimero && signoEnPrimero));
                }
            }
        }
    }

    private static double obtenerSumaGrupo(List<CuentaContable> cuentas, String grupo) {
        return cuentas.stream()
                .filter(c -> c.getGrupo().equalsIgnoreCase(grupo))
                .mapToDouble(CuentaContable::getSaldo).sum();
    }

    private static void agregarFilaER(PdfPTable tabla, String concepto, double monto, int colMonto, Color color, boolean lineaCorte, boolean llevaSigno) {
        if (colMonto == -1) {
            PdfPCell cTxt = new PdfPCell(new Phrase(" " + concepto, F_NORMAL));
            cTxt.setBorder(Rectangle.BOX);
            cTxt.setBackgroundColor(color);
            tabla.addCell(cTxt);
            for (int i = 0; i < 4; i++) {
                agregarCeldaVaciaConBorde(tabla);
            }
            return;
        }
        PdfPCell cTxt = new PdfPCell(new Phrase(" " + concepto, F_NORMAL));
        cTxt.setBorder(Rectangle.BOX);
        cTxt.setBackgroundColor(color);
        tabla.addCell(cTxt);
        for (int i = 1; i <= 4; i++) {
            if (i == colMonto) {
                agregarCeldaMontoER(tabla, monto, lineaCorte, llevaSigno);
            } else {
                agregarCeldaVaciaConBorde(tabla);
            }
        }
    }

    private static void agregarFilaDobleER(PdfPTable tabla, String concepto, double m1, int c1, boolean l1, boolean s1,
            double m2, int c2, boolean l2, boolean s2, Color color) {
        PdfPCell cTxt = new PdfPCell(new Phrase(" " + concepto, F_NORMAL));
        cTxt.setBorder(Rectangle.BOX);
        cTxt.setBackgroundColor(color);
        tabla.addCell(cTxt);
        for (int i = 1; i <= 4; i++) {
            if (i == c1) {
                agregarCeldaMontoER(tabla, m1, l1, s1);
            } else if (i == c2) {
                agregarCeldaMontoER(tabla, m2, l2, s2);
            } else {
                agregarCeldaVaciaConBorde(tabla);
            }
        }
    }

    private static void agregarFilaER_Resultado(PdfPTable tabla, String concepto, double monto, int colMonto, boolean negrita, boolean bordeGrueso, boolean llevaSigno) {
        Font fuente = negrita ? F_NEGRITA : F_NORMAL;
        PdfPCell cTxt = new PdfPCell(new Phrase(" " + concepto, fuente));
        cTxt.setBorder(Rectangle.BOX);
        tabla.addCell(cTxt);
        for (int i = 1; i <= 4; i++) {
            if (i == colMonto) {
                agregarCeldaMontoER(tabla, monto, bordeGrueso, llevaSigno);
            } else {
                agregarCeldaVaciaConBorde(tabla);
            }
        }
    }

    private static void agregarFilaTituloSimple(PdfPTable tabla, String titulo, boolean negrita) {
        Font fuente = negrita ? F_NEGRITA : F_NORMAL;
        PdfPCell c = new PdfPCell(new Phrase(titulo, fuente));
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        c.setBorder(Rectangle.BOX);
        tabla.addCell(c);
        for (int i = 0; i < 4; i++) {
            agregarCeldaVaciaConBorde(tabla);
        }
    }

    private static void agregarFilaER_ResultadoCentrado(PdfPTable tabla, String concepto, double monto, int colMonto, boolean negrita, boolean bordeGrueso, boolean llevaSigno) {
        Font fuente = negrita ? F_NEGRITA : F_NORMAL;
        PdfPCell cTxt = new PdfPCell(new Phrase(concepto, fuente));
        cTxt.setHorizontalAlignment(Element.ALIGN_CENTER);
        cTxt.setBorder(Rectangle.BOX);
        tabla.addCell(cTxt);
        for (int i = 1; i <= 4; i++) {
            if (i == colMonto) {
                agregarCeldaMontoER(tabla, monto, bordeGrueso, llevaSigno);
            } else {
                agregarCeldaVaciaConBorde(tabla);
            }
        }
    }

    private static void agregarFilaTextoCentradoCursivaMonto(PdfPTable tabla, String concepto, double monto, int colMonto, Color color, boolean bordeGrueso, boolean llevaSigno) {
        PdfPCell cTxt = new PdfPCell(new Phrase(concepto, F_ITALICA));
        cTxt.setHorizontalAlignment(Element.ALIGN_CENTER);
        cTxt.setBorder(Rectangle.BOX);
        cTxt.setBackgroundColor(color);
        tabla.addCell(cTxt);
        for (int i = 1; i <= 4; i++) {
            if (i == colMonto) {
                agregarCeldaMontoER(tabla, monto, bordeGrueso, llevaSigno);
            } else {
                agregarCeldaVaciaConBorde(tabla);
            }
        }
    }

    private static void agregarFilaTituloSimpleCursiva(PdfPTable tabla, String titulo) {
        PdfPCell c = new PdfPCell(new Phrase(titulo, F_ITALICA));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setBorder(Rectangle.BOX);
        tabla.addCell(c);
        for (int i = 0; i < 4; i++) {
            agregarCeldaVaciaConBorde(tabla);
        }
    }

    private static void agregarFilaTituloCentradoCursiva(PdfPTable tabla, String titulo) {
        PdfPCell c = new PdfPCell(new Phrase(titulo, F_ITALICA));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setBorder(Rectangle.BOX);
        tabla.addCell(c);
        for (int i = 0; i < 4; i++) {
            agregarCeldaVaciaConBorde(tabla);
        }
    }

    private static void agregarFilaMontoFlotante(PdfPTable tabla, double monto, int col, boolean lineaCorte) {
        agregarCeldaVaciaConBorde(tabla);
        for (int i = 1; i <= 4; i++) {
            if (i == col) {
                agregarCeldaMontoER(tabla, monto, lineaCorte, true);
            } else {
                agregarCeldaVaciaConBorde(tabla);
            }
        }
    }

    private static void agregarCeldaMontoER(PdfPTable tabla, double m, boolean l, boolean llevaSigno) {
        String texto = formatearMoneda(m, llevaSigno);
        PdfPCell c = new PdfPCell(new Phrase(texto, F_NORMAL));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setBorder(Rectangle.BOX);
        if (l) {
            c.setBorderWidthBottom(1.5f);
        }
        tabla.addCell(c);
    }

    private static void agregarCeldaVaciaConBorde(PdfPTable tabla) {
        PdfPCell c = new PdfPCell(new Phrase(" "));
        c.setBorder(Rectangle.BOX);
        tabla.addCell(c);
    }

    private static double obtenerSaldo(List<CuentaContable> cuentas, String nombreExacto) {
        return cuentas.stream().filter(c -> c.getNombre().equalsIgnoreCase(nombreExacto)).mapToDouble(CuentaContable::getSaldo).findFirst().orElse(0.0);
    }

    // ========================================================================
    //  BALANCE GENERAL (INTACTO)
    // ========================================================================
    private static void generarBalanceCuenta(Document doc, List<CuentaContable> cuentas) throws DocumentException {
        contadorIndice = 1;
        PdfPTable tablaPrincipal = new PdfPTable(2);
        tablaPrincipal.setWidthPercentage(100);
        tablaPrincipal.setWidths(new float[]{49, 49});
        tablaPrincipal.setSpacingBefore(10f);
        List<CuentaContable> activos = filtrarPorTipo(cuentas, "Activo");
        List<CuentaContable> pasivos = filtrarPorTipo(cuentas, "Pasivo");
        List<CuentaContable> capital = filtrarPorTipo(cuentas, "Capital");
        PdfPTable tablaActivos = new PdfPTable(4);
        tablaActivos.setWidthPercentage(100);
        tablaActivos.setWidths(new float[]{4, 40, 28, 28});
        agregarFilaTituloSeccion(tablaActivos, "Activo", 4);
        double totalActivo = 0;
        totalActivo += procesarGrupoBalanceCuenta(tablaActivos, activos, "Circulante", "Circulante", 2, 3, false, true);
        totalActivo += procesarGrupoBalanceCuenta(tablaActivos, activos, "Fijo", "Fijo", 2, 3, false, false);
        totalActivo += procesarGrupoBalanceCuenta(tablaActivos, activos, "Diferido", "Cargos Diferidos", 2, 3, true, false);
        agregarFilaTotalCuenta(tablaActivos, "Total Activo", totalActivo, 3, false);
        PdfPCell celdaIzquierda = new PdfPCell(tablaActivos);
        celdaIzquierda.setBorder(Rectangle.NO_BORDER);
        celdaIzquierda.setPaddingRight(10f);
        tablaPrincipal.addCell(celdaIzquierda);
        PdfPTable tablaPasivoCapital = new PdfPTable(4);
        tablaPasivoCapital.setWidthPercentage(100);
        tablaPasivoCapital.setWidths(new float[]{4, 40, 28, 28});
        agregarFilaTituloSeccion(tablaPasivoCapital, "Pasivo", 4);
        double totalPasivo = 0;
        totalPasivo += procesarGrupoBalanceCuenta(tablaPasivoCapital, pasivos, "Circulante", "Circulante", 2, 3, false, true);
        totalPasivo += procesarGrupoBalanceCuenta(tablaPasivoCapital, pasivos, "Fijo", "Fijo", 2, 3, false, false);
        totalPasivo += procesarGrupoBalanceCuenta(tablaPasivoCapital, pasivos, "Diferido", "Créditos Diferidos", 2, 3, true, false);
        agregarFilaTotalCuenta(tablaPasivoCapital, "Total Pasivo", totalPasivo, 3, true);
        agregarFilaTituloSeccion(tablaPasivoCapital, "Capital Contable", 4);
        //procesarGrupoBalanceCuenta(tablaPasivoCapital, capital, "", "", 2, 3, true, true);

        double tc = procesarGrupoBalanceReporte(tablaPasivoCapital, capital, "Capital", "Capital Contable", 2, 3, false, true);

        double totalCapitalCalculado = totalActivo - totalPasivo;
        agregarFilaTotalCuenta(tablaPasivoCapital, "Capital Contable", tc, 3, true);
        agregarFilaGranTotalCuenta(tablaPasivoCapital, "Total Pasivo + Capital Contable", totalPasivo + tc);
        PdfPCell celdaDerecha = new PdfPCell(tablaPasivoCapital);
        celdaDerecha.setBorder(Rectangle.NO_BORDER);
        celdaDerecha.setPaddingLeft(10f);
        tablaPrincipal.addCell(celdaDerecha);
        doc.add(tablaPrincipal);
    }

    private static double procesarGrupoBalanceCuenta(PdfPTable tabla, List<CuentaContable> lista, String f, String t, int c1, int c2, boolean u, boolean p) {
        List<CuentaContable> sub = lista.stream().filter(c -> f.isEmpty() || (c.getGrupo() != null && c.getGrupo().toUpperCase().contains(f.toUpperCase()))).collect(Collectors.toList());
        if (sub.isEmpty()) {
            return 0.0;
        }
        String tit = (t != null && !t.isEmpty()) ? t : f;
        agregarFilaTituloGrupo(tabla, tit, 4);
        if (sub.size() == 1) {
            CuentaContable c = sub.get(0);
            agregarFilaCuentaCuenta(tabla, c.getNombre(), c.getSaldo(), c2, p, u);
            return c.getSaldo();
        } else {
            double s = 0;
            for (int i = 0; i < sub.size(); i++) {
                CuentaContable c = sub.get(i);
                s += c.getSaldo();
                agregarFilaCuentaCuenta(tabla, c.getNombre(), c.getSaldo(), c1, i == 0, i == sub.size() - 1);
            }
            agregarFilaSumaGrupoCuenta(tabla, s, c2, u, p);
            return s;
        }
    }

    private static void agregarFilaCuentaCuenta(PdfPTable t, String n, double m, int c, boolean s, boolean l) {
        PdfPCell idx = new PdfPCell(new Phrase(String.valueOf(contadorIndice++), F_NORMAL));
        idx.setBorder(Rectangle.BOX);
        t.addCell(idx);
        agregarCelda(t, " " + n, F_NORMAL, false, false);
        for (int i = 2; i <= 3; i++) {
            if (i == c) {
                agregarCeldaMonto(t, formatearMoneda(m, s), l);
            } else {
                agregarCelda(t, "", F_NORMAL, false, false);
            }
        }
    }

    private static void agregarFilaSumaGrupoCuenta(PdfPTable t, double m, int c, boolean l, boolean s) {
        agregarCelda(t, "", F_NORMAL, false, false);
        agregarCelda(t, "", F_NORMAL, false, false);
        for (int i = 2; i <= 3; i++) {
            if (i == c) {
                agregarCeldaMonto(t, formatearMoneda(m, s), l);
            } else {
                agregarCelda(t, "", F_NORMAL, false, false);
            }
        }
    }

    private static void agregarFilaTotalCuenta(PdfPTable t, String tit, double m, int c, boolean l) {
        agregarCelda(t, "", F_NORMAL, false, false);
        PdfPCell ct = new PdfPCell(new Phrase(tit, F_NEGRITA));
        ct.setHorizontalAlignment(Element.ALIGN_LEFT);
        ct.setBorder(Rectangle.BOX);
        t.addCell(ct);
        for (int i = 2; i <= 3; i++) {
            if (i == c) {
                agregarCeldaMonto(t, formatearMoneda(m, true), l);
            } else {
                agregarCelda(t, "", F_NORMAL, false, false);
            }
        }
    }

    private static void agregarFilaGranTotalCuenta(PdfPTable t, String tit, double m) {
        agregarCelda(t, "", F_NORMAL, false, false);
        PdfPCell ct = new PdfPCell(new Phrase(tit, F_NEGRITA));
        ct.setHorizontalAlignment(Element.ALIGN_LEFT);
        ct.setBorder(Rectangle.BOX);
        t.addCell(ct);
        agregarCelda(t, "", F_NORMAL, false, false);
        PdfPCell cm = new PdfPCell(new Phrase(formatearMoneda(m, true), F_NORMAL));
        cm.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cm.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cm.setBorder(Rectangle.BOX);
        t.addCell(cm);
    }

    private static void generarBalanceReporte(Document doc, List<CuentaContable> cuentas) throws DocumentException {
        contadorIndice = 1;
        PdfPTable tabla = new PdfPTable(5);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{4, 40, 18, 18, 18});
        List<CuentaContable> act = filtrarPorTipo(cuentas, "Activo");
        List<CuentaContable> pas = filtrarPorTipo(cuentas, "Pasivo");
        List<CuentaContable> cap = filtrarPorTipo(cuentas, "Capital");
        agregarFilaTituloSeccion(tabla, "Activo", 5);
        double ta = 0;
        ta += procesarGrupoBalanceReporte(tabla, act, "Circulante", "Circulante", 3, 4, false, true);
        ta += procesarGrupoBalanceReporte(tabla, act, "Fijo", "Fijo", 3, 4, false, false);
        ta += procesarGrupoBalanceReporte(tabla, act, "Diferido", "Cargos Diferidos", 3, 4, true, false);
        agregarFilaTotalReporte(tabla, "Total Activo", ta, 5, false);
        agregarFilaTituloSeccion(tabla, "Pasivo", 5);
        double tp = 0;
        tp += procesarGrupoBalanceReporte(tabla, pas, "Circulante", "Circulante", 4, 5, false, true);
        tp += procesarGrupoBalanceReporte(tabla, pas, "Fijo", "Fijo", 4, 5, false, false);
        tp += procesarGrupoBalanceReporte(tabla, pas, "Diferido", "Créditos Diferidos", 4, 5, true, false);
        agregarFilaTotalReporte(tabla, "Total Pasivo", tp, 5, true);
        agregarFilaTituloSeccion(tabla, "Capital Contable", 5);
        procesarGrupoBalanceReporte(tabla, cap, "", "", 4, 5, true, true);
        double tc = procesarGrupoBalanceReporte(tabla, cap, "Capital", "Capital Contable", 4, 5, false, true);
        //double tc = ta - tp; 
        agregarFilaTotalReporte(tabla, "Capital Contable", tc, 5, false);
        doc.add(tabla);
    }

    private static double procesarGrupoBalanceReporte(PdfPTable t, List<CuentaContable> l, String f, String tit, int c1, int c2, boolean u, boolean p) {
        List<CuentaContable> sub = l.stream().filter(c -> f.isEmpty() || (c.getGrupo() != null && c.getGrupo().toUpperCase().contains(f.toUpperCase()))).collect(Collectors.toList());
        if (sub.isEmpty()) {
            return 0.0;
        }
        String ti = (tit != null && !tit.isEmpty()) ? tit : f;
        agregarFilaTituloGrupo(t, ti, 5);
        if (sub.size() == 1) {
            CuentaContable c = sub.get(0);
            agregarFilaCuentaReporte(t, c.getNombre(), c.getSaldo(), c2, p, u);
            return c.getSaldo();
        } else {
            double s = 0;
            for (int i = 0; i < sub.size(); i++) {
                CuentaContable c = sub.get(i);
                s += c.getSaldo();
                agregarFilaCuentaReporte(t, c.getNombre(), c.getSaldo(), c1, i == 0, i == sub.size() - 1);
            }
            agregarFilaSumaGrupoReporte(t, s, c2, u, p);
            return s;
        }
    }

    private static void agregarFilaCuentaReporte(PdfPTable t, String n, double m, int c, boolean s, boolean l) {
        PdfPCell idx = new PdfPCell(new Phrase(String.valueOf(contadorIndice++), F_NORMAL));
        idx.setBorder(Rectangle.BOX);
        t.addCell(idx);
        agregarCelda(t, " " + n, F_NORMAL, false, false);
        for (int i = 3; i <= 5; i++) {
            if (i == c) {
                agregarCeldaMonto(t, formatearMoneda(m, s), l);
            } else {
                agregarCelda(t, "", F_NORMAL, false, false);
            }
        }
    }

    private static void agregarFilaSumaGrupoReporte(PdfPTable t, double m, int c, boolean l, boolean s) {
        agregarCelda(t, "", F_NORMAL, false, false);
        agregarCelda(t, "", F_NORMAL, false, false);
        for (int i = 3; i <= 5; i++) {
            if (i == c) {
                agregarCeldaMonto(t, formatearMoneda(m, s), l);
            } else {
                agregarCelda(t, "", F_NORMAL, false, false);
            }
        }
    }

    private static void agregarFilaTotalReporte(PdfPTable t, String tit, double m, int c, boolean l) {
        agregarCelda(t, "", F_NORMAL, false, false);
        PdfPCell ct = new PdfPCell(new Phrase(tit, F_NEGRITA));
        ct.setHorizontalAlignment(Element.ALIGN_LEFT);
        ct.setBorder(Rectangle.BOX);
        t.addCell(ct);
        for (int i = 3; i <= 5; i++) {
            if (i == c) {
                agregarCeldaMonto(t, formatearMoneda(m, true), l);
            } else {
                agregarCelda(t, "", F_NORMAL, false, false);
            }
        }
    }

    private static void agregarFilaTituloSeccion(PdfPTable t, String tit, int c) {
        PdfPCell cell = new PdfPCell(new Phrase(tit, F_NEGRITA));
        cell.setColspan(c);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorder(Rectangle.BOX);
        cell.setBackgroundColor(GRIS_SECCION);
        t.addCell(cell);
    }

    private static void agregarFilaTituloGrupo(PdfPTable t, String n, int cols) {
        if (n == null || n.isEmpty()) {
            return;
        }
        agregarCelda(t, "", F_NORMAL, false, false);
        PdfPCell c = new PdfPCell(new Phrase(n, F_ITALICA));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setBorder(Rectangle.LEFT | Rectangle.TOP | Rectangle.BOTTOM);
        c.setBackgroundColor(GRIS_GRUPO);
        t.addCell(c);
        for (int i = 0; i < (cols - 2); i++) {
            PdfPCell e = new PdfPCell(new Phrase(""));
            int b = Rectangle.TOP | Rectangle.BOTTOM;
            if (i == (cols - 3)) {
                b |= Rectangle.RIGHT;
            }
            e.setBorder(b);
            e.setBackgroundColor(GRIS_GRUPO);
            t.addCell(e);
        }
    }

    private static void agregarCelda(PdfPTable t, String txt, Font f, boolean c, boolean s) {
        PdfPCell cell = new PdfPCell(new Phrase(txt, f));
        cell.setBorder(Rectangle.BOX);
        if (s) {
            cell.setBorder(Rectangle.LEFT | Rectangle.TOP | Rectangle.BOTTOM);
        }
        if (c) {
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        } else {
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        }
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        t.addCell(cell);
    }

    private static void agregarCeldaMonto(PdfPTable t, String txt, boolean l) {
        PdfPCell c = new PdfPCell(new Phrase(txt, F_NORMAL));
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBorder(Rectangle.BOX);
        if (l) {
            c.setBorderWidthBottom(1.5f);
        }
        t.addCell(c);
    }

    private static List<CuentaContable> filtrarPorTipo(List<CuentaContable> l, String t) {
        return l.stream().filter(c -> c.getTipo().equalsIgnoreCase(t)).collect(Collectors.toList());
    }

    // CORRECCIÓN 4: Firmas centradas al 100%
    private static void agregarFirmas(Document doc, String e, String a) throws DocumentException {
        doc.add(new Paragraph("\n\n\n"));
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{50f, 50f});

        PdfPCell c1 = new PdfPCell();
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c1.setHorizontalAlignment(Element.ALIGN_CENTER);
        c1.addElement(crearLineaFirma());
        c1.addElement(crearTextoCentrado("Elaboró"));
        c1.addElement(crearTextoCentrado(e));

        PdfPCell c2 = new PdfPCell();
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c2.setHorizontalAlignment(Element.ALIGN_CENTER);
        c2.addElement(crearLineaFirma());
        c2.addElement(crearTextoCentrado("Autorizó"));
        c2.addElement(crearTextoCentrado(a));

        t.addCell(c1);
        t.addCell(c2);
        doc.add(t);
    }

    private static Paragraph crearLineaFirma() {
        Paragraph p = new Paragraph("_________________________", F_NORMAL);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    private static Paragraph crearTextoCentrado(String t) {
        Paragraph p = new Paragraph(t, F_NORMAL);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }
}
