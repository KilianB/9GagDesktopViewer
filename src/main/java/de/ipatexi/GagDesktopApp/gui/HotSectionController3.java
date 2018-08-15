package de.ipatexi.GagDesktopApp.gui;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.PostConstruct;

import de.ipatexi.GagDesktopApp.util.BidirectionalHashMap;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import io.datafx.controller.ViewController;
import io.datafx.controller.flow.FlowHandler;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

@ViewController(value = "/fxml/ui/PostSection.fxml", title = "9 Gag Desktop")
public class HotSectionController3 {

	@FXMLViewFlowContext
	private ViewFlowContext context;
	
	@FXML
	private ScrollPane scrollPane;

	@FXML
	private VBox backgroundPane;
	
	@FXML
	private ImageView backgroundImage;
	
	private BidirectionalHashMap<Node,Integer> nodePositionLookupMap = new BidirectionalHashMap<>();
	private ObservableList<Node> itemsInScrollPane;
	
	/**
	 * Animatation
	 */
	boolean animationInProcess = false;
	
	
	@PostConstruct 
	public void init() throws IOException {
		
		Objects.requireNonNull(context, "context");
		MemoryFlowHandler contentFlowHandler = (MemoryFlowHandler) context.getRegisteredObject("ContentFlowHandler");
		

		//Remove focus border 
		scrollPane.getStyleClass().add("edge-to-edge");
		
		itemsInScrollPane = backgroundPane.getChildrenUnmodifiable();
		
		itemsInScrollPane.addListener(new ListChangeListener<Node>() {

			@Override
			public void onChanged(Change<? extends Node> c) {
				while(c.next()) {
					c.getAddedSubList().stream()
					.forEach( node -> nodePositionLookupMap.put(node, nodePositionLookupMap.size()));
				}
			}
		});
		
		
		scrollPane.vvalueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue)->{
			//Be careful of time complexity
			if(!animationInProcess) {
				Optional<Node> firstVisibleItem = itemsInScrollPane.stream().filter(node -> scrollPane.sceneToLocal(node.localToScene(node.getBoundsInLocal())).getMinY() > 0).findFirst();
				if(firstVisibleItem.isPresent()) {
					currentItem = nodePositionLookupMap.get(firstVisibleItem.get());
				}else {
					System.out.println("No first visible");
				}
			}
		});
		
		
		backgroundImage.setImage(new Image("/img/helpBlack.png"));
		
		
		
		
		Platform.runLater(()->{
			Scene scene = (Scene) context.getRegisteredObject("Scene");
			scene.setOnKeyReleased((KeyEvent e)->{
				
				switch(e.getCode()) {
					case W: 
						scrollUp();
						break;
					case A:
						break;
					case S:
						scrollDown();
						break;
					case D:
						break;
					default:
						break;
				}
			});
		});		
	}

	// Scrolling

	private int currentItem = 0;
	private boolean firstItemAdded = false;

	public void scrollUp() {
		System.out.println("Scroll Up");
		currentItem--;
		if (currentItem < 0) {
			currentItem = 0;
			//If no item is present do nothing.
			if( backgroundPane.getChildrenUnmodifiable().size() == 0 || !firstItemAdded)
				return;
		}
		// Get item
		Node item = backgroundPane.getChildrenUnmodifiable().get(currentItem);
		centerNodeInScrollPane(scrollPane,item);
	}
	
	public void scrollDown() {
		currentItem++;
		if(currentItem >= nodePositionLookupMap.size()) {
			addImage(new Image("test.jpg"),"Random Title");
		}else {
			Node nodeToScrollTo = nodePositionLookupMap.getKey(currentItem);
			centerNodeInScrollPane(scrollPane,nodeToScrollTo);
		}
	}

	
	private void centerNodeInScrollPane(ScrollPane scrollPane, Node node) {
		double h = scrollPane.getContent().getBoundsInLocal().getHeight();
		double y = (node.getBoundsInParent().getMaxY() + node.getBoundsInParent().getMinY()) / 2.0;
		double v = scrollPane.getViewportBounds().getHeight();
		double newVValue = scrollPane.getVmax() * ((y - 0.5 * v) / (h - v));
		Animation anim = new Timeline(new KeyFrame(Duration.millis(300), new KeyValue(scrollPane.vvalueProperty(), newVValue)));
		anim.setOnFinished((ActionEvent e)->{
			animationInProcess = false;
		});
		animationInProcess = true;
		anim.play();
	}

	private void addImage(Image image, String title) {

		//If this is the first item to add remove the example 
		if(!firstItemAdded) {
			backgroundPane.getChildren().clear();
			backgroundPane.setAlignment(Pos.TOP_CENTER);
			firstItemAdded= true;
		}
		
		// Create children
		Label titleLbl = new Label(title);
		titleLbl.getStyleClass().add("imageTitle");
		ImageView imageView = new ImageView(image);
		
		//Bot pane
		
		int iconSpacing = 5;
		int iconSize = 20;
		String labelSize = "medium";
		
		
		HBox upvoteWrapper = new HBox(iconSpacing);
		upvoteWrapper.setAlignment(Pos.CENTER);
		FontAwesomeIconView upvotes = new FontAwesomeIconView(FontAwesomeIcon.THUMBS_UP);
		upvotes.setGlyphSize(iconSize);
		upvotes.getStyleClass().add("postLabelColor");
		Label points = new Label("300");
		points.getStyleClass().addAll(labelSize,"postLabelColor");
		upvoteWrapper.getChildren().addAll(upvotes,points);

		HBox commentsWrapper = new HBox(iconSpacing);
		commentsWrapper.setAlignment(Pos.CENTER);
		FontAwesomeIconView comments = new FontAwesomeIconView(FontAwesomeIcon.COMMENT);
		comments.setGlyphSize(iconSize);
		comments.getStyleClass().add("postLabelColor");
		Label commentCount = new Label(labelSize);
		commentCount.getStyleClass().addAll(labelSize,"postLabelColor");
		commentsWrapper.getChildren().addAll(comments,commentCount);

		
		HBox duplicateWrapper = new HBox(iconSpacing);
		duplicateWrapper.setAlignment(Pos.CENTER);
		FontAwesomeIconView duplicate = new FontAwesomeIconView(FontAwesomeIcon.COPY);
		duplicate.setGlyphSize(iconSize);
		duplicate.getStyleClass().add("postLabelColor");
		Label duplicateCount = new Label(labelSize);
		duplicateCount.getStyleClass().addAll(labelSize,"postLabelColor");
		duplicateWrapper.getChildren().addAll(duplicate,duplicateCount);

		
		Label date = new Label("12.11.2018");
		date.getStyleClass().addAll(labelSize,"postLabelColor");

		Separator sep = new Separator(Orientation.HORIZONTAL);
		sep.setMaxWidth(image.getWidth());
		
		
		HBox bottomPane = new HBox(30,upvoteWrapper,commentsWrapper,duplicateWrapper,date);
		bottomPane.setMaxWidth(image.getWidth());
		
		VBox wrapper = new VBox(5, titleLbl, imageView,bottomPane,sep);
		wrapper.setAlignment(Pos.TOP_CENTER);
		//wrapper.setFillWidth(true);
		backgroundPane.getChildren().add(wrapper);
		backgroundPane.applyCss();
		backgroundPane.layout();
		scrollPane.layout();
		currentItem = backgroundPane.getChildren().size()-1;

		// Scroll to the bottom
		//
		Animation anim = new Timeline(new KeyFrame(Duration.millis(300), new KeyValue(scrollPane.vvalueProperty(), 1)));
		anim.setOnFinished((ActionEvent e)->{
			animationInProcess = false;
		});
		animationInProcess= true;
		anim.play();
	}
}
