package ledcontrol.runner.args4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.ParserProperties;

public class EnvVarSupportingCmdLineParser extends CmdLineParser {

	public EnvVarSupportingCmdLineParser(Object bean) {
		super(bean);
	}

	public EnvVarSupportingCmdLineParser(Object bean, ParserProperties parserProperties) {
		super(bean, parserProperties);
	}

	@Override
	public void parseArgument(String... args) throws CmdLineException {
		List<String> allArgs = new ArrayList<>();
		// Allow environment variables to override
		getOptions().forEach(h -> {
			String envVar = System.getenv(h.option.metaVar());
			if (envVar != null) {
				allArgs.add(h.option.toString());
				allArgs.add(envVar);
			}
		});

		// Add regular command line args
		allArgs.addAll(Arrays.asList(args));
		super.parseArgument(allArgs.toArray(new String[allArgs.size()]));
	}

}