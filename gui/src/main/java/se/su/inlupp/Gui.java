
package se.su.inlupp;

// Importer för JavaFX och IO
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import javafx.scene.SnapshotParameters;
import javax.imageio.ImageIO;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Gui extends Application {

    // Datastruktur för att hantera grafen och kartvyn
    Graph<String> graph = new ListGraph<>();
    StackPane pane = new StackPane();
    ImageView imageView = new ImageView();
    private boolean hasUnsavedChanges = false;
    private String imageFilePath = "";
    private Map<String, Place> placeMap = new HashMap<>();

    public void start(Stage primaryStage) {
        // Grundstruktur för fönstret
        BorderPane root = new BorderPane();
        pane.setStyle("-fx-background-color: lightgray;");

        // Meny för File
        MenuBar menuBar = new MenuBar();
        Menu fileMenu = new Menu("File");

        // Menyval
        MenuItem newMap = new MenuItem("New Map");
        MenuItem openMap = new MenuItem("Open Map");
        MenuItem saveMap = new MenuItem("Save Map");
        MenuItem saveImage = new MenuItem("Save Image");
        MenuItem exit = new MenuItem("Exit");

        fileMenu.getItems().addAll(newMap, openMap, saveMap, saveImage, new SeparatorMenuItem(), exit);
        menuBar.getMenus().add(fileMenu);

        // Knappar för funktioner
        Button findPath = new Button("Find Path");
        Button showConnection = new Button("Show Connection");
        Button newPlace = new Button("New Place");
        Button newConnection = new Button("New Connection");
        Button changeConnection = new Button("Change Connection");

        //4.2.1 NewPlace knappens funktionalitet
        newPlace.setOnAction(event -> {
            pane.setOnMouseClicked(e -> {
                javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
                dialog.setTitle("New Place");
                dialog.setHeaderText("Enter name for the new place");
                dialog.setContentText("Name:");

                dialog.showAndWait().ifPresent(name -> {
                    if (!name.trim().isEmpty() && !graph.getNodes().contains(name)) {
                        double x = e.getX() - pane.getLayoutX(); //För att alignement ska stämma utifrån bakgrundsbilden
                        double y = e.getY() - pane.getLayoutY(); //För att alignement ska stämma utifrån bakgrundsbilden
                        Place place = new Place(name, x, y);
                        graph.add(name);
                        placeMap.put(name, place);
                        drawPlace(place);
                        hasUnsavedChanges = true;
                    } else {
                        showError("Name is empty or already used.");
                    }
                });

                // Återställ muslyssnaren så att man inte råkar skapa flera
                pane.setOnMouseClicked(null);
            });
        });


        HBox hbox = new HBox(10);
        hbox.getChildren().addAll(findPath, showConnection, newPlace, newConnection, changeConnection);
        hbox.setAlignment(Pos.CENTER);

        VBox holdTop = new VBox(menuBar, hbox);
        root.setTop(holdTop);
        root.setCenter(pane);

        //4.1.1 New Map
        newMap.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose a map image");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                imageFilePath = selectedFile.getAbsolutePath();
                Image image = new Image(selectedFile.toURI().toString());
                imageView.setImage(image);
                imageView.setPreserveRatio(true);
                imageView.setFitWidth(image.getWidth());
                imageView.setFitHeight(image.getHeight());

                pane.getChildren().clear();
                pane.getChildren().add(imageView);
                pane.setPrefSize(image.getWidth(), image.getHeight());
                primaryStage.setWidth(image.getWidth());
                primaryStage.setHeight(image.getHeight());
                hasUnsavedChanges = true;
            }
        });

        //4.1.5 Exit function
        exit.setOnAction(event -> {
            if (!confirmDiscardChanges()) return;
            primaryStage.close();
        });

        // Stängning via exit-knappen
        primaryStage.setOnCloseRequest(event -> {
            if (!confirmDiscardChanges()) {
                event.consume(); // Avbryt stängning
            }
        });

        //4.1.2 Open
        openMap.setOnAction(event -> {
            if (!confirmDiscardChanges()) return;
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose a graph file");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Graph files", "*.graph")
            );
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                loadGraphFromFile(selectedFile);
            }
        });

        //4.1.3 Save Map
        saveMap.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save graph");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Graph files", "*.graph")
            );
            File selectedFile = fileChooser.showSaveDialog(primaryStage);
            if (selectedFile != null) {
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(selectedFile))) {
                    bw.write(imageFilePath);
                    bw.newLine();
                    bw.write("Vad du vill!"); // Placeholder
                    bw.newLine();
                    hasUnsavedChanges = false;
                } catch (Exception e) {
                    showError("Failed to save graph: " + e.getMessage());
                }
            }
        });

        //4.1.4 Save Image
        saveImage.setOnAction(e -> {
            WritableImage image = pane.snapshot(new SnapshotParameters(), null);
            File outputFile = new File("capture.png");
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", outputFile);
            } catch (Exception ex) {
                showError("Failed to save image: " + ex.getMessage());
            }
        });

        // Skapa scen och visa fönstret
        Scene scene = new Scene(root, 640, 480);
        primaryStage.setScene(scene);
        primaryStage.setTitle("PathFinder");
        primaryStage.show();
    }

    private void loadGraphFromFile(File file) {
        try (Scanner scanner = new Scanner(file)) {
            String imagePath = scanner.nextLine();
            Image image = new Image(new File(imagePath).toURI().toString());
            imageView.setImage(image);
            imageView.setPreserveRatio(true);
            pane.getChildren().clear();
            pane.getChildren().add(imageView);
            hasUnsavedChanges = false;
        } catch (Exception e) {
            showError("Failed to load graph: " + e.getMessage());
        }
    }

    // Enligt figur 6, error
    private boolean confirmDiscardChanges() {
        if (!hasUnsavedChanges) return true;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("There are unsaved changes.");
        alert.setContentText("Do you want to discard changes?");
        ButtonType okButton = new ButtonType("OK");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(okButton, cancelButton);
        return alert.showAndWait().orElse(cancelButton) == okButton;
    }

    // Visar felmeddelande i dialogruta
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Ritar en plats (cirkel) på kartan med tooltip och klickfunktion
    private void drawPlace(Place place) {
        Circle circle = new Circle(place.getX(), place.getY(), 8);
        circle.setFill(Color.BLUE);
        circle.setStroke(Color.BLACK);

        Tooltip tooltip = new Tooltip(place.getName());
        Tooltip.install(circle, tooltip);

        circle.setOnMouseClicked(event -> {
            if (circle.getFill().equals(Color.BLUE)) {
                circle.setFill(Color.RED);
            } else {
                circle.setFill(Color.BLUE);
            }
        });

        pane.getChildren().add(circle);
    }

    // Ritar en förbindelse (linje) mellan två platser
    private void drawConnection(Place from, Place to) {
        Line line = new Line(from.getX(), from.getY(), to.getX(), to.getY());
        line.setStroke(Color.BLACK);
        line.setStrokeWidth(2);
        pane.getChildren().add(line);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
