package se.su.inlupp;

public class Place {
    private String name;
    private double x;
    private double y;

    public Place(String name, double x, double y) {
        this.name = name;
        this.x = x;
        this.y = y;
    }

    public String getName() {
        return name;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    @Override
    public String toString() {
        return name+";"+x+";"+y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Place) {
            Place other = (Place) obj;
            return name.equals(other.name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}