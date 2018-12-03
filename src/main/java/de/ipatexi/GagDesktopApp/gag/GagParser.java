package de.ipatexi.GagDesktopApp.gag;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.github.kilianB.dataStrorage.tree.Result;
import com.github.kilianB.hashAlgorithms.PerceptiveHash;
import com.github.kilianB.matcher.DatabaseImageMatcher;

import de.ipatexi.GagDesktopApp.database.DatabaseManager;
import de.ipatexi.GagDesktopApp.gag.post.PostItem;
import de.ipatexi.GagDesktopApp.gag.testClassificationGui.ClassificationGui;
import de.ipatexi.GagDesktopApp.util.DaemonThreadFactory;
import javafx.application.Application;

public class GagParser {

	private static final Logger LOGGER = Logger.getLogger(GagParser.class.getSimpleName());

	enum Section {
		HOT, TRENDING, NEW
	};

	private final String currentSection = Section.TRENDING.toString();
	private final String BASE_URL = "https://9gag.com/v1/group-posts/group/default/type/";

	// Database
	private DatabaseManager dbManager;
	private DatabaseImageMatcher dbImageMatcher;

	private String subPath = "gagDatabaseNew";
	private String username = "sa";
	private String password = "sa";

	/**
	 * Download image meta data from 9gag
	 */
	boolean scrap = false;

	/**
	 * Maximum threads while scrapping info.
	 */
	private int maxThreads = 4;

	private int scrappingRecursionDepth = 1000;

	/*---------------------------------*/

	/**
	 * Download the final images from 9gag
	 */
	private boolean download = false;

	private boolean downloadMaxResolution = true;

	private File downloadFolder = new File("G:\\gagImages");

	/*---------------------------------*/

	boolean calculateImageSimilarity = true;

	private ThreadPoolExecutor boundCachedThreadPoolExecutor;

