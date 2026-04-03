package me.bill.fakePlayerPlugin.fakeplayer.network;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * A fake {@link Connection} subclass used for bot players.
 *
 * <p>Extends NMS {@code Connection} directly so that all {@code instanceof} checks
 * in NMS and Paper code (session tracking, protocol-state management, etc.) work
 * normally without producing ghost / "Anonymous User" entries.
 *
 * <h3>Why a subclass and not a plain Connection + injected FakeChannel?</h3>
 * <p>When a plain {@code Connection} is created via reflection and then has a
 * {@link FakeChannel} injected post-construction, NMS's session-tracking or
 * connection-manager code (which runs inside {@code placeNewPlayer()} → vanilla
 * {@code ServerGamePacketListenerImpl} constructor) calls {@code Connection.send()}
 * with initial login/state packets.  The vanilla {@code send()} has side-effects
 * beyond just writing to the channel (protocol-state registers, pending-packet
 * queues, callback notifications) that can create a phantom "Anonymous User" entry
 * with UUID 0000…0000 visible in server player-list tools.
 *
 * <p>Overriding {@code send()} as a no-op here prevents those side-effects entirely.
 * The {@link FakeServerGamePacketListenerImpl} layer (injected after
 * {@code placeNewPlayer()}) handles knockback and also discards all subsequent
 * outbound packets — so bots never receive any network traffic.
 *
 * <h3>Pipeline setup</h3>
 * <p>{@code Connection.configureSerialization()} is called on the
 * {@link FakeChannelPipeline}.  Since {@code FakeChannelPipeline.addLast()} is a
 * no-op, no real handlers are installed — but calling the method satisfies any
 * NMS internal checks that verify protocol setup was performed.
 *
 * <p>Based on the hello09x/fakeplayer reference implementation.
 */
public final class FakeConnection extends Connection {

    /**
     * Creates a fake connection backed by a {@link FakeChannel}.
     *
     * @param address the loopback / display address used for the fake remote endpoint
     */
    public FakeConnection(InetAddress address) {
        super(PacketFlow.SERVERBOUND);
        this.channel = new FakeChannel(null, address);
        this.address = new InetSocketAddress(address, 25565);
        // configureSerialization() calls addLast() on the FakeChannelPipeline,
        // which is a no-op — but it satisfies any NMS internal "pipeline ready"
        // checks and mirrors the real login flow exactly.
        Connection.configureSerialization(this.channel.pipeline(), PacketFlow.SERVERBOUND, false, null);
    }

    /**
     * Always reports this fake connection as connected so NMS does not try to
     * treat the bot as disconnected during server-side processing.
     */
    @Override
    public boolean isConnected() {
        return true;
    }

    // ── No-op send() overloads ─────────────────────────────────────────────────
    // Overriding these prevents vanilla Connection.send()'s side-effects
    // (protocol-state updates, pending-ack queues, callback notifications) from
    // running during placeNewPlayer(), which is what creates the "Anonymous User"
    // ghost entry.  The FakeServerGamePacketListenerImpl layer (injected after
    // placeNewPlayer()) handles knockback independently via its own send() override.

    @Override
    public void send(@NotNull Packet<?> packet) {
        // no-op: bots have no real client to receive packets
    }

    @Override
    public void send(@NotNull Packet<?> packet, @Nullable ChannelFutureListener listener) {
        // no-op
    }

    @Override
    public void send(@NotNull Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush) {
        // no-op
    }
}



