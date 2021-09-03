package ledcontrol.runner.args4j;

import static ledcontrol.runner.args4j.EnvVars.envVarsAndArgs;
import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.github.stefanbirkner.systemlambda.SystemLambda.WithEnvironmentVariables;

import ledcontrol.runner.args4j.EnvVars.EnvVar;

class EnvVarsTest {

	private static final String OPTION_NAME = "aaa";
	private static final String ENVVAR_NAME_LC = "bbb";

	public static interface SomeOption {
		String someOption();
	}

	public static class ParameterClass implements SomeOption {
		@Option(name = "-" + OPTION_NAME)
		public String someOption;

		@Override
		public String someOption() {
			return someOption;
		}
	}

	public static class ParameterClassMultiDashes implements SomeOption {
		@Option(name = "-----" + OPTION_NAME)
		public String someOption;

		@Override
		public String someOption() {
			return someOption;
		}
	}

	public static class ParameterClassWithEnvVar implements SomeOption {
		@EnvVar(ENVVAR_NAME_LC)
		@Option(name = "-" + OPTION_NAME)
		public String someOption;

		@Override
		public String someOption() {
			return someOption;
		}
	}

	@TestFactory
	Stream<DynamicTest> doesReadEnvVarFooIfAnnotatedUsingEnvVarAnno() throws Exception {
		return forAll(bean -> {
			parse(bean, withEnvironmentVariable(OPTION_NAME.toUpperCase(), "---ENV---"));
			assertThat(bean.someOption(), is("---ENV---"));
		});
	}

	@Test
	Stream<DynamicTest> cmdLineOverridesEnvVars() throws Exception {
		return forAll(bean -> {
			parse(bean, withEnvironmentVariable(OPTION_NAME.toUpperCase(), "---ENV---"), "-" + OPTION_NAME,
					"---CMDLINE---");
			assertThat(bean.someOption(), is("---CMDLINE---"));
		});
	}

	@Test
	Stream<DynamicTest> doesNotConsiderLowerCaseEnvVar() throws Exception {
		return forAll(bean -> {
			parse(bean, withEnvironmentVariable(OPTION_NAME.toLowerCase(), "---ENV---"));
			assertThat(bean.someOption(), is(nullValue()));
		});
	}

	@Test
	void withExplicitEnvVarAnnoIfIfEnvNameIsLowerCase() throws Exception {
		assert ENVVAR_NAME_LC.equals(ENVVAR_NAME_LC.toLowerCase());
		ParameterClassWithEnvVar bean = new ParameterClassWithEnvVar();
		parse(bean, withEnvironmentVariable(ENVVAR_NAME_LC.toUpperCase(), "---UC-ENV---") //
				.and(ENVVAR_NAME_LC, "---LC-ENV---"));
		assertThat(bean.someOption(), is("---LC-ENV---"));
	}

	private static Stream<DynamicTest> forAll(Consumer<SomeOption> consumer) {
		return Stream.of(new ParameterClass(), new ParameterClassMultiDashes())
				.map(b -> dynamicTest(b.getClass().getSimpleName(), () -> consumer.accept(b)));
	}

	private static void parse(Object bean, WithEnvironmentVariables withEnvironmentVariable, String... args) {
		try {
			withEnvironmentVariable.execute(() -> parseArgs(bean, args));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void parseArgs(Object bean, String... args) throws CmdLineException {
		CmdLineParser parser = new CmdLineParser(bean);
		parser.parseArgument(envVarsAndArgs(parser, args));
	}

}
