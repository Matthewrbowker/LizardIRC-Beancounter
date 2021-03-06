package org.lizardirc.beancounter.commands.entrymsg;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.types.GenericChannelEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;

import org.lizardirc.beancounter.hooks.CommandHandler;

class EntryMessageCommandHandler<T extends PircBotX> implements CommandHandler<T> {
    private static final String CMD_ENTRYMSG = "entrymsg";
    private static final Set<String> COMMANDS = ImmutableSet.of(CMD_ENTRYMSG);

    private static final String SWITCH_FORCE = "--force";
    private static final String SWITCH_CLEAR = "--clear";
    private static final Set<String> SWITCHES = ImmutableSet.of(SWITCH_FORCE, SWITCH_CLEAR);

    private static final String PERM_GLOBAL_ENTRYMSG = "globalEntryMsg";

    private final EntryMessageListener<T> parentListener;

    public EntryMessageCommandHandler(EntryMessageListener<T> parentListener) {
        this.parentListener = parentListener;
    }

    @Override
    public Set<String> getSubCommands(GenericMessageEvent<T> event, List<String> commands) {
        if (commands.isEmpty()) {
            return COMMANDS;
        } else {
            return SWITCHES;
        }
    }

    @Override
    public void handleCommand(GenericMessageEvent<T> event, List<String> commands, String remainder) {
        if (!commands.isEmpty() && CMD_ENTRYMSG.equals(commands.get(0))) {
            if (!(event instanceof GenericChannelEvent)) {
                event.respond("This command may only be run in a channel.");
                return;
            }

            remainder = remainder.trim();

            if (remainder.isEmpty() && commands.size() < 2) {
                EntryMessage message = parentListener.getEntryMessage(((GenericChannelEvent) event).getChannel().getName());
                if (message == null) {
                    event.respond("This channel has no entry message set.");
                } else {

                    event.respond("This channel's entry message is: " + parentListener.generateMessageString((GenericChannelEvent) event));
                }

                event.respond("Use command \"" + CMD_ENTRYMSG + " [message]\" to set or change this channel's entry message.");
                event.respond("Use command \"" + CMD_ENTRYMSG + ' ' + SWITCH_CLEAR + "\" to clear this channel's entry message.");
            } else {
                boolean forceSet = false;
                boolean clearSet = false;

                for (int i = 1; i < commands.size(); i++) {
                    switch (commands.get(i)) {
                        case SWITCH_FORCE:
                            forceSet = true;
                            break;
                        case SWITCH_CLEAR:
                            clearSet = true;
                            break;
                    }
                }

                // Verify that we have the necessary permissions to proceed
                if (forceSet) {
                    if (!parentListener.getAccessControl().hasPermission(event, PERM_GLOBAL_ENTRYMSG)) {
                        event.respond("No u!  You do not have permission to use the " + SWITCH_FORCE + " switch.");
                        return;
                    }
                } else {
                    if (!((GenericChannelEvent) event).getChannel().isOp(event.getUser())) {
                        event.respond("You must be a channel operator to change this channel's entry message.");

                        if (parentListener.getAccessControl().hasPermission(event, PERM_GLOBAL_ENTRYMSG)) {
                            event.respond("Alternatively, since you have the requisite global bot permissions, you can use the \"" +
                                SWITCH_FORCE + "\" switch to override this.");
                        }

                        return;
                    }
                }

                if (clearSet) {
                    parentListener.clearEntryMessage(((GenericChannelEvent) event).getChannel().getName());
                    event.respond("Entry message cleared for this channel");
                } else {
                    if (remainder.isEmpty()) {
                        event.respond("Error: Cannot set an empty entry message");
                    } else {
                        String setterHostmask = event.getUser().getNick() + '!' + event.getUser().getLogin() + '@' +
                            event.getUser().getHostmask();
                        EntryMessage entryMessage = new EntryMessage(remainder, setterHostmask, Instant.now());
                        parentListener.setEntryMessage(((GenericChannelEvent) event).getChannel().getName(), entryMessage);
                        event.respond("Done");
                    }
                }
            }
        }
    }
}
