package online.nasgar.antibot.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.md_5.bungee.BungeeCord;
import online.nasgar.antibot.AntiBot;
import online.nasgar.antibot.AntiBotUser;
import online.nasgar.antibot.config.Settings;

public class Sql
{

    private final AntiBot antiBot;
    private Connection connection;
    private boolean connecting = false;
    private long nextCleanUp = System.currentTimeMillis() + ( 60000 * 60 * 2 ); // + 2 hours

    private final ExecutorService executor = Executors.newFixedThreadPool( 2, new ThreadFactoryBuilder().setNameFormat( "AntiBot-SQL-%d" ).build() );
    private final Logger logger = BungeeCord.getInstance().getLogger();

    public Sql(AntiBot antiBot)
    {
        this.antiBot = antiBot;
        setupConnect();
    }

    private void setupConnect()
    {

        try
        {
            connecting = true;
            if ( executor.isShutdown() )
            {
                return;
            }
            if ( connection != null && connection.isValid( 3 ) )
            {
                return;
            }
            logger.info( "[AntiBot] Connecting to the database..." );
            long start = System.currentTimeMillis();
            if ( Settings.IMP.SQL.STORAGE_TYPE.equalsIgnoreCase( "mysql" ) )
            {
                Settings.SQL s = Settings.IMP.SQL;
                connectToDatabase( String.format( "JDBC:mysql://%s:%s/%s?useSSL=false&useUnicode=true&characterEncoding=utf-8", s.HOSTNAME, String.valueOf( s.PORT ), s.DATABASE ), s.USER, s.PASSWORD );
            } else
            {
                Class.forName( "org.sqlite.JDBC" );
                connectToDatabase( "JDBC:sqlite:AntiBot/database.db", null, null );
            }
            logger.log( Level.INFO, "[AntiBot] Connected ({0} мс)", System.currentTimeMillis() - start );
            createTable();
            alterLastJoinColumn();
            clearOldUsers();
            loadUsers();
        } catch ( SQLException | ClassNotFoundException e )
        {
            logger.log( Level.WARNING, "Can not connect to database or execute sql: ", e );
            connection = null;
        } finally
        {
            connecting = false;
        }
    }

    private void connectToDatabase(String url, String user, String password) throws SQLException
    {
        this.connection = DriverManager.getConnection( url, user, password );
    }

    private void createTable() throws SQLException
    {
        String sql = "CREATE TABLE IF NOT EXISTS `Users` ("
                + "`Name` VARCHAR(16) NOT NULL PRIMARY KEY UNIQUE,"
                + "`Ip` VARCHAR(16) NOT NULL,"
                + "`LastCheck` BIGINT NOT NULL,"
                + "`LastJoin` BIGINT NOT NULL);";

        try ( PreparedStatement statement = connection.prepareStatement( sql ) )
        {
            statement.executeUpdate();
        }
    }

    private void alterLastJoinColumn()
    {
        try ( ResultSet rs = connection.getMetaData().getColumns( null, null, "Users", "LastJoin" ) )
        {
            if ( !rs.next() )
            {
                try ( Statement st = connection.createStatement() )
                {
                    st.executeUpdate( "ALTER TABLE `Users` ADD COLUMN `LastJoin` BIGINT NOT NULL DEFAULT 0;" );
                    st.executeUpdate( "UPDATE `Users` SET LastJoin = LastCheck" );
                }
            }
        } catch ( Exception e )
        {
            logger.log( Level.WARNING, "[AntiBot] Error adding column to table", e );
        }
    }

