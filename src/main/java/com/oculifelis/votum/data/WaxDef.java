package com.oculifelis.votum.data;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** One colour of Sealing Wax: its tint and the pigment that makes it in a Seal Brazier. */
public record WaxDef(Identifier id, int color, String pigment) {
    public static WaxDef fromJson(JsonObject o) {
        Identifier id = Identifier.parse(o.get("wax").getAsString());
        return new WaxDef(id, parseColor(o.has("color") ? o.get("color").getAsString() : "#FFFFFF"),
            o.has("pigment") ? o.get("pigment").getAsString() : "");
    }

    public boolean isPigment(ItemStack stack) {
        if (stack.isEmpty() || pigment.isEmpty()) return false;
        if (pigment.startsWith("#")) {
            Identifier tag = Identifier.tryParse(pigment.substring(1));
            return tag != null && stack.is(TagKey.create(Registries.ITEM, tag));
        }
        Identifier item = Identifier.tryParse(pigment);
        return item != null && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(item);
    }

    public Item pigmentItem() {
        if (pigment.startsWith("#")) return null;
        Identifier item = Identifier.tryParse(pigment);
        return item == null ? null : BuiltInRegistries.ITEM.getOptional(item).orElse(null);
    }

    public String langKey() {
        return "wax." + id.getNamespace() + "." + id.getPath();
    }

    static int parseColor(String s) {
        String hex = s.startsWith("#") ? s.substring(1) : s;
        try {
            return Integer.parseInt(hex, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }
}
