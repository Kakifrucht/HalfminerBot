package de.halfminer.hmtsbot.actions;

import com.github.theholywaffle.teamspeak3.api.event.TextMessageEvent;

public class CommandLine {

    /**
     * ClientId (not uniqueID) of the invoker
     */
    private final int invoker;

    //Information about the command
    private final String line;
    private final String[] parsedLine;
    private final String command;
    private final String commandLine;

    public CommandLine(TextMessageEvent event) {

        this.invoker = event.getInvokerId();

        StringBuilder sb = new StringBuilder(event.getMessage());
        if (!event.getMessage().startsWith("!")) { // build standard command
            sb.insert(0, "!channelcreate ");
        }

        this.line = sb.toString();
        this.parsedLine = sb.toString().split("\\s+");
        this.command = parsedLine[0].substring(1);
        if (line.contains(" ")) commandLine = line.substring(line.indexOf(' ') + 1);
        else commandLine = "";
    }

    /**
     * @return ClientId (not uniqueID) of the invoker
     */
    public int getClientId() {
        return invoker;
    }

    /**
     * @return the whole commandline (chat message)
     */
    public String getLine() {
        return line;
    }

    /**
     * @return the whole commandline seperated at spaces
     */
    public String[] getParsedLine() {
        return parsedLine;
    }

    /**
     * @return only the command (without !)
     */
    public String getCommand() {
        return command;
    }

    /**
     * @return only the commandline (not the command)
     */
    public String getCommandLine() {
        return commandLine;
    }


}
