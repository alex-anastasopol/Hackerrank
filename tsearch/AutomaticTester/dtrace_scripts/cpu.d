
#pragma D option quiet

 /*
  * Command line arguments
  */
 inline int OPT_all    = 0;
 inline int OPT_time   = 0;
 inline int OPT_totals = 1;
 inline int OPT_top    = 0;
 inline int TOP        = 0;
 inline int INTERVAL   = 1;

 /* Initialise variables */
 dtrace:::BEGIN
 {
	cpustart[cpu] = 0;
	secs = INTERVAL;
 }

 /* Flag this thread as idle */
 sysinfo:unix:idle_enter:idlethread
 {
	idle[cpu] = 1;
 }

 /* Save kernel time between running threads */
 sched:::on-cpu 
 /cpustart[cpu]/
 {
	this->elapsed = timestamp - cpustart[cpu];
	@Procs["KERNEL"] = sum(this->elapsed);
 }

 /* Save the elapsed time of a thread */
 sched:::off-cpu,
 sched:::remain-cpu,
 profile:::profile-1sec
 /cpustart[cpu]/
 {
	/* determine the name for this thread */
	program[cpu] = pid == 0 ? idle[cpu] ? "IDLE" : "KERNEL" :
	    OPT_all ? execname : "PROCESS";

	/* save elapsed */
	this->elapsed = timestamp - cpustart[cpu];
	@Procs[program[cpu]] = sum(this->elapsed);
	cpustart[cpu] = timestamp;
 }

 /* Record the start time of a thread */
 sched:::on-cpu,
 sched:::remain-cpu
 {
	idle[cpu] = 0;
	cpustart[cpu] = timestamp;
 }


 profile:::tick-1sec
 {
	secs--;
 }



 /* Print report */
 profile:::tick-1sec 
 /secs == 0/ 
 { 
	OPT_top ? trunc(@Procs, TOP) : 1;
	printa("%s %@d\n", @Procs);
	secs = INTERVAL;
	exit(0);
 }




