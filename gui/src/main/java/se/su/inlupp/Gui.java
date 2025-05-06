package se.su.inlupp;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Gui extends Application {

  public void start(Stage primaryStage) {
    Graph<String> graph = new ListGraph<String>();

    BorderPane root = new BorderPane();

    MenuBar menuBar = new MenuBar();
    Menu fileMenu = new Menu("File");

    MenuItem newMap = new MenuItem("New Map");
    MenuItem openMap = new MenuItem("Open Map");
    MenuItem saveMap = new MenuItem("Save Map");
    MenuItem saveImage = new MenuItem("Save Image");
    MenuItem exit = new MenuItem("Exit");

    fileMenu.getItems().addAll(newMap, openMap, saveMap, saveImage, new SeparatorMenuItem(), exit);
    menuBar.getMenus().add(fileMenu);

    Button findPath = new Button("Find Path");
    Button showConnection = new Button("Show Connection");
    Button newPlace = new Button("New Place");
    Button newConnection = new Button("New Connection");
    Button changeConnection = new Button("Change Connection");

    HBox hbox = new HBox(10);
    hbox.getChildren().addAll(findPath, showConnection, newPlace, newConnection, changeConnection);

    hbox.setAlignment(Pos.CENTER);
    hbox.setSpacing(10);

    VBox holdTop = new VBox();
    holdTop.getChildren().addAll(menuBar, hbox);
    
    root.setTop(holdTop); //OBS: Måste ligga innan vi visar scenen för att funka

    Scene scene = new Scene(root); //640, 480 tidigare storlek
    primaryStage.setScene(scene);
    primaryStage.setTitle("PathFinder");
    primaryStage.show();

  }

  public static void main(String[] args) {
    launch(args);
  }
}
