// ══════════════════════════════════════════════════════════════════════════════
// ARCHIVO 1: CuentaContable.java
// ══════════════════════════════════════════════════════════════════════════════

package com.mycompany.estadosfinancieros;

public class CuentaContable {
    private String codigo;
    private String nombre;
    private String tipo;
    private String grupo;
    private double saldo;

    public CuentaContable(String codigo, String nombre, String tipo, String grupo) {
        this.codigo = codigo != null ? codigo.trim() : "";
        this.nombre = nombre != null ? nombre.trim() : "";
        this.tipo = tipo != null ? tipo.trim() : "";
        this.grupo = grupo != null ? grupo.trim() : "";
        this.saldo = 0.0;
    }
    
    public CuentaContable(String codigo, String nombre, String tipo, String grupo, double saldo) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipo = tipo;
        this.grupo = grupo;
        this.saldo = saldo;
    }

    // GETTERS
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
    public String getGrupo() { return grupo; }
    public double getSaldo() { return saldo; }

    // SETTERS
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public void setGrupo(String grupo) { this.grupo = grupo; }
    public void setSaldo(double saldo) { this.saldo = Math.max(0, saldo); }

    public boolean esParaBalance() {
        return tipo.matches("(?i)Activo|Pasivo|Capital");
    }

    public boolean esParaResultados() {
        return tipo.matches("(?i)Ingreso|Gasto") || (grupo != null && !grupo.isEmpty());
    }

// En CuentaContable.java (al final del archivo)

    @Override
    public String toString() {
        // Antes era: return codigo + " - " + nombre;
        // Ahora solo regresamos el nombre para que se vea limpio en la lista:
        return nombre; 
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CuentaContable otra = (CuentaContable) obj;
        return codigo.equalsIgnoreCase(otra.codigo);
    }

    @Override
    public int hashCode() {
        return codigo.toLowerCase().hashCode();
    }
}