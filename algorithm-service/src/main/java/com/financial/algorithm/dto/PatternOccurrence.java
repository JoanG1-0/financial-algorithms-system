package com.financial.algorithm.dto;

/**
 * Detalle de una ocurrencia del patrón de reversión a la media.
 *
 * <p>Registra el índice temporal, el precio de cierre observado,
 * el valor de la SMA en ese punto, la desviación estándar local
 * y la dirección de la desviación respecto a la media.
 */
public class PatternOccurrence {

    /** Dirección de la desviación del precio respecto a la SMA. */
    public enum Direction { ABOVE, BELOW }

    private int startIndex;
    private double price;
    private double sma;
    private double sigma;
    private Direction direction;

    public PatternOccurrence() {}

    private PatternOccurrence(Builder builder) {
        this.startIndex = builder.startIndex;
        this.price = builder.price;
        this.sma = builder.sma;
        this.sigma = builder.sigma;
        this.direction = builder.direction;
    }

    public static Builder builder() { return new Builder(); }

    public int getStartIndex() { return startIndex; }
    public void setStartIndex(int startIndex) { this.startIndex = startIndex; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getSma() { return sma; }
    public void setSma(double sma) { this.sma = sma; }

    public double getSigma() { return sigma; }
    public void setSigma(double sigma) { this.sigma = sigma; }

    public Direction getDirection() { return direction; }
    public void setDirection(Direction direction) { this.direction = direction; }

    public static class Builder {
        private int startIndex;
        private double price;
        private double sma;
        private double sigma;
        private Direction direction;

        public Builder startIndex(int startIndex) { this.startIndex = startIndex; return this; }
        public Builder price(double price) { this.price = price; return this; }
        public Builder sma(double sma) { this.sma = sma; return this; }
        public Builder sigma(double sigma) { this.sigma = sigma; return this; }
        public Builder direction(Direction direction) { this.direction = direction; return this; }

        public PatternOccurrence build() { return new PatternOccurrence(this); }
    }
}
