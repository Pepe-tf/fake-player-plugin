package me.bill.fakePlayerPlugin.command;

import me.bill.fakePlayerPlugin.fakeplayer.FakePlayerManager;
import org.bukkit.command.CommandSender;

/**
 * {@code /fpp summon [amount]} — alias for {@link SpawnCommand}.
 */
public class SummonCommand implements FppCommand {

    private final SpawnCommand delegate;

    public SummonCommand(FakePlayerManager manager) {
        this.delegate = new SpawnCommand(manager);
    }

    @Override public String getName()        { return "summon"; }
    @Override public String getUsage()       { return "[amount]"; }
    @Override public String getDescription() { return "Alias for /fpp spawn."; }
    @Override public String getPermission()  { return "fpp.spawn"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        return delegate.execute(sender, args);
    }
}

