package de.ipatexi.GagDesktopApp.database;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.h2.jdbcx.JdbcDataSource;

import de.ipatexi.GagDesktopApp.gag.PostItem;
import de.ipatexi.GagDesktopApp.gag.PostItem.Tag;

public class DatabaseManager implements AutoCloseable{

	
	Connection conn;
	
	/**
	 * Inserts a post 
	 */
	PreparedStatement addPostDatabase;
	
	
	PreparedStatement addTagsIntoDatabase;
	
	
	PreparedStatement addPotentialImageDuplicate;
	
	
	PreparedStatement updateImageHash;
	
	
	PreparedStatement doesEntryExist;
	  
	
	public DatabaseManager() throws SQLException {
		//Setup database connection
		JdbcDataSource ds = new JdbcDataSource();
		ds.setURL("jdbc:h2:~/gagDatabase");
		ds.setUser("sa");
		ds.setPassword("sa");
		conn = ds.getConnection();
		
		createTables();
		
		prepareStatements();
		
		
		//Make sure to release the database at shutdown. lose connection
		Runtime.getRuntime().addShutdownHook(new Thread(
				()->{
					try {
						if(!conn.isClosed()) {
							conn.close();
						}
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			));
	
	
	}
	
	private void createTables() throws SQLException {
		Statement stmt = conn.createStatement();
		
		if(!doesTableExist(conn,"POSTS")){
			
			stmt.execute("CREATE TABLE POSTS "
					+ "(uId INTEGER UNIQUE, title VARCHAR(1024), "
					+ "type VARCHAR(10), id VARCHAR(15) UNIQUE, "
					+ "url VARCHAR(255) UNIQUE, nsfw BOOLEAN, "
					+ "upvoteCount INTEGER, commentCount INTEGER, "
					+ "hasLongPostCover BOOLEAN, sourceURL VARCHAR(255), "
					+ "descriptionHTML VARCHAR(1024), contentURL VARCHAR(255), "
					+ "duration INTEGER, width INTEGER, height INTEGER, "
					+ "section VARCHAR(25), "
					+ "imageHash VARCHAR(255), PRIMARY KEY (uId))");
		}
		
		if(!doesTableExist(conn,"TAGS"))
			stmt.execute("CREATE TABLE TAGS "
					+ "(postUId INTEGER, tagName VARCHAR(150), tagURL VARCHAR(200), PRIMARY KEY( postUId, tagName), FOREIGN KEY (postUId) REFERENCES POSTS(uId))");
		// REQUIRES UNIQUENESS FOR WHAT EVER REASON.
		
		if(!doesTableExist(conn,"POTENTIALDUPLICATES"))
			stmt.execute("CREATE TABLE POTENTIALDUPLICATES "
					+ "(postUId INTEGER, postUId1 VARCHAR(150), hammingDistance INTEGER, PRIMARY KEY( postUId, postUID1))");
		
		// REQUIRES UNIQUENESS FOR WHAT EVER REASON.
		ResultSet rs = stmt.executeQuery("SELECT MAX(uId) FROM POSTS");
		
		if(rs.next()){
			int highestID = rs.getInt(1);
			PostItem.globalId = highestID+1;
		}
	}
	
	private void prepareStatements() throws SQLException {
		addPostDatabase = conn.prepareStatement("INSERT INTO POSTS(uId,title,type,id,url,nsfw,upvoteCount,commentCount,hasLongPostCover,sourceURL,descriptionHTML,"
				+ "contentURL,duration,width,height,imageHash,section) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

		addTagsIntoDatabase = conn.prepareStatement("INSERT INTO TAGS (postUId,tagName,tagURL) VALUES(?,?,?)");
		
		updateImageHash = conn.prepareStatement("UPDATE POSTS SET imageHash = ? WHERE uId = ?");
		
		addPotentialImageDuplicate = conn.prepareStatement("INSERT INTO POTENTIALDUPLICATES (postUId,postUId1,hammingDistance) VALUES(?,?,?)");
		
		doesEntryExist = conn.prepareStatement("SELECT * FROM POSTS WHERE id = ?");
	}
	
	private boolean doesTableExist(Connection connection, String tableName) {
		System.out.println("Does table exits");
		try {
			DatabaseMetaData metadata = connection.getMetaData();
			ResultSet res = metadata.getTables(null, null, tableName, new String[] { "TABLE" });
			return res.next();

		} catch (SQLException e) {
			e.printStackTrace();
			return false;
			// TODO returning false in this matter is semantically not correct.
		}
	}

	@Override
	public void close() throws Exception {
		if(!conn.isClosed()) {
			conn.close();
		}
	}

	
	public void addPostItem(PostItem item) {
		if(doesEntryExist(item.getId()))
			continue;
		
		addPostDatabase.setInt(1, item.getInternalId());
		addPostDatabase.setString(2, item.getTitle());
		addPostDatabase.setString(3, item.getType().toString());
		addPostDatabase.setString(4, item.getId());
		addPostDatabase.setString(5, item.getUrl());
		addPostDatabase.setBoolean(6, item.isNsfw());
		addPostDatabase.setInt(7, item.getUpvoteCount());
		addPostDatabase.setInt(8, item.getCommentCount());
		addPostDatabase.setBoolean(9, item.isHasLongPostCover());
		addPostDatabase.setString(10, item.getSourceUrl());
		addPostDatabase.setString(11, item.getDescriptionHtml());
		addPostDatabase.setString(12, item.getContentURL());
		addPostDatabase.setInt(13, item.getDuration());
		addPostDatabase.setInt(14, item.getWidth());
		addPostDatabase.setInt(15, item.getHeight());
		
		BigInteger imageHash = item.getImageHash();
		if(imageHash != null){
			addPostDatabase.setString(16, item.getImageHash().toString(16));
		}else{
			addPostDatabase.setString(16, "");
		}
		addPostDatabase.setString(17, currentSection);
		addPostDatabase.executeUpdate();
		
		List<Tag> tags = item.getTags();
		for(Tag t : tags){
			addTagsIntoDatabase.setInt(1, item.getInternalId());
			addTagsIntoDatabase.setString(2, t.key);
			addTagsIntoDatabase.setString(3, t.url);
			addTagsIntoDatabase.executeUpdate();
		}
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
