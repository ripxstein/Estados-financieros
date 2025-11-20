/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.estadosfinancieros;

/**
 *
 * @author ripxs
 */
public class CuentaContable {

    private String codigo;
    private String nombre;
    private String tipo;
    private String grupo;   // ← Necesario para el estado de resultados
    private double saldo;

    public CuentaContable(String codigo, String nombre, String tipo, String grupo) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipo = tipo;
        this.grupo = grupo;
        this.saldo = 0.0;
    }

    // ====== GETTERS ======
    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public String getGrupo() {
        return grupo;
    }

    public double getSaldo() {
        return saldo;
    }

    // ====== SETTERS ======
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public void setGrupo(String grupo) {
        this.grupo = grupo;
    }

    public void setSaldo(double saldo) {
        this.saldo = saldo;
    }

    // ====== PARA MOSTRAR EN JComboBox ======
    @Override
    public String toString() {
        return codigo + " - " + nombre;
    }
}