
 /*
  * Command line arguments
  */
 inline int OPT_def   = 1;
 inline int OPT_zone  = 0;
 inline int OPT_proj  = 0;
 inline int OPT_clear = 1;
 inline int INTERVAL  = 1;
 inline int COUNTER   = 1;
 inline string CLEAR  = "";

#pragma D option quiet
#pragma D option switchrate=10hz


#include <sys/file.h>
#include <inet/common.h>
#include <sys/byteorder.h>


/*
 * Print header
 */
dtrace:::BEGIN
{
	/* starting values */
        counts = COUNTER;
        secs = INTERVAL;
	TCP_out = 0;
	TCP_in = 0;
}

/*
 * TCP Process inbound connections
 *
 * 0x00200000 has been hardcoded. It was SS_TCP_FAST_ACCEPT, but was
 * renamed to SS_DIRECT around build 31.
 */
fbt:sockfs:sotpi_accept:entry
/(arg1 & FREAD) && (arg1 & FWRITE) && (args[0]->so_state & 0x00200000)/
{
	self->sop = args[0];
}

fbt:sockfs:sotpi_create:return
/self->sop/
{
	self->nsop = (struct sonode *)arg1;
}

fbt:sockfs:sotpi_accept:return
/self->nsop/
{
	this->tcpp = (tcp_t *)self->nsop->so_priv;
	self->connp = (conn_t *)this->tcpp->tcp_connp;
	tname[(int)self->connp] = execname;
	tpid[(int)self->connp] = pid;
	tuid[(int)self->connp] = uid;
}

fbt:sockfs:sotpi_accept:return
{
	self->nsop = 0;
	self->sop = 0;
}

/*
 * TCP Process outbound connections
 */
fbt:ip:tcp_connect:entry
{
	this->tcpp = (tcp_t *)arg0;
	self->connp = (conn_t *)this->tcpp->tcp_connp;
	tname[(int)self->connp] = execname;
	tpid[(int)self->connp] = pid;
	tuid[(int)self->connp] = uid;
	OPT_proj ? tproj[(int)self->connp] = curpsinfo->pr_projid : 1;
}

/*
 * TCP Data translations
 */
fbt:sockfs:sotpi_accept:return,
fbt:ip:tcp_connect:return
/self->connp/
{
	/* fetch ports */
#if defined(_BIG_ENDIAN)
	self->lport = self->connp->u_port.tcpu_ports.tcpu_lport;
	self->fport = self->connp->u_port.tcpu_ports.tcpu_fport;
#else
	self->lport = BSWAP_16(self->connp->u_port.tcpu_ports.tcpu_lport);
	self->fport = BSWAP_16(self->connp->u_port.tcpu_ports.tcpu_fport);
#endif

	/* fetch IPv4 addresses */
	this->fad12 =
	    (int)self->connp->connua_v6addr.connua_faddr._S6_un._S6_u8[12];
	this->fad13 =
	    (int)self->connp->connua_v6addr.connua_faddr._S6_un._S6_u8[13];
	this->fad14 =
	    (int)self->connp->connua_v6addr.connua_faddr._S6_un._S6_u8[14];
	this->fad15 =
	    (int)self->connp->connua_v6addr.connua_faddr._S6_un._S6_u8[15];
	this->lad12 =
	    (int)self->connp->connua_v6addr.connua_laddr._S6_un._S6_u8[12];
	this->lad13 =
	    (int)self->connp->connua_v6addr.connua_laddr._S6_un._S6_u8[13];
	this->lad14 =
	    (int)self->connp->connua_v6addr.connua_laddr._S6_un._S6_u8[14];
	this->lad15 =
	    (int)self->connp->connua_v6addr.connua_laddr._S6_un._S6_u8[15];

	/* convert type for use with lltostr() */
	this->fad12 = this->fad12 < 0 ? 256 + this->fad12 : this->fad12;
	this->fad13 = this->fad13 < 0 ? 256 + this->fad13 : this->fad13;
	this->fad14 = this->fad14 < 0 ? 256 + this->fad14 : this->fad14;
	this->fad15 = this->fad15 < 0 ? 256 + this->fad15 : this->fad15;
	this->lad12 = this->lad12 < 0 ? 256 + this->lad12 : this->lad12;
	this->lad13 = this->lad13 < 0 ? 256 + this->lad13 : this->lad13;
	this->lad14 = this->lad14 < 0 ? 256 + this->lad14 : this->lad14;
	this->lad15 = this->lad15 < 0 ? 256 + this->lad15 : this->lad15;

	/* stringify addresses */
	self->faddr = strjoin(lltostr(this->fad12), ".");
	self->faddr = strjoin(self->faddr, strjoin(lltostr(this->fad13), "."));
	self->faddr = strjoin(self->faddr, strjoin(lltostr(this->fad14), "."));
	self->faddr = strjoin(self->faddr, lltostr(this->fad15 + 0));
	self->laddr = strjoin(lltostr(this->lad12), ".");
	self->laddr = strjoin(self->laddr, strjoin(lltostr(this->lad13), "."));
	self->laddr = strjoin(self->laddr, strjoin(lltostr(this->lad14), "."));
	self->laddr = strjoin(self->laddr, lltostr(this->lad15 + 0));

	/* fix direction and save values */
	tladdr[(int)self->connp] = self->laddr;
	tfaddr[(int)self->connp] = self->faddr;
	tlport[(int)self->connp] = self->lport;
	tfport[(int)self->connp] = self->fport;

	/* all systems go */
	tok[(int)self->connp] = 1;
}

