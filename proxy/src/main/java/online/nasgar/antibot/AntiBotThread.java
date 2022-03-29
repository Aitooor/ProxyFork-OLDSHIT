package online.nasgar.antibot;

import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import net.md_5.bungee.BungeeCord;
import online.nasgar.antibot.utils.ManyChecksUtils;
import online.nasgar.antibot.caching.PacketUtils.KickType;
import online.nasgar.antibot.caching.PacketsPosition;
import online.nasgar.antibot.config.Settings;
import online.nasgar.antibot.utils.FailedUtils;

public class AntiBotThread
{

    private static Thread thread;
    private static final HashSet<String> TO_REMOVE_SET = new HashSet<>();
    private static BungeeCord bungee = BungeeCord.getInstance();

    public static void start()
    {
        ( thread = new Thread( () ->
        {
            while ( sleep( 1000 ) )
            {
                try
                {
                    long currTime = System.currentTimeMillis();
                    for ( Map.Entry<String, Connector> entryset : bungee.getAntiBot().getConnectedUsersSet().entrySet() )
                    {
                        Connector connector = entryset.getValue();
                        if ( !connector.isConnected() )
                        {
                            TO_REMOVE_SET.add( entryset.getKey() );
                            continue;
                        }
                        AntiBot.CheckState state = connector.getState();
                        switch ( state )
                        {
                            case SUCCESSFULLY:
                            case FAILED:
                                TO_REMOVE_SET.add( entryset.getKey() );
                                continue;
                            default:
                                if ( ( currTime - connector.getJoinTime() ) >= Settings.IMP.TIME_OUT )
                                {
                                    connector.failed( KickType.TIMED_OUT, state == AntiBot.CheckState.CAPTCHA_ON_POSITION_FAILED
                                            ? "Too long fall check" : "Captcha not entered" );
                                    TO_REMOVE_SET.add( entryset.getKey() );
                                    continue;
                                } else if ( state == AntiBot.CheckState.CAPTCHA_ON_POSITION_FAILED || state == AntiBot.CheckState.ONLY_POSITION )
                                {
                                    connector.sendMessage( PacketsPosition.CHECKING );
                                } else
                                {
                                    connector.sendMessage( PacketsPosition.CHECKING_CAPTCHA );
                                }
                                connector.sendPing();
                        }
                    }

                } catch ( Exception e )
                {
                    bungee.getLogger().log( Level.WARNING, "[AntiBot] Incomprehensible error. Please send it to the developer!", e );
                } finally
                {
                    if ( !TO_REMOVE_SET.isEmpty() )
                    {
                        for ( String remove : TO_REMOVE_SET )
                        {
                            bungee.getAntiBot().removeConnection( remove, null );
                        }
                        TO_REMOVE_SET.clear();
                    }
                }
            }

        }, "AntiBot thread" ) ).start();
    }

    public static void stop()
    {
        if ( thread != null )
        {
            thread.interrupt();
        }
    }

    private static boolean sleep(long time)
    {
        try
        {
            Thread.sleep( time );
        } catch ( InterruptedException ex )
        {
            return false;
        }
        return true;
    }

    public static void startCleanUpThread()
    {
        Thread t = new Thread( () ->
        {
            byte counter = 0;
            while ( !Thread.interrupted() && sleep( 5 * 1000 ) )
            {
                if ( ++counter == 12 )
                {
                    counter = 0;
                    ManyChecksUtils.cleanUP();
                    if ( bungee.getAntiBot() != null )
                    {
                        AntiBot antiBot = bungee.getAntiBot();
                        if ( antiBot.getServerPingUtils() != null )
                        {
                            antiBot.getServerPingUtils().cleanUP();
                        }
                        if ( antiBot.getSql() != null )
                        {
                            antiBot.getSql().tryCleanUP();
                        }
                        if ( antiBot.getGeoIp() != null )
                        {
                            antiBot.getGeoIp().tryClenUP();
                        }
                    }
                }
                FailedUtils.flushQueue();
            }
        }, "CleanUp thread" );
        t.setDaemon( true );
        t.start();
    }
}
