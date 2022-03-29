package online.nasgar.antibot.utils;

import com.google.common.collect.Queues;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Queue;
import java.util.logging.Level;
import lombok.experimental.UtilityClass;
import net.md_5.bungee.BungeeCord;
import online.nasgar.antibot.config.Settings;
import online.nasgar.antibot.caching.PacketUtils;

@UtilityClass
public class FailedUtils
{

    private final Path out = Paths.get( "AntiBot", "failed.txt" );
    private final Queue<String> writeQueue = Queues.newConcurrentLinkedQueue();

    public void addIpToQueue(String ip, PacketUtils.KickType reason)
    {
        if ( Settings.IMP.SAVE_FAILED_IPS_TO_FILE && reason != PacketUtils.KickType.COUNTRY )
        {
            writeQueue.add( ip + "|" + reason.name() + "|" + System.currentTimeMillis() );
        }
    }

    public void flushQueue()
    {
        Queue<String> queue = writeQueue;
        int j = queue.size();
        if ( j == 0 )
        {
            return;
        }
        try ( BufferedWriter writer = Files.newBufferedWriter( out, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND ) )
        {
            String line;
            while ( j-- > 0 && ( line = queue.poll() ) != null )
            {
                writer.write( line );
                writer.newLine();
            }
        } catch ( Exception e )
        {
            BungeeCord.getInstance().getLogger().log( Level.WARNING, "[AntiBot] Could not save failed ips to file", e );
        }
    }
}
