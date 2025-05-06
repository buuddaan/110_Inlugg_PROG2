package se.su.inlupp;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;


public class Gui extends Application {

  StackPane pane = new StackPane(); //Instansvariabel för att kunna återanvändas
  ImageView imageView = new ImageView(); //Läggs innan start() för att kunna nås av alla metoder



  public void start(Stage primaryStage) {
    Graph<String> graph = new ListGraph<String>();
    BorderPane root = new BorderPane();
    pane.setStyle("-fx-background-color: lightgray;"); //Bara för att visa vart Pane ligger innan bild kommer

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
    root.setCenter(pane);

    newMap.setOnAction(event -> {
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle("Choose a map image");
      fileChooser.getExtensionFilters().addAll(
              new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.gif")
      );


      File selectedFile = fileChooser.showOpenDialog(primaryStage); //showOpenD.. pop-up "Open File"
      if (selectedFile != null) {
        javafx.scene.image.Image image = new javafx.scene.image.Image(selectedFile.toURI().toString());
        imageView.setImage(image);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(image.getWidth());
        imageView.setFitHeight(image.getHeight());

        pane.getChildren().clear();
        pane.getChildren().add(imageView);
        pane.setPrefSize(image.getWidth(), image.getHeight());
        primaryStage.setWidth(image.getWidth()); //testar
        primaryStage.setHeight(image.getHeight()); //testar

      }
    });

    Scene scene = new Scene(root, 600, 400);
    primaryStage.setScene(scene);
    primaryStage.setTitle("PathFinder"); //Stilgrej att den är till vänster i Windows/Linux men i mitten för Mac
    primaryStage.show();
  }

  public static void main(String[] args){
      launch(args);
    }
}

