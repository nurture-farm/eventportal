#!/bin/bash
DEPLOY_ENV=$1

if [ -z "$DEPLOY_ENV" ]
then
      echo "Deploy env not specified. Please use this script as make deploy_(dev|stage|prod)"
      exit 1;
fi

# Default values for dev
VERSION="latest"
TAG="latest"

# For stage build tag will be latest-stage
if [ "$DEPLOY_ENV" == 'stage' ]; then
  VERSION="latest-stage"
  TAG="latest-stage"
fi

# For prod build create a new tag based on date version and branch
if [ "$DEPLOY_ENV" == 'prod' ]; then
  if [ -z "$2" ]; then
        printf "Branch not specified. \nPlease use this script as ./deploy_container.sh prod branch version"
        exit 1;
  fi
  BRANCH=$2
  if [ -z "$3" ]; then
        printf "Version not specified. \nPlease use this script as ./deploy_container.sh prod branch version"
        exit 1;
  fi
  VERSION="$3"
  TAG="$(date +'%Y-%m-%d')-v$VERSION-$BRANCH"

  read -p "$TAG will be used as tag do you want to continue?[y] (y|n): " permission
  permission=${permission:-y}
  if [ "$permission" != 'y' ]; then
        printf "Bailing going for a drink would you like to join."
        exit 1;
  fi
fi

echo "Creating build and deploying to env $DEPLOY_ENV with version $VERSION and tag $TAG"

# Create a maven build for the project
mvn clean package || exit

# Create a tag for prod build and push to the repo
if [ "$DEPLOY_ENV" == 'prod' ]; then
  echo "Creating a tag from the current branch with tag v$VERSION"
  git tag -a "v$VERSION" -m "Prod release with container tag as $TAG"
  git push --follow-tags
fi


# Create a docker container for the given env
docker build -t platform/event-portal:"$VERSION" --no-cache --build-arg BUILD_FOR="$DEPLOY_ENV" .