package edu.mit.mitmobile2.tour;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.maps.GeoPoint;

public class Tour {
	
	public List<TourItem> getTourList(StartLocation startLocation) {
		return getTourList(getSite(startLocation.mSiteGuid));
	}
	
	public List<TourItem> getTourList(Site site) {
		ArrayList<TourItem> tourItems = new ArrayList<TourItem>();	
		
		int currentSiteIndex = site.mSiteIndex;
		
		int lastSiteIndex = currentSiteIndex - 1;
		if(currentSiteIndex == 0) {
			lastSiteIndex = mSites.size() - 1;
		} 
		
		boolean finished = false;
		while(!finished) {
			Site currentSite = mSites.get(currentSiteIndex);
			tourItems.add(currentSite);
			if(currentSiteIndex != lastSiteIndex) {
				tourItems.add(currentSite.getExitDirections());
			} else {
				finished = true;
			}
			
			currentSiteIndex++;
			if(currentSiteIndex == mSites.size()) {
				currentSiteIndex = 0;
			}
		}
		
		return tourItems;
	}

	public ArrayList<ParcelableGeoPoint> getPathGeoPoints() {
		ArrayList<ParcelableGeoPoint> geoPoints = new ArrayList<ParcelableGeoPoint>();
		
		for(Site site : mSites) {
			for(GeoPoint geoPoint : site.getExitDirections().getPath().getGeoPoints()) {
				geoPoints.add(new ParcelableGeoPoint(geoPoint));
			}
		}
		
		return geoPoints;
	}
	
	public ArrayList<TourMapItem> getDefaultTourMapItems() {
		ArrayList<TourMapItem> tourMapItems = new ArrayList<TourMapItem>();
	
		for(Site site : mSites) {
			tourMapItems.add(site.getTourMapItem(TourSiteStatus.FUTURE));
		}
		return tourMapItems;
	}
	
	public static ArrayList<TourMapItem> getTourMapItems(List<TourItem> tourItems, int position) {
		// determine the currentSite
		Site currentSite = null;
		while(position > -1) {
			if(tourItems.get(position).getClass() == Site.class) {
				currentSite = (Site) tourItems.get(position);
				break;
			}
			position--;
		}
		
		ArrayList<TourMapItem> tourMapItems = new ArrayList<TourMapItem>();
		
		TourSiteStatus status = TourSiteStatus.VISITED;
		for(TourItem tourItem : tourItems) {
			if(tourItem.getClass() == Site.class) {
				Site site = (Site) tourItem;
				
				// determine the status of the current site
				
				if(status == TourSiteStatus.CURRENT) {
					// the previous site was current so move status to future
					status = TourSiteStatus.FUTURE;
				}
				
				if((currentSite != null) && currentSite.getSiteGuid().equals(site.getSiteGuid())) {
					status = TourSiteStatus.CURRENT;
				}
				
				tourMapItems.add(site.getTourMapItem(status));			
			}
		}
		
		return tourMapItems;
	}
	
    enum TourSiteStatus {
    	VISITED,
    	CURRENT,
    	FUTURE
    }
    
	/*
	 * A simplified representation of Sites that can be passed
	 * to activities via a bundle
	 */
	public static class TourMapItem implements Parcelable {

		private String mSiteGuid;
		private String mPhotoUrl;
		private ParcelableGeoPoint mGeoPoint;
		private String mTitle;
		private TourSiteStatus mStatus;
		
		TourMapItem(String siteGuid, GeoPoint geoPoint, String title, String photoUrl, TourSiteStatus status) {
			mSiteGuid = siteGuid;
			mGeoPoint = new ParcelableGeoPoint(geoPoint);
			mTitle = title;
			mPhotoUrl = photoUrl;
			mStatus = status;
		}
		
		public static final Parcelable.Creator<TourMapItem> CREATOR = new Parcelable.Creator<TourMapItem>() {

			@Override
			public TourMapItem createFromParcel(Parcel source) {
				String siteGuid = source.readString();
				ParcelableGeoPoint geoPoint = source.readParcelable(ParcelableGeoPoint.class.getClassLoader());
				String title = source.readString();
				String photoUrl = source.readString();
				TourSiteStatus status = TourSiteStatus.values()[source.readInt()];
				
				return new TourMapItem(siteGuid, geoPoint, title, photoUrl, status);
			}

			@Override
			public TourMapItem[] newArray(int size) {
				return new TourMapItem[size];
			}
		};

		@Override
		public int describeContents() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(mSiteGuid);
			dest.writeParcelable(mGeoPoint, 0);
			dest.writeString(mTitle);
			dest.writeString(mPhotoUrl);
			dest.writeInt(mStatus.ordinal());
		}
		
