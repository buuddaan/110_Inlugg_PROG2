package se.su.inlupp;

public class NodeEdge<T> implements Edge<T> {
    private final T destination;
    private final String name;
    private int weight;

    public NodeEdge(T destination, String name, int weight) {
        this.destination = destination;
        this.name = name;
        this.weight = weight;
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
        this.weight = weight;
    }
}