    private void clearOldUsers() throws SQLException
    {
        if ( Settings.IMP.SQL.PURGE_TIME <= 0 )
        {
            return;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.add( Calendar.DATE, -Settings.IMP.SQL.PURGE_TIME );
        long until = calendar.getTimeInMillis();
        try ( PreparedStatement statement = connection.prepareStatement( "SELECT `Name` FROM `Users` WHERE `LastJoin` < " + until + ";" ) )
        {
            ResultSet set = statement.executeQuery();
            while ( set.next() )
            {
                antiBot.removeUser( set.getString( "Name" ) );
            }
        }
        if ( this.connection != null )
        {
            try ( PreparedStatement statement = connection.prepareStatement( "DELETE FROM `Users` WHERE `LastJoin` < " + until + ";" ) )
            {
                logger.log( Level.INFO, "[AntiBot] {0} accounts cleared", statement.executeUpdate() );
            }
        }
    }

    private void loadUsers() throws SQLException
    {
        try ( PreparedStatement statament = connection.prepareStatement( "SELECT * FROM `Users`;" );
                ResultSet set = statament.executeQuery() )
        {
            int i = 0;
            while ( set.next() )
            {
                String name = set.getString( "Name" );
                String ip = set.getString( "Ip" );
                if ( isInvalidName( name ) )
                {
                    removeUser( "REMOVE FROM `Users` WHERE `Ip` = '" + ip + "' AND `LastCheck` = '" + set.getLong( "LastCheck" ) + "';" );
                    continue;
                }
                long lastCheck = set.getLong( "LastCheck" );
                long lastJoin = set.getLong( "LastJoin" );
                AntiBotUser antiBotUser = new AntiBotUser( name, ip, lastCheck, lastJoin );
                antiBot.addUserToCache(antiBotUser);
                i++;
            }
            logger.log( Level.INFO, "[AntiBot] Whitelist of players uploaded successfully ({0})", i );
        }
    }

    private boolean isInvalidName(String name)
    {
        return name.contains( "'" ) || name.contains( "\"" );
    }

    private void removeUser(String sql)
    {
        if ( connection != null )
        {
            this.executor.execute( () ->
            {
                try ( PreparedStatement statament = connection.prepareStatement( sql ) )
                {
                    statament.execute();
                } catch ( SQLException ignored )
                {
                }
            } );
        }
    }

    public void saveUser(AntiBotUser antiBotUser)
    {
        if ( connecting || isInvalidName( antiBotUser.getName() ) )
        {
            return;
        }
        if ( connection != null )
        {
            this.executor.execute( () ->
            {
                final long timestamp = System.currentTimeMillis();
                String sql = "SELECT `Name` FROM `Users` where `Name` = '" + antiBotUser.getName() + "' LIMIT 1;";
                try ( Statement statament = connection.createStatement();
                        ResultSet set = statament.executeQuery( sql ) )
                {
                    if ( !set.next() )
                    {
                        sql = "INSERT INTO `Users` (`Name`, `Ip`, `LastCheck`, `LastJoin`) VALUES "
                            + "('" + antiBotUser.getName() + "','" + antiBotUser.getIp() + "',"
                            + "'" + antiBotUser.getLastCheck() + "','" + antiBotUser.getLastJoin() + "');";
                        statament.executeUpdate( sql );
                    } else
                    {
                        sql = "UPDATE `Users` SET `Ip` = '" + antiBotUser.getIp() + "', `LastCheck` = '" + antiBotUser.getLastCheck() + "',"
                            + " `LastJoin` = '" + antiBotUser.getLastJoin() + "' where `Name` = '" + antiBotUser.getName() + "';";
                        statament.executeUpdate( sql );
                    }
                } catch ( SQLException ex )
                {
                    logger.log( Level.WARNING, "[AntiBot] Can't query database", ex );
                    logger.log( Level.WARNING, sql );
                    executor.execute( () -> setupConnect() );
                }
            } );
        }
    }

    public void tryCleanUP()
    {
        if ( Settings.IMP.SQL.PURGE_TIME > 0 && nextCleanUp - System.currentTimeMillis() <= 0 )
        {
            nextCleanUp = System.currentTimeMillis() + ( 60000 * 60 * 2 ); // + 2 hours
            try
            {
                clearOldUsers();
            } catch ( SQLException ex )
            {
                setupConnect();
                logger.log( Level.WARNING, "[AntiBot] Can't clear user", ex );
            }
        }
    }

    public void close()
    {
        this.executor.shutdownNow();
        try
        {
            if ( connection != null )
            {
                this.connection.close();
            }
        } catch ( SQLException ignore )
        {
        }
        this.connection = null;
    }
}
