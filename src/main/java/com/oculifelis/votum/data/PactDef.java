package com.oculifelis.votum.data;

import com.fundamentum.damage.Elements;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One pact from {@code data/<ns>/votum/pacts/<id>.json}: which signet and wax seal it, the oath to finish, the clause
 * never to break, and the boon its ring or pendant gives while worn.
 */
public record PactDef(
    Identifier id,
    Identifier signet,
    Identifier wax,
    Piece piece,
    Identifier faction,
    int reward,
    int penalty,
    Oath oath,
    Clause clause,
    Boon boon
) {
    public enum Piece { RING, PENDANT }

    public enum OathType { KILL, BREED, TRADE, ABSTAIN }

    public enum ClauseType { KILL, HURT, DIE_TO }

    /** The deed to finish. {@code days} only counts for ABSTAIN: in-game days sworn with the clause kept. */
    public record Oath(OathType type, Targets targets, int count, int days) {
        public int goal() {
            return type == OathType.ABSTAIN ? days : count;
        }
    }

    public record Clause(ClauseType type, Targets targets) { }

    public record Mod(Identifier attribute, Identifier element, boolean resistance, double amount, AttributeModifier.Operation operation) {
        public Holder<Attribute> resolve() {
            if (element != null) {
                Elements.Element el = findElement(element);
                if (el == null) return null;
                return resistance ? el.resistance() : el.damage();
            }
            if (attribute == null) return null;
            return BuiltInRegistries.ATTRIBUTE.get(attribute).<Holder<Attribute>>map(h -> h).orElse(null);
        }
    }

    /**
     * What a worn, uncracked piece gives: attribute changes, creature kinds that will not target the wearer, safety
     * from enderman stares, and standing with the pact's faction.
     */
    public record Boon(List<Mod> modifiers, Targets truce, boolean gazeSafe, int standing) { }

    public static PactDef fromJson(JsonObject o) {
        Identifier id = Identifier.parse(o.get("pact").getAsString());
        JsonObject oj = o.has("oath") ? o.getAsJsonObject("oath") : new JsonObject();
        Oath oath = new Oath(
            OathType.valueOf(str(oj, "type", "kill").toUpperCase(Locale.ROOT)),
            Targets.parse(oj.get("targets")),
            oj.has("count") ? Math.max(1, oj.get("count").getAsInt()) : 1,
            oj.has("days") ? Math.max(1, oj.get("days").getAsInt()) : 1);
        JsonObject cj = o.has("clause") ? o.getAsJsonObject("clause") : new JsonObject();
        Clause clause = new Clause(
            ClauseType.valueOf(str(cj, "type", "kill").toUpperCase(Locale.ROOT)),
            Targets.parse(cj.get("targets")));
        JsonObject bj = o.has("boon") ? o.getAsJsonObject("boon") : new JsonObject();
        List<Mod> mods = new ArrayList<>();
        if (bj.has("modifiers")) {
            for (JsonElement e : bj.getAsJsonArray("modifiers")) {
                JsonObject m = e.getAsJsonObject();
                Identifier attr = m.has("attribute") ? Identifier.tryParse(m.get("attribute").getAsString()) : null;
                Identifier el = m.has("element") ? elementId(m.get("element").getAsString()) : null;
                boolean res = !"damage".equals(str(m, "kind", "resistance"));
                AttributeModifier.Operation op = switch (str(m, "operation", "add_value")) {
                    case "add_multiplied_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
                    case "add_multiplied_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
                    default -> AttributeModifier.Operation.ADD_VALUE;
                };
                mods.add(new Mod(attr, el, res, m.has("amount") ? m.get("amount").getAsDouble() : 0, op));
            }
        }
        Boon boon = new Boon(List.copyOf(mods), Targets.parse(bj.get("truce")),
            bj.has("gaze_safe") && bj.get("gaze_safe").getAsBoolean(),
            bj.has("standing") ? bj.get("standing").getAsInt() : 0);
        String faction = str(o, "faction", null);
        return new PactDef(id,
            Identifier.parse(str(o, "signet", "votum:skull_signet")),
            Identifier.parse(str(o, "wax", "votum:bone")),
            "pendant".equals(str(o, "piece", "ring")) ? Piece.PENDANT : Piece.RING,
            faction == null ? null : Identifier.parse(faction),
            o.has("reward") ? o.get("reward").getAsInt() : 0,
            o.has("penalty") ? o.get("penalty").getAsInt() : 0,
            oath, clause, boon);
    }

    public Item signetItem() {
        return BuiltInRegistries.ITEM.getOptional(signet).orElse(null);
    }

    public String nameKey() {
        return "pact." + id.getNamespace() + "." + id.getPath();
    }

    public String oathKey() {
        return nameKey() + ".oath";
    }

    public String clauseKey() {
        return nameKey() + ".clause";
    }

    public String boonKey() {
        return nameKey() + ".boon";
    }

    static Identifier elementId(String s) {
        return s.contains(":") ? Identifier.tryParse(s) : Identifier.fromNamespaceAndPath("fundamentum", s);
    }

    static Elements.Element findElement(Identifier id) {
        Elements.Element el = Elements.get(id);
        if (el != null) return el;
        for (Elements.Element e : Elements.all()) {
            if (e.id().getPath().equals(id.getPath())) return e;
        }
        return null;
    }

    static String str(JsonObject o, String key, String def) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : def;
    }
}
