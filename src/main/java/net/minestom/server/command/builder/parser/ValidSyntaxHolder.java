package net.minestom.server.command.builder.parser;

import net.minestom.server.command.builder.CommandSyntax;
import net.minestom.server.command.builder.arguments.Argument;

import java.util.Map;

/**
 * Holds the data of a validated syntax.
 */
public class ValidSyntaxHolder {
    public CommandSyntax syntax;
    public Map<Argument<?>, Object> argumentsValue;
}
