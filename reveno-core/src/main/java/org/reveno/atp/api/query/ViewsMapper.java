package org.reveno.atp.api.query;

@FunctionalInterface
public interface ViewsMapper<Entity, View> {

	View map(long id, Entity entity, MappingContext repository);
}
