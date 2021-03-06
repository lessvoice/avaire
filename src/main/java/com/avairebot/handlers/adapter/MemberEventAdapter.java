package com.avairebot.handlers.adapter;

import com.avairebot.AvaIre;
import com.avairebot.contracts.handlers.EventAdapter;
import com.avairebot.database.controllers.GuildController;
import com.avairebot.database.transformers.ChannelTransformer;
import com.avairebot.database.transformers.GuildTransformer;
import com.avairebot.permissions.Permissions;
import com.avairebot.utilities.RoleUtil;
import com.avairebot.utilities.StringReplacementUtil;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemberEventAdapter extends EventAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MemberEventAdapter.class);

    /**
     * Instantiates the event adapter and sets the avaire class instance.
     *
     * @param avaire The AvaIre application class instance.
     */
    public MemberEventAdapter(AvaIre avaire) {
        super(avaire);
    }

    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        GuildTransformer transformer = GuildController.fetchGuild(avaire, event.getGuild());
        if (transformer == null) {
            LOGGER.warn("Failed to get a valid guild transformer during member join! User:{}, Guild:{}",
                event.getMember().getUser().getId(), event.getGuild().getId()
            );
            return;
        }

        for (ChannelTransformer channelTransformer : transformer.getChannels()) {
            if (channelTransformer.getWelcome().isEnabled()) {
                TextChannel textChannel = event.getGuild().getTextChannelById(channelTransformer.getId());
                if (textChannel == null) {
                    continue;
                }

                if (!event.getGuild().getSelfMember().hasPermission(textChannel, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE)) {
                    continue;
                }

                textChannel.sendMessage(StringReplacementUtil.parseGuildJoinLeaveMessage(
                    event.getGuild(), textChannel, event.getUser(),
                    channelTransformer.getWelcome().getMessage() == null ?
                        "Welcome %user% to **%server%!**" :
                        channelTransformer.getWelcome().getMessage())
                ).queue();
            }
        }

        if (event.getUser().isBot()) {
            return;
        }

        if (transformer.getAutorole() != null) {
            Role role = event.getGuild().getRoleById(transformer.getAutorole());
            if (canGiveRole(event, role)) {
                event.getGuild().getController().addSingleRoleToMember(
                    event.getMember(), role
                ).queue();
            }
        }
    }

    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        GuildTransformer transformer = GuildController.fetchGuild(avaire, event.getGuild());
        if (transformer == null) {
            LOGGER.warn("Failed to get a valid guild transformer during member leave! User:{}, Guild:{}",
                event.getMember().getUser().getId(), event.getGuild().getId()
            );
            return;
        }

        for (ChannelTransformer channelTransformer : transformer.getChannels()) {
            if (channelTransformer.getGoodbye().isEnabled()) {
                TextChannel textChannel = event.getGuild().getTextChannelById(channelTransformer.getId());
                if (textChannel == null) {
                    continue;
                }

                if (!event.getGuild().getSelfMember().hasPermission(textChannel, Permission.MESSAGE_READ, Permission.MESSAGE_WRITE)) {
                    continue;
                }

                textChannel.sendMessage(StringReplacementUtil.parseGuildJoinLeaveMessage(
                    event.getGuild(), textChannel, event.getUser(),
                    channelTransformer.getGoodbye().getMessage() == null ?
                        "%user% has left **%server%**! :(" :
                        channelTransformer.getGoodbye().getMessage())
                ).queue();
            }
        }
    }

    private boolean canGiveRole(GuildMemberJoinEvent event, Role role) {
        return role != null
            && event.getGuild().getSelfMember().hasPermission(Permissions.MANAGE_ROLES.getPermission())
            && RoleUtil.isRoleHierarchyLower(event.getGuild().getSelfMember().getRoles(), role);
    }
}
