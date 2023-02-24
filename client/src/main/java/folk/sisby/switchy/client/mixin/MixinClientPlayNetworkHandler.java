package folk.sisby.switchy.client.mixin;

import folk.sisby.switchy.client.SwitchyClientCommands;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.ChatCommandC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Garden System
 * @since 2.0.0
 * @see SwitchyClientCommands
 * Records server commands before they're sent into a map of previous commands per player UUID.
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler {

	@Inject(at = @At("HEAD"), method = "sendPacket")
	public void sendPacket(Packet<?> packet, CallbackInfo ci)
	{
		if (packet instanceof ChatCommandC2SPacket chatPacket)
		{
			SwitchyClientCommands.HISTORY = chatPacket.command();
		}
	}
}
