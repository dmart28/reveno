package org.reveno.atp.core.api;

import org.reveno.atp.api.commands.CommandContext;

/**
 * Used for generating unique ID of entities across current domain model.
 *
 * @author Artem Dmitriev <art.dm.ser@gmail.com>
 */
public interface IdGenerator {

    void context(CommandContext context);

    long next(Class<?> entityType);

}
