/**
 * 
 */
package org.unicode.cldr.web;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author srl
 * 
 * A worker thread that performs various SurveyTool tasks, starting with booting.
 */
public class SurveyThread extends Thread {
	/**
	 * Are we still running?
	 */
	boolean surveyThreadIsRunning = true;
	
	/**
	 * @author srl
	 * A SurveyTask is a unit of work that can be done by the SurveyThread.
	 * 
	 * Usage:
     *   startupThread.addTask(new SurveyThread.SurveyTask("startup") {
	 *		public void run() throws Throwable {
	 *			doStartup();
	 *		}
     *   });
	 * 
	 */
	public static abstract class SurveyTask {
		/**
		 * Name of the task.
		 */
		public String name;
		
		private boolean taskRunning = true;
		
		/**
		 * Request this task to stop.
		 */
		public void stop() {
			System.err.println(this+" - stop requested.");
			taskRunning = false;
		}
		
		/**
		 * Kill this task by interrupting. (Doesn't actually kill the thread?)
		 */
		public void kill() {
			stop();
			theThread.interrupt();
		}
		
		protected SurveyThread theThread = null;
		
		/**
		 * Is this task still running? Check this periodically.
		 * @return
		 */
		public boolean running() {
			return taskRunning && theThread.surveyThreadIsRunning;
		}
		
		/**
		 * C'tor. The name is the initial name of the task.
		 * @param taskName
		 */
		SurveyTask(String taskName) {
			name = taskName;
		}
		/**
		 * Get some info about the task.
		 */
		public String toString() {
			return "Task: "+name;
		}
		/**
		 * Do the work.
		 * @throws Throwable  - any exception will be noted.
		 */
		abstract public void run() throws Throwable;
	}
	
	/**
	 * Debugging info on the main loop.
	 */
	private static boolean DEBUG = true;
	
	/**
	 * How many tasks are remaining? 0 if none.
	 * @return
	 */
	public int tasksRemaining() {
		return tasks.size();
	}
	
	/**
	 * The current state of the thread.
	 */
	public String toString() {
		return "{ST Threads: Tasks waiting:"+tasksRemaining()+", Current:"+current+", Running:"+surveyThreadIsRunning+"}";
	}
	
	/**
	 * The status, as HTML 
	 */
	public String htmlStatus() {
		if(tasksRemaining()==0&&current==null) return null;
		StringBuffer sb = new StringBuffer();
		if(current!=null) {
			sb.append(current);
		}
		if(tasksRemaining()>0) {
			if(sb.length()>0) {
				sb.append(" * ");
			}
			sb.append("(waiting tasks: "+tasksRemaining()+")");
		}
		return sb.toString();
	}

	/**
	 * The current task, or null if none.
	 */
	SurveyTask current = null;

	/**
	 * The main run loop.  Perform tasks or wait.
	 */
	public void run() {
		if(DEBUG) System.err.println("SurveyThread: Bootation.");
		setName();
		while(this.surveyThreadIsRunning) {
			try {
				if(DEBUG) System.err.println("SurveyThread: About to take from queue (count:"+tasksRemaining()+"):");
				current = tasks.take();
				setName();
				if(DEBUG) System.err.println("SurveyThread: Got: " + current);
				current.theThread=this; // set the back pointer
			} catch (InterruptedException e) {
				if(DEBUG) System.err.println("SurveyThread: Interrupted- running="+surveyThreadIsRunning);
			}
		    if(current!=null) try {
				if(DEBUG) System.err.println("SurveyThread(count:"+tasksRemaining()+"): About to run: "+current);
		    	current.run();
				if(DEBUG) System.err.println("SurveyThread(count:"+tasksRemaining()+"): Done running : "+current);
		    } catch(Throwable t) {
				if(DEBUG) System.err.println("SurveyThread(count:"+tasksRemaining()+"): Got exception on: "+current + " - "+t.toString());
		        t.printStackTrace();
//		        SurveyMain.busted("While working on task " + current + " - "+t.toString(), t);
		    }
			current = null; /* done. */
			setName();
		}
		if(DEBUG) System.err.println("SurveyThread(count:"+tasksRemaining()+"): exitting!");
	}
	
	/**
	 * Add a task to the thread list
	 * @param name a name to override the standard name
	 * @param t task to add
	 */
	public void addTask(String name, SurveyTask t) {
		t.name = name;
		this.addTask(t);
	}
	/**
	 * Add a task, use the default name.  Throws an internal error if for some reason it couldn't be added.
	 * @param t
	 */
	public void addTask(SurveyTask t) {
		if(!tasks.offer(t)) {
			String complaint = "SurveyThread: can't add task " + t.name;
			System.err.println(complaint);
			throw new InternalError(complaint);
		}
	}

    /**
     * Request the ST to stop at its next available opportunity.
     */
    public void requestStop() {
		surveyThreadIsRunning = false;  // shutdown the next time through
    	addTask(new SurveyTask("shutdown") {
    		public void run() throws Throwable {
    			System.err.println("Shutdown task: stop requested!");
    			// add other items here.
    		}
    	});
    }
    
    /**
     * 
     */
    public void interruptStop() {
    	surveyThreadIsRunning = false;
    	this.interrupt();
    }	
	
	/**
	 * Construct the thread. Needs a pointer to the SurveyTool..
	 * @param sm
	 */
	SurveyThread(SurveyMain sm) {
		this.sm = sm;
		current = null;
		setName();
	}
	
	private void setName() {
		this.setName(toString());
	}
	
	/**
	 * Main list of tasks.
	 */
	LinkedBlockingQueue<SurveyTask> tasks = new LinkedBlockingQueue<SurveyTask>();
	
	/**
	 * Back-pointer.
	 */
	SurveyMain sm;

	/**
	 * Try to shut down the threads cleanly.
	 */
	public void attemptCleanShutdown() {
		boolean clean = true;
	
    	System.err.println("SurveyThread: attempting shutdown...");
        try {
        	if(!this.isAlive()) return;
        	System.err.println("attempting requestStop()");
        	this.requestStop();
        	Thread.sleep(1000);
        	if(!this.isAlive()) return;
        	
        	SurveyTask aCurrent = current;
        	if(aCurrent != null) {
        		System.err.println("Attempting task stop on " + aCurrent+ "..");
        		aCurrent.stop();
        		Thread.sleep(1000);
        		if(!this.isAlive()) return;
        	}
        	aCurrent = current; // in case it changed
        	if(aCurrent != null) {
        		System.err.println("Attempting task kill on " + aCurrent +"..");
        		aCurrent.kill();
        		Thread.sleep(1000);
        		if(!this.isAlive()) return;
        	}
        	
        	System.err.println("Attempting interrupt stop");
        	this.interruptStop();
    		Thread.sleep(1000);
    		if(!this.isAlive()) return;
    		
    		clean=false;
    		System.err.println("Give up. Could not stop thread in time.");
        } catch(Throwable t) {
        	clean= false;
        	System.err.println("Trying to do a shutdown in SurveyThread: got " + t.toString());
        } finally {
        	if(clean == true) {
        		System.err.println("SurveyThread: clean shutdown.");
        	}
        }
	}
}
