#!/bin/bash

# Set constants
VERSION_FILE="src/mangara/miniweb/MiniWeb.java"
CHANGELOG="CHANGELOG.md"
TEST_OUTPUT="test-output.tmp"
JAR_FILE="store/MiniWeb.jar"
PROJECT_DIR=${PWD##*/}


# Process parameters
while [[ $# -gt 0 ]]
do
    key="$1"

    case $key in
        --final)
            opt_final=TRUE
            shift # past argument
        ;;
        *)
                # unknown option
        ;;
    esac

    shift # past argument or value
done


# Verify that this is being run on the release branch
branch="$(hg branch)"

if [ ! $branch = "release" ]
then
    echo "ERROR: Current branch is \"$branch\". To use this script, switch to the release branch with \"hg update release\", merge all the necessary changes from the main branch, then run this script again."
    exit 1
fi


# Get the version number
versionRegex="_VERSION ?= ?([0-9][0-9]*);"

codeMajorVersionLine="$(grep MAJOR_VERSION $VERSION_FILE)"
[[ $codeMajorVersionLine =~ $versionRegex ]]
codeMajorVersion="${BASH_REMATCH[1]}"

codeMinorVersionLine="$(grep MINOR_VERSION $VERSION_FILE)"
[[ $codeMinorVersionLine =~ $versionRegex ]]
codeMinorVersion="${BASH_REMATCH[1]}"

version="${codeMajorVersion}.${codeMinorVersion}"


# Verify that the change log has been updated
latestVersionChangeLine="$(grep --max-count=1 "## \[" $CHANGELOG)"

# Check that the version is correct
changelogVersionRegex="\[([0-9][0-9]*\.[0-9][0-9]*)\]"
[[ $latestVersionChangeLine =~ $changelogVersionRegex ]]
changelogVersion="${BASH_REMATCH[1]}"

if [ ! $changelogVersion = $version ]
then
    echo "ERROR: Latest change log version (${changelogVersion}) does not match current code version (${version})."
    exit 1
fi

# Check that the date is correct
changelogDateRegex="([0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9])"
[[ $latestVersionChangeLine =~ $changelogDateRegex ]]
changelogDate="${BASH_REMATCH[1]}"

if [[ -z $changelogDate ]]
then
    echo "ERROR: The change log does not specify a date for the latest version."
    exit 1
fi

if [[ ! $changelogDate = "$(date +'%Y-%m-%d')" ]]
then
    echo "ERROR: Latest change log version date (${changelogDate}) does not match today's date ($(date +'%Y-%m-%d'))."
    exit 1
fi


# Clean and build the project
echo "Cleaning and building the project... "
ant -S clean
ant -S jar
echo "done."


# Remove the old jar file
echo -n "Removing old jar file... "
for f in MiniWeb*.jar; do
    [ -e "$f" ] && rm "$f"
done
echo "done."


# Create the new jar file
echo -n "Creating new zip file... "
RELEASE_FILE="MiniWeb-v$version.jar"

if [ ! -e "$JAR_FILE" ]
then
    echo "ERROR: Jar file $JAR_FILE is missing. Run a clean and build operation in NetBeans to generate this file."
    exit 1
fi

cp "$JAR_FILE" "$RELEASE_FILE"
chmod +x "$RELEASE_FILE" # Make it executable
echo "done."


# Finalize release
if [ $opt_final ]
then
    # Commit these changes
    echo -n "Committing changes... "
    hg add "$RELEASE_FILE"
    hg remove --after
    hg commit -m "Updated release zip."


    # Tag the current revision with the release number
    hg tag "v$version"
    echo "done."
fi
