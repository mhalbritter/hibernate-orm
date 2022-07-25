#!/usr/bin/env bash

set -euo pipefail

BLUE='\033[0;34m'
NC='\033[0m'

IGNORE_PACKAGES=(common)

# Restore last run state
LAST_SUCCESSFUL_PACKAGE=""
if [ -f last-successful-agent-run ]; then
  LAST_SUCCESSFUL_PACKAGE=$(cat last-successful-agent-run)
  printf "$BLUE=== Resuming from $LAST_SUCCESSFUL_PACKAGE ===$NC\n"
  echo "If you want start from scratch, delete 'last-successful-agent-run' file"
fi

for PACKAGE_PATH in hibernate-core/src/test/java/org/hibernate/orm/test/*; do
  PACKAGE=$(basename "$PACKAGE_PATH")

  if [ "$PACKAGE" == "$LAST_SUCCESSFUL_PACKAGE" ] || [ "$PACKAGE" \< "$LAST_SUCCESSFUL_PACKAGE" ]; then
    # Tests for this package have been executed already, skip it
    printf "$BLUE=== Skipping tests for package $PACKAGE, tests have been executed already ===$NC\n"
    continue
  fi

  if [[ " ${IGNORE_PACKAGES[*]} " =~ " $PACKAGE " ]]; then
    # Package is on the ignore list
    printf "$BLUE=== Skipping tests for package $PACKAGE, package is on ignore list ===$NC\n"
    continue;
  fi

  printf "$BLUE=== Running tests for package $PACKAGE ===$NC\n"
  ./gradlew hibernate-core:test --tests "org.hibernate.orm.test.$PACKAGE.*" -Pdb=h2 -Pagent

  printf "$BLUE=== Merging native-image metadata with existing one ===$NC\n"
  ./gradlew hibernate-core:metadataCopy

  # Save current run state for resuming
  echo "$PACKAGE" > last-successful-agent-run
done
