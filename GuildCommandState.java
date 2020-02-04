package me.scmiller.guildarmory.Discord.Commands;

import com.jagrosh.jdautilities.command.CommandEvent;

/**
 * Extends CommandState.  Adding a publicly accessible CommandEvent
 * object for references to user and guild id's, messages, channels, etc.
 * 
 * @author &#064scottc_miller
 *
 */
public class GuildCommandState extends CommandState{
	
	public final CommandEvent EVENT;
	
	public GuildCommandState(CommandEvent COMMAND_EVENT, int FINAL_STATE) {
		super(FINAL_STATE);
		this.EVENT = COMMAND_EVENT;
	}

	public GuildCommandState(CommandEvent COMMAND_EVENT, int FIRST_STATE, int FINAL_STATE, int CLOSE) {
		super(FIRST_STATE, FINAL_STATE, CLOSE);
		this.EVENT = COMMAND_EVENT;
	}

	public GuildCommandState(CommandEvent COMMAND_EVENT, int FINAL_STATE, int CLOSE) {
		super(FINAL_STATE, CLOSE);
		this.EVENT = COMMAND_EVENT;
	}
}
