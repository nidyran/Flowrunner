# Flowrunner

[![CI](https://img.shields.io/github/actions/workflow/status/nidyran/Flowrunner/ci.yml?branch=main&label=CI)](https://github.com/nidyran/Flowrunner/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/nidyran/Flowrunner)](https://github.com/nidyran/Flowrunner/blob/main/LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fnidyran%2FFlowrunner%2Fmain%2Fpom.xml&query=%2F%2A%5Blocal-name%28%29%3D%27project%27%5D%2F%2A%5Blocal-name%28%29%3D%27properties%27%5D%2F%2A%5Blocal-name%28%29%3D%27spring-boot.version%27%5D&label=Spring%20Boot&color=6DB33F)](https://spring.io/projects/spring-boot)
[![REST Assured](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fnidyran%2FFlowrunner%2Fmain%2Fpom.xml&query=%2F%2A%5Blocal-name%28%29%3D%27project%27%5D%2F%2A%5Blocal-name%28%29%3D%27properties%27%5D%2F%2A%5Blocal-name%28%29%3D%27rest-assured.version%27%5D&label=REST%20Assured&color=%23DF4C4C)](https://rest-assured.io/)
[![GitHub stars](https://img.shields.io/github/stars/nidyran/Flowrunner)](https://github.com/nidyran/Flowrunner/stargazers)

Flowrunner lets developers and testers define, configure, and run API flows — from a single request to full campaigns, headlessly in CI or interactively during dev. Business-agnostic and target-app-agnostic, built with Java, Spring Boot, and REST Assured.

## Concepts

### Flow dimensions

A **dimension** describes an axis a flow can be run against. Dimensions are optional and entirely up to you — define whatever axes make sense for your use case, or none at all. Each dimension has:

- `key` — the identifier used to reference the dimension
- `name` — a human-readable label
- `defaultValue` — optional value used when none is supplied at run time
- `required` — whether a value for this dimension must be supplied to run the flow (defaults to `false`)

Example dimensions:

- **Application** — which app the flow targets, e.g. `Customer`, `Backoffice`, `Customer Service App`
- **Environment** — which environment to run against, e.g. `Local stack`, `Dev`, `UAT`
- **Channel** — which channel the flow simulates, e.g. `Web`, `Mob`, `API`

Dimensions are declared under `flowrunner.flow.dimensions` in configuration:

```yaml
flowrunner:
  flow:
    dimensions:
      - key: application
        name: Application
        required: true
      - key: environment
        name: Environment
        defaultValue: Dev
        required: true
      - key: channel
        name: Channel
        defaultValue: Web
        required: false
```

## Star history

<a href="https://www.star-history.com/?repos=nidyran%2FFlowrunner&type=date&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/image?repos=nidyran/Flowrunner&type=date&theme=dark&legend=top-left" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/image?repos=nidyran/Flowrunner&type=date&legend=top-left" />
   <img alt="Star History Chart" src="https://api.star-history.com/image?repos=nidyran/Flowrunner&type=date&legend=top-left" />
 </picture>
</a>
