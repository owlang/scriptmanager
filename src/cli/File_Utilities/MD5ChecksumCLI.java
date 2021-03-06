package cli.File_Utilities;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.Date;

import scripts.File_Utilities.MD5Checksum;

/**
	File_UtilitiesCLI/MD5ChecksumCLI
*/
@Command(name = "md5checksum", mixinStandardHelpOptions = true,
		description = "Calculate MD5 checksum for an input file")
public class MD5ChecksumCLI implements Callable<Integer> {
	
	@Parameters( index = "0", description = "The file we want to calculate the MD5checksum for. Alternatively use md5 <file> or md5checksum <file>")
	private File input;

	@Option(names = {"-o", "--output"}, description = "specify output filepath")
	private File output = new File("md5checksum.txt");
	
	@Override
	public Integer call() throws Exception {
		String md5hash = MD5Checksum.calculateMD5(input.getAbsolutePath());
		PrintStream OUT = new PrintStream( output );
		OUT.println("MD5 (" + input.getName() + ") = " + md5hash);
		return(0);
	}
}

