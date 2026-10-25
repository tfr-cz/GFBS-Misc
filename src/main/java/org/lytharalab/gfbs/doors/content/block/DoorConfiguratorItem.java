package org.lytharalab.gfbs.doors.content.block;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class DoorConfiguratorItem extends Item {
    public DoorConfiguratorItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gfbs_doors.door_configurator.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.gfbs_doors.door_configurator.2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
