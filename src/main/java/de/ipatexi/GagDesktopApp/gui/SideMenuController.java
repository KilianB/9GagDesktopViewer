package de.ipatexi.GagDesktopApp.gui;

import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXToggleButton;

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import demos.gui.uicomponents.*;
import io.datafx.controller.ViewController;
import io.datafx.controller.flow.Flow;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.FlowHandler;
import io.datafx.controller.flow.FlowView;
import io.datafx.controller.flow.action.ActionTrigger;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import io.datafx.controller.util.VetoException;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;

import javax.annotation.PostConstruct;
import java.util.Objects;

@ViewController(value = "/fxml/SideMenu.fxml", title = "Material Design Example")
public class SideMenuController {

	@FXMLViewFlowContext
	private ViewFlowContext context;

	@FXML
	@ActionTrigger("hot")
	private Label hot;

	@FXML
	@ActionTrigger("trending")
	private Label trending;
	
	@FXML
	@ActionTrigger("fresh")
	private Label fresh;
	
	
	@FXML
	FontAwesomeIconView hotIcon;

	@FXML
	private JFXListView<Label> sideList;

	@FXML
	private JFXToggleButton nightModeToggle;

	/**
	 * init fxml when loaded.
	 */
	@PostConstruct
	public void init() {
		Objects.requireNonNull(context, "context");
		MemoryFlowHandler contentFlowHandler = (MemoryFlowHandler) context.getRegisteredObject("ContentFlowHandler");
		
		Label sectionLabel = (Label) context.getRegisteredObject("SectionLabel");
		
		
		sideList.propagateMouseEventsToParent();
		sideList.getSelectionModel().selectedItemProperty().addListener((o, oldVal, newVal) -> {
			if (newVal != null) {
				try {
					String id = newVal.getId();
					System.out.println("Handle: " + id);
					sectionLabel.setText(o.getValue().getText());
					
					//FlowView view = contentFlowHandler.getCurrentView();
					
					//FlowAction action = view.getActionById(id);
					
					if(id =="hot" || id == "trending") {
						contentFlowHandler.handle(id);
					}else if(id != "jumpToSection") {
						contentFlowHandler.handle(newVal.getId());
					}else {
						contentFlowHandler.handle("hot");
					}
					
				} catch (VetoException exc) {
					exc.printStackTrace();
				} catch (FlowException exc) {
					exc.printStackTrace();
				}
			}
//			new Thread(() -> {
//				Platform.runLater(() -> {
//					if (newVal != null) {
//						try {
//							String id = newVal.getId();
//							System.out.println("Handle: " + o.getValue().getText());
//							sectionLabel.setText(o.getValue().getText());
//							
//							//FlowView view = contentFlowHandler.getCurrentView();
//							
//							//FlowAction action = view.getActionById(id);
//							
//							if(id =="hot" || id == "trending") {
//								contentFlowHandler.handle(id);
//							}else if(id != "jumpToSection") {
//								contentFlowHandler.handle(newVal.getId());
//							}else {
//								contentFlowHandler.handle("hot");
//							}
//							
//						} catch (VetoException exc) {
//							exc.printStackTrace();
//						} catch (FlowException exc) {
//							exc.printStackTrace();
//						}
//					}
//				});
//			}).start();
		});

		
		nightModeToggle.selectedProperty().addListener(((observable, oldValue, newValue) -> {
			if (newValue) {
				setCSS("ViewerNight","ViewerDay");
			} else {
				setCSS("ViewerDay","ViewerNight");
			}
		}));
		
		Flow contentFlow = (Flow) context.getRegisteredObject("ContentFlow");
		bindNodeToController(hot, HotSectionController.class, contentFlow, contentFlowHandler);
		bindNodeToController(trending, HotSectionController2.class, contentFlow, contentFlowHandler);
		bindNodeToController(fresh, HotSectionController3.class, contentFlow, contentFlowHandler);
	}

	private void bindNodeToController(Node node, Class<?> controllerClass, Flow flow, FlowHandler flowHandler) {
		flow.withGlobalLink(node.getId(), controllerClass);
	}
	
	
	/**
	 * Appends a new stylesheet to the scene and optionally removes another one.
	 * The css files are searched for in the ressource subfolder css/NAME_OF_CSSFILE.css
	 * @param cssName	The name of the css file to be added to the scene
	 * @param removeCSS The name of the css file to be removed from the scene or null if no css shall be removed
	 */
	private void setCSS(String cssName,String removeCSS) {		
		Scene scene = nightModeToggle.getScene();
		ObservableList<String> stylesheets = scene.getStylesheets();
		if(removeCSS != null) {
			stylesheets.remove(getClass().getResource("/css/"+removeCSS+".css").toExternalForm());
		}
		stylesheets.add(getClass().getResource("/css/"+cssName+".css").toExternalForm());
	}

}
