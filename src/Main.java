import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackListener;

import org.cmc.music.metadata.IMusicMetadata;
import org.cmc.music.metadata.MusicMetadataSet;
import org.cmc.music.myid3.MyID3;

public class Main
{
	public static BufferedReader bReader;
	public static BufferedWriter bWriter;

	public static String musicFilePath = "Music";
	
	public static String priorityFileString = "priorityQueue";
	public static String standardFileString = "standardQueue";
	public static String nowPlayingFileString = "nowPlaying";
	public static String dataBaseFileString = "QBMusicDatabase.db";
	public static String configFileString = "QueueBot.ini";
	
	public static String nowPlaying;
	
	public static File priorityFile;
	public static File standardFile;
	public static File nowPlayingFile;
	public static File dataBaseFile;
	public static File configFile = new File(configFileString);
	
	public static int songId;
	public static int vetoRequirement = 3;
	public static int veto;
	
	public static float readDelay = 2;
	public static int downVotePurgeMin = -2;

	public static Thread songThread = new Thread();
	public static SoundJLayer song = null;
	
	public static ArrayList<Song> prioritySongs = new ArrayList<Song>();
	public static ArrayList<Song> standardSongs = new ArrayList<Song>();

	public static void main(String[] args)
	{
		// Populate File Strings and Settings
		readConfigFile();
		
		dataBaseFile = new File(dataBaseFileString);
		
		if((!dataBaseFile.exists()) || ((args.length) > 0 && (args[0].equals("--buildDB"))))
			buildDB();

		System.out.println("All Systems Go!  QueueBot has Started!");
		
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
	
						if((line.charAt(0) == '+') || (line.charAt(0) == '-'))
							changePriority(line.charAt(0), Integer.parseInt(line.substring(3)));
						else if((line.charAt(0) == 'V'))
							addToVeto();
						else if((line.charAt(0) == 'A'))
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
					songName = songName + prioritySongs.get(0).getFilename();
					nowPlaying = prioritySongs.get(0).getMetadata();
					prioritySongs.remove(0);
				}
				else
				{
					songName = songName + standardSongs.get(0).getFilename();
					nowPlaying = standardSongs.get(0).getMetadata();
					standardSongs.remove(0);
				}
				
				veto = 0;
				song = new SoundJLayer(songName);
				song.play();
				
				updateNowPlaying();
			}
		}
		
		clearNowPlaying();
		System.out.println("Queue's Are Empty!\nThank you for using QueueBot!");
	}
	
	public static void buildDB()
	{	
		System.out.println("Building QueueBot Database - This may take a while!");
		long timeStart = System.currentTimeMillis();
		
		File directory = new File(musicFilePath);
		File[] songs = directory.listFiles();
		
		ArrayList<String> songList = new ArrayList<String>();
		
		for(int i = 0; i < songs.length; i++)
		{
			if((songs[i].isFile()) && (songs[i].getName().substring(songs[i].getName().length()-4).equals(".mp3")))
			{
				File song = new File(musicFilePath + songs[i].getName());
				
				MusicMetadataSet songMetaDataSet;
				IMusicMetadata songMetaData = null;

				try
				{
					songMetaDataSet = new MyID3().read(song);
					songMetaData = songMetaDataSet.getSimplified();
				}
				catch(Exception e) { e.printStackTrace(); }

				songList.add(getCSVRow(songMetaData) + "\\" + songs[i].getName());
			}
		}
		
		try
		{
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(dataBaseFileString));
			
			bWriter.write("");
			for(int i = 0; i < songList.size(); i++)
				bWriter.append(songList.get(i) + "\n");
			
			bWriter.close();
		}
		catch(Exception e) { e.printStackTrace(); }
		
		long timeStop = System.currentTimeMillis();
		System.out.println(songs.length + " songs added to database in " + getTime(timeStop-timeStart));
	}

	public static String getTime(double time)
	{
		int hours = (int)time/(1000 * 60 * 60);
		int minutes = (int)time/(1000 * 60);
		double seconds = time/1000;
		
		String output = "";
		if(hours > 0)
			output = output + hours + " hours, ";
		if(minutes > 0)
			output = output + minutes + " minutes, ";
		if(seconds > 0)
			output = output + seconds + " seconds";
		
		if(output.charAt(output.length()-2) == ',')
			output = output.substring(0, output.length()-2);
		
		return output;
	}
	
	public static String getCSVRow(IMusicMetadata m)
	{
		return m.getArtist() + "\\" + m.getSongTitle() + "\\" + m.getAlbum();
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
				else if(variable[0].equals("QBMusicDatabase"))
					dataBaseFileString = variable[1];
				else if(variable[0].equals("VetoRequirement"))
					vetoRequirement = Integer.parseInt(variable[1]);
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
					int tmpId = prioritySongs.get(j).getRating();
					int tmpPriority = prioritySongs.get(j).getRating();
					String tmpFilename = prioritySongs.get(j).getFilename();
					String tmpTitle = prioritySongs.get(j).getTitle();
					String tmpArtist = prioritySongs.get(j).getArtist();
					String tmpAlbum = prioritySongs.get(j).getAlbum();
					
					prioritySongs.get(j).setInfo(prioritySongs.get(j+1).getId(), prioritySongs.get(j+1).getRating(), prioritySongs.get(j+1).getFilename(), prioritySongs.get(j+1).getTitle(), prioritySongs.get(j+1).getArtist(), prioritySongs.get(j+1).getAlbum());
					prioritySongs.get(j+1).setInfo(tmpId, tmpPriority, tmpFilename, tmpTitle, tmpArtist, tmpAlbum);
				}
			}
		}
	}
	
	public static void changePriority(char direction, int id)
	{
		for(int i = 0; i < prioritySongs.size(); i++)
		{
			if(prioritySongs.get(i).getId() == id)
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
			bWriter.write(veto + "/" + vetoRequirement + "\\\\" + nowPlaying + "\n");
			if(prioritySongs.size() > 0)
			{
				for(int i = 0; i < prioritySongs.size(); i++)
					bWriter.append(nowPlayingString(prioritySongs.get(i)) + "\n");
			}
			
			bWriter.close();
		}
		catch(IOException ioe) { ioe.printStackTrace(); }
	}
	
	public static void clearNowPlaying()
	{
		try
		{
			bWriter = new BufferedWriter(new FileWriter(nowPlayingFile));
			bWriter.write("");
			bWriter.close();
		}
		catch(IOException ioe) { ioe.printStackTrace(); }
	}
	
	public static String nowPlayingString(Song song)
	{
		return song.getId() + "\\\\" + song.getRating() + "\\\\" + song.getArtist() + "\\\\" + song.getTitle() + "\\\\" + song.getAlbum();
	}
	
	public static void addToVeto()
	{
		veto++;
		if(veto >= vetoRequirement)
			song.getPlayingThread().stop();
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
            String urlAsString = "file://" + this.filePath;

            this.player = new AdvancedPlayer
            (
                new java.net.URL(urlAsString).openStream(),
                javazoom.jl.player.FactoryRegistry.systemRegistry().createAudioDevice()
            );

            this.player.setPlayBackListener(this);

            this.playerThread = new Thread(this, "AudioPlayerThread");

            this.playerThread.start();
        }
        catch (Exception ex) { ex.printStackTrace(); }
    }

    public void run()
    {
        try { this.player.play(); }
        catch (javazoom.jl.decoder.JavaLayerException ex) { ex.printStackTrace(); }
    }
    
    public Thread getPlayingThread() { return playerThread; }
}