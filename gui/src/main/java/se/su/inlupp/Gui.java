
package se.su.inlupp;

// Importer för JavaFX och IO
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
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
import javafx.scene.layout.*;
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

    //Datastruktur för att hantera grafen och kartvyn
    Graph<String> graph = new ListGraph<>();
    Pane overlayPane = new Pane(); //transparant lager över bilden för koordinater
    ImageView imageView = new ImageView();
    StackPane pane = new StackPane(imageView, overlayPane);
    private List<Circle> selectedCircles = new ArrayList<>(); // Denna rad ska läggas till

    private boolean hasUnsavedChanges = false;
    private String imageFilePath = "";
    private Map<String, Place> placeMap = new HashMap<>();
    private double extraHeight;

    public void start(Stage primaryStage) {
        // Grundstruktur för fönstret
        BorderPane root = new BorderPane();
        pane.setStyle("-fx-background-color: lightgray;");
        //overlayPane.setStyle("-fx-background-color: transparent;"); //Tillbaka till transparent
        overlayPane.setStyle("-fx-background-color: transparent;");
        overlayPane.setPickOnBounds(false);

        // En lyssnare som explicit ändrar positionen av
        imageView.boundsInParentProperty().addListener((obs, oldBounds, newBounds) -> {
            // Apply the exact bounds of the ImageView to the overlay
            overlayPane.setMaxWidth(newBounds.getWidth());
            overlayPane.setMaxHeight(newBounds.getHeight());
        });

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

            //för rätt musval
            overlayPane.setCursor(Cursor.CROSSHAIR);
            //Inaktivera
            newPlace.setDisable(true);

            //Style grejer, kolla i uppgiftsbeskrivningen
            overlayPane.setOnMouseClicked(e -> {

                javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
                dialog.setTitle("New Place");
                dialog.setHeaderText("Enter name for the new place");
                dialog.setContentText("Name:");

                dialog.showAndWait().ifPresent(name -> {
                    if (!name.trim().isEmpty() && !graph.getNodes().contains(name)) {
                        double x = e.getX();
                        double y = e.getY();
                        Place place = new Place(name, x, y);
                        graph.add(name);
                        placeMap.put(name, place);
                        drawPlace(place);

                        hasUnsavedChanges = true;
                    } else {
                        showError("Name is empty or already used.");
                    }
                });

                    // Återställer muslyssnaren så att man inte råkar skapa flerapane.setOnMouseClicked(null);
                enableButton(newPlace);

            });
        });

        HBox hbox = new HBox(10);
        hbox.getChildren().addAll(findPath, showConnection, newPlace, newConnection, changeConnection);
        hbox.setAlignment(Pos.CENTER);

        VBox holdTop = new VBox(menuBar, hbox);
        root.setTop(holdTop);
        root.setCenter(pane);

        // Skapa scen och visa fönstret
        Scene scene = new Scene(root, 640, 480);
        primaryStage.setScene(scene);
        primaryStage.setTitle("PathFinder");
        primaryStage.show();
        extraHeight =  (primaryStage.getHeight() - primaryStage.getScene().getHeight()) + holdTop.getHeight();


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

                pane.getChildren().setAll(imageView, overlayPane);
                pane.setPrefSize(image.getWidth(), image.getHeight());
                primaryStage.setWidth(image.getWidth());
                primaryStage.setHeight(image.getHeight() + extraHeight); //tillagt 20 här bara /EF tog bort den härifrån

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
                    bw.write(imageFilePath); //Senare lägga till bw.write("file:"+imageFilePath); istället eftersom tidigare filer har det. Ingen funktionalitet för split än bara
                    bw.newLine();
                    bw.write(createSaveStringOfGraphs()); // Placeholder
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
    }

    private void enableButton(Button buttonName) {
        buttonName.setDisable(false);
        overlayPane.setCursor(Cursor.DEFAULT);
        overlayPane.setOnMouseClicked(null); // Rensa eventhandler
    }



    private void loadGraphFromFile(File file) {
        try (Scanner scanner = new Scanner(file)) {
            String imagePath = scanner.nextLine();
            Image image = new Image(new File(imagePath).toURI().toString());
            imageView.setImage(image);
            imageView.setPreserveRatio(true);
            String graphString = scanner.nextLine();
            addPlacesFromFile(graphString);
            // Här 2 rader
            // Don't resize the image, keep it at its original dimensions
            imageView.setFitWidth(0);
            imageView.setFitHeight(0);

            pane.getChildren().setAll(imageView, overlayPane);
            pane.setPrefSize(image.getWidth(), image.getHeight()); //Tillagd för test 9/5 23:49

            //och här
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
                // Om färgen är blå och max 2 ej redan valda
                if (selectedCircles.size() < 2) {
                    circle.setFill(Color.RED);
                    selectedCircles.add(circle);
                } else {
                    // Visa felmeddelande eller ignorera klicket
                    showError("Du kan inte markera mer än två ställen samtidigt!");
                }
            } else {
                // Avmarkera
                circle.setFill(Color.BLUE);
                selectedCircles.remove(circle);
            }
        });

        overlayPane.getChildren().add(circle); // Glöm inte lägga till cirkeln i vyn
    }

    // Ritar en förbindelse (linje) mellan två platser
    private void drawConnection(Place from, Place to) {
        Line line = new Line(from.getX(), from.getY(), to.getX(), to.getY());
        line.setStroke(Color.BLACK);
        line.setStrokeWidth(2);
        pane.getChildren().add(line);
    }

    private void addPlacesFromFile(String stringOfPlaces){
        List<Place> placesToAdd = new ArrayList<>();
        String[] values = stringOfPlaces.split(";");
        String city;
        for (int i = 0; i < values.length; i+=3){
            city = values[i];
            double x = Double.parseDouble(values[i+1]);
            double y = Double.parseDouble(values[i+2]);
            placesToAdd.add(new Place(city, x, y));
        }
        for (Place p : placesToAdd){
            graph.add(p.getName());
            placeMap.put(p.getName(), p);
            drawPlace(p);
        }

//TODO implementera funktionalitet för connection
        //koppla musklick 1, musklick 2 (MAX 2) och koppla de till plats
        // graph.net .någonting .connect?

    }

    private String createSaveStringOfGraphs(){
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Place> entry : placeMap.entrySet()) {
            sb.append(entry.getValue().toString());
            sb.append(";"); // append semicolon after each place
        }
        // Remove the last extra semicolon
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
