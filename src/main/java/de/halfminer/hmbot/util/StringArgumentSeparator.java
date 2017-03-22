package de.halfminer.hmbot.util;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Helper class to parse a String into arguments and easily convert between different types.
 */
@SuppressWarnings("ALL")
public class StringArgumentSeparator {

    private final String concatenated;
    private String[] arguments;
    private int startingPoint = 0;

    public StringArgumentSeparator(String string) {
        this.concatenated = string;
        arguments = doSplit(string, ' ');
    }

    public StringArgumentSeparator(String string, char separator) {
        this.concatenated = string;
        arguments = doSplit(string, separator);
    }

    public StringArgumentSeparator(String[] strings) {
        String concat = "";
        for (String str : strings) {
            concat += str;
        }
        this.concatenated = concat;
        arguments = strings;
    }

    private String[] doSplit(String toSplit, char separator) {
        return toSplit.split(Pattern.quote(separator + ""));
    }

    public String[] getArguments() {
        return arguments;
    }

    public String getConcatenatedString() {
        return concatenated;
    }

    public int getLength() {
        return arguments.length;
    }

    public StringArgumentSeparator removeFirstElement() {
        if (arguments.length == 1)
            arguments = new String[0];
        else
            arguments = Arrays.copyOfRange(arguments, 1, arguments.length);
        return this;
    }

    public boolean meetsLength(int length) {
        return getLength() >= length;
    }

    public String getArgument(int arg) {
        if (!meetsLength(arg + 1)) return "";
        return arguments[arg];
    }

    /**
     * Gets the argument as integer.
     *
     * @param arg argument to get
     * @return parsed integer or Integer.MIN_VALUE if couldn't be parsed
     */
    public int getArgumentInt(int arg) {
        String str = getArgument(arg).replaceAll(" ", "");
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return Integer.MIN_VALUE;
        }
    }

    public int getArgumentIntMinimum(int arg, int minimum) {
        if (!meetsLength(arg + 1)) return minimum;
        return Math.max(minimum, getArgumentInt(arg));
    }
}