/*
 * TCP Clear connp
 */
fbt:ip:tcp_get_conn:return
{
	/* Q_TO_CONN */
	this->connp = (conn_t *)arg1;
	tok[(int)this->connp] = 0;
	tpid[(int)this->connp] = 0;
	tuid[(int)this->connp] = 0;
	tname[(int)this->connp] = 0;
	tproj[(int)this->connp] = 0;
}

/*
 * TCP Process "port closed"
 */
fbt:ip:tcp_xmit_early_reset:entry
{
	this->queuep = (queue_t *)`tcp_g_q; /* ` */
	this->connp = (conn_t *)this->queuep->q_ptr;
	this->tcpp = (tcp_t *)this->connp->conn_tcp;
	self->zoneid = this->connp->conn_zoneid;

	/* split addresses */
	this->ipha = (ipha_t *)args[1]->b_rptr;
	this->fad15 = (this->ipha->ipha_src & 0xff000000) >> 24;
	this->fad14 = (this->ipha->ipha_src & 0x00ff0000) >> 16;
	this->fad13 = (this->ipha->ipha_src & 0x0000ff00) >> 8;
	this->fad12 = (this->ipha->ipha_src & 0x000000ff);
	this->lad15 = (this->ipha->ipha_dst & 0xff000000) >> 24;
	this->lad14 = (this->ipha->ipha_dst & 0x00ff0000) >> 16;
	this->lad13 = (this->ipha->ipha_dst & 0x0000ff00) >> 8;
	this->lad12 = (this->ipha->ipha_dst & 0x000000ff);

	/* stringify addresses */
	self->faddr = strjoin(lltostr(this->fad12), ".");
	self->faddr = strjoin(self->faddr, strjoin(lltostr(this->fad13), "."));
	self->faddr = strjoin(self->faddr, strjoin(lltostr(this->fad14), "."));
	self->faddr = strjoin(self->faddr, lltostr(this->fad15 + 0));
	self->laddr = strjoin(lltostr(this->lad12), ".");
	self->laddr = strjoin(self->laddr, strjoin(lltostr(this->lad13), "."));
	self->laddr = strjoin(self->laddr, strjoin(lltostr(this->lad14), "."));
	self->laddr = strjoin(self->laddr, lltostr(this->lad15 + 0));

	self->reset = 1;
}

/*
 * TCP Fetch "port closed" ports
 */
fbt:ip:tcp_xchg:entry
/self->reset/
{
#if defined(_BIG_ENDIAN)
	self->lport = (uint16_t)arg0;
	self->fport = (uint16_t)arg1;
#else
	self->lport = BSWAP_16((uint16_t)arg0);
	self->fport = BSWAP_16((uint16_t)arg1);
#endif
	self->lport = BE16_TO_U16(arg0);
	self->fport = BE16_TO_U16(arg1);
}

