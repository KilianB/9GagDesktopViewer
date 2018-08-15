package de.ipatexi.GagDesktopApp.gag;

import java.math.BigInteger;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PostItem {

	
	
	String title;	//Title of the post
	
	Type type;	//Photo, Gif, Video
	String id;	//Id of the post
	String url;	//id to the 9gag page
	boolean nsfw;	//not safe for work
	
	int upvoteCount;	//How many upvotes does a post have	
	int commentCount;	//How often did people comment under the picture
	
	//Extended post foldable?
	boolean hasLongPostCover;
	
	//Tags can be added to individual posts
	ArrayList<Tag> tags = new ArrayList<Tag>();
	
	String sourceUrl; //unused?
	
	//currently unused
	ArrayList<Object> targetedAdTags = new ArrayList<Object>(); // array empty. unused?
	String descriptionHtml; // empty unused?
	
	String contentURL; //Direct link to the content. Either mp4 or .jpg file.
	
	//Video specific
	int duration;	//If it is a video how long is it?

	//Photo specific
	int width; 
	int height;

	//End metainformation
	
	//A unique id to differentiate the posts. We want to know how far apart the individual items are.
	public static int globalId = 0;
	int internalId;
	BigInteger imageHash;
	
	
	public PostItem(){
		internalId = globalId++;
	}
	
	public String getTitle() {
		return title;
	}


	public void setTitle(String title) {
		this.title = title;
	}


	public Type getType() {
		return type;
	}


	public void setType(Type type) {
		this.type = type;
	}


	public String getId() {
		return id;
	}


	public void setId(String id) {
		this.id = id;
	}


	public String getUrl() {
		return url;
	}


	public void setUrl(String url) {
		this.url = url;
	}


	public boolean isNsfw() {
		return nsfw;
	}


	public void setNsfw(boolean nsfw) {
		this.nsfw = nsfw;
	}


	public int getUpvoteCount() {
		return upvoteCount;
	}


	public void setUpvoteCount(int upvoteCount) {
		this.upvoteCount = upvoteCount;
	}


	public int getCommentCount() {
		return commentCount;
	}


	public void setCommentCount(int commentCount) {
		this.commentCount = commentCount;
	}


	public int getDuration() {
		return duration;
	}


	public void setDuration(int duration) {
		this.duration = duration;
	}


	public ArrayList<Tag> getTags() {
		return tags;
	}


	public void addTag(Tag t){
		tags.add(t);
	}


	public String getSourceUrl() {
		return sourceUrl;
	}


	public void setSourceUrl(String sourceUrl) {
		this.sourceUrl = sourceUrl;
	}


	public ArrayList<Object> getTargetedAdTags() {
		return targetedAdTags;
	}


	public void setTargetedAdTags(ArrayList<Object> targetedAdTags) {
		this.targetedAdTags = targetedAdTags;
	}


	public String getDescriptionHtml() {
		return descriptionHtml;
	}


	public void setDescriptionHtml(String descriptionHtml) {
		this.descriptionHtml = descriptionHtml;
	}


	public boolean isHasLongPostCover() {
		return hasLongPostCover;
	}


	public void setHasLongPostCover(boolean hasLongPostCover) {
		this.hasLongPostCover = hasLongPostCover;
	}

	

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getInternalId() {
		return internalId;
	}

	public void setInternalId(int internalId) {
		this.internalId = internalId;
	}

	public BigInteger getImageHash() {
		return imageHash;
	}

	public void setImageHash(BigInteger imageHash) {
		this.imageHash = imageHash;
	}

	public String getContentURL() {
		return contentURL;
	}

	public void setContentURL(String contentURL) {
		this.contentURL = contentURL;
	}




	public static class Tag{
		String key;
		String url;
		
		public Tag(String key, String url){
			this.key = key;
			this.url = url;
		}
	}
	
	public enum Type{
		Photo,
		Animated,
		Article,
		Video,
		Gif
	}
	
	
	public static PostItem parseItem(JSONObject item){
		PostItem pItem = new PostItem();
		
		pItem.setNsfw(item.getInt("nsfw")!=0);
		pItem.setTitle(item.getString("title"));
		pItem.setHasLongPostCover(item.getInt("hasLongPostCover")!= 0);
		
		pItem.setUrl(item.getString("url"));
		
		
		JSONArray tags = item.getJSONArray("tags");
		for(int i = 0; i < tags.length(); i++){
			JSONObject tagBody = (JSONObject) tags.get(i);
			pItem.addTag(new Tag(tagBody.getString("key"),tagBody.getString("url")));
		}
		
		pItem.setSourceUrl(item.getString("sourceUrl"));
		pItem.setCommentCount(item.getInt("commentsCount"));
		
		JSONArray targetedadTags = item.getJSONArray("targetedAdTags");
		if(targetedadTags.length() > 0){
			System.out.println("targetdAds used. TODO handle it!");
		}
		
		
		pItem.setDescriptionHtml(item.getString("descriptionHtml"));
		pItem.setId(item.getString("id"));
		pItem.setSourceUrl(item.getString("sourceDomain"));
		pItem.setUpvoteCount(item.getInt("upVoteCount"));
		
		Type type = Type.valueOf(item.getString("type"));
		
		JSONObject imageData = (JSONObject) item.get("images");
		
		if(type.equals(Type.Animated)){
			//Check if it's a video or a gif
			
			JSONObject videoObject = (JSONObject) imageData.get("image460sv");
			pItem.setWidth(videoObject.getInt("width"));
			pItem.setHeight(videoObject.getInt("height"));
			pItem.setContentURL(videoObject.getString("url"));
			if(videoObject.getInt("hasAudio") != 0){
				pItem.setType(Type.Video);
				//Video
				pItem.setDuration(videoObject.getInt("duration"));
			}else{
				//Gif
				pItem.setType(Type.Gif);
			}
			
		}else{
			pItem.setType(Type.Photo);
			try{
			JSONObject photoObject = (JSONObject) imageData.get("image700");
			pItem.setContentURL(photoObject.getString("url"));
			pItem.setWidth(photoObject.getInt("width"));
			pItem.setHeight(photoObject.getInt("height"));	
			}catch(JSONException e){
				System.out.println(imageData
						 + e.getMessage());
			}
			 if(type.equals(Type.Article)){
				pItem.setType(Type.Article);		
				JSONObject articleData = (JSONObject) item.get("article");
			}
			 
		}
		
		return pItem;
	}
}

