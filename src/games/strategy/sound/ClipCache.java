package games.strategy.sound;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

class ClipCache {
  private final HashMap<URL, Clip> clipMap = new HashMap<URL, Clip>();
  private final List<URL> cacheOrder = new ArrayList<URL>();
  private final int maxSize;

  ClipCache(final int max) {
    if (max < 1) {
      throw new IllegalArgumentException("ClipCache max must be at least 1");
    }
    maxSize = max;
  }

  public synchronized Clip get(final URL file) {
    Clip clip = clipMap.get(file);
    if (clip != null) {
      cacheOrder.remove(file);
      cacheOrder.add(file);
      return clip;
    }
    if (clipMap.size() >= maxSize) {
      final URL leastPlayed = cacheOrder.get(0);
      // System.out.println("Removing " + leastPlayed + " and adding " + file);
      final Clip leastClip = clipMap.remove(leastPlayed);
      leastClip.stop();
      leastClip.flush();
      leastClip.close();
      cacheOrder.remove(leastPlayed);
    }
    clip = createClip(file, false);
    clipMap.put(file, clip);
    cacheOrder.add(file);
    return clip;
  }
  
  private static synchronized Clip createClip(final URL clipFile, final boolean testOnly) {
    try {
      final AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(clipFile);
      final AudioFormat format = audioInputStream.getFormat();
      final DataLine.Info info = new DataLine.Info(Clip.class, format);
      final Clip clip = (Clip) AudioSystem.getLine(info);
      clip.open(audioInputStream);
      if (!testOnly) {
        return clip;
      }
      clip.close();
      return null;
    }
    // these can happen if the sound isnt configured, its not that bad.
    catch (final LineUnavailableException e) {
      e.printStackTrace(System.out);
    } catch (final IOException e) {
      e.printStackTrace(System.out);
    } catch (final UnsupportedAudioFileException e) {
      e.printStackTrace(System.out);
    } catch (final RuntimeException re) {
      re.printStackTrace(System.out);
    }
    return null;
  }

  public synchronized void removeAll() {
    for (final Clip clip : clipMap.values()) {
      clip.stop();
      clip.flush();
      clip.close();
    }
    clipMap.clear();
    cacheOrder.clear();
  }

}
