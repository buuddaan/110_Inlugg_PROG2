package se.su.inlupp;

// Importer för JavaFX och IO
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
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
    Pane overlayPane = new Pane(); // Transparent lager över bilden för koordinater
    ImageView imageView = new ImageView();
    StackPane pane = new StackPane(imageView, overlayPane);
    private List<Circle> selectedCircles = new ArrayList<>();

    private boolean hasUnsavedChanges = false;
    private String imageFilePath = "";
    private Map<String, Place> placeMap = new HashMap<>();
    private double extraHeight;

    Button findPath;
    Button showConnection;
    Button newPlace;
    Button newConnection;
    Button changeConnection;


    public void start(Stage primaryStage) {


        // Grundstruktur för fönstret
        BorderPane root = new BorderPane();
        pane.setStyle("-fx-background-color: lightgray;");
        //overlayPane.setStyle("-fx-background-color: transparent;"); //Tillbaka till transparent
        overlayPane.setStyle("-fx-background-color: transparent;");
        overlayPane.setPickOnBounds(false);



        // En lyssnare som explicit ändrar överlappspanelens storlek
        imageView.boundsInParentProperty().addListener((obs, oldBounds, newBounds) -> {
            // Applicera exakta mått från ImageView till överlappspanelen
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
         findPath = new Button("Find Path");
         showConnection = new Button("Show Connection");
         newPlace = new Button("New Place");
         newConnection = new Button("New Connection");
         changeConnection = new Button("Change Connection");

        disableAllButtons();

        //4.2.1 Funktionalitet för Ny Plats-knappen
        newPlace.setOnAction(event -> {
            // För rätt muspekare
            overlayPane.setCursor(Cursor.CROSSHAIR);
            // Inaktivera knappen
            disableButton(newPlace);

            // Ställ in händelsehanterare för musklick
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

        //4.2.3 Funktionalitet för Ny Förbindelse-knappen
        newConnection.setOnAction(event -> {
            // Inaktivera knapp när den klickas
            disableButton(newConnection);

            Place[] places = getSelectedPlaces();
            if (places == null) {
                enableButton(newConnection); // Återaktivera om validering misslyckas
                return;
            }

            final String node1 = places[0].getName();
            final String node2 = places[1].getName();

            // Kontrollera om förbindelse redan finns
            if (graph.getEdgeBetween(node1, node2) != null) {
                showError("There already is a connection between the selected places.");
                enableButton(newConnection); // Återaktivera knappen
                return;
            }

            // Create dialog for connection details
            javafx.scene.control.Dialog<ButtonType> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Connection");
            dialog.setHeaderText("Connection from " + node1 + " to " + node2);

            // Create dialog pane layout
            javafx.scene.control.DialogPane dialogPane = dialog.getDialogPane();

            // Create button types with OK on the left and Cancel on the right (like in image 2)
            ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            // Add button types in the order OK, Cancel (this is key to match image 2)
            dialogPane.getButtonTypes().addAll(okButton, cancelButton);

            javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
            grid.setHgap(10.0);
            grid.setVgap(10.0);
            grid.setPadding(new Insets(20, 150, 10, 10));

            TextField nameField = new TextField();
            TextField timeField = new TextField();

            grid.add(new Label("Name:"), 0, 0);
            grid.add(nameField, 1, 0);
            grid.add(new Label("Time:"), 0, 1);
            grid.add(timeField, 1, 1);

            dialogPane.setContent(grid);

            // Show dialog and process result
            Place place1 = places[0];
            Place place2 = places[1];
            dialog.showAndWait().ifPresent(response -> {
                if (response == okButton) { // Note: Using okButton instead of ButtonType.OK
                    String name = nameField.getText().trim();
                    String timeText = timeField.getText().trim();

                    // Validate input
                    if (name.isEmpty()) {
                        showError("Name cannot be empty.");
                        return;
                    }

                    int time;
                    try {
                        time = Integer.parseInt(timeText);
                        if (time < 0) {
                            showError("Time must be a positive number.");
                            return;
                        }
                    } catch (NumberFormatException e) {
                        showError("Time must consist of digits only.");
                        return;
                    }

                    // Create the connection in the graph
                    graph.connect(node1, node2, name, time);

                    // Draw the connection on the map
                    drawConnection(place1, place2);

                    // Set unsaved changes flag
                    hasUnsavedChanges = true;

                    // Återställ valda cirklar
                    resetSelectedCircles();

                    // Återaktivera knappen
                    enableButton(newConnection);
                } else {
                    // Användaren klickade på Avbryt, återställ bara valen
                    resetSelectedCircles();

                    // Återaktivera knappen
                    enableButton(newConnection);
                }
            });
        }); //slut på newConnction

        //4.2.4 ShowConnection
        showConnection.setOnAction(event -> {
            // Inaktivera knapp när den klickas
            disableButton(showConnection);

            Place[] places = getSelectedPlaces();
            if (places == null) {
                enableButton(showConnection); // Återaktivera
                return;
            }

            String node1 = places[0].getName();
            String node2 = places[1].getName();

            // Kontrollera om en förbindelse finns mellan de valda platserna
            Edge<String> edge = graph.getEdgeBetween(node1, node2);
            if (edge == null) {
                showError("There is no connection between the selected places.");
                enableButton(showConnection); // Återaktivera knappen
                return;
            }

            // Create a dialog to show connection information
            javafx.scene.control.Dialog<ButtonType> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Connection");
            dialog.setHeaderText("Connection from " + node1 + " to " + node2);

            // Create the dialog content layout
            javafx.scene.control.DialogPane dialogPane = dialog.getDialogPane();
            ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            dialogPane.getButtonTypes().add(okButton);

            GridPane grid = new GridPane();
            grid.setHgap(10.0);
            grid.setVgap(10.0);
            grid.setPadding(new Insets(20, 150, 10, 10));

            // Create non-editable fields
            TextField nameField = new TextField(edge.getName());
            nameField.setEditable(false);
            TextField timeField = new TextField(Integer.toString(edge.getWeight()));
            timeField.setEditable(false);

            grid.add(new Label("Name:"), 0, 0);
            grid.add(nameField, 1, 0);
            grid.add(new Label("Time:"), 0, 1);
            grid.add(timeField, 1, 1);

            dialogPane.setContent(grid);

            // Visa dialogrutan
            dialog.showAndWait();

            // Återställ val efter att ha visat förbindelseinformation
            resetSelectedCircles();

            // Återaktivera knappen
            enableButton(showConnection);
        });

        //4.2.5 Funktionalitet för Ändra Förbindelse-knappen
        changeConnection.setOnAction(event -> {
            // Inaktivera knapp när den klickas
            disableButton(changeConnection);

            Place[] places = getSelectedPlaces();
            if (places == null) {
                enableButton(changeConnection); // Återaktivera om validering misslyckas
                return;
            }

            String node1 = places[0].getName();
            String node2 = places[1].getName();

            // Kontrollera om en förbindelse finns mellan de valda platserna
            Edge<String> edge = graph.getEdgeBetween(node1, node2);
            if (edge == null) {
                showError("There is no connection between the selected places.");
                enableButton(changeConnection); // Återaktivera knappen
                return;
            }

            // Create a dialog to edit connection time
            javafx.scene.control.Dialog<ButtonType> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Connection");
            dialog.setHeaderText("Connection from " + node1 + " to " + node2);

            // Create the dialog content layout
            javafx.scene.control.DialogPane dialogPane = dialog.getDialogPane();
            ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialogPane.getButtonTypes().addAll(okButton, cancelButton);

            GridPane grid = new GridPane();
            grid.setHgap(10.0);
            grid.setVgap(10.0);
            grid.setPadding(new Insets(20, 150, 10, 10));

            // Create fields: name (non-editable), time (editable)
            TextField nameField = new TextField(edge.getName());
            nameField.setEditable(false);
            TextField timeField = new TextField(); // Empty initially, as shown in the assignment

            grid.add(new Label("Name:"), 0, 0);
            grid.add(nameField, 1, 0);
            grid.add(new Label("Time:"), 0, 1);
            grid.add(timeField, 1, 1);

            dialogPane.setContent(grid);

            // Show the dialog and process the result
            dialog.showAndWait().ifPresent(response -> {
                if (response == okButton) {
                    // Validate and save the new time
                    String timeText = timeField.getText().trim();

                    try {
                        int newTime = Integer.parseInt(timeText);
                        if (newTime < 0) {
                            showError("Time must be a positive number.");
                            return;
                        }

                        // Update the connection weight in the graph
                        graph.setConnectionWeight(node1, node2, newTime);
                        hasUnsavedChanges = true;
                    } catch (NumberFormatException e) {
                        showError("Time must consist of digits only.");
                    }
                }

                // Återställ val efter ändring av förbindelseinformation
                resetSelectedCircles();

                // Återaktivera knappen
                enableButton(changeConnection);
            });
        });

        //4.2.6 Funktionalitet för Hitta Väg-knappen
        findPath.setOnAction(event -> {
            // Inaktivera knapp när den klickas
            disableButton(findPath);

            Place[] places = getSelectedPlaces();
            if (places == null) {
                enableButton(findPath); // Återaktivera om validering misslyckas
                return;
            }

            String node1 = places[0].getName();
            String node2 = places[1].getName();

            // Hitta en väg mellan de valda platserna
            List<Edge<String>> path = graph.getPath(node1, node2);

            if (path == null) {
                showError("There is no path between the selected places.");
                enableButton(findPath); // Återaktivera knappen
                return;
            }

            // Create a dialog to show the path information
            javafx.scene.control.Dialog<ButtonType> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Message");
            dialog.setHeaderText("The Path from " + node1 + " to " + node2 + ":");

            // Create the dialog content
            javafx.scene.control.DialogPane dialogPane = dialog.getDialogPane();
            ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            dialogPane.getButtonTypes().add(okButton);

            // Create content to display the path
            VBox content = new VBox(5);
            content.setPadding(new Insets(10));

            int totalTime = 0;
            String currentPlace = node1;

            // For each edge in the path
            for (Edge<String> edge : path) {
                String nextPlace = edge.getDestination();
                String connectionName = edge.getName();
                int time = edge.getWeight();

                totalTime += time;

                Label segment = new Label("to " + nextPlace + " by " + connectionName + " takes " + time);
                content.getChildren().add(segment);

                currentPlace = nextPlace;
            }

            // Add total time
            Label totalLabel = new Label("Total " + totalTime);
            content.getChildren().add(totalLabel);

            dialogPane.setContent(content);

            // Visa dialogrutan
            dialog.showAndWait();

            // Återställ val efter att ha visat väginformation
            resetSelectedCircles();

            // Återaktivera knappen
            enableButton(findPath);
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
            if (confirmDiscardChanges()){ //Frågetecken gällande dessa. Senaste ändringen bara. Bör kontro
                graph = new ListGraph<>();
                placeMap.clear();
                overlayPane.getChildren().clear();
                selectedCircles.clear();

            }

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
                primaryStage.setHeight(image.getHeight() + extraHeight); // Justera för menyer och knappar

                hasUnsavedChanges = true;

                enableAllButtons();
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

                enableAllButtons();
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
                    bw.write(imageFilePath); // Spara bildfilen
                    bw.newLine();
                    bw.write(createSaveStringOfGraphs()); // Spara platsinformation
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

    // Aktiverar en knapp och återställer muspekaren
    private void enableButton(Button buttonName) {
        buttonName.setDisable(false);
        overlayPane.setCursor(Cursor.DEFAULT);
        overlayPane.setOnMouseClicked(null); // Rensar eventhanterare
    }

    // Inaktiverar en knapp
    private void disableButton(Button buttonName) {
        buttonName.setDisable(true);
    }

    //Ny metod för att disablea alla knappar från start
    private void disableAllButtons(){
        disableButton(newPlace);
        disableButton(newConnection);
        disableButton(showConnection);
        disableButton(changeConnection);
        disableButton(findPath);
    }

    //Ny metod för att enablea efter map har öppnats , se punkt 4.2
    private void enableAllButtons(){
        enableButton(newPlace);
        enableButton(newConnection);
        enableButton(showConnection);
        enableButton(changeConnection);
        enableButton(findPath);
    }

    // Ny metod för att kontrollera om två platser är valda
    private boolean checkTwoPlacesSelected() {
        if (selectedCircles.size() != 2) {
            showError("Two places must be selected!");
            return false;
        }
        return true;
    }

    // Ny metod för att hämta de två valda platserna
    private Place[] getSelectedPlaces() {
        if (!checkTwoPlacesSelected()) {
            return null;
        }

        Place place1 = null;
        Place place2 = null;

        for (Map.Entry<String, Place> entry : placeMap.entrySet()) {
            Place place = entry.getValue();
            if (Math.abs(place.getX() - selectedCircles.get(0).getCenterX()) < 0.1 &&
                    Math.abs(place.getY() - selectedCircles.get(0).getCenterY()) < 0.1) {
                place1 = place;
            }
            if (Math.abs(place.getX() - selectedCircles.get(1).getCenterX()) < 0.1 &&
                    Math.abs(place.getY() - selectedCircles.get(1).getCenterY()) < 0.1) {
                place2 = place;
            }
        }

        if (place1 == null || place2 == null) {
            showError("Could not identify the selected places.");
            return null;
        }

        return new Place[]{place1, place2};
    }

    // Ny metod för att återställa valda cirklar
    private void resetSelectedCircles() {
        for (Circle circle : selectedCircles) {
            circle.setFill(Color.BLUE);
        }
        selectedCircles.clear();
    }

    // Laddar graf från fil
    private void loadGraphFromFile(File file) {
        try (Scanner scanner = new Scanner(file)) {
            String imagePath = scanner.nextLine();
            Image image = new Image(new File(imagePath).toURI().toString());
            imageView.setImage(image);
            imageView.setPreserveRatio(true);
            String graphString = scanner.nextLine();
            addPlacesFromFile(graphString);
            // Ändra inte bildens storlek, behåll ursprungliga dimensioner
            imageView.setFitWidth(0);
            imageView.setFitHeight(0);

            pane.getChildren().setAll(imageView, overlayPane);
            pane.setPrefSize(image.getWidth(), image.getHeight());

            hasUnsavedChanges = false;

        } catch (Exception e) {
            showError("Failed to load graph: " + e.getMessage());
        }
    }

    // Bekräftar om osparade ändringar ska kastas
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
                    showError("You can not mark more than two places at a time");
                }
            } else {
                // Avmarkera
                circle.setFill(Color.BLUE);
                selectedCircles.remove(circle);
            }
        });

        overlayPane.getChildren().add(circle); // Lägg till cirkeln i vyn
    }

    // Ritar en förbindelse (linje) mellan två platser
    private void drawConnection(Place from, Place to) {
        Line line = new Line(from.getX(), from.getY(), to.getX(), to.getY());
        line.setStroke(Color.BLACK);
        line.setStrokeWidth(2);
        // Lägg till linjen i overlayPane istället för pane, och på index 0 så den hamnar bakom cirklarna
        overlayPane.getChildren().add(0, line);
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