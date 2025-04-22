package se.su.inlupp;

import java.util.*;

public class ListGraph<T> implements Graph<T> {
  // T en typparameter, ex Int, String. Nyckel

  private Map<T, Set<Edge>> nodes = new HashMap<>();


  @Override
  public void add(T node) {
      nodes.putIfAbsent(node, new HashSet<>());
      //throw new UnsupportedOperationException("Unimplemented method 'add'");
  }

  @Override
  public void connect(T node1, T node2, String name, int weight) {

    if (!nodes.containsKey(node1) || !nodes.containsKey(node2))  { //Kontrollerar så att båda noderna finns, annars kastas exception
      throw new NoSuchElementException("One or both of the nodes does not exist");
    }
      add(node1);
      add(node2);

      Set<Edge> fromNodes = nodes.get(node1);
      Set<Edge> toNodes = nodes.get(node2);

      fromNodes.add(new NodeEdge(node1, name, weight));
      toNodes.add(new NodeEdge(node2, name, weight));



    throw new UnsupportedOperationException("Unimplemented method 'connect'");
  }

  @Override
  public void setConnectionWeight(T node1, T node2, int weight) {
    throw new UnsupportedOperationException("Unimplemented method 'setConnectionWeight'");
  }

  @Override
  public Set<T> getNodes() {
    throw new UnsupportedOperationException("Unimplemented method 'getNodes'");
  }

  @Override
  public Collection<Edge<T>> getEdgesFrom(T node) {
    throw new UnsupportedOperationException("Unimplemented method 'getEdgesFrom'");
  }

  @Override
  public Edge<T> getEdgeBetween(T node1, T node2) {
    throw new UnsupportedOperationException("Unimplemented method 'getEdgeBetween'");
  }

  @Override
  public void disconnect(T node1, T node2) {
    throw new UnsupportedOperationException("Unimplemented method 'disconnect'");
  }

  @Override
  public void remove(T node) {

    if (!nodes.containsKey(node)) {
      throw new NoSuchElementException("The node does not exist");
    }

   Iterator<Map.Entry<T, Set<Edge>>> nodeIterator = nodes.entrySet().iterator(); //Valde Map.Entry för att kunna iterera över både värden + nycklar samtidigt
    while (nodeIterator.hasNext()) {
        Map.Entry<T, Set<Edge>> entry = nodeIterator.next();
        T currentNode = entry.getKey(); //Hämtar ut nyckeln från nästa nod?


        if (!currentNode.equals(node)) { //Hoppar över noden som vi fått in i vår metod, ska inte tas bort än!
            Set<Edge> edges = entry.getValue(); //hämtar alla kanter från aktuell nod
            Iterator<Edge> edgeIterator = edges.iterator(); //skapar iterator för att säkert kunna ta bort nod under iteration

            while (edgeIterator.hasNext()) {
                Edge<T> edge = edgeIterator.next();
                if (edge.getDestination().equals(node)) {
                    edgeIterator.remove(); //ta bort kanten
                }
            }
        }
    }
    nodes.remove(node);
    throw new UnsupportedOperationException("Unimplemented method 'remove'");
  }


  @Override
  public boolean pathExists(T from, T to) {
    throw new UnsupportedOperationException("Unimplemented method 'pathExists'");
  }

  @Override
  public List<Edge<T>> getPath(T from, T to) {
    throw new UnsupportedOperationException("Unimplemented method 'getPath'");
  }
}

