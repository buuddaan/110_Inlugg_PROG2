// PROG2 VT2025, Inlämningsuppgift del 2
// Grupp 110
// Elvira Fröjd eljo2851
// Mathilda Wallen mawa6612
// Matilda Fahle mafa2209
// Sista innan inlämning!!

package se.su.inlupp;

//FX stuff
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
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
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

//För filhanteringen + IO
import javax.imageio.ImageIO;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Scanner;

//Util stuff
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Gui extends Application {

    //Datastruktur för att hantera grafen och kartvyn
    private Graph<String> graph = new ListGraph<>();
    private Pane overlayPane = new Pane(); // Transparent lager över bilden för koordinater
    private ImageView imageView = new ImageView();
    private StackPane pane = new StackPane(imageView, overlayPane);
    private List<Circle> selectedCircles = new ArrayList<>();

    private boolean hasUnsavedChanges = false;
    private String imageFilePath = "";
    private Map<String, Place> placeMap = new HashMap<>();
    private double extraHeight;

    private Button findPath;
    private Button showConnection;
    private Button newPlace;
    private Button newConnection;
    private Button changeConnection;

    private MenuBar menuBar;
    private Menu fileMenu;
    private MenuItem newMap, openMap, saveMap, saveImage, exit;

    @Override //För att överskrida en metod i application i java fx
    public void start(Stage primaryStage) {
        initializeUI();
        setupMenuBar();
        setupButtons();

        // Skapa scen och visa fönstret
        Scene scene = new Scene(createRootPane(), 640, 480);
        primaryStage.setScene(scene);
        primaryStage.setTitle("PathFinder");
        primaryStage.show();
        extraHeight = (primaryStage.getHeight() - primaryStage.getScene().getHeight()) + menuBar.getHeight() + 30; // För spacer och knappar

        // Stängning via exit-knappen
        primaryStage.setOnCloseRequest(event -> {
            if (!confirmDiscardChanges()) {
                event.consume(); // Avbryt stängning
            }
        });
    }

    //fixar fönstret
    private void initializeUI() {
        // En lyssnare som explicit ändrar överlappspanelens storlek
        imageView.boundsInParentProperty().addListener((obs, oldBounds, newBounds) -> {
            // Applicera exakta mått från ImageView till överlappspanelen
            overlayPane.setMaxWidth(newBounds.getWidth());
            overlayPane.setMaxHeight(newBounds.getHeight());
        });

        pane.setStyle("-fx-background-color: lightgray;");
        overlayPane.setStyle("-fx-background-color: transparent;");
        overlayPane.setPickOnBounds(false);
    }

    //meny och funktionalitet
    private void setupMenuBar() {
        menuBar = new MenuBar();
        fileMenu = new Menu("File");

        // Menyval
        newMap = new MenuItem("New Map");
        openMap = new MenuItem("Open Map");
        saveMap = new MenuItem("Save Map");
        saveImage = new MenuItem("Save Image");
        exit = new MenuItem("Exit");

        fileMenu.getItems().addAll(newMap, openMap, saveMap, saveImage, new SeparatorMenuItem(), exit);
        menuBar.getMenus().add(fileMenu);

        // Sätta upp händelsehanterare för menyobjekt
        newMap.setOnAction(new NewMapListener());
        openMap.setOnAction(new OpenMapListener());
        saveMap.setOnAction(new SaveMapListener());
        saveImage.setOnAction(new SaveImageListener());
        exit.setOnAction(new ExitListener());


    }
    // fixa knapparna o deras funktionalitet i en metod)
    private void setupButtons() {
        findPath = new Button("Find Path");
        showConnection = new Button("Show Connection");
        newPlace = new Button("New Place");
        newConnection = new Button("New Connection");
        changeConnection = new Button("Change Connection");

        disableAllButtons();

        // Sätta upp händelsehanterare för knappar
        findPath.setOnAction(new FindPathListener());
        showConnection.setOnAction(new ShowConnectionListener());
        newPlace.setOnAction(new NewPlaceListener());
        newConnection.setOnAction(new NewConnectionListener());
        changeConnection.setOnAction(new ChangeConnectionListener());
    }

    //root layot fixas här
    private BorderPane createRootPane() {
        BorderPane root = new BorderPane();

        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(findPath, showConnection, newPlace, newConnection, changeConnection);
        buttonBox.setAlignment(Pos.CENTER);

        Region spacer1 = new Region();
        spacer1.setMinHeight(10);
        Region spacer2 = new Region();
        spacer2.setMinHeight(10);

        VBox topSection = new VBox(menuBar, spacer1, buttonBox, spacer2);

        root.setTop(topSection);
        root.setCenter(pane);

        return root;
    }

    //findPath knapp
    private class FindPathListener implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent event) {
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
                return;
            }

            // Visa dialogruta me connection-info
            javafx.scene.control.Dialog<ButtonType> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Message");
            dialog.setHeaderText("The Path from " + node1 + " to " + node2 + ":");

            // Skapa dialogcontenta
            javafx.scene.control.DialogPane dialogPane = dialog.getDialogPane();
            ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            dialogPane.getButtonTypes().add(okButton);

            VBox content = new VBox(5);
            content.setPadding(new Insets(10));

            int totalTime = 0;
            String currentPlace = node1;

            // hitta varje edge i en path
            for (Edge<String> edge : path) {
                String nextPlace = edge.getDestination();
                String connectionName = edge.getName();
                int time = edge.getWeight();

                totalTime += time;

                Label segment = new Label(currentPlace + " to " + nextPlace + " by " + connectionName + " takes " + time + " hours");
                content.getChildren().add(segment);

                currentPlace = nextPlace;
            }

            // Visa total restid
            Label totalLabel = new Label("The total travel time is " + totalTime + " hours.");
            content.getChildren().add(totalLabel);

            dialogPane.setContent(content);

            // Visa dialogrutan
            dialog.showAndWait();

            // Återställ cirklar efter visat väginformation
            resetSelectedCircles();

            // Återaktivera knappen
            enableButton(findPath);
        }
    }

    // showConnection
    private class ShowConnectionListener implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent event) {
            // Inaktivera knapp när den klickas
            disableButton(showConnection);

            Place[] places = getSelectedPlaces();
            if (places == null) {
                enableButton(showConnection); // Återaktivera knappar
                return;
            }

            String node1 = places[0].getName();
            String node2 = places[1].getName();

            // Kontrollera om connection finns mellan valda platser, edge.
            Edge<String> edge = graph.getEdgeBetween(node1, node2);
            if (edge == null) {
                showError("There is no connection between the selected places.");
                return;
            }

            // visa information om connection
            javafx.scene.control.Dialog<ButtonType> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Connection");
            dialog.setHeaderText("Connection from " + node1 + " to " + node2);

            // skapa innehåll i dialogruta
            javafx.scene.control.DialogPane dialogPane = dialog.getDialogPane();
            ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            dialogPane.getButtonTypes().add(okButton);

            GridPane grid = new GridPane();
            grid.setHgap(10.0);
            grid.setVgap(10.0);
            grid.setPadding(new Insets(20, 150, 10, 10)); //padding vis sidor, framförallt för vi inte fick ovan och nedan.

            // skapa icke-edtibara tetfält
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
        }
    }

    // New Place
    private class NewPlaceListener implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent event) {
            // För rätt muspekare
            overlayPane.setCursor(Cursor.CROSSHAIR);
            // Inaktivera knappen
            disableButton(newPlace);
            if (!selectedCircles.isEmpty()) {
                resetSelectedCircles();
                // om man markerar två platser och sen klickar newPlace få vi en bugg man sen klickar på new Connection, newPlace knappen går ej att klicka på.
                // alternativ ha ett felmeddelande istället.
            }

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

                // Återställer muslyssnaren så att man inte råkar skapa flera
                overlayPane.setOnMouseClicked(null);
                enableButton(newPlace);
            });
        }
    }

    // NewConnection
    private class NewConnectionListener implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent event) {
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
                return;
            }

            //dialog för connectionrutan
            javafx.scene.control.Dialog<ButtonType> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Connection");
            dialog.setHeaderText("Connection from " + node1 + " to " + node2);

            // dialogpanelayout
            javafx.scene.control.DialogPane dialogPane = dialog.getDialogPane();

            ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            // Lägg till knapptyp i ordn OK, Cancel
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

            // Visa dialog och resultat
            Place place1 = places[0];
            Place place2 = places[1];
            dialog.showAndWait().ifPresent(response -> {
                if (response == okButton) {
                    String name = nameField.getText().trim();
                    String timeText = timeField.getText().trim();

                    if (name.isEmpty()) {
                        showError("Name can not be empty.");
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
                        showError("Time must consist of only numbers.");
                        return;
                    }

                    // connecta graf
                    graph.connect(node1, node2, name, time);

                    // rita connection på kartan
                    drawConnection(place1, place2);

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
        }
    }

    // ChangeConnection
    private class ChangeConnectionListener implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent event) {
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
                return;
            }

            //skapa dialogfönster och ändra tid mellan connections
            javafx.scene.control.Dialog<ButtonType> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("Connection");
            dialog.setHeaderText("Connection from " + node1 + " to " + node2);

            // layout
            javafx.scene.control.DialogPane dialogPane = dialog.getDialogPane();
            ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialogPane.getButtonTypes().addAll(okButton, cancelButton);

            GridPane grid = new GridPane();
            grid.setHgap(10.0);
            grid.setVgap(10.0);
            grid.setPadding(new Insets(20, 150, 10, 10));

            // färdmedel som inte går att ändra namnet på och tid som går att ändra
            TextField nameField = new TextField(edge.getName());
            nameField.setEditable(false);
            TextField timeField = new TextField();

            grid.add(new Label("Name:"), 0, 0);
            grid.add(nameField, 1, 0);
            grid.add(new Label("Time:"), 0, 1);
            grid.add(timeField, 1, 1);

            dialogPane.setContent(grid);

            // visa dialog och ge resultst
            dialog.showAndWait().ifPresent(response -> {
                if (response == okButton) {
                    // spara new tid
                    String timeText = timeField.getText().trim();

                    try {
                        int newTime = Integer.parseInt(timeText);
                        if (newTime < 0) {
                            showError("Time must be a positive number.");
                            return;
                        }

                        //uppdatera connection med vikten
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
        }
    }

    // NewMap
    private class NewMapListener implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent event) {
            if (confirmDiscardChanges()) {
                graph = new ListGraph<>();
                placeMap.clear();
                overlayPane.getChildren().clear();
                selectedCircles.clear();
            } else {
                return; // Avbryter om användaren inte vill förkasta ändringar
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose a map image");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            File selectedFile = fileChooser.showOpenDialog(pane.getScene().getWindow());
            if (selectedFile != null) {
                imageFilePath = selectedFile.getAbsolutePath();
                Image image = new Image(selectedFile.toURI().toString());
                stageToImageSize(image, (Stage) pane.getScene().getWindow());

                enableAllButtons();
            }
        }
    }


    //OpenMap
    private class OpenMapListener implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent event) {
            if (!confirmDiscardChanges()) return;

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose a graph file");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Graph files", "*.graph")
            );
            File selectedFile = fileChooser.showOpenDialog(pane.getScene().getWindow());
            if (selectedFile != null) {
                loadGraphFromFile(selectedFile);

                enableAllButtons();
            }
        }
    }

    //SaveMap
    private class SaveMapListener implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent event) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save graph");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Graph files", "*.graph")
            );
            File selectedFile = fileChooser.showSaveDialog(pane.getScene().getWindow());
            if (selectedFile != null) {
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(selectedFile))) {
                    bw.write("file:"+ imageFilePath); // Spara bildfilen
                    bw.newLine();
                    bw.write(createSaveStringOfGraphs()); // Spara platsinformation
                    bw.newLine();

                    List<String> connections = getConnectionsAsStrings();
                    for(String connection : connections) {
                        bw.write(connection);
                        bw.newLine();
                    }

                    hasUnsavedChanges = false;
                } catch (Exception e) {
                    showError("Failed to save graph: " + e.getMessage());
                }
            }
        }
    }

    //SaveImage
    private class SaveImageListener implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent event) {
            WritableImage image = pane.snapshot(new SnapshotParameters(), null);
            // Korrigerat enligt uppgiftsbeskrivningen 4.1.4 - spara på toppnivå i projektmappen
            File outputFile = new File("../capture.png"); //filen hamnar i prog2-inlupp-template/. Utan ../ hade den hamnat i GUI
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", outputFile);
            } catch (Exception ex) {
                showError("Failed to save image: " + ex.getMessage());
            }
        }
    }

    private class ExitListener implements EventHandler<ActionEvent> {
        @Override
        public void handle(ActionEvent event) {
            if (!confirmDiscardChanges()) return;
            ((Stage) pane.getScene().getWindow()).close();
        }
    }



    // Våra hjälpmetoder

    private void enableButton(Button buttonName) {
        buttonName.setDisable(false);
        overlayPane.setCursor(Cursor.DEFAULT);
        overlayPane.setOnMouseClicked(null); // Rensar eventhanterare
    }

    private void disableButton(Button buttonName) {
        buttonName.setDisable(true);
    }

    private void disableAllButtons() {
        disableButton(newPlace);
        disableButton(newConnection);
        disableButton(showConnection);
        disableButton(changeConnection);
        disableButton(findPath);
    }

    private void enableAllButtons() {
        enableButton(newPlace);
        enableButton(newConnection);
        enableButton(showConnection);
        enableButton(changeConnection);
        enableButton(findPath);
    }

    private boolean checkTwoPlacesSelected() {
        if (selectedCircles.size() != 2) {
            showError("Two places must be selected!");
            return false;
        }
        return true;
    }

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

    private void resetSelectedCircles() {
        for (Circle circle : selectedCircles) {
            circle.setFill(Color.BLUE);
        }
        selectedCircles.clear();
    }

    private void loadGraphFromFile(File file) {
        try (Scanner scanner = new Scanner(file)) {
            String imagePath = scanner.nextLine();
            String imageFilePath = imagePath.substring(imagePath.indexOf(":")+1);
            Image image = new Image(new File(imageFilePath).toURI().toString());
            imageView.setImage(image);
            imageView.setPreserveRatio(true);

            String graphString = scanner.nextLine();

            graph = new ListGraph<>();
            placeMap.clear();
            overlayPane.getChildren().clear();
            selectedCircles.clear();

            addPlacesFromFile(graphString);

            while (scanner.hasNextLine()){
                String connectionString = scanner.nextLine();
                String [] parts = connectionString.split(";");
                if(parts.length == 4){
                    String from = parts[0];
                    String to = parts[1];
                    String name = parts[2];
                    int weight = Integer.parseInt(parts[3]);

                    if (graph.getEdgeBetween(from,to) == null){
                        graph.connect(from,to,name,weight);

                        Place fromPlace = placeMap.get(from);
                        Place toPlace = placeMap.get(to);
                        if (fromPlace != null && toPlace != null) {
                            drawConnection(fromPlace,toPlace);
                        }
                    }
                }
            }
            stageToImageSize(image, (Stage) pane.getScene().getWindow());

            hasUnsavedChanges = false;

        } catch (Exception e) {
            showError("Failed to load graph: " + e.getMessage());
        }
    }

    private boolean confirmDiscardChanges() {
        if (!hasUnsavedChanges) return true;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Warning");
        alert.setHeaderText("There are unsaved changes.");
        alert.setContentText("Do you want to discard and continue anyway?");
        ButtonType okButton = new ButtonType("OK, Discard changes");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(okButton, cancelButton);
        return alert.showAndWait().orElse(cancelButton) == okButton;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        enableAllButtons();

        alert.setTitle("Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.showAndWait();
        enableAllButtons();
    }

    private void drawPlace(Place place) {
        Circle circle = new Circle(place.getX(), place.getY(), 8);
        circle.setFill(Color.BLUE);
        circle.setStroke(Color.BLACK);

        Tooltip tooltip = new Tooltip(place.getName());
        Tooltip.install(circle, tooltip);

        circle.setOnMouseClicked(new PlaceClickListener(circle));

        overlayPane.getChildren().add(circle); // Lägg till cirkeln i vyn
    }


    // Inre klass för att hantera klick på platser

    private class PlaceClickListener implements EventHandler<javafx.scene.input.MouseEvent> {
        private Circle circle;

        public PlaceClickListener(Circle circle) {
            this.circle = circle;
        }

        @Override
        public void handle(javafx.scene.input.MouseEvent event) {
            if (circle.getFill().equals(Color.BLUE)) {

                // Om färgen är blå och max 2 ej redan valda
                if (selectedCircles.size() < 2) {
                    circle.setFill(Color.RED);
                    selectedCircles.add(circle);
                } else {
                    // Visa felmeddelande eller ignorera klicket
                    showError("You can not mark more than two places at a time");
                    //Försvar: Snyggare med tydlighet för användaren. Tekniskt sett händer inget annat än felmeddelande, kreativ frihet osv :D
                }
            } else {
                // Avmarkera cirkel
                circle.setFill(Color.BLUE);
                selectedCircles.remove(circle);
            }
        }
    }

    private void drawConnection(Place from, Place to) {
        Line line = new Line(from.getX(), from.getY(), to.getX(), to.getY());
        line.setStroke(Color.BLACK);
        line.setStrokeWidth(2);
        // Lägg till linjen i overlayPane istället för pane, och på index 0 så den hamnar bakom cirklarna
        overlayPane.getChildren().add(0, line);
    }

    private void addPlacesFromFile(String stringOfPlaces) {
        List<Place> placesToAdd = new ArrayList<>();
        String[] values = stringOfPlaces.split(";");
        String city;
        for (int i = 0; i < values.length; i += 3) {
            city = values[i];
            double x = Double.parseDouble(values[i + 1]);
            double y = Double.parseDouble(values[i + 2]);
            placesToAdd.add(new Place(city, x, y));
        }
        for (Place p : placesToAdd) {
            graph.add(p.getName());
            placeMap.put(p.getName(), p);
            drawPlace(p);
        }
    }

    private String createSaveStringOfGraphs() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Place> entry : placeMap.entrySet()) {
            sb.append(entry.getValue().toString());
            sb.append(";"); // lägg till ett ; efter varje
        }
        // ta bort sista ;
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    private List<String> getConnectionsAsStrings() {
        List<String> connections = new ArrayList<>();
        for(String nodeName : graph.getNodes()){
            Collection<Edge<String>> edges = graph.getEdgesFrom(nodeName);

            for(Edge<String> edge : edges) {
                String destinationNode = edge.getDestination();
                String connectionsString = nodeName + ";" + destinationNode + ";" +
                        edge.getName() + ";" + edge.getWeight() ;
                connections.add(connectionsString);
            }
        }
        return connections;
    }

    private void stageToImageSize(Image image, Stage stage) {
        imageView.setImage(image);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(image.getWidth());
        imageView.setFitHeight(image.getHeight());

        pane.getChildren().setAll(imageView, overlayPane);
        pane.setPrefSize(image.getWidth(), image.getHeight());

        stage.setWidth(image.getWidth());
        stage.setHeight(image.getHeight() + extraHeight);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

