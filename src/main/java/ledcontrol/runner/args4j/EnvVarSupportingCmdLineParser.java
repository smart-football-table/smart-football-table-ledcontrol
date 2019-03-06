package ledcontrol.runner.args4j;

import static java.util.Collections.addAll;

import java.util.ArrayList;
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
		List<String> allArgs = new ArrayList<>(envVarArgs());
		addAll(allArgs, args);
		super.parseArgument(allArgs.toArray(new String[allArgs.size()]));
	}

	private List<String> envVarArgs() {
		List<String> args = new ArrayList<>();
		getOptions().forEach(h -> {
			String envVar = System.getenv(h.option.metaVar());
			if (envVar != null) {
				args.add(h.option.toString());
				args.add(envVar);
			}
		});
		return args;
	}

}