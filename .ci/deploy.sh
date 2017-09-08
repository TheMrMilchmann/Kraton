#!/bin/bash

SLUG="TheMrMilchmann/Kraton"
JDK="oraclejdk8"
BRANCH="master"

set -e

if [ "$TRAVIS_REPO_SLUG" == "$SLUG" ] && [ "$TRAVIS_JDK_VERSION" == "$JDK" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "$BRANCH" ]; then

    # Upload snapshot artifacts to OSSRH.

    source ./gradlew.sh uploadArchives --parallel -Psnapshot

    # Upload latest Javadoc to Github pages.

    echo -e "[deploy.sh] Publishing Javadoc...\n"
    cp -R build/docs/javadoc

    cd $HOME
    git config --global user.email "travis@travis-ci.org"
    git config --global user.name "travis-ci"
    git clone --quiet --branch=gh-pages https://${GH_TOKEN}@github.com/TheMrMilchmann/Kraton gh-pages > /dev/null

    cd gp-pages
    git rm -rf .
    cp -Rf $HOME/javadoc-latest .
    git add -f .
    git commit -m "ci: update javadoc (travis build $TRAVIS_BUILD_NUMBER)"
    git push -fq origin gh-pages > /dev/null

    echo -e "Published Javadoc to gh-pages.\n"
fi