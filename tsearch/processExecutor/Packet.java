package ro.cst.tsearch.processExecutor;

import java.io.Serializable;

public class Packet implements Serializable {

	private static final long serialVersionUID = 6934787698823956818L;

	public static final int EXEC_REQUEST = 0x0001;
	public static final int SUCCESS_RESPONSE = 0x0002;
	public static final int ERROR_RESPONSE = 0x0004;
	public static final int INVALID_VALUE = 0x0008;

	public static final int CAPTURE_OUTPUT_MASK = 0x1000;
	public static final int CAPTURE_ERROR_MASK = 0x2000;

	private int packetType;
	private String[] cmdToExecute;
	private int returnValue;

	private String output = "";
	private String error = "";
	private String workingDirectory = "";

	public Packet(int packetType, String[] cmd, int retVal) {
		this.packetType = packetType;
		cmdToExecute = cmd;
		returnValue = retVal;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	public String getOutput() {
		return output;
	}

	public void setError(String error) {
		this.error = error;
	}

	public String getError() {
		return error;
	}

	public int getPacketType() {
		return packetType;
	}

	public int getReturnValue() {
		return returnValue;
	}

	public String[] getCommand() {
		return cmdToExecute;
	}

	public void setMask(int mask) {
		packetType = packetType | mask;
	}

	public boolean testMask(int mask) {
		return (this.packetType & mask) > 0;
	}

	public static boolean testMask(int result, int mask) {
		return (result & mask) > 0;
	}

	public void setCmdToExecute(String[] cmdToExecute) {
		this.cmdToExecute = cmdToExecute;
	}

	public void setPacketType(int packetType) {
		this.packetType = packetType;
	}

	public void setReturnValue(int returnValue) {
		this.returnValue = returnValue;
	}

	public String getWorkingDirectory() {
		return workingDirectory;
	}

	public void setWorkingDirectory(String workingDirectory) {
		this.workingDirectory = workingDirectory;
	}

}