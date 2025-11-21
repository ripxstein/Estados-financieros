// ══════════════════════════════════════════════════════════════════════════════
// ARCHIVO 2: CatalogoCuentas.java
// ══════════════════════════════════════════════════════════════════════════════

package com.mycompany.estadosfinancieros;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class CatalogoCuentas {
    private List<CuentaContable> cuentas;
    private String mensajeError;
    private boolean cargadoCorrectamente;

    public CatalogoCuentas() {
        cuentas = new ArrayList<>();
        mensajeError = null;
        cargadoCorrectamente = false;
        cargarDesdeExcel("catalogo_cuentas.xlsx");
    }

    public CatalogoCuentas(String rutaArchivo) {
        cuentas = new ArrayList<>();
        mensajeError = null;
        cargadoCorrectamente = false;
        cargarDesdeExcel(rutaArchivo);
    }
    
    public void setCuentas(List<CuentaContable> cuentas) {
        this.cuentas = cuentas;
    }

    public List<CuentaContable> getCuentas() {
        return cuentas;
    }

    public boolean isCargadoCorrectamente() {
        return cargadoCorrectamente;
    }

    public String getMensajeError() {
        return mensajeError;
    }

    public List<CuentaContable> getCuentasParaBalance() {
        return cuentas.stream()
                .filter(c -> c.getSaldo() > 0 && c.esParaBalance())
                .toList();
    }

    public List<CuentaContable> getCuentasParaResultados() {
        return cuentas.stream()
                .filter(c -> c.getSaldo() > 0 && c.esParaResultados())
                .toList();
    }

    public Optional<CuentaContable> buscarPorCodigo(String codigo) {
        return cuentas.stream()
                .filter(c -> c.getCodigo().equalsIgnoreCase(codigo))
                .findFirst();
    }

    public void reiniciarSaldos() {
        cuentas.forEach(c -> c.setSaldo(0));
    }

    private void cargarDesdeExcel(String nombreArchivo) {
        File archivo = new File(nombreArchivo);
        
        if (!archivo.exists()) {
            mensajeError = "No se encontró el archivo: " + nombreArchivo;
            System.err.println("⚠️ " + mensajeError);
            return;
        }

        try (FileInputStream entrada = new FileInputStream(archivo);
             Workbook workbook = new XSSFWorkbook(entrada)) {
            
            Sheet hoja = workbook.getSheetAt(0);
            int cuentasCargadas = 0;

            for (int i = 1; i <= hoja.getLastRowNum(); i++) {
                Row fila = hoja.getRow(i);
                if (fila == null) continue;

                String codigo = obtenerTexto(fila.getCell(0));
                String nombre = obtenerTexto(fila.getCell(1));
                String tipo   = obtenerTexto(fila.getCell(2));
                String grupo  = obtenerTexto(fila.getCell(3));

                if (codigo == null || codigo.isEmpty() || 
                    nombre == null || nombre.isEmpty()) continue;

                cuentas.add(new CuentaContable(codigo, nombre, tipo, grupo));
                cuentasCargadas++;
            }

            cargadoCorrectamente = true;
            System.out.println("✔️ Catálogo cargado: " + cuentasCargadas + " cuentas");

        } catch (Exception e) {
            mensajeError = "Error al leer el archivo: " + e.getMessage();
            System.err.println("❌ " + mensajeError);
            e.printStackTrace();
        }
    }

    private String obtenerTexto(Cell celda) {
        if (celda == null) return null;
        return switch (celda.getCellType()) {
            case STRING -> celda.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(celda))
                    yield celda.getDateCellValue().toString();
                double val = celda.getNumericCellValue();
                yield (val == (int) val) ? String.valueOf((int) val) : String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(celda.getBooleanCellValue());
            case FORMULA -> {
                try { yield celda.getStringCellValue(); }
                catch (Exception e) { yield String.valueOf(celda.getNumericCellValue()); }
            }
            default -> null;
        };
    }
}