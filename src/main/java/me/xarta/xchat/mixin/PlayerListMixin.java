
package me.xarta.xchat.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Redirect(
            method = {"placeNewPlayer", "remove"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"
            )
    )
    private void xchat$filterJoinLeave(PlayerList instance, Component message, boolean bypassHiddenChat) {
        if (xchat$isJoinOrLeave(message)) {
            return;
        }
        instance.broadcastSystemMessage(message, bypassHiddenChat);
    }

    @Unique
    private static boolean xchat$isJoinOrLeave(Component comp) {
        if (comp.getContents() instanceof TranslatableContents tc) {
            String key = tc.getKey();
            return "multiplayer.player.joined".equals(key)
                    || "multiplayer.player.left".equals(key)
                    || "multiplayer.player.joined.renamed".equals(key);
        }
        return false;
    }
}