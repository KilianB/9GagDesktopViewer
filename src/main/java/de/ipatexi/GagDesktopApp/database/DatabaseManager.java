package de.ipatexi.GagDesktopApp.database;

import java.awt.Dimension;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.h2.jdbcx.JdbcDataSource;

import de.ipatexi.GagDesktopApp.gag.Tag;
import de.ipatexi.GagDesktopApp.gag.post.AnimatedPost;
import de.ipatexi.GagDesktopApp.gag.post.ImagePost;
import de.ipatexi.GagDesktopApp.gag.post.PostItem;
import de.ipatexi.GagDesktopApp.gag.post.PostItem.Type;
import de.ipatexi.GagDesktopApp.gag.post.Section;

public class DatabaseManager implements AutoCloseable {

	Connection conn;

	/**
	 * Inserts generic post metadata
	 */
	PreparedStatement addPostDatabase;

	/**
	 * Insert tags
	 */
	PreparedStatement addTagsIntoDatabase;

	/**
	 * Insert sections
	 */
	PreparedStatement addSection;

	/**
	 * Insert image post specific data
	 */
	PreparedStatement addImagePost;
	
	PreparedStatement addPotentialImageDuplicate;

	PreparedStatement doesEntryExist;

	public DatabaseManager() throws SQLException {
		// Setup database connection
		JdbcDataSource ds = new JdbcDataSource();
		ds.setURL("jdbc:h2:~/gagDatabaseNew");
		ds.setUser("sa");
		ds.setPassword("sa");
		conn = ds.getConnection();

		createTables();

		prepareStatements();

		// Make sure to release the database at shutdown. lose connection
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				if (!conn.isClosed()) {
					conn.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}));

	}

	private void createTables() throws SQLException {
		Statement stmt = conn.createStatement();

		if (!doesTableExist(conn, "SECTION")) {
			stmt.execute(
					"CREATE TABLE SECTION (name VARCHAR(50) PRIMARY KEY, url VARCHAR(255), imageUrl VARCHAR(255) )");
		}

		if (!doesTableExist(conn, "POSTS")) {

			// we do it

			// BASE
			stmt.execute(
					"CREATE TABLE POSTS (id VARCHAR(15) PRIMARY KEY, title VARCHAR(1024), url VARCHAR(255) UNIQUE, nsfw BOOLEAN,"
							+ "promoted BOOLEAN,  hasLongPostCover BOOLEAN, sectionName VARCHAR(50), creationDate DATETIME,"
							+ "descriptionURL VARCHAR(255), upvoteCount INTEGER, commentCount INTEGER, isVoteMasked BOOLEAN, lastQueried Datetime,"
							+ "type VARCHAR(25), FOREIGN KEY (sectionName) REFERENCES SECTION(name))");

			// stmt.execute("CREATE TABLE TAGS (key VARCHAR(150) PRIMARY KEY, url
			// VARCHAR(150))");
		}

		if (!doesTableExist(conn, "TAGS"))
			stmt.execute("CREATE TABLE TAGS "
					+ "(postId  VARCHAR(15), tagName VARCHAR(150), tagURL VARCHAR(200), PRIMARY KEY( postId, tagName), FOREIGN KEY (postId) REFERENCES POSTS(id))");

		if (!doesTableExist(conn, "IMAGEPOST"))
			stmt.execute("CREATE TABLE IMAGEPOST "
					+ "(postId  VARCHAR(15), url VARCHAR(200), width INTEGER, height INTEGER,format VARCHAR(5), PRIMARY KEY( postId, url), FOREIGN KEY (postId) REFERENCES POSTS(id))");

		// REQUIRES UNIQUENESS FOR WHAT EVER REASON.

//		if (!doesTableExist(conn, "POTENTIALDUPLICATES"))
//			stmt.execute("CREATE TABLE POTENTIALDUPLICATES "
//					+ "(postUId INTEGER, postUId1 VARCHAR(150), hammingDistance INTEGER, PRIMARY KEY( postUId, postUID1))");

		// REQUIRES UNIQUENESS FOR WHAT EVER REASON.
//		ResultSet rs = stmt.executeQuery("SELECT MAX(uId) FROM POSTS");
//
//		if (rs.next()) {
//			int highestID = rs.getInt(1);
//			PostItem.setGlobalInt(highestID + 1);
//		}
	}

	private void prepareStatements() throws SQLException {
//		addPostDatabase = conn.prepareStatement(
//				"INSERT INTO POSTS(uId,title,type,id,url,nsfw,upvoteCount,commentCount,hasLongPostCover,sourceURL,descriptionHTML,"
//						+ "contentURL,duration,width,height,section) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

		addSection = conn.prepareStatement("MERGE INTO SECTION KEY(name) VALUES(?,?,?)");

		addPostDatabase = conn.prepareStatement(
				"INSERT INTO POSTS(id,title,url,nsfw,promoted,hasLongPostCover,sectionName, creationDate, descriptionURL, upvoteCount,commentCount,isVoteMasked,lastQueried,"
						+ "type) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

		addTagsIntoDatabase = conn.prepareStatement("INSERT INTO TAGS (postId,tagName,tagURL) VALUES(?,?,?)");

		addImagePost = conn.prepareStatement("INSERT INTO IMAGEPOST (postId,url,width,height,format) VALUES(?,?,?,?,?)");

//		addPotentialImageDuplicate = conn
//				.prepareStatement("INSERT INTO POTENTIALDUPLICATES (postUId,postUId1,hammingDistance) VALUES(?,?,?)");

		doesEntryExist = conn.prepareStatement("SELECT * FROM POSTS WHERE id = ?");
	}

	private boolean doesTableExist(Connection connection, String tableName) throws SQLException {
		DatabaseMetaData metadata = connection.getMetaData();
		ResultSet res = metadata.getTables(null, null, tableName, new String[] { "TABLE" });
		return res.next();
	}

	@Override
	public void close() throws Exception {
		if (!conn.isClosed()) {
			conn.close();
		}
	}

	//DO we need to synchronize or can we recreate the prepared statements?
	//TODO implement a lock for each statement
	public synchronized void addPostItem(PostItem item) throws SQLException {

		if (doesEntryExist(item.getId()))
			return;

		Section section = item.getSection();
		// Merge section
		addSection.setString(1, section.getName());
		addSection.setString(2, section.getUrl());
		addSection.setString(3, section.getImageUrl());

		addSection.executeUpdate();

		int id = 1;
		addPostDatabase.setString(id++, item.getId());
		addPostDatabase.setString(id++, item.getTitle());
		addPostDatabase.setString(id++, item.getUrl());
		addPostDatabase.setBoolean(id++, item.isNsfw());
		addPostDatabase.setBoolean(id++, item.isPromoted());
		addPostDatabase.setBoolean(id++, item.isHasLongPostCover());
		addPostDatabase.setString(id++, section.getName());
		addPostDatabase.setTimestamp(id++, new Timestamp(item.getCreationTime().getEpochSecond() * 1000));
		addPostDatabase.setString(id++, item.getDescriptionHtml());
		addPostDatabase.setInt(id++, item.getUpvoteCount());
		addPostDatabase.setInt(id++, item.getCommentCount());
		addPostDatabase.setBoolean(id++, item.isVoteMasked());
		addPostDatabase.setTimestamp(id++, new Timestamp(item.getQueryTime().toInstant().toEpochMilli()));
		addPostDatabase.setString(id++, item.getType().toString());

//		addPostDatabase.setString(17, currentSection);
		try {
			addPostDatabase.executeUpdate();
		} catch (SQLException ex) {
			ex.printStackTrace();
			System.out.println("Error adding: " + item + " " + item.getInternalId() + " " + item.getId());
		}

		List<Tag> tags = item.getTags();
		for (Tag t : tags) {
			addTagsIntoDatabase.setString(1, item.getId());
			addTagsIntoDatabase.setString(2, t.getKey());
			addTagsIntoDatabase.setString(3, t.getUrl());
			addTagsIntoDatabase.executeUpdate();
		}
		
		
		if(item instanceof AnimatedPost || item.getType().equals(Type.Photo) ) {
			
		
			List<Dimension> dims;
			List<URL> imageUrl;
			List<URL> webPUrl;
			
			if(item.getType().equals(Type.Photo)) {
				ImagePost iPost = (ImagePost) item;
				dims = iPost.getAvailableDimensions();
				imageUrl = iPost.getImageUrl();
				webPUrl = iPost.getImageUrlWebp();
			}else {
				//TODO same base class ?
				AnimatedPost iPost = (AnimatedPost) item;
				dims = iPost.getAvailableDimensionsThumbnail();
				imageUrl = iPost.getImageUrlThumbnail();
				webPUrl = iPost.getImageUrlWebpThumbnail();
			}
			
			for(int i = 0; i < dims.size(); i++) {
				
				String urlS = imageUrl.get(i).toString();
				addImagePost.setString(1,item.getId());
				addImagePost.setString(2,urlS);
				addImagePost.setInt(3,(int) dims.get(i).getWidth());
				addImagePost.setInt(4,(int) dims.get(i).getHeight());
				//So far always jpg
				String extension = urlS.substring(urlS.lastIndexOf(".")+1);
				addImagePost.setString(5,extension);
				addImagePost.executeUpdate();
				//So far always webp 
				addImagePost.setString(2,webPUrl.get(i).toString());
				urlS = webPUrl.get(i).toString();
				extension = urlS.substring(urlS.lastIndexOf(".")+1);
				addImagePost.setString(5,extension);
				addImagePost.executeUpdate();
			}
		}
		
		//TODO save test of video gif and article
		
	}
	
	/**
	 * 
	 * @param maxResolution
	 * @return
	 * @throws SQLException
	 */
	public Map<String,String> getImageDownloadURLs(boolean maxResolution) throws SQLException{
		
		Map<String,String> result = new HashMap<>();
		
		String res = maxResolution ? "MAX" : "MIN";
		
		//My h2 sql is pretty weka. Can probably be done in 1 query.
		
		//Get the maximum resolution image ? or do we want the min resolution image?
		Statement s = conn.createStatement();
		
		ResultSet rs = s.executeQuery("SELECT POSTID, "+res+"(WIDTH),"+res+"(HEIGHT) FROM IMAGEPOST WHERE FORMAT = 'jpg' GROUP BY  POSTID");
		
		PreparedStatement p = conn.prepareStatement("SELECT POSTID,URL FROM IMAGEPOST WHERE POSTID = ? AND WIDTH = ? AND HEIGHT = ? AND FORMAT = ?");
		
		while(rs.next()) {
			
			p.setString(1,rs.getString(1));
			p.setInt(2,rs.getInt(2));
			p.setInt(3,rs.getInt(3));
			p.setString(4,"jpg");
			ResultSet rss = p.executeQuery();
			rss.next();
			result.put(rss.getString(1),rss.getString(2));
		}
		return result;
	}
	

	private boolean doesEntryExist(String uId) throws SQLException {
		doesEntryExist.setString(1, uId);
		ResultSet result = doesEntryExist.executeQuery();
		if (result.next()) {
			return true;
		}
		return false;
	}
}
