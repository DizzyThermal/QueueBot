public class Song
{
	private int id;
	private int rating;
	private String artist;
	private String title;

	public Song(int id, int rating, String song)
	{
		this.id = id;
		this.rating = rating;
		artist = song.split(" - ")[0];
		title = song.split(" - ")[1];
	}
	
	public Song(int id, int rating, String title, String artist)
	{
		this.id = id;
		this.rating = rating;
		this.title = title;
		this.artist = artist;
	}
	
	public void setInfo(int rating, String title, String artist)
	{
		this.rating = rating;
		this.title = title;
		this.artist = artist;
	}
	
	public int getId()						{ return id;						}
	public int getRating()					{ return rating;					}
	public String getTitle()				{ return title;						}
	public String getArtist()				{ return artist;					}
	public String getSong()					{ return artist + " - " + title;	}
	
	public void setId(int id)				{ this.id = id;			}
	public void setRating(int rating)		{ this.rating = rating;	}
	public void setTitle(String title)		{ this.title = title;	}
	public void setArtist(String artist)	{ this.artist = artist;	}
}