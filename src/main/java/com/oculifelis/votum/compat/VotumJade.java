package com.oculifelis.votum.compat;

import com.oculifelis.votum.Votum;
import com.oculifelis.votum.data.PactDef;
import com.oculifelis.votum.data.VotumTables;
import com.oculifelis.votum.data.WaxDef;
import com.oculifelis.votum.world.CharterPostBlock;
import com.oculifelis.votum.world.CharterPostBlockEntity;
import com.oculifelis.votum.world.SealBrazierBlock;
import com.oculifelis.votum.world.SealBrazierBlockEntity;
import com.oculifelis.votum.world.SignetDeskBlock;
import com.oculifelis.votum.world.SignetDeskBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/** Jade lines for the brazier, desk and charter post. All of it comes from block entity data already synced to clients. */
@WailaPlugin
public final class VotumJade implements IWailaPlugin {
    private static final Identifier UID = Votum.id("blocks");

    @Override
    public void registerClient(IWailaClientRegistration r) {
        Lines lines = new Lines();
        r.registerBlockComponent(lines, SealBrazierBlock.class);
        r.registerBlockComponent(lines, SignetDeskBlock.class);
        r.registerBlockComponent(lines, CharterPostBlock.class);
    }

    static Component waxName(Identifier wax) {
        WaxDef def = wax == null ? null : VotumTables.wax(wax);
        MutableComponent c = Component.translatable(def == null ? "item.votum.sealing_wax" : def.langKey());
        return def == null ? c : c.withColor(def.color());
    }

    static Component pactName(Identifier pact) {
        PactDef def = pact == null ? null : VotumTables.pact(pact);
        return def == null ? Component.translatable("tooltip.votum.unknown_pact") : Component.translatable(def.nameKey());
    }

    static final class Lines implements IBlockComponentProvider {
        @Override
        public void appendTooltip(ITooltip t, BlockAccessor a, IPluginConfig config) {
            switch (a.getBlockEntity()) {
                case SealBrazierBlockEntity be -> {
                    if (be.honey() > 0) t.add(Component.translatable("jade.votum.honey", be.honey(), SealBrazierBlockEntity.MAX_HONEY));
                    if (be.wax() != null) {
                        t.add(waxName(be.wax()));
                        long left = be.remaining();
                        t.add(left > 0 ? Component.translatable("jade.votum.melting", (left + 19) / 20)
                            : Component.translatable("jade.votum.ready"));
                    }
                    if (!be.hasHeat()) t.add(Component.translatable("jade.votum.no_heat"));
                }
                case SignetDeskBlockEntity be -> {
                    if (!be.held().isEmpty()) t.add(be.held().getHoverName());
                    if (be.waxCount() > 0) {
                        t.add(Component.translatable("jade.votum.wax_count", be.waxCount(), SignetDeskBlockEntity.MAX_WAX)
                            .append(" ").append(waxName(be.wax())));
                    }
                }
                case CharterPostBlockEntity be -> {
                    if (!be.ownerName().isEmpty()) t.add(Component.translatable("jade.votum.owner", be.ownerName()));
                    if (be.pinned() != null) t.add(Component.translatable("jade.votum.pinned", pactName(be.pinned())));
                    t.add(Component.translatable("jade.votum.finished", be.finished()));
                }
                case null, default -> { }
            }
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
