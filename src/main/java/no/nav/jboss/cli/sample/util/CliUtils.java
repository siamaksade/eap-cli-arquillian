package no.nav.jboss.cli.sample.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jboss.dmr.ModelNode;

public class CliUtils {
	private static final String K_RESULT = "result";
	private static final String FILE_PREFIX = "jboss-cli-test";
	private static final String FILE_SUFFIX = ".cli";
	
	private static final String V_SUCCESS = "success";
	private static final String K_OUTCOME = "outcome";
	
    private static File createFile(String[] cmd) {
    	File file;
		try {
			file = File.createTempFile(FILE_PREFIX, FILE_SUFFIX);
		} catch (IOException e) {
			throw new RuntimeException("Failed to create cli script: " + e.getLocalizedMessage());
		}
		
        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            for(String line : cmd) {
                writer.write(line);
                writer.write('\n');
            }
        } catch (IOException e) {
        	throw new RuntimeException("Failed to write to " + file.getAbsolutePath() + ": " + e.getLocalizedMessage());
        } finally {
            if(writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                }
            }
        }
        
        return file;
    }
    
    public static String execute(String cmd, boolean logFailure) {
    	return execute(new String[] {cmd}, logFailure);
    }
    
	public static String execute(String [] cmd, boolean logFailure) {
		return execute(createFile(cmd), logFailure);
	}
	
	public static String execute(File f, boolean logFailure) {
		final String jbossDist = System.getenv("JBOSS_HOME");
		if (jbossDist == null) {
			throw new RuntimeException("JBOSS_HOME env property is not set");
		}

		if (!f.exists()) {
			throw new RuntimeException("File " + f.getAbsolutePath() + " doesn't exist");
		}
		
		final ProcessBuilder builder = new ProcessBuilder();
		final List<String> command = new ArrayList<String>();
		command.add("java");
		command.add("-Djava.net.preferIPv4Stack=true");
		command.add("-Djava.net.preferIPv6Addresses=false");
		command.add("-jar");
		command.add(jbossDist + File.separatorChar + "jboss-modules.jar");
		command.add("-mp");
		command.add(jbossDist + File.separatorChar + "modules");
		command.add("org.jboss.as.cli");
		command.add("-c");
		command.add("--controller=" + "localhost" + ":" + "9999");
		command.add("--file=" + f.getAbsolutePath());
		builder.command(command);
		Process cliProc = null;
		try {
			cliProc = builder.start();
		} catch (IOException e) {
			throw new RuntimeException("Failed to start CLI process: " + e.getLocalizedMessage());
		}
		
        try {
            @SuppressWarnings("unused")
			int exitCode = cliProc.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted waiting for the CLI process.");
        }

		String output = null;
		try {
			int bytesTotal = cliProc.getInputStream().available();
			if (bytesTotal > 0) {
				final byte[] bytes = new byte[bytesTotal];
				cliProc.getInputStream().read(bytes);
				output = new String(bytes);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to read command's output: " + e.getLocalizedMessage());
		}

		return output;
	}

	public static boolean isSuccess(String output) {
		if (output == null) {
			return false;
		}
		
		ModelNode node = ModelNode.fromString(output);
		
		if (node != null && node.get(K_OUTCOME) != null) {
			return V_SUCCESS.equals(stripQuotes(node.get(K_OUTCOME).toString()));
		} 
		
		return false;
	}
	
	public static ModelNode getResultObject(String output) {
		return ModelNode.fromString(output).get(K_RESULT);
	}
	
	public static String stripQuotes(String str) {
		return str == null ? null : str.replaceAll("^\"(.*)\"$", "$1");
	}
}