/*
 * TCP Print "port closed"
 */
fbt:ip:tcp_xmit_early_reset:return
{
	self->name = "<closed>";
	self->pid = 0;
	self->uid = 0;
	self->proj = 0;
	self->size = 54 * 2;	/* should check trailers */
	OPT_def ? @out[self->uid, self->pid, self->laddr, self->lport,
	    self->faddr, self->fport, self->name] = sum(self->size) : 1;
	OPT_zone ? @out[self->zoneid, self->pid, self->laddr, self->lport,
	    self->faddr, self->fport, self->name] = sum(self->size) : 1;
	OPT_proj ? @out[self->proj, self->pid, self->laddr, self->lport,
	    self->faddr, self->fport, self->name] = sum(self->size) : 1;
	self->reset = 0;
	self->size = 0;
	self->name = 0;
}

/*
 * TCP Process Write
 */
fbt:ip:tcp_send_data:entry
{
	self->conn_p = (conn_t *)args[0]->tcp_connp;
}

fbt:ip:tcp_send_data:entry
/tok[(int)self->conn_p]/
{
        self->size = msgdsize(args[2]) + 14;	/* should check trailers */
	self->uid = tuid[(int)self->conn_p];
	self->laddr = tladdr[(int)self->conn_p];
	self->faddr = tfaddr[(int)self->conn_p];
	self->lport = tlport[(int)self->conn_p];
	self->fport = tfport[(int)self->conn_p];
	OPT_proj ? self->proj = tproj[(int)self->conn_p] : 1;
	self->zoneid = self->conn_p->conn_zoneid;
        self->ok = 2;

	/* follow inetd -> in.* transitions */
	self->name = pid && (tname[(int)self->conn_p] == "inetd") ?
	    execname : tname[(int)self->conn_p];
	self->pid = pid && (tname[(int)self->conn_p] == "inetd") ?
	    pid : tpid[(int)self->conn_p];
	tname[(int)self->conn_p] = self->name;
	tpid[(int)self->conn_p] = self->pid;
}

/*
 * TCP Process Read
 */
fbt:ip:tcp_rput_data:entry
{
	self->conn_p = (conn_t *)arg0;
        self->size = msgdsize(args[1]) + 14;	/* should check trailers */
}

fbt:ip:tcp_rput_data:entry
/tok[(int)self->conn_p]/
{
	self->uid = tuid[(int)self->conn_p];
	self->laddr = tladdr[(int)self->conn_p];
	self->faddr = tfaddr[(int)self->conn_p];
	self->lport = tlport[(int)self->conn_p];
	self->fport = tfport[(int)self->conn_p];
	OPT_proj ? self->proj = tproj[(int)self->conn_p] : 1;
	self->zoneid = self->conn_p->conn_zoneid;
	self->ok = 2;

	/* follow inetd -> in.* transitions */
	self->name = pid && (tname[(int)self->conn_p] == "inetd") ?
	    execname : tname[(int)self->conn_p];
	self->pid = pid && (tname[(int)self->conn_p] == "inetd") ?
	    pid : tpid[(int)self->conn_p];
	tname[(int)self->conn_p] = self->name;
	tpid[(int)self->conn_p] = self->pid;
}

/*
 * TCP Complete printing outbound handshake
 */
fbt:ip:tcp_connect:return
/self->connp/
{
	self->name = tname[(int)self->connp];
	self->pid = tpid[(int)self->connp];
	self->uid = tuid[(int)self->connp];
	self->zoneid = self->connp->conn_zoneid;
	OPT_proj ? self->proj = tproj[(int)self->connp] : 1;
	self->size = 54;	/* should check trailers */

	/* this packet occured before connp was fully established */
	OPT_def ? @out[self->uid, self->pid, self->laddr, self->lport,
	    self->faddr, self->fport, self->name] = sum(self->size) : 1;
	OPT_zone ? @out[self->zoneid, self->pid, self->laddr, self->lport,
	    self->faddr, self->fport, self->name] = sum(self->size) : 1;
	OPT_proj ? @out[self->proj, self->pid, self->laddr, self->lport,
	    self->faddr, self->fport, self->name] = sum(self->size) : 1;
}