		public String getSiteGuid() {
			return mSiteGuid;
		}
		
		public GeoPoint getGeoPoint() {
			return mGeoPoint;
		}
		
		public String getTitle() {
			return mTitle;
		}
		
		public String getPhotoUrl() {
			return mPhotoUrl;
		}
		
		public TourSiteStatus getStatus() {
			return mStatus;
		}
		
		public static interface LocationSupplier {
			public Location getLocation();
		}
		
		LocationSupplier mLocationSupplier;
		public void setLocationSupplier(LocationSupplier locationSupplier) {
			mLocationSupplier = locationSupplier;
		}
		
		public Float distance() {
			Location location = mLocationSupplier.getLocation();
			if(location == null) {
				return null;
			}
			
			float[] distance = new float[1];
			double endLatitude = mGeoPoint.getLatitudeE6() / 1E6;
			double endLongitude = mGeoPoint.getLongitudeE6() / 1E6;
			
			Location.distanceBetween(location.getLatitude(), location.getLongitude(), endLatitude, endLongitude, distance);
			return distance[0];
		}
		
		public float distanceBetween(TourMapItem other) {
			float[] distance = new float[1];
			double startLatitude = this.getGeoPoint().getLatitudeE6() / 1E6;
			double startLongitude = this.getGeoPoint().getLongitudeE6() / 1E6;
			
			double endLatitude = other.getGeoPoint().getLatitudeE6() / 1E6;
			double endLongitude = other.getGeoPoint().getLongitudeE6() / 1E6;
			
			Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, distance);
			return distance[0];
		}
	}

	public static class ParcelableGeoPoint extends GeoPoint implements Parcelable {
		public ParcelableGeoPoint(int latitudeE6, int longitudeE6) {
			super(latitudeE6, longitudeE6);
		}

		public ParcelableGeoPoint(GeoPoint geoPoint) {
			super(geoPoint.getLatitudeE6(), geoPoint.getLongitudeE6());
		}
		
		public static final Parcelable.Creator<ParcelableGeoPoint> CREATOR = new Parcelable.Creator<ParcelableGeoPoint>() {

			@Override
			public ParcelableGeoPoint createFromParcel(Parcel source) {
				int latitudeE6 = source.readInt();
				int longitudeE6 = source.readInt();
				return new ParcelableGeoPoint(latitudeE6, longitudeE6);
			}

			@Override
			public ParcelableGeoPoint[] newArray(int size) {
				return new ParcelableGeoPoint[size];
			}
		};
		
		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(getLatitudeE6());
			dest.writeInt(getLongitudeE6());
		}
	}
	
	abstract public static class TourItem {
		abstract String getLabel();
		
		abstract String getTitle();
		abstract TourItemContent getContent();
		
		abstract Path getPath();
		abstract PhotoInfo getPhotoInfo();
		
		public abstract String getAudioUrl();
		
	}	
	
	public static class PhotoInfo {
		String mPhotoUrl;
		String mPhotoLabel;
		
		PhotoInfo(String photoUrl, String photoLabel) {
			mPhotoUrl = photoUrl;
			mPhotoLabel = photoLabel;
		}
		
		public String getPhotoUrl() {
			return mPhotoUrl;
		}
		
		public String getPhotoLabel() {
			return mPhotoLabel;
		}
	}
	public static class TourItemContent {
		HashMap<String, SideTrip> mSideTrips = new HashMap<String, SideTrip>();
		ArrayList<TourItemContentNode> mContentNodes = new ArrayList<TourItemContentNode>();
		
		public void addSideTrip(SideTrip sideTrip) {
			mContentNodes.add(sideTrip);
			mSideTrips.put(sideTrip.getId(), sideTrip);
		}
		
		public void addHtml(String html) {
			mContentNodes.add(new HtmlContentNode(html));
		}
		
		public List<TourItemContentNode> getContentNodes() {
			return mContentNodes;
		}
		
		public SideTrip getSideTrip(String id) {
			return mSideTrips.get(id);
		}
	}
	
	abstract public static class TourItemContentNode {
		abstract String getHtml();
	};
	
	public static class HtmlContentNode extends TourItemContentNode {
		private String mHtml;
		
		HtmlContentNode(String html) {
			mHtml = html;
		}
		
		@Override
		public String getHtml() {
			return mHtml;
		}
	}
	
	public static class SideTrip extends TourItemContentNode {
		String mId;
		String mHtml; 
		String mTitle;
		String mPhotoUrl;
		String mAudioUrl;
		
		SideTrip(String id, String title, String html, String photoUrl, String audioUrl) {
			mId = id;
			mTitle = title;
			mHtml = html;
			mPhotoUrl = photoUrl;
		}	
		
		public String getId() {
			return mId;
		}
		
		public String getTitle() {
			return mTitle;
		}
		
		public String getHtml() {
			return mHtml;
		}
		
		public String getPhotoUrl() {
			return mPhotoUrl;
		}
		
		public String getAudioUrl() {
			return mAudioUrl;
		}
	}
	
	public static class Path {
		private ArrayList<GeoPoint> mGeoPoints = new ArrayList<GeoPoint>();
		private int mZoom;
		
		public Path(int zoom) {
			mZoom = zoom;
		}
		
		public void addGeoPoint(GeoPoint geoPoint) {
			mGeoPoints.add(geoPoint);
		}
		
		public List<GeoPoint> getGeoPoints() {
			return mGeoPoints;
		}
		
		public int getZoom() {
			return mZoom;
		}
	}
	
	
	public class TourHeader {
		private String mGuid;
		private String mTitle;	
		private String mDescriptionTop;
		private String mDescriptionBottom;
	
		public TourHeader(String guid, String title, String descriptionTop, String descriptionBottom) {
			mGuid = guid;
			mTitle = title;
			mDescriptionTop = descriptionTop;
			mDescriptionBottom = descriptionBottom;
		}
	
		public String getGuid() {
			return mGuid;
		}

		public String getDescriptionTop() {
			return mDescriptionTop;
		}
		
		public String getDescriptionBottom() {
			return mDescriptionBottom;
		}
		
		public String getTitle() {
			return mTitle;
		}	
	}
	
	private TourHeader mHeader;
	
	public Tour(TourHeader header) {
		mHeader = header;
	}
	
	public Tour(String guid, String title, String topHtml, String bottomHtml) {
		mHeader = new TourHeader(guid, title, topHtml, bottomHtml);
	}
	
	public TourHeader getHeader() {
		return mHeader;
	}
	
	public static class FooterLink {
		 private String mTitle;
		 private String mUrl;
		 
		 FooterLink(String title, String url) {
			 mUrl = url;
			 mTitle = title;
		 }
		 
		 public String getTitle() {
			 return mTitle;
		 }
         
         public String getUrl() {
        	 return mUrl;
         }
	}
	 
	public class TourFooter {
		 private String mFeedbackSubject;
		 private ArrayList<FooterLink> mLinks = new ArrayList<FooterLink>();
	
		 public void setFeedbackSubject(String feedbackSubject) {
			 mFeedbackSubject = feedbackSubject;
		 }

		 public String getFeedbackSubject() {
			 return mFeedbackSubject;
		 }
		 
		 public void addLink(String title, String url) {
			 mLinks.add(new FooterLink(title, url));
		 }
		 
		 public List<FooterLink> getLinks() {
			 return mLinks;
		 }
	}
	
	private TourFooter mFooter = new TourFooter();
	public void setFeedbackSubject(String feedbackSubject) {
		mFooter.setFeedbackSubject(feedbackSubject);
	}
	
	public TourFooter getFooter() {
		return mFooter;
	}
	
	private ArrayList<Site> mSites = new ArrayList<Site>();
	
	public Site addSite(String guid, String name, String photoUrl, String thumbnailUrl, String audioUrl, GeoPoint geoPoint) {
		Site site = new Site(mSites.size(), guid, name, photoUrl, thumbnailUrl, audioUrl, geoPoint);
		mSites.add(site);
		return site;
	}
	
	public List<Site> getSites() {
		return mSites;
	}
	
	public Site getSite(String guid) {
		for(Site site: mSites) {
			if(site.mSiteGuid.equals(guid)) {
				return site;
			}
		}
		
		return null;
	}
	
	public class Site extends TourItem {
		private int mSiteIndex;
		private String mSiteGuid;
		private String mName;
		private TourItemContent mContent;
		private PhotoInfo mPhotoInfo;
		private String mThumbnailUrl;
		private String mAudioUrl;
		private GeoPoint mGeoPoint;
		
		public Site(int siteIndex, String guid, String name, String photoUrl, String thumbnailUrl, String audioUrl, GeoPoint geoPoint) {
			mSiteIndex = siteIndex;
			mSiteGuid = guid;
			mName = name;
			mContent = new TourItemContent();
			mPhotoInfo = new PhotoInfo(photoUrl, name);
			mThumbnailUrl = thumbnailUrl;
			mAudioUrl = audioUrl;
			mGeoPoint = geoPoint;
		}
		
		public GeoPoint getGeoPoint() {
			return mGeoPoint;
		}
		
		public String getSiteGuid() {
			return mSiteGuid;
		}
		
		public String getName() {
			return mName;
		}
		
		private Directions mExitDirections;
		
		public Directions setExitDirections(String title, String destinationGuid, int zoom) {
			mExitDirections = new Directions(title, destinationGuid, zoom);
			return mExitDirections;
		}
		
		public Directions getExitDirections() {
			return mExitDirections;
		}

		// TourItem getters
		@Override
		TourItemContent getContent() { 
			return mContent; 
		}

		@Override
		Path getPath() { 
			return null; 
		}

		@Override
		PhotoInfo getPhotoInfo() { 
			return mPhotoInfo; 
		}

		@Override
		String getTitle() { 
			return mName; 
		}

		@Override
		String getLabel() {
			return mName;
		}
		
		public TourMapItem getTourMapItem(TourSiteStatus status) {
			return new TourMapItem(mSiteGuid, mGeoPoint, mName, mThumbnailUrl, status);
		}

		public String getThumbnailUrl() {
			return mThumbnailUrl;
		}
		
		@Override
		public
		String getAudioUrl() {
			return mAudioUrl;
		}
	}
	
	public class Directions extends TourItem {
		private String mTitle;
		private TourItemContent mContent;
		private Path mPath;
		private String mDestinationGuid;
		private String mPhotoUrl;
		private String mAudioUrl;
		
		Directions(String title, String destinationGuid, int zoom) {
			mTitle = title;
			mContent = new TourItemContent();
			mPath = new Path(zoom);
			mDestinationGuid = destinationGuid;
		}
		
		@Override
		public String getTitle() {
			return mTitle;
		}
		
		@Override
		public TourItemContent getContent() {
			return mContent;
		}

		
		public String getDestinationGuid() {
			return mDestinationGuid;
		}
		
		public void setPath(Path path) {
			mPath = path;
		}
		
		public void setPhotoUrl(String photoUrl) {
			mPhotoUrl = photoUrl;
		}
		
		public void setAudioUrl(String audioUrl) {
			mAudioUrl = audioUrl;
		}
		
		@Override
		Path getPath() {
			return mPath;
		}
		
		@Override
		PhotoInfo getPhotoInfo() {
			if(mPhotoUrl != null) {
				return new PhotoInfo(mPhotoUrl, getSite(mDestinationGuid).getTitle());
			} else {
				return getSite(mDestinationGuid).getPhotoInfo();
			}
		}

		@Override
		String getLabel() {
			return "Walk to " + getSite(mDestinationGuid).getLabel();
		}

		@Override
		public
		String getAudioUrl() {
			return mAudioUrl;
		}
	}
	
	private String mStartLocationsHeader;
	public void setStartLocationsHeader(String header) {
		mStartLocationsHeader = header;
	}
	
	public String getStartLocationsHeader() {
		return mStartLocationsHeader;
	}
	
	private ArrayList<StartLocation> mStartLocations = new ArrayList<StartLocation>();
	
	public StartLocation addStartLocation(String title, String locationId, String siteGuid, String content, String photoUrl, GeoPoint geoPoint) {
		StartLocation startLocation = new StartLocation(title, locationId, siteGuid, content, photoUrl, geoPoint);
		mStartLocations.add(startLocation);
		return startLocation;
	}
	
	public List<StartLocation> getStartLocations() {
		return mStartLocations;
	}
	
	public StartLocation getStartLocation(String id) {
		for(StartLocation startLocation : mStartLocations) {
			if(startLocation.mLocationId.equals(id)) {
				return startLocation;
			}
		}
		return null;
	}
	
	public class StartLocation {
		private String mTitle;
		private String mSiteGuid;
		private String mLocationId;
		private String mContent;
		private String mPhotoUrl;
		
		private GeoPoint mGeoPoint;
		private float mDistance;
	
		public StartLocation(String title, String locationId, String siteGuid, String content, String photoUrl, GeoPoint geoPoint) {
			mTitle = title;
			mLocationId = locationId;
			mSiteGuid = siteGuid;
			mContent = content;
			mPhotoUrl = photoUrl;
			mGeoPoint = geoPoint;
		}
		
		public String getTitle() {
			return mTitle;
		}
		
		public String getSiteGuid() {
			return mSiteGuid;
		}
		
		public String getLocationId() {
			return mLocationId;
		}
		
		public String getContent() {
			return mContent;
		}
		
		public String getPhotoUrl() {
			return mPhotoUrl;
		}
		
		
		public void setGeoPoint(GeoPoint geoPoint) {
			mGeoPoint = geoPoint;
		}
		
		public GeoPoint getGeoPoint() {
			return mGeoPoint;
		}
		
		public float getDistance() {
			return mDistance;
		}

		public void setDistance(float distance) {
			mDistance = distance;
		}
	}
}
