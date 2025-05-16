package se.su.inlupp;


import java.util.*;


public class ListGraph<T> implements Graph<T> {

    // nodes är en Map där varje nod (T) pekar på en mängd (Set) av Edge<T> – alltså alla utgående kanter (förbindelser) från den noden
    private Map<T, Set<Edge<T>>> nodes = new HashMap<>(); // T = typparameter (generisk), ex String. Nyckel T är nod i grafen

    // Tar emot en node och stoppar in i grafen
    @Override
    public void add(T node) {
        nodes.putIfAbsent(node, new HashSet<>()); // Om frånvarande, lägg till nod.
    }

    @Override
    public void connect(T node1, T node2, String name, int weight) { //node = place, edge = connection, nodeEdge = en plats alla connections
        checkIfNodesExists(node1, node2);
        checkIfWeightIsValid(weight);
        checkIfNoExistingEdge(node1, node2);

        //Lägger till noder i båda riktningar med samma namn och vikt :D
        // Ser till att conections blir oriktad
        nodes.get(node1).add(new NodeEdge<>(node2, name, weight));
        nodes.get(node2).add(new NodeEdge<>(node1, name, weight));

    }

    // Tar emot två noder och ett heltal, förbindelsernas vikt.
    @Override
    public void setConnectionWeight(T node1, T node2, int weight) {
        checkIfWeightIsValid(weight);
        checkIfNodesExists(node1, node2);

        // Kollar att det finns en kant mellan noderna
        if (getEdgeBetween(node1, node2) == null) {
            throw new NoSuchElementException(); // Om det inte finns - undantag
        }
        // Hitta och uppdatera vikten i båda riktningarna (oriktad)
        for (Edge<T> edge : nodes.get(node1)) {
            if (edge.getDestination().equals(node2)) {
                edge.setWeight(weight);
                break; //Hindrar onödiga iterationer om vi redan hittar rätt direkt
            }
        }
        for (Edge<T> edge : nodes.get(node2)) {
            if (edge.getDestination().equals(node1)) {
                edge.setWeight(weight);
                break;
            }
        }
    }

    // Skapa en kopia av mängden innehållande noderna
    @Override
    public Set<T> getNodes() {
        return new HashSet<>(nodes.keySet());
    }

    // tar en nod och returnerar en kopia av samling av alla kanter som leder från denna nod.
    @Override
    public Collection<Edge<T>> getEdgesFrom(T node) {
        checkIfNodesExists(node); // om saknas får vi undantag NoSuchElementEX... Separat metod :D
        return new HashSet<>(nodes.get(node));
    }

    // Tar emot två noder och returnerar kanten mellan noderna.
    @Override
    public Edge<T> getEdgeBetween(T node1, T node2) {
        checkIfNodesExists(node1, node2); // om saknas får vi undantag NoSuchElementEX...

        for (Edge<T> edge : nodes.get(node1)) {
            if (edge.getDestination().equals(node2)) {
                return edge;
            }
        }
        return null;
    }

    // tar emot två noder och tar bort kanten mellan dem.
    @Override
    public void disconnect(T node1, T node2) {
        checkIfNodesExists(node1, node2);

        if (getEdgeBetween(node1, node2) == null) {
            throw new IllegalStateException("There is no connection between the nodes");
            // Vi valde att inte skapa en ny metod för detta undantag då argumenten är annorlunda i disconnect metoden. :)
        }

        // Ta bort kant från node1 till node2 (två för oriktad)
        nodes.get(node1).removeIf(edgeInCollection -> edgeInCollection.getDestination().equals(node2));
        // Ta bort kant från node2 till node1
        nodes.get(node2).removeIf(edgeInCollection -> edgeInCollection.getDestination().equals(node1));

    }

    // ta emot en nod och tar bort från grafen + alla kanter
    @Override
    public void remove(T node) {
        checkIfNodesExists(node);

        Iterator<Map.Entry<T, Set<Edge<T>>>> nodeIterator = nodes.entrySet().iterator(); //Valde Map.Entry för att kunna iterera över både värden/kanter + nycklar(noder) samtidigt

        // lopar igenom varje nod i grafen
        while (nodeIterator.hasNext()) {
            Map.Entry<T, Set<Edge<T>>> entry = nodeIterator.next();
            T currentNode = entry.getKey(); //Hämtar ut noden(nyckeln i HashMap) från det aktuella Map.entry-objetet och det läggs i currentNode.

            if (!currentNode.equals(node)) { //Hoppar över noden som vi fått in i vår metod, ska inte tas bort än!
                Set<Edge<T>> edges = entry.getValue(); //hämtar alla kanter från aktuell nod
                Iterator<Edge<T>> edgeIterator = edges.iterator(); //skapar iterator för att säkert kunna ta bort nod under iteration

                // loopar igenom alla kanter. om en kant leder till noden som ska bort -> ta bort den kanten
                while (edgeIterator.hasNext()) {
                    Edge<T> edge = edgeIterator.next();
                    if (edge.getDestination().equals(node)) {
                        edgeIterator.remove(); //ta bort kanten
                    }
                }
            }
        }
        nodes.remove(node); //när alla kanter är borta, ta bort noden
    }

    // tar emot två noder och returnerar true om väg mellan finns. Om någon av noderna inte finns ska de bli false
    @Override
    public boolean pathExists(T from, T to) {
        if (!nodes.containsKey(from) || !nodes.containsKey(to)) {return false;}
        Set<T> visited = new HashSet<>();
        return depthFirstSearch(from, to, visited);
    }

    //ny hjälpmetod för djupet först sökning
    private boolean depthFirstSearch(T current, T goal, Set<T> visited) {
        if (current.equals(goal)) { // om vårt nuvarande plats om mål plats är samma har vi hittat direkt
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

    // tar
    @Override
    public List<Edge<T>> getPath(T from, T to) {
        checkIfNodesExists(from, to);

        // om det inte fick en path, null
        if (!pathExists(from, to)) {
            return null;
        }

        // Map för hur man hittar varje node
        Map<T, T> previousNode = new HashMap<>();

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
                    previousNode.put(neighbor, current);
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
        if (!previousNode.containsKey(to)) {
            return null;
        }

        // kopiera och skapa ny path
        List<Edge<T>> path = new ArrayList<>();
        T current = to;

        while (!current.equals(from)) {
            T prev = previousNode.get(current);
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

    //detta är Beatrice toString
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