/*
 * TCP Complete printing inbound handshake
 */
fbt:sockfs:sotpi_accept:return
/self->connp/
{
	self->name = tname[(int)self->connp];
	self->pid = tpid[(int)self->connp];
	self->uid = tuid[(int)self->connp];
	self->zoneid = self->connp->conn_zoneid;
	OPT_proj ? self->proj = tproj[(int)self->connp] : 1;
	self->size = 54 * 3;	/* should check trailers */

	/* these packets occured before connp was fully established */
	OPT_def ? @out[self->uid, self->pid, self->laddr, self->lport,
	    self->faddr, self->fport, self->name] = sum(self->size) : 1;
	OPT_zone ? @out[self->zoneid, self->pid, self->laddr, self->lport,
	    self->faddr, self->fport, self->name] = sum(self->size) : 1;
	OPT_proj ? @out[self->proj, self->pid, self->laddr, self->lport,
	    self->faddr, self->fport, self->name] = sum(self->size) : 1;
}

/*
 * TCP Save data
 */
fbt:ip:tcp_send_data:entry,
fbt:ip:tcp_rput_data:entry
/self->ok == 2/ 
{
	/* save r+w data*/
	OPT_def ? @out[self->uid, self->pid, self->laddr, self->lport,
	    self->faddr, self->fport, self->name] = sum(self->size) : 1;
	OPT_zone ? @out[self->zoneid, self->pid, self->laddr, self->lport,
	    self->faddr, self->fport, self->name] = sum(self->size) : 1;
	OPT_proj ? @out[self->proj, self->pid, self->laddr, self->lport,
	    self->faddr, self->fport, self->name] = sum(self->size) : 1;
}

/* 
 * TCP Clear connect variables
 */
fbt:sockfs:sotpi_accept:return,
fbt:ip:tcp_connect:return
/self->connp/
{
	self->faddr = 0;
	self->laddr = 0;
	self->fport = 0;
	self->lport = 0;
	self->connp = 0;
	self->name = 0;
	self->pid = 0;
	self->uid = 0;
}

/* 
 * TCP Clear r/w variables
 */
fbt:ip:tcp_send_data:entry,
fbt:ip:tcp_rput_data:entry
{
	self->ok = 0;
	self->uid = 0;
	self->pid = 0;
	self->size = 0;
	self->name = 0;
	self->lport = 0;
	self->fport = 0;
	self->laddr = 0;
	self->faddr = 0;
	self->conn_p = 0;
	self->zoneid = 0;
	self->proj = 0;
}

/*
 * TCP Systemwide Stats
 */
mib:::tcpOutDataBytes       { TCP_out += args[0]; }
mib:::tcpRetransBytes       { TCP_out += args[0]; }
mib:::tcpInDataInorderBytes { TCP_in  += args[0]; }
mib:::tcpInDataDupBytes     { TCP_in  += args[0]; }
mib:::tcpInDataUnorderBytes { TCP_in  += args[0]; }

/*
 * Timer
 */
profile:::tick-1sec
{
        secs--;
}

/*
 * Print Report
 */
profile:::tick-1sec
/secs == 0/
{
	/* convert TCP counters to Kbytes */
	TCP_out /= 1024;
	TCP_in  /= 1024; 

	/* print status */
	OPT_clear ? printf("%s", CLEAR) : 1;
        printf("TCPin %d\nTCPout %d\n",
            TCP_in, TCP_out);

	/* clear data */
        trunc(@out);
	TCP_in = 0;
	TCP_out = 0;
        secs = INTERVAL;
        counts--;
}

/*
 * End of program
 */
profile:::tick-1sec
/counts == 0/
{
        exit(0);
}

/*
 * Cleanup for Ctrl-C
 */
dtrace:::END
{
        trunc(@out);
}

