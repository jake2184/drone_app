package jake.imperial.drone.utils;

/**
 * Build topic strings used by the application.
 */
public class TopicFactory {
    private final static String TAG = TopicFactory.class.getName();

    /**
     * @param event The event to create a topic string for.
     * @return The event topic for the specified event string.
     */
    public static String getEventTopic(String event) {
        return Constants.EVENT_TOPIC + event + Constants.FORMAT_TOPIC;
    }

    /**
     * @param command The command to create a topic string for.
     * @return The command topic for the specified command string.
     */
    public static String getCommandTopic(String command) {
        return Constants.COMMAND_TOPIC + command + Constants.FORMAT_TOPIC;
    }

    public static String getTextMessage(String text) {
        return "{\"d\":{" +
                "\"text\":\"" + text + "\"" +
                " } }";
    }
}
