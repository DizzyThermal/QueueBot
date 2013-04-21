import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Main
{
	public static BufferedReader bReader;
	public static BufferedWriter bWriter;

	public static String musicFilePath = "Music";
	
	public static String priorityFileString = "priorityQueue";
	public static String standardFileString = "standardQueue";
	public static String nowPlayingFileString = "nowPlaying";
	public static String configFileString = "QueueBot.ini";
	
	public static File priorityFile;
	public static File standardFile;
	public static File nowPlayingFile;
	public static File configFile = new File(configFileString);
	
	public static float readDelay = (float)2.5;
	
	public static int songId;

	public static ArrayList<Song> prioritySongs = new ArrayList<Song>();
	public static ArrayList<Song> standardSongs = new ArrayList<Song>();

	public static void main(String[] args)
	{
		// Populate File Strings and Settings
		readConfigFile();

		try
		{
			bReader = new BufferedReader(new FileReader(standardFile));
			String line;
			
			while((line = bReader.readLine()) != null)
				standardSongs.add(new Song(songId++, 0, line));
		}
		catch(IOException ioe) { ioe.printStackTrace(); }
		
		while(true)
		{
			if(priorityFile.length() > 0)
			{
				try
				{
					bReader = new BufferedReader(new FileReader(priorityFile));
					String line;
					
					while((line = bReader.readLine()) != null)
					{
						if(line.equals(""))
							continue;
	
						if(line.contains("[+]") || line.contains("[-]"))
							changePriority(line);
						else
							prioritySongs.add(new Song(songId++, 0, line));
					}
					bReader.close();
					
					bWriter = new BufferedWriter(new FileWriter(priorityFile));
					bWriter.write("");
					
					bWriter.close();
					
					if(prioritySongs.size() > 0)
					{
						bWriter = new BufferedWriter(new FileWriter(nowPlayingFile));
						bWriter.write(nowPlayingString(0) + "\n");
						for(int i = 1; i < prioritySongs.size(); i++)
							bWriter.append(nowPlayingString(i) + "\n");
						
						bWriter.close();
					}
				}
				catch(IOException ioe) { ioe.printStackTrace(); }
			}
			else
			{
				try { Thread.sleep((long)(readDelay * 1000)); }
				catch (Exception e) { e.printStackTrace(); }
			}
		}
	}
	
	public static void readConfigFile()
	{
		try
		{
			bReader = new BufferedReader(new FileReader(configFile));
			String line;

			while((line = bReader.readLine()) != null)
			{
				line = line.trim();
				if((line.equals("")) || (line.charAt(0) == '#'))
					continue;
				
				String[] variable = line.split("=");
				variable[0] = variable[0].trim();
				variable[1] = variable[1].trim();
				
				if(variable[1].charAt(0) == '\"')
					variable[1] = variable[1].substring(1, variable[1].length()-1);
				
				if(variable[0].equals("MusicFilePath"))
					musicFilePath = variable[1];
				else if(variable[0].equals("PriorityQueueFile"))
					priorityFileString = variable[1];
				else if(variable[0].equals("StandardQueueFile"))
					standardFileString = variable[1];
				else if(variable[0].equals("NowPlayingFile"))
					nowPlayingFileString = variable[1];
				else if(variable[0].equals("ReadDelay"))
					readDelay = Float.parseFloat(variable[1]);
			}
			bReader.close();
		}
		catch(IOException ioe) { ioe.printStackTrace(); }
		
		priorityFile = new File(priorityFileString);
		standardFile = new File(standardFileString);
		nowPlayingFile = new File(nowPlayingFileString);
	}
	
	public static void reprioritizeSongs()
	{
		// Bubble Sort Songs in Queue
		for(int i = 0; i < prioritySongs.size(); i++)
		{
			for(int j = 0; j < (prioritySongs.size() - 1 - i); j++)
			{
				if(prioritySongs.get(j).getRating() < prioritySongs.get(j+1).getRating())
				{
					int tmpPriority = prioritySongs.get(j).getRating();
					String tmpTitle = prioritySongs.get(j).getTitle();
					String tmpArtist = prioritySongs.get(j).getArtist();
					
					prioritySongs.get(j).setInfo(prioritySongs.get(j+1).getRating(), prioritySongs.get(j+1).getTitle(), prioritySongs.get(j+1).getArtist());
					prioritySongs.get(j+1).setInfo(tmpPriority, tmpTitle, tmpArtist);
				}
			}
		}
	}
	
	public static void changePriority(String song)
	{
		char direction = song.charAt(1);
		song = song.substring(4); //Ditch Plus or Minus
		
		for(int i = 0; i < prioritySongs.size(); i++)
		{
			if(prioritySongs.get(i).getSong().equals(song))
			{
				switch(direction)
				{
					case '+':
						prioritySongs.get(i).setRating(prioritySongs.get(i).getRating() + 1);
					break;
					case '-':
						prioritySongs.get(i).setRating(prioritySongs.get(i).getRating() - 1);
					break;
				}
				
				reprioritizeSongs();
			}
		}		
	}
	
	public static String nowPlayingString(int index)
	{
		return "[" + prioritySongs.get(index).getRating() + "] " + prioritySongs.get(index).getArtist() + " - " + prioritySongs.get(index).getTitle();
	}
}