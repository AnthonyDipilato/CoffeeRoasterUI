/*
 *          Coffee Roaster
 *      Written by Anthony DiPilato
 *      
 *      This project is the user interface for the coffee roaster that I built
 *      You can see the build log details at [url]
 *      
 *      The UI runs on a Raspberry Pi connected to an arduino via USB
 *      The arduino acts as slave to the Raspberry Pi
 *         
 *      USE AT YOUR OWN RISK
 *      I am posting this project for educational use only.
 *      This project involves, electricity, moving parts, propane, and fire.
 *      I will not be held liable for damages and/or injuries resulting from the use of this code
 *      or from reproducing this project.
 *
 *
 *      Commands and adresses for arduino
 *      Commands
 *      0   -   Status
 *      1   -   Relay on
 *      2   -   Relay off
 *      3   -   Set proportional valve
 *
 *      Addresses
 *      0   -   All
 *      1   -   Drum Temperature
 *      2   -   Chamber Temperature
 *      3   -   Exhausr Temperature
 *      4   -   Flame Status
 *      5   -   Drum Relay
 *      6   -   Cooling Relay
 *      7   -   Exhaust Relay
 *      8   -   Gas Relay
 *      9   -   Ignitor
 *      10  -   Proportional Valve
 *  
 */
package roasterui;

import eu.hansolo.colors.MaterialDesign;
import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.GaugeBuilder;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;


/**
 *
 * @author anthonydipilato
 */
public class RoasterUI extends Application {
    // Gauges
    private         Gauge    chamber;
    private         Gauge    exhaust;
    private         Gauge    drum;
    // Panes
    private         GridPane pane;
    private         GridPane anchorPane;
    // Chart
    private         LineChart lineChart;
    private         CategoryAxis xAxis;
    private         NumberAxis yAxis;
    private         XYChart.Series drumSeries;
    private         XYChart.Series exhaustSeries;
    private         XYChart.Series chamberSeries;
    private         int seriesPoint;
    // Timer
    private         Label clockDisplay;
    private         DateFormat timeFormat;
    // Slider Label
    private         Button proValveValue;
    // Timer
    private         Timeline timer;
    private         long timerStart = 0;
    private         long timerCurrent;
    private         long timerResumed = 0;
    private         boolean timerStatus;
    private         long lastLog = 0;
    // Control Buttons
    private         ToggleButton ignitorBtn;
    private         ToggleButton gasBtn;
    private         ToggleButton drumBtn;
    private         ToggleButton exhaustBtn;
    private         ToggleButton coolingBtn;
    private         Button flameBtn;
    // Status Variables
    private         boolean ignitorStatus = false;
    private         boolean gasStatus = false;
    private         boolean exhaustStatus = false;
    private         boolean coolingStatus = false;
    private         boolean drumStatus = false;
    private         boolean flameStatus = false;
    private         int     proValve = 0;
    // Temperature Variables
    private         int drumTemp = 0;
    private         int chamberTemp = 0;
    private         int exhaustTemp = 0;
    // log array
    private         ArrayList<Log> logData;
    // Arduino Connection
    private ArduinoSerial arduino;
    
