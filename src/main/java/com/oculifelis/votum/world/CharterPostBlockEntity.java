package com.oculifelis.votum.world;

import com.oculifelis.votum.data.PactDef;
import com.oculifelis.votum.data.VotumTables;
import com.oculifelis.votum.item.PactItem;
import com.oculifelis.votum.item.SealPieceItem;
import com.oculifelis.votum.pact.PactLedger;
import com.oculifelis.votum.registry.VotumBlockEntities;
import com.oculifelis.votum.registry.VotumItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Owner, the pinned pact and the owner's finished-pact count (kept so the comparator works while the owner is away).
 */
public class CharterPostBlockEntity extends VotumBlockEntity {
    private static final Set<CharterPostBlockEntity> LOADED = Collections.newSetFromMap(new WeakHashMap<>());

    private UUID owner;
    private String ownerName = "";
    private Identifier pinned;
    private int finished;

    public CharterPostBlockEntity(BlockPos pos, BlockState state) {
        super(VotumBlockEntities.CHARTER_POST.get(), pos, state);
    }

    public UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    public Identifier pinned() {
        return pinned;
    }

    public int finished() {
        return finished;
    }

    public boolean isOwner(Player player) {
        return owner != null && owner.equals(player.getUUID());
    }

    void setOwner(Player player) {
        owner = player.getUUID();
        ownerName = player.getName().getString();
        finished = PactLedger.of(player).finished().size();
        changed();
    }

    /** Brings every loaded post the player owns up to date with their finished count. */
    public static void refreshFor(ServerPlayer player) {
        int count = PactLedger.of(player).finished().size();
        for (CharterPostBlockEntity be : List.copyOf(LOADED)) {
            if (be.isRemoved() || be.level == null || !be.isOwner(player) || be.finished == count) continue;
            be.finished = count;
            be.ownerName = player.getName().getString();
            be.changed();
            be.level.updateNeighbourForOutputSignal(be.worldPosition, be.getBlockState().getBlock());
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            LOADED.add(this);
            if (owner != null && level.getServer() != null) {
                ServerPlayer p = level.getServer().getPlayerList().getPlayer(owner);
                if (p != null) refreshFor(p);
            }
        }
    }

    @Override
    public void setRemoved() {
        LOADED.remove(this);
        super.setRemoved();
    }

    public InteractionResult useItem(ItemStack stack, Player player) {
        if (level == null) return InteractionResult.PASS;
        boolean client = level.isClientSide();

        if (stack.is(VotumItems.SEALED_WARRANT.get())) {
            if (!isOwner(player)) {
                if (!client) player.sendOverlayMessage(Component.translatable("message.votum.post_not_owner", ownerName));
                return InteractionResult.SUCCESS;
            }
            Identifier id = VotumItems.pactOf(stack);
            if (id == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!client) {
                if (pinned != null) give(player, VotumItems.withPact(VotumItems.SEALED_WARRANT.get(), pinned));
                pinned = id;
                stack.consume(1, player);
                level.playSound(null, worldPosition, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 0.8F, 1.0F);
                update();
            }
            return InteractionResult.SUCCESS;
        }

        if (stack.is(VotumItems.BLANK_WARRANT.get()) && pinned != null) {
            if (!client) copy(stack, player);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private void copy(ItemStack blank, Player player) {
        PactDef pact = VotumTables.pact(pinned);
        if (pact == null) {
            player.sendOverlayMessage(Component.translatable("tooltip.votum.unknown_pact"));
            return;
        }
        Inventory inv = player.getInventory();
        int slot = -1;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(VotumItems.SEALING_WAX.get()) && pact.wax().equals(VotumItems.waxOf(s))) {
                slot = i;
                break;
            }
        }
        if (slot < 0 && !player.getAbilities().instabuild) {
            player.sendOverlayMessage(Component.translatable("message.votum.post_needs_wax",
                Component.translatable(VotumTables.wax(pact.wax()) != null ? VotumTables.wax(pact.wax()).langKey() : "item.votum.sealing_wax")));
            return;
        }
        if (slot >= 0) inv.getItem(slot).consume(1, player);
        blank.consume(1, player);
        give(player, VotumItems.withPact(VotumItems.SEALED_WARRANT.get(), pinned));
        level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5F, 1.8F);
    }

    public InteractionResult useEmpty(Player player) {
        if (level == null) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isSecondaryUseActive() && isOwner(player) && pinned != null) {
            give(player, VotumItems.withPact(VotumItems.SEALED_WARRANT.get(), pinned));
            pinned = null;
            level.playSound(null, worldPosition, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 0.8F, 0.8F);
            update();
            return InteractionResult.SUCCESS;
        }
        list(player);
        return InteractionResult.SUCCESS;
    }

    private void list(Player player) {
        PactLedger.State st = PactLedger.of(player);
        player.sendSystemMessage(Component.translatable("message.votum.post_header", ownerName).withStyle(ChatFormatting.GOLD));
        if (pinned != null) {
            player.sendSystemMessage(Component.translatable("message.votum.post_pinned", pactName(pinned)));
        }
        if (st.sworn().isEmpty() && st.finished().isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.votum.post_none").withStyle(ChatFormatting.GRAY));
        }
        st.sworn().forEach((id, s) -> {
            PactDef def = VotumTables.pact(id);
            int goal = def == null ? 1 : def.oath().goal();
            int progress = def != null && def.oath().type() == PactDef.OathType.ABSTAIN
                ? (int) Math.min(goal, (level.getGameTime() - s.since()) / 24000L) : s.progress();
            player.sendSystemMessage(Component.translatable("message.votum.post_sworn", pactName(id), progress, goal));
        });
        for (Identifier id : st.finished()) {
            player.sendSystemMessage(Component.translatable("message.votum.post_finished", pactName(id)).withStyle(ChatFormatting.GREEN));
        }
        for (ItemStack worn : com.fundamentum.slots.Slots.stacks(player, com.oculifelis.votum.pact.Pacts.SLOT)) {
            if (worn.getItem() instanceof SealPieceItem && VotumItems.isCracked(worn)) {
                PactDef def = PactItem.pactOf(worn);
                if (def != null) {
                    player.sendSystemMessage(Component.translatable("message.votum.post_cracked", pactName(def.id())).withStyle(ChatFormatting.RED));
                }
            }
        }
    }

    private static Component pactName(Identifier id) {
        PactDef def = VotumTables.pact(id);
        return def == null ? Component.literal(id.toString()) : Component.translatable(def.nameKey());
    }

    private void update() {
        BlockState s = getBlockState();
        boolean has = pinned != null;
        if (level != null && s.getValue(CharterPostBlock.PINNED) != has) {
            level.setBlock(worldPosition, s.setValue(CharterPostBlock.PINNED, has), 3);
        }
        changed();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null && pinned != null) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                VotumItems.withPact(VotumItems.SEALED_WARRANT.get(), pinned));
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (owner != null) out.putString("owner", owner.toString());
        out.putString("owner_name", ownerName);
        if (pinned != null) out.putString("pinned", pinned.toString());
        out.putInt("finished", finished);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        String o = in.getStringOr("owner", "");
        try {
            owner = o.isEmpty() ? null : UUID.fromString(o);
        } catch (IllegalArgumentException e) {
            owner = null;
        }
        ownerName = in.getStringOr("owner_name", "");
        String p = in.getStringOr("pinned", "");
        pinned = p.isEmpty() ? null : Identifier.tryParse(p);
        finished = in.getIntOr("finished", 0);
    }
}
