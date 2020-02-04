package me.scmiller.guildarmory.Discord.Commands;

import java.util.concurrent.TimeUnit;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import me.scmiller.guildarmory.Discord.Commands.SubCommands.SubCommand;
import me.scmiller.guildarmory.Discord.Utilities.Utils;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;

/**
 * WaiterSwitch allows for EventWaiter to be recursively called while pass vital
 * information about previous commands so that step-by-step commands can be implemented
 * to substantially increase the user-friendliness of the Guild Armory Bot
 * 
 * @param cs GuildCommandState that is updated and recursively passed
 */
public class WaiterSwitch{
	public void execute(EventWaiter waiter, SubCommand[] commands, GuildCommandState cs) {
		if(cs.getState() != cs.CLOSE) {
			
			/**
			 * Event Waiter can now be called with command state position and previous event 
			 * info easily accessible with no need to re-parse responses.  Without this info
			 * and recursively calling the next EventWaiter, error handling becomes near
			 * impossible once beyond the second call of EventWaiter.
			 * 
			 * State Switcher:
			 * 		Every time waiterSwitch is called, it determines which sub-command to call
			 * 		via a call to the GuidCommandState's current state.  The Command State must 
			 * 		be updated by setting it equal to the method's return of GuildCommandState.
			 * 		Then, if the new state is still valid, it is passed into a new call of
			 * 		waiterSwitch.  This continues under the state equals CommandState.CLOSE or
			 * 		CommandState.close() is called.
			 */
			waiter.waitForEvent(PrivateMessageReceivedEvent.class, e -> cs.EVENT.getAuthor().equals(e.getAuthor()) 
					|| cs.isTryingAgain(), e -> 
			{
				GuildCommandState wcs = cs;
				
				Message pm = e.getMessage();
				String pmRaw = pm.getContentRaw();
	
				
				if(pmRaw.equalsIgnoreCase("done") || pmRaw.equalsIgnoreCase("cancel")) {
					cs.close();
				}else {
					int commandIndex = wcs.getState() - wcs.FIRST_STATE;
					if(wcs.getState() <= wcs.FINAL_STATE && wcs.getState() >= wcs.FIRST_STATE && commandIndex < commands.length) 
					{
						
						wcs = commands[commandIndex].execute(wcs, pmRaw);
						if(wcs.getState() != wcs.CLOSE) {
							this.execute(waiter, commands, wcs);
						}
								
					}else 
					{
						wcs.close();
					}
				}
			}, 90 , TimeUnit.SECONDS, () -> Utils.sendPrivateMessage(cs.EVENT.getAuthor(), 
					new MessageBuilder("\n**Oops, yah took to long.**\n*"
							+ "Enter Command in Guild Channel to try again!*")
						.build()));
		}
	}
}
