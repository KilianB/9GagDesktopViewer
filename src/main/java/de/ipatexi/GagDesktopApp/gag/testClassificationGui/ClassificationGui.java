package de.ipatexi.GagDesktopApp.gag.testClassificationGui;

import java.io.File;
import java.util.concurrent.Semaphore;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * @author Kilian
 *
 */
public class ClassificationGui extends Application {

	//UGLY
	
	ImageView leftImgView = new ImageView();
	ImageView rightImgView = new ImageView();

	Semaphore userDecide = new Semaphore(0);

	KeyCode matchKey = KeyCode.D;
	KeyCode nomatchKey = KeyCode.A;

	Label img1Path = new Label();
	Label img2Path = new Label();
	
	Label img1Dimension = new Label();
	Label img2Dimension = new Label();
	
	Label distance = new Label();
	
	//TODO title of both posts
	
	
	boolean match;

	
	private static Semaphore initLock = new Semaphore(0);
	public static ClassificationGui ref;
	

	
	@Override
	public void start(Stage primaryStage) throws Exception {

		Screen screen = Screen.getPrimary();
		Rectangle2D screenBounds = screen.getBounds();

		BorderPane bp = new BorderPane();

		
		
		
		//Center image view
		HBox leftBox = new HBox(leftImgView);
		leftBox.setAlignment(Pos.CENTER);
		VBox leftRoot = new VBox(img1Path,img1Dimension,leftBox);
		
		
		
		HBox rightBox = new HBox(rightImgView);
		rightBox.setAlignment(Pos.CENTER);
		bp.setLeft(leftRoot);
		bp.setRight(rightBox);

		
		/*Bottom instruction */
		ImageView botImg = new ImageView(new Image("file:helpKeysBlack.png"));
		VBox bottom = new VBox(botImg);
		bottom.setAlignment(Pos.CENTER);
		VBox.setMargin(botImg,new Insets(0,0,20,0));
		bp.setBottom(bottom);
		
		Scene scene = new Scene(bp, screenBounds.getWidth(), screenBounds.getHeight());

		leftImgView.fitWidthProperty().bind(scene.widthProperty().divide(2));
		leftImgView.fitHeightProperty().bind(scene.heightProperty().divide(2));

		rightImgView.fitWidthProperty().bind(scene.widthProperty().divide(2));
		rightImgView.fitHeightProperty().bind(scene.heightProperty().divide(2));

		scene.setOnKeyReleased(event -> {
			if (event.getCode().equals(matchKey)) {
				match = true;
				userDecide.release();
				System.out.println("Match");
			} else if (event.getCode().equals(nomatchKey)) {
				match = false;
				userDecide.release();
				System.out.println("No Match");
			}
		});
		ref = this;
		initLock.release();
		primaryStage.setScene(scene);
		primaryStage.setTitle("9Gag Duplicate Classifier");
		primaryStage.show();
	}

	/**
	 * Check if the two images are matchign images. Blocking call until use has
	 * decided to choose
	 * 
	 * @param f1 The first image
	 * @param f2 The second image
	 * @param normalizedHammingDistance 
	 * @return true if the images match, false
	 */
	public boolean matchingImages(File f1, File f2, double normalizedHammingDistance) {

		Image img1 = new Image("file:"+f1.getAbsolutePath());
		Image img2 = new Image("file:"+f2.getAbsolutePath());
		// Set images.
		leftImgView.setImage(img1);
		rightImgView.setImage(img2);
		
		img1Path.setText(f1.getAbsolutePath());
		img2Path.setText(f2.getAbsolutePath());
		
		img1Dimension.setText(img1.getWidth()+" x "+ img1.getHeight());
		img2Dimension.setText(img2.getWidth()+" x "+ img2.getHeight());
		distance.setText(String.format("%.3f",normalizedHammingDistance));
		
		// Wait for response
		try {
			userDecide.drainPermits();
			userDecide.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return match;
	}

	/**
	 * @return
	 */
	public static ClassificationGui getReference() {
		try {
			initLock.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return ref;
	}

}
