package de.ipatexi.GagDesktopApp.gag;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;

import org.h2.jdbcx.JdbcDataSource;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.github.kilianB.hashAlgorithms.DifferenceHash;
import com.github.kilianB.hashAlgorithms.DifferenceHash.Precision;
import com.github.kilianB.hashAlgorithms.HashingAlgorithm;
import com.github.kilianB.matcher.Hash;
import com.github.kilianB.matcher.ImageMatcher;

import de.ipatexi.GagDesktopApp.database.DatabaseManager;
import de.ipatexi.GagDesktopApp.gag.PostItem.Tag;
import de.ipatexi.GagDesktopApp.util.DaemonThreadFactory;


public class GagParser {

	enum Section{
		HOT,
		TRENDING,
		NEW
	};
	
	private final String currentSection = Section.TRENDING.toString();
	private final String BASE_URL = "https://9gag.com/v1/group-posts/group/default/type/";

	private DatabaseManager dbManager;

	
	
	private int maxThreads = 20;
	private ThreadPoolExecutor boundCachedThreadPoolExecutor;
	
	
	/**
	 * Download image meta data from 9gag
	 */
	boolean scrap = false;
	
	/**
	 * Download the final images from 9gag
	 */
	boolean download = false;
	
	
	boolean calculateImageSimilarity = true;
	
	
	public static void main(String[] args) {
		new GagParser();
		
		
		
	}


	public GagParser() {
			
		
		dbManager = new DatabaseManager();
		
		/*
		 * Create a thread pool executor handling all tasks of the parser
		 */
		boundCachedThreadPoolExecutor = new ThreadPoolExecutor(0, maxThreads,60,TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>(),
				new DaemonThreadFactory());
		
			//Create second table for tags
			
			
			//Create third table for tags
			
			
			// Entry point. retrieve first 10 posts + end id.
			
			
			if(scrap)
				scrap9GagContent(currentSection.toLowerCase(),"",0);
			
			if(download)
				downloadImage();
			
			if(calculateImageSimilarity)
				calculateImageSimilarity();
	}
	
	
	
	
	int scrappingRecursionDepth = 1000;
	
	
	
	/**
	 * 9 gag exposes information of it's posts in JSON format. 
	 * The JSON contains meta information e.g. upvote count path to the image tags etc ...
	 * Download the information in preparation for future download of the images. 
	 * 
	 * @param content 
	 */
	private void scrap9GagContent(String sectionEndpoint,String tokenQuery,int currentDepth) {
		
		//Construct the target url to query
		try {
			URL content = new URL(BASE_URL+sectionEndpoint + tokenQuery);

			if(currentDepth > scrappingRecursionDepth){
				return;
			}
						
			try (InputStream is = content.openStream()){
				JSONTokener tokener = new JSONTokener(is);
				JSONObject obj = new JSONObject(tokener);

				JSONObject data = (JSONObject) obj.get("data");
				
				//Do some reassignments to get the effective final state required by lambdas
				String pathToNextData = data.getString("nextCursor");
				int nextDepth = ++currentDepth;
				
				
				//The new path is known. We can already spawn the next thread.
				
				if(pathToNextData != null){
					boundCachedThreadPoolExecutor.execute(()->{
						scrap9GagContent(sectionEndpoint,"?" + pathToNextData,nextDepth);
					});
				}

				JSONArray postData = data.getJSONArray("posts");
				
				for (int i = 0; i < postData.length(); i++) {
					JSONObject postObj = postData.getJSONObject(i);
					//System.out.println(postObj);

					PostItem item = PostItem.parseItem(postObj);
					
					dbManager.addPostItem(item);
				}	
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		
		
	
	}

	
	

	
	
	int bitRes = 64;
	
	
	//Download image data
	private void downloadImage(){
		
		File imageDir = new File("images/");
		imageDir.mkdir();
		
		HashingAlgorithm hash = new DifferenceHash(bitRes,Precision.Double);
		
		String query = "SELECT uId,contentURL FROM POSTS WHERE type = 'Photo'AND ImageHash = ''";
		try {
			
			ReentrantLock lock = new ReentrantLock();
			ResultSet rs = conn.createStatement().executeQuery(query);
	
			ExecutorService executor = Executors.newFixedThreadPool(8);
			ArrayList<Future> futures = new ArrayList<Future>();
			for(int i = 0; i < 8; i++){
				
				Future f = executor.submit(() ->{
				lock.lock();
				
				try {
					while(rs.next()){
						int uId =  rs.getInt(1);
						String contentURL = rs.getString(2);
						lock.unlock();
						
						try {
							BufferedImage image = ImageIO.read(new URL(contentURL));
							
							ImageIO.write(image, "png", new File("images/" + uId+ ".png"));
							
							BigInteger hashValue = hash.hash(image).getHashValue();

							updateImageHash.setString(1, hashValue.toString(16));
							updateImageHash.setInt(2, uId);
							updateImageHash.executeUpdate();
							
							System.out.println(uId);
						
						} catch (IOException e) {
							if(lock.isHeldByCurrentThread())
								lock.unlock();
							e.printStackTrace();
						}
						lock.lock();
					}
					
				} catch (SQLException e) {
					e.printStackTrace();
					lock.unlock();
				}finally{
					if(lock.isHeldByCurrentThread())
						lock.unlock();
				}
			});
				futures.add(f);
			}
			
			for(Future f : futures){
				try {
					f.get();
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
			
			executor.shutdown();			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	class ImageData{
		public ImageData(int uId, BigInteger imagehash) {
			this.uid = uId;
			this.hash = imagehash;
		}
		int uid;
		BigInteger hash;
	}
	LinkedList<ImageData> imageList = new LinkedList<>();

	
	private void calculateImageSimilarity(){
		//9046
		String query = "SELECT uId,ImageHash FROM POSTS WHERE ImageHash != ''";
		try {
			ResultSet rs = conn.createStatement().executeQuery(query);
			
			int threshold = 20;
			int i = 0;
			
			
			//14 good to find the same kind of memes
			
			long start = System.currentTimeMillis();
			while(rs.next()){
				int uId = rs.getInt(1);
				BigInteger imagehash =  new BigInteger(rs.getString(2),16);
				
				Iterator<ImageData> iter = imageList.iterator();
				
				//System.out.println("Size: " + imageList.size() + " " + imagehash.toString(2));
				
				while(iter.hasNext()){
					ImageData data = iter.next();
					
					
					
					int distance = ImageMatcher.hammingDistance(data.hash,imagehash);
					
					if(distance < threshold){
						//Potential match inspect further
						
						//System.out.println("Distance smaller: " + distance);
						//do perceptive hashing later just save it to db.
						addPotentialImageDuplicate.setInt(1, uId);
						addPotentialImageDuplicate.setInt(2, data.uid);
						addPotentialImageDuplicate.setInt(3, distance);
						//addPotentialImageDuplicate.execute();
					}
				}
				//After we checked all add it to our list
				imageList.add(new ImageData(uId,imagehash));
				
				//System.out.println(i++);
			}
			System.out.println(System.currentTimeMillis() - start);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		//SELECT HAMMINGDISTANCE,COUNT(*) FROM POTENTIALDUPLICATES GROUP BY HAMMINGDISTANCE
		
	}
	
	
	
}
