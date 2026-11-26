package com.oculifelis.votum.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

/** A list of entity ids and {@code #tags} read from data. An empty list matches nothing. */
public record Targets(List<Identifier> ids, List<TagKey<EntityType<?>>> tags) {
    public static final Targets NONE = new Targets(List.of(), List.of());

    public static Targets parse(JsonElement el) {
        if (el == null || el.isJsonNull()) return NONE;
        List<Identifier> ids = new ArrayList<>();
        List<TagKey<EntityType<?>>> tags = new ArrayList<>();
        JsonArray arr;
        if (el.isJsonArray()) {
            arr = el.getAsJsonArray();
        } else {
            arr = new JsonArray();
            arr.add(el);
        }
        for (JsonElement e : arr) {
            String s = e.getAsString();
            if (s.startsWith("#")) {
                Identifier id = Identifier.tryParse(s.substring(1));
                if (id != null) tags.add(TagKey.create(Registries.ENTITY_TYPE, id));
            } else {
                Identifier id = Identifier.tryParse(s);
                if (id != null) ids.add(id);
            }
        }
        return new Targets(List.copyOf(ids), List.copyOf(tags));
    }

    public boolean isEmpty() {
        return ids.isEmpty() && tags.isEmpty();
    }

    public boolean matches(Entity entity) {
        if (entity == null || isEmpty()) return false;
        EntityType<?> type = entity.getType();
        for (TagKey<EntityType<?>> tag : tags) {
            if (type.builtInRegistryHolder().is(tag)) return true;
        }
        if (!ids.isEmpty()) {
            Identifier key = EntityType.getKey(type);
            return ids.contains(key);
        }
        return false;
    }
}
