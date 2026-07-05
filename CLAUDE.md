# CLAUDE.md

Standing rules for AI-assisted work in this repository.

## Project overview

Flowrunner is a business-agnostic, declarative flow/process execution engine (Java 21, Spring Boot, REST Assured). The current core is the dimension model and its validation:

- `FlowDimension` (`properties`) — schema node describing an axis flows run against (`key`, `name`, `defaultValue`, `required`, `children`); forms a tree such as environment → application → channel.
- `FlowDimensionInstance` (`properties`) — a configured value of a dimension (`dimension` = the dimension's key, `key` = the instance's own key, `name`, `metadata` map, `children`); mutable Lombok bean so visitors can adjust the tree.
- `FlowProperties` (`properties`) — `@ConfigurationProperties(prefix = "flowrunner.flow")` record binding `dimensions` (schema) and `configuration` (instances) directly from YAML.
- `FlowConfigurationValidator` (`validation`) — walks schema against instances; collects all errors, then throws `FlowConfigurationValidationException`.
- `FlowConfigurationLoader` (`validation`) — `ApplicationRunner` gated by `flowrunner.flow.validate-on-startup` (default `true`); runs `PreLoadConfigurationVisitor` beans, validation, then `PostLoadConfigurationVisitor` beans.

## Workflow

- Pull `main` before starting any work.
- Commit and push every completed change to `origin/main` without waiting to be asked; pull/rebase first if the remote has diverged.
- Verify with `mvn -B clean test` before committing. A task isn't done until the full suite passes.
- Commit messages: imperative summary line, body explaining why when it isn't obvious.
- When the config format, properties, or validation behavior changes, update the README in the same commit (Concepts sections and the Properties table).

## Build

- `mvn -B clean test` — full test suite.
- `mvn -B clean verify` — tests plus packaging, JaCoCo report and OpenRewrite.
- OpenRewrite (`rewrite-maven-plugin`) runs on every build and may auto-format sources and add license headers; expect it to touch files and include its changes in the commit for the task. It has semantically mangled YAML resources before — review `git diff` after building and never commit unreviewed rewrite changes, especially to resources.
- CI generates Javadoc for GitHub Pages via `mvn -B javadoc:javadoc` (see `.github/workflows/publish-javadoc.yml`); the plugin is deliberately not bound to the default lifecycle (binding it forces a nested lifecycle fork that double-runs resource/compile phases) and its `outputDirectory` must stay `target/site`, where the Pages workflow uploads from.
- The build targets Java 21. Running tests on a newer JDK (e.g. an IDE pointing at JDK 25) breaks the JaCoCo agent with "Unsupported class file major version"; fix the IDE/JDK selection, not the pom.
- Dependency and plugin versions come from the Spring Boot parent BOM wherever possible; don't add new dependencies without asking.

## Code practices

Standards followed across the codebase — new code must match them:

- Dependency injection is constructor-based only: `private final` attributes with `@RequiredArgsConstructor` on top of every Spring component class. No field or setter injection, no `@Autowired`.
- Optional or multiple collaborators (e.g. the pre/post load visitors) are injected as `ObjectProvider<T>` and consumed with `orderedStream()`, so zero-to-many beans work without `required = false` or `List` injection.
- Immutable types are records; mutable model classes use Lombok `@Getter`/`@Setter` (no hand-written accessors or builders).
- Nullness is annotated with jspecify (`@NonNull`) where the contract matters.
- String composition uses `"...".formatted(...)`; shared constants (e.g. `Strings.EMPTY`) over literals.
- Validation-style operations collect all errors into a list and fail once at the end, rather than failing fast on the first.

## Code style

- No defensive dual-shape handling: when code would need a conditional only to tolerate multiple equivalent input formats, normalize the input (fix the YAML/config/fixture) to one canonical form and keep the code single-path.
- `@ConfigurationProperties` bind structurally to typed Java objects only — no `Map<String, Object>` intermediates, no `@ConstructorBinding` conversion constructors, no custom converters. If the config shape can't bind directly, change the YAML format to mirror the object model.
- Mutable config model classes use Lombok `@Getter`/`@Setter` with field initializers for nested collections; immutable ones are records.
- Every source file carries the MIT license header (OpenRewrite enforces it).

## Validation conventions

- Error messages read `Missing required dimension 'environment[dev].application'` — dimension paths qualified by instance key, one error per offending branch.
- The validator collects every error before throwing; it never fails fast on the first.
- A new validation rule ships with a dedicated YAML fixture and a failure test asserting the exact message.

## Testing

- Every task ships with a set of tests covering the change — happy path, failure cases and edge cases — aiming for high test coverage.
- Test coverage must never decrease: every task keeps the JaCoCo coverage at least at its previous level (check `target/site/jacoco` after `mvn -B clean verify`). Current baseline: 100% instruction coverage.
- Every test class is a `@SpringBootTest` booting the real application context — no plain unit tests with hand-rolled Spring types, no `ApplicationContextRunner`.
- Constructor injection in tests via `@RequiredArgsConstructor` + `@TestConstructor(autowireMode = AutowireMode.ALL)`.
- YAML fixtures live in `src/test/resources`, one per test class, named `flow-test-<scenario>.yaml` with invalid variants suffixed `-invalid`; selected per class with `@SpringBootTest(properties = "spring.config.location=classpath:/<fixture>.yaml")`.
- Tests that assert validation failures disable startup validation with `flowrunner.flow.validate-on-startup=false` and call `FlowConfigurationValidator.validate()` directly.
- Each test class carries a class-level Javadoc summarizing the cases it covers.
