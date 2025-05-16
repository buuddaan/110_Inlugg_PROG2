// PROG2 VT2025, Inlämningsuppgift del 1
// Grupp 110
// Elvira Fröjd eljo2851
// Mathilda Wallen mawa6612
// Matilda Fahle mafa2209

package se.su.inlupp;

public interface Edge<T> {

  int getWeight();

  void setWeight(int weight);

  T getDestination();

  String getName();
}
