ifneq ($(wildcard /opt/homebrew/opt/openjdk/bin/java),)
  export JAVA_HOME := /opt/homebrew/opt/openjdk
  export PATH := $(JAVA_HOME)/bin:$(PATH)
endif

MVN ?= mvn

.PHONY: build up down test smoke regression report clean

build:
	$(MVN) -B -DskipTests package

up: build
	docker compose up -d --build --wait

down:
	docker compose down -v

test: build
	$(MVN) -B -pl automation test

smoke: build
	$(MVN) -B -pl automation test -Dsurefire.groups=smoke

regression: test

report:
	@if command -v allure >/dev/null 2>&1; then allure serve automation/target/allure-results; else echo "Allure CLI not installed. Results: automation/target/allure-results"; fi

clean:
	$(MVN) -B clean
	docker compose down -v --remove-orphans >/dev/null 2>&1 || true