	public static void main(String[] args) {
		try {
			new GagParser();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public GagParser() throws SQLException {

		dbManager = new DatabaseManager(subPath, username, password);

		/*
		 * Create a thread pool executor handling all tasks of the parser
		 */
		boundCachedThreadPoolExecutor = new ThreadPoolExecutor(0, maxThreads, 60, TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>(), new DaemonThreadFactory());

		// Create second table for tags

		// Create third table for tags

		// Entry point. retrieve first 10 posts + end id.

		if (scrap)
			scrap9GagContent(currentSection.toLowerCase(), "", 0);

		if (download)
			downloadImage();

		if (calculateImageSimilarity)
			// calculateImageSimilarity();
			labelTestImages();
	}

	/**
	 * 9 gag exposes information of it's posts in JSON format. The JSON contains
	 * meta information e.g. upvote count path to the image tags etc ... Download
	 * the information in preparation for future download of the images.
	 * 
	 * @param content
	 * @throws SQLException
	 */
	/**
	 * 
	 * @param sectionEndpoint endpoint (Trending, Hot, Fresh)
	 * @param tokenQuery      (optional) search term
	 * @param currentDepth    current depth for recursive scraping
	 * @throws SQLException
	 */
	private void scrap9GagContent(String sectionEndpoint, String tokenQuery, int currentDepth) throws SQLException {
		// Construct the target url to query
		try {
			URL content = new URL(BASE_URL + sectionEndpoint + tokenQuery);

			if (currentDepth > scrappingRecursionDepth) {
				return;
			}

			try (InputStream is = content.openStream()) {
				JSONTokener tokener = new JSONTokener(is);
				JSONObject obj = new JSONObject(tokener);

				JSONObject data = (JSONObject) obj.get("data");

				// Do some reassignments to get the effective final state required by lambdas
				String pathToNextData = data.getString("nextCursor");
				int nextDepth = ++currentDepth;

				// The new path is known. We can already spawn the next thread.

				if (pathToNextData != null && !boundCachedThreadPoolExecutor.isShutdown()) {
					boundCachedThreadPoolExecutor.execute(() -> {
						try {
							scrap9GagContent(sectionEndpoint, "?" + pathToNextData, nextDepth);
						} catch (SQLException e) {
							e.printStackTrace();
						}
					});
				}
				JSONArray postData = data.getJSONArray("posts");

				for (int i = 0; i < postData.length(); i++) {
					JSONObject postObj = postData.getJSONObject(i);
					// System.out.println(postObj);

					PostItem item = PostItem.parseItem(postObj);

					dbManager.addPostItem(item);
				}
			} catch (JSONException j) {
				if (j.getMessage().contains("JSONObject[\"nextCursor\"] not found")) {
					System.out.println("Done");
					boundCachedThreadPoolExecutor.shutdown();
				} else {
					j.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * For each entry in the database download the image to disk if it not already
	 * present
	 */
	private void downloadImage() {
		downloadFolder.mkdir();

		AtomicInteger counter = new AtomicInteger(0);

		try {
			Map<String, String> imagesToDownload = dbManager.getImageDownloadURLs(downloadMaxResolution);

			ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(8);

			ConcurrentLinkedDeque<String> queue = new ConcurrentLinkedDeque<>(imagesToDownload.keySet());
			List<Future<Void>> waitForFinish = new ArrayList<>();

			for (int i = 0; i < executor.getMaximumPoolSize(); i++) {

				waitForFinish.add(executor.submit(() -> {
					String key = null;
					while ((key = queue.poll()) != null) {
						String url = imagesToDownload.get(key);

						// Check if image exist. No need to download again
						File saveLocation = new File(downloadFolder.getAbsolutePath() + "/" + key + ".jpg");
						if (!saveLocation.exists()) {
							BufferedImage image = ImageIO.read(new URL(url));
							ImageIO.write(image, "png", saveLocation);
						} else {
							LOGGER.fine("Image already exists. Skip download");
						}
						System.out.println(counter.getAndIncrement() + "/" + imagesToDownload.size());
					}
					return null;
				}));
			}

			for (Future<Void> f : waitForFinish) {
				f.get();
			}

			executor.shutdown();
		} catch (SQLException | InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

//
//		downloadFolder.mkdir();
//
//		HashingAlgorithm hash = new DifferenceHash(bitRes, Precision.Double);
//
//		String query = "SELECT uId,contentURL FROM POSTS WHERE type = 'Photo'AND ImageHash = ''";
//		try {
//
//			ReentrantLock lock = new ReentrantLock();
//			ResultSet rs = conn.createStatement().executeQuery(query);
//
//			ExecutorService executor = Executors.newFixedThreadPool(8);
//			ArrayList<Future> futures = new ArrayList<Future>();
//			for (int i = 0; i < 8; i++) {
//
//				Future f = executor.submit(() -> {
//					lock.lock();
//
//					try {
//						while (rs.next()) {
//							int uId = rs.getInt(1);
//							String contentURL = rs.getString(2);
//							lock.unlock();
//
//							try {
//								BufferedImage image = ImageIO.read(new URL(contentURL));
//
//								ImageIO.write(image, "png", new File("images/" + uId + ".png"));
//
//								BigInteger hashValue = hash.hash(image).getHashValue();
//
//								updateImageHash.setString(1, hashValue.toString(16));
//								updateImageHash.setInt(2, uId);
//								updateImageHash.executeUpdate();
//
//								System.out.println(uId);
//
//							} catch (IOException e) {
//								if (lock.isHeldByCurrentThread())
//									lock.unlock();
//								e.printStackTrace();
//							}
//							lock.lock();
//						}
//
//					} catch (SQLException e) {
//						e.printStackTrace();
//						lock.unlock();
//					} finally {
//						if (lock.isHeldByCurrentThread())
//							lock.unlock();
//					}
//				});
//				futures.add(f);
//			}
//
//			for (Future f : futures) {
//				try {
//					f.get();
//				} catch (InterruptedException | ExecutionException e) {
//					e.printStackTrace();
//				}
//			}
//
//			executor.shutdown();
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	/**
	 * Potential duplicate images are assessed if they are duplicates or not
	 */
	private void labelTestImages() {

		/* Start gui */
		new Thread(() -> {
			Application.launch(ClassificationGui.class);
		}).start();

		// Blocks until the gui has fully loaded
		ClassificationGui gui = ClassificationGui.getReference();

		try {
			DatabaseImageMatcher matcher = DatabaseImageMatcher.getFromDatabase(subPath, username, password, 0);

			// Matcher was not created beforehand lets to it
			if (matcher == null) {
				matcher = new DatabaseImageMatcher(subPath, username, password);
				matcher.addHashingAlgorithm(new PerceptiveHash(64), 0.1f);
				matcher.serializeToDatabase(0);
			}

			// Add images
			File[] images = downloadFolder.listFiles();
			int i = 0;
			for (File image : images) {
				try {
					matcher.addImage(image);
					System.out.println(i++);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			
			// Get already labeled images

			Map<String, PriorityQueue<Result<String>>> matchingImages = matcher.getAllMatchingImages();

			System.out.println("matching images " + matchingImages.size());

			for (Entry<String, PriorityQueue<Result<String>>> entry : matchingImages.entrySet()) {

				PriorityQueue<Result<String>> matchedImages = entry.getValue();
				// Skip. only itself
				if (matchedImages.size() != 1) {

					String img0 = entry.getKey();

					while (!matchedImages.isEmpty()) {
						Result<String> img1Res = matchedImages.poll();
						String img1 = img1Res.getValue();
						// Skip itself
						if (!img0.equals(img1) && !dbManager.areImagesLabeled(img0, img1)) {
							boolean match = gui.matchingImages(new File(entry.getKey()), new File(img1),
									img1Res.normalizedHammingDistance);
							dbManager.addLabeledTestImage(img0, img1, match);
						}
					}
				}
			}
			System.out.println("Done");
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
		System.out.println("Done!");
	}

//	private void calculateImageSimilarity() {
//
//		try {
//			Thread.sleep(1000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//
//		try {
//			matcher = new DatabaseImageMatcher("imgHash", "sa", "");
//			matcher.addHashingAlgorithm(new PerceptiveHash(64), 0.1f);
//			// TODO use file name filter
//			File[] images = downloadFolder.listFiles();
////			
////			int i = 0;
////			for(File image: images) {
////				try {
////					matcher.addImage(image);
////					System.out.println(i++);
////				} catch (IOException e) {
////					e.printStackTrace();
////				} catch (SQLException e) {
////					e.printStackTrace();
////				}
//			// }
//
//			Map<String, PriorityQueue<Result<String>>> matchingImages = matcher.getAllMatchingImages();
//
//			ClassificationGui gui = ClassificationGui.ref;
//
//			for (Entry<String, PriorityQueue<Result<String>>> entry : matchingImages.entrySet()) {
//
//				PriorityQueue<Result<String>> matchedImages = entry.getValue();
//				// Skip. only itself
//				if (matchedImages.size() != 1) {
//					matchedImages.poll();
//					System.out.println(entry.getKey() + ": " + matchedImages);
//
//					gui.matchingImages(new File(entry.getKey()), new File(matchedImages.poll().getValue()));
//
//				}
//			}
//
//		} catch (
//
//		ClassNotFoundException e1) {
//			e1.printStackTrace();
//		} catch (SQLException e1) {
//			e1.printStackTrace();
//		}
//
//		System.out.println("Done");
//
////		// 9046
////		String query = "SELECT uId,ImageHash FROM POSTS WHERE ImageHash != ''";
////		try {
////			ResultSet rs = conn.createStatement().executeQuery(query);
////
////			int threshold = 20;
////			int i = 0;
////
////			// 14 good to find the same kind of memes
////
////			long start = System.currentTimeMillis();
////			while (rs.next()) {
////				int uId = rs.getInt(1);
////				BigInteger imagehash = new BigInteger(rs.getString(2), 16);
////
////				Iterator<ImageData> iter = imageList.iterator();
////
////				// System.out.println("Size: " + imageList.size() + " " +
////				// imagehash.toString(2));
////
////				while (iter.hasNext()) {
////					ImageData data = iter.next();
////
////					int distance = ImageMatcher.hammingDistance(data.hash, imagehash);
////
////					if (distance < threshold) {
////						// Potential match inspect further
////
////						// System.out.println("Distance smaller: " + distance);
////						// do perceptive hashing later just save it to db.
////						addPotentialImageDuplicate.setInt(1, uId);
////						addPotentialImageDuplicate.setInt(2, data.uid);
////						addPotentialImageDuplicate.setInt(3, distance);
////						// addPotentialImageDuplicate.execute();
////					}
////				}
////				// After we checked all add it to our list
////				imageList.add(new ImageData(uId, imagehash));
////
////				// System.out.println(i++);
////			}
////			System.out.println(System.currentTimeMillis() - start);
////		} catch (SQLException e) {
////			// TODO Auto-generated catch block
////			e.printStackTrace();
////		}
////
////		// SELECT HAMMINGDISTANCE,COUNT(*) FROM POTENTIALDUPLICATES GROUP BY
////		// HAMMINGDISTANCE
//
//	}

}
