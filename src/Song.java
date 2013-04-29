public class Song
{
	private int id;
	private int rating;
	private String filename;
	private String artist;
	private String title;
	private String album;

	public Song(int id, int rating, String rawLine)
	{
		this.id = id;
		this.rating = rating;

		String[] lineArray = rawLine.split("\\\\");

		filename = lineArray[2];
		artist = lineArray[4];
		title = lineArray[6];
		album = lineArray[8];
	}
	
	public Song(int id, int rating, String title, String artist, String album)
	{
		this.id = id;
		this.rating = rating;
		this.title = title;
		this.artist = artist;
		this.album = album;
	}
	
	public void setInfo(int id, int rating, String filename, String title, String artist, String album)
	{
		this.id = id;
		this.rating = rating;
		this.filename = filename;
		this.title = title;
		this.artist = artist;
		this.album = album;
	}

	public int getId()						{ return id;										}
	public int getRating()					{ return rating;									}
	public String getFilename()				{ return filename;									}
	public String getTitle()				{ return title;										}
	public String getArtist()				{ return artist;									}
	public String getAlbum()				{ return album;										}
	public String getMetadata()				{ return artist + "\\\\" + title + "\\\\" + album;	}

	public void setRating(int rating)		{ this.rating = rating;	}
	public void setTitle(String title)		{ this.title = title;	}
	public void setArtist(String artist)	{ this.artist = artist;	}
	public void setAlbum(String album)		{ this.album = album;	}
}