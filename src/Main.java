import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackListener;

public class Main
{
	public static BufferedReader bReader;
	public static BufferedWriter bWriter;

	public static String musicFilePath = "Music";
	
	public static String priorityFileString = "priorityQueue";
	public static String standardFileString = "standardQueue";
	public static String nowPlayingFileString = "nowPlaying";
	public static String configFileString = "QueueBot.ini";
	
	public static String nowPlaying;
	
	public static File priorityFile;
	public static File standardFile;
	public static File nowPlayingFile;
	public static File configFile = new File(configFileString);
	
	public static float readDelay = 2;
	public static int downVotePurgeMin = -2;
	
	public static int songId;

	public static Thread songThread = new Thread();
	public static SoundJLayer song = null;
	
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

	MainLoop:
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
					
					updateNowPlaying();
				}
				catch(IOException ioe) { ioe.printStackTrace(); }
			}
			else
			{
				if(readDelay > 0)
				{
					try { Thread.sleep((long)(readDelay * 1000)); }
					catch (Exception e) { e.printStackTrace(); }
				}
			}
			
			if((song == null) || (!(song.getPlayingThread().isAlive())))
			{
				if(prioritySongs.isEmpty() && standardSongs.isEmpty())
					break MainLoop;

				String songName = musicFilePath;
				if(prioritySongs.size() > 0)
				{
					songName = songName + prioritySongs.get(0).getSong() + ".mp3";
					nowPlaying = prioritySongs.get(0).getSong();
					prioritySongs.remove(0);
				}
				else
				{
					songName = songName + standardSongs.get(0).getSong() + ".mp3";
					nowPlaying = standardSongs.get(0).getSong();
					standardSongs.remove(0);
				}
				
				song = new SoundJLayer(songName);
				song.play();
				
				updateNowPlaying();
			}
		}
		
		System.out.println("Queue's Are Empty!\nThank you for using QueueBot!");
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
				else if(variable[0].equals("DownVotePurgeMin"))
					downVotePurgeMin = Integer.parseInt(variable[1]);
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
				
				if(prioritySongs.get(i).getRating() <= downVotePurgeMin)
					prioritySongs.remove(i);
				
				reprioritizeSongs();
			}
		}		
	}
	
	public static void updateNowPlaying()
	{
		try
		{
			bWriter = new BufferedWriter(new FileWriter(nowPlayingFile));
			bWriter.write(nowPlaying + "\n");
			if(prioritySongs.size() > 0)
			{
				for(int i = 0; i < prioritySongs.size(); i++)
					bWriter.append(nowPlayingString(prioritySongs.get(i), true) + "\n");
			}
			for(int i = 0; i < standardSongs.size(); i++)
				bWriter.append(nowPlayingString(standardSongs.get(i), false) + "\n");
			
			bWriter.close();
		}
		catch(IOException ioe) { ioe.printStackTrace(); }
	}
	
	public static String nowPlayingString(Song song, boolean priority)
	{
		String str = "";
		if(priority)
			str = "[" + song.getRating() + "] ";
		return str + song.getArtist() + " - " + song.getTitle();
	}
}

class SoundJLayer extends PlaybackListener implements Runnable
{
    private String filePath;
    private AdvancedPlayer player;
    private Thread playerThread;    

    public SoundJLayer(String filePath)
    {
        this.filePath = filePath;
    }

    public void play()
    {
        try
        {
            String urlAsString = 
                "file://" 
                + this.filePath;

            this.player = new AdvancedPlayer
            (
                new java.net.URL(urlAsString).openStream(),
                javazoom.jl.player.FactoryRegistry.systemRegistry().createAudioDevice()
            );

            this.player.setPlayBackListener(this);

            this.playerThread = new Thread(this, "AudioPlayerThread");

            this.playerThread.start();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    // Runnable members

    public void run()
    {
        try
        {
            this.player.play();
        }
        catch (javazoom.jl.decoder.JavaLayerException ex)
        {
            ex.printStackTrace();
        }
    }
    
    public Thread getPlayingThread() { return playerThread; }
}