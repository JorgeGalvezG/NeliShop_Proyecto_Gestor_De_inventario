package io.carpets.DTOs;

public class MontosCalculados {
    private double subtotal;
    private double igvSolo;
    private double totalConIGV;

    public MontosCalculados(double subtotal, double igvSolo, double totalConIGV) {
        this.subtotal = subtotal;
        this.igvSolo = igvSolo;
        this.totalConIGV = totalConIGV;
    }

    // Getters
    public double getSubtotal() { return subtotal; }
    public double getIgvSolo() { return igvSolo; }
    public double getTotalConIGV() { return totalConIGV; }

    // Setters
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }
    public void setIgvSolo(double igvSolo) { this.igvSolo = igvSolo; }
    public void setTotalConIGV(double totalConIGV) { this.totalConIGV = totalConIGV; }

    @Override
    public String toString() {
        return String.format("Subtotal: %.2f, IGV: %.2f, Total: %.2f", subtotal, igvSolo, totalConIGV);
    }
}