package pro.bhushan.attributeswap.mixin;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import pro.bhushan.attributeswap.AttributeSwap;

/** While a swap is being sent, queue packets without flushing so the whole swap goes out in one write. */
@Mixin(Connection.class)
public abstract class ConnectionMixin {
	@ModifyVariable(
		method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
		at = @At("HEAD"), argsOnly = true)
	private boolean attributeswap$holdFlush(boolean flush) {
		return flush && !AttributeSwap.isBatching((Connection) (Object) this);
	}
}