    @Override public void init(){
        logData = new ArrayList ();
        timeFormat = new SimpleDateFormat( "mm:ss.S" );
        
        // Create a pane for gauges
        HBox gauges = addGauges();
        
        // Vbox container for Buttons
        VBox btnBox = addBtnBox();
        // Vbox for slider below gauges
        VBox sliderBox = addSliderBox();
        // Log Button Box
        VBox logBtnBox = logBtnBox();

        
        // Setup Chart
        xAxis = new CategoryAxis();
        yAxis = new NumberAxis();
        xAxis.setLabel("Time");
        yAxis.setLabel("Temperature");
        // Set Y axis range so it doesn't bounce as new data is added
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(750);
        yAxis.setTickUnit(100);
        
        //create the chart
        lineChart = new LineChart(xAxis,yAxis);
        lineChart.setAnimated(false);
        seriesPoint = 0;
        
        // Set up data series for chart temps
        drumSeries = new XYChart.Series();
        exhaustSeries = new XYChart.Series();
        chamberSeries = new XYChart.Series();
        // Series display names
        drumSeries.setName("Drum");
        chamberSeries.setName("Exhaust");
        exhaustSeries.setName("Chamber");
        // Initial data for series
        drumSeries.getData().add(new XYChart.Data("00:00", 0));
        chamberSeries.getData().add(new XYChart.Data("00:00", 0));
        exhaustSeries.getData().add(new XYChart.Data("00:00", 0)); 
        
        // Add Chart Series
        lineChart.getData().addAll(drumSeries, chamberSeries, exhaustSeries);
        lineChart.getStyleClass().add("chart");
        
        
        //  Flame Status
        VBox flameBox = new VBox();
        flameBox.setPadding(new Insets(20));
        flameBox.setSpacing(20);
        Label flameLabel = new Label("Flame Status");
        flameBtn = new Button("OFF");
        flameLabel.getStyleClass().add("flameLabel");
        flameBtn.getStyleClass().add("flameOff");
        flameBox.getChildren().add(flameLabel);
        flameBox.getChildren().add(flameBtn);
        
        
        pane = new GridPane();
        pane.setPadding(new Insets(20));
        pane.setHgap(10);
        pane.setVgap(15);
        pane.setBackground(new Background(new BackgroundFill(MaterialDesign.GREY_900.get(), CornerRadii.EMPTY, Insets.EMPTY)));
        
        
        
        pane.add(gauges,       0, 0);
        
        // Add Button Box
        pane.add(btnBox,        1, 0, 1, 2);
        // Add Slider box to pane
        pane.add(sliderBox,     0, 1, 1, 1);
        // Flame box
        pane.add(flameBox,     1, 1);
        
        // Grid lines
        pane.setGridLinesVisible(true);
        
        
        // Add Linechart to pane
        pane.add(lineChart,     0, 2, 1, 1);
        // Add Log Buttons
        pane.add(logBtnBox,     1, 2, 1, 1);
        

        
        
        
        // Anchor Pane to center Items
        anchorPane = new GridPane();
        anchorPane.setPadding(new Insets(20));
        anchorPane.add(pane,0,0);
        anchorPane.setBackground(new Background(new BackgroundFill(MaterialDesign.GREY_900.get(), CornerRadii.EMPTY, Insets.EMPTY)));
        anchorPane.setAlignment(Pos.CENTER);

    }
    
    
    private HBox addGauges(){
        HBox hbox = new HBox();
        hbox.setPadding(new Insets(10));
        hbox.setSpacing(8);
        // Build Gauges
        GaugeBuilder builder = GaugeBuilder.create()
                                           .skinType(Gauge.SkinType.SLIM)
                                           .barBackgroundColor(MaterialDesign.GREY_800.get())
                                           .animated(true)
                                          .animationDuration(750);
        chamber         = builder.decimals(0).maxValue(700).unit("Chamber").build();
        exhaust         = builder.decimals(0).maxValue(700).unit("Exhaust").build();
        drum            = builder.decimals(0).maxValue(700).unit("Drum").build();
        // Vboxes to build gauges
        VBox chamberBox        = getVBox("Chamber Temp", MaterialDesign.RED_300.get(), chamber);
        VBox exhaustBox     = getVBox("Exhaust Temp", MaterialDesign.ORANGE_300.get(), exhaust);
        VBox drumBox = getVBox("Drum Temp", MaterialDesign.CYAN_300.get(), drum);

        hbox.getChildren().add(chamberBox);
        hbox.getChildren().add(exhaustBox);
        hbox.getChildren().add(drumBox);
        
        return hbox;
    }
    
