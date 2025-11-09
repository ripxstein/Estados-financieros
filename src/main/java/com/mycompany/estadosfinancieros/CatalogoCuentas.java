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

    public void cargarDesdeExcel(String rutaArchivo) {
        try (FileInputStream fis = new FileInputStream(rutaArchivo);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet hoja = workbook.getSheetAt(0);
            int filaInicio = 1; // omitir encabezado

            for (int i = filaInicio; i <= hoja.getLastRowNum(); i++) {
                Row fila = hoja.getRow(i);
                if (fila == null) continue;

                Cell cCodigo = fila.getCell(0);
                Cell cNombre = fila.getCell(1);
                Cell cTipo = fila.getCell(2);

                if (cCodigo == null || cNombre == null || cTipo == null) continue;

                String codigo = cCodigo.getStringCellValue().trim();
                String nombre = cNombre.getStringCellValue().trim();
                String tipo = cTipo.getStringCellValue().trim();

                if (!codigo.isEmpty() && !nombre.isEmpty() && !tipo.isEmpty()) {
                    cuentas.add(new CuentaContable(codigo, nombre, tipo));
                }
            }

            System.out.println("Catálogo de cuentas cargado desde Excel: " + cuentas.size() + " cuentas.");
        } catch (FileNotFoundException e) {
            System.err.println("⚠️ No se encontró el archivo Excel: " + rutaArchivo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
