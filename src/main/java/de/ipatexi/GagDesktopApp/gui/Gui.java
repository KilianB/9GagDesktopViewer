package de.ipatexi.GagDesktopApp.gui;

import com.github.kilianB.matcher.SingleImageMatcher;
import com.jfoenix.controls.JFXDecorator;
import com.jfoenix.svg.SVGGlyph;
import com.jfoenix.svg.SVGGlyphLoader;
import io.datafx.controller.flow.Flow;
import io.datafx.controller.flow.container.DefaultFlowContainer;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class Gui extends Application {

	@FXMLViewFlowContext
	private ViewFlowContext flowContext;

	public static void main(String[] args) {
		launch(args);
	}
	
	
	//TODO @BackAction für stats page
	
 
	@Override
	public void start(Stage stage) throws Exception {
		new Thread(() -> {
			try {
				SVGGlyphLoader.loadGlyphsFont(Gui.class.getResourceAsStream("fonts/icomoon.svg"), "icomoon.svg");
			} catch (IOException ioExc) {
				ioExc.printStackTrace();
			}
		}).start();

		
		double width = 800;
		double height = 600;
		try {
			Rectangle2D bounds = Screen.getScreens().get(0).getBounds();
			width = bounds.getWidth() / 2;
			height = bounds.getHeight() / 1.1;
		} catch (Exception e) {
		}

		DefaultFlowContainer container = new DefaultFlowContainer();
		JFXDecorator decorator = new JFXDecorator(stage, container.getView());
		decorator.setCustomMaximize(true);
		decorator.setGraphic(new SVGGlyph(""));
		
		Scene scene = new Scene(decorator, width, height);

		SingleImageMatcher imageMatcher = SingleImageMatcher.createDefaultMatcher();
		
		Flow flow = new Flow(MainController.class);
		
		flowContext = new ViewFlowContext();
		flowContext.register("Stage", stage);
		flowContext.register("Scene",scene);
		flow.createHandler(flowContext).start(container);

		

		stage.setTitle("9 Gag Desktop App");

		final ObservableList<String> stylesheets = scene.getStylesheets();
		stylesheets.addAll(
				Gui.class.getResource("/css/jfoenix-main-demo.css").toExternalForm(),
				Gui.class.getResource("/css/ViewerDay.css").toExternalForm(),
				Gui.class.getResource("/css/Main.css").toExternalForm()
				);
		stage.setScene(scene);
		stage.show();
	}

}
