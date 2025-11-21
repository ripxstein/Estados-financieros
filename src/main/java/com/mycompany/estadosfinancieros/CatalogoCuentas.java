package com.mycompany.estadosfinancieros;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author ripxs
 */

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class CatalogoCuentas {

    private List<CuentaContable> cuentas;

    public CatalogoCuentas() {
        cuentas = new ArrayList<>();
        cargarDesdeExcel("catalogo_cuentas.xlsx");
    }

    public List<CuentaContable> getCuentas() {
        return cuentas;
    }

    public void setCuentas(List<CuentaContable> cuentas) {
        this.cuentas = cuentas;
    }

    /**
     * Lee el archivo Excel y crea las cuentas contables.
     */
    private void cargarDesdeExcel(String nombreArchivo) {
        try {
            File archivo = new File(nombreArchivo);
            if (!archivo.exists()) {
                System.out.println("⚠️ No se encontró el archivo: " + nombreArchivo);
                return;
            }

            FileInputStream entrada = new FileInputStream(archivo);
            Workbook workbook = new XSSFWorkbook(entrada);
            Sheet hoja = workbook.getSheetAt(0);

            for (int i = 1; i <= hoja.getLastRowNum(); i++) {  // Saltar encabezado
                Row fila = hoja.getRow(i);
                if (fila == null) continue;

                // Obtener valores como texto aunque la celda sea numérica
                String codigo = obtenerTexto(fila.getCell(0));
                String nombre = obtenerTexto(fila.getCell(1));
                String tipo   = obtenerTexto(fila.getCell(2));
                String grupo  = obtenerTexto(fila.getCell(3));

                if (codigo == null || nombre == null) continue;

                CuentaContable cuenta = new CuentaContable(codigo, nombre, tipo, grupo);
                cuentas.add(cuenta);
            }

            workbook.close();
            entrada.close();

            System.out.println("✔️ Catálogo cargado: " + cuentas.size() + " cuentas");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Convierte cualquier celda a texto, evitando errores comunes de POI.
     */
    private String obtenerTexto(Cell celda) {
        if (celda == null) return null;

        switch (celda.getCellType()) {

            case STRING:
                return celda.getStringCellValue().trim();

            case NUMERIC:
                if (DateUtil.isCellDateFormatted(celda)) {
                    return celda.getDateCellValue().toString();
                } else {
                    // Quitar .0 para códigos numéricos
                    double val = celda.getNumericCellValue();
                    if (val == (int) val)
                        return String.valueOf((int) val);
                    else
                        return String.valueOf(val);
                }

            case BOOLEAN:
                return String.valueOf(celda.getBooleanCellValue());

            case FORMULA:
                try {
                    return celda.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(celda.getNumericCellValue());
                }

            case BLANK:
            default:
                return null;
        }
    }
}