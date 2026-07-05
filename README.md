# Flowrunner

[![CI](https://img.shields.io/github/actions/workflow/status/nidyran/Flowrunner/ci.yml?branch=main&label=CI)](https://github.com/nidyran/Flowrunner/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/nidyran/Flowrunner)](https://github.com/nidyran/Flowrunner/blob/main/LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/nidyran/Flowrunner)](https://github.com/nidyran/Flowrunner/stargazers)

Flowrunner lets developers and testers define, configure, and run API flows — from a single request to full campaigns, headlessly in CI or interactively during dev. Business-agnostic and target-app-agnostic, built with Java, Spring Boot, and REST Assured.

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

### Configuration validation

At startup, `flowrunner.flow.configuration` is validated against the dimension tree: for every dimension marked `required`, a value must be present at the matching path in the configuration. If any required dimension is missing a value, startup fails with a `FlowConfigurationValidationException` listing every missing dimension.

### Pre/post load configuration visitors

Two extension points let you hook into this validation lifecycle by registering beans that implement:

- `PreLoadConfigurationVisitor` — runs before validation. Use it to fill gaps in the configuration (e.g. resolve missing values from another source) so that validation doesn't fail on dimensions you can supply another way.
- `PostLoadConfigurationVisitor` — runs after validation succeeds. Use it to customize or extend the configuration further once it's known to be valid.

Any number of beans of each type can be registered; they run in bean order.