    // Gas Slider Box
    private VBox addSliderBox(){
        HBox hbox = new HBox();
        hbox.setPadding(new Insets(10));
        hbox.setSpacing(8);
        // slider
        Slider gasSlider = new Slider();
        gasSlider.setMin(0);
        gasSlider.setMax(100);
        gasSlider.setValue(0);
        gasSlider.getStyleClass().add("slider");
        // Set Button
        Button setBtn = new Button("Set");
        setBtn.getStyleClass().add("setBtn");
        // Provalve value
        proValveValue = new Button(Integer.toString(proValve) + "%");
        proValveValue.getStyleClass().add("proValve");
        proValveValue.setTextAlignment(TextAlignment.RIGHT);
        
        
        
        hbox.getChildren().add(gasSlider);
        hbox.getChildren().add(setBtn);
        hbox.getChildren().add(proValveValue);
        
        // Set listener
        setBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                proValve = (int) gasSlider.getValue();
                proValveValue.setText(Integer.toString(proValve) + "%");
                arduino.send(3, proValve); // command address for provalve 3
            }
        });
        // VBox for label slider
        VBox vbox = new VBox();
        vbox.setPadding(new Insets(10));
        vbox.setSpacing(8);
        Label sliderLabel = new Label("Proportional Valve");
        sliderLabel.getStyleClass().add("slider-label");
        vbox.getChildren().add(sliderLabel);
        vbox.getChildren().add(hbox);
        return vbox;
    }
    
    
    // Logging Buttons
    private VBox logBtnBox(){
        VBox vbox = new VBox();
        vbox.setPadding(new Insets(20));
        vbox.setSpacing(20);
        
        // Buttons
        ToggleButton logBtn = new ToggleButton("Logging");
        Button firstBtn = new Button("First Crack");
        Button secondBtn = new Button("Second Crack");
        Button dumpBtn = new Button("Dump");
        Button saveBtn = new Button("Save Log");
        Button resetBtn = new Button("Reset");
        Button quitBtn = new Button("Exit");
        
        logBtn.getStyleClass().add("tBtn");
        firstBtn.getStyleClass().add("tBtn");
        secondBtn.getStyleClass().add("tBtn");
        dumpBtn.getStyleClass().add("tBtn");
        saveBtn.getStyleClass().add("tBtn");
        resetBtn.getStyleClass().add("tBtn");
        quitBtn.getStyleClass().add("tBtn");
        
        // Timer
        clockDisplay = new Label("00:00.00");
        Font.loadFont(getClass().getResourceAsStream("/resources/fonts/liquid_crystal/LiquidCrystal-Normal.otf"), 24);
        clockDisplay.getStyleClass().add("clock");
        
        // Listeners for buttons
        logBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                toggleTimer();
            }
        });
        resetBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                timerStart = System.currentTimeMillis();
                timerResumed = 0;
                clockDisplay.setText( timeFormat.format(0) );
                logData.clear();
            }
        });
        
        saveBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                saveLog();
            }
        });
        
        quitBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                stop();
            }
        });
        
        vbox.getChildren().add(clockDisplay);
        vbox.getChildren().add(logBtn);
        vbox.getChildren().add(resetBtn);
        vbox.getChildren().add(firstBtn);
        vbox.getChildren().add(secondBtn);
        vbox.getChildren().add(dumpBtn);
        vbox.getChildren().add(saveBtn);
        vbox.getChildren().add(quitBtn);
        
        
        return vbox;
    }
    
    
    
    // Toggle Button Box
    private VBox addBtnBox(){
        VBox vbox = new VBox();
        vbox.setPadding(new Insets(20));
        vbox.setSpacing(20);
        
        // Initialize toggle Buttons
        ignitorBtn = new ToggleButton("Ignitor");
        gasBtn = new ToggleButton("Gas Valve");
        drumBtn = new ToggleButton("Drum");
        exhaustBtn = new ToggleButton("Exhaust Fan");
        coolingBtn = new ToggleButton("Cooling Fan");
        
        // Add CSS styles
        ignitorBtn.getStyleClass().add("tBtn");
        gasBtn.getStyleClass().add("tBtn");
        drumBtn.getStyleClass().add("tBtn");
        exhaustBtn.getStyleClass().add("tBtn");
        coolingBtn.getStyleClass().add("tBtn");
        
        //Button Actions
        ignitorBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                setItem("ignitor");
            }
        });
        gasBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                setItem("gas");
            }
        });
        drumBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                setItem("drum");
            }
        });
        exhaustBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                setItem("exhaust");
            }
        });
        coolingBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                setItem("cooling");
            }
        });
        
        // Add buttons to vbox
        vbox.getChildren().add(ignitorBtn);
        vbox.getChildren().add(gasBtn);
        vbox.getChildren().add(drumBtn);
        vbox.getChildren().add(exhaustBtn);
        vbox.getChildren().add(coolingBtn);
        

        return vbox;
    }
    
    private void setItem(String item){
        int command; int address; boolean status = false;
        switch (item){
            case "ignitor":
                ignitorStatus = !ignitorStatus; 
                status = ignitorStatus;
                address = 9;
                break;
            case "gas":
                gasStatus = !gasStatus; 
                status = gasStatus;
                address = 8;
                break;
            case "exhaust":
                exhaustStatus = !exhaustStatus; 
                status = exhaustStatus;
                address = 7;
                break;
            case "cooling":
                coolingStatus = !coolingStatus;
                status = coolingStatus;
                address = 6;
                break;
            case "drum":
                drumStatus = !drumStatus;
                status = drumStatus;
                address = 5;
                break;
            default:
                return;
        }
        command = (status) ? 1 : 2; // commands: 1 - relay on, 2 - relay off
        arduino.send(command, address);
    }
    
    private VBox getVBox(final String TEXT, final Color COLOR, final Gauge GAUGE) {
        Rectangle bar = new Rectangle(200, 3);
        bar.setArcWidth(6);
        bar.setArcHeight(6);
        bar.setFill(COLOR);

        Label label = new Label(TEXT);
        label.setTextFill(COLOR);
        label.setAlignment(Pos.CENTER);
        label.setPadding(new Insets(0, 0, 10, 0));

        GAUGE.setBarColor(COLOR);

        VBox vBox = new VBox(bar, label, GAUGE);
        vBox.setSpacing(3);
        vBox.setAlignment(Pos.CENTER);
        return vBox;
    }
    
    @Override
    public void start(Stage stage) {
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        
        Scene scene = new Scene(anchorPane);
        scene.getStylesheets().add("resources/RoasterUIStyle.css"); 
        
        stage.setTitle("Coffee Roaster v3.0");
        stage.setScene(scene);
        
        stage.setX(primaryScreenBounds.getMinX()-100);
        stage.setY(primaryScreenBounds.getMinY()-100);
        stage.setWidth(primaryScreenBounds.getWidth()+100);
        stage.setHeight(primaryScreenBounds.getHeight()+100);
        stage.setFullScreen(true);
        stage.show();

        // Timer display loop
        timerSetup();
        // Loop for sending status commands to arduino
        statusLoop();
        // Loop for checking responses from arduino
        queueLoop();
    }
    

    
    private void statusLoop(){
        arduino = new ArduinoSerial();
        arduino.connect();
         int statusFreq = 1000;
        Timeline statusTimer = new Timeline(
            new KeyFrame(Duration.millis(statusFreq), event -> {
                arduino.send(0,0);
            })
        );
        statusTimer.setCycleCount(Timeline.INDEFINITE);
        statusTimer.play();
        
        
    }
    
    
    public void processResponse(String response){
        String[] parts = response.split(",");
        // declare vars for readability
        int command = Integer.parseInt(parts[0]);
        int address = Integer.parseInt(parts[1]);
        int value;
        if (command != 0){
            System.out.println("Command: "+response);
            return;
        }
        // response commands should always be 0
        value = Integer.parseInt(parts[2]);
        
        switch (address) {
            // Drum Temp
            case 1:
                drumTemp = value;
                drum.setValue(drumTemp);
                break;
            // Chamber Temp
            case 2:
                chamberTemp = value;
                chamber.setValue(chamberTemp);
                break;
            // Exhaust Temp
            case 3:
                exhaustTemp = value;
                exhaust.setValue(exhaustTemp);
                break;
            // Flame Status
            case 4:
                if(flameStatus && value == 0){
                    flameStatus = false;
                    flameBtn.setText("OFF");
                    flameBtn.getStyleClass().removeAll("flameOn");
                    flameBtn.getStyleClass().add("flameOff");
                }
                if(!flameStatus && value == 1){
                    flameStatus = true;
                    flameBtn.setText("ON");
                    flameBtn.getStyleClass().removeAll("flameOff");
                    flameBtn.getStyleClass().add("flameOn");
                }
                break;
            // Drum Motor
            case 5:
                if(drumStatus && value == 0){
                    drumStatus = false;
                    drumBtn.setSelected(false);
                }
                if(!drumStatus && value == 1){
                    drumStatus = true;
                    drumBtn.setSelected(true);
                }
                break;
            // Cooling Fan
            case 6:
                if(coolingStatus && value == 0){
                    coolingStatus = false;
                    coolingBtn.setSelected(false);
                }
                if(!coolingStatus && value == 1){
                    coolingStatus = true;
                    coolingBtn.setSelected(true);
                }
                break;
            // Exhaust Fan
            case 7:
                if(exhaustStatus && value == 0){
                    exhaustStatus = false;
                    exhaustBtn.setSelected(false);
                }
                if(!exhaustStatus && value == 1){
                    exhaustStatus = true;
                    exhaustBtn.setSelected(true);
                }
                break;
            // Gas Valve
            case 8:
                if(gasStatus && value == 0){
                    gasStatus = false;
                    gasBtn.setSelected(false);
                }
                if(!gasStatus && value == 1){
                    gasStatus = true;
                    gasBtn.setSelected(true);
                }
                break;
            // Ignitor Status
            case 9:
                if(ignitorStatus && value == 0){
                    ignitorStatus = false;
                    ignitorBtn.setSelected(false);
                }
                if(!ignitorStatus && value == 1){
                    ignitorStatus = true;
                    ignitorBtn.setSelected(true);
                }
                break;
            // Proportional Valve
            case 10:
                if (value != proValve){
                    proValve = value;
                    proValveValue.setText(Integer.toString(proValve) + "%");
                }
                break;
        }
        
    }
    
    public void queueLoop(){
        int queueFreq = 250;
        Timeline queueTimeline = new Timeline(
            new KeyFrame(Duration.millis(queueFreq), event -> {
                checkQueue();
            })
        );
        queueTimeline.setCycleCount(Timeline.INDEFINITE);
        queueTimeline.play();
    }
    
    
    private void checkQueue(){
        while (arduino.commandQueue.size() != 0){
            String response = arduino.checkQueue();
            processResponse(response);
        }
    }
    
    private void timerSetup() {
        int logFreq = 1000;
        timer = new Timeline(
            new KeyFrame(Duration.millis(10), event -> {
                // update timer
                timerCurrent = System.currentTimeMillis() - timerStart + timerResumed;
                clockDisplay.setText( timeFormat.format(timerCurrent) );
                if (System.currentTimeMillis() - lastLog > logFreq) {
                    logTemps();
                    lastLog = System.currentTimeMillis();
                }
            })
        );
        timer.setCycleCount(Timeline.INDEFINITE);
        //timer.play();
        timerStatus = false;
    }
    
    private void logTemps(){
        Log logItem = new Log();
        logItem.timestamp = timerCurrent;
        
        String xValue = String.format("%02d:%02d", 
        TimeUnit.MILLISECONDS.toMinutes(timerCurrent) -  
        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timerCurrent)), // The change is in this line
        TimeUnit.MILLISECONDS.toSeconds(timerCurrent) - 
        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timerCurrent)));  
        
        logItem.chamberTemp = chamberTemp;
        chamberSeries.getData().add(new XYChart.Data(xValue, logItem.chamberTemp));
        
        logItem.exhaustTemp = exhaustTemp;
        exhaustSeries.getData().add(new XYChart.Data(xValue, logItem.exhaustTemp));

        logItem.drumTemp = drumTemp;
        drumSeries.getData().add(new XYChart.Data(xValue, logItem.drumTemp));
        
        logItem.ignitorStatus = ignitorStatus;
        logItem.gasStatus = gasStatus;
        logItem.exhaustStatus = exhaustStatus;
        logItem.coolingStatus = coolingStatus;
        logItem.proValve = proValve;
        logData.add(logItem);

        seriesPoint++;
        if(seriesPoint > 15){
            chamberSeries.getData().remove(0);
            exhaustSeries.getData().remove(0);
            drumSeries.getData().remove(0);
            // update
            //xAxis.setAutoRanging(false);
            //xAxis.setLowerBound(seriesPoint - 11);
            //xAxis.setUpperBound(seriesPoint);
        }
    }
    
    private void toggleTimer(){
        if (!timerStatus) {
                timerStart = System.currentTimeMillis();
                timer.play();
                timerStatus = true;
            }else{
                timer.stop();
                timerResumed = timerCurrent;
                timerStatus = false;
            }  
    }
    
    private void saveLog(){
        String output = "Timestamp, Drum Temp, Chamber Temp, Exhaust Temp, Provalve \n";
        for (Log item : logData) {
            output += item.timestamp+", "+item.drumTemp+", "+item.chamberTemp+", "+item.exhaustTemp+", "+item.proValve+" \n";
        }
        DateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd-HH-mm" );
        String filename = "/Development/Roast-" + dateFormat.format(System.currentTimeMillis()) + ".csv";
        try{
            PrintWriter writer = new PrintWriter(filename, "UTF-8");
            writer.print(output);
            writer.close();
        } catch (IOException e) {
           // do something
        }
    }
    
    
    @Override public void stop() {
        arduino.close();
        System.exit(0);
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
