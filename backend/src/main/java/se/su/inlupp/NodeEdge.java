package se.su.inlupp;

public class NodeEdge<T> implements Edge<T> {
    private final T destination;
    private final String name;
    private int weight;

    public NodeEdge(T destination, String name, int weight) {
        this.destination = destination;
        this.name = name;
        setWeight(weight);
    }
    @Override
    public T getDestination() {
        return destination;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getWeight() {
        return weight;
    }

    @Override
    public void setWeight(int weight) {
        if (weight < 0) {
            throw new IllegalArgumentException("Weight can not be negative");
        }
        this.weight = weight;
    }

    @Override
    public String toString(){
        return "till " + destination + " med " + name + " tar " + weight;
        }

}
