# Flowrunner

[![CI](https://img.shields.io/github/actions/workflow/status/nidyran/Flowrunner/ci.yml?branch=main&label=CI)](https://github.com/nidyran/Flowrunner/actions/workflows/ci.yml)
[![CodeQL](https://img.shields.io/github/actions/workflow/status/nidyran/Flowrunner/codeql.yml?branch=main&label=CodeQL)](https://github.com/nidyran/Flowrunner/security/code-scanning)
[![Coverage](.github/badges/jacoco.svg)](https://github.com/nidyran/Flowrunner/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/nidyran/Flowrunner)](https://github.com/nidyran/Flowrunner/blob/main/LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/nidyran/Flowrunner)](https://github.com/nidyran/Flowrunner/stargazers)

Flowrunner lets developers and testers define, configure, and run API flows — from a single request to full campaigns, headlessly in CI or interactively during dev. Business-agnostic and target-app-agnostic, built with Java, Spring Boot, and REST Assured.

## Why this project exists

Flowrunner is a ground-up rebuild of a tool I originally wrote for a specific project — one whose dimensions were hardcoded to that project's world. This version generalizes the idea: dimensions are entirely yours to define.

I built the original as a way to speed up my own development process. Testing APIs across multiple applications — each with several layers of security, complicated authentication flows, and second factors — had become a nightmare. Before writing my own tool, I invested a lot of time in existing solutions like Postman, Karate, and many others; none of them fit the way I work as a developer. So I built one that does.

What started as an idea is now a fully working solution with many features. I'm sharing it with the community as a small return for all the tools I've been using for free since I started working in this industry.

## Built with

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.0-6DB33F)](https://spring.io/projects/spring-boot)
[![REST Assured](https://img.shields.io/badge/REST_Assured-6.0.0-DF4C4C)](https://rest-assured.io/)
[![Jackson](https://img.shields.io/badge/Jackson-3.1.4-brightgreen)](https://github.com/FasterXML/jackson)

## Concepts

### Flow dimensions

A **dimension** describes an axis a flow can be run against. Dimensions are optional and entirely up to you — define whatever axes make sense for your use case, or none at all. Each dimension has:

- `key` — the identifier used to reference the dimension
- `name` — a human-readable label
- `defaultValue` — optional value used when none is supplied at run time
- `required` — whether a value for this dimension must be supplied to run the flow (defaults to `false`)
- `children` — optional list of nested sub-dimensions

Dimensions can nest: a dimension's `children` are themselves dimensions, so hierarchies like environment → application → channel can be expressed as a single tree instead of a flat list, with the tree structure itself expressing the parent/child relationship.

Example values for a typical hierarchy:

- **Environment** — e.g. `local`, `dev`, `uat`
- **Application** — e.g. `Customer`, `Backoffice`, `Customer Service App`
- **Channel** — e.g. `Web`, `Mob`, `API`

Dimensions are declared under `flowrunner.flow.dimensions` in configuration:

```yaml
flowrunner:
  flow:
    dimensions:
      - key: environment
        name: Environment
        defaultValue: local
        required: true
        children:
          - key: application
            name: Application
            required: true
            children:
              - key: channel
                name: Channel
                defaultValue: Web
                required: false
```

### Flow dimension instances

The actual configured values live under `flowrunner.flow.configuration` as a tree of **dimension instances**, bound directly to `FlowDimensionInstance` objects. Each instance has:

- `dimension` — the key of the dimension this instance belongs to (matches a `FlowDimension.key`)
- `key` — the key of the instance itself (e.g. `dev`, `customer`, `WEB`)
- `name` — an optional human-readable label
- `metadata` — an optional map of arbitrary attributes (e.g. `host`, `port`)
- `children` — instances of child dimensions under this instance

A dimension can have any number of instances at any level — several environments, several applications per environment, several channels per application:

```yaml
flowrunner:
  flow:
    configuration:
      - dimension: environment
        key: dev
        metadata:
          host: localhost
          port: 8080
        children:
          - dimension: application
            key: customer
            name: Customer
            children:
              - dimension: channel
                key: WEB
                name: Web
      - dimension: environment
        key: prod
        metadata:
          host: prod.flowrunner.dev
          port: 443
        children:
          - dimension: application
            key: customer
            name: Customer
```

### Configuration validation

At startup, the configuration is validated against the dimension tree: every branch must contain at least one instance of each dimension marked `required`. If any branch is missing a required dimension, startup fails with a `FlowConfigurationValidationException` listing every offending path, qualified by instance key — e.g. a `prod` environment without any `application` instances is reported as `Missing required dimension 'environment[prod].application'`. An absent optional dimension doesn't exempt its subtree: required dimensions nested below it are still reported.

Startup validation can be turned off with `flowrunner.flow.validate-on-startup: false`; `FlowConfigurationValidator.validate()` can then be invoked on demand.

Given the dimension tree above (environment and application required, channel optional), this configuration fails validation:

```yaml
flowrunner:
  flow:
    configuration:
      - dimension: environment
        key: dev
        children:
          - dimension: application
            key: customer
            name: Customer
      - dimension: environment
        key: prod
        # no application instances, but application is required
```

The `dev` branch is fine; only the `prod` branch is reported.

### Pre/post load configuration visitors

Two extension points let you hook into this validation lifecycle by registering beans that implement:

- `PreLoadConfigurationVisitor` — runs before validation. Use it to fill gaps in the configuration (e.g. resolve missing values from another source) so that validation doesn't fail on dimensions you can supply another way.
- `PostLoadConfigurationVisitor` — runs after validation succeeds. Use it to customize or extend the configuration further once it's known to be valid.

Both receive the list of root `FlowDimensionInstance` objects; instances are mutable, so visitors can adjust the tree in place. Any number of beans of each type can be registered; they run in bean order.

### Supported dimension patterns

A flow handler declares which dimension instances it can run against via `supportedDimensionsPattern()` — a regular expression matched against dot-separated instance-key paths through the configured dimension tree (e.g. `dev.customer.WEB`), one segment per dimension level. Instead of hand-writing the regex, build it fluently with `DimensionPattern`:

```java
// any environment -> customer application -> anything below
DimensionPattern.any().with("customer").build();

// dev or uat environment -> any application -> WEB channel, exactly that depth
DimensionPattern.anyOf("dev", "uat").any().with("WEB").exact().build();
```

By default a pattern also matches any deeper path below its last segment; end the chain with `exact()` to match that depth only. Keys are quoted, so regex metacharacters in instance keys are matched literally.

## Properties

| Property | Type | Default | Description |
|---|---|---|---|
| `flowrunner.flow.dimensions` | list of `FlowDimension` | — | The dimension tree describing the axes flows can be run against. |
| `flowrunner.flow.dimensions[].key` | string | — | Identifier used to reference the dimension. |
| `flowrunner.flow.dimensions[].name` | string | — | Human-readable label. |
| `flowrunner.flow.dimensions[].defaultValue` | string | — | Value used when none is supplied at run time. |
| `flowrunner.flow.dimensions[].required` | boolean | `false` | Whether an instance of this dimension must be configured in every branch. |
| `flowrunner.flow.dimensions[].children` | list of `FlowDimension` | — | Nested sub-dimensions. |
| `flowrunner.flow.configuration` | list of `FlowDimensionInstance` | — | The configured dimension instances, validated against the dimension tree at startup. |
| `flowrunner.flow.configuration[].dimension` | string | — | Key of the dimension this instance belongs to. |
| `flowrunner.flow.configuration[].key` | string | — | Key of the instance itself (e.g. `dev`, `customer`). |
| `flowrunner.flow.configuration[].name` | string | — | Human-readable label. |
| `flowrunner.flow.configuration[].metadata` | map | empty | Arbitrary attributes of the instance (e.g. `host`, `port`). |
| `flowrunner.flow.configuration[].children` | list of `FlowDimensionInstance` | empty | Instances of child dimensions under this instance. |
| `flowrunner.flow.validate-on-startup` | boolean | `true` | Whether the configuration is validated (and the pre/post load visitors run) at application startup. |

## Known issues

- **`@PropertySource` / `@TestPropertySource` don't support YAML files** — test classes can't load their YAML fixtures through `@TestPropertySource(locations = ...)`; Spring Boot declined shipping a built-in `YamlPropertySourceFactory` because it couldn't honor `spring.config.activate.on-profile`, `spring.config.import` or multi-document YAML. Workaround: tests select their fixture with `@SpringBootTest(properties = "spring.config.location=classpath:/<fixture>.yaml")`, which routes through Spring Boot's full YAML processing. See [spring-boot#33434](https://github.com/spring-projects/spring-boot/issues/33434) and [spring-boot#42603 (declined)](https://github.com/spring-projects/spring-boot/pull/42603).
