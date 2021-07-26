package me.scmiller.guildarmory.Discord.Commands;

/**
 * CommandState enables the {@link WaiterSwitch} to know its position in
 * the sub-command array and reference information otherwise outside of it's
 * scope.  
 * 
 * TODO: Altering CommandState so that subcommands are managed via a modified 
 * linked list data structure may enable subcommands to be referenced by outside
 * commands without running into issues surrounding state position handling.<br></br>
 * 
 * {@link CommandState#CLOSE} 
 * 
 * @author &#064scottc_miller
 *
 */
public class CommandState 
{
	/** Value used to end command. Default is -1. */
	public final int CLOSE; 
	
	/** Value of last state.  State will never be > FINAL_STATE*/
	public final int FINAL_STATE;
	
	/** Value of initial state. Default is 0 */
	public final int FIRST_STATE;  
	
	/** Offset between {@link #FIRST_STATE} and 0 */
	private final int ARRAY_OFFSET;
	
	/* String array of the message received at each given state. 
	 * NOTE: If a prompt is given, the next state will contain the response */
	private String[] messages;
	
	//Current state of the command
	private int currentState;
	
	/**Flag command for bad input, will rerun the state where the prompt
	for the response was given. */
	private boolean tryingAgain;
	
	
	/**
	 * CommandState enables the {@link WaiterSwitch} to know its position in
	 * the sub-command array and reference information otherwise outside of it's
	 * scope. 
	 * @param FINAL_STATE Last possible sub-command state.
	 */
	public CommandState(int FINAL_STATE){
		this.FIRST_STATE = 0;
		this.FINAL_STATE = FINAL_STATE;
		this.CLOSE = -1;
		this.currentState = 0;
		
		this.ARRAY_OFFSET = 0;
		this.tryingAgain  = false;
		messages = new String[this.FINAL_STATE];
	}
	
	/**
	 * CommandState enables the {@link WaiterSwitch} to know its position in
	 * the sub-command array and reference information otherwise outside of it's
	 * scope.
	 * @param FINAL_STATE Last possible sub-command state.
	 * @param CLOSE Overrides default exit value of -1.
	 */
	public CommandState(int FINAL_STATE, int CLOSE){
		this.FIRST_STATE = 0;
		this.FINAL_STATE = FINAL_STATE;
		this.CLOSE = CLOSE;
		this.currentState = 0;
		
		this.ARRAY_OFFSET = 0;
		this.tryingAgain  = false;
		messages = new String[this.FINAL_STATE];
	}
	
	/**
	 * CommandState enables the {@link WaiterSwitch} to know its position in
	 * the sub-command array and reference information otherwise outside of it's
	 * scope.
	 * @param FIRST_STATE Overrides the default initial state value of 0.
	 * @param FINAL_STATE Last possible sub-command state.
	 * @param CLOSE Overrides default exit value of -1.
	 */
	public CommandState(int FIRST_STATE, int FINAL_STATE, int CLOSE){
		this.FIRST_STATE = FIRST_STATE;
		this.FINAL_STATE = FINAL_STATE;
		this.CLOSE = CLOSE;
		this.currentState = 0;
		
		this.ARRAY_OFFSET = 0 - FIRST_STATE;
		this.tryingAgain  = false;
		messages = new String[this.FINAL_STATE];
		
	}
	
	/**
	 * Jump to a non-adjacent state.
	 * 
	 * @param state
	 * @param msg
	 */
	public void jumpToState(int state, String msg) {
		//Only jump states if state is valid
		messages[state - this.ARRAY_OFFSET] = msg;
		currentState = state <= FINAL_STATE && state >= FIRST_STATE ? state : currentState;
	}
	
	/**
	 * Flag command when bad input given.  Sets {@link #tryingAgain} to true.
	 */
	public void tryAgain() {
		currentState = (currentState + 1 >= this.FIRST_STATE) ? currentState - 1 : currentState;
		tryingAgain = true;
	}
	
	/**
	 * Increases command state value by 1 and inserts message received this command
	 * @param msg
	 */
	public void next(String msg) {
		messages[currentState - this.ARRAY_OFFSET] = msg;
		currentState = (currentState + 1 <= this.FINAL_STATE) ? currentState + 1 : currentState;
		tryingAgain = false;
		
	}
	
	/**
	 * Decreases command state value by 1 and inserts message received this command
	 * @param msg
	 */
	public void back (String msg) {
		messages[currentState -this.ARRAY_OFFSET] = msg;
		currentState = (currentState + 1 >= this.FIRST_STATE) ? currentState - 1 : currentState;
		tryingAgain = false;
	}
	
	/**
	 * @return current state
	 */
	public int getState() {
		return currentState;
	}
	/**
	 * 
	 * @param state CommandState where message was received
	 * @return user input in raw string form
	 */
	public String getMessageAt(int state) {
		return state <= this.FINAL_STATE && state >= this.FIRST_STATE ? messages[state - this.ARRAY_OFFSET] : "";
	}
	
	/**
	 * Check if bad input was given, and command wishes to try again.
	 * @return {@link #tryingAgain}
	 */
	public boolean isTryingAgain() {
		return tryingAgain;
	}
	
	/**
	 * Closes command by setting the state to {@link #CLOSE}
	 */
	public void close() {
		currentState = this.CLOSE;
	}
}
