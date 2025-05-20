// PROG2 VT2025, Inlämningsuppgift del 1
// Grupp 110
// Elvira Fröjd eljo2851
// Mathilda Wallen mawa6612
// Matilda Fahle mafa2209


package se.su.inlupp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

public class ListGraph<T> implements Graph<T> {

    private Map<T, Set<Edge<T>>> nodes = new HashMap<>(); // T = typparameter, ex String. Nyckel T är nod i grafen. En map som lagrar varje nod och kanter, edge.


    @Override
    public void add(T node) {
        nodes.putIfAbsent(node, new HashSet<>());
    }

    @Override
    public void connect(T node1, T node2, String name, int weight) { //node = place, edge = connection, nodeedge = en plats alla connections
        checkIfNodesExists(node1, node2);
        checkIfWeightIsValid(weight); //kan vara något överflödig, fungerar även utan men men
        checkIfNoExistingEdge(node1, node2); //om noderna inte finns, undantag

        //Lägger till noder i båda riktningar med samma namn och vikt :D
        nodes.get(node1).add(new NodeEdge<>(node2, name, weight));
        nodes.get(node2).add(new NodeEdge<>(node1, name, weight));
    }

    @Override
    public void setConnectionWeight(T node1, T node2, int weight) {
        checkIfWeightIsValid(weight);
        checkIfNodesExists(node1, node2);

        if (getEdgeBetween(node1, node2) == null) {
            throw new NoSuchElementException();
        }
        // itererar alla node1:s kanter och kollar om destination är node2. Om kanten finns, uppdatera vikten.
        for (Edge<T> edge : nodes.get(node1)) {
            if (edge.getDestination().equals(node2)) {
                edge.setWeight(weight);
                break; // break för att for-each för att stoppa när vi hittat rätt.
            }
        }
        // samma fast andra hållet pga hela grafen är oriktad
        for (Edge<T> edge : nodes.get(node2)) {
            if (edge.getDestination().equals(node1)) {
                edge.setWeight(weight);
                break;
            }
        }
    }

    // kopia av noderna
    @Override
    public Set<T> getNodes() {
        return new HashSet<>(nodes.keySet()); //keySet() = hämtar ett set av nycklarna i en map
    }

    @Override
    public Collection<Edge<T>> getEdgesFrom(T node) {
        checkIfNodesExists(node);
        return new HashSet<>(nodes.get(node)); // Kopia av kanterna från noderna
    }

    @Override
    public Edge<T> getEdgeBetween(T node1, T node2) {
        checkIfNodesExists(node1, node2);

        // kollar om det finns en kant mellan node1 och node2
        for (Edge<T> edge : nodes.get(node1)) {
            if (edge.getDestination().equals(node2)) {
                return edge;
            }
        }
        return null; //om ingen kant returnera null
    }

    @Override
    public void disconnect(T node1, T node2) {
        checkIfNodesExists(node1, node2);

        if (getEdgeBetween(node1, node2) == null) {
            throw new IllegalStateException("There is no connection between the nodes");
            // Vi valde att inte skapa en ny metod för detta undantag då argumenten är annorlunda i disconnect metoden. :)
        }

        // Ta bort kant från node1 till node2
        nodes.get(node1).removeIf(edgeInCollection -> edgeInCollection.getDestination().equals(node2));
        // Ta bort kant från node2 till node1
        nodes.get(node2).removeIf(edgeInCollection -> edgeInCollection.getDestination().equals(node1));

    }

    @Override
    public void remove(T node) {
        checkIfNodesExists(node);

        Iterator<Map.Entry<T, Set<Edge<T>>>> nodeIterator = nodes.entrySet().iterator(); //Valde Map.Entry för att kunna iterera över både värden + nycklar samtidigt
        while (nodeIterator.hasNext()) {
            Map.Entry<T, Set<Edge<T>>> entry = nodeIterator.next();
            T currentNode = entry.getKey(); //Hämtar ut nyckeln från nästa nod?

            if (!currentNode.equals(node)) { //Hoppar över noden som vi fått in i vår metod, ska inte tas bort än!
                Set<Edge<T>> edges = entry.getValue(); //hämtar alla kanter från aktuell nod
                Iterator<Edge<T>> edgeIterator = edges.iterator(); //skapar iterator för att säkert kunna ta bort nod under iteration

                while (edgeIterator.hasNext()) {
                    Edge<T> edge = edgeIterator.next();
                    if (edge.getDestination().equals(node)) {
                        edgeIterator.remove(); //ta bort kanten
                    }
                }
            }
        }
        nodes.remove(node);
    }

    @Override
    public boolean pathExists(T from, T to) {
        if (!nodes.containsKey(from) || !nodes.containsKey(to)) {
            return false;
        }
        Set<T> visited = new HashSet<>();
        return depthFirstSearch(from, to, visited);
    }

    private boolean depthFirstSearch(T current, T goal, Set<T> visited) { //ny hjälpmetod för djupet först sökning
        if (current.equals(goal)) {
            return true;
        }

        visited.add(current);

        for (Edge<T> edge : nodes.get(current)) {
            T neighbor = edge.getDestination();
            if (!visited.contains(neighbor)) {
                if (depthFirstSearch(neighbor, goal, visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<Edge<T>> getPath(T from, T to) {
        checkIfNodesExists(from, to);

        // om det inte fick en path, null
        if (!pathExists(from, to)) {
            return null;
        }

        // Map för hur man hittar varje node
        Map<T, T> predecessor = new HashMap<>();

        // Använd BFS för den kortaste vägen fram
        Queue<T> queue = new LinkedList<>();
        Set<T> visited = new HashSet<>();

        visited.add(from);
        queue.add(from);

        while (!queue.isEmpty()) {
            T current = queue.poll();

            // hitta alla grannar
            for (Edge<T> edge : nodes.get(current)) {
                T neighbor = edge.getDestination();

                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    predecessor.put(neighbor, current);
                    queue.add(neighbor);

                    // om vi hittat rätt väg, break.
                    if (neighbor.equals(to)) {
                        queue.clear();
                        break;
                    }
                }
            }
        }

        // om vi inte hittat rätt
        if (!predecessor.containsKey(to)) {
            return null;
        }

        // kopiera och skapa ny path
        List<Edge<T>> path = new ArrayList<>();
        T current = to;

        while (!current.equals(from)) {
            T prev = predecessor.get(current);
            path.add(0, getEdgeBetween(prev, current));
            current = prev;
        }

        return path;
    }


    // Privata hjälpmetoder för undantag
    private void checkIfNodesExists(T node1, T node2) {
        if (!nodes.containsKey(node1) || !nodes.containsKey(node2)) {
            throw new NoSuchElementException("One or both of the nodes does not exist");
        }
    }

    private void checkIfNodesExists(T node) {
        if (!nodes.containsKey(node)) {
            throw new NoSuchElementException("The node does not exist");
        }
    }

    private void checkIfWeightIsValid(int weight) {
        if (weight < 0) {
            throw new IllegalArgumentException("Weight can not be negative!");
        }
    }

    private void checkIfNoExistingEdge(T node1, T node2) {
        if (getEdgeBetween(node1, node2) != null) {
            throw new IllegalStateException("There already is a connection between the nodes");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Nodes");
        sb.append("\n");
        for (Map.Entry<T, Set<Edge<T>>> keyValue : nodes.entrySet()) {
            sb.append(keyValue.getKey()).append(": ").append(keyValue.getValue()).append("\n");
        }
        return sb.toString();
    }
}


