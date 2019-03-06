package ledcontrol.runner.args4j;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Stream.concat;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.ParserProperties;
import org.kohsuke.args4j.spi.OptionHandler;

public class EnvVarSupportingCmdLineParser extends CmdLineParser {

	public EnvVarSupportingCmdLineParser(Object bean) {
		super(bean);
	}

	public EnvVarSupportingCmdLineParser(Object bean, ParserProperties parserProperties) {
		super(bean, parserProperties);
	}

	@Override
	public void parseArgument(String... args) throws CmdLineException {
		super.parseArgument(concat(envVarArgs(), stream(args)).toArray(String[]::new));
	}

	private Stream<String> envVarArgs() {
		@SuppressWarnings("rawtypes")
		Function<OptionHandler, List<String>> mapper = this::readEnvVar;
		return getOptions().stream().map(mapper).flatMap(Collection::stream);
	}

	private List<String> readEnvVar(@SuppressWarnings("rawtypes") OptionHandler h) {
		String envVar = System.getenv(h.option.metaVar());
		return envVar == null ? emptyList() : Arrays.asList(h.option.toString(), envVar);
	}

}