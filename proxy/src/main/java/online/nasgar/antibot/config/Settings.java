package online.nasgar.antibot.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Settings extends Config
{

    @Ignore
    public static final Settings IMP = new Settings();

    @Final
    public String BOT_FILTER_VERSION = "3.8.11";

    @Create
    public MESSAGES MESSAGES;
    @Create
    public GEO_IP GEO_IP;
    @Create
    public PING_CHECK PING_CHECK;
    @Create
    public SERVER_PING_CHECK SERVER_PING_CHECK;
    @Create
    public PROTECTION PROTECTION;
    @Create
    public SQL SQL;
    @Comment(
        {
                "How many players / bots must enter in 1 minute for the protection to turn on",
                "Recommended settings when no ads: ",
                "Up to 150 online - 25, up to 250 - 30, up to 350 - 35, up to 550 - 40.45, above - adjust for yourself",
                "During advertising or when toko, toko have set protection, it is recommended to increase these values"
        })
    public int PROTECTION_THRESHOLD = 30;
    @Comment("How long is auto protection active? In milliseconds. 1 sec = 1000")
    public int PROTECTION_TIME = 120000;
    @Comment("Whether to check for a bot when entering the server during a bot attack, regardless of whether it passed the check or not")
    public boolean FORCE_CHECK_ON_ATTACK = true;
    @Comment("Show online from filter")
    public boolean SHOW_ONLINE = true;
    @Comment("How much time the player has to clear the defense. In milliseconds. 1 sec = 1000")
    public int TIME_OUT = 12700;
    @Comment("Should the fix from 'Team 'xxx' already exist in this scoreboard'")
    public boolean FIX_SCOREBOARD_TEAMS = true;
    @Comment("Should I write the IP addresses of players / bots that failed the check to a file?")
    public boolean SAVE_FAILED_IPS_TO_FILE = true;

    public void reload(File file)
    {
        load( file );
        save( file );
    }

    @Comment("Don't use '\\n', use %nl%")
    public static class MESSAGES
    {

        public String PREFIX = "&c&lAntiBot";
        public String CHECKING = "%prefix% &aWait for validation to complete...";
        public String CHECKING_CAPTCHA = "%prefix% & Enter the number from the picture in the chat";
        public String CHECKING_CAPTCHA_WRONG = "%prefix% &cYou entered the captcha incorrectly, please try again. You have &a%s &c%s";
        public String SUCCESSFULLY = "%prefix% &aTest passed, enjoy your game";
        public String KICK_MANY_CHECKS = "%prefix%%nl%%nl%&c Suspicious activity detected from your ip%nl%%nl%&aPlease try again in 10 minutes";
        public String KICK_NOT_PLAYER = "%prefix%%nl%%nl%&cYou are not verified, you may be a bot%nl%&7&oIf not, please try again";
        public String KICK_COUNTRY = "%prefix%%nl%%nl%&c Your country is not allowed on the server";
        public String KICK_BIG_PING = "%prefix%%nl%%nl%&cYour ping is very high, most likely you are a bot";
        @Comment(
            {
                    "Title%nl%Subtitle", "Leave blank to disable( ex: CHECKING_TITLE = \"\" )",
                    "Disabling titles may slightly improve performance"
            })
        public String CHECKING_TITLE = "&c&lAntiBot%nl%&aChecking";
        public String CHECKING_TITLE_SUS = "&c Test passed%nl%& Have fun";
        public String CHECKING_TITLE_CAPTCHA = "%nl%&rEnter the captcha into the chat!";
    }

    @Comment("Enable or disable GeoIp")
    public static class GEO_IP
    {

        @Comment(
            {
                    "When validation works",
                    "0 - Always",
                    "1 - Only during bot attack",
                    "2 - Disable"
            })
        public int MODE = 1;
        @Comment(
            {
                    "How exactly does GeoIp work",
                    "0 - White list(Only those countries that are on the list can enter)",
                    "1 - Black list (Only those countries that are not on the list can enter)"
            })
        public int TYPE = 0;
        @Comment(
            {
                    "Where to download GEOIP",
                    "Change the link if for some reason it does not download for this one",
                    "File must end with .mmdb or be packed into .tar.gz"
            })
        public String NEW_GEOIP_DOWNLOAD_URL = "https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-Country&license_key=%license_key%&suffix=tar.gz";
        @Comment(
            {
                    "If the key stops working, then in order to get a new one, you need to register at https://www.maxmind.com/",
                    "and generate a new key at https://www.maxmind.com/en/accounts/current/license-key"
            })
        public String MAXMIND_LICENSE_KEY = "P5g0fVdAQIq8yQau";
        @Comment("Allowed countries")
        public List<String> ALLOWED_COUNTRIES = Arrays.asList( "RU", "UA", "BY", "KZ", "EE", "MD", "KG", "AZ", "LT", "LV", "GE", "PL" );
    }

    @Comment("Enable or disable high ping check")
    public static class PING_CHECK
    {

        @Comment(
            {
                    "When validation works",
                    "0 - Always",
                    "1 - Only during bot attack",
                    "2 - Disable"
            })
        public int MODE = 1;
        @Comment("Maximum allowed ping")
        public int MAX_PING = 350;
    }

    @Comment("Enable or disable check for direct connection")
    public static class SERVER_PING_CHECK
    {

        @Comment(
            {
                    "When validation works",
                    "0 - Always",
                    "1 - Only during bot attack",
                    "2 - Disable",
                    "Disabled by default, as it is not very stable during strong attacks"
            })
        public int MODE = 2;
        @Comment("How long can you enter the server after receiving the modd server")
        public int CACHE_TIME = 12;
        public List<String> KICK_MESSAGE = new ArrayList()
        {
            {
                add( "%nl%" );
                add( "%nl%" );
                add( "&cYou were kicked! Don't use a direct connection" );
            }
        };
    }

    @Comment(
        {
                "Setting how the protection will work",
                "0 - Captcha check only",
                "1 - Drop test + captcha",
                "2 - Check for a fall, if failed, then captcha"
        })
    public static class PROTECTION
    {

        @Comment("Operating mode until no attack")
        public int NORMAL = 2;
        @Comment("Working mode during attack")
        public int ON_ATTACK = 1;
        @Comment(
            {
                    "Should I enable constant checking of players on entry?",
                    "When enabling this feature, don't forget to increase the protection-threshold limits"
            })
        public boolean ALWAYS_CHECK = false;

        @Comment(
            {
                    "Should we check players whose ip is 127.0.0.1?", "May be useful when using Geyser",
                    "0 - check", "1 - disable check", "2 - check on every visit"
            })
        public int CHECK_LOCALHOST = 0;

        @Comment("Can I disable verification for clients with Geyser-standalone? Authorization type must be floodgate.")
        public boolean SKIP_GEYSER = false;
        /*
        @Comment(
                {
                    "Cuando funcionan las comprobaciones de protocolo adicionales",
                    " (Paquetes a los que el cliente siempre debe responder)",
                    "0 - Siempre",
                    "1 - Solo durante el ataque del bot",
                    "2 - Deshabilitar"
                })
        public int COMPROBACIONES_ADICIONALES = 1;
         */
    }

    @Comment("Database setup")
    public static class SQL
    {

        @Comment("Database type. sqlite or mysql")
        public String STORAGE_TYPE = "sqlite";
        @Comment("After how many days to remove players from the database who passed the verification and did not log in again. 0 or less to disable")
        public int PURGE_TIME = 14;
        @Comment("settings for mysql")
        public String HOSTNAME = "127.0.0.1";
        public int PORT = 3306;
        public String USER = "user";
        public String PASSWORD = "password";
        public String DATABASE = "database";
    }
}
